package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class WorkflowExecuteRes {
    private Boolean success;
    private Long durationMs;
    private Map<String, Object> output;
    private List<NodeTrace> nodeTraces;
    private String errorMessage;

    @Data
    @Builder
    public static class NodeTrace {
        private String nodeId;
        private String nodeType;
        private Long durationMs;
        private Object input;
        private Object output;
        private String errorMessage;
    }
}
