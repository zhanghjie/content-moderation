package com.moderation.service;

import com.moderation.model.req.ApiConnectionTestReq;
import com.moderation.model.req.LlmProfileSaveReq;
import com.moderation.model.res.ApiConnectionTestRes;
import com.moderation.model.res.LlmProfilesRes;

public interface ApiConfigService {
    LlmProfilesRes getConfig();
    LlmProfilesRes saveConfig(LlmProfileSaveReq req);
    LlmProfilesRes deleteConfig(String configCode);
    LlmProfilesRes setDefaultConfig(String configCode);
    ApiConnectionTestRes testLlm(ApiConnectionTestReq req);
    ApiConnectionTestRes testYidun(ApiConnectionTestReq req);
}
