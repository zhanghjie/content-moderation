package com.moderation.skillos.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ExecutionPlan {
    private String planId;
    private String executionId;
    private String policyId;
    private String version;
    private OffsetDateTime generatedAt;
    private Map<String, Object> plannerMeta;
    private List<ExecutionPlanStep> steps;
}
