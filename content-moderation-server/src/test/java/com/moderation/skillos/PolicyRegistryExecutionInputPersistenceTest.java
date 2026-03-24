package com.moderation.skillos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.entity.PolicyDefinitionEntity;
import com.moderation.mapper.PolicyDefinitionMapper;
import com.moderation.skillos.model.PolicyDefinition;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.registry.PolicyRegistry;
import com.moderation.skillos.registry.SkillRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyRegistryExecutionInputPersistenceTest {
    private static class InMemoryPolicyDefinitionMapper implements PolicyDefinitionMapper {
        private final Map<String, PolicyDefinitionEntity> store = new ConcurrentHashMap<>();

        @Override
        public List<PolicyDefinitionEntity> listAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public PolicyDefinitionEntity selectById(String policyId) {
            return store.get(policyId);
        }

        @Override
        public int upsert(PolicyDefinitionEntity entity) {
            store.put(entity.getPolicyId(), entity);
            return 1;
        }

        @Override
        public int deleteById(String policyId) {
            return store.remove(policyId) == null ? 0 : 1;
        }
    }

    @Test
    void should_PersistExecutionInput_When_RegisterPolicy() {
        InMemoryPolicyDefinitionMapper mapper = new InMemoryPolicyDefinitionMapper();
        SkillRegistry skillRegistry = mock(SkillRegistry.class);
        when(skillRegistry.get(anyString())).thenReturn(new SkillDefinition());

        PolicyRegistry policyRegistry = new PolicyRegistry();
        ReflectionTestUtils.setField(policyRegistry, "policyDefinitionMapper", mapper);
        ReflectionTestUtils.setField(policyRegistry, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(policyRegistry, "skillRegistry", skillRegistry);

        PolicyDefinition policy = new PolicyDefinition();
        policy.setPolicyId("policy_exec_input_test_v1");
        policy.setName("Policy执行输入持久化测试");
        policy.setVersion("v1");
        policy.setSkillPipeline(List.of("role_detect"));
        policy.setConfig(new LinkedHashMap<>());
        policy.setExecutionInput(new LinkedHashMap<>(Map.of(
                "videoUrl", "https://example.com/demo.mp4",
                "transcript", "hello"
        )));

        policyRegistry.register(policy);

        PolicyDefinition reloaded = policyRegistry.get("policy_exec_input_test_v1");
        assertThat(reloaded.getExecutionInput()).containsEntry("videoUrl", "https://example.com/demo.mp4");
        assertThat(reloaded.getExecutionInput()).containsEntry("transcript", "hello");
    }
}

