package com.moderation.skillos.planner;

import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.ExecutionPlanStep;
import com.moderation.skillos.model.ExecutionState;
import com.moderation.skillos.model.PolicyDefinition;
import com.moderation.skillos.model.SkillResult;

import java.util.Map;
import java.util.Optional;

public interface Replanner {
    Optional<ExecutionPlan> replan(
            ExecutionPlan plan,
            PolicyDefinition policy,
            Map<String, Object> input,
            ExecutionState state,
            ExecutionPlanStep failedStep,
            SkillResult failedResult
    );
}
