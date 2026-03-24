package com.moderation.skillos.model;

import lombok.Data;

import java.util.Map;

@Data
public class SkillContext {
    private String policyId;
    private String skillId;
    private Map<String, Object> state;
    private Map<String, Object> input;
    private Map<String, Object> policyConfig;
}
