package com.moderation.skillos.executor;

import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component("asrSkillExecutor")
public class AsrSkillExecutor implements SkillExecutor {
    @Override
    public SkillResult execute(SkillContext context) {
        Object provided = context.getInput().get("transcript");
        String transcript;
        if (provided != null && !String.valueOf(provided).isBlank()) {
            transcript = String.valueOf(provided);
        } else {
            String videoUrl = String.valueOf(context.getInput().getOrDefault("videoUrl", "")).toLowerCase(Locale.ROOT);
            transcript = videoUrl.contains("call")
                    ? "我们今晚私下聊，来床上一起看电影"
                    : "欢迎来到直播间，今天给大家讲解课程";
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("transcript", transcript);
        output.put("language", "zh-CN");
        return SkillResult.success(output);
    }
}
