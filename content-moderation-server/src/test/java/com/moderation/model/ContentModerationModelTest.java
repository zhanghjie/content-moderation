package com.moderation.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * 内容审核模型接口测试
 * TDD 第一个测试：验证接口可以被实现和调用
 */
class ContentModerationModelTest {
    
    @Test
    void test_Interface_CanBeImplemented() {
        // Given: 创建一个测试实现
        ContentModerationModel testModel = new ContentModerationModel() {
            @Override
            public ModerationResult moderate(ContentContent content) {
                return ModerationResult.builder()
                        .result("PASS")
                        .confidence(1.0)
                        .success(true)
                        .build();
            }
            
            @Override
            public ModelType getModelType() {
                return ModelType.BACKUP_MODEL;
            }
            
            @Override
            public boolean supports(ContentType contentType) {
                return true;
            }
        };
        
        // When: 调用接口方法
        ContentContent content = ContentContent.builder()
                .contentId("test-001")
                .contentType(ContentType.VIDEO)
                .contentUrl("https://example.com/video.mp4")
                .build();
        
        ModerationResult result = testModel.moderate(content);
        
        // Then: 验证结果
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isEqualTo("PASS");
        assertThat(result.getConfidence()).isEqualTo(1.0);
    }
    
    @Test
    void test_ModelType_ShouldHaveCorrectInfo() {
        // When & Then: 验证枚举值
        assertThat(ModelType.BYTEPLUS_VIDEO).isNotNull();
        assertThat(ModelType.BYTEPLUS_VIDEO.getCode()).isEqualTo("byteplus-video");
        assertThat(ModelType.BACKUP_MODEL.getCode()).isEqualTo("backup");
    }
    
    @Test
    void test_ContentType_ShouldHaveAllTypes() {
        // When & Then: 验证所有类型
        assertThat(ContentType.values()).hasSize(4);
        assertThat(ContentType.VIDEO).isNotNull();
        assertThat(ContentType.IMAGE).isNotNull();
        assertThat(ContentType.TEXT).isNotNull();
        assertThat(ContentType.AUDIO).isNotNull();
    }
}
