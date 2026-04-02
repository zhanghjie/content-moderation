package com.moderation.skillos.planner;

import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.ExecutionPlanStep;
import com.moderation.skillos.model.PolicyDefinition;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DefaultDynamicPolicyPlanner implements DynamicPolicyPlanner {
    private final SkillRegistry skillRegistry;

    @Override
    public ExecutionPlan refine(ExecutionPlan plan, PolicyDefinition policy, Map<String, Object> input) {
        if (plan == null) {
            return null;
        }
        Map<String, Object> config = policy.getConfig() == null ? new LinkedHashMap<>() : policy.getConfig();
        Map<String, Object> dynamicConfig = asMap(config.get("dynamicPlanner"));
        boolean enabled = asBoolean(dynamicConfig.get("enabled")) || asBoolean(config.get("dynamicPlannerEnabled"));
        if (!enabled) {
            if (plan.getPlanType() == null || plan.getPlanType().isBlank()) {
                plan.setPlanType("STATIC");
            }
            ensureMeta(plan).put("dynamicPlannerApplied", false);
            ensureMeta(plan).put("inputKinds", detectInputKinds(input));
            ensureMeta(plan).put("videoAware", hasVideoUrl(input));
            return plan;
        }

        Map<String, Object> candidateMap = asMap(dynamicConfig.get("candidateSkills"));
        Map<String, Object> stepSkillMap = asMap(dynamicConfig.get("stepSkillMap"));
        List<PlannerDecision> decisions = new ArrayList<>();
        List<ExecutionPlanStep> steps = plan.getSteps() == null ? new ArrayList<>() : new ArrayList<>(plan.getSteps());
        boolean videoAware = hasVideoUrl(input);

        for (int i = 0; i < steps.size(); i++) {
            ExecutionPlanStep step = steps.get(i);
            String stepId = step.getStepId();
            String originalSkillId = step.getSkillId();
            String chosenSkillId = originalSkillId;
            List<String> candidates = new ArrayList<>();

            Object stepOverride = stepSkillMap.get(stepId);
            if (stepOverride != null) {
                candidates.add(String.valueOf(stepOverride));
                chosenSkillId = String.valueOf(stepOverride);
            }

            Object candidateObj = candidateMap.get(stepId);
            if (candidateObj instanceof List<?> list && !list.isEmpty()) {
                for (Object item : list) {
                    if (item == null) {
                        continue;
                    }
                    String candidateId = String.valueOf(item).trim();
                    if (candidateId.isBlank()) {
                        continue;
                    }
                    candidates.add(candidateId);
                    if (skillExists(candidateId)) {
                        chosenSkillId = candidateId;
                        break;
                    }
                }
            }

            if (candidates.isEmpty()) {
                candidates.add(originalSkillId);
            }

            if (!chosenSkillId.equals(originalSkillId) && skillExists(chosenSkillId)) {
                SkillDefinition chosenSkill = skillRegistry.get(chosenSkillId);
                step.setSkillId(chosenSkillId);
                step.setSkillSnapshot(buildSkillSnapshot(chosenSkillId, chosenSkill));
            }

            decisions.add(PlannerDecision.builder()
                    .type("select_skill")
                    .stepId(stepId)
                    .candidates(candidates)
                    .chosen(chosenSkillId)
                    .reason(chosenSkillId.equals(originalSkillId)
                            ? "Dynamic Planner 未启用候选替换，沿用静态编译结果"
                            : "Dynamic Planner 在候选技能中完成受限选择")
                    .metadata(Map.of(
                            "stepOrder", step.getStepOrder(),
                            "originalSkillId", originalSkillId,
                            "dynamicEnabled", true
                    ))
                    .build());
        }

        plan.setSteps(steps);
        plan.setPlanType("DYNAMIC");
        plan.setPlannerDecisions(decisions);
        ensureMeta(plan).put("dynamicPlannerApplied", true);
        ensureMeta(plan).put("dynamicPlannerStrategy", "CONSTRAINED_RULE_BASED");
        ensureMeta(plan).put("videoAware", videoAware);
        ensureMeta(plan).put("inputKinds", detectInputKinds(input));
        ensureMeta(plan).put("runtimePlan", buildRuntimePlan(steps, videoAware));
        return plan;
    }

    private boolean skillExists(String skillId) {
        try {
            return skillRegistry.get(skillId) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private Map<String, Object> ensureMeta(ExecutionPlan plan) {
        if (plan.getPlannerMeta() == null) {
            plan.setPlannerMeta(new LinkedHashMap<>());
        }
        return plan.getPlannerMeta();
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

    private boolean hasVideoUrl(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        Object videoUrl = input.get("videoUrl");
        return videoUrl != null && !String.valueOf(videoUrl).isBlank();
    }

    private List<String> detectInputKinds(Map<String, Object> input) {
        List<String> kinds = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return kinds;
        }
        Object videoUrl = input.get("videoUrl");
        if (videoUrl != null && !String.valueOf(videoUrl).isBlank()) {
            kinds.add("VIDEO");
        }
        Object coverUrl = input.get("coverUrl");
        if (coverUrl != null && !String.valueOf(coverUrl).isBlank()) {
            kinds.add("IMAGE");
        }
        Object transcript = input.get("transcript");
        if (transcript != null && !String.valueOf(transcript).isBlank()) {
            kinds.add("TEXT");
        }
        if (kinds.isEmpty()) {
            kinds.add("GENERIC");
        }
        return kinds;
    }

    private Map<String, Object> buildRuntimePlan(List<ExecutionPlanStep> steps, boolean videoAware) {
        Map<String, Object> runtimePlan = new LinkedHashMap<>();
        List<Map<String, Object>> fusionGroups = new ArrayList<>();
        List<String> stateBreakpoints = new ArrayList<>();
        Map<String, String> traceLevel = new LinkedHashMap<>();
        List<String> replanPoints = new ArrayList<>();

        if (steps == null || steps.isEmpty()) {
            runtimePlan.put("enabled", false);
            runtimePlan.put("fusionGroups", fusionGroups);
            runtimePlan.put("stateBreakpoints", stateBreakpoints);
            runtimePlan.put("traceLevel", traceLevel);
            runtimePlan.put("replanPoints", replanPoints);
            return runtimePlan;
        }

        if (videoAware && steps.size() >= 2) {
            ExecutionPlanStep first = steps.get(0);
            ExecutionPlanStep second = steps.get(1);
            fusionGroups.add(Map.of(
                    "skills", List.of(first.getStepId(), second.getStepId()),
                    "mode", "mergeable_llm_call",
                    "reason", "video input detected, compact runtime plan prepared"
            ));
            traceLevel.put(first.getStepId(), "none");
            traceLevel.put(second.getStepId(), "full");
            stateBreakpoints.add(second.getStepId());
            replanPoints.add(second.getStepId());
        }

        for (ExecutionPlanStep step : steps) {
            traceLevel.putIfAbsent(step.getStepId(), videoAware ? "full" : "standard");
            Object skillType = step.getSkillSnapshot() == null ? null : step.getSkillSnapshot().get("type");
            String type = skillType == null ? "" : String.valueOf(skillType).trim().toUpperCase();
            if ("SEMANTIC".equals(type) || "OUTPUT".equals(type)) {
                if (!stateBreakpoints.contains(step.getStepId())) {
                    stateBreakpoints.add(step.getStepId());
                }
                if (!replanPoints.contains(step.getStepId())) {
                    replanPoints.add(step.getStepId());
                }
            }
        }

        runtimePlan.put("enabled", true);
        runtimePlan.put("inputVideoAware", videoAware);
        runtimePlan.put("fusionGroups", fusionGroups);
        runtimePlan.put("stateBreakpoints", stateBreakpoints);
        runtimePlan.put("traceLevel", traceLevel);
        runtimePlan.put("replanPoints", replanPoints);
        return runtimePlan;
    }
}
