package com.moderation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("prompt_module_set")
public class PromptModuleSetEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String analysisType;

    private String defaultModules;

    private OffsetDateTime updatedAt;
}

