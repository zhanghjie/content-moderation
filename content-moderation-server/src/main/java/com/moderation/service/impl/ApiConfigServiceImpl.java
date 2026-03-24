package com.moderation.service.impl;

import com.moderation.model.req.ApiConnectionTestReq;
import com.moderation.model.req.LlmProfileSaveReq;
import com.moderation.model.res.ApiConnectionTestRes;
import com.moderation.model.res.LlmProfilesRes;
import com.moderation.service.ApiConfigService;
import com.moderation.service.LlmProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String endpoint = req != null && hasText(req.getEndpoint()) ? req.getEndpoint()
                : profile == null ? null : profile.endpoint();
        String provider = profile == null ? null : profile.provider();
        endpoint = normalizeChatCompletionsEndpoint(provider, endpoint);
        String apiKey = profile == null ? null : profile.apiKey();
        String model = profile == null ? "ping-test-model" : profile.model();
        Integer timeout = req != null && req.getTimeoutMs() != null ? req.getTimeoutMs()
                : profile == null ? 120000 : profile.timeoutMs();
        return testLlmEndpoint(endpoint, timeout, apiKey, model);
    }

    @Override
    public ApiConnectionTestRes testYidun(ApiConnectionTestReq req) {
        String endpoint = req != null && hasText(req.getEndpoint()) ? sanitizeEndpoint(req.getEndpoint()) : null;
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

    private ApiConnectionTestRes testLlmEndpoint(String endpoint, Integer timeoutMs, String apiKey, String model) {
        if (!hasText(endpoint)) {
            return ApiConnectionTestRes.builder().success(false).statusCode(0).message("endpoint 不能为空").build();
        }
        if (!hasText(apiKey)) {
            return ApiConnectionTestRes.builder().success(false).statusCode(0).message("API Key 未配置").build();
        }
        int timeout = timeoutMs == null || timeoutMs < 1000 ? 10000 : timeoutMs;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", hasText(model) ? model : "ping-test-model");
            requestBody.put("messages", List.of(Map.of("role", "user", "content", "ping")));
            requestBody.put("max_tokens", 1);
            ResponseEntity<Map> response = createRestTemplate(timeout)
                    .postForEntity(endpoint, new HttpEntity<>(requestBody, headers), Map.class);
            int code = response.getStatusCode().value();
            boolean ok = code >= 200 && code < 300;
            return ApiConnectionTestRes.builder()
                    .success(ok)
                    .statusCode(code)
                    .message(ok ? "连接成功" : "连接失败")
                    .build();
        } catch (HttpStatusCodeException e) {
            return ApiConnectionTestRes.builder()
                    .success(false)
                    .statusCode(e.getStatusCode().value())
                    .message("连接失败")
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
        String normalizedEndpoint = sanitizeEndpoint(endpoint);
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

    private String sanitizeEndpoint(String endpoint) {
        if (endpoint == null) {
            return "";
        }
        String normalized = endpoint.trim();
        while (normalized.length() >= 2) {
            boolean wrappedByBacktick = normalized.startsWith("`") && normalized.endsWith("`");
            boolean wrappedByDoubleQuote = normalized.startsWith("\"") && normalized.endsWith("\"");
            boolean wrappedBySingleQuote = normalized.startsWith("'") && normalized.endsWith("'");
            if (!wrappedByBacktick && !wrappedByDoubleQuote && !wrappedBySingleQuote) {
                break;
            }
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private RestTemplate createRestTemplate(int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }
}
