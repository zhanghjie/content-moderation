package com.moderation.skillos.planner;

import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.ExecutionPlanStep;
import com.moderation.skillos.model.PolicyDefinition;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DefaultPolicyPlanner implements PolicyPlanner {
    @Override
    public ExecutionPlan plan(PolicyDefinition policy, Map<String, Object> input) {
        String executionId = "exec_" + UUID.randomUUID().toString().replace("-", "");
        String planId = "plan_" + UUID.randomUUID().toString().replace("-", "");
        List<ExecutionPlanStep> steps = new ArrayList<>();
        List<String> pipeline = policy.getSkillPipeline() == null ? List.of() : policy.getSkillPipeline();
        for (int i = 0; i < pipeline.size(); i++) {
            String skillId = pipeline.get(i);
            steps.add(ExecutionPlanStep.builder()
                    .stepId("step_" + (i + 1))
                    .stepOrder(i + 1)
                    .skillId(skillId)
                    .mode("SKILL")
                    .timeoutMs(3000)
                    .retryPolicy(Map.of("maxAttempts", 1))
                    .dependsOn(i == 0 ? List.of() : List.of("step_" + i))
                    .inputBindings(Map.of("source", "state"))
                    .outputBindings(Map.of("target", skillId))
                    .onFailure("STOP")
                    .build());
        }
        Map<String, Object> plannerMeta = new LinkedHashMap<>();
        plannerMeta.put("pipelineSize", steps.size());
        plannerMeta.put("inputKeys", input == null ? List.of() : input.keySet());
        plannerMeta.put("generatedBy", "DefaultPolicyPlanner");
        return ExecutionPlan.builder()
                .planId(planId)
                .executionId(executionId)
                .policyId(policy.getPolicyId())
                .version(policy.getVersion())
                .generatedAt(OffsetDateTime.now())
                .plannerMeta(plannerMeta)
                .steps(steps)
                .build();
    }
}
