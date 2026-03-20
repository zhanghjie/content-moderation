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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
                        : (TaskStatus.fromCode(task.getStatus()) == TaskStatus.COMPLETED
                        ? ModerationResultEnum.NOT_HIT.getCode()
                        : null));

        if (TaskStatus.fromCode(task.getStatus()) == TaskStatus.COMPLETED && task.getResultJson() != null) {
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
        List<VideoAnalyzeRes> list = pageResult.getRecords().stream().map(task -> {
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

            if (TaskStatus.fromCode(task.getStatus()) == TaskStatus.COMPLETED && task.getResultJson() != null) {
                ParsedResult parsed = parseResultJson(task.getResultJson());
                builder.violations(parsed.violations).summary(parsed.summary);
            }
            return builder.build();
        }).toList();

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
        try {
            JsonNode root = objectMapper.readTree(json);
            List<ViolationDTO> violations = new ArrayList<>();
            JsonNode vioNode = root.path("violations");
            if (vioNode.isArray()) {
                for (JsonNode v : vioNode) {
                    ViolationDTO dto = new ViolationDTO();
                    dto.setType(v.path("type").asText());
                    dto.setDetected(v.path("detected").asBoolean(false));
                    dto.setConfidence(v.path("confidence").isNumber() ? v.path("confidence").asDouble() : 0.0);
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
            Integer durationSec = readIntCompat(root, "video_duration_sec", "videoDurationSec");
            Double overall = summaryNode.path("overall_confidence").isNumber()
                    ? summaryNode.path("overall_confidence").asDouble()
                    : (summaryNode.path("overallConfidence").isNumber() ? summaryNode.path("overallConfidence").asDouble() : null);
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
            throw new RuntimeException("Failed to parse LLM result JSON: " + e.getMessage(), e);
        }
    }

    private Integer readIntCompat(JsonNode node, String snake, String camel) {
        if (node == null || node.isMissingNode()) return null;
        JsonNode v = node.get(snake);
        if (v == null) v = node.get(camel);
        if (v == null || v.isNull() || !v.isNumber()) return null;
        return v.asInt();
    }

}
