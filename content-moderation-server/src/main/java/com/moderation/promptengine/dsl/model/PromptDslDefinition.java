package com.moderation.promptengine.dsl.model;

import lombok.Data;

import java.util.Map;

@Data
public class PromptDslDefinition {
    private String id;
    private String type;
    private Integer version;
    private ModelConfig model;
    private TemplateConfig template;
    private Map<String, Object> inputSchema;
    private Map<String, Object> outputSchema;
    private PromptConfig config;

    @Data
    public static class ModelConfig {
        private String name;
        private Double temperature;
        private Integer maxTokens;
    }

    @Data
    public static class TemplateConfig {
        private String system;
        private String instruction;
    }

    @Data
    public static class PromptConfig {
        private Double temperature;
        private Integer maxTokens;
    }
}
