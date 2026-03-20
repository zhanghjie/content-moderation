package com.moderation.model.req;

import lombok.Data;

import java.util.List;

@Data
public class PromptComposePreviewReq {
    private String analysisType;
    private String callId;
    private String contentId;
    private String videoUrl;
    private Long userId;
    private List<String> modules;
}

