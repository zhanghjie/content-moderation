package com.moderation.service.impl;

import com.moderation.llm.GeminiApiSupport;
import com.moderation.model.req.ApiConnectionTestReq;
import com.moderation.model.req.LlmProfileSaveReq;
import com.moderation.model.res.ApiConnectionTestRes;
import com.moderation.model.res.LlmProfilesRes;
import com.moderation.service.ApiConfigService;
import com.moderation.service.LlmProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        String apiKey = profile == null ? null : profile.apiKey();
        String model = profile == null ? "ping-test-model" : profile.model();
        boolean geminiRequest = GeminiApiSupport.isGeminiProvider(provider) || GeminiApiSupport.isGeminiEndpoint(endpoint);
        if (geminiRequest) {
            model = GeminiApiSupport.resolveTestModel(provider, endpoint, model);
            endpoint = GeminiApiSupport.buildGenerateContentEndpoint(endpoint, model, apiKey);
        } else {
            endpoint = normalizeChatCompletionsEndpoint(provider, endpoint);
        }
        Integer timeout = req != null && req.getTimeoutMs() != null ? req.getTimeoutMs()
                : profile == null ? 120000 : profile.timeoutMs();
        log.info("testLlm request, configCode: {}, provider: {}, endpoint: {}, model: {}, timeoutMs: {}",
                configCode,
                provider,
                endpoint,
                model,
                timeout);
        return testLlmEndpoint(endpoint, timeout, apiKey, model, provider);
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

    private ApiConnectionTestRes testLlmEndpoint(String endpoint, Integer timeoutMs, String apiKey, String model, String provider) {
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
            Map<String, Object> requestBody = new HashMap<>();
            boolean asrEndpoint = isAsrEndpoint(provider, endpoint, model);
            boolean geminiRequest = GeminiApiSupport.isGeminiProvider(provider) || GeminiApiSupport.isGeminiEndpoint(endpoint);
            if (geminiRequest) {
                requestBody.putAll(GeminiApiSupport.buildGenerateContentRequest("ping", null, 1));
            } else if (asrEndpoint) {
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("X-DashScope-Async", "enable");
                requestBody.put("model", hasText(model) ? model : "fun-asr");
                requestBody.put("input", Map.of("file_urls", List.of("https://example.com/test.wav")));
            } else {
                headers.set("Authorization", "Bearer " + apiKey);
                requestBody.put("model", hasText(model) ? model : "ping-test-model");
                requestBody.put("messages", List.of(Map.of("role", "user", "content", "ping")));
                requestBody.put("max_tokens", 1);
            }
            log.info("testLlm payload, asrEndpoint: {}, provider: {}, endpoint: {}, model: {}, body: {}",
                    asrEndpoint, provider, endpoint, model, requestBody);
            ResponseEntity<Map> response = createRestTemplate(timeout)
                    .postForEntity(endpoint, new HttpEntity<>(requestBody, headers), Map.class);
            int code = response.getStatusCode().value();
            boolean ok = code >= 200 && code < 300;
            log.info("testLlm response, statusCode: {}, provider: {}, endpoint: {}, body: {}",
                    code, provider, endpoint, response.getBody());
            return ApiConnectionTestRes.builder()
                    .success(ok)
                    .statusCode(code)
                    .message(ok ? "连接成功" : "连接失败")
                    .build();
        } catch (HttpStatusCodeException e) {
            boolean asrEndpoint = isAsrEndpoint(provider, endpoint, model);
            String responseBody = safeResponseBody(e.getResponseBodyAsString());
            log.warn("testLlm http error, asrEndpoint: {}, provider: {}, endpoint: {}, model: {}, statusCode: {}, responseBody: {}",
                    asrEndpoint, provider, endpoint, model, e.getStatusCode().value(), responseBody);
            if (asrEndpoint && e.getStatusCode().value() == 400) {
                // ASR 接口对测试用 file_urls 可能返回参数校验错误，但说明 endpoint 可达且鉴权已通过
                return ApiConnectionTestRes.builder()
                        .success(true)
                        .statusCode(400)
                        .message("连接成功（ASR 接口可达，测试参数校验返回 400 属于预期）")
                        .build();
            }
            return ApiConnectionTestRes.builder()
                    .success(false)
                    .statusCode(e.getStatusCode().value())
                    .message("连接失败: HTTP " + e.getStatusCode().value())
                    .build();
        } catch (Exception e) {
            log.error("testLlm unexpected error, provider: {}, endpoint: {}, model: {}", provider, endpoint, model, e);
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

    private boolean isAsrEndpoint(String provider, String endpoint, String model) {
        String e = endpoint == null ? "" : endpoint.trim().toLowerCase();
        String m = model == null ? "" : model.trim().toLowerCase();
        return e.contains("/audio/asr/") || m.contains("fun-asr");
    }

    private String safeResponseBody(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim();
        if (trimmed.length() <= 1000) {
            return trimmed;
        }
        return trimmed.substring(0, 1000) + "...(truncated)";
    }

    private String normalizeChatCompletionsEndpoint(String provider, String endpoint) {
        String normalizedEndpoint = GeminiApiSupport.sanitizeEndpoint(endpoint);
        if (normalizedEndpoint.isBlank()) {
            return normalizedEndpoint;
        }
        String noTailSlash = normalizedEndpoint.replaceAll("/+$", "");
        if (noTailSlash.endsWith("/chat/completions")) {
            return normalizedEndpoint;
        }
        String providerName = provider == null ? "" : provider.trim().toLowerCase();
        if ("byteplus".equals(providerName)) {
            if (noTailSlash.matches("^https?://[^/]+$")) {
                return noTailSlash + "/api/v3/chat/completions";
            }
            if (noTailSlash.endsWith("/api/v3")) {
                return noTailSlash + "/chat/completions";
            }
        }

        // OpenAI compatible endpoints:
        // - https://host
        // - https://host/v1
        // - https://host/compatible-mode/v1 (DashScope)
        if (noTailSlash.matches("^https?://[^/]+$")) {
            return noTailSlash + "/v1/chat/completions";
        }
        if (noTailSlash.endsWith("/v1")) {
            return noTailSlash + "/chat/completions";
        }
        return noTailSlash;
    }

    private String sanitizeEndpoint(String endpoint) {
        return GeminiApiSupport.sanitizeEndpoint(endpoint);
    }

    private RestTemplate createRestTemplate(int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }
}
