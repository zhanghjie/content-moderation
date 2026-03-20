package com.moderation.controller;

import com.moderation.common.BaseResult;
import com.moderation.model.req.ApiConnectionTestReq;
import com.moderation.model.req.LlmProfileSaveReq;
import com.moderation.model.res.ApiConnectionTestRes;
import com.moderation.model.res.LlmProfilesRes;
import com.moderation.service.ApiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "API 配置", description = "第三方 API 配置管理")
public class ApiConfigController {

    private final ApiConfigService apiConfigService;

    @GetMapping("/api-config")
    @Operation(summary = "获取 API 配置")
    public BaseResult<LlmProfilesRes> getApiConfig() {
        return BaseResult.success(apiConfigService.getConfig());
    }

    @PutMapping("/api-config")
    @Operation(summary = "保存 API 配置")
    public BaseResult<LlmProfilesRes> saveApiConfig(@RequestBody LlmProfileSaveReq req) {
        try {
            return BaseResult.success(apiConfigService.saveConfig(req));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @DeleteMapping("/api-config/{configCode}")
    @Operation(summary = "删除 LLM 配置")
    public BaseResult<LlmProfilesRes> deleteApiConfig(@PathVariable String configCode) {
        return BaseResult.success(apiConfigService.deleteConfig(configCode));
    }

    @PutMapping("/api-config/default/{configCode}")
    @Operation(summary = "设置默认 LLM 配置")
    public BaseResult<LlmProfilesRes> setDefaultApiConfig(@PathVariable String configCode) {
        return BaseResult.success(apiConfigService.setDefaultConfig(configCode));
    }

    @PostMapping("/test-llm")
    @Operation(summary = "测试 LLM 连接")
    public BaseResult<ApiConnectionTestRes> testLlm(@RequestBody(required = false) ApiConnectionTestReq req) {
        return BaseResult.success(apiConfigService.testLlm(req));
    }

    @PostMapping("/test-yidun")
    @Operation(summary = "测试易盾连接")
    public BaseResult<ApiConnectionTestRes> testYidun(@RequestBody(required = false) ApiConnectionTestReq req) {
        return BaseResult.success(apiConfigService.testYidun(req));
    }
}
