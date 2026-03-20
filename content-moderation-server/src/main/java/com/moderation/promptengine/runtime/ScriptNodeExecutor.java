package com.moderation.promptengine.runtime;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ScriptNodeExecutor {

    private final WorkflowValueResolver valueResolver;

    public ScriptNodeExecutor(WorkflowValueResolver valueResolver) {
        this.valueResolver = valueResolver;
    }

    public Object execute(String runtime, String code, WorkflowRuntimeContext context) {
        if (runtime == null || runtime.isBlank()) {
            throw new IllegalArgumentException("script.runtime 不能为空");
        }
        String r = runtime.trim().toLowerCase();
        if (!"js".equals(r) && !"python".equals(r)) {
            throw new IllegalArgumentException("script.runtime 仅支持 js/python");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("script.code 不能为空");
        }
        String normalized = code.trim();
        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("script.code 必须返回对象结构");
        }
        String body = normalized.substring(start + 1, end).trim();
        Map<String, Object> output = new LinkedHashMap<>();
        if (body.isBlank()) return output;
        String[] parts = body.split(",");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length < 2) continue;
            String key = kv[0].trim();
            String expr = kv[1].trim();
            if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith("\"") && key.endsWith("\""))) {
                key = key.substring(1, key.length() - 1);
            }
            Object value = valueResolver.resolveExpression(expr, context);
            output.put(key, value);
        }
        return output;
    }
}
