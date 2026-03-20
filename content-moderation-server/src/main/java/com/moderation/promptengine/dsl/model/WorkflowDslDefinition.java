package com.moderation.promptengine.dsl.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkflowDslDefinition {
    private String id;
    private String type;
    private Integer version;
    private ContextDefinition context;
    private List<NodeDefinition> nodes;
    private List<EdgeDefinition> edges;
    private Map<String, String> output;

    @Data
    public static class ContextDefinition {
        private Map<String, String> inputs;
        private Map<String, String> computed;
        private List<ExternalSourceDefinition> external;
    }

    @Data
    public static class ExternalSourceDefinition {
        private String name;
        private String type;
        private String url;
    }

    @Data
    public static class NodeDefinition {
        private String id;
        private String type;
        private String promptRef;
        private Map<String, String> inputMapping;
        private Map<String, String> input;
        private String tool;
        private String runtime;
        private String code;
        private String outputKey;
    }

    @Data
    public static class EdgeDefinition {
        private String from;
        private String to;
    }
}
