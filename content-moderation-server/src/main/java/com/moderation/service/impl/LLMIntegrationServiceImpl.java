package com.moderation.service.impl;

import com.moderation.config.LLMProperties;
import com.moderation.model.impl.LLMIntegrationService;
import com.moderation.service.LlmProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * LLM 集成服务实现
 * 对接 BytePlus API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMIntegrationServiceImpl implements LLMIntegrationService {
    
    private final LLMProperties llmProperties;
    private final LlmProfileService llmProfileService;
    private RestTemplate restTemplate;
    
    @Override
    public String analyzeVideo(String videoUrl, String contentId) {
        return analyzeVideo(videoUrl, contentId, llmProperties.getPromptTemplate());
    }

    @Override
    public String analyzeVideo(String videoUrl, String contentId, String prompt) {
        log.info("Analyzing video, contentId: {}, videoUrl: {}", contentId, videoUrl);
        LlmProfileService.LlmRuntimeProfile profile = llmProfileService.findDefaultEnabled().orElse(null);
        
        // 构建请求
        Map<String, Object> requestBody = buildRequestBody(videoUrl, prompt, profile);
        
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + (profile == null ? llmProperties.getApiKey() : profile.apiKey()));
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            // 调用 BytePlus API
            ResponseEntity<Map> response = getRestTemplate().postForEntity(
                    profile == null ? llmProperties.getEndpoint() : profile.endpoint(),
                    request,
                    Map.class
            );
            
            log.info("BytePlus API response status: {}", response.getStatusCode());
            
            // 解析响应
            return extractContent(response.getBody());
            
        } catch (Exception e) {
            log.error("BytePlus API call failed, contentId: {}", contentId, e);
            throw new RuntimeException("LLM API call failed: " + e.getMessage(), e);
        }
    }

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = createRestTemplate();
        }
        return restTemplate;
    }

    private RestTemplate createRestTemplate() {
        Integer timeoutMs = llmProperties.getTimeoutMs();
        int timeout = timeoutMs != null ? timeoutMs : 120000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }
    
    /**
     * 构建 LLM 请求体
     */
    private Map<String, Object> buildRequestBody(String videoUrl) {
        return buildRequestBody(videoUrl, buildPrompt(), null);
    }

    private Map<String, Object> buildRequestBody(String videoUrl, String prompt, LlmProfileService.LlmRuntimeProfile profile) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", profile == null ? llmProperties.getModel() : profile.model());
        
        // 构建消息
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        
        // 构建内容（视频 URL + 提示词）
        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> videoPart = new HashMap<>();
        videoPart.put("type", "video_url");
        Map<String, Object> videoUrlObj = new HashMap<>();
        videoUrlObj.put("url", videoUrl);
        videoUrlObj.put("fps", llmProperties.getVideoFps() == null ? 5 : llmProperties.getVideoFps());
        videoPart.put("video_url", videoUrlObj);
        content.add(videoPart);
        
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);
        
        userMessage.put("content", content);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", profile == null
                ? (llmProperties.getMaxTokens() == null ? 3000 : llmProperties.getMaxTokens())
                : (profile.maxTokens() == null ? 3000 : profile.maxTokens()));
        
        return requestBody;
    }
    
    /**
     * 构建提示词
     * 从配置文件中读取提示词模板
     */
    private String buildPrompt() {
        return llmProperties.getPromptTemplate();
    }
    
    /**
     * 提取 LLM 响应内容
     * 使用安全的类型检查，避免 ClassCastException
     */
    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        try {
            // 安全的类型检查
            Object choicesObj = responseBody.get("choices");
            if (!(choicesObj instanceof List)) {
                throw new RuntimeException("Invalid LLM response format: choices is not a list");
            }
            
            List<Map<String, Object>> choices = (List<Map<String, Object>>) choicesObj;
            if (choices.isEmpty()) {
                throw new RuntimeException("No choices in LLM response");
            }

            Object firstChoiceObj = choices.get(0);
            if (!(firstChoiceObj instanceof Map)) {
                throw new RuntimeException("Invalid LLM response format: first choice is not a map");
            }
            
            Map<String, Object> firstChoice = (Map<String, Object>) firstChoiceObj;
            Object messageObj = firstChoice.get("message");
            
            if (!(messageObj instanceof Map)) {
                throw new RuntimeException("Invalid LLM response format: message is not a map");
            }
            
            Map<String, Object> message = (Map<String, Object>) messageObj;
            Object contentObj = message.get("content");
            
            if (!(contentObj instanceof String)) {
                throw new RuntimeException("Invalid LLM response format: content is not a string");
            }
            
            return (String) contentObj;

        } catch (ClassCastException e) {
            log.error("Failed to cast LLM response, type mismatch", e);
            throw new RuntimeException("Failed to parse LLM response: type mismatch", e);
        } catch (Exception e) {
            log.error("Failed to extract content from LLM response", e);
            throw new RuntimeException("Failed to parse LLM response: " + e.getMessage(), e);
        }
    }
}
