package com.moderation.promptengine.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.model.res.PromptDslValidateRes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptDslValidationServiceTest {

    private final PromptDslValidationService service = new PromptDslValidationService(
            new ObjectMapper(),
            new PromptDslParseService(new ObjectMapper())
    );

    @Test
    void should_validate_prompt_dsl_successfully() {
        String dsl = """
                id: generic_text_summary
                type: prompt
                version: 1
                model:
                  name: gpt-4o
                  temperature: 0.3
                input_schema:
                  text: string
                template:
                  system: 你是一个文本总结助手
                  instruction: 请总结以下内容 {{text}}
                output_schema:
                  summary: string
                """;

        PromptDslValidateRes res = service.validate(dsl);

        assertThat(res.getValid()).isTrue();
        assertThat(res.getDslType()).isEqualTo("PROMPT");
        assertThat(res.getErrors()).isEmpty();
    }

    @Test
    void should_validate_workflow_dsl_successfully() {
        String dsl = """
                id: generic_ai_workflow
                type: workflow
                version: 1
                context:
                  inputs:
                    text: string
                nodes:
                  - id: summarize
                    type: llm
                    prompt_ref: generic_text_summary
                    input_mapping:
                      text: "{{context.text}}"
                    output_key: summary
                output:
                  result: "{{summary}}"
                """;

        PromptDslValidateRes res = service.validate(dsl);

        assertThat(res.getValid()).isTrue();
        assertThat(res.getDslType()).isEqualTo("WORKFLOW");
        assertThat(res.getErrors()).isEmpty();
    }

    @Test
    void should_return_errors_for_invalid_dsl() {
        String dsl = """
                id: broken
                type: workflow
                version: 1
                nodes:
                  - id: n1
                    type: llm
                    output_key: out
                """;

        PromptDslValidateRes res = service.validate(dsl);

        assertThat(res.getValid()).isFalse();
        assertThat(res.getErrors()).isNotEmpty();
    }
}
