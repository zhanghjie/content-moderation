package com.moderation.skillos.planner;

import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.ExecutionPlanStep;
import com.moderation.skillos.model.ExecutionState;
import com.moderation.skillos.model.PolicyDefinition;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.model.SkillResult;
import com.moderation.skillos.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DefaultReplanner implements Replanner {
    private final SkillRegistry skillRegistry;

    @Override
    public Optional<ExecutionPlan> replan(
            ExecutionPlan plan,
            PolicyDefinition policy,
            Map<String, Object> input,
            ExecutionState state,
            ExecutionPlanStep failedStep,
            SkillResult failedResult
    ) {
        Map<String, Object> config = policy.getConfig() == null ? new LinkedHashMap<>() : policy.getConfig();
        Map<String, Object> replanConfig = asMap(config.get("replan"));
        if (replanConfig.isEmpty() && !asBoolean(config.get("replanEnabled"))) {
            return Optional.empty();
        }

        if (failedStep == null) {
            return Optional.empty();
        }

        String failedSkillId = failedStep.getSkillId();
        String replacementSkillId = resolveReplacementSkillId(replanConfig, failedSkillId);
        if (replacementSkillId == null || replacementSkillId.isBlank() || replacementSkillId.equals(failedSkillId)) {
            return Optional.empty();
        }
        if (!skillExists(replacementSkillId)) {
            return Optional.empty();
        }

        List<ExecutionPlanStep> updatedSteps = new ArrayList<>();
        for (ExecutionPlanStep step : plan.getSteps()) {
            ExecutionPlanStep copy = copyStep(step);
            if (failedStep.getStepId() != null && failedStep.getStepId().equals(step.getStepId())) {
                SkillDefinition replacementSkill = skillRegistry.get(replacementSkillId);
                copy.setSkillId(replacementSkillId);
                copy.setSkillSnapshot(buildSkillSnapshot(replacementSkillId, replacementSkill));
                copy.setOnFailure("STOP");
            }
            updatedSteps.add(copy);
        }

        Map<String, Object> plannerMeta = new LinkedHashMap<>();
        if (plan.getPlannerMeta() != null) {
            plannerMeta.putAll(plan.getPlannerMeta());
        }
        plannerMeta.put("replanned", true);
        plannerMeta.put("replannedFromPlanId", plan.getPlanId());
        plannerMeta.put("replannedFromExecutionId", plan.getExecutionId());
        plannerMeta.put("replannedFromStepId", failedStep.getStepId());
        plannerMeta.put("replannedFromSkillId", failedSkillId);
        if (failedResult != null) {
            plannerMeta.put("replanErrorMessage", failedResult.getMessage());
        }

        List<PlannerDecision> decisions = new ArrayList<>();
        if (plan.getPlannerDecisions() != null) {
            decisions.addAll(plan.getPlannerDecisions());
        }
        decisions.add(PlannerDecision.builder()
                .type("replace_skill")
                .stepId(failedStep.getStepId())
                .candidates(List.of(failedSkillId, replacementSkillId))
                .chosen(replacementSkillId)
                .reason("Runtime Replanner 根据失败结果替换技能")
                .metadata(Map.of(
                        "failedSkillId", failedSkillId,
                        "replacementSkillId", replacementSkillId
                ))
                .build());

        ExecutionPlan replanned = ExecutionPlan.builder()
                .planId("plan_" + UUID.randomUUID().toString().replace("-", ""))
                .executionId("exec_" + UUID.randomUUID().toString().replace("-", ""))
                .policyId(plan.getPolicyId())
                .version(plan.getVersion())
                .planType("REPLAN")
                .generatedAt(OffsetDateTime.now())
                .policySnapshot(plan.getPolicySnapshot() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(plan.getPolicySnapshot()))
                .plannerMeta(plannerMeta)
                .plannerDecisions(decisions)
                .steps(updatedSteps)
                .build();
        return Optional.of(replanned);
    }

    private String resolveReplacementSkillId(Map<String, Object> replanConfig, String failedSkillId) {
        Object overrideObj = replanConfig.get("skillOverrides");
        if (overrideObj instanceof Map<?, ?> overrideMap) {
            Object candidate = overrideMap.get(failedSkillId);
            if (candidate != null) {
                return String.valueOf(candidate).trim();
            }
        }
        Object altObj = replanConfig.get("alternatives");
        if (altObj instanceof Map<?, ?> altMap) {
            Object candidate = altMap.get(failedSkillId);
            if (candidate instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null && skillExists(String.valueOf(item).trim())) {
                        return String.valueOf(item).trim();
                    }
                }
            }
            if (candidate instanceof String text && skillExists(text.trim())) {
                return text.trim();
            }
        }
        Object fallback = replanConfig.get("fallbackSkillId");
        if (fallback instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return null;
    }

    private boolean skillExists(String skillId) {
        try {
            return skillRegistry.get(skillId) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private ExecutionPlanStep copyStep(ExecutionPlanStep step) {
        if (step == null) {
            return null;
        }
        return ExecutionPlanStep.builder()
                .stepId(step.getStepId())
                .stepOrder(step.getStepOrder())
                .skillId(step.getSkillId())
                .skillSnapshot(step.getSkillSnapshot() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(step.getSkillSnapshot()))
                .mode(step.getMode())
                .timeoutMs(step.getTimeoutMs())
                .retryPolicy(step.getRetryPolicy() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(step.getRetryPolicy()))
                .dependsOn(step.getDependsOn() == null ? List.of() : new ArrayList<>(step.getDependsOn()))
                .inputBindings(step.getInputBindings() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(step.getInputBindings()))
                .outputBindings(step.getOutputBindings() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(step.getOutputBindings()))
                .onFailure(step.getOnFailure())
                .build();
    }

    private Map<String, Object> buildSkillSnapshot(String skillId, SkillDefinition skill) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("skillId", skillId);
        if (skill == null) {
            snapshot.put("missing", true);
            return snapshot;
        }
        snapshot.put("name", skill.getName());
        snapshot.put("type", skill.getType());
        snapshot.put("description", skill.getDescription());
        snapshot.put("tags", skill.getTags() == null ? List.of() : new ArrayList<>(skill.getTags()));
        snapshot.put("outputSchema", skill.getOutputSchema() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(skill.getOutputSchema()));
        snapshot.put("stateMapping", skill.getStateMapping() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(skill.getStateMapping()));
        snapshot.put("executionConfig", skill.getExecutionConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(skill.getExecutionConfig()));
        snapshot.put("scriptConfig", skill.getScriptConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(skill.getScriptConfig()));
        snapshot.put("status", skill.getStatus());
        snapshot.put("timeoutMs", skill.getTimeoutMs());
        snapshot.put("version", skill.getVersion());
        return snapshot;
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return "true".equalsIgnoreCase(text.trim());
        }
        return false;
    }
}
