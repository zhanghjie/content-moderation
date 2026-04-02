package com.moderation.skillos.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.llm.GeminiApiSupport;
import com.moderation.service.LlmProfileService;
import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.model.SkillResult;
import com.moderation.skillos.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("geminiSkillExecutor")
@RequiredArgsConstructor
@Slf4j
public class GeminiSkillExecutor implements SkillExecutor {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("(?:\\{\\{|#\\{)\\s*([^}]+?)\\s*(?:\\}\\}|\\})");

    private final SkillRegistry skillRegistry;
    private final LlmProfileService llmProfileService;
    private final ObjectMapper objectMapper;

    @Override
    public SkillResult execute(SkillContext context) {
        SkillDefinition definition = skillRegistry.get(context.getSkillId());
        Map<String, Object> scriptConfig = definition.getScriptConfig() == null ? Map.of() : definition.getScriptConfig();
        Map<String, Object> executionConfig = definition.getExecutionConfig() == null ? Map.of() : definition.getExecutionConfig();
        SkillContext promptContext = buildPromptContext(context, definition, scriptConfig);
        String prompt = buildPrompt(promptContext, scriptConfig, definition);
        String modelName = stringValue(executionConfig.get("llm_model"));
        String configCode = stringValue(executionConfig.get("llm_config_code"));
        Double temperature = numberToDouble(executionConfig.get("temperature"));
        Integer maxTokens = numberToInteger(executionConfig.get("max_tokens"));
        LlmProfileService.LlmRuntimeProfile profile = resolveProfile(configCode);
        String endpoint = profile == null ? GeminiApiSupport.DEFAULT_ENDPOINT : profile.endpoint();
        String apiKey = profile == null ? "" : profile.apiKey();
        String model = modelName.isBlank() ? (profile == null ? "" : profile.model()) : modelName;
        Integer timeout = profile == null ? 120000 : profile.timeoutMs();
        boolean geminiRequest = GeminiApiSupport.isGeminiProvider(profile == null ? null : profile.provider())
                || GeminiApiSupport.isGeminiEndpoint(endpoint);
        if (geminiRequest) {
            model = GeminiApiSupport.resolveTestModel(profile == null ? null : profile.provider(), endpoint, model);
        }
        String finalEndpoint = GeminiApiSupport.buildGenerateContentEndpoint(endpoint, model, apiKey);
        log.info(
                "Gemini skill execute, skillId: {}, model: {}, configCode: {}, endpoint: {}, timeoutMs: {}, promptChars: {}",
                context.getSkillId(),
                model,
                configCode,
                finalEndpoint,
                timeout == null ? 120000 : timeout,
                prompt == null ? 0 : prompt.length()
        );
        log.info("Gemini skill prompt preview, skillId: {}, prompt: {}", context.getSkillId(), previewPrompt(prompt));
        String content = invokeGemini(finalEndpoint, prompt, temperature, maxTokens, timeout);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("skillId", context.getSkillId());
        output.put("model", model);
        output.put("configCode", configCode);

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

            Map<String, Object> parsed = objectMapper.readValue(jsonContent, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            });
            output.putAll(parsed);
            output.put("_rawLlmResult", content);
        } catch (Exception e) {
            output.put("llmResult", content);
            output.put("_parseError", "LLM结果不是合法的JSON对象: " + e.getMessage());
        }

        return SkillResult.success(output);
    }

    private String invokeGemini(String endpoint, String prompt, Double temperature, Integer maxTokens, Integer timeoutMs) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = GeminiApiSupport.buildGenerateContentRequest(
                prompt,
                temperature,
                maxTokens == null ? 3000 : maxTokens
        );
        ResponseEntity<Map> response = createRestTemplate(timeoutMs == null ? 120000 : timeoutMs)
                .postForEntity(endpoint, new HttpEntity<>(requestBody, headers), Map.class);
        return GeminiApiSupport.extractText(response.getBody());
    }

    private LlmProfileService.LlmRuntimeProfile resolveProfile(String configCode) {
        if (configCode != null && !configCode.isBlank()) {
            return llmProfileService.findByCode(configCode.trim()).orElse(null);
        }
        return llmProfileService.findDefaultEnabled().orElse(null);
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
        return null;
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

    private RestTemplate createRestTemplate(int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }
}
