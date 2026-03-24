package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SkillTemplateListRes {
    private List<SkillTemplateRes> templates;
}
