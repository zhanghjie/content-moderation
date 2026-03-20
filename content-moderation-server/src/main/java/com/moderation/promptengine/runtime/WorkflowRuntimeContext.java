package com.moderation.promptengine.runtime;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class WorkflowRuntimeContext {
    private final Map<String, Object> context = new LinkedHashMap<>();
    private final Map<String, Object> external = new LinkedHashMap<>();
    private final Map<String, Object> outputs = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> nodes = new LinkedHashMap<>();

    public void putContext(String key, Object value) {
        context.put(key, value);
    }

    public void putExternal(String key, Object value) {
        external.put(key, value);
    }

    public void putOutput(String key, Object value) {
        outputs.put(key, value);
    }

    public void putNodeOutput(String nodeId, Object value) {
        nodes.put(nodeId, Map.of("output", value));
    }
}
