package com.moderation.service;

import com.moderation.entity.VideoAnalysisTaskEntity;
import com.moderation.mapper.VideoAnalysisTaskMapper;
import com.moderation.model.req.VideoAnalyzeReq;
import com.moderation.model.res.VideoAnalyzeRes;
import com.moderation.service.impl.VideoAnalysisProcessor;
import com.moderation.service.impl.VideoAnalysisServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 视频分析服务测试
 */
@ExtendWith(MockitoExtension.class)
class VideoAnalysisServiceTest {
    
    @Mock
    private VideoAnalysisTaskMapper videoAnalysisTaskMapper;

    @Mock
    private VideoAnalysisProcessor videoAnalysisProcessor;

    @Mock
    private Executor videoAnalysisExecutor;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @InjectMocks
    private VideoAnalysisServiceImpl videoAnalysisService;
    
    private VideoAnalyzeReq testRequest;
    
    @BeforeEach
    void setUp() {
        testRequest = VideoAnalyzeReq.builder()
                .callId("test-call-001")
                .contentId("test-content-001")
                .videoUrl("https://example.com/test.mp4")
                .coverUrl("https://example.com/cover.jpg")
                .build();
    }
    
    @Test
    void should_CreateTask_When_AnalyzeRequested() {
        // Given
        when(videoAnalysisTaskMapper.insert(any(VideoAnalysisTaskEntity.class))).thenReturn(1);

        ArgumentCaptor<VideoAnalysisTaskEntity> taskCaptor = ArgumentCaptor.forClass(VideoAnalysisTaskEntity.class);

        // When
        VideoAnalyzeRes result = videoAnalysisService.analyze(testRequest);

        // Then
        verify(videoAnalysisTaskMapper).insert(taskCaptor.capture());
        VideoAnalysisTaskEntity capturedTask = taskCaptor.getValue();

        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getCallId()).isEqualTo(testRequest.getCallId());

        assertThat(capturedTask.getCallId()).isEqualTo(testRequest.getCallId());
        assertThat(capturedTask.getContentId()).isEqualTo(testRequest.getContentId());
        assertThat(capturedTask.getVideoUrl()).isEqualTo(testRequest.getVideoUrl());
        assertThat(capturedTask.getStatus()).isEqualTo("PENDING");
    }
    
    @Test
    void should_ReturnResult_When_TaskFound() {
        // Given
        String callId = "test-call-001";
        VideoAnalysisTaskEntity task = new VideoAnalysisTaskEntity();
        task.setTaskId("test-task-id");
        task.setCallId(callId);
        task.setStatus("COMPLETED");
        
        when(videoAnalysisTaskMapper.selectByCallId(callId)).thenReturn(task);
        
        // When
        VideoAnalyzeRes result = videoAnalysisService.getResult(callId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isEqualTo("test-task-id");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getModerationResult()).isEqualTo("NOT_HIT");
    }
    
    @Test
    void should_ReturnNotFound_When_TaskNotFound() {
        // Given
        String callId = "non-existent-call";
        when(videoAnalysisTaskMapper.selectByCallId(callId)).thenReturn(null);
        
        // When
        VideoAnalyzeRes result = videoAnalysisService.getResult(callId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("NOT_FOUND");
    }
}
