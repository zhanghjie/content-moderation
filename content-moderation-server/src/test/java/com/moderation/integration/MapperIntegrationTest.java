package com.moderation.integration;

import com.moderation.entity.VideoAnalysisTaskEntity;
import com.moderation.entity.ViolationEventEntity;
import com.moderation.mapper.VideoAnalysisTaskMapper;
import com.moderation.mapper.ViolationEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapper 层集成测试
 * 验证 MyBatis-Plus Mapper 与数据库的集成
 * 
 * 注意：此测试需要 PostgreSQL 数据库或 H2 内存数据库
 * 由于 MyBatis-Plus 与 Spring Boot 3.2 的兼容性问题，此测试暂时无法运行
 */
@SpringBootTest
@ActiveProfiles("test")
class MapperIntegrationTest {

    @Autowired
    private VideoAnalysisTaskMapper videoAnalysisTaskMapper;

    @Autowired
    private ViolationEventMapper violationEventMapper;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        videoAnalysisTaskMapper.delete(null);
        violationEventMapper.delete(null);
    }

    @Test
    @DisplayName("应该插入和查询视频分析任务")
    void should_InsertAndSelect_Task() {
        // Given
        VideoAnalysisTaskEntity task = new VideoAnalysisTaskEntity();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setCallId("mapper-test-001");
        task.setContentId("content-001");
        task.setVideoUrl("https://example.com/video.mp4");
        task.setCoverUrl("https://example.com/cover.jpg");
        task.setStatus("PENDING");
        task.setRetryCount(0);

        // When - 插入
        int insertResult = videoAnalysisTaskMapper.insert(task);
        assertThat(insertResult).isEqualTo(1);

        // When - 查询
        VideoAnalysisTaskEntity found = videoAnalysisTaskMapper.selectByCallId("mapper-test-001");

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getTaskId()).isEqualTo(task.getTaskId());
        assertThat(found.getCallId()).isEqualTo(task.getCallId());
        assertThat(found.getContentId()).isEqualTo(task.getContentId());
        assertThat(found.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("应该更新任务状态")
    void should_UpdateTaskStatus() {
        // Given - 创建任务
        VideoAnalysisTaskEntity task = new VideoAnalysisTaskEntity();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setCallId("mapper-test-update-001");
        task.setContentId("content-001");
        task.setVideoUrl("https://example.com/video.mp4");
        task.setStatus("PENDING");
        task.setRetryCount(0);

        videoAnalysisTaskMapper.insert(task);

        // When - 更新
        task.setStatus("PROCESSING");
        int updateResult = videoAnalysisTaskMapper.updateById(task);

        // Then
        assertThat(updateResult).isEqualTo(1);

        VideoAnalysisTaskEntity updated = videoAnalysisTaskMapper.selectByCallId("mapper-test-update-001");
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    @DisplayName("应该插入和查询违规事件")
    void should_InsertAndSelect_ViolationEvent() {
        // Given
        ViolationEventEntity event = new ViolationEventEntity();
        event.setEventId(UUID.randomUUID().toString().replace("-", ""));
        event.setTaskId("task-mapper-001");
        event.setCallId("mapper-test-violation-001");
        event.setUserId(1001L);
        event.setContentId("content-mapper-001");
        event.setViolationType("CALL_IN_BED");
        event.setConfidence(0.95);
        event.setEvidence("主播全程躺姿通话");
        event.setStartSec(0);
        event.setEndSec(125);
        event.setProcessed(false);

        // When - 插入
        int insertResult = violationEventMapper.insert(event);
        assertThat(insertResult).isEqualTo(1);

        // When - 查询
        List<ViolationEventEntity> events = violationEventMapper.selectList(null);
        List<ViolationEventEntity> filtered = events.stream()
                .filter(e -> "mapper-test-violation-001".equals(e.getCallId()))
                .toList();

        // Then
        assertThat(filtered).isNotEmpty();
        assertThat(filtered.get(0).getViolationType()).isEqualTo("CALL_IN_BED");
        assertThat(filtered.get(0).getConfidence()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("应该查询未处理的违规事件")
    void should_QueryUnprocessedEvents() {
        // Given - 创建已处理和未处理的事件
        ViolationEventEntity processed = new ViolationEventEntity();
        processed.setEventId(UUID.randomUUID().toString().replace("-", ""));
        processed.setTaskId("task-mapper-processed");
        processed.setCallId("mapper-test-processed");
        processed.setUserId(1001L);
        processed.setContentId("content-mapper-processed");
        processed.setViolationType("NOISY");
        processed.setConfidence(0.8);
        processed.setEvidence("背景噪音大");
        processed.setStartSec(10);
        processed.setEndSec(20);
        processed.setProcessed(true);

        ViolationEventEntity unprocessed = new ViolationEventEntity();
        unprocessed.setEventId(UUID.randomUUID().toString().replace("-", ""));
        unprocessed.setTaskId("task-mapper-unprocessed");
        unprocessed.setCallId("mapper-test-unprocessed");
        unprocessed.setUserId(1002L);
        unprocessed.setContentId("content-mapper-unprocessed");
        unprocessed.setViolationType("BLACK_SCREEN");
        unprocessed.setConfidence(0.99);
        unprocessed.setEvidence("黑屏超过 30 秒");
        unprocessed.setStartSec(0);
        unprocessed.setEndSec(35);
        unprocessed.setProcessed(false);

        violationEventMapper.insert(processed);
        violationEventMapper.insert(unprocessed);

        // When - 查询未处理的事件
        List<ViolationEventEntity> unprocessedList = violationEventMapper.selectList(null).stream()
                .filter(e -> !e.getProcessed())
                .toList();

        // Then
        assertThat(unprocessedList).hasSize(1);
        assertThat(unprocessedList.get(0).getViolationType()).isEqualTo("BLACK_SCREEN");
    }

    @Test
    @DisplayName("应该删除任务")
    void should_DeleteTask() {
        // Given - 创建任务
        VideoAnalysisTaskEntity task = new VideoAnalysisTaskEntity();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setCallId("mapper-test-delete");
        task.setContentId("content-001");
        task.setVideoUrl("https://example.com/video.mp4");
        task.setStatus("PENDING");

        videoAnalysisTaskMapper.insert(task);

        // When - 删除
        int deleteResult = videoAnalysisTaskMapper.delete(null);

        // Then
        assertThat(deleteResult).isGreaterThanOrEqualTo(1);

        VideoAnalysisTaskEntity found = videoAnalysisTaskMapper.selectByCallId("mapper-test-delete");
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("应该查询多个任务")
    void should_QueryMultipleTasks() {
        // Given - 创建多个任务
        for (int i = 0; i < 3; i++) {
            VideoAnalysisTaskEntity task = new VideoAnalysisTaskEntity();
            task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
            task.setCallId("mapper-test-multi-" + i);
            task.setContentId("content-multi-" + i);
            task.setVideoUrl("https://example.com/video" + i + ".mp4");
            task.setStatus("PENDING");
            videoAnalysisTaskMapper.insert(task);
        }

        // When - 查询所有任务
        List<VideoAnalysisTaskEntity> allTasks = videoAnalysisTaskMapper.selectList(null);

        // Then - 至少能查到刚创建的 3 个任务
        assertThat(allTasks.size()).isGreaterThanOrEqualTo(3);
    }
}
