package com.moderation.controller;

import com.moderation.common.BaseResult;
import com.moderation.model.req.VideoAnalyzeReq;
import com.moderation.model.res.VideoAnalyzeRes;
import com.moderation.model.res.PromptModulesRes;
import com.moderation.model.res.TaskListRes;
import com.moderation.prompt.PromptCatalogService;
import com.moderation.service.VideoAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    
    @PostMapping("/analyze")
    @Operation(summary = "发起视频分析", description = "对指定视频进行 AI 分析，识别违规行为")
    public BaseResult<VideoAnalyzeRes> analyze(@RequestBody @Validated VideoAnalyzeReq req) {
        log.info("Video analyze request: {}", req);
        VideoAnalyzeRes result = videoAnalysisService.analyze(req);
        return BaseResult.success(result);
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
