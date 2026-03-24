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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyRegistrySchemaValidationTest {
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

    private PolicyRegistry createPolicyRegistry() {
        InMemoryPolicyDefinitionMapper mapper = new InMemoryPolicyDefinitionMapper();
        SkillRegistry skillRegistry = mock(SkillRegistry.class);
        when(skillRegistry.get(anyString())).thenReturn(new SkillDefinition());
        PolicyRegistry policyRegistry = new PolicyRegistry();
        ReflectionTestUtils.setField(policyRegistry, "policyDefinitionMapper", mapper);
        ReflectionTestUtils.setField(policyRegistry, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(policyRegistry, "skillRegistry", skillRegistry);
        return policyRegistry;
    }

    private PolicyDefinition basePolicy(String policyId) {
        PolicyDefinition policy = new PolicyDefinition();
        policy.setPolicyId(policyId);
        policy.setName("schema-validate-test");
        policy.setVersion("v1");
        policy.setSkillPipeline(List.of("behavior_feature_extract"));
        policy.setConfig(new LinkedHashMap<>());
        policy.setExecutionInput(new LinkedHashMap<>());
        return policy;
    }

    @Test
    void should_RejectNestedSchemaDefinition_InMap() {
        PolicyRegistry policyRegistry = createPolicyRegistry();
        PolicyDefinition policy = basePolicy("policy_schema_nested_map");
        policy.setConfig(new LinkedHashMap<>(Map.of(
                "safe", true,
                "nested", Map.of(
                        "deep", Map.of(
                                "inputSchema", Map.of("x", "string")
                        )
                )
        )));

        assertThatThrownBy(() -> policyRegistry.register(policy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Policy config 不允许定义 inputSchema/outputSchema/stateMapping");
    }

    @Test
    void should_RejectNestedSchemaDefinition_InJsonString() {
        PolicyRegistry policyRegistry = createPolicyRegistry();
        PolicyDefinition policy = basePolicy("policy_schema_nested_json_string");
        policy.setConfig(new LinkedHashMap<>(Map.of(
                "safe", true,
                "nestedText", "{\"layer\":{\"stateMapping\":{\"a\":\"b\"}}}"
        )));

        assertThatThrownBy(() -> policyRegistry.register(policy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Policy config 不允许定义 inputSchema/outputSchema/stateMapping");
    }
}

