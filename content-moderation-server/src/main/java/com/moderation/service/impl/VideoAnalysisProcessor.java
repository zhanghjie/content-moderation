package com.moderation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.common.ModerationResultEnum;
import com.moderation.common.TaskStatus;
import com.moderation.entity.VideoAnalysisTaskEntity;
import com.moderation.mapper.VideoAnalysisTaskMapper;
import com.moderation.model.dto.ViolationDTO;
import com.moderation.model.impl.LLMIntegrationService;
import com.moderation.model.req.VideoAnalyzeReq;
import com.moderation.model.res.VideoAnalyzeRes.VideoSummaryDTO;
import com.moderation.prompt.PromptComposer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoAnalysisProcessor {

    private final VideoAnalysisTaskMapper videoAnalysisTaskMapper;
    private final LLMIntegrationService llmIntegrationService;
    private final PromptComposer promptComposer;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void process(String taskId, VideoAnalyzeReq req) {
        VideoAnalysisTaskEntity task = videoAnalysisTaskMapper.selectByTaskId(taskId);
        if (task == null) return;

        try {
            task.setStatus(TaskStatus.PROCESSING.getCode());
            videoAnalysisTaskMapper.updateById(task);

            String analysisType = normalizeAnalysisType(task.getAnalysisType());
            String json;

            if ("HOST_VIOLATION".equals(analysisType)) {
                if (req.getUserId() == null) {
                    throw new IllegalArgumentException("userId is required for HOST_VIOLATION");
                }
                PromptComposer.ComposedPrompt composed = promptComposer.composeHostViolation(
                        req.getPromptModules(),
                        Map.of(
                                "CALL_ID", req.getCallId(),
                                "USER_ID", String.valueOf(req.getUserId()),
                                "VIDEO_URL", req.getVideoUrl()
                        )
                );
                task.setPromptModules(String.join(",", composed.modules()));
                task.setPromptSnapshot(composed.prompt());
                String raw = llmIntegrationService.analyzeVideo(req.getVideoUrl(), req.getContentId(), composed.prompt());
                json = extractJson(raw);
            } else {
                String raw = llmIntegrationService.analyzeVideo(req.getVideoUrl(), req.getContentId());
                json = extractJson(raw);
            }

            task.setResultJson(json);
            ParsedResult parsed = parseResultJson(json);
            task.setModerationResult(deriveModerationResult(parsed.violations, parsed.hasViolationSignal));
            task.setOverallConfidence(deriveOverallConfidence(parsed.violations, parsed.hasViolationSignal));
            task.setStatus(TaskStatus.COMPLETED.getCode());
            task.setCompletedAt(OffsetDateTime.now());
            videoAnalysisTaskMapper.updateById(task);
        } catch (Exception e) {
            log.error("Process video analysis failed, taskId: {}", taskId, e);
            task.setStatus(TaskStatus.FAILED.getCode());
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(OffsetDateTime.now());
            videoAnalysisTaskMapper.updateById(task);
        }
    }

    private String normalizeAnalysisType(String analysisType) {
        if (analysisType == null || analysisType.isBlank()) return "STANDARD";
        return analysisType.trim().toUpperCase();
    }

    private String extractJson(String llmContent) {
        if (llmContent == null) return "{}";
        String trimmed = llmContent.trim();
        int fenceStart = trimmed.indexOf("```");
        if (fenceStart >= 0) {
            int fenceEnd = trimmed.lastIndexOf("```");
            if (fenceEnd > fenceStart) {
                String inside = trimmed.substring(fenceStart + 3, fenceEnd).trim();
                if (inside.startsWith("json")) inside = inside.substring(4).trim();
                trimmed = inside;
            }
        }
        return trimmed;
    }

    private record ParsedResult(List<ViolationDTO> violations, VideoSummaryDTO summary, boolean hasViolationSignal) {}

    private ParsedResult parseResultJson(String json) {
        if (json == null || json.isBlank()) {
            return new ParsedResult(new ArrayList<>(), new VideoSummaryDTO(), false);
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            List<ViolationDTO> violations = new ArrayList<>();
            JsonNode vioNode = root.path("violations");
            boolean hasViolationSignal = root.has("violations") && vioNode.isArray();
            if (vioNode.isArray()) {
                for (JsonNode v : vioNode) {
                    ViolationDTO dto = new ViolationDTO();
                    dto.setType(v.path("type").asText());
                    dto.setDetected(v.path("detected").asBoolean(false));
                    dto.setConfidence(v.path("confidence").isNumber() ? v.path("confidence").asDouble() : null);
                    dto.setEvidence(v.path("evidence").asText());
                    dto.setStartSec(readIntCompat(v, "start_sec", "startSec"));
                    dto.setEndSec(readIntCompat(v, "end_sec", "endSec"));
                    violations.add(dto);
                }
            }

            VideoSummaryDTO summary = new VideoSummaryDTO();
            JsonNode summaryNode = root.path("summary");
            Integer total = readIntCompat(summaryNode, "total_violations", "totalViolations");
            Integer high = readIntCompat(summaryNode, "high_confidence_count", "highConfidenceCount");
            String primary = summaryNode.path("primary_violation").asText(summaryNode.path("primaryViolation").asText(null));

            if (total == null) total = (int) violations.stream().filter(v -> Boolean.TRUE.equals(v.getDetected())).count();
            if (high == null) high = (int) violations.stream().filter(v -> Boolean.TRUE.equals(v.getDetected()) && v.getConfidence() != null && v.getConfidence() >= 0.85).count();
            if (primary == null || primary.isBlank()) {
                primary = violations.stream().filter(v -> Boolean.TRUE.equals(v.getDetected())).map(ViolationDTO::getType).findFirst().orElse("");
            }
            summary.setTotalViolations(total);
            summary.setHighConfidenceCount(high);
            summary.setPrimaryViolation(primary);
            return new ParsedResult(violations, summary, hasViolationSignal);
        } catch (Exception e) {
            log.warn("Failed to parse video analysis result JSON, fallback to empty summary. error={}", e.getMessage());
            return new ParsedResult(new ArrayList<>(), new VideoSummaryDTO(), false);
        }
    }

    private Integer readIntCompat(JsonNode node, String snake, String camel) {
        if (node == null || node.isMissingNode()) return null;
        JsonNode v = node.get(snake);
        if (v == null) v = node.get(camel);
        if (v == null || v.isNull() || !v.isNumber()) return null;
        return v.asInt();
    }

    private String deriveModerationResult(List<ViolationDTO> violations, boolean hasViolationSignal) {
        if (!hasViolationSignal) return null;
        List<ViolationDTO> detected = violations.stream().filter(v -> Boolean.TRUE.equals(v.getDetected())).toList();
        if (detected.isEmpty()) return ModerationResultEnum.NOT_HIT.getCode();
        boolean high = detected.stream().anyMatch(v -> v.getConfidence() != null && v.getConfidence() >= 0.9);
        if (high || detected.size() >= 2) return ModerationResultEnum.HIT.getCode();
        return ModerationResultEnum.SUSPECTED.getCode();
    }

    private Double deriveOverallConfidence(List<ViolationDTO> violations, boolean hasViolationSignal) {
        if (!hasViolationSignal) return null;
        double avg = violations.stream()
                .filter(v -> Boolean.TRUE.equals(v.getDetected()) && v.getConfidence() != null)
                .mapToDouble(ViolationDTO::getConfidence)
                .average()
                .orElse(Double.NaN);
        if (Double.isNaN(avg)) return null;
        return Math.max(0.0, Math.min(1.0, avg));
    }
}
