package com.moderation.controller;

import com.moderation.common.BaseResult;
import com.moderation.model.req.PolicyExecuteReq;
import com.moderation.model.req.ExecutionFeedbackReq;
import com.moderation.model.req.PolicyRegisterReq;
import com.moderation.model.req.SkillCloneReq;
import com.moderation.model.req.SkillRegisterReq;
import com.moderation.model.req.SkillScriptTestReq;
import com.moderation.model.res.PolicyExecuteRes;
import com.moderation.model.res.PolicyListRes;
import com.moderation.model.res.SkillListRes;
import com.moderation.model.res.SkillScriptTestRes;
import com.moderation.model.res.SkillTemplateListRes;
import com.moderation.model.res.SkillTemplateRes;
import com.moderation.model.res.SkillUsageRes;
import com.moderation.skillos.engine.PolicyExecuteResult;
import com.moderation.skillos.engine.PolicyExecutionEngine;
import com.moderation.skillos.model.PolicyDefinition;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.registry.PolicyRegistry;
import com.moderation.skillos.registry.SkillRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "SkillOS", description = "Skill 注册、Policy 配置、策略执行")
public class SkillOsController {
    private final SkillRegistry skillRegistry;
    private final PolicyRegistry policyRegistry;
    private final PolicyExecutionEngine policyExecutionEngine;

    @PostMapping("/skills/register")
    @Operation(summary = "注册 Skill")
    public BaseResult<SkillListRes> registerSkill(@RequestBody SkillRegisterReq req) {
        return saveSkill(req, false);
    }

    @PostMapping("/skills/draft")
    @Operation(summary = "保存 Skill 草稿")
    public BaseResult<SkillListRes> saveDraft(@RequestBody SkillRegisterReq req) {
        return saveSkill(req, true);
    }

    @PutMapping("/skills/{skillId}")
    @Operation(summary = "更新 Skill")
    public BaseResult<SkillListRes> updateSkill(@PathVariable String skillId, @RequestBody SkillRegisterReq req) {
        req.setSkillId(skillId);
        return saveSkill(req, false);
    }

    @PostMapping("/skills/{skillId}/publish")
    @Operation(summary = "发布 Skill")
    public BaseResult<SkillListRes> publishSkill(@PathVariable String skillId) {
        try {
            skillRegistry.publish(skillId);
            return BaseResult.success(SkillListRes.builder().skills(skillRegistry.list()).build());
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @PostMapping("/skills/{skillId}/clone")
    @Operation(summary = "复制 Skill")
    public BaseResult<SkillListRes> cloneSkill(@PathVariable String skillId, @RequestBody SkillCloneReq req) {
        try {
            skillRegistry.cloneSkill(skillId, req.getNewSkillId(), req.getNewName());
            return BaseResult.success(SkillListRes.builder().skills(skillRegistry.list()).build());
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @DeleteMapping("/skills/{skillId}")
    @Operation(summary = "删除 Skill")
    public BaseResult<SkillListRes> deleteSkill(@PathVariable String skillId) {
        return doDeleteSkill(skillId);
    }

    @PostMapping("/skills/{skillId}/delete")
    @Operation(summary = "删除 Skill（兼容调用）")
    public BaseResult<SkillListRes> deleteSkillByPost(@PathVariable String skillId) {
        return doDeleteSkill(skillId);
    }

    private BaseResult<SkillListRes> doDeleteSkill(String skillId) {
        try {
            skillRegistry.removeSkill(skillId);
            return BaseResult.success(SkillListRes.builder().skills(skillRegistry.list()).build());
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @GetMapping("/skills/list")
    @Operation(summary = "查询 Skill 列表")
    public BaseResult<SkillListRes> listSkills() {
        return BaseResult.success(SkillListRes.builder().skills(skillRegistry.list()).build());
    }

    @GetMapping("/skills/templates")
    @Operation(summary = "查询 Skill 模板")
    public BaseResult<SkillTemplateListRes> listTemplates() {
        List<SkillTemplateRes> templates = skillRegistry.listTemplates().stream().map(item -> SkillTemplateRes.builder()
                .templateId(item.getSkillId())
                .name(item.getName())
                .type(item.getType())
                .description(item.getDescription())
                .tags(item.getTags())
                .outputSchema(item.getOutputSchema())
                .stateMapping(item.getStateMapping())
                .executionConfig(item.getExecutionConfig())
                .scriptConfig(item.getScriptConfig())
                .build()).collect(Collectors.toList());
        return BaseResult.success(SkillTemplateListRes.builder().templates(templates).build());
    }

    @GetMapping("/skills/{skillId}/usages")
    @Operation(summary = "查询 Skill 被策略引用情况")
    public BaseResult<SkillUsageRes> skillUsages(@PathVariable String skillId) {
        List<String> policyIds = policyRegistry.list().stream()
                .filter(policy -> policy.getSkillPipeline() != null && policy.getSkillPipeline().contains(skillId))
                .map(PolicyDefinition::getPolicyId)
                .collect(Collectors.toList());
        return BaseResult.success(SkillUsageRes.builder().skillId(skillId).policyIds(policyIds).build());
    }

    @PostMapping("/skills/{skillId}/script/test")
    @Operation(summary = "调试 Skill 脚本")
    public BaseResult<SkillScriptTestRes> testScript(@PathVariable String skillId, @RequestBody SkillScriptTestReq req) {
        try {
            Map<String, Object> output = skillRegistry.testScript(skillId, req.getScriptContent(), req.getInput(), req.getState());
            return BaseResult.success(SkillScriptTestRes.builder()
                    .success(true)
                    .message("脚本调试成功")
                    .output(output)
                    .writeToState(new LinkedHashMap<>())
                    .build());
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        } catch (Exception e) {
            return BaseResult.failed(500, "脚本调试失败: " + e.getMessage());
        }
    }

    @PostMapping("/policy/register")
    @Operation(summary = "注册 Policy")
    public BaseResult<PolicyListRes> registerPolicy(@RequestBody PolicyRegisterReq req) {
        try {
            PolicyDefinition definition = new PolicyDefinition();
            definition.setPolicyId(req.getPolicyId());
            definition.setName(req.getName());
            definition.setSkillPipeline(req.getSkillPipeline());
            definition.setConfig(req.getConfig());
            definition.setExecutionInput(req.getExecutionInput());
            definition.setVersion(req.getVersion());
            policyRegistry.register(definition);
            return BaseResult.success(PolicyListRes.builder().policies(policyRegistry.list()).build());
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @GetMapping("/policy/list")
    @Operation(summary = "查询 Policy 列表")
    public BaseResult<PolicyListRes> listPolicies() {
        return BaseResult.success(PolicyListRes.builder().policies(policyRegistry.list()).build());
    }

    @DeleteMapping("/policy/{policyId}")
    @Operation(summary = "删除 Policy")
    public BaseResult<PolicyListRes> deletePolicy(@PathVariable String policyId) {
        return doDeletePolicy(policyId);
    }

    @PostMapping("/policy/{policyId}/delete")
    @Operation(summary = "删除 Policy（兼容调用）")
    public BaseResult<PolicyListRes> deletePolicyByPost(@PathVariable String policyId) {
        return doDeletePolicy(policyId);
    }

    private BaseResult<PolicyListRes> doDeletePolicy(String policyId) {
        try {
            policyRegistry.remove(policyId);
            return BaseResult.success(PolicyListRes.builder().policies(policyRegistry.list()).build());
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @PostMapping("/execute")
    @Operation(summary = "执行 Policy Pipeline")
    public BaseResult<PolicyExecuteRes> execute(@RequestBody PolicyExecuteReq req) {
        try {
            PolicyExecuteResult result = policyExecutionEngine.execute(req.getPolicyId(), req.getInput());
            return BaseResult.success(PolicyExecuteRes.builder()
                    .executionId(result.getExecutionId())
                    .planId(result.getPlanId())
                    .policyId(result.getPolicyId())
                    .status(result.getStatus())
                    .success(result.isSuccess())
                    .durationMs(result.getDurationMs())
                    .plan(result.getPlan())
                    .state(result.getState().getData())
                    .traces(result.getTraces())
                    .errorMessage(result.getErrorMessage())
                    .build());
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        } catch (Exception e) {
            return BaseResult.failed(500, "执行失败: " + e.getMessage());
        }
    }

    @GetMapping("/policy/{policyId}/executions/latest")
    @Operation(summary = "查询 Policy 最新执行结果")
    public BaseResult<PolicyExecuteRes> latestExecution(@PathVariable String policyId) {
        try {
            PolicyExecuteRes res = policyExecutionEngine.getLatestExecutionByPolicyId(policyId);
            return BaseResult.success(res);
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @GetMapping("/executions/{executionId}")
    @Operation(summary = "查询执行详情")
    public BaseResult<PolicyExecuteRes> executionDetail(@PathVariable String executionId) {
        try {
            return BaseResult.success(policyExecutionEngine.getExecutionById(executionId));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @GetMapping("/executions/{executionId}/traces")
    @Operation(summary = "查询执行轨迹")
    public BaseResult<Map<String, Object>> executionTraces(@PathVariable String executionId) {
        try {
            return BaseResult.success(Map.of(
                    "executionId", executionId,
                    "traces", policyExecutionEngine.listExecutionTraces(executionId)
            ));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @PostMapping("/executions/{executionId}/replay")
    @Operation(summary = "回放执行轨迹")
    public BaseResult<PolicyExecuteRes> replay(@PathVariable String executionId) {
        try {
            return BaseResult.success(policyExecutionEngine.replayExecution(executionId));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @PostMapping("/executions/{executionId}/feedback")
    @Operation(summary = "提交执行反馈")
    public BaseResult<Map<String, Object>> feedback(@PathVariable String executionId, @RequestBody ExecutionFeedbackReq req) {
        try {
            policyExecutionEngine.submitFeedback(executionId, req);
            return BaseResult.success(Map.of("executionId", executionId, "saved", true));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    private BaseResult<SkillListRes> saveSkill(SkillRegisterReq req, boolean draft) {
        try {
            SkillDefinition definition = new SkillDefinition();
            definition.setSkillId(req.getSkillId());
            definition.setName(req.getName());
            definition.setType(req.getType());
            definition.setDescription(req.getDescription());
            definition.setTags(req.getTags());
            definition.setOutputSchema(req.getOutputSchema());
            definition.setStateMapping(req.getStateMapping());
            definition.setExecutionConfig(req.getExecutionConfig());
            definition.setScriptConfig(req.getScriptConfig());
            definition.setStatus(req.getStatus());
            definition.setTimeoutMs(req.getTimeoutMs());
            definition.setVersion(req.getVersion());
            if (draft) {
                skillRegistry.saveDraft(definition);
            } else {
                skillRegistry.register(definition);
            }
            return BaseResult.success(SkillListRes.builder().skills(skillRegistry.list()).build());
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }
}
