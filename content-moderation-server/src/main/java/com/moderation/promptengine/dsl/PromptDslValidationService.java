package com.moderation.promptengine.dsl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.model.res.PromptDslValidateRes;
import com.moderation.promptengine.dsl.model.PromptDslDefinition;
import com.moderation.promptengine.dsl.model.WorkflowDslDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PromptDslValidationService {

    private final ObjectMapper objectMapper;
    private final PromptDslParseService promptDslParseService;

    public PromptDslValidateRes validate(String rawDsl) {
        List<String> errors = new ArrayList<>();
        if (rawDsl == null || rawDsl.isBlank()) {
            errors.add("dsl 不能为空");
            return PromptDslValidateRes.builder().dslType("UNKNOWN").valid(false).errors(errors).build();
        }
        Object loaded;
        try {
            loaded = promptDslParseService.parseAndNormalize(rawDsl);
        } catch (Exception e) {
            errors.add("YAML 解析失败: " + e.getMessage());
            return PromptDslValidateRes.builder().dslType("UNKNOWN").valid(false).errors(errors).build();
        }
        if (!(loaded instanceof Map<?, ?> map)) {
            errors.add("dsl 必须是对象结构");
            return PromptDslValidateRes.builder().dslType("UNKNOWN").valid(false).errors(errors).build();
        }
        Map<String, Object> normalized = castToStringObjectMap(map);
        String type = asString(normalized.get("type")).toUpperCase(Locale.ROOT);
        if (type.isBlank()) {
            errors.add("type 不能为空");
            return PromptDslValidateRes.builder().dslType("UNKNOWN").valid(false).errors(errors).build();
        }
        if ("PROMPT".equals(type)) {
            validatePrompt(normalized, errors);
        } else if ("WORKFLOW".equals(type)) {
            validateWorkflow(normalized, errors);
        } else {
            errors.add("type 仅支持 prompt/workflow");
        }
        return PromptDslValidateRes.builder()
                .dslType(type)
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private void validatePrompt(Map<String, Object> normalized, List<String> errors) {
        PromptDslDefinition prompt = objectMapper.convertValue(normalized, PromptDslDefinition.class);
        if (isBlank(prompt.getId())) errors.add("prompt.id 不能为空");
        if (prompt.getVersion() == null || prompt.getVersion() <= 0) errors.add("prompt.version 必须大于 0");
        if (prompt.getModel() == null || isBlank(prompt.getModel().getName())) errors.add("prompt.model.name 不能为空");
        if (prompt.getTemplate() == null || isBlank(prompt.getTemplate().getInstruction())) errors.add("prompt.template.instruction 不能为空");
        if (prompt.getInputSchema() == null || prompt.getInputSchema().isEmpty()) errors.add("prompt.input_schema 不能为空");
    }

    private void validateWorkflow(Map<String, Object> normalized, List<String> errors) {
        if (!normalized.containsKey("output") && normalized.containsKey("outputMapping")) {
            normalized.put("output", normalized.get("outputMapping"));
        }
        WorkflowDslDefinition workflow = objectMapper.convertValue(normalized, WorkflowDslDefinition.class);
        if (isBlank(workflow.getId())) errors.add("workflow.id 不能为空");
        if (workflow.getVersion() == null || workflow.getVersion() <= 0) errors.add("workflow.version 必须大于 0");
        if (workflow.getContext() == null || workflow.getContext().getInputs() == null) errors.add("workflow.context.inputs 不能为空");
        if (workflow.getNodes() == null || workflow.getNodes().isEmpty()) errors.add("workflow.nodes 不能为空");
        if (workflow.getOutput() == null || workflow.getOutput().isEmpty()) errors.add("workflow.output 不能为空");
        List<WorkflowDslDefinition.NodeDefinition> nodes = workflow.getNodes();
        if (nodes == null || nodes.isEmpty()) return;
        Set<String> nodeIds = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            WorkflowDslDefinition.NodeDefinition node = nodes.get(i);
            String path = "workflow.nodes[" + i + "]";
            if (isBlank(node.getId())) {
                errors.add(path + ".id 不能为空");
            } else if (!nodeIds.add(node.getId())) {
                errors.add(path + ".id 不能重复");
            }
            if (isBlank(node.getType())) {
                errors.add(path + ".type 不能为空");
                continue;
            }
            if (isBlank(node.getOutputKey())) errors.add(path + ".output_key 不能为空");
            String type = node.getType().trim().toLowerCase(Locale.ROOT);
            if ("llm".equals(type)) {
                if (isBlank(node.getPromptRef())) errors.add(path + ".prompt_ref 不能为空");
                if (node.getInputMapping() == null || node.getInputMapping().isEmpty()) errors.add(path + ".input_mapping 不能为空");
            } else if ("tool".equals(type)) {
                if (isBlank(node.getTool())) errors.add(path + ".tool 不能为空");
                if (node.getInput() == null || node.getInput().isEmpty()) errors.add(path + ".input 不能为空");
            } else if ("script".equals(type)) {
                if (isBlank(node.getRuntime())) errors.add(path + ".runtime 不能为空");
                if (isBlank(node.getCode())) errors.add(path + ".code 不能为空");
            } else {
                errors.add(path + ".type 仅支持 llm/tool/script");
            }
        }
        if (workflow.getEdges() == null || workflow.getEdges().isEmpty()) return;
        for (int i = 0; i < workflow.getEdges().size(); i++) {
            WorkflowDslDefinition.EdgeDefinition edge = workflow.getEdges().get(i);
            String path = "workflow.edges[" + i + "]";
            if (isBlank(edge.getFrom()) || isBlank(edge.getTo())) {
                errors.add(path + " from/to 不能为空");
                continue;
            }
            if (!nodeIds.contains(edge.getFrom())) errors.add(path + ".from 节点不存在");
            if (!nodeIds.contains(edge.getTo())) errors.add(path + ".to 节点不存在");
        }
    }

    private Map<String, Object> castToStringObjectMap(Map<?, ?> map) {
        return objectMapper.convertValue(map, new TypeReference<>() {});
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
