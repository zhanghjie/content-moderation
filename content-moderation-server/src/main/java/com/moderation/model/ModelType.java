package com.moderation.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模型类型枚举
 */
@Getter
@AllArgsConstructor
public enum ModelType {
    
    BYTEPLUS_VIDEO("byteplus-video", "豆包视频理解"),
    BYTEPLUS_IMAGE("byteplus-image", "豆包图片理解"),
    BYTEPLUS_TEXT("byteplus-text", "豆包文本理解"),
    BACKUP_MODEL("backup", "备用模型");
    
    private final String code;
    private final String description;
}
