package com.moderation.skillos.engine;

import com.moderation.skillos.model.ExecutionState;
import com.moderation.skillos.model.ExecutionPlan;
import com.moderation.skillos.model.SkillExecutionTrace;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PolicyExecuteResult {
    private String executionId;
    private String planId;
    private String policyId;
    private String status;
    private boolean success;
    private long durationMs;
    private ExecutionPlan plan;
    private ExecutionState state;
    private List<SkillExecutionTrace> traces;
    private String errorMessage;
}
