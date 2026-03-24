package com.moderation.skillos;

import com.moderation.skillos.engine.PolicyExecuteResult;
import com.moderation.skillos.engine.PolicyExecutionEngine;
import com.moderation.skillos.engine.DefaultPlanExecutor;
import com.moderation.skillos.executor.AsrSkillExecutor;
import com.moderation.skillos.executor.RoleDetectSkillExecutor;
import com.moderation.skillos.executor.SemanticAnalysisSkillExecutor;
import com.moderation.skillos.executor.VideoParseSkillExecutor;
import com.moderation.skillos.executor.ViolationAggregateSkillExecutor;
import com.moderation.skillos.executor.ViolationBlackScreenSkillExecutor;
import com.moderation.skillos.executor.ViolationCallInBedSkillExecutor;
import com.moderation.mapper.PolicyExecutionFeedbackMapper;
import com.moderation.mapper.PolicyExecutionMapper;
import com.moderation.mapper.PolicyExecutionStepMapper;
import com.moderation.skillos.model.PolicyDefinition;
import com.moderation.skillos.planner.DefaultPolicyPlanner;
import com.moderation.skillos.registry.PolicyRegistry;
import com.moderation.skillos.registry.SkillRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyExecutionEngineTest {
    private PolicyExecutionEngine policyExecutionEngine;

    private PolicyRegistry policyRegistry;

    @BeforeEach
    void setUp() {
        SkillRegistry skillRegistry = new SkillRegistry();
        skillRegistry.initDefaults();
        policyRegistry = new PolicyRegistry();
        policyRegistry.initDefaults();

        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("roleDetectSkillExecutor", new RoleDetectSkillExecutor());
        applicationContext.getBeanFactory().registerSingleton("videoParseSkillExecutor", new VideoParseSkillExecutor());
        applicationContext.getBeanFactory().registerSingleton("asrSkillExecutor", new AsrSkillExecutor());
        applicationContext.getBeanFactory().registerSingleton("semanticAnalysisSkillExecutor", new SemanticAnalysisSkillExecutor());
        applicationContext.getBeanFactory().registerSingleton("violationCallInBedSkillExecutor", new ViolationCallInBedSkillExecutor());
        applicationContext.getBeanFactory().registerSingleton("violationBlackScreenSkillExecutor", new ViolationBlackScreenSkillExecutor());
        applicationContext.getBeanFactory().registerSingleton("violationAggregateSkillExecutor", new ViolationAggregateSkillExecutor());
        policyExecutionEngine = new PolicyExecutionEngine(
                policyRegistry,
                new DefaultPolicyPlanner(),
                new DefaultPlanExecutor(skillRegistry, applicationContext),
                Mockito.mock(PolicyExecutionMapper.class),
                Mockito.mock(PolicyExecutionStepMapper.class),
                Mockito.mock(PolicyExecutionFeedbackMapper.class),
                new ObjectMapper()
        );
    }

    @Test
    void should_ExecuteVideoRiskPipeline_When_DefaultPolicyRun() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("videoUrl", "https://demo/video_call_black.mp4");
        input.put("transcript", "来床上聊，黑屏画面很多");

        PolicyExecuteResult result = policyExecutionEngine.execute("video_risk_v1", input);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getState().getData()).containsKeys("role_detect", "video_parse", "asr", "semantic_analysis", "violation_aggregate");
        assertThat(result.getTraces()).hasSize(7);
    }

    @Test
    void should_SkipDisabledSkill_When_ConfigProvidesDisabledSkills() {
        PolicyDefinition policy = new PolicyDefinition();
        policy.setPolicyId("video_risk_skip_black");
        policy.setName("视频风控-跳过黑屏");
        policy.setVersion("v1");
        policy.setSkillPipeline(List.of(
                "role_detect",
                "video_parse",
                "asr",
                "semantic_analysis",
                "violation_call_in_bed",
                "violation_black_screen",
                "violation_aggregate"
        ));
        policy.setConfig(new LinkedHashMap<>(Map.of(
                "disabledSkills", List.of("violation_black_screen"),
                "enableCallInBedCheck", true,
                "callInBedThreshold", 0.8D
        )));
        policyRegistry.register(policy);

        PolicyExecuteResult result = policyExecutionEngine.execute("video_risk_skip_black", Map.of(
                "videoUrl", "https://demo/video_call_black.mp4",
                "transcript", "来床上聊，黑屏画面很多"
        ));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTraces().stream().filter(trace -> "violation_black_screen".equals(trace.getSkillId())).findFirst())
                .isPresent()
                .get()
                .extracting("skipped")
                .isEqualTo(true);
    }
}
