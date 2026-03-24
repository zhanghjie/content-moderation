package com.moderation.skillos.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.entity.PolicyDefinitionEntity;
import com.moderation.mapper.PolicyDefinitionMapper;
import com.moderation.skillos.model.PolicyDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PolicyRegistry {
    @Autowired(required = false)
    private PolicyDefinitionMapper policyDefinitionMapper;
    @Autowired(required = false)
    private ObjectMapper objectMapper;
    @Autowired
    private SkillRegistry skillRegistry;
    private final Map<String, PolicyDefinition> policies = new ConcurrentHashMap<>();

    @PostConstruct
    public void initDefaults() {
        if (!isSqlReadMode()) {
            loadPersistedPolicies();
        }
    }

    public void register(PolicyDefinition policy) {
        if (policy == null || isBlank(policy.getPolicyId())) {
            throw new IllegalArgumentException("policyId 不能为空");
        }
        if (policy.getSkillPipeline() == null || policy.getSkillPipeline().isEmpty()) {
            throw new IllegalArgumentException("skillPipeline 不能为空");
        }
        validateSkillPipeline(policy.getSkillPipeline());
        validateNoSchemaDefinition(policy.getConfig());
        if (policy.getVersion() == null || policy.getVersion().isBlank()) {
            policy.setVersion("v1");
        }
        if (policy.getConfig() == null) {
            policy.setConfig(new LinkedHashMap<>());
        }
        if (policy.getExecutionInput() == null) {
            policy.setExecutionInput(new LinkedHashMap<>());
        }
        if (!isSqlReadMode()) {
            policies.put(policy.getPolicyId(), policy);
        }
        persistPolicy(policy);
    }

    public PolicyDefinition get(String policyId) {
        if (isSqlReadMode()) {
            PolicyDefinitionEntity entity = policyDefinitionMapper.selectById(policyId);
            if (entity == null) {
                throw new IllegalArgumentException("Policy 不存在: " + policyId);
            }
            return toPolicyDefinition(entity);
        }
        PolicyDefinition policy = policies.get(policyId);
        if (policy == null) {
            throw new IllegalArgumentException("Policy 不存在: " + policyId);
        }
        return policy;
    }

    public List<PolicyDefinition> list() {
        if (isSqlReadMode()) {
            List<PolicyDefinitionEntity> entities = policyDefinitionMapper.listAll();
            List<PolicyDefinition> result = new ArrayList<>();
            for (PolicyDefinitionEntity entity : entities) {
                result.add(toPolicyDefinition(entity));
            }
            return result;
        }
        return new ArrayList<>(policies.values());
    }

    public void remove(String policyId) {
        if (isBlank(policyId)) {
            throw new IllegalArgumentException("policyId 不能为空");
        }
        if (isSqlReadMode()) {
            int deleted = policyDefinitionMapper.deleteById(policyId);
            if (deleted <= 0) {
                throw new IllegalArgumentException("Policy 不存在: " + policyId);
            }
            policies.remove(policyId);
            return;
        }
        if (policies.remove(policyId) == null) {
            throw new IllegalArgumentException("Policy 不存在: " + policyId);
        }
        deletePersistedPolicy(policyId);
    }

    private void registerIfAbsent(PolicyDefinition definition) {
        if (hasPolicy(definition.getPolicyId())) {
            return;
        }
        register(definition);
    }

    private void loadPersistedPolicies() {
        if (policyDefinitionMapper == null || objectMapper == null) {
            return;
        }
        List<PolicyDefinitionEntity> entities = policyDefinitionMapper.listAll();
        for (PolicyDefinitionEntity entity : entities) {
            PolicyDefinition definition = toPolicyDefinition(entity);
            policies.put(definition.getPolicyId(), definition);
        }
    }

    private PolicyDefinition toPolicyDefinition(PolicyDefinitionEntity entity) {
        PolicyDefinition definition = new PolicyDefinition();
        definition.setPolicyId(entity.getPolicyId());
        definition.setName(entity.getName());
        definition.setSkillPipeline(readList(entity.getSkillPipelineJson()));
        definition.setConfig(readMap(entity.getConfigJson()));
        definition.setExecutionInput(readMap(entity.getExecutionInputJson()));
        definition.setVersion(entity.getVersion());
        return definition;
    }

    private void persistPolicy(PolicyDefinition policy) {
        if (policyDefinitionMapper == null || objectMapper == null) {
            return;
        }
        PolicyDefinitionEntity entity = new PolicyDefinitionEntity();
        entity.setPolicyId(policy.getPolicyId());
        entity.setName(policy.getName());
        entity.setSkillPipelineJson(writeJson(policy.getSkillPipeline() == null ? List.of() : policy.getSkillPipeline()));
        entity.setConfigJson(writeJson(policy.getConfig()));
        entity.setExecutionInputJson(writeJson(policy.getExecutionInput()));
        entity.setVersion(policy.getVersion());
        policyDefinitionMapper.upsert(entity);
    }

    private void deletePersistedPolicy(String policyId) {
        if (policyDefinitionMapper == null) {
            return;
        }
        policyDefinitionMapper.deleteById(policyId);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> readMap(String text) {
        if (isBlank(text) || objectMapper == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private List<String> readList(String text) {
        if (isBlank(text) || objectMapper == null) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private void validateSkillPipeline(List<String> pipeline) {
        for (String skillId : pipeline) {
            if (isBlank(skillId)) {
                throw new IllegalArgumentException("skillPipeline 中存在空 skillId");
            }
            skillRegistry.get(skillId);
        }
    }

    private void validateNoSchemaDefinition(Object config) {
        if (containsSchemaDefinition(config)) {
            throw new IllegalArgumentException("Policy config 不允许定义 inputSchema/outputSchema/stateMapping，请在 Skill 注册中心维护");
        }
    }

    private boolean containsSchemaDefinition(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if ("inputSchema".equalsIgnoreCase(key)
                        || "outputSchema".equalsIgnoreCase(key)
                        || "stateMapping".equalsIgnoreCase(key)) {
                    return true;
                }
                if (containsSchemaDefinition(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (containsSchemaDefinition(item)) {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof String text) {
            String content = text.trim();
            if ((content.startsWith("{") && content.endsWith("}")) || (content.startsWith("[") && content.endsWith("]"))) {
                Object parsed = tryReadJson(content);
                if (parsed != null && containsSchemaDefinition(parsed)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Object tryReadJson(String content) {
        if (objectMapper == null) {
            return null;
        }
        try {
            return objectMapper.readValue(content, Object.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isSqlReadMode() {
        return policyDefinitionMapper != null && objectMapper != null;
    }

    private boolean hasPolicy(String policyId) {
        if (isSqlReadMode()) {
            return policyDefinitionMapper.selectById(policyId) != null;
        }
        return policies.containsKey(policyId);
    }
}
