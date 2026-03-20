package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PromptModuleManageRes {
    private String analysisType;
    private List<String> defaultModules;
    private List<PromptModuleDetail> modules;

    @Data
    @Builder
    public static class PromptModuleDetail {
        private String code;
        private String title;
        private String category;
        private String content;
        private Boolean enabled;
        private Integer sortOrder;
    }
}

