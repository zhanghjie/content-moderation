package com.moderation.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模型路由器
 * 根据内容类型选择合适的审核模型
 */
@Component
@Slf4j
public class ModelRouter {
    
    private final List<ContentModerationModel> models;
    private final ContentModerationModel backupModel;
    private final Map<ModelType, ContentModerationModel> modelMap;
    
    public ModelRouter(List<ContentModerationModel> models, ContentModerationModel backupModel) {
        this.models = models;
        this.backupModel = backupModel;
        this.modelMap = models.stream()
                .collect(Collectors.toMap(
                        ContentModerationModel::getModelType,
                        Function.identity()
                ));
    }
    
    /**
     * 根据内容类型选择模型
     */
    public ContentModerationModel selectModel(ContentType contentType) {
        for (ContentModerationModel model : models) {
            if (model.supports(contentType)) {
                log.info("Selected model: {} for content type: {}", 
                        model.getModelType(), contentType);
                return model;
            }
        }
        
        log.warn("No model found for content type: {}, using backup", contentType);
        return backupModel;
    }
    
    /**
     * 根据模型类型获取模型
     */
    public ContentModerationModel getModel(ModelType modelType) {
        ContentModerationModel model = modelMap.get(modelType);
        if (model == null) {
            log.warn("Model not found: {}, using backup", modelType);
            return backupModel;
        }
        return model;
    }
}
