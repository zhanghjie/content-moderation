package com.moderation.model.req;

import lombok.Data;

import java.util.Map;

@Data
public class SkillScriptTestReq {
    private Map<String, Object> input;
    private Map<String, Object> state;
    private String scriptContent;
}
