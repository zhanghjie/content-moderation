package com.moderation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("policy_execution_step")
public class PolicyExecutionStepEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private String executionId;
    private String stepId;
    private String skillId;
    private String status;
    private Integer attempt;
    private Long durationMs;
    private String inputJson;
    private String outputJson;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime createdAt;
}
