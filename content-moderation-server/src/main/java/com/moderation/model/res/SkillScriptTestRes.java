package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SkillScriptTestRes {
    private boolean success;
    private String message;
    private Map<String, Object> output;
    private Map<String, Object> writeToState;
}
