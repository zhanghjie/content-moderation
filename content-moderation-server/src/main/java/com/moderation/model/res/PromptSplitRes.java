package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PromptSplitRes {
    private String analysisType;
    private String llmConfigCode;
    private String rawModelResponse;
    private List<PromptModuleManageRes.PromptModuleDetail> modules;
}
