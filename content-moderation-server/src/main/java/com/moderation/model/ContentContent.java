package com.moderation.model;

import lombok.Builder;
import lombok.Data;

/**
 * 待审核内容
 */
@Data
@Builder
public class ContentContent {
    
    /**
     * 内容 ID
     */
    private String contentId;
    
    /**
     * 内容类型
     */
    private ContentType contentType;
    
    /**
     * 内容 URL（视频/图片）
     */
    private String contentUrl;
    
    /**
     * 文本内容
     */
    private String text;
}
