package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class SkillTemplateRes {
    private String templateId;
    private String name;
    private String type;
    private String description;
    private List<String> tags;
    private Map<String, Object> outputSchema;
    private Map<String, Object> stateMapping;
    private Map<String, Object> executionConfig;
    private Map<String, Object> scriptConfig;
}
