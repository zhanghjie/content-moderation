package com.moderation.skillos.engine;

import com.moderation.skillos.executor.SkillExecutor;
import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.ExecutionPlanStep;
import com.moderation.skillos.model.ExecutionState;
import com.moderation.skillos.model.PolicyDefinition;
import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.model.SkillExecutionTrace;
import com.moderation.skillos.model.SkillResult;
import com.moderation.skillos.model.StepStatus;
import com.moderation.skillos.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DefaultPlanExecutor implements PlanExecutor {
    private final SkillRegistry skillRegistry;
    private final ApplicationContext applicationContext;

    @Override
    public PlanExecuteResult execute(ExecutionPlan plan, PolicyDefinition policy, Map<String, Object> input) {
        long start = System.currentTimeMillis();
        List<SkillExecutionTrace> traces = new ArrayList<>();
        ExecutionState state = new ExecutionState();
        Map<String, Object> safeInput = input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
        state.put("input", safeInput);
        state.put("executionId", plan.getExecutionId());
        state.put("planId", plan.getPlanId());
        boolean success = true;
        String errorMessage = null;
        String executionStatus = "RUNNING";
        try {
            for (ExecutionPlanStep step : plan.getSteps()) {
                String skillId = step.getSkillId();
                if (shouldSkip(policy, skillId, state.getData())) {
                    traces.add(SkillExecutionTrace.builder()
                            .traceId(newTraceId())
                            .stepId(step.getStepId())
                            .status(StepStatus.SKIPPED.name())
                            .skillId(skillId)
                            .attempt(1)
                            .success(true)
                            .skipped(true)
                            .durationMs(0L)
                            .message("根据 policy config 跳过")
                            .build());
                    continue;
                }
                SkillDefinition skill = skillRegistry.get(skillId);
                SkillExecutor executor = (SkillExecutor) applicationContext.getBean(resolveExecutorBean(skill));
                SkillContext context = new SkillContext();
                context.setPolicyId(policy.getPolicyId());
                context.setSkillId(skillId);
                context.setState(state.getData());
                context.setInput(safeInput);
                context.setPolicyConfig(policy.getConfig());
                OffsetDateTime startedAt = OffsetDateTime.now();
                long skillStart = System.currentTimeMillis();
                SkillResult result = executor.execute(context);
                long skillDuration = System.currentTimeMillis() - skillStart;
                OffsetDateTime endedAt = OffsetDateTime.now();
                if (result == null || !result.isSuccess()) {
                    success = false;
                    errorMessage = result == null ? "Skill 返回为空" : result.getMessage();
                    traces.add(SkillExecutionTrace.builder()
                            .traceId(newTraceId())
                            .stepId(step.getStepId())
                            .status(StepStatus.FAILED.name())
                            .skillId(skillId)
                            .attempt(1)
                            .success(false)
                            .skipped(false)
                            .durationMs(skillDuration)
                            .startedAt(startedAt)
                            .endedAt(endedAt)
                            .input(safeInput)
                            .message(errorMessage)
                            .build());
                    break;
                }
                Map<String, Object> outputData = normalizeOutput(result.getOutput());
                state.put(skillId, outputData);
                applyStateMapping(skill, outputData, state.getData());
                applyWriteToState(outputData, state.getData());
                state.put("finalResult", outputData);
                traces.add(SkillExecutionTrace.builder()
                        .traceId(newTraceId())
                        .stepId(step.getStepId())
                        .status(StepStatus.SUCCESS.name())
                        .skillId(skillId)
                        .attempt(1)
                        .success(true)
                        .skipped(false)
                        .durationMs(skillDuration)
                        .startedAt(startedAt)
                        .endedAt(endedAt)
                        .input(safeInput)
                        .output(result.getOutput())
                        .message(result.getMessage())
                        .build());
            }
            executionStatus = success ? "SUCCEEDED" : "FAILED";
        } catch (Exception ex) {
            success = false;
            executionStatus = "FAILED";
            errorMessage = ex.getMessage();
        }
        long duration = System.currentTimeMillis() - start;
        state.put("policyId", policy.getPolicyId());
        state.put("success", success);
        state.put("durationMs", duration);
        if (errorMessage != null) {
            state.put("errorMessage", errorMessage);
        }
        return PlanExecuteResult.builder()
                .success(success)
                .durationMs(duration)
                .state(state)
                .traces(traces)
                .errorMessage(errorMessage)
                .executionStatus(executionStatus)
                .build();
    }

    private void applyStateMapping(SkillDefinition skill, Map<String, Object> output, Map<String, Object> state) {
        if (skill == null || skill.getStateMapping() == null || skill.getStateMapping().isEmpty()) {
            return;
        }
        Object writeToStateObj = skill.getStateMapping().get("write_to_state");
        if (!(writeToStateObj instanceof Map<?, ?> writeToStateMap) || writeToStateMap.isEmpty()) {
            return;
        }
        for (Map.Entry<?, ?> entry : writeToStateMap.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String outputKey = String.valueOf(entry.getKey());
            Object targetPathObj = entry.getValue();
            if (targetPathObj == null) {
                continue;
            }
            String targetPath = String.valueOf(targetPathObj).trim();
            if (targetPath.isBlank()) {
                continue;
            }
            if (output.containsKey(outputKey)) {
                putStateValue(state, normalizeStatePath(targetPath), output.get(outputKey));
            }
        }
    }

    private void applyWriteToState(Map<String, Object> output, Map<String, Object> state) {
        Object writeToStateObj = output.get("writeToState");
        if (!(writeToStateObj instanceof Map<?, ?> writeToStateMap) || writeToStateMap.isEmpty()) {
            return;
        }
        for (Map.Entry<?, ?> entry : writeToStateMap.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String targetPath = String.valueOf(entry.getKey()).trim();
            if (targetPath.isBlank()) {
                continue;
            }
            putStateValue(state, normalizeStatePath(targetPath), entry.getValue());
        }
    }

    private Map<String, Object> normalizeOutput(Object output) {
        if (output instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return normalized;
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("value", output);
        return normalized;
    }

    private String normalizeStatePath(String path) {
        String normalized = path == null ? "" : path.trim();
        if (normalized.startsWith("state.")) {
            normalized = normalized.substring("state.".length());
        } else if ("state".equalsIgnoreCase(normalized)) {
            normalized = "";
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private void putStateValue(Map<String, Object> state, String path, Object value) {
        if (state == null || path == null || path.isBlank()) {
            return;
        }
        String[] parts = path.split("\\.");
        Map<String, Object> current = state;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isBlank()) {
                continue;
            }
            if (i == parts.length - 1) {
                current.put(part, value);
                return;
            }
            Object next = current.get(part);
            if (!(next instanceof Map<?, ?>)) {
                Map<String, Object> nested = new LinkedHashMap<>();
                current.put(part, nested);
                current = nested;
                continue;
            }
            current = (Map<String, Object>) next;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean shouldSkip(PolicyDefinition policy, String skillId, Map<String, Object> state) {
        Map<String, Object> config = policy.getConfig();
        if (config == null) {
            return false;
        }
        Object disabledSkills = config.get("disabledSkills");
        if (disabledSkills instanceof List<?> list && list.contains(skillId)) {
            return true;
        }
        Object switchesObj = config.get("skillSwitches");
        if (switchesObj instanceof Map<?, ?> switchesMap) {
            Object switchValue = switchesMap.get(skillId);
            if (switchValue instanceof Boolean bool && !bool) {
                return true;
            }
            if (switchValue instanceof String text && "false".equalsIgnoreCase(text)) {
                return true;
            }
        }
        Object stateRuleObj = config.get("skipIfStateMissing");
        if (stateRuleObj instanceof Map<?, ?> stateRuleMap) {
            Object keysObj = stateRuleMap.get(skillId);
            if (keysObj instanceof List<?> keys) {
                for (Object key : keys) {
                    if (key != null && !state.containsKey(String.valueOf(key))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String newTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveExecutorBean(SkillDefinition skill) {
        Map<String, Object> executionConfig = skill.getExecutionConfig();
        Object modeValue = executionConfig == null ? null : executionConfig.get("execution_mode");
        if ("LLM".equalsIgnoreCase(String.valueOf(modeValue))) {
            return "llmSkillExecutor";
        }
        return skill.getExecutorBean();
    }
}
