package com.moderation.skillos.executor;

import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component("violationAggregateSkillExecutor")
public class ViolationAggregateSkillExecutor implements SkillExecutor {
    @Override
    @SuppressWarnings("unchecked")
    public SkillResult execute(SkillContext context) {
        Map<String, Object> callInBed = (Map<String, Object>) context.getState().get("violation_call_in_bed");
        Map<String, Object> blackScreen = (Map<String, Object>) context.getState().get("violation_black_screen");

        boolean callInBedViolated = callInBed != null && Boolean.TRUE.equals(callInBed.get("violated"));
        boolean blackScreenViolated = blackScreen != null && Boolean.TRUE.equals(blackScreen.get("violated"));

        List<String> reasons = new ArrayList<>();
        if (callInBedViolated) reasons.add(String.valueOf(callInBed.get("reason")));
        if (blackScreenViolated) reasons.add(String.valueOf(blackScreen.get("reason")));

        String riskLevel = reasons.isEmpty() ? "LOW" : reasons.size() == 1 ? "MEDIUM" : "HIGH";
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("riskLevel", riskLevel);
        output.put("riskDetected", !reasons.isEmpty());
        output.put("reasons", reasons);
        return SkillResult.success(output);
    }
}
