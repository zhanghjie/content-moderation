package com.moderation.prompt;

import com.moderation.model.res.PromptModuleManageRes;
import com.moderation.model.res.PromptModulesRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PromptCatalogService {

    private final PromptModuleManageService promptModuleManageService;

    public PromptModulesRes getHostViolationModules() {
        PromptModuleManageRes managed = promptModuleManageService.getModules("HOST_VIOLATION");
        List<PromptModulesRes.PromptModuleItem> items = managed.getModules().stream()
                .filter(m -> Boolean.TRUE.equals(m.getEnabled()))
                .map(m -> PromptModulesRes.PromptModuleItem.builder()
                        .code(m.getCode())
                        .title(m.getTitle())
                        .category(m.getCategory())
                        .build())
                .toList();
        return PromptModulesRes.builder()
                .analysisType("HOST_VIOLATION")
                .defaultModules(managed.getDefaultModules())
                .modules(items)
                .build();
    }
}
