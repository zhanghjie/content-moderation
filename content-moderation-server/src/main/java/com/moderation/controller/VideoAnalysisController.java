package com.moderation.controller;

import com.moderation.common.BaseResult;
import com.moderation.model.req.VideoAnalyzeReq;
import com.moderation.model.res.VideoAnalyzeRes;
import com.moderation.model.res.PromptModulesRes;
import com.moderation.model.res.TaskListRes;
import com.moderation.prompt.PromptCatalogService;
import com.moderation.service.VideoAnalysisService;
import com.moderation.skillos.engine.PolicyExecuteResult;
import com.moderation.skillos.engine.PolicyExecutionEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 视频分析控制器
 */
@RestController
@RequestMapping("/api/v1/video")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "视频分析 API", description = "视频内容分析相关接口")
public class VideoAnalysisController {
    
    private final VideoAnalysisService videoAnalysisService;
    private final PromptCatalogService promptCatalogService;
    private final PolicyExecutionEngine policyExecutionEngine;
    
    @PostMapping("/analyze")
    @Operation(summary = "发起视频分析", description = "对指定视频进行 AI 分析，识别违规行为")
    public BaseResult<VideoAnalyzeRes> analyze(@RequestBody @Validated VideoAnalyzeReq req) {
        log.info("Video analyze request: {}", req);
        VideoAnalyzeRes result = videoAnalysisService.analyze(req);
        return BaseResult.success(result);
    }
    
    @PostMapping("/analyze-and-save")
    @Operation(summary = "执行 Policy 并保存任务", description = "基于 Policy 执行分析，自动追加 OUTPUT Skill 转换格式，并保存任务")
    public BaseResult<VideoAnalyzeRes> analyzeAndSave(@RequestBody @Validated VideoAnalyzeReq req) {
        log.info("Video analyze and save request: {}", req);
        
        // 1. 合并基础参数和动态 policyInput
        Map<String, Object> input = new LinkedHashMap<>();
        if (req.getPolicyInput() != null) {
            input.putAll(req.getPolicyInput());
        }
        // 覆盖/补充基础字段
        input.put("callId", req.getCallId());
        input.put("contentId", req.getContentId());
        input.put("videoUrl", req.getVideoUrl());
        input.put("coverUrl", req.getCoverUrl());
        input.put("analysisType", req.getAnalysisType());
        input.put("userId", req.getUserId());
        
        // 2. 执行 Policy（自动追加 OUTPUT Skill）
        try {
            PolicyExecuteResult result = policyExecutionEngine.execute(req.getPolicyId(), input);
            log.info("Policy executed, executionId: {}, success: {}", result.getExecutionId(), result.isSuccess());
            
            // 3. 从执行结果中提取 OUTPUT Skill 的输出
            // TODO: 需要从 result.getState() 或 result.getTraces() 中获取最后一个 Skill 的输出
            
            // 临时返回
            VideoAnalyzeRes res = VideoAnalyzeRes.builder()
                .callId(req.getCallId())
                .status("COMPLETED")
                .build();
            return BaseResult.success(res);
            
        } catch (Exception e) {
            log.error("Policy execute failed, policyId: {}", req.getPolicyId(), e);
            return BaseResult.failed("Policy 执行失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/result/{callId}")
    @Operation(summary = "查询分析结果", description = "根据 callId 查询视频分析结果")
    public BaseResult<VideoAnalyzeRes> getResult(@PathVariable String callId) {
        log.info("Video result request, callId: {}", callId);
        VideoAnalyzeRes result = videoAnalysisService.getResult(callId);
        return BaseResult.success(result);
    }

    @PostMapping("/reanalyze/{callId}")
    @Operation(summary = "重新分析任务", description = "基于最近一次任务参数重新触发分析")
    public BaseResult<VideoAnalyzeRes> reAnalyze(@PathVariable String callId) {
        VideoAnalyzeRes res = videoAnalysisService.reAnalyze(callId);
        return BaseResult.success(res);
    }

    @GetMapping("/tasks")
    @Operation(summary = "查询任务列表", description = "分页查询视频分析任务列表")
    public BaseResult<TaskListRes> getTasks(
            @RequestParam(required = false) String callId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        TaskListRes res = videoAnalysisService.getTasks(callId, status, result, page, pageSize);
        return BaseResult.success(res);
    }

    @GetMapping("/prompt-modules")
    @Operation(summary = "获取 Prompt 模块", description = "获取指定分析类型可用的 Prompt 模块与默认组合")
    public BaseResult<PromptModulesRes> getPromptModules(@RequestParam String analysisType) {
        if ("HOST_VIOLATION".equalsIgnoreCase(analysisType)) {
            return BaseResult.success(promptCatalogService.getHostViolationModules());
        }
        return BaseResult.success(PromptModulesRes.builder().analysisType(analysisType).build());
    }
}
