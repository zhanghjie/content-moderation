package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
public class VideoDraftRes {
    private String taskId;
    private String policyId;
    private String analysisType;
    private Map<String, Object> policyInput;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

