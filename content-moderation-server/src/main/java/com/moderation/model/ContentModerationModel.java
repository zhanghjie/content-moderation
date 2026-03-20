package com.moderation.model;

/**
 * 内容审核模型接口
 */
public interface ContentModerationModel {
    
    /**
     * 执行内容审核
     */
    ModerationResult moderate(ContentContent content);
    
    /**
     * 获取模型类型
     */
    ModelType getModelType();
    
    /**
     * 是否支持该内容类型
     */
    boolean supports(ContentType contentType);
}
