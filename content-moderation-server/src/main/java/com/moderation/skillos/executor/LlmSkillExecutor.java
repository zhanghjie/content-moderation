package com.moderation.skillos.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.promptengine.runtime.PromptEngineLlmService;
import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.model.SkillResult;
import com.moderation.skillos.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("llmSkillExecutor")
@RequiredArgsConstructor
public class LlmSkillExecutor implements SkillExecutor {
    private final SkillRegistry skillRegistry;
    private final PromptEngineLlmService promptEngineLlmService;
    private final ObjectMapper objectMapper;

    @Override
    public SkillResult execute(SkillContext context) {
        SkillDefinition definition = skillRegistry.get(context.getSkillId());
        Map<String, Object> scriptConfig = definition.getScriptConfig() == null ? Map.of() : definition.getScriptConfig();
        Map<String, Object> executionConfig = definition.getExecutionConfig() == null ? Map.of() : definition.getExecutionConfig();
        String prompt = buildPrompt(context, scriptConfig, definition);
        String modelName = stringValue(executionConfig.get("llm_model"));
        String configCode = stringValue(executionConfig.get("llm_config_code"));
        Double temperature = numberToDouble(executionConfig.get("temperature"));
        Integer maxTokens = numberToInteger(executionConfig.get("max_tokens"));
        String content = promptEngineLlmService.chat(prompt, modelName, temperature, maxTokens, configCode);
        
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("skillId", context.getSkillId());
        output.put("model", modelName);
        output.put("configCode", configCode);
        
        // SchemaCollapse: 尝试解析 LLM 返回的 JSON
        try {
            String jsonContent = content.trim();
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring(7);
            } else if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.substring(3);
            }
            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
            }
            jsonContent = jsonContent.trim();
            
            Map<String, Object> parsed = objectMapper.readValue(jsonContent, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            output.putAll(parsed);
            output.put("_rawLlmResult", content); // 保留原始返回用于追溯
        } catch (Exception e) {
            // 解析失败则退化为字符串
            output.put("llmResult", content);
            output.put("_parseError", "LLM结果不是合法的JSON对象: " + e.getMessage());
        }
        
        return SkillResult.success(output);
    }

    private String buildPrompt(SkillContext context, Map<String, Object> scriptConfig, SkillDefinition definition) {
        String basePrompt = stringValue(scriptConfig.get("prompt"));
        if (basePrompt.isBlank()) {
            basePrompt = "你是内容审核分析助手，请基于输入和上下文输出结构化结论。";
        }
        String prompt = basePrompt + "\n\n输入:\n" + toJson(context.getInput()) + "\n\n状态:\n" + toJson(context.getState()) + "\n\n策略配置:\n" + toJson(context.getPolicyConfig());
        
        if (definition.getOutputSchema() != null && !definition.getOutputSchema().isEmpty()) {
            prompt += "\n\n请严格按照以下 JSON Schema 输出结果（不要输出 markdown code block 以外的多余解释）：\n" + toJson(definition.getOutputSchema());
        }
        return prompt;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Double numberToDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer numberToInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
