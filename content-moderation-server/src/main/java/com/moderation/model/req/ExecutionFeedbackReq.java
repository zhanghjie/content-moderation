package com.moderation.model.req;

import lombok.Data;

import java.util.Map;

@Data
public class ExecutionFeedbackReq {
    private String traceId;
    private Double score;
    private String label;
    private String action;
    private String comment;
    private Map<String, Object> metadata;
}
