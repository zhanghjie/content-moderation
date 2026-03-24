package com.moderation.skillos.executor;

import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillResult;

public interface SkillExecutor {
    SkillResult execute(SkillContext context);
}
