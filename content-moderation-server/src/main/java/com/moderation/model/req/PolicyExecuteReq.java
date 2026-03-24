package com.moderation.model.req;

import lombok.Data;

import java.util.Map;

@Data
public class PolicyExecuteReq {
    private String policyId;
    private Map<String, Object> input;
}
