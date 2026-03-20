package com.moderation.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.service.LlmProfileService;
import com.moderation.prompt.PromptModuleManageService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PromptSplitServiceImplTest {

    @Test
    void should_append_deepseek_chat_path_for_host_endpoint() throws Exception {
        PromptSplitServiceImpl service = new PromptSplitServiceImpl(
                mock(LlmProfileService.class),
                mock(PromptModuleManageService.class),
                new ObjectMapper()
        );
        Method method = PromptSplitServiceImpl.class.getDeclaredMethod("normalizeChatCompletionsEndpoint", String.class, String.class);
        method.setAccessible(true);

        String endpoint = (String) method.invoke(service, "deepseek", "https://api.deepseek.com");

        assertThat(endpoint).isEqualTo("https://api.deepseek.com/v1/chat/completions");
    }
}
