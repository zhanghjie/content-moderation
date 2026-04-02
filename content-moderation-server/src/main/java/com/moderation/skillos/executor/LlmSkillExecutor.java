package com.moderation.skillos.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.model.impl.LLMIntegrationService;
import com.moderation.promptengine.runtime.PromptEngineLlmService;
import com.moderation.service.LlmProfileService;
import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.model.SkillResult;
import com.moderation.skillos.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("llmSkillExecutor")
@RequiredArgsConstructor
@Slf4j
public class LlmSkillExecutor implements SkillExecutor {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("(?:\\{\\{|#\\{)\\s*([^}]+?)\\s*(?:\\}\\}|\\})");

    private final SkillRegistry skillRegistry;
    private final PromptEngineLlmService promptEngineLlmService;
    private final LLMIntegrationService llmIntegrationService;
    private final LlmProfileService llmProfileService;
    private final ObjectMapper objectMapper;
    private final PythonSkillExecutor pythonSkillExecutor;

    @Override
    public SkillResult execute(SkillContext context) {
        SkillDefinition definition = skillRegistry.get(context.getSkillId());
        Map<String, Object> scriptConfig = definition.getScriptConfig() == null ? Map.of() : definition.getScriptConfig();
        Map<String, Object> executionConfig = definition.getExecutionConfig() == null ? Map.of() : definition.getExecutionConfig();
        if (isPythonMode(executionConfig)) {
            log.warn("Skill {} configured as PYTHON but Python execution is disabled. Fallback to LLM strategy.", context.getSkillId());
        }
        SkillContext promptContext = buildPromptContext(context, definition, scriptConfig);
        String prompt = buildPrompt(promptContext, scriptConfig, definition);
        String modelName = stringValue(executionConfig.get("llm_model"));
        String configCode = stringValue(executionConfig.get("llm_config_code"));
        Double temperature = numberToDouble(executionConfig.get("temperature"));
        Integer maxTokens = numberToInteger(executionConfig.get("max_tokens"));
        String mode = resolveExecutionMode(executionConfig);
        String videoUrl = resolveVideoUrl(context);
        log.info(
                "LLM skill execute, skillId: {}, mode: {}, model: {}, configCode: {}, temperature: {}, maxTokens: {}, promptChars: {}, videoUrlPresent: {}",
                context.getSkillId(),
                mode,
                modelName,
                configCode,
                temperature,
                maxTokens,
                prompt == null ? 0 : prompt.length(),
                !videoUrl.isBlank()
        );
        log.info("LLM skill prompt preview, skillId: {}, prompt: {}", context.getSkillId(), previewPrompt(prompt));
        String content;
        if (!videoUrl.isBlank() && isByteplusProfile(configCode)) {
            String contentId = resolveContentId(context);
            log.info("LLM skill uses BytePlus video strategy, skillId: {}, contentId: {}, videoUrl: {}", context.getSkillId(), contentId, videoUrl);
            content = llmIntegrationService.analyzeVideo(videoUrl, contentId, prompt);
        } else {
            content = promptEngineLlmService.chat(prompt, modelName, temperature, maxTokens, configCode);
        }
        
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

    private SkillContext buildPromptContext(SkillContext context, SkillDefinition definition, Map<String, Object> scriptConfig) {
        SkillContext promptContext = new SkillContext();
        promptContext.setPolicyId(context.getPolicyId());
        promptContext.setSkillId(context.getSkillId());
        promptContext.setInput(context.getInput() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(context.getInput()));
        Map<String, Object> state = context.getState() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(context.getState());
        Map<String, Object> preScriptResult = executePreScriptIfNeeded(definition, context, scriptConfig);
        if (preScriptResult != null) {
            state.put("preScriptResult", preScriptResult);
        }
        promptContext.setState(state);
        promptContext.setPolicyConfig(context.getPolicyConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(context.getPolicyConfig()));
        return promptContext;
    }

    private Map<String, Object> executePreScriptIfNeeded(SkillDefinition definition, SkillContext context, Map<String, Object> scriptConfig) {
        // 临时禁用 Python 前置脚本执行能力
        // if (!isPythonScriptConfig(scriptConfig)) {
        //     return null;
        // }
        // SkillResult preScriptResult = pythonSkillExecutor.execute(context);
        // Map<String, Object> preScriptState = new LinkedHashMap<>();
        // preScriptState.put("skillId", definition.getSkillId());
        // preScriptState.put("success", preScriptResult != null && preScriptResult.isSuccess());
        // preScriptState.put("message", preScriptResult == null ? null : preScriptResult.getMessage());
        // preScriptState.put("output", preScriptResult == null ? null : preScriptResult.getOutput());
        // return preScriptState;
        return null;
    }

    private boolean isPythonScriptConfig(Map<String, Object> scriptConfig) {
        String language = stringValue(scriptConfig.get("language"));
        if ("python".equalsIgnoreCase(language)) {
            return true;
        }
        String fileName = stringValue(scriptConfig.get("fileName"));
        if (fileName.isBlank()) {
            fileName = stringValue(scriptConfig.get("scriptFileName"));
        }
        return fileName.toLowerCase().endsWith(".py");
    }

    private boolean isPythonMode(Map<String, Object> executionConfig) {
        String mode = resolveExecutionMode(executionConfig);
        return "PYTHON".equals(mode) || "PY".equals(mode) || "PY_SCRIPT".equals(mode);
    }

    private String resolveExecutionMode(Map<String, Object> executionConfig) {
        Object modeValue = executionConfig.get("execution_mode");
        if (modeValue == null) {
            modeValue = executionConfig.get("executionMode");
        }
        if (modeValue == null) {
            modeValue = executionConfig.get("mode");
        }
        return String.valueOf(modeValue == null ? "" : modeValue).trim().toUpperCase();
    }

    private String previewPrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        String normalized = prompt.replace("\r", "\\r").replace("\n", "\\n");
        int limit = 1200;
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit) + "...(truncated)";
    }

    private String resolveVideoUrl(SkillContext context) {
        if (context == null || context.getInput() == null) {
            return "";
        }
        Object value = context.getInput().get("videoUrl");
        if (value == null) {
            value = context.getInput().get("video_url");
        }
        return String.valueOf(value == null ? "" : value).trim();
    }

    private String resolveContentId(SkillContext context) {
        if (context != null && context.getInput() != null) {
            Object callId = context.getInput().get("callId");
            if (callId == null) {
                callId = context.getInput().get("call_id");
            }
            if (callId != null && !String.valueOf(callId).isBlank()) {
                return String.valueOf(callId).trim();
            }
        }
        return context.getPolicyId() + ":" + context.getSkillId();
    }

    private boolean isByteplusProfile(String configCode) {
        String provider = llmProfileService.findByCode(configCode).map(LlmProfileService.LlmRuntimeProfile::provider)
                .or(() -> llmProfileService.findDefaultEnabled().map(LlmProfileService.LlmRuntimeProfile::provider))
                .orElse("");
        return "byteplus".equalsIgnoreCase(provider.trim().toLowerCase(Locale.ROOT));
    }

    private String buildPrompt(SkillContext context, Map<String, Object> scriptConfig, SkillDefinition definition) {
        String basePrompt = renderTemplate(stringValue(scriptConfig.get("prompt")), context);
        if (basePrompt.isBlank()) {
            basePrompt = "你是内容审核分析助手，请基于输入和上下文输出结构化结论。";
        }
        String prompt = basePrompt + "\n\n输入:\n" + toJson(context.getInput()) + "\n\n状态:\n" + toJson(context.getState());
        Object preScriptResult = context.getState() == null ? null : context.getState().get("preScriptResult");
        if (preScriptResult != null) {
            prompt += "\n\n前置Py脚本结果:\n" + toJson(preScriptResult);
        }
        prompt += "\n\n策略配置:\n" + toJson(context.getPolicyConfig());
        
        if (definition.getOutputSchema() != null && !definition.getOutputSchema().isEmpty()) {
            prompt += "\n\n请严格按照以下 JSON Schema 输出结果（不要输出 markdown code block 以外的多余解释）：\n" + toJson(definition.getOutputSchema());
        }
        return prompt;
    }

    private String renderTemplate(String template, SkillContext context) {
        if (template == null || template.isBlank()) {
            return "";
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        if (matcher.matches()) {
            return stringify(resolveExpression(matcher.group(1), context));
        }
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            out.append(template, cursor, matcher.start());
            out.append(stringify(resolveExpression(matcher.group(1), context)));
            cursor = matcher.end();
        }
        out.append(template.substring(cursor));
        return out.toString();
    }

    private Object resolveExpression(String expression, SkillContext context) {
        if (expression == null) {
            return null;
        }
        String expr = expression.trim();
        if (expr.startsWith("input.")) {
            return resolveFromSource(context.getInput(), expr.substring("input.".length()));
        }
        if (expr.startsWith("state.")) {
            return resolveFromSource(context.getState(), expr.substring("state.".length()));
        }
        if (expr.startsWith("policyConfig.")) {
            return resolveFromSource(context.getPolicyConfig(), expr.substring("policyConfig.".length()));
        }
        Object value = resolveFromSource(context.getInput(), expr);
        if (value != null) {
            return value;
        }
        value = resolveFromSource(context.getState(), expr);
        if (value != null) {
            return value;
        }
        return resolveFromSource(context.getPolicyConfig(), expr);
    }

    private Object resolveFromSource(Map<String, Object> source, String expr) {
        if (source == null || expr == null || expr.isBlank()) {
            return null;
        }
        if (source.containsKey(expr)) {
            return source.get(expr);
        }
        int dotIndex = expr.indexOf('.');
        if (dotIndex > 0) {
            String root = expr.substring(0, dotIndex);
            if (source.containsKey(root)) {
                return pickByPath(source.get(root), expr.substring(dotIndex + 1));
            }
        }
        return findByKeyRecursively(source, expr);
    }

    private Object findByKeyRecursively(Object source, String key) {
        if (source == null || key == null || key.isBlank()) {
            return null;
        }
        if (source instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null && key.equals(String.valueOf(entry.getKey()))) {
                    return entry.getValue();
                }
            }
            for (Object value : map.values()) {
                Object nested = findByKeyRecursively(value, key);
                if (nested != null) {
                    return nested;
                }
            }
        }
        if (source instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                Object nested = findByKeyRecursively(item, key);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private Object pickByPath(Object source, String path) {
        if (source == null || path == null || path.isBlank()) {
            return null;
        }
        Object current = source;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String str) {
            return str;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
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
