package com.moderation.skillos.executor;

import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component("videoParseSkillExecutor")
public class VideoParseSkillExecutor implements SkillExecutor {
    @Override
    public SkillResult execute(SkillContext context) {
        String videoUrl = String.valueOf(context.getInput().getOrDefault("videoUrl", ""));
        String normalized = videoUrl.toLowerCase(Locale.ROOT);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("videoUrl", videoUrl);
        output.put("durationSec", normalized.contains("long") ? 180 : 56);
        output.put("blackFrameRatio", normalized.contains("black") ? 0.78D : 0.16D);
        output.put("audioTrack", !normalized.contains("mute"));
        return SkillResult.success(output);
    }
}
