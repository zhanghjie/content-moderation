package com.moderation.skillos.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.entity.PolicyExecutionEntity;
import com.moderation.entity.PolicyExecutionFeedbackEntity;
import com.moderation.entity.PolicyExecutionStepEntity;
import com.moderation.mapper.PolicyExecutionFeedbackMapper;
import com.moderation.mapper.PolicyExecutionMapper;
import com.moderation.mapper.PolicyExecutionStepMapper;
import com.moderation.model.req.ExecutionFeedbackReq;
import com.moderation.model.res.PolicyExecuteRes;
import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.ExecutionPlanStep;
import com.moderation.skillos.model.PolicyDefinition;
import com.moderation.skillos.model.SkillResult;
import com.moderation.skillos.model.SkillExecutionTrace;
import com.moderation.skillos.planner.PolicyPlanner;
import com.moderation.skillos.planner.Replanner;
import com.moderation.skillos.registry.PolicyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyExecutionEngine {
    private final PolicyRegistry policyRegistry;
    private final PolicyPlanner policyPlanner;
    private final PlanExecutor planExecutor;
    private final Replanner replanner;
    private final PolicyExecutionMapper policyExecutionMapper;
    private final PolicyExecutionStepMapper policyExecutionStepMapper;
    private final PolicyExecutionFeedbackMapper policyExecutionFeedbackMapper;
    private final ObjectMapper objectMapper;

    public PolicyExecuteResult execute(String policyId, Map<String, Object> input) {
        PolicyDefinition policy = policyRegistry.get(policyId);
        Map<String, Object> safeInput = input;
        if (safeInput == null || safeInput.isEmpty()) {
            safeInput = policy.getExecutionInput();
        }
        if (safeInput == null) {
            safeInput = new LinkedHashMap<>();
        } else {
            safeInput = new LinkedHashMap<>(safeInput);
        }
        ExecutionPlan plan = policyPlanner.plan(policy, safeInput);
        PlanExecuteResult planResult = planExecutor.execute(plan, policy, safeInput);
        if (!planResult.isSuccess()) {
            Optional<ExecutionPlan> replanned = tryReplan(plan, policy, safeInput, planResult);
            if (replanned.isPresent()) {
                plan = replanned.get();
                planResult = planExecutor.execute(plan, policy, safeInput);
            }
        }
        persistExecution(plan, planResult);
        persistAutoFeedback(plan.getExecutionId(), planResult);

        return PolicyExecuteResult.builder()
                .executionId(plan.getExecutionId())
                .planId(plan.getPlanId())
                .policyId(policyId)
                .status(planResult.getExecutionStatus())
                .success(planResult.isSuccess())
                .durationMs(planResult.getDurationMs())
                .plan(plan)
                .state(planResult.getState())
                .traces(planResult.getTraces())
                .errorMessage(planResult.getErrorMessage())
                .build();
    }

    private Optional<ExecutionPlan> tryReplan(ExecutionPlan plan, PolicyDefinition policy, Map<String, Object> input, PlanExecuteResult result) {
        if (replanner == null || result == null || result.getState() == null) {
            return Optional.empty();
        }
        ExecutionPlanStep failedStep = findFailedStep(plan, result.getTraces());
        if (failedStep == null) {
            return Optional.empty();
        }
        SkillResult failedResult = SkillResult.failed(result.getErrorMessage() == null ? "运行时执行失败" : result.getErrorMessage());
        return replanner.replan(plan, policy, input, result.getState(), failedStep, failedResult);
    }

    private ExecutionPlanStep findFailedStep(ExecutionPlan plan, List<SkillExecutionTrace> traces) {
        if (plan == null || plan.getSteps() == null || traces == null || traces.isEmpty()) {
            return null;
        }
        List<SkillExecutionTrace> reversed = new ArrayList<>(traces);
        for (int i = reversed.size() - 1; i >= 0; i--) {
            SkillExecutionTrace trace = reversed.get(i);
            if (trace == null || trace.isSuccess() || trace.isSkipped()) {
                continue;
            }
            String stepId = trace.getStepId();
            if (stepId == null) {
                continue;
            }
            for (ExecutionPlanStep step : plan.getSteps()) {
                if (step != null && stepId.equals(step.getStepId())) {
                    return step;
                }
            }
        }
        return null;
    }

    public PolicyExecuteRes getExecutionById(String executionId) {
        PolicyExecutionEntity execution = policyExecutionMapper.selectByExecutionId(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("执行记录不存在: " + executionId);
        }
        List<PolicyExecutionStepEntity> steps = policyExecutionStepMapper.selectByExecutionId(executionId);
        return toPolicyExecuteRes(execution, steps);
    }

    public PolicyExecuteRes getLatestExecutionByPolicyId(String policyId) {
        PolicyExecutionEntity execution = policyExecutionMapper.selectLatestByPolicyId(policyId);
        if (execution == null) {
            return null;
        }
        List<PolicyExecutionStepEntity> steps = policyExecutionStepMapper.selectByExecutionId(execution.getExecutionId());
        return toPolicyExecuteRes(execution, steps);
    }

    public List<SkillExecutionTrace> listExecutionTraces(String executionId) {
        return policyExecutionStepMapper.selectByExecutionId(executionId).stream()
                .map(this::toTrace)
                .collect(Collectors.toList());
    }

    public PolicyExecuteRes replayExecution(String executionId) {
        return getExecutionById(executionId);
    }

    public void submitFeedback(String executionId, ExecutionFeedbackReq req) {
        PolicyExecutionEntity execution = policyExecutionMapper.selectByExecutionId(executionId);
        if (execution == null) {
            throw new IllegalArgumentException("执行记录不存在: " + executionId);
        }
        PolicyExecutionFeedbackEntity entity = new PolicyExecutionFeedbackEntity();
        entity.setExecutionId(executionId);
        entity.setTraceId(req.getTraceId());
        entity.setSource("HUMAN");
        entity.setScore(req.getScore());
        entity.setLabel(req.getLabel());
        entity.setAction(req.getAction());
        entity.setComment(req.getComment());
        entity.setMetadataJson(writeJson(req.getMetadata()));
        policyExecutionFeedbackMapper.insert(entity);
    }

    private void persistExecution(ExecutionPlan plan, PlanExecuteResult result) {
        PolicyExecutionEntity executionEntity = new PolicyExecutionEntity();
        executionEntity.setExecutionId(plan.getExecutionId());
        executionEntity.setPlanId(plan.getPlanId());
        executionEntity.setPolicyId(plan.getPolicyId());
        executionEntity.setStatus(result.getExecutionStatus());
        executionEntity.setDurationMs(result.getDurationMs());
        executionEntity.setPlanSnapshotJson(writeJson(plan));
        executionEntity.setStateJson(writeJson(result.getState().getData()));
        executionEntity.setErrorMessage(result.getErrorMessage());
        policyExecutionMapper.insert(executionEntity);
        for (SkillExecutionTrace trace : result.getTraces()) {
            PolicyExecutionStepEntity stepEntity = new PolicyExecutionStepEntity();
            stepEntity.setTraceId(trace.getTraceId());
            stepEntity.setExecutionId(plan.getExecutionId());
            stepEntity.setStepId(trace.getStepId());
            stepEntity.setSkillId(trace.getSkillId());
            stepEntity.setStatus(trace.getStatus());
            stepEntity.setAttempt(trace.getAttempt());
            stepEntity.setDurationMs(trace.getDurationMs());
            stepEntity.setInputJson(writeJson(trace.getInput()));
            stepEntity.setOutputJson(writeJson(trace.getOutput()));
            stepEntity.setErrorMessage(trace.getMessage());
            stepEntity.setStartedAt(trace.getStartedAt());
            stepEntity.setEndedAt(trace.getEndedAt());
            policyExecutionStepMapper.insert(stepEntity);
        }
    }

    private void persistAutoFeedback(String executionId, PlanExecuteResult result) {
        PolicyExecutionFeedbackEntity feedback = new PolicyExecutionFeedbackEntity();
        feedback.setExecutionId(executionId);
        feedback.setSource("AUTO");
        feedback.setScore(result.isSuccess() ? 1.0 : 0.0);
        feedback.setLabel(result.isSuccess() ? "PASS" : "FAIL");
        feedback.setAction(result.isSuccess() ? "KEEP" : "REVIEW");
        feedback.setComment(result.isSuccess() ? "执行成功，自动评估通过" : "执行失败，建议人工复核");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("traceCount", result.getTraces() == null ? 0 : result.getTraces().size());
        metadata.put("durationMs", result.getDurationMs());
        metadata.put("status", result.getExecutionStatus());
        feedback.setMetadataJson(writeJson(metadata));
        policyExecutionFeedbackMapper.insert(feedback);
    }

    private PolicyExecuteRes toPolicyExecuteRes(PolicyExecutionEntity execution, List<PolicyExecutionStepEntity> steps) {
        ExecutionPlan plan = readPlan(execution.getPlanSnapshotJson());
        Map<String, Object> state = readState(execution.getStateJson());
        List<SkillExecutionTrace> traces = steps.stream().map(this::toTrace).collect(Collectors.toList());
        return PolicyExecuteRes.builder()
                .executionId(execution.getExecutionId())
                .planId(execution.getPlanId())
                .policyId(execution.getPolicyId())
                .status(execution.getStatus())
                .success("SUCCEEDED".equalsIgnoreCase(execution.getStatus()))
                .durationMs(execution.getDurationMs() == null ? 0L : execution.getDurationMs())
                .plan(plan)
                .state(state)
                .traces(traces)
                .errorMessage(execution.getErrorMessage())
                .build();
    }

    private SkillExecutionTrace toTrace(PolicyExecutionStepEntity step) {
        return SkillExecutionTrace.builder()
                .traceId(step.getTraceId())
                .stepId(step.getStepId())
                .status(step.getStatus())
                .skillId(step.getSkillId())
                .attempt(step.getAttempt() == null ? 1 : step.getAttempt())
                .success("SUCCESS".equalsIgnoreCase(step.getStatus()) || "SKIPPED".equalsIgnoreCase(step.getStatus()))
                .skipped("SKIPPED".equalsIgnoreCase(step.getStatus()))
                .durationMs(step.getDurationMs() == null ? 0L : step.getDurationMs())
                .startedAt(step.getStartedAt())
                .endedAt(step.getEndedAt())
                .input(readAny(step.getInputJson()))
                .output(readAny(step.getOutputJson()))
                .message(step.getErrorMessage())
                .build();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> readState(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private ExecutionPlan readPlan(String json) {
        try {
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, ExecutionPlan.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Object readAny(String json) {
        try {
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
