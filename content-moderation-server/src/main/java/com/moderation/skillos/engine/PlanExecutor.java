package com.moderation.skillos.engine;

import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.PolicyDefinition;

import java.util.Map;

public interface PlanExecutor {
    PlanExecuteResult execute(ExecutionPlan plan, PolicyDefinition policy, Map<String, Object> input);
}
