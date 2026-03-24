package com.moderation.skillos.engine;

import com.moderation.skillos.model.ExecutionState;
import com.moderation.skillos.model.SkillExecutionTrace;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlanExecuteResult {
    private boolean success;
    private long durationMs;
    private ExecutionState state;
    private List<SkillExecutionTrace> traces;
    private String errorMessage;
    private String executionStatus;
}
