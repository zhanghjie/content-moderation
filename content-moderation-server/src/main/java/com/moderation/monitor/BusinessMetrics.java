package com.moderation.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务监控指标
 */
@Component
@RequiredArgsConstructor
public class BusinessMetrics {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * 记录违规检测
     */
    public void recordViolationDetected(String violationType, double confidence) {
        meterRegistry.counter("business.violation.detected",
                "violation_type", violationType).increment();
        
        meterRegistry.summary("business.violation.confidence",
                "violation_type", violationType).record(confidence);
    }
    
    /**
     * 记录审核结果分布
     */
    public void recordModerationResult(String result) {
        meterRegistry.counter("business.moderation.result",
                "result", result).increment();
    }
    
    /**
     * 记录内容类型分布
     */
    public void recordContentType(String contentType) {
        meterRegistry.counter("business.content.type",
                "type", contentType).increment();
    }
}
