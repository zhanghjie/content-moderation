package com.moderation.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.entity.VideoAnalysisTaskEntity;
import com.moderation.mapper.VideoAnalysisTaskMapper;
import com.moderation.model.req.VideoAnalyzeReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 视频分析 API 集成测试
 * 使用 Testcontainers + PostgreSQL 进行真实环境测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VideoAnalysisApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VideoAnalysisTaskMapper videoAnalysisTaskMapper;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        videoAnalysisTaskMapper.delete(null);
    }

    @Test
    @DisplayName("应该返回 200 - 当请求有效时")
    void should_Return200_When_ValidRequest() throws Exception {
        // Given
        VideoAnalyzeReq request = VideoAnalyzeReq.builder()
                .callId("test-call-001")
                .contentId("test-content-001")
                .videoUrl("https://example.com/video.mp4")
                .coverUrl("https://example.com/cover.jpg")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/video/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.taskId").exists())
                .andExpect(jsonPath("$.data.callId").value("test-call-001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("应该返回 400 - 当缺少 callId 时")
    void should_Return400_When_MissingCallId() throws Exception {
        // Given
        VideoAnalyzeReq request = VideoAnalyzeReq.builder()
                .contentId("test-content-001")
                .videoUrl("https://example.com/video.mp4")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/video/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("应该返回 400 - 当缺少 videoUrl 时")
    void should_Return400_When_MissingVideoUrl() throws Exception {
        // Given
        VideoAnalyzeReq request = VideoAnalyzeReq.builder()
                .callId("test-call-001")
                .contentId("test-content-001")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/video/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("应该返回分析结果 - 当任务存在时")
    void should_ReturnResult_When_TaskExists() throws Exception {
        // Given - 先创建一个任务
        VideoAnalyzeReq createRequest = VideoAnalyzeReq.builder()
                .callId("test-call-002")
                .contentId("test-content-002")
                .videoUrl("https://example.com/video.mp4")
                .build();

        mockMvc.perform(post("/api/v1/video/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // When & Then - 查询结果
        mockMvc.perform(get("/api/v1/video/result/test-call-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.callId").value("test-call-002"))
                .andExpect(jsonPath("$.data.status").exists());
    }

    @Test
    @DisplayName("应该返回 NOT_FOUND - 当任务不存在时")
    void should_ReturnNotFound_When_TaskNotExists() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/video/result/non-existent-call"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("应该幂等处理 - 相同 callId 重复请求")
    void should_Idempotent_When_SameCallId() throws Exception {
        // Given
        VideoAnalyzeReq request = VideoAnalyzeReq.builder()
                .callId("test-call-idempotent")
                .contentId("test-content-001")
                .videoUrl("https://example.com/video.mp4")
                .build();

        // When - 第一次请求
        String firstResponse = mockMvc.perform(post("/api/v1/video/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // When - 第二次相同请求
        String secondResponse = mockMvc.perform(post("/api/v1/video/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then - 两次请求都应该成功
        assertThat(firstResponse).contains("\"code\":200");
        assertThat(secondResponse).contains("\"code\":200");
    }
}
