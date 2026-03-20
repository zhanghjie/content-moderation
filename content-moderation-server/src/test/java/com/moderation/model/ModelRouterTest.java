package com.moderation.model;

import com.moderation.model.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 模型路由器测试
 */
class ModelRouterTest {
    
    private ContentModerationModel videoModel;
    private ContentModerationModel backupModel;
    private ModelRouter modelRouter;
    
    @BeforeEach
    void setUp() {
        // 创建 Mock 模型
        videoModel = mock(BytePlusVideoModel.class);
        when(videoModel.supports(ContentType.VIDEO)).thenReturn(true);
        when(videoModel.supports(any())).thenAnswer(i -> 
            i.getArgument(0) == ContentType.VIDEO);
        when(videoModel.getModelType()).thenReturn(ModelType.BYTEPLUS_VIDEO);
        
        backupModel = mock(BackupModel.class);
        when(backupModel.supports(any())).thenReturn(true);
        when(backupModel.getModelType()).thenReturn(ModelType.BACKUP_MODEL);
        
        modelRouter = new ModelRouter(List.of(videoModel), backupModel);
    }
    
    @Test
    void should_SelectVideoModel_When_VideoContent() {
        // When
        ContentModerationModel selected = modelRouter.selectModel(ContentType.VIDEO);
        
        // Then
        assertThat(selected).isEqualTo(videoModel);
    }
    
    @Test
    void should_SelectBackupModel_When_NoModelFound() {
        // When
        ContentModerationModel selected = modelRouter.selectModel(ContentType.TEXT);
        
        // Then
        assertThat(selected).isEqualTo(backupModel);
    }
    
    @Test
    void should_ReturnBackupModel_When_GetModelNotFound() {
        // When
        ContentModerationModel model = modelRouter.getModel(ModelType.BYTEPLUS_TEXT);
        
        // Then
        assertThat(model).isEqualTo(backupModel);
    }
    
    @Test
    void should_ReturnVideoModel_When_GetModelFound() {
        // When
        ContentModerationModel model = modelRouter.getModel(ModelType.BYTEPLUS_VIDEO);
        
        // Then
        assertThat(model).isEqualTo(videoModel);
    }
}
