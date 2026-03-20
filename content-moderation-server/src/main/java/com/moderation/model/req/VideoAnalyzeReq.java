package com.moderation.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 视频分析请求
 */
@Data
@Builder
public class VideoAnalyzeReq {

    /**
     * 调用 ID，唯一标识一次分析请求
     */
    @NotBlank(message = "callId is required")
    private String callId;

    /**
     * 内容 ID，视频内容的唯一标识
     */
    @NotBlank(message = "contentId is required")
    private String contentId;

    /**
     * 视频 URL，支持 http/https 协议
     */
    @NotBlank(message = "videoUrl is required")
    @Pattern(regexp = "^https?://.+", message = "videoUrl must be a valid URL starting with http or https")
    private String videoUrl;

    /**
     * 封面 URL（可选）
     */
    @Pattern(regexp = "^https?://.*", message = "coverUrl must be a valid URL starting with http or https")
    private String coverUrl;

    /**
     * 分析类型（STANDARD / HOST_VIOLATION）
     */
    private String analysisType;

    /**
     * 主播 userId（主播违规识别场景）
     */
    private Long userId;

    /**
     * Prompt 模块编码列表（可选，不传则使用默认组合）
     */
    private List<String> promptModules;
}
