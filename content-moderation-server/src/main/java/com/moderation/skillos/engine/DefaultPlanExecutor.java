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
                state.put(skillId, result.getOutput());
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
