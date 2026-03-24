package com.moderation.skillos.executor;

import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("violationBlackScreenSkillExecutor")
public class ViolationBlackScreenSkillExecutor implements SkillExecutor {
    @Override
    @SuppressWarnings("unchecked")
    public SkillResult execute(SkillContext context) {
        boolean enabled = readBoolean(context.getPolicyConfig(), "enableBlackScreenCheck", true);
        Map<String, Object> videoParse = (Map<String, Object>) context.getState().get("video_parse");
        double blackFrameRatio = videoParse == null ? 0D : readDouble(videoParse.get("blackFrameRatio"), 0D);
        double threshold = readDouble(context.getPolicyConfig().get("blackScreenThreshold"), 0.5D);
        boolean violated = enabled && blackFrameRatio >= threshold;

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("enabled", enabled);
        output.put("blackFrameRatio", blackFrameRatio);
        output.put("threshold", threshold);
        output.put("violated", violated);
        output.put("reason", violated ? "命中画面违规规则：黑屏占比过高" : "未命中");
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
