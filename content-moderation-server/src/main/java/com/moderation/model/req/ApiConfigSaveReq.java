package com.moderation.model.req;

import lombok.Data;

@Data
public class ApiConfigSaveReq {

    private LlmConfig llm;

    private YidunConfig yidun;

    @Data
    public static class LlmConfig {
        private String provider;
        private String endpoint;
        private String model;
        private String apiKey;
        private Integer timeoutMs;
    }

    @Data
    public static class YidunConfig {
        private String secretId;
        private String secretKey;
        private String callbackUrl;
        private Integer timeoutMs;
    }
}

