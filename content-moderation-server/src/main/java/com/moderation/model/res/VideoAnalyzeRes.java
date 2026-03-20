package com.moderation.model.res;

import com.moderation.model.dto.ViolationDTO;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 视频分析响应
 */
@Data
@Builder
public class VideoAnalyzeRes {
    
    private String taskId;
    
    private String callId;

    private String contentId;

    private String videoUrl;

    private String coverUrl;
    
    private String status;

    /**
     * 分析类型：STANDARD / HOST_VIOLATION
     */
    private String analysisType;

    /**
     * 主播 userId（主播违规识别场景）
     */
    private Long userId;
    
    /**
     * 违规命中结果：NOT_HIT, HIT, SUSPECTED
     */
    private String moderationResult;
    
    private List<ViolationDTO> violations;
    
    private VideoSummaryDTO summary;

    /**
     * 总体置信度（0-1）
     */
    private Double overallConfidence;

    /**
     * 本次使用的 Prompt 模块列表（逗号分隔）
     */
    private String promptModules;

    private OffsetDateTime createdAt;

    private OffsetDateTime completedAt;

    private String errorMessage;

    /**
     * LLM 返回的原始 JSON（用于调试/复现）
     */
    private String resultJson;
    
    @Data
    public static class VideoSummaryDTO {
        private Integer totalViolations;
        private Integer highConfidenceCount;
        private String primaryViolation;
        private Integer videoDurationSec;
        private Double overallConfidence;
    }
}
