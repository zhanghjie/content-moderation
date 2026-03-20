package com.moderation.promptengine.dsl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.promptengine.dsl.model.PromptDslDefinition;
import com.moderation.promptengine.dsl.model.WorkflowDslDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PromptDslParseService {

    private final ObjectMapper objectMapper;

    public PromptDslDefinition parsePrompt(String dsl) {
        Map<String, Object> normalized = parseAndNormalize(dsl);
        return objectMapper.convertValue(normalized, PromptDslDefinition.class);
    }

    public WorkflowDslDefinition parseWorkflow(String dsl) {
        Map<String, Object> normalized = parseAndNormalize(dsl);
        if (!normalized.containsKey("output") && normalized.containsKey("outputMapping")) {
            normalized.put("output", normalized.get("outputMapping"));
        }
        return objectMapper.convertValue(normalized, WorkflowDslDefinition.class);
    }

    public Map<String, Object> parseAndNormalize(String dsl) {
        Object loaded = new Yaml().load(dsl);
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("dsl 必须是对象结构");
        }
        return normalizeMap(castToStringObjectMap(map));
    }

    private Map<String, Object> normalizeMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(snakeToCamel(entry.getKey()), normalizeValue(entry.getValue()));
        }
        return result;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return normalizeMap(castToStringObjectMap(map));
        }
        if (value instanceof List<?> list) {
            List<Object> items = new ArrayList<>(list.size());
            for (Object item : list) items.add(normalizeValue(item));
            return items;
        }
        return value;
    }

    private Map<String, Object> castToStringObjectMap(Map<?, ?> map) {
        return objectMapper.convertValue(map, new TypeReference<>() {});
    }

    private String snakeToCamel(String key) {
        if (key == null || key.isBlank()) return "";
        String[] parts = key.split("_");
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isBlank()) continue;
            builder.append(parts[i].substring(0, 1).toUpperCase(Locale.ROOT)).append(parts[i].substring(1));
        }
        return builder.toString();
    }
}
