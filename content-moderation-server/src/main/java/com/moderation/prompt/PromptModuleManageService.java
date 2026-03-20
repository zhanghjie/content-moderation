package com.moderation.prompt;

import com.moderation.model.req.PromptDefaultModulesReq;
import com.moderation.model.req.PromptModuleSaveReq;
import com.moderation.model.res.PromptModuleManageRes;

import java.util.List;
import java.util.Map;

public interface PromptModuleManageService {
    PromptModuleManageRes getModules(String analysisType);
    PromptModuleManageRes saveModule(PromptModuleSaveReq req);
    PromptModuleManageRes updateModule(String analysisType, String code, PromptModuleSaveReq req);
    PromptModuleManageRes deleteModule(String analysisType, String code);
    PromptModuleManageRes saveDefaultModules(PromptDefaultModulesReq req);
    PromptModuleManageRes replaceModules(String analysisType, List<PromptModuleSaveReq> modules);
    List<String> resolveModules(String analysisType, List<String> moduleCodes);
    Map<String, String> moduleContentMap(String analysisType);
}
