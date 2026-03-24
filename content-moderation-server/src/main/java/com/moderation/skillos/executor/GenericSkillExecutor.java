package com.moderation.skillos.executor;

import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("genericSkillExecutor")
public class GenericSkillExecutor implements SkillExecutor {
    @Override
    public SkillResult execute(SkillContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("skillId", context.getSkillId());
        output.put("message", "该 Skill 尚未绑定专用执行器，已由通用执行器兜底");
        output.put("input", context.getInput());
        return SkillResult.success(output);
    }
}
