package com.moderation.controller;

import com.moderation.common.BaseResult;
import com.moderation.model.req.PromptSplitReq;
import com.moderation.model.res.PromptSplitRes;
import com.moderation.prompt.PromptComposer;
import com.moderation.prompt.PromptModuleManageService;
import com.moderation.promptengine.dsl.PromptDslValidationService;
import com.moderation.promptengine.runtime.WorkflowExecuteService;
import com.moderation.service.PromptSplitService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptManageControllerTest {

    @Test
    void should_return_500_body_when_split_runtime_exception() {
        PromptModuleManageService moduleService = mock(PromptModuleManageService.class);
        PromptComposer composer = mock(PromptComposer.class);
        PromptSplitService splitService = mock(PromptSplitService.class);
        PromptDslValidationService dslValidationService = mock(PromptDslValidationService.class);
        WorkflowExecuteService workflowExecuteService = mock(WorkflowExecuteService.class);
        PromptManageController controller = new PromptManageController(moduleService, composer, splitService, dslValidationService, workflowExecuteService);
        PromptSplitReq req = new PromptSplitReq();
        req.setRawPrompt("x");
        when(splitService.split(req)).thenThrow(new RuntimeException("boom"));

        BaseResult<PromptSplitRes> result = controller.splitPrompt(req);

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).contains("Prompt 拆分失败");
    }
}
