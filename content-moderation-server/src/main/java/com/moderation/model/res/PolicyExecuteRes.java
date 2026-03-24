package com.moderation.model.res;

import com.moderation.skillos.model.SkillExecutionTrace;
import com.moderation.skillos.model.ExecutionPlan;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PolicyExecuteRes {
    private String executionId;
    private String planId;
    private String policyId;
    private String status;
    private boolean success;
    private long durationMs;
    private ExecutionPlan plan;
    private Map<String, Object> state;
    private List<SkillExecutionTrace> traces;
    private String errorMessage;
}
