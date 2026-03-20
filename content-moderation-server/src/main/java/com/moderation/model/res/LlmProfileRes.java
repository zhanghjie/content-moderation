package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmProfileRes {
    private String configCode;
    private String displayName;
    private String provider;
    private String endpoint;
    private String model;
    private Integer timeoutMs;
    private Integer maxTokens;
    private Boolean enabled;
    private Boolean isDefault;
    private Boolean apiKeyConfigured;
}
