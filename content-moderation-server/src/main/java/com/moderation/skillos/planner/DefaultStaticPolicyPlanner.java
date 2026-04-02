package com.moderation.skillos.planner;

import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.ExecutionPlanStep;
import com.moderation.skillos.model.PolicyDefinition;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DefaultStaticPolicyPlanner implements StaticPolicyPlanner {
    private final SkillRegistry skillRegistry;

    @Override
    public ExecutionPlan plan(PolicyDefinition policy, Map<String, Object> input) {
        String executionId = "exec_" + UUID.randomUUID().toString().replace("-", "");
        String planId = "plan_" + UUID.randomUUID().toString().replace("-", "");
        List<ExecutionPlanStep> steps = new ArrayList<>();
        List<PlannerDecision> decisions = new ArrayList<>();
        List<Map<String, Object>> logicalPlan = new ArrayList<>();
        List<Map<String, Object>> executionGraphNodes = new ArrayList<>();
        List<Map<String, Object>> executionGraphEdges = new ArrayList<>();
        List<String> pipeline = policy.getSkillPipeline() == null ? List.of() : policy.getSkillPipeline();
        boolean videoAware = hasVideoUrl(input);
        for (int i = 0; i < pipeline.size(); i++) {
            String skillId = pipeline.get(i);
            SkillDefinition skill = null;
            try {
                skill = skillRegistry.get(skillId);
            } catch (Exception ignored) {
                // 保持执行计划生成不中断，详情页将退化为原始 JSON 展示
            }
            steps.add(ExecutionPlanStep.builder()
                    .stepId("step_" + (i + 1))
                    .stepOrder(i + 1)
                    .skillId(skillId)
                    .skillSnapshot(buildSkillSnapshot(skillId, skill))
                    .mode("SKILL")
                    .timeoutMs(3000)
                    .retryPolicy(Map.of("maxAttempts", 1))
                    .dependsOn(i == 0 ? List.of() : List.of("step_" + i))
                    .inputBindings(Map.of("source", "state"))
                    .outputBindings(Map.of("target", skillId))
                    .onFailure("STOP")
                    .build());

            Map<String, Object> logicalNode = new LinkedHashMap<>();
            logicalNode.put("stepId", "step_" + (i + 1));
            logicalNode.put("skillId", skillId);
            logicalNode.put("stepOrder", i + 1);
            logicalNode.put("skillType", skill == null ? "UNKNOWN" : skill.getType());
            logicalNode.put("intent", inferIntent(skill));
            logicalPlan.add(logicalNode);

            Map<String, Object> graphNode = new LinkedHashMap<>();
            graphNode.put("id", "step_" + (i + 1));
            graphNode.put("skillId", skillId);
            graphNode.put("type", skill == null ? "UNKNOWN" : skill.getType());
            graphNode.put("order", i + 1);
            executionGraphNodes.add(graphNode);

            if (i > 0) {
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("from", "step_" + i);
                edge.put("to", "step_" + (i + 1));
                edge.put("kind", "sequential");
                executionGraphEdges.add(edge);
            }

            decisions.add(PlannerDecision.builder()
                    .type("select_skill")
                    .stepId("step_" + (i + 1))
                    .candidates(List.of(skillId))
                    .chosen(skillId)
                    .reason("Static Planner 固定编译 Policy skillPipeline")
                    .metadata(Map.of("stepOrder", i + 1))
                    .build());
        }
        
        Map<String, Object> plannerMeta = new LinkedHashMap<>();
        plannerMeta.put("planLayer", "STATIC");
        plannerMeta.put("pipelineSize", pipeline.size());
        plannerMeta.put("inputKeys", input == null ? List.of() : input.keySet());
        plannerMeta.put("inputKinds", detectInputKinds(input));
        plannerMeta.put("videoAware", videoAware);
        plannerMeta.put("logicalPlan", logicalPlan);
        plannerMeta.put("runtimePlan", buildRuntimePlan(steps, input, videoAware));
        plannerMeta.put("executionGraph", Map.of(
                "nodes", executionGraphNodes,
                "edges", executionGraphEdges
        ));
        plannerMeta.put("generatedBy", "DefaultStaticPolicyPlanner");
        return ExecutionPlan.builder()
                .planId(planId)
                .executionId(executionId)
                .policyId(policy.getPolicyId())
                .version(policy.getVersion())
                .planType("STATIC")
                .generatedAt(OffsetDateTime.now())
                .policySnapshot(buildPolicySnapshot(policy))
                .plannerMeta(plannerMeta)
                .plannerDecisions(decisions)
                .steps(steps)
                .build();
    }

    private Map<String, Object> buildPolicySnapshot(PolicyDefinition policy) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("policyId", policy.getPolicyId());
        snapshot.put("name", policy.getName());
        snapshot.put("version", policy.getVersion());
        snapshot.put("skillPipeline", policy.getSkillPipeline() == null ? List.of() : new ArrayList<>(policy.getSkillPipeline()));
        snapshot.put("config", policy.getConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(policy.getConfig()));
        snapshot.put("executionInput", policy.getExecutionInput() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(policy.getExecutionInput()));
        return snapshot;
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

    private String inferIntent(SkillDefinition skill) {
        if (skill == null || skill.getType() == null) {
            return "generic";
        }
        String type = skill.getType().trim().toUpperCase();
        return switch (type) {
            case "PERCEPTION" -> "observe";
            case "SEMANTIC" -> "understand";
            case "DECISION" -> "decide";
            case "GUARD" -> "guard";
            case "OUTPUT" -> "summarize";
            default -> "generic";
        };
    }

    private Map<String, Object> buildRuntimePlan(List<ExecutionPlanStep> steps, Map<String, Object> input, boolean videoAware) {
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

        boolean hasVideo = videoAware || hasVideoUrl(input);
        if (hasVideo && steps.size() >= 2) {
            ExecutionPlanStep first = steps.get(0);
            ExecutionPlanStep second = steps.get(1);
            fusionGroups.add(Map.of(
                    "skills", List.of(first.getStepId(), second.getStepId()),
                    "mode", "mergeable_llm_call",
                    "reason", "video input detected, perception+semantic can be compacted at planning time"
            ));
            traceLevel.put(first.getStepId(), "none");
            traceLevel.put(second.getStepId(), "full");
            stateBreakpoints.add(second.getStepId());
            replanPoints.add(second.getStepId());
        }

        for (int i = 0; i < steps.size(); i++) {
            ExecutionPlanStep step = steps.get(i);
            if (!traceLevel.containsKey(step.getStepId())) {
                traceLevel.put(step.getStepId(), hasVideo ? "full" : "standard");
            }
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
        runtimePlan.put("inputVideoAware", hasVideo);
        runtimePlan.put("fusionGroups", fusionGroups);
        runtimePlan.put("stateBreakpoints", stateBreakpoints);
        runtimePlan.put("traceLevel", traceLevel);
        runtimePlan.put("replanPoints", replanPoints);
        return runtimePlan;
    }
}
