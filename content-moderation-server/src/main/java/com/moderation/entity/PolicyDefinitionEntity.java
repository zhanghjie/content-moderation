package com.moderation.entity;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PolicyDefinitionEntity {
    private String policyId;
    private String name;
    private String skillPipelineJson;
    private String configJson;
    private String executionInputJson;
    private String version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
