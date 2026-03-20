package com.moderation.model.req;

import lombok.Data;

@Data
public class LlmProfileSaveReq {
    private String configCode;
    private String displayName;
    private String provider;
    private String endpoint;
    private String model;
    private String apiKey;
    private Integer timeoutMs;
    private Integer maxTokens;
    private Boolean enabled;
    private Boolean isDefault;
}
