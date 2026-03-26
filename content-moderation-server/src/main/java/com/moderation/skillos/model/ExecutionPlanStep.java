package com.moderation.skillos.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ExecutionPlanStep {
    private String stepId;
    private Integer stepOrder;
    private String skillId;
    private Map<String, Object> skillSnapshot;
    private String mode;
    private Integer timeoutMs;
    private Map<String, Object> retryPolicy;
    private List<String> dependsOn;
    private Map<String, Object> inputBindings;
    private Map<String, Object> outputBindings;
    private String onFailure;
}
