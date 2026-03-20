package com.moderation.model.impl;

import com.moderation.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 备用模型（用于降级）
 * 当主模型不可用时使用
 */
@Component
@Slf4j
public class BackupModel implements ContentModerationModel {
    
    @Override
    public ModerationResult moderate(ContentContent content) {
        log.warn("Using backup model, contentId: {}", content.getContentId());
        
        // 备用模型返回 SUSPECTED，转人工复核
        return ModerationResult.builder()
                .result("SUSPECTED")
                .confidence(0.0)
                .modelType(ModelType.BACKUP_MODEL)
                .success(true)
                .build();
    }
    
    @Override
    public ModelType getModelType() {
        return ModelType.BACKUP_MODEL;
    }
    
    @Override
    public boolean supports(ContentType contentType) {
        return true; // 支持所有类型
    }
}
