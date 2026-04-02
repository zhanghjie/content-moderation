package com.moderation.llm;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeminiApiSupport {
    public static final String DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com";
    public static final String DEFAULT_TEST_MODEL = "gemini-2.0-flash";

    private GeminiApiSupport() {
    }

    public static boolean isGeminiProvider(String provider) {
        return "gemini".equalsIgnoreCase(trim(provider));
    }

    public static boolean isGeminiEndpoint(String endpoint) {
        String normalizedEndpoint = sanitizeEndpoint(endpoint);
        if (normalizedEndpoint.isBlank()) {
            return false;
        }
        String lowerCase = normalizedEndpoint.toLowerCase();
        return lowerCase.contains("generativelanguage.googleapis.com");
    }

    public static String resolveTestModel(String provider, String model) {
        return resolveTestModel(provider, null, model);
    }

    public static String resolveTestModel(String provider, String endpoint, String model) {
        String normalizedModel = trim(model);
        boolean geminiRequest = isGeminiProvider(provider) || isGeminiEndpoint(endpoint);
        if (geminiRequest && !normalizedModel.toLowerCase().startsWith("gemini-")) {
            return DEFAULT_TEST_MODEL;
        }
        return normalizedModel.isBlank() ? DEFAULT_TEST_MODEL : normalizedModel;
    }

    public static String buildGenerateContentEndpoint(String endpoint, String model, String apiKey) {
        String normalizedModel = trim(model);
        if (normalizedModel.isBlank()) {
            throw new IllegalArgumentException("Gemini model 不能为空");
        }
        String normalizedEndpoint = sanitizeEndpoint(endpoint);
        if (normalizedEndpoint.isBlank()) {
            normalizedEndpoint = DEFAULT_ENDPOINT;
        }
        String noTailSlash = normalizedEndpoint.replaceAll("/+$", "");
        String path = noTailSlash;
        if (!noTailSlash.contains(":generateContent")) {
            if (noTailSlash.matches("^https?://[^/]+$")) {
                path = noTailSlash + "/v1beta/models/" + normalizedModel + ":generateContent";
            } else if (noTailSlash.endsWith("/v1beta")) {
                path = noTailSlash + "/models/" + normalizedModel + ":generateContent";
            } else if (noTailSlash.endsWith("/v1beta/models")) {
                path = noTailSlash + "/" + normalizedModel + ":generateContent";
            } else if (noTailSlash.contains("/models/")) {
                path = noTailSlash + ":generateContent";
            } else {
                path = noTailSlash + "/v1beta/models/" + normalizedModel + ":generateContent";
            }
        }
        if (trim(apiKey).isBlank()) {
            return path;
        }
        return UriComponentsBuilder.fromUriString(path)
                .queryParam("key", apiKey)
                .build()
                .toUriString();
    }

    public static Map<String, Object> buildGenerateContentRequest(String prompt, Double temperature, Integer maxOutputTokens) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt == null ? "" : prompt)))));
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        if (temperature != null) {
            generationConfig.put("temperature", temperature);
        }
        if (maxOutputTokens != null) {
            generationConfig.put("maxOutputTokens", maxOutputTokens);
        }
        if (!generationConfig.isEmpty()) {
            requestBody.put("generationConfig", generationConfig);
        }
        return requestBody;
    }

    @SuppressWarnings("unchecked")
    public static String extractText(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return "";
        }
        Object candidatesObj = responseBody.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            throw new IllegalArgumentException("Gemini 响应缺少 candidates");
        }
        Object firstObj = candidates.get(0);
        if (!(firstObj instanceof Map<?, ?> first)) {
            throw new IllegalArgumentException("Gemini 响应 candidates 格式非法");
        }
        Object contentObj = first.get("content");
        if (!(contentObj instanceof Map<?, ?> content)) {
            throw new IllegalArgumentException("Gemini 响应缺少 content");
        }
        Object partsObj = content.get("parts");
        if (!(partsObj instanceof List<?> parts)) {
            throw new IllegalArgumentException("Gemini 响应缺少 parts");
        }
        List<String> textParts = new ArrayList<>();
        for (Object item : parts) {
            if (item instanceof Map<?, ?> part && part.get("text") != null) {
                textParts.add(String.valueOf(part.get("text")));
            }
        }
        if (textParts.isEmpty()) {
            throw new IllegalArgumentException("Gemini 响应未返回文本内容");
        }
        return String.join("", textParts);
    }

    public static String sanitizeEndpoint(String endpoint) {
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

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
