package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ApiConfigRes {

    private LlmConfig llm;

    private YidunConfig yidun;

    private OffsetDateTime updatedAt;

    @Data
    @Builder
    public static class LlmConfig {
        private String provider;
        private String endpoint;
        private String model;
        private Integer timeoutMs;
        private Boolean apiKeyConfigured;
    }

    @Data
    @Builder
    public static class YidunConfig {
        private String secretId;
        private String callbackUrl;
        private Integer timeoutMs;
        private Boolean secretKeyConfigured;
    }
}

