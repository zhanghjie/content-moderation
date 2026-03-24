package com.moderation.entity;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SkillDefinitionEntity {
    private String skillId;
    private String name;
    private String type;
    private String description;
    private String tagsJson;
    private String outputSchemaJson;
    private String stateMappingJson;
    private String executionConfigJson;
    private String scriptConfigJson;
    private String status;
    private Integer timeoutMs;
    private String version;
    private String executorBean;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
