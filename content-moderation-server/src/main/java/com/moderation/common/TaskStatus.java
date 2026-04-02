package com.moderation.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务状态枚举
 * 用于视频分析任务的状态管理
 */
@Getter
@AllArgsConstructor
public enum TaskStatus {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),
    
    /**
     * 待处理
     */
    PENDING("PENDING", "待处理"),
    
    /**
     * 处理中
     */
    PROCESSING("PROCESSING", "处理中"),
    
    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成"),
    
    /**
     * 失败
     */
    FAILED("FAILED", "失败");
    
    /**
     * 状态码
     */
    private final String code;
    
    /**
     * 状态描述
     */
    private final String description;
    
    /**
     * 根据状态码获取枚举
     * @param code 状态码
     * @return 任务状态枚举
     */
    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown task status: " + code);
    }
}
