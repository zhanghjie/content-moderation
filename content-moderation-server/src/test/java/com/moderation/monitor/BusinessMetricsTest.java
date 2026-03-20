package com.moderation.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 业务监控指标测试
 */
class BusinessMetricsTest {
    
    private MeterRegistry meterRegistry;
    private BusinessMetrics businessMetrics;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        businessMetrics = new BusinessMetrics(meterRegistry);
    }
    
    @Test
    void should_RecordViolationDetected_When_ViolationFound() {
        // When
        businessMetrics.recordViolationDetected("CALL_IN_BED", 0.95);
        
        // Then
        var counter = meterRegistry.find("business.violation.detected")
                .tag("violation_type", "CALL_IN_BED")
                .counter();
        
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
    
    @Test
    void should_RecordViolationConfidence_When_ViolationFound() {
        // When
        businessMetrics.recordViolationDetected("CALL_IN_BED", 0.95);
        businessMetrics.recordViolationDetected("CALL_IN_BED", 0.88);
        
        // Then
        var summary = meterRegistry.find("business.violation.confidence")
                .tag("violation_type", "CALL_IN_BED")
                .summary();
        
        assertThat(summary).isNotNull();
        assertThat(summary.count()).isEqualTo(2);
    }
    
    @Test
    void should_RecordModerationResult_When_Completed() {
        // When
        businessMetrics.recordModerationResult("PASS");
        businessMetrics.recordModerationResult("PASS");
        businessMetrics.recordModerationResult("REJECT");
        
        // Then
        var passCounter = meterRegistry.find("business.moderation.result")
                .tag("result", "PASS")
                .counter();
        
        var rejectCounter = meterRegistry.find("business.moderation.result")
                .tag("result", "REJECT")
                .counter();
        
        assertThat(passCounter.count()).isEqualTo(2.0);
        assertThat(rejectCounter.count()).isEqualTo(1.0);
    }
    
    @Test
    void should_RecordContentType_When_ContentReceived() {
        // When
        businessMetrics.recordContentType("VIDEO");
        businessMetrics.recordContentType("IMAGE");
        
        // Then
        var videoCounter = meterRegistry.find("business.content.type")
                .tag("type", "VIDEO")
                .counter();
        
        var imageCounter = meterRegistry.find("business.content.type")
                .tag("type", "IMAGE")
                .counter();
        
        assertThat(videoCounter.count()).isEqualTo(1.0);
        assertThat(imageCounter.count()).isEqualTo(1.0);
    }
}
