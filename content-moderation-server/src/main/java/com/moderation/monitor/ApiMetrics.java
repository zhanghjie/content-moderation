package com.moderation.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * API 监控指标
 */
@Component
@RequiredArgsConstructor
public class ApiMetrics {
    
    private final MeterRegistry meterRegistry;
    
    /**
     * 记录 API 请求耗时
     */
    public void recordApiLatency(String apiName, long durationMs) {
        Timer.builder("api.latency")
                .tag("api", apiName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录 API 请求结果
     */
    public void recordApiResult(String apiName, String result) {
        meterRegistry.counter("api.result", 
                "api", apiName,
                "result", result).increment();
    }
    
    /**
     * 记录 API 错误
     */
    public void recordApiError(String apiName, String errorCode) {
        meterRegistry.counter("api.error", 
                "api", apiName,
                "error_code", errorCode).increment();
    }
}
