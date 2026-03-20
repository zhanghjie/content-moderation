package com.moderation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内容类型枚举
 */
@Getter
@AllArgsConstructor
public enum ContentType {
    
    VIDEO("video", "视频"),
    IMAGE("image", "图片"),
    TEXT("text", "文本"),
    AUDIO("audio", "音频");
    
    private final String code;
    private final String description;
}
