package com.moderation.service;

import com.moderation.model.req.LlmProfileSaveReq;
import com.moderation.model.res.LlmProfileRes;
import com.moderation.model.res.LlmProfilesRes;

import java.util.Optional;

public interface LlmProfileService {
    LlmProfilesRes listProfiles();
    LlmProfilesRes saveProfile(LlmProfileSaveReq req);
    LlmProfilesRes deleteProfile(String configCode);
    LlmProfilesRes setDefault(String configCode);
    Optional<LlmRuntimeProfile> findDefaultEnabled();
    Optional<LlmRuntimeProfile> findByCode(String configCode);

    record LlmRuntimeProfile(
            String configCode,
            String provider,
            String endpoint,
            String model,
            String apiKey,
            Integer timeoutMs,
            Integer maxTokens
    ) {}
}
