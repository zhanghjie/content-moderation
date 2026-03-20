package com.moderation.model.req;

import lombok.Data;

@Data
public class PromptSplitReq {
    private String analysisType;
    private String rawPrompt;
    private String llmConfigCode;
    private Boolean applyToScene;
}
