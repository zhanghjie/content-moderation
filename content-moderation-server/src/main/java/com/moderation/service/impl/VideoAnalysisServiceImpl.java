package com.moderation.service.impl;

import com.moderation.common.TaskStatus;
import com.moderation.common.ModerationResultEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moderation.entity.VideoAnalysisTaskEntity;
import com.moderation.mapper.VideoAnalysisTaskMapper;
import com.moderation.model.dto.ViolationDTO;
import com.moderation.model.req.VideoAnalyzeReq;
import com.moderation.model.req.VideoDraftSaveReq;
import com.moderation.model.res.TaskListRes;
import com.moderation.model.res.VideoAnalyzeRes;
import com.moderation.model.res.VideoDraftRes;
import com.moderation.model.res.VideoAnalyzeRes.VideoSummaryDTO;
import com.moderation.service.VideoAnalysisService;
import com.moderation.skillos.engine.PolicyExecuteResult;
import com.moderation.skillos.engine.PolicyExecutionEngine;
import com.moderation.skillos.registry.PolicyRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final PolicyRegistry policyRegistry;

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
        task.setPolicyId(req.getPolicyId());

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
                task.setModerationResult(deriveModerationResult(parsed.violations, parsed.hasViolationSignal));
            }
            task.setOverallConfidence(readDouble(finalResult.get("overallConfidence")));
            if (task.getOverallConfidence() == null) {
                task.setOverallConfidence(deriveOverallConfidence(parsed.violations, parsed.hasViolationSignal));
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
                .updatedAt(task.getUpdatedAt())
                .completedAt(task.getCompletedAt())
                .errorMessage(task.getErrorMessage())
                .resultJson(task.getResultJson())
                .policyId(task.getPolicyId())
                .moderationResult(task.getModerationResult());

        if (isCompleted(task.getStatus()) && task.getResultJson() != null) {
            ParsedResult parsed = parseResultJson(task.getResultJson());
            builder.violations(parsed.violations).summary(parsed.summary);
            if (!parsed.hasViolationSignal && parsed.explicitModerationResult == null) {
                builder.moderationResult(null);
            }
            if (!parsed.hasViolationSignal
                    && parsed.explicitOverallConfidence == null
                    && parsed.summary.getOverallConfidence() == null) {
                builder.overallConfidence(null);
            }
        }
        
        VideoAnalyzeRes result = builder.build();
        
        // 填充 Policy 名称
        if (result.getPolicyId() != null && !result.getPolicyId().isBlank()) {
            try {
                com.moderation.skillos.model.PolicyDefinition policyDef = policyRegistry.get(result.getPolicyId());
                result.setPolicyName(policyDef.getName());
            } catch (Exception e) {
                log.debug("Failed to get policy name, policyId: {}", result.getPolicyId());
                result.setPolicyName("未知策略");
            }
        }
        
        return result;
    }

    @Override
    public VideoDraftRes saveDraft(VideoDraftSaveReq req) {
        String policyId = req.getPolicyId() == null ? "" : req.getPolicyId().trim();
        if (policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        Map<String, Object> policyInput = req.getPolicyInput() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(req.getPolicyInput());
        String analysisType = normalizeAnalysisType(req.getAnalysisType());

        VideoAnalysisTaskEntity draft = null;
        String incomingTaskId = req.getTaskId() == null ? "" : req.getTaskId().trim();
        if (!incomingTaskId.isBlank()) {
            VideoAnalysisTaskEntity existing = videoAnalysisTaskMapper.selectByTaskId(incomingTaskId);
            if (existing != null && TaskStatus.DRAFT.getCode().equalsIgnoreCase(existing.getStatus())) {
                draft = existing;
            }
        }

        if (draft == null) {
            draft = new VideoAnalysisTaskEntity();
            draft.setTaskId(UUID.randomUUID().toString().replace("-", ""));
            draft.setStatus(TaskStatus.DRAFT.getCode());
            draft.setRetryCount(0);
        }

        String taskId = draft.getTaskId();
        String callId = readString(policyInput.get("callId"));
        String contentId = readString(policyInput.get("contentId"));
        String videoUrl = readString(policyInput.get("videoUrl"));
        String coverUrl = readString(policyInput.get("coverUrl"));
        Long userId = readLong(policyInput.get("userId"));

        if (callId == null || callId.isBlank()) {
            callId = "DRAFT_" + taskId;
        }
        if (contentId == null || contentId.isBlank()) {
            contentId = callId;
        }
        if (videoUrl == null || videoUrl.isBlank()) {
            videoUrl = "https://draft.local/" + taskId;
        }

        draft.setCallId(callId);
        draft.setContentId(contentId);
        draft.setVideoUrl(videoUrl);
        draft.setCoverUrl(coverUrl);
        draft.setAnalysisType(analysisType);
        draft.setUserId(userId);
        draft.setPolicyId(policyId);
        draft.setDraftPayloadJson(writeDraftPayload(policyId, analysisType, policyInput));
        draft.setModerationResult(null);
        draft.setOverallConfidence(null);
        draft.setResultJson(null);
        draft.setErrorMessage(null);
        draft.setCompletedAt(null);

        if (draft.getId() == null) {
            videoAnalysisTaskMapper.insert(draft);
        } else {
            videoAnalysisTaskMapper.updateById(draft);
        }
        return toDraftRes(draft, policyInput);
    }

    @Override
    public VideoDraftRes getDraft(String taskId) {
        VideoAnalysisTaskEntity draft = videoAnalysisTaskMapper.selectByTaskId(taskId);
        if (draft == null || !TaskStatus.DRAFT.getCode().equalsIgnoreCase(draft.getStatus())) {
            throw new IllegalArgumentException("draft not found: " + taskId);
        }
        return toDraftRes(draft, readDraftPolicyInput(draft));
    }

    @Override
    public VideoAnalyzeRes executeDraft(String taskId) {
        VideoAnalysisTaskEntity task = videoAnalysisTaskMapper.selectByTaskId(taskId);
        if (task == null || !TaskStatus.DRAFT.getCode().equalsIgnoreCase(task.getStatus())) {
            throw new IllegalArgumentException("draft not found: " + taskId);
        }
        if (task.getPolicyId() == null || task.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("draft policyId is missing");
        }

        Map<String, Object> input = readDraftPolicyInput(task);
        if (!input.containsKey("callId")) input.put("callId", task.getCallId());
        if (!input.containsKey("contentId")) input.put("contentId", task.getContentId());
        if (!input.containsKey("videoUrl")) input.put("videoUrl", task.getVideoUrl());
        if (!input.containsKey("coverUrl")) input.put("coverUrl", task.getCoverUrl());
        if (!input.containsKey("analysisType")) input.put("analysisType", task.getAnalysisType());
        if (!input.containsKey("userId")) input.put("userId", task.getUserId());

        task.setStatus(TaskStatus.PROCESSING.getCode());
        task.setErrorMessage(null);
        task.setModerationResult(null);
        task.setOverallConfidence(null);
        task.setResultJson(null);
        task.setCompletedAt(null);
        videoAnalysisTaskMapper.updateById(task);

        try {
            PolicyExecuteResult policyResult = policyExecutionEngine.execute(task.getPolicyId(), input);
            task.setTraceId(policyResult.getExecutionId());

            Map<String, Object> finalResult = extractFinalResult(policyResult);
            String resultJson = writeJson(finalResult);
            ParsedResult parsed = parseResultJson(resultJson);

            task.setResultJson(resultJson);
            task.setModerationResult(readString(finalResult.get("moderationResult")));
            if (task.getModerationResult() == null || task.getModerationResult().isBlank()) {
                task.setModerationResult(deriveModerationResult(parsed.violations, parsed.hasViolationSignal));
            }
            task.setOverallConfidence(readDouble(finalResult.get("overallConfidence")));
            if (task.getOverallConfidence() == null) {
                task.setOverallConfidence(deriveOverallConfidence(parsed.violations, parsed.hasViolationSignal));
            }
            task.setStatus(policyResult.isSuccess() ? TaskStatus.COMPLETED.getCode() : TaskStatus.FAILED.getCode());
            task.setCompletedAt(java.time.OffsetDateTime.now());
            task.setErrorMessage(policyResult.getErrorMessage());
            videoAnalysisTaskMapper.updateById(task);

            return buildPolicyAnalyzeRes(task, parsed, resultJson);
        } catch (Exception e) {
            log.error("Execute draft failed, taskId: {}", taskId, e);
            task.setStatus(TaskStatus.FAILED.getCode());
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(java.time.OffsetDateTime.now());
            videoAnalysisTaskMapper.updateById(task);
            return buildPolicyFailureRes(task, e.getMessage());
        }
    }

    @Override
    public TaskListRes getTasks(String callId, String policyId, String status, String result, Integer page, Integer pageSize) {
        int p = page == null || page < 1 ? 1 : page;
        int ps = pageSize == null || pageSize < 1 ? 10 : pageSize;

        QueryWrapper<VideoAnalysisTaskEntity> wrapper = new QueryWrapper<>();
        if (callId != null && !callId.isBlank()) {
            wrapper.like("call_id", callId.trim());
        }
        if (policyId != null && !policyId.isBlank()) {
            wrapper.eq("policy_id", policyId.trim());
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
        
        // 批量查询 Policy 名称并填充
        enrichPolicyNames(list);

        return TaskListRes.builder()
                .total(pageResult.getTotal())
                .page(p)
                .pageSize(ps)
                .list(list)
                .build();
    }

    /**
     * 批量填充 Policy 名称
     */
    private void enrichPolicyNames(List<VideoAnalyzeRes> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        
        // 收集所有 policyId
        List<String> policyIds = tasks.stream()
                .map(VideoAnalyzeRes::getPolicyId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        
        if (policyIds.isEmpty()) {
            return;
        }
        
        // 批量查询 Policy 名称
        Map<String, String> policyNameMap = new LinkedHashMap<>();
        for (String policyId : policyIds) {
            try {
                com.moderation.skillos.model.PolicyDefinition policyDef = policyRegistry.get(policyId);
                policyNameMap.put(policyId, policyDef.getName());
            } catch (Exception e) {
                log.debug("Failed to get policy name, policyId: {}", policyId);
                policyNameMap.put(policyId, "未知策略");
            }
        }
        
        // 填充到任务列表
        for (VideoAnalyzeRes task : tasks) {
            if (task.getPolicyId() != null && !task.getPolicyId().isBlank()) {
                task.setPolicyName(policyNameMap.get(task.getPolicyId()));
            }
        }
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

    private String deriveModerationResult(List<ViolationDTO> violations, boolean hasViolationSignal) {
        if (!hasViolationSignal) {
            return null;
        }
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

    private Double deriveOverallConfidence(List<ViolationDTO> violations, boolean hasViolationSignal) {
        if (!hasViolationSignal) {
            return null;
        }
        double avg = violations.stream()
                .filter(v -> Boolean.TRUE.equals(v.getDetected()) && v.getConfidence() != null)
                .mapToDouble(ViolationDTO::getConfidence)
                .average()
                .orElse(Double.NaN);
        if (Double.isNaN(avg)) {
            return null;
        }
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

    private Long readLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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

    private String writeDraftPayload(String policyId, String analysisType, Map<String, Object> policyInput) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("policyId", policyId);
        payload.put("analysisType", analysisType);
        payload.put("policyInput", policyInput == null ? new LinkedHashMap<>() : policyInput);
        return writeJson(payload);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readDraftPolicyInput(VideoAnalysisTaskEntity draft) {
        if (draft == null || draft.getDraftPayloadJson() == null || draft.getDraftPayloadJson().isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(draft.getDraftPayloadJson(), Map.class);
            Object input = payload.get("policyInput");
            if (input instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null) {
                        normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                return normalized;
            }
            return new LinkedHashMap<>();
        } catch (Exception e) {
            log.warn("Failed to parse draft payload json, taskId={}, error={}", draft.getTaskId(), e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private VideoDraftRes toDraftRes(VideoAnalysisTaskEntity draft, Map<String, Object> policyInput) {
        return VideoDraftRes.builder()
                .taskId(draft.getTaskId())
                .policyId(draft.getPolicyId())
                .analysisType(draft.getAnalysisType())
                .policyInput(policyInput == null ? new HashMap<>() : policyInput)
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .build();
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


    private record ParsedResult(
            List<ViolationDTO> violations,
            VideoSummaryDTO summary,
            boolean hasViolationSignal,
            String explicitModerationResult,
            Double explicitOverallConfidence
    ) {}

    private ParsedResult parseResultJson(String json) {
        if (json == null || json.isBlank()) {
            return new ParsedResult(new ArrayList<>(), new VideoSummaryDTO(), false, null, null);
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            List<ViolationDTO> violations = new ArrayList<>();
            JsonNode vioNode = root.path("violations");
            boolean hasViolationSignal = root.has("violations") && vioNode.isArray();
            String explicitModerationResult = readTextCompat(root, "moderation_result", "moderationResult");
            Double explicitOverallConfidence = readDoubleCompat(root, "overall_confidence", "overallConfidence");
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
            return new ParsedResult(
                    violations,
                    summary,
                    hasViolationSignal,
                    explicitModerationResult,
                    explicitOverallConfidence
            );
        } catch (Exception e) {
            log.warn("Failed to parse task result JSON, fallback to empty summary. task result will still be returned safely. error={}", e.getMessage());
            return new ParsedResult(new ArrayList<>(), new VideoSummaryDTO(), false, null, null);
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
                    .policyId(task.getPolicyId())
                    .promptModules(task.getPromptModules())
                    .overallConfidence(task.getOverallConfidence())
                    .createdAt(task.getCreatedAt())
                    .updatedAt(task.getUpdatedAt())
                    .completedAt(task.getCompletedAt())
                    .errorMessage(task.getErrorMessage())
                    .moderationResult(task.getModerationResult());

            if (isCompleted(task.getStatus()) && task.getResultJson() != null) {
                ParsedResult parsed = parseResultJson(task.getResultJson());
                builder.violations(parsed.violations).summary(parsed.summary);
                if (!parsed.hasViolationSignal && parsed.explicitModerationResult == null) {
                    builder.moderationResult(null);
                }
                if (task.getModerationResult() == null || task.getModerationResult().isBlank()) {
                    builder.moderationResult(deriveModerationResult(parsed.violations, parsed.hasViolationSignal));
                }
                if (!parsed.hasViolationSignal
                        && parsed.explicitOverallConfidence == null
                        && parsed.summary.getOverallConfidence() == null) {
                    builder.overallConfidence(null);
                }
                if (task.getOverallConfidence() == null) {
                    builder.overallConfidence(deriveOverallConfidence(parsed.violations, parsed.hasViolationSignal));
                }
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
                    .policyId(task.getPolicyId())
                    .promptModules(task.getPromptModules())
                    .overallConfidence(task.getOverallConfidence())
                    .createdAt(task.getCreatedAt())
                    .updatedAt(task.getUpdatedAt())
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
