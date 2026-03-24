package com.moderation.skillos.executor;

import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("roleDetectSkillExecutor")
public class RoleDetectSkillExecutor implements SkillExecutor {
    @Override
    public SkillResult execute(SkillContext context) {
        Object roleInput = context.getInput().get("role");
        String role = roleInput == null ? "host" : String.valueOf(roleInput);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("role", role);
        output.put("trusted", "host".equalsIgnoreCase(role) || "streamer".equalsIgnoreCase(role));
        return SkillResult.success(output);
    }
}
