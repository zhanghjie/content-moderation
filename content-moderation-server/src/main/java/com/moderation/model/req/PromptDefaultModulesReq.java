package com.moderation.model.req;

import lombok.Data;

import java.util.List;

@Data
public class PromptDefaultModulesReq {
    private String analysisType;
    private List<String> defaultModules;
}

