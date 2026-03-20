package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PromptDslValidateRes {
    private String dslType;
    private Boolean valid;
    private List<String> errors;
}
