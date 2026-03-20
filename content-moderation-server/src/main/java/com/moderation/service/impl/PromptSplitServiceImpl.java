package com.moderation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.model.req.PromptModuleSaveReq;
import com.moderation.model.req.PromptSplitReq;
import com.moderation.model.res.PromptModuleManageRes;
import com.moderation.model.res.PromptSplitRes;
import com.moderation.prompt.PromptModuleManageService;
import com.moderation.service.LlmProfileService;
import com.moderation.service.PromptSplitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptSplitServiceImpl implements PromptSplitService {

    private final LlmProfileService llmProfileService;
    private final PromptModuleManageService promptModuleManageService;
    private final ObjectMapper objectMapper;

    @Override
    public PromptSplitRes split(PromptSplitReq req) {
        if (req == null || req.getRawPrompt() == null || req.getRawPrompt().isBlank()) {
            throw new IllegalArgumentException("rawPrompt 不能为空");
        }
        String analysisType = req.getAnalysisType() == null || req.getAnalysisType().isBlank()
                ? "HOST_VIOLATION" : req.getAnalysisType().trim().toUpperCase();
        LlmProfileService.LlmRuntimeProfile profile = (req.getLlmConfigCode() == null || req.getLlmConfigCode().isBlank())
                ? llmProfileService.findDefaultEnabled().orElseThrow(() -> new IllegalArgumentException("未找到可用默认模型配置"))
                : llmProfileService.findByCode(req.getLlmConfigCode()).orElseThrow(() -> new IllegalArgumentException("模型配置不存在或未启用"));
        String modelContent = invokeSplitModel(profile, req.getRawPrompt(), analysisType);
        List<PromptModuleSaveReq> modules = parseModules(modelContent, analysisType);
        if (Boolean.TRUE.equals(req.getApplyToScene())) {
            promptModuleManageService.replaceModules(analysisType, modules);
        }
        return PromptSplitRes.builder()
                .analysisType(analysisType)
                .llmConfigCode(profile.configCode())
                .rawModelResponse(modelContent)
                .modules(modules.stream().map(this::toDetail).toList())
                .build();
    }

    private String invokeSplitModel(LlmProfileService.LlmRuntimeProfile profile, String rawPrompt, String analysisType) {
        if (profile.endpoint() == null || profile.endpoint().isBlank()) {
            throw new IllegalArgumentException("模型 endpoint 不能为空");
        }
        if (profile.model() == null || profile.model().isBlank()) {
            throw new IllegalArgumentException("模型 model 不能为空");
        }
        RestTemplate restTemplate = createRestTemplate(profile.timeoutMs() == null ? 120000 : profile.timeoutMs());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + profile.apiKey());

        String instruction = """
                你是 Prompt 架构拆分器。请把输入 Prompt 按框架拆分成模块，并严格只输出 JSON：
                {
                  "modules":[
                    {"code":"%s_BASE","title":"任务说明与核心原则","category":"REQUIRED|PLUGGABLE|FREE","content":"...","enabled":true,"sortOrder":0}
                  ]
                }
                要求：
                1) code 仅使用大写字母、数字、下划线
                2) category 仅允许 REQUIRED/PLUGGABLE/FREE
                3) 至少输出 BASE、RULES、JSON 三个模块
                4) content 保留原文语义
                """.formatted(analysisType);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", profile.model());
        requestBody.put("messages", List.of(Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "text", "text", instruction + "\n\n待拆分原始Prompt:\n" + rawPrompt)
                )
        )));
        requestBody.put("max_tokens", profile.maxTokens() == null ? 3000 : profile.maxTokens());
        String endpoint = normalizeChatCompletionsEndpoint(profile.provider(), profile.endpoint());
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(requestBody, headers), Map.class);
            return extractContent(response.getBody());
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            String reason = body == null || body.isBlank() ? e.getStatusText() : body;
            throw new IllegalArgumentException("模型接口调用失败(" + e.getRawStatusCode() + "): " + reason);
        } catch (RestClientException e) {
            throw new IllegalArgumentException("模型接口调用失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message.get("content");
            if (content instanceof String contentText) {
                return contentText;
            }
            if (content instanceof List<?> contentList) {
                StringBuilder merged = new StringBuilder();
                for (Object item : contentList) {
                    if (item instanceof Map<?, ?> contentMap) {
                        Object text = contentMap.get("text");
                        if (text != null) {
                            merged.append(text);
                        }
                    }
                }
                if (!merged.isEmpty()) {
                    return merged.toString();
                }
            }
            return String.valueOf(content);
        } catch (Exception e) {
            throw new IllegalArgumentException("模型返回内容不可解析");
        }
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

    private List<PromptModuleSaveReq> parseModules(String modelContent, String analysisType) {
        try {
            JsonNode root = objectMapper.readTree(modelContent);
            JsonNode modulesNode = root.path("modules");
            if (!modulesNode.isArray()) throw new IllegalArgumentException("模型未返回 modules 数组");
            List<PromptModuleSaveReq> out = new ArrayList<>();
            int i = 0;
            for (JsonNode n : modulesNode) {
                String content = n.path("content").asText("");
                if (content.isBlank()) continue;
                PromptModuleSaveReq req = new PromptModuleSaveReq();
                req.setAnalysisType(analysisType);
                String code = n.path("code").asText(analysisType + "_MODULE_" + i);
                req.setCode(code.toUpperCase().replaceAll("[^A-Z0-9_]", "_"));
                req.setTitle(n.path("title").asText(req.getCode()));
                String category = n.path("category").asText("PLUGGABLE").toUpperCase();
                if (!List.of("REQUIRED", "PLUGGABLE", "FREE").contains(category)) category = "PLUGGABLE";
                req.setCategory(category);
                req.setContent(content);
                req.setEnabled(!n.has("enabled") || n.path("enabled").asBoolean(true));
                req.setSortOrder(n.has("sortOrder") ? n.path("sortOrder").asInt(i) : i);
                out.add(req);
                i++;
            }
            if (out.isEmpty()) throw new IllegalArgumentException("拆分结果为空");
            return out;
        } catch (Exception e) {
            throw new IllegalArgumentException("拆分结果格式错误: " + e.getMessage());
        }
    }

    private PromptModuleManageRes.PromptModuleDetail toDetail(PromptModuleSaveReq req) {
        return PromptModuleManageRes.PromptModuleDetail.builder()
                .code(req.getCode())
                .title(req.getTitle())
                .category(req.getCategory())
                .content(req.getContent())
                .enabled(req.getEnabled())
                .sortOrder(req.getSortOrder())
                .build();
    }

    private RestTemplate createRestTemplate(int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }
}
