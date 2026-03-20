package com.moderation.promptengine.runtime;

import com.moderation.model.req.WorkflowExecuteReq;
import com.moderation.model.res.PromptDslValidateRes;
import com.moderation.model.res.WorkflowExecuteRes;
import com.moderation.promptengine.dsl.PromptDslParseService;
import com.moderation.promptengine.dsl.PromptDslValidationService;
import com.moderation.promptengine.dsl.model.PromptDslDefinition;
import com.moderation.promptengine.dsl.model.WorkflowDslDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkflowExecuteService {
    private final PromptDslValidationService promptDslValidationService;
    private final PromptDslParseService promptDslParseService;
    private final WorkflowValueResolver workflowValueResolver;
    private final ScriptNodeExecutor scriptNodeExecutor;
    private final PromptEngineLlmService promptEngineLlmService;

    public WorkflowExecuteRes execute(WorkflowExecuteReq req) {
        long start = System.currentTimeMillis();
        try {
            validateReq(req);
            PromptDslValidateRes workflowValidation = promptDslValidationService.validate(req.getWorkflowDsl());
            if (!Boolean.TRUE.equals(workflowValidation.getValid())) {
                return fail(start, String.join("; ", workflowValidation.getErrors()), List.of());
            }
            WorkflowDslDefinition workflow = promptDslParseService.parseWorkflow(req.getWorkflowDsl());
            Map<String, PromptDslDefinition> promptMap = loadPromptMap(req.getPromptDsls());
            WorkflowRuntimeContext context = buildBaseContext(workflow, req.getInputs());
            List<WorkflowExecuteRes.NodeTrace> nodeTraces = new ArrayList<>();
            for (WorkflowDslDefinition.NodeDefinition node : sortNodes(workflow)) {
                long nodeStart = System.currentTimeMillis();
                Object input = resolveNodeInput(node, context);
                Object output;
                String nodeError = null;
                try {
                    output = executeNode(node, input, context, promptMap);
                } catch (Exception e) {
                    output = null;
                    nodeError = e.getMessage();
                }
                if (nodeError != null) {
                    nodeTraces.add(WorkflowExecuteRes.NodeTrace.builder()
                            .nodeId(node.getId())
                            .nodeType(node.getType())
                            .durationMs(System.currentTimeMillis() - nodeStart)
                            .input(input)
                            .output(output)
                            .errorMessage(nodeError)
                            .build());
                    return fail(start, "节点执行失败: " + node.getId() + " - " + nodeError, nodeTraces);
                }
                context.putOutput(node.getOutputKey(), output);
                context.putNodeOutput(node.getId(), output);
                nodeTraces.add(WorkflowExecuteRes.NodeTrace.builder()
                        .nodeId(node.getId())
                        .nodeType(node.getType())
                        .durationMs(System.currentTimeMillis() - nodeStart)
                        .input(input)
                        .output(output)
                        .build());
            }
            Map<String, Object> output = resolveWorkflowOutput(workflow, context);
            return WorkflowExecuteRes.builder()
                    .success(true)
                    .durationMs(System.currentTimeMillis() - start)
                    .output(output)
                    .nodeTraces(nodeTraces)
                    .build();
        } catch (Exception e) {
            return fail(start, e.getMessage(), List.of());
        }
    }

    private WorkflowExecuteRes fail(long start, String error, List<WorkflowExecuteRes.NodeTrace> traces) {
        return WorkflowExecuteRes.builder()
                .success(false)
                .durationMs(System.currentTimeMillis() - start)
                .errorMessage(error)
                .output(Map.of())
                .nodeTraces(traces)
                .build();
    }

    private void validateReq(WorkflowExecuteReq req) {
        if (req == null) throw new IllegalArgumentException("请求不能为空");
        if (req.getWorkflowDsl() == null || req.getWorkflowDsl().isBlank()) throw new IllegalArgumentException("workflowDsl 不能为空");
    }

    private Map<String, PromptDslDefinition> loadPromptMap(List<String> promptDsls) {
        Map<String, PromptDslDefinition> map = new LinkedHashMap<>();
        if (promptDsls == null) return map;
        for (String promptDsl : promptDsls) {
            PromptDslValidateRes validation = promptDslValidationService.validate(promptDsl);
            if (!Boolean.TRUE.equals(validation.getValid())) {
                throw new IllegalArgumentException("promptDsl 非法: " + String.join("; ", validation.getErrors()));
            }
            PromptDslDefinition prompt = promptDslParseService.parsePrompt(promptDsl);
            map.put(prompt.getId(), prompt);
        }
        return map;
    }

    private WorkflowRuntimeContext buildBaseContext(WorkflowDslDefinition workflow, Map<String, Object> inputMap) {
        WorkflowRuntimeContext context = new WorkflowRuntimeContext();
        Map<String, Object> inputs = inputMap == null ? Map.of() : inputMap;
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            context.putContext(entry.getKey(), entry.getValue());
        }
        if (workflow.getContext() != null && workflow.getContext().getExternal() != null) {
            for (WorkflowDslDefinition.ExternalSourceDefinition ext : workflow.getContext().getExternal()) {
                if (ext == null || ext.getName() == null || ext.getName().isBlank() || ext.getUrl() == null || ext.getUrl().isBlank()) continue;
                Object value = new RestTemplate().getForObject(renderText(ext.getUrl(), context), Object.class);
                context.putExternal(ext.getName(), value);
            }
        }
        if (workflow.getContext() != null && workflow.getContext().getComputed() != null) {
            for (Map.Entry<String, String> entry : workflow.getContext().getComputed().entrySet()) {
                context.putContext(entry.getKey(), workflowValueResolver.resolveExpression(entry.getValue(), context));
            }
        }
        return context;
    }

    private List<WorkflowDslDefinition.NodeDefinition> sortNodes(WorkflowDslDefinition workflow) {
        Map<String, WorkflowDslDefinition.NodeDefinition> nodes = new LinkedHashMap<>();
        for (WorkflowDslDefinition.NodeDefinition node : workflow.getNodes()) nodes.put(node.getId(), node);
        Map<String, Integer> indegree = new LinkedHashMap<>();
        Map<String, List<String>> next = new LinkedHashMap<>();
        for (String nodeId : nodes.keySet()) {
            indegree.put(nodeId, 0);
            next.put(nodeId, new ArrayList<>());
        }
        if (workflow.getEdges() != null) {
            for (WorkflowDslDefinition.EdgeDefinition edge : workflow.getEdges()) {
                if (!nodes.containsKey(edge.getFrom()) || !nodes.containsKey(edge.getTo())) continue;
                next.get(edge.getFrom()).add(edge.getTo());
                indegree.put(edge.getTo(), indegree.get(edge.getTo()) + 1);
            }
        }
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }
        List<WorkflowDslDefinition.NodeDefinition> ordered = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            if (!visited.add(nodeId)) continue;
            ordered.add(nodes.get(nodeId));
            for (String child : next.get(nodeId)) {
                indegree.put(child, indegree.get(child) - 1);
                if (indegree.get(child) == 0) queue.add(child);
            }
        }
        if (ordered.size() != nodes.size()) {
            throw new IllegalArgumentException("workflow 存在环或无效依赖");
        }
        return ordered;
    }

    private Object resolveNodeInput(WorkflowDslDefinition.NodeDefinition node, WorkflowRuntimeContext context) {
        if ("tool".equalsIgnoreCase(node.getType())) {
            Map<String, Object> input = new LinkedHashMap<>();
            if (node.getInput() != null) {
                for (Map.Entry<String, String> entry : node.getInput().entrySet()) {
                    input.put(entry.getKey(), workflowValueResolver.resolveTemplateValue(entry.getValue(), context));
                }
            }
            return input;
        }
        Map<String, Object> input = new LinkedHashMap<>();
        if (node.getInputMapping() != null) {
            for (Map.Entry<String, String> entry : node.getInputMapping().entrySet()) {
                input.put(entry.getKey(), workflowValueResolver.resolveTemplateValue(entry.getValue(), context));
            }
        }
        return input;
    }

    @SuppressWarnings("unchecked")
    private Object executeNode(
            WorkflowDslDefinition.NodeDefinition node,
            Object input,
            WorkflowRuntimeContext context,
            Map<String, PromptDslDefinition> promptMap
    ) {
        String nodeType = node.getType() == null ? "" : node.getType().trim().toLowerCase();
        if ("llm".equals(nodeType)) {
            PromptDslDefinition prompt = promptMap.get(node.getPromptRef());
            if (prompt == null) throw new IllegalArgumentException("未找到 promptRef: " + node.getPromptRef());
            Map<String, Object> in = input instanceof Map<?, ?> ? (Map<String, Object>) input : Map.of();
            String system = prompt.getTemplate() == null ? "" : renderText(prompt.getTemplate().getSystem(), context, in);
            String instruction = prompt.getTemplate() == null ? "" : renderText(prompt.getTemplate().getInstruction(), context, in);
            String finalPrompt = (system == null || system.isBlank()) ? instruction : system + "\n\n" + instruction;
            Double temperature = prompt.getConfig() == null ? null : prompt.getConfig().getTemperature();
            Integer maxTokens = prompt.getConfig() == null ? null : prompt.getConfig().getMaxTokens();
            String model = prompt.getModel() == null ? null : prompt.getModel().getName();
            return promptEngineLlmService.chat(finalPrompt, model, temperature, maxTokens);
        }
        if ("tool".equals(nodeType)) {
            if ("echo".equalsIgnoreCase(node.getTool())) return input;
            throw new IllegalArgumentException("暂不支持 tool: " + node.getTool());
        }
        if ("script".equals(nodeType)) {
            return scriptNodeExecutor.execute(node.getRuntime(), node.getCode(), context);
        }
        throw new IllegalArgumentException("不支持节点类型: " + nodeType);
    }

    private Map<String, Object> resolveWorkflowOutput(WorkflowDslDefinition workflow, WorkflowRuntimeContext context) {
        Map<String, Object> output = new LinkedHashMap<>();
        if (workflow.getOutput() == null) return output;
        for (Map.Entry<String, String> entry : workflow.getOutput().entrySet()) {
            output.put(entry.getKey(), workflowValueResolver.resolveTemplateValue(entry.getValue(), context));
        }
        return output;
    }

    private String renderText(String template, WorkflowRuntimeContext context) {
        return renderText(template, context, Map.of());
    }

    private String renderText(String template, WorkflowRuntimeContext context, Map<String, Object> localInput) {
        if (template == null || template.isBlank()) return "";
        String out = template;
        for (Map.Entry<String, Object> entry : localInput.entrySet()) {
            out = out.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        Object resolved = workflowValueResolver.resolveTemplateValue(out, context);
        return resolved == null ? "" : String.valueOf(resolved);
    }
}
