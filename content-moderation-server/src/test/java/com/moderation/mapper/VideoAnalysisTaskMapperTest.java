package com.moderation.mapper;

import com.moderation.entity.VideoAnalysisTaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * 视频分析任务 Mapper 测试
 * 使用 H2 内存数据库进行快速测试
 */
@SpringBootTest
@ActiveProfiles("test")
class VideoAnalysisTaskMapperTest {
    
    @Autowired
    private VideoAnalysisTaskMapper videoAnalysisTaskMapper;
    
    @Test
    void should_InsertAndSelect_When_TaskCreated() {
        // Given
        VideoAnalysisTaskEntity task = new VideoAnalysisTaskEntity();
        task.setTaskId("test-task-001");
        task.setCallId("test-call-001");
        task.setContentId("test-content-001");
        task.setVideoUrl("https://example.com/test.mp4");
        task.setStatus("PENDING");
        task.setRetryCount(0);
        
        // When
        int inserted = videoAnalysisTaskMapper.insert(task);
        
        // Then
        assertThat(inserted).isEqualTo(1);
        assertThat(task.getId()).isNotNull();
    }
    
    @Test
    void should_SelectByCallId_When_TaskExists() {
        // Given
        VideoAnalysisTaskEntity task = new VideoAnalysisTaskEntity();
        task.setTaskId("test-task-002");
        task.setCallId("test-call-002");
        task.setContentId("test-content-002");
        task.setVideoUrl("https://example.com/test.mp4");
        task.setStatus("PENDING");
        task.setRetryCount(0);
        
        videoAnalysisTaskMapper.insert(task);
        
        // When
        VideoAnalysisTaskEntity found = videoAnalysisTaskMapper.selectByCallId("test-call-002");
        
        // Then
        assertThat(found).isNotNull();
        assertThat(found.getTaskId()).isEqualTo("test-task-002");
        assertThat(found.getCallId()).isEqualTo("test-call-002");
    }
    
    @Test
    void should_ReturnNull_When_TaskNotFound() {
        // When
        VideoAnalysisTaskEntity found = videoAnalysisTaskMapper.selectByCallId("non-existent-call");
        
        // Then
        assertThat(found).isNull();
    }
}
