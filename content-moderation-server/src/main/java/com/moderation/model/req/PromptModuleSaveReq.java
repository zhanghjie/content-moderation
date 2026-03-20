package com.moderation.model.req;

import lombok.Data;

@Data
public class PromptModuleSaveReq {
    private String analysisType;
    private String code;
    private String title;
    private String category;
    private String content;
    private Boolean enabled;
    private Integer sortOrder;
}

