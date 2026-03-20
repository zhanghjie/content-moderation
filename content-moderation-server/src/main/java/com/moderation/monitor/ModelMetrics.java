package com.moderation.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 模型监控指标
 */
@Component
@RequiredArgsConstructor
public class ModelMetrics {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * 记录模型调用
     */
    public void recordModelCall(String modelType, String contentType, boolean success) {
        meterRegistry.counter("model.call", 
                "model_type", modelType,
                "content_type", contentType,
                "success", String.valueOf(success)).increment();
    }
    
    /**
     * 记录模型响应时间
     */
    public void recordModelLatency(String modelType, long durationMs) {
        meterRegistry.timer("model.latency", 
                "model_type", modelType).record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录模型置信度分布
     */
    public void recordModelConfidence(String modelType, double confidence) {
        meterRegistry.summary("model.confidence", 
                "model_type", modelType).record(confidence);
    }
}
