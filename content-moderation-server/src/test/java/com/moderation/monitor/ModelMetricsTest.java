package com.moderation.monitor;

import com.moderation.model.ModelType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * 模型监控指标测试
 */
class ModelMetricsTest {
    
    private MeterRegistry meterRegistry;
    private ModelMetrics modelMetrics;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        modelMetrics = new ModelMetrics(meterRegistry);
    }
    
    @Test
    void should_RecordModelCall_When_ModelCalled() {
        // When
        modelMetrics.recordModelCall("byteplus-video", "video", true);
        
        // Then
        var counter = meterRegistry.find("model.call")
                .tag("model_type", "byteplus-video")
                .tag("content_type", "video")
                .tag("success", "true")
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
    
    @Test
    void should_RecordModelCall_When_ModelFailed() {
        // When
        modelMetrics.recordModelCall("byteplus-video", "video", false);
        
        // Then
        var counter = meterRegistry.find("model.call")
                .tag("model_type", "byteplus-video")
                .tag("content_type", "video")
                .tag("success", "false")
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
    
    @Test
    void should_RecordModelLatency_When_ModelCalled() {
        // When
        modelMetrics.recordModelLatency("byteplus-video", 500L);
        
        // Then
        var timer = meterRegistry.find("model.latency")
                .tag("model_type", "byteplus-video")
                .timer();
        
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }
    
    @Test
    void should_RecordModelConfidence_When_ResultReturned() {
        // When
        modelMetrics.recordModelConfidence("byteplus-video", 0.95);
        modelMetrics.recordModelConfidence("byteplus-video", 0.88);
        
        // Then
        var summary = meterRegistry.find("model.confidence")
                .tag("model_type", "byteplus-video")
                .summary();
        
        assertThat(summary).isNotNull();
        assertThat(summary.count()).isEqualTo(2);
    }
}
