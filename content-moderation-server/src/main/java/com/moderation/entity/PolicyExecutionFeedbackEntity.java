package com.moderation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("policy_execution_feedback")
public class PolicyExecutionFeedbackEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String executionId;
    private String traceId;
    private String source;
    private Double score;
    private String label;
    private String action;
    private String comment;
    private String metadataJson;
    private OffsetDateTime createdAt;
}
