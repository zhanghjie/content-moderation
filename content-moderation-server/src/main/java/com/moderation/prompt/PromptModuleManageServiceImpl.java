package com.moderation.prompt;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moderation.config.LLMProperties;
import com.moderation.entity.PromptModuleEntity;
import com.moderation.entity.PromptModuleSetEntity;
import com.moderation.mapper.PromptModuleMapper;
import com.moderation.mapper.PromptModuleSetMapper;
import com.moderation.model.req.PromptDefaultModulesReq;
import com.moderation.model.req.PromptModuleSaveReq;
import com.moderation.model.res.PromptModuleManageRes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromptModuleManageServiceImpl implements PromptModuleManageService {

    private final PromptModuleMapper promptModuleMapper;
    private final PromptModuleSetMapper promptModuleSetMapper;
    private final ObjectMapper objectMapper;
    private final LLMProperties llmProperties;

    @PostConstruct
    public void init() {
        String analysisType = "HOST_VIOLATION";
        Long count = promptModuleMapper.selectCount(new QueryWrapper<PromptModuleEntity>()
                .eq("analysis_type", analysisType));
        if (count != null && count > 0) return;
        LLMProperties.PromptModuleSet set = llmProperties.getPromptModules().getHostViolation();
        int i = 0;
        if (set.getModules() != null) {
            for (Map.Entry<String, String> entry : set.getModules().entrySet()) {
                PromptModuleEntity entity = new PromptModuleEntity();
                entity.setAnalysisType(analysisType);
                entity.setCode(entry.getKey());
                entity.setTitle(toTitle(entry.getKey()));
                entity.setCategory(inferCategory(entry.getKey()));
                entity.setContent(entry.getValue());
                entity.setEnabled(true);
                entity.setSortOrder(i++);
                promptModuleMapper.insert(entity);
            }
        }
        PromptModuleSetEntity moduleSet = new PromptModuleSetEntity();
        moduleSet.setAnalysisType(analysisType);
        moduleSet.setDefaultModules(writeJson(set.getDefaultModules()));
        moduleSet.setUpdatedAt(OffsetDateTime.now());
        promptModuleSetMapper.insert(moduleSet);
    }

    @Override
    public PromptModuleManageRes getModules(String analysisType) {
        String type = normalizeType(analysisType);
        List<PromptModuleEntity> modules = listModules(type);
        PromptModuleSetEntity set = getModuleSet(type);
        return PromptModuleManageRes.builder()
                .analysisType(type)
                .defaultModules(readJsonList(set == null ? null : set.getDefaultModules()))
                .modules(modules.stream().map(this::toDetail).toList())
                .build();
    }

    @Override
    public PromptModuleManageRes saveModule(PromptModuleSaveReq req) {
        String type = normalizeType(req.getAnalysisType());
        if (!hasText(req.getCode()) || !hasText(req.getContent())) {
            throw new IllegalArgumentException("code 和 content 不能为空");
        }
        QueryWrapper<PromptModuleEntity> q = new QueryWrapper<PromptModuleEntity>()
                .eq("analysis_type", type)
                .eq("code", req.getCode().trim().toUpperCase(Locale.ROOT));
        PromptModuleEntity existed = promptModuleMapper.selectOne(q);
        if (existed != null) {
            throw new IllegalArgumentException("模块编码已存在");
        }
        PromptModuleEntity entity = new PromptModuleEntity();
        entity.setAnalysisType(type);
        entity.setCode(req.getCode().trim().toUpperCase(Locale.ROOT));
        entity.setTitle(hasText(req.getTitle()) ? req.getTitle().trim() : toTitle(entity.getCode()));
        entity.setCategory(normalizeCategory(req.getCategory()));
        entity.setContent(req.getContent().trim());
        entity.setEnabled(req.getEnabled() == null ? true : req.getEnabled());
        entity.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        promptModuleMapper.insert(entity);
        ensureModuleSet(type);
        return getModules(type);
    }

    @Override
    public PromptModuleManageRes updateModule(String analysisType, String code, PromptModuleSaveReq req) {
        String type = normalizeType(analysisType);
        String moduleCode = normalizeCode(code);
        PromptModuleEntity existed = promptModuleMapper.selectOne(new QueryWrapper<PromptModuleEntity>()
                .eq("analysis_type", type)
                .eq("code", moduleCode));
        if (existed == null) {
            throw new IllegalArgumentException("模块不存在");
        }
        if (hasText(req.getTitle())) existed.setTitle(req.getTitle().trim());
        if (hasText(req.getCategory())) existed.setCategory(normalizeCategory(req.getCategory()));
        if (hasText(req.getContent())) existed.setContent(req.getContent().trim());
        if (req.getEnabled() != null) existed.setEnabled(req.getEnabled());
        if (req.getSortOrder() != null) existed.setSortOrder(req.getSortOrder());
        promptModuleMapper.updateById(existed);
        return getModules(type);
    }

    @Override
    public PromptModuleManageRes deleteModule(String analysisType, String code) {
        String type = normalizeType(analysisType);
        String moduleCode = normalizeCode(code);
        promptModuleMapper.delete(new QueryWrapper<PromptModuleEntity>()
                .eq("analysis_type", type)
                .eq("code", moduleCode));

        PromptModuleSetEntity set = getModuleSet(type);
        if (set != null) {
            List<String> defaults = readJsonList(set.getDefaultModules()).stream()
                    .filter(c -> !moduleCode.equals(c))
                    .toList();
            set.setDefaultModules(writeJson(defaults));
            set.setUpdatedAt(OffsetDateTime.now());
            promptModuleSetMapper.updateById(set);
        }
        return getModules(type);
    }

    @Override
    public PromptModuleManageRes saveDefaultModules(PromptDefaultModulesReq req) {
        String type = normalizeType(req.getAnalysisType());
        List<String> defaults = req.getDefaultModules() == null ? List.of() : req.getDefaultModules().stream()
                .filter(this::hasText)
                .map(this::normalizeCode)
                .toList();
        List<String> existingCodes = listModules(type).stream().map(PromptModuleEntity::getCode).toList();
        for (String code : defaults) {
            if (!existingCodes.contains(code)) {
                throw new IllegalArgumentException("默认模块不存在: " + code);
            }
        }
        PromptModuleSetEntity set = ensureModuleSet(type);
        set.setDefaultModules(writeJson(defaults));
        set.setUpdatedAt(OffsetDateTime.now());
        promptModuleSetMapper.updateById(set);
        return getModules(type);
    }

    @Override
    public PromptModuleManageRes replaceModules(String analysisType, List<PromptModuleSaveReq> modules) {
        String type = normalizeType(analysisType);
        promptModuleMapper.delete(new QueryWrapper<PromptModuleEntity>().eq("analysis_type", type));
        List<String> defaults = new ArrayList<>();
        List<PromptModuleSaveReq> source = modules == null ? List.of() : modules;
        int order = 0;
        for (PromptModuleSaveReq req : source) {
            if (req == null || !hasText(req.getCode()) || !hasText(req.getContent())) continue;
            PromptModuleEntity entity = new PromptModuleEntity();
            entity.setAnalysisType(type);
            entity.setCode(normalizeCode(req.getCode()));
            entity.setTitle(hasText(req.getTitle()) ? req.getTitle().trim() : toTitle(entity.getCode()));
            entity.setCategory(normalizeCategory(req.getCategory()));
            entity.setContent(req.getContent().trim());
            entity.setEnabled(req.getEnabled() == null || req.getEnabled());
            entity.setSortOrder(req.getSortOrder() == null ? order : req.getSortOrder());
            entity.setUpdatedAt(OffsetDateTime.now());
            promptModuleMapper.insert(entity);
            if (Boolean.TRUE.equals(entity.getEnabled())) defaults.add(entity.getCode());
            order += 1;
        }
        PromptModuleSetEntity set = ensureModuleSet(type);
        set.setDefaultModules(writeJson(defaults));
        set.setUpdatedAt(OffsetDateTime.now());
        promptModuleSetMapper.updateById(set);
        return getModules(type);
    }

    @Override
    public List<String> resolveModules(String analysisType, List<String> moduleCodes) {
        String type = normalizeType(analysisType);
        List<String> selected = moduleCodes == null || moduleCodes.isEmpty()
                ? readJsonList(getOrDefaultSet(type).getDefaultModules())
                : moduleCodes.stream().filter(this::hasText).map(this::normalizeCode).toList();
        Map<String, String> contentMap = moduleContentMap(type);
        return selected.stream().filter(contentMap::containsKey).toList();
    }

    @Override
    public Map<String, String> moduleContentMap(String analysisType) {
        String type = normalizeType(analysisType);
        List<PromptModuleEntity> modules = listModules(type);
        Map<String, String> map = new LinkedHashMap<>();
        for (PromptModuleEntity module : modules) {
            if (Boolean.TRUE.equals(module.getEnabled())) {
                map.put(module.getCode(), module.getContent());
            }
        }
        return map;
    }

    private PromptModuleSetEntity getOrDefaultSet(String analysisType) {
        PromptModuleSetEntity set = getModuleSet(analysisType);
        if (set != null) return set;
        return ensureModuleSet(analysisType);
    }

    private PromptModuleSetEntity getModuleSet(String analysisType) {
        return promptModuleSetMapper.selectOne(new QueryWrapper<PromptModuleSetEntity>()
                .eq("analysis_type", analysisType));
    }

    private PromptModuleSetEntity ensureModuleSet(String analysisType) {
        PromptModuleSetEntity set = getModuleSet(analysisType);
        if (set != null) return set;
        set = new PromptModuleSetEntity();
        set.setAnalysisType(analysisType);
        set.setDefaultModules(writeJson(new ArrayList<>()));
        set.setUpdatedAt(OffsetDateTime.now());
        promptModuleSetMapper.insert(set);
        return set;
    }

    private List<PromptModuleEntity> listModules(String analysisType) {
        return promptModuleMapper.selectList(new QueryWrapper<PromptModuleEntity>()
                .eq("analysis_type", analysisType)
                .orderByAsc("sort_order")
                .orderByAsc("code"));
    }

    private PromptModuleManageRes.PromptModuleDetail toDetail(PromptModuleEntity entity) {
        return PromptModuleManageRes.PromptModuleDetail.builder()
                .code(entity.getCode())
                .title(entity.getTitle())
                .category(entity.getCategory())
                .content(entity.getContent())
                .enabled(entity.getEnabled())
                .sortOrder(entity.getSortOrder())
                .build();
    }

    private List<String> readJsonList(String value) {
        if (!hasText(value)) return new ArrayList<>();
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeJson(List<String> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new ArrayList<>() : value);
        } catch (Exception e) {
            throw new RuntimeException("serialize default modules failed");
        }
    }

    private String normalizeType(String analysisType) {
        return hasText(analysisType) ? analysisType.trim().toUpperCase(Locale.ROOT) : "HOST_VIOLATION";
    }

    private String normalizeCode(String code) {
        if (!hasText(code)) throw new IllegalArgumentException("code 不能为空");
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCategory(String category) {
        if (!hasText(category)) return "PLUGGABLE";
        String c = category.trim().toUpperCase(Locale.ROOT);
        if (!List.of("REQUIRED", "PLUGGABLE", "FREE").contains(c)) {
            throw new IllegalArgumentException("category 仅支持 REQUIRED/PLUGGABLE/FREE");
        }
        return c;
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

    private String toTitle(String code) {
        return switch (code) {
            case "HOST_VIOLATION_BASE" -> "任务说明与核心原则";
            case "HOST_VIOLATION_RULES" -> "违规类型判定规则";
            case "HOST_VIOLATION_JSON" -> "JSON 输出规范";
            default -> code;
        };
    }

    private String inferCategory(String code) {
        if (Objects.equals(code, "HOST_VIOLATION_BASE") || Objects.equals(code, "HOST_VIOLATION_JSON")) {
            return "REQUIRED";
        }
        return "PLUGGABLE";
    }
}
