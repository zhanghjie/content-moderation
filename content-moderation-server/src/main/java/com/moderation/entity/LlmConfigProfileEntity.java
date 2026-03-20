package com.moderation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("llm_config_profile")
public class LlmConfigProfileEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configCode;

    private String displayName;

    private String provider;

    private String endpoint;

    private String model;

    private String apiKeyEnc;

    private Integer timeoutMs;

    private Integer maxTokens;

    private Boolean enabled;

    private Boolean isDefault;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
