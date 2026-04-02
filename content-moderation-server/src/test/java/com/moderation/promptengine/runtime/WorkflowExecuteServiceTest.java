package com.moderation.promptengine.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.model.req.WorkflowExecuteReq;
import com.moderation.model.res.WorkflowExecuteRes;
import com.moderation.promptengine.dsl.PromptDslParseService;
import com.moderation.promptengine.dsl.PromptDslValidationService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WorkflowExecuteServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PromptDslParseService parseService = new PromptDslParseService(new ObjectMapper());
    private final PromptDslValidationService validationService = new PromptDslValidationService(objectMapper, parseService);
    private final WorkflowValueResolver valueResolver = new WorkflowValueResolver(objectMapper);
    private final ScriptNodeExecutor scriptNodeExecutor = new ScriptNodeExecutor(valueResolver);
    private final PromptEngineLlmService llmService = mock(PromptEngineLlmService.class);
    private final WorkflowExecuteService workflowExecuteService = new WorkflowExecuteService(
            validationService,
            parseService,
            valueResolver,
            scriptNodeExecutor,
            llmService,
            objectMapper
    );

    @Test
    void should_execute_script_workflow_successfully() {
        WorkflowExecuteReq req = new WorkflowExecuteReq();
        req.setWorkflowDsl("""
                id: generic_ai_workflow
                type: workflow
                version: 1
                context:
                  inputs:
                    text: string
                  computed:
                    is_long_text: "len(text) > 3"
                nodes:
                  - id: enrich
                    type: script
                    runtime: js
                    code: |
                      return {
                        result: text,
                        long: is_long_text
                      }
                    output_key: enriched
                output:
                  result: "{{enriched}}"
                """);
        req.setInputs(Map.of("text", "hello"));

        WorkflowExecuteRes res = workflowExecuteService.execute(req);

        assertThat(res.getSuccess()).isTrue();
        assertThat(res.getOutput()).containsKey("result");
        assertThat(res.getNodeTraces()).hasSize(1);
    }

    @Test
    void should_return_failed_when_workflow_has_cycle() {
        WorkflowExecuteReq req = new WorkflowExecuteReq();
        req.setWorkflowDsl("""
                id: loop_workflow
                type: workflow
                version: 1
                context:
                  inputs:
                    text: string
                nodes:
                  - id: a
                    type: script
                    runtime: js
                    code: "return { x: text }"
                    output_key: a_out
                  - id: b
                    type: script
                    runtime: js
                    code: "return { y: a_out }"
                    output_key: b_out
                edges:
                  - from: a
                    to: b
                  - from: b
                    to: a
                output:
                  result: "{{a_out}}"
                """);
        req.setInputs(Map.of("text", "x"));

        WorkflowExecuteRes res = workflowExecuteService.execute(req);

        assertThat(res.getSuccess()).isFalse();
        assertThat(res.getErrorMessage()).contains("环");
    }
}
