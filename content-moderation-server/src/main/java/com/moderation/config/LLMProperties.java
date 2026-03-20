package com.moderation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LLMProperties {

    /**
     * LLM 提供商
     */
    private String provider;

    /**
     * API 端点
     */
    private String endpoint;

    /**
     * 模型名称
     */
    private String model;

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 超时时间（毫秒）
     */
    private Integer timeoutMs;

    /**
     * 最大 token 数
     */
    private Integer maxTokens = 3000;

    /**
     * 视频抽帧 fps（video_url 输入使用）
     */
    private Integer videoFps = 5;

    /**
     * 提示词模板
     */
    private String promptTemplate;

    /**
     * Prompt 模块化配置（可拔插）
     */
    private PromptModules promptModules = new PromptModules();

    @Data
    public static class PromptModules {
        private PromptModuleSet hostViolation = new PromptModuleSet();
    }

    @Data
    public static class PromptModuleSet {
        private List<String> defaultModules = new ArrayList<>();
        private Map<String, String> modules = new LinkedHashMap<>();
    }
}
