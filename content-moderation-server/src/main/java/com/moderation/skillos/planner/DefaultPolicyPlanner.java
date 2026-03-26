package com.moderation.skillos.planner;

import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.PolicyDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DefaultPolicyPlanner implements PolicyPlanner {
    private final StaticPolicyPlanner staticPolicyPlanner;
    private final DynamicPolicyPlanner dynamicPolicyPlanner;

    @Override
    public ExecutionPlan plan(PolicyDefinition policy, Map<String, Object> input) {
        ExecutionPlan staticPlan = staticPolicyPlanner.plan(policy, input);
        return dynamicPolicyPlanner.refine(staticPlan, policy, input);
    }
}
