package com.moderation.service.impl;

import com.moderation.config.LLMProperties;
import com.moderation.service.LlmProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LLM 集成服务测试 - 纯 Mockito 测试，不依赖 Spring Context
 */
class LLMIntegrationServiceTest {

    private RestTemplate restTemplate;
    private LLMProperties llmProperties;
    private LlmProfileService llmProfileService;
    private LLMIntegrationServiceImpl llmService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        llmProperties = new LLMProperties();
        llmProperties.setEndpoint("https://api.test.com/chat/completions");
        llmProperties.setModel("test-model");
        llmProperties.setApiKey("test-api-key");
        llmProperties.setTimeoutMs(5000);
        llmProfileService = mock(LlmProfileService.class);
        when(llmProfileService.findDefaultEnabled()).thenReturn(Optional.empty());

        llmService = new LLMIntegrationServiceImpl(llmProperties, llmProfileService);
        // 注入 Mock RestTemplate
        try {
            var field = LLMIntegrationServiceImpl.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(llmService, restTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject restTemplate", e);
        }
    }

    @Test
    @DisplayName("应该返回响应 - 当 API 调用成功时")
    void should_ReturnResponse_When_APIcallSuccess() {
        // Given
        String videoUrl = "https://example.com/video.mp4";
        String contentId = "test-001";

        // Mock API 响应
        Map<String, Object> mockResponse = buildMockResponse();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        String result = llmService.analyzeVideo(videoUrl, contentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("PASS");
    }

    @Test
    @DisplayName("应该抛出异常 - 当 API 调用失败时")
    void should_ThrowException_When_APIcallFails() {
        // Given
        String videoUrl = "https://example.com/video.mp4";
        String contentId = "test-002";

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("API Error"));

        // When & Then
        assertThatThrownBy(() -> llmService.analyzeVideo(videoUrl, contentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LLM API call failed");
    }

    @Test
    @DisplayName("应该解析违规信息 - 当响应包含 violations 时")
    void should_ParseViolations_When_ResponseContainsViolations() {
        // Given
        String videoUrl = "https://example.com/video.mp4";
        String contentId = "test-003";

        Map<String, Object> mockResponse = buildMockResponseWithViolations();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        // When
        String result = llmService.analyzeVideo(videoUrl, contentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("CALL_IN_BED");
    }

    private Map<String, Object> buildMockResponse() {
        Map<String, Object> response = new HashMap<>();

        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();

        Map<String, Object> message = new HashMap<>();
        message.put("content", "{\"result\": \"PASS\"}");

        choice.put("message", message);
        choices.add(choice);

        response.put("choices", choices);
        return response;
    }

    private Map<String, Object> buildMockResponseWithViolations() {
        Map<String, Object> response = new HashMap<>();

        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();

        Map<String, Object> message = new HashMap<>();
        message.put("content", "{\"violations\": [{\"type\": \"CALL_IN_BED\", \"confidence\": 0.95}], \"result\": \"REJECT\"}");

        choice.put("message", message);
        choices.add(choice);

        response.put("choices", choices);
        return response;
    }
}
