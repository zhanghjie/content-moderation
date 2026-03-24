package com.moderation.skillos.executor;

import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("violationCallInBedSkillExecutor")
public class ViolationCallInBedSkillExecutor implements SkillExecutor {
    @Override
    @SuppressWarnings("unchecked")
    public SkillResult execute(SkillContext context) {
        boolean enabled = readBoolean(context.getPolicyConfig(), "enableCallInBedCheck", true);
        Map<String, Object> semantic = (Map<String, Object>) context.getState().get("semantic_analysis");
        double score = semantic == null ? 0D : readDouble(semantic.get("callInBedScore"), 0D);
        double threshold = readDouble(context.getPolicyConfig().get("callInBedThreshold"), 0.8D);
        boolean violated = enabled && score >= threshold;

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("enabled", enabled);
        output.put("score", score);
        output.put("threshold", threshold);
        output.put("violated", violated);
        output.put("reason", violated ? "命中语义违规规则：疑似引导上床" : "未命中");
        return SkillResult.success(output);
    }

    private boolean readBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private double readDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
