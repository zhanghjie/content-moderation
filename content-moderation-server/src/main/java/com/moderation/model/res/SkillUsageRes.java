package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SkillUsageRes {
    private String skillId;
    private List<String> policyIds;
}
