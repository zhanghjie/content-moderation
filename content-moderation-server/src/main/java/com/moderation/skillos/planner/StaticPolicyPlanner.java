package com.moderation.skillos.planner;

import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.PolicyDefinition;

import java.util.Map;

public interface StaticPolicyPlanner {
    ExecutionPlan plan(PolicyDefinition policy, Map<String, Object> input);
}
