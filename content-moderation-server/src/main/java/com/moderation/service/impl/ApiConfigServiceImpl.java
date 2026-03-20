package com.moderation.service.impl;

import com.moderation.model.req.ApiConnectionTestReq;
import com.moderation.model.req.LlmProfileSaveReq;
import com.moderation.model.res.ApiConnectionTestRes;
import com.moderation.model.res.LlmProfilesRes;
import com.moderation.service.ApiConfigService;
import com.moderation.service.LlmProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class ApiConfigServiceImpl implements ApiConfigService {

    private final LlmProfileService llmProfileService;

    @Override
    public LlmProfilesRes getConfig() {
        return llmProfileService.listProfiles();
    }

    @Override
    public LlmProfilesRes saveConfig(LlmProfileSaveReq req) {
        return llmProfileService.saveProfile(req);
    }

    @Override
    public LlmProfilesRes deleteConfig(String configCode) {
        return llmProfileService.deleteProfile(configCode);
    }

    @Override
    public LlmProfilesRes setDefaultConfig(String configCode) {
        return llmProfileService.setDefault(configCode);
    }

    @Override
    public ApiConnectionTestRes testLlm(ApiConnectionTestReq req) {
        String configCode = req == null ? null : req.getConfigCode();
        var profile = hasText(configCode)
                ? llmProfileService.findByCode(configCode).orElse(null)
                : llmProfileService.findDefaultEnabled().orElse(null);
        String endpoint = req != null && hasText(req.getEndpoint()) ? req.getEndpoint().trim()
                : profile == null ? null : profile.endpoint();
        if (profile != null && (req == null || !hasText(req.getEndpoint()))) {
            endpoint = normalizeChatCompletionsEndpoint(profile.provider(), endpoint);
        }
        Integer timeout = req != null && req.getTimeoutMs() != null ? req.getTimeoutMs()
                : profile == null ? 120000 : profile.timeoutMs();
        return testEndpoint(endpoint, timeout);
    }

    @Override
    public ApiConnectionTestRes testYidun(ApiConnectionTestReq req) {
        String endpoint = req != null && hasText(req.getEndpoint()) ? req.getEndpoint().trim() : null;
        Integer timeout = req != null && req.getTimeoutMs() != null ? req.getTimeoutMs() : 60000;
        return testEndpoint(endpoint, timeout);
    }

    private ApiConnectionTestRes testEndpoint(String endpoint, Integer timeoutMs) {
        if (!hasText(endpoint)) {
            return ApiConnectionTestRes.builder().success(false).statusCode(0).message("endpoint 不能为空").build();
        }
        int timeout = timeoutMs == null || timeoutMs < 1000 ? 10000 : timeoutMs;
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.connect();
            int code = conn.getResponseCode();
            boolean ok = code >= 200 && code < 400;
            return ApiConnectionTestRes.builder()
                    .success(ok)
                    .statusCode(code)
                    .message(ok ? "连接成功" : "连接失败")
                    .build();
        } catch (Exception e) {
            return ApiConnectionTestRes.builder()
                    .success(false)
                    .statusCode(0)
                    .message("连接失败: " + e.getMessage())
                    .build();
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String normalizeChatCompletionsEndpoint(String provider, String endpoint) {
        String normalizedEndpoint = endpoint == null ? "" : endpoint.trim();
        if (normalizedEndpoint.endsWith("/chat/completions")) {
            return normalizedEndpoint;
        }
        String providerName = provider == null ? "" : provider.trim().toLowerCase();
        if ("deepseek".equals(providerName) && normalizedEndpoint.matches("^https?://[^/]+/?$")) {
            return normalizedEndpoint.replaceAll("/+$", "") + "/v1/chat/completions";
        }
        if ("byteplus".equals(providerName) && normalizedEndpoint.matches("^https?://[^/]+/?$")) {
            return normalizedEndpoint.replaceAll("/+$", "") + "/api/v3/chat/completions";
        }
        return normalizedEndpoint;
    }
}
