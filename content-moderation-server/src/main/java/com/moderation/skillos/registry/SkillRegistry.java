package com.moderation.skillos.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.entity.SkillDefinitionEntity;
import com.moderation.mapper.SkillDefinitionMapper;
import com.moderation.skillos.model.SkillDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SkillRegistry {
    private static final Set<String> ALLOWED_TYPES = Set.of("PERCEPTION", "SEMANTIC", "DECISION", "GUARD", "OUTPUT");
    private static final Map<String, String> TYPE_ALIASES = Map.of(
            "感知类", "PERCEPTION",
            "语义类", "SEMANTIC",
            "决策类", "DECISION",
            "校验类", "GUARD",
            "输出类", "OUTPUT"
    );
    @Autowired(required = false)
    private SkillDefinitionMapper skillDefinitionMapper;
    @Autowired(required = false)
    private ObjectMapper objectMapper;
    private final Map<String, SkillDefinition> templates = new ConcurrentHashMap<>();
    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();

    @PostConstruct
    public void initDefaults() {
        if (!isSqlReadMode()) {
            loadPersistedSkills();
        }
        initTemplates();
    }

    public void register(SkillDefinition skillDefinition) {
        saveSkill(skillDefinition, false);
    }

    public void saveDraft(SkillDefinition skillDefinition) {
        saveSkill(skillDefinition, true);
    }

    public SkillDefinition publish(String skillId) {
        SkillDefinition skillDefinition = get(skillId);
        skillDefinition.setStatus("PUBLISHED");
        if (!isSqlReadMode()) {
            skills.put(skillId, skillDefinition);
        }
        persistSkill(skillDefinition);
        return skillDefinition;
    }

    public SkillDefinition cloneSkill(String sourceSkillId, String newSkillId, String newName) {
        SkillDefinition source = get(sourceSkillId);
        if (isBlank(newSkillId)) {
            throw new IllegalArgumentException("newSkillId 不能为空");
        }
        if (hasSkill(newSkillId.trim())) {
            throw new IllegalArgumentException("newSkillId 已存在");
        }
        SkillDefinition copy = deepCopy(source);
        copy.setSkillId(newSkillId.trim());
        copy.setName(isBlank(newName) ? source.getName() + "-副本" : newName.trim());
        copy.setStatus("DRAFT");
        if (!isSqlReadMode()) {
            skills.put(copy.getSkillId(), copy);
        }
        persistSkill(copy);
        return copy;
    }

    public void removeSkill(String skillId) {
        if (isBlank(skillId)) {
            throw new IllegalArgumentException("skillId 不能为空");
        }
        if (isSqlReadMode()) {
            int deleted = skillDefinitionMapper.deleteById(skillId);
            if (deleted <= 0) {
                throw new IllegalArgumentException("Skill 不存在: " + skillId);
            }
            skills.remove(skillId);
            return;
        }
        if (skills.remove(skillId) == null) {
            throw new IllegalArgumentException("Skill 不存在: " + skillId);
        }
        deletePersistedSkill(skillId);
    }

    public List<SkillDefinition> listTemplates() {
        return new ArrayList<>(templates.values());
    }

    public SkillDefinition getTemplate(String templateId) {
        SkillDefinition template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }
        return deepCopy(template);
    }

    public Map<String, Object> testScript(String skillId, String scriptContent, Map<String, Object> input, Map<String, Object> state) {
        SkillDefinition skill = get(skillId);
        String content = isBlank(scriptContent)
                ? String.valueOf(skill.getScriptConfig() == null ? "" : skill.getScriptConfig().getOrDefault("content", ""))
                : scriptContent;
        if (isBlank(content)) {
            throw new IllegalArgumentException("脚本内容不能为空");
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("skillId", skillId);
        output.put("summary", "脚本调试执行成功（模拟执行）");
        output.put("input", input == null ? new LinkedHashMap<>() : input);
        output.put("state", state == null ? new LinkedHashMap<>() : state);
        output.put("scriptPreview", content.length() > 120 ? content.substring(0, 120) : content);
        return output;
    }

    private void saveSkill(SkillDefinition skillDefinition, boolean forceDraftStatus) {
        if (skillDefinition == null || isBlank(skillDefinition.getSkillId())) {
            throw new IllegalArgumentException("skillId 不能为空");
        }
        if (isBlank(skillDefinition.getType())) {
            throw new IllegalArgumentException("技能类型不能为空");
        }
        String normalizedType = normalizeType(skillDefinition.getType());
        if (!ALLOWED_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("技能类型非法，仅支持：感知类、语义类、决策类、校验类、输出类");
        }
        if (isBlank(skillDefinition.getDescription())) {
            throw new IllegalArgumentException("description 不能为空");
        }
        skillDefinition.setType(normalizedType);
        skillDefinition.setSkillId(skillDefinition.getSkillId().trim());
        skillDefinition.setName(isBlank(skillDefinition.getName()) ? skillDefinition.getSkillId() : skillDefinition.getName().trim());
        skillDefinition.setDescription(skillDefinition.getDescription().trim());
        if (skillDefinition.getVersion() == null || skillDefinition.getVersion().isBlank()) {
            skillDefinition.setVersion("v1");
        }
        if (skillDefinition.getOutputSchema() == null) {
            skillDefinition.setOutputSchema(new LinkedHashMap<>());
        }
        if (skillDefinition.getStateMapping() == null) {
            skillDefinition.setStateMapping(new LinkedHashMap<>());
        }
        if (skillDefinition.getExecutionConfig() == null) {
            skillDefinition.setExecutionConfig(new LinkedHashMap<>());
        }
        if (skillDefinition.getScriptConfig() == null) {
            skillDefinition.setScriptConfig(defaultScriptConfig());
        }
        if (skillDefinition.getTimeoutMs() == null || skillDefinition.getTimeoutMs() <= 0) {
            skillDefinition.setTimeoutMs(3000);
        }
        if (forceDraftStatus || isBlank(skillDefinition.getStatus())) {
            skillDefinition.setStatus("DRAFT");
        }
        skillDefinition.setExecutorBean(resolveExecutorBean(skillDefinition));
        if (!isSqlReadMode()) {
            skills.put(skillDefinition.getSkillId(), skillDefinition);
        }
        persistSkill(skillDefinition);
    }

    public SkillDefinition get(String skillId) {
        if (isSqlReadMode()) {
            SkillDefinitionEntity entity = skillDefinitionMapper.selectById(skillId);
            if (entity == null) {
                throw new IllegalArgumentException("Skill 不存在: " + skillId);
            }
            return toDefinition(entity);
        }
        SkillDefinition skillDefinition = skills.get(skillId);
        if (skillDefinition == null) {
            throw new IllegalArgumentException("Skill 不存在: " + skillId);
        }
        return skillDefinition;
    }

    public List<SkillDefinition> list() {
        if (isSqlReadMode()) {
            List<SkillDefinitionEntity> entities = skillDefinitionMapper.listAll();
            List<SkillDefinition> result = new ArrayList<>();
            for (SkillDefinitionEntity entity : entities) {
                result.add(toDefinition(entity));
            }
            return result;
        }
        return new ArrayList<>(skills.values());
    }

    private SkillDefinition defaultSkill(String skillId, String name, String type, String executorBean, String description, List<String> tags) {
        SkillDefinition definition = new SkillDefinition();
        definition.setSkillId(skillId);
        definition.setName(name);
        definition.setType(type);
        definition.setDescription(description);
        definition.setTags(tags);
        definition.setExecutorBean(executorBean);
        definition.setTimeoutMs(3000);
        definition.setVersion("v1");
        definition.setOutputSchema(new LinkedHashMap<>());
        definition.setStateMapping(new LinkedHashMap<>(Map.of(
                "read_from_state", List.of(),
                "write_to_state", new LinkedHashMap<String, Object>()
        )));
        definition.setExecutionConfig(new LinkedHashMap<>(Map.of(
                "execution_mode", "JAVA",
                "llm_model", "",
                "temperature", 0.2D
        )));
        definition.setScriptConfig(defaultScriptConfig());
        definition.setStatus("PUBLISHED");
        return definition;
    }

    private void initTemplates() {
        templates.put("tpl_video_perception", templateSkill(
                "tpl_video_perception",
                "视频感知模板",
                "PERCEPTION",
                "适用于视频帧特征抽取",
                List.of("video", "perception"),
                Map.of("frame_quality", "number"),
                Map.of("read_from_state", List.of("video.frames"), "write_to_state", Map.of("frame_quality", "state.video.frame_quality"))
        ));
        templates.put("tpl_semantic_risk", templateSkill(
                "tpl_semantic_risk",
                "语义风险模板",
                "SEMANTIC",
                "适用于对话语义风险识别",
                List.of("semantic", "risk"),
                Map.of("risk_score", "number", "risk_terms", "array"),
                Map.of("read_from_state", List.of("audio.transcript"), "write_to_state", Map.of("risk_score", "state.risk.semantic_score"))
        ));
    }

    private void registerArchitectureSkills() {
        SkillDefinition intake = defaultSkill(
                "session_intake",
                "会话接入标准化",
                "PERCEPTION",
                "genericSkillExecutor",
                "接收录屏会话并标准化为统一输入结构",
                List.of("session", "intake", "pipeline")
        );
        intake.setOutputSchema(new LinkedHashMap<>(Map.of("normalized_session", "object")));
        intake.setStateMapping(new LinkedHashMap<>(Map.of(
                "read_from_state", List.of("raw.session"),
                "write_to_state", Map.of("normalized_session", "state.session.normalized")
        )));
        intake.setScriptConfig(scriptConfig("function main(ctx){ return { output:{ normalized_session: ctx.input }, writeToState:{ session: ctx.input } } }"));
        registerIfAbsent(intake);

        SkillDefinition behavior = defaultSkill(
                "behavior_feature_extract",
                "主播行为特征提取",
                "SEMANTIC",
                "genericSkillExecutor",
                "抽取主播响应速度、热情度与出镜状态",
                List.of("behavior", "feature", "quality")
        );
        behavior.setOutputSchema(new LinkedHashMap<>(Map.of(
                "response_speed_score", "number",
                "enthusiasm_score", "number",
                "on_camera_state", "string"
        )));
        behavior.setStateMapping(new LinkedHashMap<>(Map.of(
                "read_from_state", List.of("session.normalized", "video.frames", "audio.transcript"),
                "write_to_state", Map.of("behavior_profile", "state.features.behavior")
        )));
        behavior.setScriptConfig(scriptConfig("function main(ctx){ return { output:{ response_speed_score:0.8, enthusiasm_score:0.75, on_camera_state:'active' }, writeToState:{ behavior_profile:'ok' } } }"));
        registerIfAbsent(behavior);

        SkillDefinition style = defaultSkill(
                "service_style_extract",
                "服务风格信号提取",
                "SEMANTIC",
                "genericSkillExecutor",
                "识别情感陪伴型、销售导向型等服务风格信号",
                List.of("style", "semantic", "service")
        );
        style.setOutputSchema(new LinkedHashMap<>(Map.of(
                "style_primary", "string",
                "style_confidence", "number"
        )));
        style.setStateMapping(new LinkedHashMap<>(Map.of(
                "read_from_state", List.of("audio.transcript", "features.behavior"),
                "write_to_state", Map.of("service_style", "state.features.style")
        )));
        style.setScriptConfig(scriptConfig("function main(ctx){ return { output:{ style_primary:'情感陪伴', style_confidence:0.81 }, writeToState:{ style:'情感陪伴' } } }"));
        registerIfAbsent(style);

        SkillDefinition riskClue = defaultSkill(
                "risk_clue_detect",
                "潜在风险线索探测",
                "GUARD",
                "genericSkillExecutor",
                "识别隐性导流、人设矛盾等潜在风险线索",
                List.of("risk", "clue", "guard")
        );
        riskClue.setOutputSchema(new LinkedHashMap<>(Map.of(
                "risk_clues", "array",
                "risk_score", "number"
        )));
        riskClue.setStateMapping(new LinkedHashMap<>(Map.of(
                "read_from_state", List.of("audio.transcript", "video.frames", "features.style"),
                "write_to_state", Map.of("risk_clues", "state.risk.clues")
        )));
        riskClue.setScriptConfig(scriptConfig("function main(ctx){ return { output:{ risk_clues:[], risk_score:0.2 }, writeToState:{ risk_level:'low' } } }"));
        registerIfAbsent(riskClue);

        SkillDefinition fusion = defaultSkill(
                "evidence_fusion",
                "多源证据融合",
                "DECISION",
                "genericSkillExecutor",
                "对语音、图像、语义标签进行交叉验证与置信度加权",
                List.of("fusion", "evidence", "profile")
        );
        fusion.setOutputSchema(new LinkedHashMap<>(Map.of(
                "quality_profile", "object",
                "fusion_confidence", "number"
        )));
        fusion.setStateMapping(new LinkedHashMap<>(Map.of(
                "read_from_state", List.of("features.behavior", "features.style", "risk.clues"),
                "write_to_state", Map.of("quality_profile", "state.profile.quality")
        )));
        fusion.setScriptConfig(scriptConfig("function main(ctx){ return { output:{ quality_profile:{ service_attitude:'good' }, fusion_confidence:0.84 }, writeToState:{ profile_ready:true } } }"));
        registerIfAbsent(fusion);

        SkillDefinition tags = defaultSkill(
                "governance_tag_generate",
                "治理标签生成",
                "OUTPUT",
                "genericSkillExecutor",
                "将融合结果输出为稳定、可解释、可复用的治理级标签",
                List.of("tag", "governance", "output")
        );
        tags.setOutputSchema(new LinkedHashMap<>(Map.of(
                "service_attitude", "string",
                "risk_level", "string",
                "interaction_style", "string",
                "service_health", "string"
        )));
        tags.setStateMapping(new LinkedHashMap<>(Map.of(
                "read_from_state", List.of("profile.quality", "risk.clues"),
                "write_to_state", Map.of("governance_tags", "state.tags.governance")
        )));
        tags.setScriptConfig(scriptConfig("function main(ctx){ return { output:{ service_attitude:'积极', risk_level:'低', interaction_style:'陪伴型', service_health:'健康' }, writeToState:{ published:true } } }"));
        registerIfAbsent(tags);
    }

    private void registerIfAbsent(SkillDefinition skillDefinition) {
        if (hasSkill(skillDefinition.getSkillId())) {
            return;
        }
        register(skillDefinition);
    }

    private void loadPersistedSkills() {
        if (skillDefinitionMapper == null || objectMapper == null) {
            return;
        }
        List<SkillDefinitionEntity> entities = skillDefinitionMapper.listAll();
        for (SkillDefinitionEntity entity : entities) {
            SkillDefinition definition = toDefinition(entity);
            skills.put(definition.getSkillId(), definition);
        }
    }

    private SkillDefinition toDefinition(SkillDefinitionEntity entity) {
        SkillDefinition definition = new SkillDefinition();
        definition.setSkillId(entity.getSkillId());
        definition.setName(entity.getName());
        definition.setType(entity.getType());
        definition.setDescription(entity.getDescription());
        definition.setTags(readList(entity.getTagsJson()));
        definition.setOutputSchema(readMap(entity.getOutputSchemaJson()));
        definition.setStateMapping(readMap(entity.getStateMappingJson()));
        definition.setExecutionConfig(readMap(entity.getExecutionConfigJson()));
        definition.setScriptConfig(readMap(entity.getScriptConfigJson()));
        definition.setStatus(entity.getStatus());
        definition.setTimeoutMs(entity.getTimeoutMs());
        definition.setVersion(entity.getVersion());
        definition.setExecutorBean(entity.getExecutorBean());
        if (isBlank(definition.getExecutorBean())) {
            definition.setExecutorBean(resolveExecutorBean(definition));
        }
        return definition;
    }

    private void persistSkill(SkillDefinition definition) {
        if (skillDefinitionMapper == null || objectMapper == null) {
            return;
        }
        SkillDefinitionEntity entity = new SkillDefinitionEntity();
        entity.setSkillId(definition.getSkillId());
        entity.setName(definition.getName());
        entity.setType(definition.getType());
        entity.setDescription(definition.getDescription());
        entity.setTagsJson(writeJson(definition.getTags() == null ? List.of() : definition.getTags()));
        entity.setOutputSchemaJson(writeJson(definition.getOutputSchema()));
        entity.setStateMappingJson(writeJson(definition.getStateMapping()));
        entity.setExecutionConfigJson(writeJson(definition.getExecutionConfig()));
        entity.setScriptConfigJson(writeJson(definition.getScriptConfig()));
        entity.setStatus(definition.getStatus());
        entity.setTimeoutMs(definition.getTimeoutMs());
        entity.setVersion(definition.getVersion());
        entity.setExecutorBean(definition.getExecutorBean());
        skillDefinitionMapper.upsert(entity);
    }

    private void deletePersistedSkill(String skillId) {
        if (skillDefinitionMapper == null) {
            return;
        }
        skillDefinitionMapper.deleteById(skillId);
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

    private SkillDefinition templateSkill(
            String templateId,
            String name,
            String type,
            String description,
            List<String> tags,
            Map<String, Object> outputSchema,
            Map<String, Object> stateMapping
    ) {
        SkillDefinition template = new SkillDefinition();
        template.setSkillId(templateId);
        template.setName(name);
        template.setType(type);
        template.setDescription(description);
        template.setTags(tags);
        template.setOutputSchema(new LinkedHashMap<>(outputSchema));
        template.setStateMapping(new LinkedHashMap<>(stateMapping));
        template.setExecutionConfig(new LinkedHashMap<>(Map.of(
                "execution_mode", "JAVA",
                "timeout", 3000,
                "llm_model", "",
                "temperature", 0.2D
        )));
        template.setScriptConfig(defaultScriptConfig());
        template.setStatus("TEMPLATE");
        template.setVersion("v1");
        template.setTimeoutMs(3000);
        template.setExecutorBean("genericSkillExecutor");
        return template;
    }

    private Map<String, Object> defaultScriptConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("language", "javascript");
        config.put("entry", "main");
        config.put("content", "function main(ctx){ return { output: {}, writeToState: {} } }");
        config.put("env", new LinkedHashMap<>());
        config.put("dependencies", List.of());
        return config;
    }

    private Map<String, Object> scriptConfig(String content) {
        Map<String, Object> config = defaultScriptConfig();
        config.put("content", content);
        return config;
    }

    private SkillDefinition deepCopy(SkillDefinition source) {
        SkillDefinition copy = new SkillDefinition();
        copy.setSkillId(source.getSkillId());
        copy.setName(source.getName());
        copy.setType(source.getType());
        copy.setDescription(source.getDescription());
        copy.setTags(source.getTags() == null ? List.of() : new ArrayList<>(source.getTags()));
        copy.setOutputSchema(source.getOutputSchema() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source.getOutputSchema()));
        copy.setStateMapping(source.getStateMapping() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source.getStateMapping()));
        copy.setExecutionConfig(source.getExecutionConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source.getExecutionConfig()));
        copy.setScriptConfig(source.getScriptConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source.getScriptConfig()));
        copy.setExecutorBean(source.getExecutorBean());
        copy.setTimeoutMs(source.getTimeoutMs());
        copy.setVersion(source.getVersion());
        copy.setStatus(source.getStatus());
        return copy;
    }

    private String resolveExecutorBean(SkillDefinition skillDefinition) {
        String configured = skillDefinition.getExecutorBean() == null ? "" : skillDefinition.getExecutorBean().trim();
        String normalizedMode = resolveExecutionMode(skillDefinition.getExecutionConfig());
        if ("LLM".equals(normalizedMode)) {
            return "llmSkillExecutor";
        }
        if ("ASR".equals(normalizedMode)) {
            return "asrSkillExecutor";
        }
        if (isPythonMode(normalizedMode)) {
            return pythonExecutionEnabled() ? "pythonSkillExecutor" : "llmSkillExecutor";
        }
        if (!isBlank(configured)) {
            if ("pythonSkillExecutor".equals(configured)) {
                return pythonExecutionEnabled() ? "pythonSkillExecutor" : "llmSkillExecutor";
            }
            if ("genericSkillExecutor".equals(configured) && hasLlmHints(skillDefinition)) {
                return "llmSkillExecutor";
            }
            return configured;
        }
        SkillDefinition existed = findExistingSkill(skillDefinition.getSkillId());
        if (existed != null && !isBlank(existed.getExecutorBean())) {
            String existedBean = existed.getExecutorBean().trim();
            if ("pythonSkillExecutor".equals(existedBean)) {
                return pythonExecutionEnabled() ? "pythonSkillExecutor" : "llmSkillExecutor";
            }
            if ("genericSkillExecutor".equals(existedBean) && hasLlmHints(skillDefinition)) {
                return "llmSkillExecutor";
            }
            return existedBean;
        }
        String builtIn = switch (skillDefinition.getSkillId()) {
            case "role_detect" -> "roleDetectSkillExecutor";
            case "video_parse" -> "videoParseSkillExecutor";
            case "asr" -> "asrSkillExecutor";
            case "semantic_analysis" -> "semanticAnalysisSkillExecutor";
            case "violation_call_in_bed" -> "violationCallInBedSkillExecutor";
            case "violation_black_screen" -> "violationBlackScreenSkillExecutor";
            case "violation_aggregate" -> "violationAggregateSkillExecutor";
            default -> "";
        };
        if (!builtIn.isBlank()) {
            return builtIn;
        }
        if (hasLlmHints(skillDefinition)) {
            return "llmSkillExecutor";
        }
        return "genericSkillExecutor";
    }

    private boolean hasLlmHints(SkillDefinition definition) {
        Map<String, Object> executionConfig = definition.getExecutionConfig() == null ? Map.of() : definition.getExecutionConfig();
        Map<String, Object> scriptConfig = definition.getScriptConfig() == null ? Map.of() : definition.getScriptConfig();
        String llmConfigCode = String.valueOf(executionConfig.getOrDefault("llm_config_code", "")).trim();
        String llmModel = String.valueOf(executionConfig.getOrDefault("llm_model", "")).trim();
        String prompt = String.valueOf(scriptConfig.getOrDefault("prompt", "")).trim();
        String language = String.valueOf(scriptConfig.getOrDefault("language", "")).trim();
        return !llmConfigCode.isBlank()
                || !llmModel.isBlank()
                || !prompt.isBlank()
                || "javascript".equalsIgnoreCase(language)
                || "python".equalsIgnoreCase(language);
    }

    private String resolveExecutionMode(Map<String, Object> executionConfig) {
        Object modeValue = executionConfig == null ? null : executionConfig.get("execution_mode");
        if (modeValue == null && executionConfig != null) {
            modeValue = executionConfig.get("executionMode");
        }
        if (modeValue == null && executionConfig != null) {
            modeValue = executionConfig.get("mode");
        }
        return String.valueOf(modeValue == null ? "" : modeValue).trim().toUpperCase();
    }

    private boolean isPythonMode(String mode) {
        return "PYTHON".equals(mode) || "PY".equals(mode) || "PY_SCRIPT".equals(mode);
    }

    private boolean pythonExecutionEnabled() {
        return false;
    }

    private String normalizeType(String type) {
        String raw = type == null ? "" : type.trim();
        String alias = TYPE_ALIASES.get(raw);
        if (alias != null) {
            return alias;
        }
        return raw.toUpperCase();
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private boolean isSqlReadMode() {
        return skillDefinitionMapper != null && objectMapper != null;
    }

    private boolean hasSkill(String skillId) {
        if (isSqlReadMode()) {
            return skillDefinitionMapper.selectById(skillId) != null;
        }
        return skills.containsKey(skillId);
    }

    private SkillDefinition findExistingSkill(String skillId) {
        if (isSqlReadMode()) {
            SkillDefinitionEntity entity = skillDefinitionMapper.selectById(skillId);
            return entity == null ? null : toDefinition(entity);
        }
        return skills.get(skillId);
    }
}
