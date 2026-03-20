package com.moderation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("prompt_module")
public class PromptModuleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String analysisType;

    private String code;

    private String title;

    private String category;

    private String content;

    private Boolean enabled;

    private Integer sortOrder;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}

