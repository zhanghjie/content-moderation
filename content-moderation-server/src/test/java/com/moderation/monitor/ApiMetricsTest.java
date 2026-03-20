package com.moderation.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * API 监控指标测试
 */
class ApiMetricsTest {
    
    private MeterRegistry meterRegistry;
    private ApiMetrics apiMetrics;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        apiMetrics = new ApiMetrics(meterRegistry);
    }
    
    @Test
    void should_RecordLatency_When_ApiCalled() {
        // When
        apiMetrics.recordApiLatency("video.analyze", 100L);
        
        // Then
        var timer = meterRegistry.find("api.latency")
                .tag("api", "video.analyze")
                .timer();
        
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }
    
    @Test
    void should_RecordResult_When_ApiCompleted() {
        // When
        apiMetrics.recordApiResult("video.analyze", "success");
        
        // Then
        var counter = meterRegistry.find("api.result")
                .tag("api", "video.analyze")
                .tag("result", "success")
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
    
    @Test
    void should_RecordResult_When_ApiFailed() {
        // When
        apiMetrics.recordApiResult("video.analyze", "failed");
        
        // Then
        var counter = meterRegistry.find("api.result")
                .tag("api", "video.analyze")
                .tag("result", "failed")
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
    
    @Test
    void should_RecordError_When_ApiFailed() {
        // When
        apiMetrics.recordApiError("video.analyze", "500");
        
        // Then
        var counter = meterRegistry.find("api.error")
                .tag("api", "video.analyze")
                .tag("error_code", "500")
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
    
    @Test
    void should_RecordMultipleLatency_When_MultipleCalls() {
        // When
        apiMetrics.recordApiLatency("video.analyze", 100L);
        apiMetrics.recordApiLatency("video.analyze", 200L);
        apiMetrics.recordApiLatency("video.analyze", 150L);
        
        // Then
        var timer = meterRegistry.find("api.latency")
                .tag("api", "video.analyze")
                .timer();
        
        assertThat(timer.count()).isEqualTo(3);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(450.0);
    }
}
