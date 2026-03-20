package com.moderation.model.impl;

import com.moderation.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * BytePlus 视频审核模型实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BytePlusVideoModel implements ContentModerationModel {
    
    private final LLMIntegrationService llmService;
    
    @Override
    public ModerationResult moderate(ContentContent content) {
        log.info("BytePlus video moderation, contentId: {}", content.getContentId());
        
        try {
            // 调用 LLM 服务分析视频
            String response = llmService.analyzeVideo(
                content.getContentUrl(),
                content.getContentId()
            );
            
            // TODO: 解析 LLM 响应
            ModerationResult result = ModerationResult.builder()
                    .result("NOT_HIT")
                    .confidence(1.0)
                    .success(true)
                    .modelType(ModelType.BYTEPLUS_VIDEO)
                    .build();
            
            log.info("BytePlus video moderation completed, contentId: {}, result: {}", 
                    content.getContentId(), result.getResult());
            
            return result;
            
        } catch (Exception e) {
            log.error("BytePlus video moderation failed, contentId: {}", 
                    content.getContentId(), e);
            
            return ModerationResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .modelType(ModelType.BYTEPLUS_VIDEO)
                    .build();
        }
    }
    
    @Override
    public ModelType getModelType() {
        return ModelType.BYTEPLUS_VIDEO;
    }
    
    @Override
    public boolean supports(ContentType contentType) {
        return contentType == ContentType.VIDEO;
    }
}
