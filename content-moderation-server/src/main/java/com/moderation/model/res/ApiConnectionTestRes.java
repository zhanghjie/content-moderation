package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiConnectionTestRes {
    private Boolean success;
    private Integer statusCode;
    private String message;
}

