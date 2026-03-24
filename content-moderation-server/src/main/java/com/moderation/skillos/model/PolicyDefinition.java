package com.moderation.skillos.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PolicyDefinition {
    private String policyId;
    private String name;
    private List<String> skillPipeline;
    private Map<String, Object> config;
    private Map<String, Object> executionInput;
    private String version;
}
