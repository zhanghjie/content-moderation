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
        List<String> pipeline = policy.getSkillPipeline() == null ? List.of() : policy.getSkillPipeline();
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
        plannerMeta.put("pipelineSize", steps.size());
        plannerMeta.put("inputKeys", input == null ? List.of() : input.keySet());
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
}
