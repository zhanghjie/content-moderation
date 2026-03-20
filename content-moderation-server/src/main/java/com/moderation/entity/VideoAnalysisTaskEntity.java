package com.moderation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 视频分析任务实体
 */
@Data
@TableName("video_analysis_task")
public class VideoAnalysisTaskEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String taskId;
    
    private String callId;
    
    private String contentId;
    
    private String videoUrl;
    
    private String coverUrl;

    /**
     * 分析类型（如 STANDARD / HOST_VIOLATION）
     */
    private String analysisType;

    /**
     * 主播 userId（主播违规识别场景）
     */
    private Long userId;
    
    private String status;
    
    private Integer retryCount;
    
    private String traceId;

    /**
     * 本次使用的 Prompt 模块列表（逗号分隔）
     */
    private String promptModules;

    /**
     * 本次实际下发的 Prompt 快照
     */
    private String promptSnapshot;

    /**
     * LLM 返回的原始结果（字符串 JSON）
     */
    private String resultJson;

    /**
     * 违规命中结果（NOT_HIT/HIT/SUSPECTED）
     */
    private String moderationResult;

    /**
     * 总体置信度（0-1）
     */
    private Double overallConfidence;
    
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
    
    private OffsetDateTime completedAt;
    
    private String errorMessage;
}
