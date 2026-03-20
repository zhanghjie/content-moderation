package com.moderation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 违规事件实体
 */
@Data
@TableName("violation_event")
public class ViolationEventEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String eventId;
    
    private String taskId;

    private String callId;
    
    private String contentId;

    private Long userId;
    
    private String violationType;
    
    private Double confidence;
    
    private String evidence;
    
    private Integer startSec;
    
    private Integer endSec;
    
    private String promptVersion;
    
    private String modelVersion;
    
    private Boolean processed;
    
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
