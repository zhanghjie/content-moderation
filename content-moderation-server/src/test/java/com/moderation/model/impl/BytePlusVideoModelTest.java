package com.moderation.model.impl;

import com.moderation.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BytePlus 视频审核模型测试
 */
@ExtendWith(MockitoExtension.class)
class BytePlusVideoModelTest {
    
    @Mock
    private LLMIntegrationService llmService;
    
    @InjectMocks
    private BytePlusVideoModel bytePlusVideoModel;
    
    @Test
    void should_ReturnSuccess_When_LLMCallSuccess() {
        // Given
        String mockResponse = "{\"violations\": [], \"result\": \"PASS\"}";
        when(llmService.analyzeVideo(any(), any())).thenReturn(mockResponse);
        
        ContentContent content = ContentContent.builder()
                .contentId("test-001")
                .contentType(ContentType.VIDEO)
                .contentUrl("https://example.com/video.mp4")
                .build();
        
        // When
        ModerationResult result = bytePlusVideoModel.moderate(content);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getModelType()).isEqualTo(ModelType.BYTEPLUS_VIDEO);
        verify(llmService).analyzeVideo(any(), any());
    }
    
    @Test
    void should_ReturnFailure_When_LLMCallFailed() {
        // Given
        when(llmService.analyzeVideo(any(), any()))
                .thenThrow(new RuntimeException("LLM API Error"));
        
        ContentContent content = ContentContent.builder()
                .contentId("test-002")
                .contentType(ContentType.VIDEO)
                .contentUrl("https://example.com/video.mp4")
                .build();
        
        // When
        ModerationResult result = bytePlusVideoModel.moderate(content);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("LLM API Error");
    }
    
    @Test
    void should_SupportVideo_When_CheckSupports() {
        // When
        boolean supports = bytePlusVideoModel.supports(ContentType.VIDEO);
        
        // Then
        assertThat(supports).isTrue();
    }
    
    @Test
    void should_NotSupportImage_When_CheckSupports() {
        // When
        boolean supports = bytePlusVideoModel.supports(ContentType.IMAGE);
        
        // Then
        assertThat(supports).isFalse();
    }
    
    @Test
    void should_ReturnCorrectModelType() {
        // When
        ModelType modelType = bytePlusVideoModel.getModelType();
        
        // Then
        assertThat(modelType).isEqualTo(ModelType.BYTEPLUS_VIDEO);
    }
}
