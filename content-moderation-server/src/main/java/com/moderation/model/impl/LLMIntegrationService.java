package com.moderation.model.impl;

/**
 * LLM 集成服务接口
 */
public interface LLMIntegrationService {
    
    /**
     * 分析视频内容
     * @param videoUrl 视频 URL
     * @param contentId 内容 ID
     * @return LLM 响应 JSON
     */
    String analyzeVideo(String videoUrl, String contentId);

    /**
     * 分析视频内容（自定义提示词）
     * @param videoUrl 视频 URL
     * @param contentId 内容 ID
     * @param prompt 提示词
     * @return LLM 响应内容
     */
    String analyzeVideo(String videoUrl, String contentId, String prompt);
}
