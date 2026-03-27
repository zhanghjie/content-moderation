package com.moderation.service.impl;

import com.moderation.common.TaskStatus;
import com.moderation.common.ModerationResultEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moderation.entity.VideoAnalysisTaskEntity;
import com.moderation.mapper.VideoAnalysisTaskMapper;
import com.moderation.model.dto.ViolationDTO;
import com.moderation.model.req.VideoAnalyzeReq;
import com.moderation.model.res.TaskListRes;
import com.moderation.model.res.VideoAnalyzeRes;
import com.moderation.model.res.VideoAnalyzeRes.VideoSummaryDTO;
import com.moderation.service.VideoAnalysisService;
import com.moderation.skillos.engine.PolicyExecuteResult;
import com.moderation.skillos.engine.PolicyExecutionEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.Arrays;

/**
 * 视频分析服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoAnalysisServiceImpl implements VideoAnalysisService {

    private final VideoAnalysisTaskMapper videoAnalysisTaskMapper;
    private final ObjectMapper objectMapper;
    private final VideoAnalysisProcessor videoAnalysisProcessor;
    private final PolicyExecutionEngine policyExecutionEngine;

    @Qualifier("videoAnalysisExecutor")
    private final Executor videoAnalysisExecutor;

    @Override
    public VideoAnalyzeRes analyze(VideoAnalyzeReq req) {
        String analysisType = normalizeAnalysisType(req.getAnalysisType());
        String businessId = normalizeBusinessId(req.getCallId(), req.getContentId());
        String callId = businessId;
        String contentId = businessId;
        req.setCallId(callId);
        req.setContentId(contentId);
        Long userId = req.getUserId();
        log.info("Starting video analysis, callId: {}, contentId: {}",
                callId, contentId);

        VideoAnalysisTaskEntity existing = videoAnalysisTaskMapper.selectActiveByContentId(contentId);
        if (existing != null) {
            log.info("Reused active task, taskId: {}, callId: {}", existing.getTaskId(), callId);
            return simpleTaskRes(existing);
        }

        VideoAnalysisTaskEntity task = new VideoAnalysisTaskEntity();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setCallId(callId);
        task.setContentId(contentId);
        task.setVideoUrl(req.getVideoUrl());
        task.setCoverUrl(req.getCoverUrl());
        task.setAnalysisType(analysisType);
        task.setUserId(userId);
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setRetryCount(0);
        try {
            videoAnalysisTaskMapper.insert(task);
        } catch (DataIntegrityViolationException ex) {
            VideoAnalysisTaskEntity conflict = videoAnalysisTaskMapper.selectActiveByContentId(contentId);
            if (conflict != null) {
                log.info("Duplicate submit blocked, reused taskId: {}", conflict.getTaskId());
                return simpleTaskRes(conflict);
            }
            throw ex;
        }
        log.info("Video analysis task created, taskId: {}", task.getTaskId());

        videoAnalysisExecutor.execute(() -> videoAnalysisProcessor.process(task.getTaskId(), req));

        return simpleTaskRes(task);
    }

    @Override
    public VideoAnalyzeRes analyzeAndSave(VideoAnalyzeReq req) {
        String analysisType = normalizeAnalysisType(req.getAnalysisType());
        String businessId = normalizeBusinessId(req.getCallId(), req.getContentId());
        req.setCallId(businessId);
        req.setContentId(businessId);
        req.setAnalysisType(analysisType);

        Map<String, Object> input = buildPolicyInput(req);
        Long userId = req.getUserId();
        log.info("Starting policy video analysis, policyId: {}, callId: {}, contentId: {}",
                req.getPolicyId(), businessId, businessId);

        VideoAnalysisTaskEntity existing = videoAnalysisTaskMapper.selectActiveByContentId(businessId);
        if (existing != null) {
            log.info("Reused active policy task, taskId: {}, callId: {}", existing.getTaskId(), businessId);
            return simpleTaskRes(existing);
        }

        VideoAnalysisTaskEntity task = new VideoAnalysisTaskEntity();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setCallId(businessId);
        task.setContentId(businessId);
        task.setVideoUrl(req.getVideoUrl());
        task.setCoverUrl(req.getCoverUrl());
        task.setAnalysisType(analysisType);
        task.setUserId(userId);
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setRetryCount(0);
        task.setPromptModules(joinPromptModules(req.getPromptModules()));

        try {
            videoAnalysisTaskMapper.insert(task);
        } catch (DataIntegrityViolationException ex) {
            VideoAnalysisTaskEntity conflict = videoAnalysisTaskMapper.selectActiveByContentId(businessId);
            if (conflict != null) {
                log.info("Duplicate policy submit blocked, reused taskId: {}", conflict.getTaskId());
                return simpleTaskRes(conflict);
            }
            throw ex;
        }

        task.setStatus(TaskStatus.PROCESSING.getCode());
        videoAnalysisTaskMapper.updateById(task);

        try {
            PolicyExecuteResult policyResult = policyExecutionEngine.execute(req.getPolicyId(), input);
            task.setTraceId(policyResult.getExecutionId());

            Map<String, Object> finalResult = extractFinalResult(policyResult);
            String resultJson = writeJson(finalResult);
            ParsedResult parsed = parseResultJson(resultJson);

            task.setResultJson(resultJson);
            task.setModerationResult(readString(finalResult.get("moderationResult")));
            if (task.getModerationResult() == null || task.getModerationResult().isBlank()) {
                task.setModerationResult(deriveModerationResult(parsed.violations));
            }
            task.setOverallConfidence(readDouble(finalResult.get("overallConfidence")));
            if (task.getOverallConfidence() == null) {
                task.setOverallConfidence(deriveOverallConfidence(parsed.violations));
            }
            task.setStatus(policyResult.isSuccess() ? TaskStatus.COMPLETED.getCode() : TaskStatus.FAILED.getCode());
            task.setCompletedAt(java.time.OffsetDateTime.now());
            task.setErrorMessage(policyResult.getErrorMessage());
            videoAnalysisTaskMapper.updateById(task);

            return buildPolicyAnalyzeRes(task, parsed, resultJson);
        } catch (Exception e) {
            log.error("Policy video analysis failed, policyId: {}, callId: {}", req.getPolicyId(), businessId, e);
            task.setStatus(TaskStatus.FAILED.getCode());
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(java.time.OffsetDateTime.now());
            videoAnalysisTaskMapper.updateById(task);
            return buildPolicyFailureRes(task, e.getMessage());
        }
    }

    @Override
    public VideoAnalyzeRes getResult(String callId) {
        log.info("Getting video analysis result, callId: {}", callId);

        VideoAnalysisTaskEntity task = videoAnalysisTaskMapper.selectByCallId(callId);

        if (task == null) {
            return VideoAnalyzeRes.builder()
                    .status("NOT_FOUND")
                    .build();
        }

        VideoAnalyzeRes.VideoAnalyzeResBuilder builder = VideoAnalyzeRes.builder()
                .taskId(task.getTaskId())
                .callId(task.getCallId())
                .contentId(task.getContentId())
                .videoUrl(task.getVideoUrl())
                .coverUrl(task.getCoverUrl())
                .status(task.getStatus())
                .analysisType(task.getAnalysisType())
                .userId(task.getUserId())
                .promptModules(task.getPromptModules())
                .overallConfidence(task.getOverallConfidence())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .errorMessage(task.getErrorMessage())
                .resultJson(task.getResultJson())
                .moderationResult(task.getModerationResult() != null
                        ? task.getModerationResult()
                        : (isCompleted(task.getStatus())
                        ? ModerationResultEnum.NOT_HIT.getCode()
                        : null));

        if (isCompleted(task.getStatus()) && task.getResultJson() != null) {
            ParsedResult parsed = parseResultJson(task.getResultJson());
            builder.violations(parsed.violations).summary(parsed.summary);
        }

        return builder.build();
    }

    @Override
    public TaskListRes getTasks(String callId, String status, String result, Integer page, Integer pageSize) {
        int p = page == null || page < 1 ? 1 : page;
        int ps = pageSize == null || pageSize < 1 ? 10 : pageSize;

        QueryWrapper<VideoAnalysisTaskEntity> wrapper = new QueryWrapper<>();
        if (callId != null && !callId.isBlank()) {
            wrapper.like("call_id", callId.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status.trim().toUpperCase());
        }
        if (result != null && !result.isBlank()) {
            wrapper.eq("moderation_result", result.trim().toUpperCase());
        }
        wrapper.orderByDesc("created_at");

        Page<VideoAnalysisTaskEntity> pageResult = videoAnalysisTaskMapper.selectPage(new Page<>(p, ps), wrapper);
        List<VideoAnalyzeRes> list = pageResult.getRecords().stream()
                .map(this::toListItemSafely)
                .toList();

        return TaskListRes.builder()
                .total(pageResult.getTotal())
                .page(p)
                .pageSize(ps)
                .list(list)
                .build();
    }

    @Override
    public VideoAnalyzeRes reAnalyze(String callId) {
        if (callId == null || callId.isBlank()) {
            throw new IllegalArgumentException("callId is blank");
        }
        VideoAnalysisTaskEntity latest = videoAnalysisTaskMapper.selectByCallId(callId);
        if (latest == null) {
            return VideoAnalyzeRes.builder().status("NOT_FOUND").build();
        }

        List<String> modules = null;
        if (latest.getPromptModules() != null && !latest.getPromptModules().isBlank()) {
            modules = Arrays.stream(latest.getPromptModules().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        return analyze(VideoAnalyzeReq.builder()
                .callId(latest.getCallId())
                .contentId(latest.getContentId())
                .videoUrl(latest.getVideoUrl())
                .coverUrl(latest.getCoverUrl())
                .analysisType(latest.getAnalysisType())
                .userId(latest.getUserId())
                .promptModules(modules)
                .build());
    }

    private String normalizeAnalysisType(String analysisType) {
        if (analysisType == null || analysisType.isBlank()) return "STANDARD";
        return analysisType.trim().toUpperCase();
    }

    private String deriveModerationResult(List<ViolationDTO> violations) {
        List<ViolationDTO> detected = violations.stream().filter(v -> Boolean.TRUE.equals(v.getDetected())).toList();
        if (detected.isEmpty()) {
            return ModerationResultEnum.NOT_HIT.getCode();
        }
        boolean high = detected.stream().anyMatch(v -> v.getConfidence() != null && v.getConfidence() >= 0.9);
        if (high || detected.size() >= 2) {
            return ModerationResultEnum.HIT.getCode();
        }
        return ModerationResultEnum.SUSPECTED.getCode();
    }

    private Double deriveOverallConfidence(List<ViolationDTO> violations) {
        double avg = violations.stream()
                .filter(v -> Boolean.TRUE.equals(v.getDetected()) && v.getConfidence() != null)
                .mapToDouble(ViolationDTO::getConfidence)
                .average()
                .orElse(1.0);
        return Math.max(0.0, Math.min(1.0, avg));
    }

    private Map<String, Object> buildPolicyInput(VideoAnalyzeReq req) {
        Map<String, Object> input = new LinkedHashMap<>();
        if (req.getPolicyInput() != null) {
            input.putAll(req.getPolicyInput());
        }
        input.put("callId", req.getCallId());
        input.put("contentId", req.getContentId());
        input.put("videoUrl", req.getVideoUrl());
        input.put("coverUrl", req.getCoverUrl());
        input.put("analysisType", req.getAnalysisType());
        input.put("userId", req.getUserId());
        return input;
    }

    private VideoAnalyzeRes buildPolicyAnalyzeRes(VideoAnalysisTaskEntity task, ParsedResult parsed, String resultJson) {
        return VideoAnalyzeRes.builder()
                .taskId(task.getTaskId())
                .callId(task.getCallId())
                .contentId(task.getContentId())
                .videoUrl(task.getVideoUrl())
                .coverUrl(task.getCoverUrl())
                .status(task.getStatus())
                .analysisType(task.getAnalysisType())
                .userId(task.getUserId())
                .moderationResult(task.getModerationResult())
                .violations(parsed.violations)
                .summary(parsed.summary)
                .overallConfidence(task.getOverallConfidence())
                .promptModules(task.getPromptModules())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .errorMessage(task.getErrorMessage())
                .resultJson(resultJson)
                .build();
    }

    private VideoAnalyzeRes buildPolicyFailureRes(VideoAnalysisTaskEntity task, String errorMessage) {
        return VideoAnalyzeRes.builder()
                .taskId(task.getTaskId())
                .callId(task.getCallId())
                .contentId(task.getContentId())
                .videoUrl(task.getVideoUrl())
                .coverUrl(task.getCoverUrl())
                .status(task.getStatus())
                .analysisType(task.getAnalysisType())
                .userId(task.getUserId())
                .moderationResult(task.getModerationResult())
                .overallConfidence(task.getOverallConfidence())
                .promptModules(task.getPromptModules())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .errorMessage(errorMessage)
                .build();
    }

    private Map<String, Object> extractFinalResult(PolicyExecuteResult policyResult) {
        if (policyResult == null || policyResult.getState() == null || policyResult.getState().getData() == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> data = policyResult.getState().getData();
        Object finalResult = data.get("finalResult");
        if (finalResult instanceof Map<?, ?> map) {
            return normalizeMap(map);
        }
        Object output = data.get(OUTPUT_SKILL_ID);
        if (output instanceof Map<?, ?> map) {
            return normalizeMap(map);
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> normalizeMap(Map<?, ?> source) {
        Map<String, Object> target = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                target.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return target;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new LinkedHashMap<>() : value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String readString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Double readDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String joinPromptModules(List<String> promptModules) {
        if (promptModules == null || promptModules.isEmpty()) {
            return null;
        }
        return String.join(",", promptModules.stream().map(String::trim).filter(s -> !s.isBlank()).toList());
    }

    private String normalizeBusinessId(String callId, String contentId) {
        String cId = contentId == null ? null : contentId.trim();
        String call = callId == null ? null : callId.trim();
        if (cId != null && !cId.isBlank()) {
            if (call != null && !call.isBlank() && !call.equals(cId)) {
                log.warn("callId/contentId mismatch, normalize to contentId. callId={}, contentId={}", call, cId);
            }
            return cId;
        }
        return call;
    }

    private static final String OUTPUT_SKILL_ID = "output_1774514701619";

    private VideoAnalyzeRes simpleTaskRes(VideoAnalysisTaskEntity task) {
        return VideoAnalyzeRes.builder()
                .taskId(task.getTaskId())
                .callId(task.getCallId())
                .status(task.getStatus())
                .analysisType(task.getAnalysisType())
                .userId(task.getUserId())
                .build();
    }


    private record ParsedResult(List<ViolationDTO> violations, VideoSummaryDTO summary) {}

    private ParsedResult parseResultJson(String json) {
        if (json == null || json.isBlank()) {
            return new ParsedResult(new ArrayList<>(), new VideoSummaryDTO());
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            List<ViolationDTO> violations = new ArrayList<>();
            JsonNode vioNode = root.path("violations");
            if (vioNode.isArray()) {
                for (JsonNode v : vioNode) {
                    ViolationDTO dto = new ViolationDTO();
                    dto.setType(v.path("type").asText());
                    dto.setDetected(v.path("detected").asBoolean(false));
                    Double confidence = readDoubleCompat(v, "confidence", "confidence");
                    dto.setConfidence(confidence != null ? confidence : 0.0);
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
            String primary = readTextCompat(summaryNode, "primary_violation", "primaryViolation");
            Integer durationSec = readIntCompat(root, "video_duration_sec", "videoDurationSec");
            Double overall = readDoubleCompat(summaryNode, "overall_confidence", "overallConfidence");
            if (total == null) total = (int) violations.stream().filter(v -> Boolean.TRUE.equals(v.getDetected())).count();
            if (high == null) high = (int) violations.stream().filter(v -> Boolean.TRUE.equals(v.getDetected()) && v.getConfidence() != null && v.getConfidence() >= 0.85).count();
            if (primary == null || primary.isBlank()) {
                primary = violations.stream().filter(v -> Boolean.TRUE.equals(v.getDetected())).map(ViolationDTO::getType).findFirst().orElse("");
            }
            summary.setTotalViolations(total);
            summary.setHighConfidenceCount(high);
            summary.setPrimaryViolation(primary);
            summary.setVideoDurationSec(durationSec);
            summary.setOverallConfidence(overall);
            return new ParsedResult(violations, summary);
        } catch (Exception e) {
            log.warn("Failed to parse task result JSON, fallback to empty summary. task result will still be returned safely. error={}", e.getMessage());
            return new ParsedResult(new ArrayList<>(), new VideoSummaryDTO());
        }
    }

    private VideoAnalyzeRes toListItemSafely(VideoAnalysisTaskEntity task) {
        try {
            VideoAnalyzeRes.VideoAnalyzeResBuilder builder = VideoAnalyzeRes.builder()
                    .taskId(task.getTaskId())
                    .callId(task.getCallId())
                    .contentId(task.getContentId())
                    .videoUrl(task.getVideoUrl())
                    .coverUrl(task.getCoverUrl())
                    .status(task.getStatus())
                    .analysisType(task.getAnalysisType())
                    .userId(task.getUserId())
                    .promptModules(task.getPromptModules())
                    .overallConfidence(task.getOverallConfidence())
                    .createdAt(task.getCreatedAt())
                    .completedAt(task.getCompletedAt())
                    .errorMessage(task.getErrorMessage())
                    .moderationResult(task.getModerationResult());

            if (isCompleted(task.getStatus()) && task.getResultJson() != null) {
                ParsedResult parsed = parseResultJson(task.getResultJson());
                builder.violations(parsed.violations).summary(parsed.summary);
            }
            return builder.build();
        } catch (Exception e) {
            log.warn("Failed to map task list item safely, taskId={}, callId={}, error={}",
                    task.getTaskId(), task.getCallId(), e.getMessage());
            return VideoAnalyzeRes.builder()
                    .taskId(task.getTaskId())
                    .callId(task.getCallId())
                    .contentId(task.getContentId())
                    .videoUrl(task.getVideoUrl())
                    .coverUrl(task.getCoverUrl())
                    .status(task.getStatus())
                    .analysisType(task.getAnalysisType())
                    .userId(task.getUserId())
                    .promptModules(task.getPromptModules())
                    .overallConfidence(task.getOverallConfidence())
                    .createdAt(task.getCreatedAt())
                    .completedAt(task.getCompletedAt())
                    .errorMessage(task.getErrorMessage())
                    .moderationResult(task.getModerationResult())
                    .violations(new ArrayList<>())
                    .summary(new VideoSummaryDTO())
                    .build();
        }
    }

    private boolean isCompleted(String status) {
        return status != null && TaskStatus.COMPLETED.getCode().equalsIgnoreCase(status.trim());
    }

    private Integer readIntCompat(JsonNode node, String snake, String camel) {
        if (node == null || node.isMissingNode()) return null;
        JsonNode v = node.get(snake);
        if (v == null) v = node.get(camel);
        if (v == null || v.isNull() || !v.isNumber()) return null;
        return v.asInt();
    }

    private Double readDoubleCompat(JsonNode node, String snake, String camel) {
        if (node == null || node.isMissingNode()) return null;
        JsonNode v = node.get(snake);
        if (v == null) v = node.get(camel);
        if (v == null || v.isNull() || !v.isNumber()) return null;
        return v.asDouble();
    }

    private String readTextCompat(JsonNode node, String snake, String camel) {
        if (node == null || node.isMissingNode()) return null;
        JsonNode v = node.get(snake);
        if (v == null) v = node.get(camel);
        if (v == null || v.isNull()) return null;
        String text = v.asText();
        return text == null || text.isBlank() ? null : text;
    }

}
