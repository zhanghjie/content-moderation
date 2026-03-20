package com.moderation.model.req;

import lombok.Data;

@Data
public class ApiConnectionTestReq {
    private String configCode;
    private String endpoint;
    private Integer timeoutMs;
}
