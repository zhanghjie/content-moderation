package com.moderation.skillos.planner;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PlannerDecision {
    private String type;
    private String stepId;
    private List<String> candidates;
    private String chosen;
    private String reason;
    private Map<String, Object> metadata;
}
