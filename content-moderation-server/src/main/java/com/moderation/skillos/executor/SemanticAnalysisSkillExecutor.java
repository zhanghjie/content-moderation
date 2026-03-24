package com.moderation.skillos.executor;

import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component("semanticAnalysisSkillExecutor")
public class SemanticAnalysisSkillExecutor implements SkillExecutor {
    @Override
    @SuppressWarnings("unchecked")
    public SkillResult execute(SkillContext context) {
        Map<String, Object> asr = (Map<String, Object>) context.getState().get("asr");
        String transcript = asr == null ? "" : String.valueOf(asr.getOrDefault("transcript", ""));
        String normalized = transcript.toLowerCase(Locale.ROOT);
        double score = (normalized.contains("床") || normalized.contains("私下") || normalized.contains("约"))
                ? 0.92D
                : 0.12D;
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("transcript", transcript);
        output.put("callInBedScore", score);
        output.put("riskTermsMatched", score > 0.8D);
        return SkillResult.success(output);
    }
}
