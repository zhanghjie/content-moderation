package com.moderation.skillos.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillResult {
    private boolean success;
    private Object output;
    private String message;

    public static SkillResult success(Object output) {
        return SkillResult.builder().success(true).output(output).build();
    }

    public static SkillResult failed(String message) {
        return SkillResult.builder().success(false).message(message).build();
    }
}
