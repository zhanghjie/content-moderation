package com.moderation.skillos.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class SkillExecutionTrace {
    private String traceId;
    private String stepId;
    private String status;
    private String skillId;
    private int attempt;
    private boolean success;
    private boolean skipped;
    private long durationMs;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private Object input;
    private Object output;
    private String message;
}
