package com.moderation.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PromptComposer {

    private final PromptModuleManageService promptModuleManageService;
    private final ObjectMapper objectMapper;

    public ComposedPrompt compose(String analysisType, List<String> moduleCodes, Map<String, String> variables) {
        String type = (analysisType == null || analysisType.isBlank()) ? "HOST_VIOLATION" : analysisType.trim().toUpperCase();
        List<String> selected = promptModuleManageService.resolveModules(type, moduleCodes);
        Map<String, String> contentMap = promptModuleManageService.moduleContentMap(type);

        String prompt = selected.stream()
                .map(code -> resolveModule(contentMap, code))
                .filter(Objects::nonNull)
                .map(this::renderStructuredModule)
                .map(content -> applyVariables(content, variables))
                .collect(Collectors.joining("\n\n"));

        return new ComposedPrompt(selected, prompt);
    }

    public ComposedPrompt composeHostViolation(List<String> moduleCodes, Map<String, String> variables) {
        return compose("HOST_VIOLATION", moduleCodes, variables);
    }

    private String resolveModule(Map<String, String> modules, String code) {
        if (modules == null) return null;
        return modules.get(code);
    }

    private String renderStructuredModule(String content) {
        if (content == null) return null;
        String trimmed = content.trim();
        if (!trimmed.startsWith("{")) return content;
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if ("RULE_CHECKLIST".equalsIgnoreCase(node.path("type").asText())) {
                JsonNode items = node.path("items");
                if (!items.isArray()) return "";
                List<String> lines = new java.util.ArrayList<>();
                for (JsonNode item : items) {
                    boolean enabled = item.path("enabled").asBoolean(false);
                    String text = item.path("text").asText("").trim();
                    if (enabled && !text.isBlank()) lines.add("- " + text);
                }
                return String.join("\n", lines);
            }
            return content;
        } catch (Exception ignored) {
            return content;
        }
    }

    private String applyVariables(String content, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) return content;
        String result = content;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{{" + key + "}}", value);
        }
        return result;
    }

    public record ComposedPrompt(List<String> modules, String prompt) {}
}
