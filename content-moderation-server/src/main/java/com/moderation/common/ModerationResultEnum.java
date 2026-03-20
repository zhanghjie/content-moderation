package com.moderation.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 违规命中结果枚举
 * 用于内容违规命中状态判定
 */
@Getter
@AllArgsConstructor
public enum ModerationResultEnum {
    
    /**
     * 未命中违规
     */
    NOT_HIT("NOT_HIT", "未命中违规", "success"),
    
    /**
     * 命中违规
     */
    HIT("HIT", "命中违规", "error"),
    
    /**
     * 疑似命中违规
     */
    SUSPECTED("SUSPECTED", "疑似命中", "warning");
    
    /**
     * 结果码
     */
    private final String code;
    
    /**
     * 结果描述
     */
    private final String description;
    
    /**
     * UI 类型（用于前端展示）
     */
    private final String uiType;
    
    /**
     * 根据结果码获取枚举
     * @param code 结果码
     * @return 审核结果枚举
     */
    public static ModerationResultEnum fromCode(String code) {
        for (ModerationResultEnum result : values()) {
            if (result.code.equals(code)) {
                return result;
            }
        }
        throw new IllegalArgumentException("Unknown moderation result: " + code);
    }
}
