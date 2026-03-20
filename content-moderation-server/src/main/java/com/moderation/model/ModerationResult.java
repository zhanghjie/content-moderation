package com.moderation.model;

import lombok.Builder;
import lombok.Data;

/**
 * 内容审核结果
 */
@Data
@Builder
public class ModerationResult {
    
    /**
     * 违规命中结果：NOT_HIT, HIT, SUSPECTED
     */
    private String result;
    
    /**
     * 置信度 0-1
     */
    private Double confidence;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 使用的模型
     */
    private ModelType modelType;
    
    /**
     * 错误信息
     */
    private String errorMessage;
}
