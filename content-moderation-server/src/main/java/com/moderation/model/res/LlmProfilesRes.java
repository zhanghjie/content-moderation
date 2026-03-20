package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LlmProfilesRes {
    private String defaultConfigCode;
    private List<LlmProfileRes> profiles;
}
