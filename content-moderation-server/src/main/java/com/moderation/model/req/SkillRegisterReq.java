package com.moderation.model.req;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SkillRegisterReq {
    private String skillId;
    private String name;
    private String type;
    private String description;
    private List<String> tags;
    private Map<String, Object> outputSchema;
    private Map<String, Object> stateMapping;
    private Map<String, Object> executionConfig;
    private Map<String, Object> scriptConfig;
    private String status;
    private Integer timeoutMs;
    private String version;
}
