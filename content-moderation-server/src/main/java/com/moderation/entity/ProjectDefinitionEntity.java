package com.moderation.entity;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProjectDefinitionEntity {
    private String projectId;
    private String name;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
