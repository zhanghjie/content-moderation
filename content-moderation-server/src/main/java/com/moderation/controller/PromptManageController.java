package com.moderation.controller;

import com.moderation.common.BaseResult;
import com.moderation.model.req.PromptComposePreviewReq;
import com.moderation.model.req.PromptDefaultModulesReq;
import com.moderation.model.req.PromptDslValidateReq;
import com.moderation.model.req.PromptModuleSaveReq;
import com.moderation.model.req.PromptSplitReq;
import com.moderation.model.req.WorkflowExecuteReq;
import com.moderation.model.res.PromptComposePreviewRes;
import com.moderation.model.res.PromptDslValidateRes;
import com.moderation.model.res.PromptModuleManageRes;
import com.moderation.model.res.PromptSplitRes;
import com.moderation.model.res.WorkflowExecuteRes;
import com.moderation.promptengine.dsl.PromptDslValidationService;
import com.moderation.promptengine.runtime.WorkflowExecuteService;
import com.moderation.prompt.PromptComposer;
import com.moderation.prompt.PromptModuleManageService;
import com.moderation.service.PromptSplitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
@Tag(name = "Prompt管理", description = "Prompt模块增删改查与组合预览")
public class PromptManageController {

    private final PromptModuleManageService promptModuleManageService;
    private final PromptComposer promptComposer;
    private final PromptSplitService promptSplitService;
    private final PromptDslValidationService promptDslValidationService;
    private final WorkflowExecuteService workflowExecuteService;

    @GetMapping("/modules")
    @Operation(summary = "查询模块列表")
    public BaseResult<PromptModuleManageRes> getModules(@RequestParam(defaultValue = "HOST_VIOLATION") String analysisType) {
        return BaseResult.success(promptModuleManageService.getModules(analysisType));
    }

    @PostMapping("/modules")
    @Operation(summary = "新增模块")
    public BaseResult<PromptModuleManageRes> createModule(@RequestBody PromptModuleSaveReq req) {
        try {
            return BaseResult.success(promptModuleManageService.saveModule(req));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @PutMapping("/modules/{analysisType}/{code}")
    @Operation(summary = "更新模块")
    public BaseResult<PromptModuleManageRes> updateModule(
            @PathVariable String analysisType,
            @PathVariable String code,
            @RequestBody PromptModuleSaveReq req
    ) {
        try {
            return BaseResult.success(promptModuleManageService.updateModule(analysisType, code, req));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @DeleteMapping("/modules/{analysisType}/{code}")
    @Operation(summary = "删除模块")
    public BaseResult<PromptModuleManageRes> deleteModule(@PathVariable String analysisType, @PathVariable String code) {
        return BaseResult.success(promptModuleManageService.deleteModule(analysisType, code));
    }

    @PutMapping("/default-modules")
    @Operation(summary = "更新默认模块组合")
    public BaseResult<PromptModuleManageRes> saveDefaultModules(@RequestBody PromptDefaultModulesReq req) {
        try {
            return BaseResult.success(promptModuleManageService.saveDefaultModules(req));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @PostMapping("/compose-preview")
    @Operation(summary = "组合预览Prompt")
    public BaseResult<PromptComposePreviewRes> composePreview(@RequestBody PromptComposePreviewReq req) {
        Map<String, String> variables = new HashMap<>();
        variables.put("CALL_ID", req.getCallId());
        variables.put("CONTENT_ID", req.getContentId());
        variables.put("VIDEO_URL", req.getVideoUrl());
        variables.put("USER_ID", req.getUserId() == null ? "" : String.valueOf(req.getUserId()));
        String analysisType = req.getAnalysisType() == null || req.getAnalysisType().isBlank()
                ? "HOST_VIOLATION"
                : req.getAnalysisType().trim().toUpperCase();
        PromptComposer.ComposedPrompt composed = promptComposer.compose(analysisType, req.getModules(), variables);
        return BaseResult.success(PromptComposePreviewRes.builder()
                .analysisType(analysisType)
                .prompt(composed.prompt())
                .build());
    }

    @PostMapping("/split")
    @Operation(summary = "将原始Prompt拆分为模块")
    public BaseResult<PromptSplitRes> splitPrompt(@RequestBody PromptSplitReq req) {
        try {
            return BaseResult.success(promptSplitService.split(req));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        } catch (Exception e) {
            return BaseResult.failed(500, "Prompt 拆分失败: " + e.getMessage());
        }
    }

    @PostMapping("/dsl/validate")
    @Operation(summary = "校验 Prompt/Workflow DSL")
    public BaseResult<PromptDslValidateRes> validateDsl(@RequestBody PromptDslValidateReq req) {
        return BaseResult.success(promptDslValidationService.validate(req.getDsl()));
    }

    @PostMapping("/workflow/execute")
    @Operation(summary = "执行 Workflow DSL")
    public BaseResult<WorkflowExecuteRes> executeWorkflow(@RequestBody WorkflowExecuteReq req) {
        return BaseResult.success(workflowExecuteService.execute(req));
    }
}
