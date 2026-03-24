package com.moderation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("policy_execution")
public class PolicyExecutionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String executionId;
    private String planId;
    private String policyId;
    private String status;
    private Long durationMs;
    private String planSnapshotJson;
    private String stateJson;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
