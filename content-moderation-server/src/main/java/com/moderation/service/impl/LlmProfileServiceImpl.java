package com.moderation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moderation.entity.LlmConfigProfileEntity;
import com.moderation.mapper.LlmConfigProfileMapper;
import com.moderation.model.req.LlmProfileSaveReq;
import com.moderation.model.res.LlmProfileRes;
import com.moderation.model.res.LlmProfilesRes;
import com.moderation.security.SecretCryptoService;
import com.moderation.service.LlmProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LlmProfileServiceImpl implements LlmProfileService {

    private final LlmConfigProfileMapper mapper;
    private final SecretCryptoService cryptoService;

    @Override
    public LlmProfilesRes listProfiles() {
        ensureDefaultProfile();
        List<LlmConfigProfileEntity> list = mapper.selectList(new LambdaQueryWrapper<LlmConfigProfileEntity>()
                .orderByDesc(LlmConfigProfileEntity::getIsDefault)
                .orderByAsc(LlmConfigProfileEntity::getCreatedAt));
        String defaultCode = list.stream().filter(i -> Boolean.TRUE.equals(i.getIsDefault()))
                .map(LlmConfigProfileEntity::getConfigCode).findFirst().orElse(null);
        return LlmProfilesRes.builder()
                .defaultConfigCode(defaultCode)
                .profiles(list.stream().map(this::toRes).toList())
                .build();
    }

    @Override
    @Transactional
    public LlmProfilesRes saveProfile(LlmProfileSaveReq req) {
        validate(req);
        String code = req.getConfigCode().trim().toUpperCase();
        LlmConfigProfileEntity entity = mapper.selectOne(new LambdaQueryWrapper<LlmConfigProfileEntity>()
                .eq(LlmConfigProfileEntity::getConfigCode, code));
        boolean creating = entity == null;
        if (creating) entity = new LlmConfigProfileEntity();
        entity.setConfigCode(code);
        entity.setDisplayName(req.getDisplayName().trim());
        entity.setProvider(emptyAs(req.getProvider(), "byteplus"));
        entity.setEndpoint(req.getEndpoint().trim());
        entity.setModel(req.getModel().trim());
        if (req.getApiKey() != null && !req.getApiKey().isBlank()) {
            entity.setApiKeyEnc(cryptoService.encrypt(req.getApiKey().trim()));
        } else if (creating) {
            throw new IllegalArgumentException("apiKey不能为空");
        }
        entity.setTimeoutMs(req.getTimeoutMs() == null ? 120000 : req.getTimeoutMs());
        entity.setMaxTokens(req.getMaxTokens() == null ? 3000 : req.getMaxTokens());
        entity.setEnabled(req.getEnabled() == null || req.getEnabled());
        entity.setIsDefault(Boolean.TRUE.equals(req.getIsDefault()));
        if (creating) mapper.insert(entity); else mapper.updateById(entity);
        if (Boolean.TRUE.equals(entity.getIsDefault())) {
            clearOtherDefault(entity.getConfigCode());
        }
        ensureOneDefault();
        return listProfiles();
    }

    @Override
    @Transactional
    public LlmProfilesRes deleteProfile(String configCode) {
        mapper.delete(new LambdaQueryWrapper<LlmConfigProfileEntity>()
                .eq(LlmConfigProfileEntity::getConfigCode, configCode == null ? null : configCode.trim().toUpperCase()));
        ensureOneDefault();
        return listProfiles();
    }

    @Override
    @Transactional
    public LlmProfilesRes setDefault(String configCode) {
        String code = configCode == null ? null : configCode.trim().toUpperCase();
        LlmConfigProfileEntity entity = mapper.selectOne(new LambdaQueryWrapper<LlmConfigProfileEntity>()
                .eq(LlmConfigProfileEntity::getConfigCode, code));
        if (entity == null) throw new IllegalArgumentException("配置不存在");
        clearOtherDefault(code);
        entity.setIsDefault(true);
        mapper.updateById(entity);
        return listProfiles();
    }

    @Override
    public Optional<LlmRuntimeProfile> findDefaultEnabled() {
        ensureDefaultProfile();
        LlmConfigProfileEntity entity = mapper.selectOne(new LambdaQueryWrapper<LlmConfigProfileEntity>()
                .eq(LlmConfigProfileEntity::getIsDefault, true)
                .eq(LlmConfigProfileEntity::getEnabled, true)
                .last("limit 1"));
        return Optional.ofNullable(entity).map(this::toRuntime);
    }

    @Override
    public Optional<LlmRuntimeProfile> findByCode(String configCode) {
        if (configCode == null || configCode.isBlank()) return Optional.empty();
        LlmConfigProfileEntity entity = mapper.selectOne(new LambdaQueryWrapper<LlmConfigProfileEntity>()
                .eq(LlmConfigProfileEntity::getConfigCode, configCode.trim().toUpperCase())
                .eq(LlmConfigProfileEntity::getEnabled, true)
                .last("limit 1"));
        return Optional.ofNullable(entity).map(this::toRuntime);
    }

    private LlmRuntimeProfile toRuntime(LlmConfigProfileEntity item) {
        return new LlmRuntimeProfile(
                item.getConfigCode(),
                item.getProvider(),
                item.getEndpoint(),
                item.getModel(),
                cryptoService.decrypt(item.getApiKeyEnc()),
                item.getTimeoutMs(),
                item.getMaxTokens()
        );
    }

    private LlmProfileRes toRes(LlmConfigProfileEntity item) {
        return LlmProfileRes.builder()
                .configCode(item.getConfigCode())
                .displayName(item.getDisplayName())
                .provider(item.getProvider())
                .endpoint(item.getEndpoint())
                .model(item.getModel())
                .timeoutMs(item.getTimeoutMs())
                .maxTokens(item.getMaxTokens())
                .enabled(item.getEnabled())
                .isDefault(item.getIsDefault())
                .apiKeyConfigured(item.getApiKeyEnc() != null && !item.getApiKeyEnc().isBlank())
                .build();
    }

    private void validate(LlmProfileSaveReq req) {
        if (req == null) throw new IllegalArgumentException("参数不能为空");
        if (req.getConfigCode() == null || req.getConfigCode().isBlank()) throw new IllegalArgumentException("configCode不能为空");
        if (req.getDisplayName() == null || req.getDisplayName().isBlank()) throw new IllegalArgumentException("displayName不能为空");
        if (req.getEndpoint() == null || req.getEndpoint().isBlank()) throw new IllegalArgumentException("endpoint不能为空");
        if (req.getModel() == null || req.getModel().isBlank()) throw new IllegalArgumentException("model不能为空");
    }

    private String emptyAs(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private void clearOtherDefault(String code) {
        mapper.update(null, new LambdaUpdateWrapper<LlmConfigProfileEntity>()
                .set(LlmConfigProfileEntity::getIsDefault, false)
                .ne(LlmConfigProfileEntity::getConfigCode, code));
    }

    private void ensureOneDefault() {
        LlmConfigProfileEntity currentDefault = mapper.selectOne(new LambdaQueryWrapper<LlmConfigProfileEntity>()
                .eq(LlmConfigProfileEntity::getIsDefault, true)
                .last("limit 1"));
        if (currentDefault != null) return;
        LlmConfigProfileEntity first = mapper.selectOne(new LambdaQueryWrapper<LlmConfigProfileEntity>()
                .orderByAsc(LlmConfigProfileEntity::getCreatedAt)
                .last("limit 1"));
        if (first != null) {
            first.setIsDefault(true);
            mapper.updateById(first);
        }
    }

    private void ensureDefaultProfile() {
        long count = mapper.selectCount(new LambdaQueryWrapper<>());
        if (count > 0) return;
        LlmConfigProfileEntity seed = new LlmConfigProfileEntity();
        seed.setConfigCode("DEFAULT");
        seed.setDisplayName("默认模型");
        seed.setProvider("byteplus");
        seed.setEndpoint("https://ark.ap-southeast.bytepluses.com/api/v3/chat/completions");
        seed.setModel("seed-2-0-lite-260228");
        seed.setApiKeyEnc(cryptoService.encrypt("your-api-key"));
        seed.setTimeoutMs(120000);
        seed.setMaxTokens(3000);
        seed.setEnabled(true);
        seed.setIsDefault(true);
        mapper.insert(seed);
    }
}
