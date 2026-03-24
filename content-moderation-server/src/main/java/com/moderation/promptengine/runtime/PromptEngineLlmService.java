package com.moderation.promptengine.runtime;

import com.moderation.config.LLMProperties;
import com.moderation.service.LlmProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromptEngineLlmService {
    private final LlmProfileService llmProfileService;
    private final LLMProperties llmProperties;

    public String chat(String prompt, String modelName, Double temperature, Integer maxTokens) {
        return chat(prompt, modelName, temperature, maxTokens, null);
    }

    public String chat(String prompt, String modelName, Double temperature, Integer maxTokens, String configCode) {
        LlmProfileService.LlmRuntimeProfile profile = resolveProfile(configCode).orElse(null);
        String endpoint = profile == null ? llmProperties.getEndpoint() : profile.endpoint();
        String model = modelName == null || modelName.isBlank()
                ? (profile == null ? llmProperties.getModel() : profile.model())
                : modelName;
        String apiKey = profile == null ? llmProperties.getApiKey() : profile.apiKey();
        Integer timeout = profile == null ? llmProperties.getTimeoutMs() : profile.timeoutMs();
        String provider = profile == null ? null : profile.provider();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        if (temperature != null) requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens == null ? (profile == null ? llmProperties.getMaxTokens() : profile.maxTokens()) : maxTokens);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        ResponseEntity<Map> response = createRestTemplate(timeout == null ? 120000 : timeout)
                .postForEntity(normalizeChatCompletionsEndpoint(provider, endpoint), new HttpEntity<>(requestBody, headers), Map.class);
        return extractContent(response.getBody());
    }

    private Optional<LlmProfileService.LlmRuntimeProfile> resolveProfile(String configCode) {
        if (configCode != null && !configCode.isBlank()) {
            return llmProfileService.findByCode(configCode.trim());
        }
        return llmProfileService.findDefaultEnabled();
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        if (responseBody == null) return "";
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message.get("content");
            if (content instanceof String text) return text;
            if (content instanceof List<?> list) {
                StringBuilder out = new StringBuilder();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map && map.get("text") != null) out.append(map.get("text"));
                }
                return out.toString();
            }
            return String.valueOf(content);
        } catch (Exception e) {
            throw new IllegalArgumentException("LLM 响应解析失败");
        }
    }

    private String normalizeChatCompletionsEndpoint(String provider, String endpoint) {
        String normalized = endpoint == null ? "" : endpoint.trim();
        if (normalized.endsWith("/chat/completions")) return normalized;
        String providerName = provider == null ? "" : provider.trim().toLowerCase();
        if ("deepseek".equals(providerName) && normalized.matches("^https?://[^/]+/?$")) {
            return normalized.replaceAll("/+$", "") + "/v1/chat/completions";
        }
        if ("byteplus".equals(providerName) && normalized.matches("^https?://[^/]+/?$")) {
            return normalized.replaceAll("/+$", "") + "/api/v3/chat/completions";
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
