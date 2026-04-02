package com.moderation.service.impl;

import com.moderation.llm.GeminiApiSupport;
import com.moderation.model.req.ApiConnectionTestReq;
import com.moderation.model.res.ApiConnectionTestRes;
import com.moderation.service.LlmProfileService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiConfigServiceImplTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testLlmShouldSucceedWithValidAuthorization() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.createContext("/v1/chat/completions", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            int code = "Bearer valid-key".equals(auth) ? 200 : 401;
            byte[] body = "{\"choices\":[{\"message\":{\"content\":\"pong\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        int port = server.getAddress().getPort();

        LlmProfileService profileService = Mockito.mock(LlmProfileService.class);
        Mockito.when(profileService.findByCode("DEEPSEEK")).thenReturn(Optional.of(
                new LlmProfileService.LlmRuntimeProfile(
                        "DEEPSEEK", "deepseek", "http://127.0.0.1:" + port, "deepseek-chat", "valid-key", 3000, 1
                )
        ));
        ApiConfigServiceImpl service = new ApiConfigServiceImpl(profileService);

        ApiConnectionTestReq req = new ApiConnectionTestReq();
        req.setConfigCode("DEEPSEEK");
        ApiConnectionTestRes res = service.testLlm(req);

        assertEquals(200, res.getStatusCode(), res.getMessage());
        assertTrue(res.getSuccess());
        assertEquals("连接成功", res.getMessage());
    }

    @Test
    void geminiResolverShouldFallbackToDefaultModelWhenEndpointLooksLikeGemini() {
        String model = GeminiApiSupport.resolveTestModel(
                "deepseek",
                "https://generativelanguage.googleapis.com",
                "deepseek-chat"
        );

        assertEquals(GeminiApiSupport.DEFAULT_TEST_MODEL, model);
        assertEquals(
                "https://generativelanguage.googleapis.com/v1beta/models/" + GeminiApiSupport.DEFAULT_TEST_MODEL + ":generateContent?key=test-key",
                GeminiApiSupport.buildGenerateContentEndpoint(
                        "https://generativelanguage.googleapis.com",
                        model,
                        "test-key"
                )
        );
    }

    @Test
    void testLlmShouldFailWhenApiKeyMissing() {
        LlmProfileService profileService = Mockito.mock(LlmProfileService.class);
        Mockito.when(profileService.findByCode("DEFAULT")).thenReturn(Optional.of(
                new LlmProfileService.LlmRuntimeProfile(
                        "DEFAULT", "byteplus", "https://example.com", "seed-2-0-lite-260228", "", 3000, 1
                )
        ));
        ApiConfigServiceImpl service = new ApiConfigServiceImpl(profileService);

        ApiConnectionTestReq req = new ApiConnectionTestReq();
        req.setConfigCode("DEFAULT");
        ApiConnectionTestRes res = service.testLlm(req);

        assertFalse(res.getSuccess());
        assertEquals(0, res.getStatusCode());
        assertEquals("API Key 未配置", res.getMessage());
    }
}
