package com.moderation.model.res;

import com.moderation.skillos.model.SkillDefinition;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SkillListRes {
    private List<SkillDefinition> skills;
}
