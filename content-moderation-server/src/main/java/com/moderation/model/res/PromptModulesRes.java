package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PromptModulesRes {

    private String analysisType;

    private List<String> defaultModules;

    private List<PromptModuleItem> modules;

    @Data
    @Builder
    public static class PromptModuleItem {
        private String code;
        private String title;
        private String category;
    }
}
