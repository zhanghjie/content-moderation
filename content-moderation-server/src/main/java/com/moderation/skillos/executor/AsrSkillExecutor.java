package com.moderation.skillos.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.service.LlmProfileService;
import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.model.SkillResult;
import com.moderation.skillos.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component("asrSkillExecutor")
@RequiredArgsConstructor
@Slf4j
public class AsrSkillExecutor implements SkillExecutor {
    private static final String DEFAULT_ENDPOINT = "https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription";
    private static final String DEFAULT_MODEL = "fun-asr";
    private static final int DEFAULT_TIMEOUT_MS = 120_000;
    private static final int DEFAULT_POLL_INTERVAL_MS = 2_000;

    private final SkillRegistry skillRegistry;
    private final LlmProfileService llmProfileService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public SkillResult execute(SkillContext context) {
        Map<String, Object> input = context.getInput() == null ? Map.of() : context.getInput();
        String callId = stringValue(input.get("callId"));
        String providedTranscript = stringValue(input.get("transcript"));
        log.info("ASR execute start, policyId: {}, skillId: {}, callId: {}, hasTranscriptInput: {}",
                context.getPolicyId(), context.getSkillId(), callId, !providedTranscript.isBlank());
        if (!providedTranscript.isBlank()) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("transcript", providedTranscript);
            output.put("language", "zh-CN");
            output.put("provider", "input");
            log.info("ASR skipped remote call because transcript is provided, skillId: {}, callId: {}", context.getSkillId(), callId);
            return SkillResult.success(output);
        }

        String videoUrl = firstNonBlank(stringValue(input.get("videoUrl")), stringValue(input.get("video_url")));
        if (videoUrl.isBlank()) {
            log.warn("ASR missing videoUrl, skillId: {}, callId: {}", context.getSkillId(), callId);
            return SkillResult.failed("ASR 执行失败：缺少 videoUrl");
        }

        try {
            SkillDefinition definition = skillRegistry.get(context.getSkillId());
            Map<String, Object> executionConfig = definition.getExecutionConfig() == null ? Map.of() : definition.getExecutionConfig();
            Map<String, Object> scriptConfig = definition.getScriptConfig() == null ? Map.of() : definition.getScriptConfig();
            String endpoint = firstNonBlank(
                    stringValue(executionConfig.get("asr_endpoint")),
                    stringValue(scriptConfig.get("asrEndpoint")),
                    ""
            );
            String model = firstNonBlank(
                    stringValue(executionConfig.get("asr_model")),
                    stringValue(scriptConfig.get("asrModel")),
                    ""
            );
            Integer timeoutMs = firstPositiveIntOrNull(
                    toInt(executionConfig.get("asr_timeout_ms")),
                    toInt(scriptConfig.get("asrTimeoutMs"))
            );
            int pollIntervalMs = firstPositiveInt(
                    toInt(executionConfig.get("asr_poll_interval_ms")),
                    toInt(scriptConfig.get("asrPollIntervalMs")),
                    DEFAULT_POLL_INTERVAL_MS
            );
            String asrConfigCode = firstNonBlank(
                    stringValue(executionConfig.get("asr_config_code")),
                    stringValue(scriptConfig.get("asrConfigCode"))
            );
            if (!asrConfigCode.isBlank()) {
                var profile = llmProfileService.findByCode(asrConfigCode).orElse(null);
                if (profile != null) {
                    endpoint = firstNonBlank(endpoint, profile.endpoint());
                    model = firstNonBlank(model, profile.model());
                    timeoutMs = firstPositiveIntOrNull(timeoutMs, profile.timeoutMs());
                }
            }
            endpoint = firstNonBlank(endpoint, DEFAULT_ENDPOINT);
            model = firstNonBlank(model, DEFAULT_MODEL);
            timeoutMs = firstPositiveInt(timeoutMs, null, DEFAULT_TIMEOUT_MS);
            String apiKey = resolveApiKey(executionConfig, scriptConfig, asrConfigCode);
            log.info("ASR config resolved, skillId: {}, callId: {}, configCode: {}, endpoint: {}, model: {}, timeoutMs: {}, pollIntervalMs: {}, apiKeyConfigured: {}",
                    context.getSkillId(), callId, asrConfigCode, endpoint, model, timeoutMs, pollIntervalMs, !apiKey.isBlank());
            if (apiKey.isBlank()) {
                return SkillResult.failed("ASR 执行失败：未配置百炼 API Key（executionConfig.asr_api_key / 环境变量 DASHSCOPE_API_KEY）");
            }

            String taskId = submitTask(endpoint, model, videoUrl, apiKey);
            Map<String, Object> finalPayload = pollTask(endpoint, taskId, apiKey, timeoutMs, pollIntervalMs);
            Map<String, Object> output = buildAsrOutput(finalPayload, taskId, model);
            log.info("ASR execute finished, skillId: {}, callId: {}, taskId: {}, transcriptLen: {}, source: {}",
                    context.getSkillId(), callId, taskId, stringValue(output.get("transcript")).length(), output.get("transcriptSource"));
            return SkillResult.success(output);
        } catch (Exception e) {
            log.error("ASR Skill execute failed, skillId: {}, input: {}", context.getSkillId(), input, e);
            return SkillResult.failed("ASR 执行失败：" + e.getMessage());
        }
    }

    private String submitTask(String endpoint, String model, String videoUrl, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("X-DashScope-Async", "enable");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", Map.of("file_urls", List.of(videoUrl)));
        log.info("ASR submit request, endpoint: {}, model: {}, videoUrlHost: {}", endpoint, model, extractHost(videoUrl));

        ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(payload, headers), Map.class);
        Map<String, Object> body = response.getBody() == null ? Map.of() : response.getBody();
        Map<String, Object> output = asMap(body.get("output"));
        String taskId = firstNonBlank(stringValue(output.get("task_id")), stringValue(body.get("task_id")));
        if (taskId.isBlank()) {
            throw new IllegalStateException("未获取到 task_id，response=" + toJson(body));
        }
        log.info("ASR task submitted, endpoint: {}, model: {}, taskId: {}, statusCode: {}", endpoint, model, taskId, response.getStatusCode().value());
        return taskId;
    }

    private Map<String, Object> pollTask(String endpoint, String taskId, String apiKey, int timeoutMs, int pollIntervalMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String queryUrl = buildTaskQueryUrl(endpoint, taskId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        int attempt = 0;
        while (System.currentTimeMillis() < deadline) {
            attempt++;
            ResponseEntity<Map> response = restTemplate.exchange(
                    queryUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            Map<String, Object> body = response.getBody() == null ? Map.of() : response.getBody();
            Map<String, Object> output = asMap(body.get("output"));
            String status = stringValue(output.get("task_status")).toUpperCase();
            log.info("ASR poll status, taskId: {}, attempt: {}, http: {}, taskStatus: {}", taskId, attempt, response.getStatusCode().value(), status);
            if ("SUCCEEDED".equals(status)) {
                log.info("ASR task succeeded, taskId: {}", taskId);
                return body;
            }
            if ("FAILED".equals(status) || "CANCELED".equals(status)) {
                throw new IllegalStateException("ASR 任务失败，taskId=" + taskId + ", response=" + toJson(body));
            }
            TimeUnit.MILLISECONDS.sleep(Math.max(500, pollIntervalMs));
        }
        throw new IllegalStateException("ASR 任务超时，taskId=" + taskId + ", timeoutMs=" + timeoutMs);
    }

    private Map<String, Object> buildAsrOutput(Map<String, Object> payload, String taskId, String model) {
        Map<String, Object> output = asMap(payload.get("output"));
        List<Map<String, Object>> results = asListOfMap(output.get("results"));

        List<String> transcriptLines = new ArrayList<>();
        Map<String, List<String>> urlTranscriptCache = new LinkedHashMap<>();
        List<String> fetchErrors = new ArrayList<>();
        boolean hasDirectText = false;
        boolean hasUrlText = false;
        for (Map<String, Object> item : results) {
            String text = firstNonBlank(stringValue(item.get("text")), stringValue(item.get("sentence")));
            if (!text.isBlank()) {
                transcriptLines.add(text);
                hasDirectText = true;
                continue;
            }

            String transcriptionUrl = stringValue(item.get("transcription_url"));
            if (transcriptionUrl.isBlank()) {
                continue;
            }
            List<String> extracted = urlTranscriptCache.computeIfAbsent(transcriptionUrl, url -> fetchTranscriptLinesByUrl(url, fetchErrors));
            if (!extracted.isEmpty()) {
                transcriptLines.addAll(extracted);
                hasUrlText = true;
            }
        }
        String transcript = String.join("\n", deduplicateKeepOrder(transcriptLines));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "bailian-fun-asr");
        result.put("model", model);
        result.put("taskId", taskId);
        result.put("transcript", transcript);
        result.put("rawResults", results);
        result.put("writeToState", Map.of(
                "state.audio.transcript", transcript,
                "state.audio.asrProvider", "bailian-fun-asr",
                "state.audio.asrModel", model,
                "state.audio.asrTaskId", taskId
        ));
        result.put("transcriptSource", hasDirectText ? "results.text" : (hasUrlText ? "transcription_url" : "none"));
        result.put("executorVersion", "asr-transcription-url-v1");
        if (!fetchErrors.isEmpty()) {
            result.put("transcriptionFetchErrors", fetchErrors);
        }
        log.info("ASR build output, taskId: {}, resultsSize: {}, transcriptSource: {}, transcriptLen: {}, fetchErrorCount: {}",
                taskId, results.size(), result.get("transcriptSource"), transcript.length(), fetchErrors.size());
        if (transcript.isBlank()) {
            log.warn("ASR transcript is empty, taskId: {}, resultsSize: {}", taskId, results.size());
        }
        return result;
    }

    private List<String> fetchTranscriptLinesByUrl(String transcriptionUrl, List<String> fetchErrors) {
        URI uri = null;
        try {
            uri = URI.create(transcriptionUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    String.class
            );
            String body = response.getBody() == null ? "" : response.getBody().trim();
            log.info("ASR transcription_url fetched, host: {}, http: {}, bodyLen: {}",
                    extractHost(transcriptionUrl), response.getStatusCode().value(), body.length());
            if (body.isBlank()) {
                fetchErrors.add("empty body: " + transcriptionUrl);
                log.warn("ASR transcription_url returned empty body, url: {}", transcriptionUrl);
                return List.of();
            }
            List<String> lines = extractTranscriptLinesFromBody(body);
            if (lines.isEmpty()) {
                fetchErrors.add("parsed no lines: " + transcriptionUrl);
                log.warn("ASR transcription_url parsed but no transcript lines, url: {}", transcriptionUrl);
            } else {
                log.info("ASR transcription_url parsed, url: {}, lines: {}", transcriptionUrl, lines.size());
            }
            return lines;
        } catch (Exception e) {
            log.warn("ASR transcription_url request uri, raw: {}, effectiveUri: {}", transcriptionUrl, uri);
            fetchErrors.add("fetch failed: " + transcriptionUrl + " | " + e.getClass().getSimpleName() + ": " + e.getMessage());
            log.warn("ASR transcription_url fetch failed, url: {}", transcriptionUrl, e);
            return List.of();
        }
    }

    private List<String> extractTranscriptLinesFromBody(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            List<String> lines = new ArrayList<>();
            collectTranscriptLines(root, lines);
            return deduplicateKeepOrder(lines);
        } catch (Exception e) {
            String text = body.trim();
            if (text.isBlank()) {
                return List.of();
            }
            return List.of(text);
        }
    }

    private String extractHost(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? "-" : uri.getHost();
        } catch (Exception ignored) {
            return "-";
        }
    }

    private void collectTranscriptLines(JsonNode node, List<String> collector) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            appendTextField(node, "text", collector);
            appendTextField(node, "sentence", collector);
            appendTextField(node, "transcript", collector);

            collectArrayField(node, "sentences", collector);
            collectArrayField(node, "segments", collector);
            collectArrayField(node, "utterances", collector);
            collectArrayField(node, "paragraphs", collector);
            collectArrayField(node, "results", collector);

            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if ("text".equals(key) || "sentence".equals(key) || "transcript".equals(key)
                        || "sentences".equals(key) || "segments".equals(key)
                        || "utterances".equals(key) || "paragraphs".equals(key)
                        || "results".equals(key)) {
                    return;
                }
                collectTranscriptLines(entry.getValue(), collector);
            });
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectTranscriptLines(item, collector);
            }
            return;
        }
        if (node.isTextual()) {
            String text = node.asText("").trim();
            if (!text.isBlank()) {
                collector.add(text);
            }
        }
    }

    private void collectArrayField(JsonNode parent, String field, List<String> collector) {
        JsonNode arrayNode = parent.get(field);
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                collectTranscriptLines(item, collector);
            }
        }
    }

    private void appendTextField(JsonNode parent, String field, List<String> collector) {
        JsonNode value = parent.get(field);
        if (value != null && value.isTextual()) {
            String text = value.asText("").trim();
            if (!text.isBlank()) {
                collector.add(text);
            }
        }
    }

    private List<String> deduplicateKeepOrder(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String line : lines) {
            String text = line == null ? "" : line.trim();
            if (text.isBlank()) continue;
            if (seen.add(text)) {
                normalized.add(text);
            }
        }
        return normalized;
    }

    private String buildTaskQueryUrl(String endpoint, String taskId) {
        URI uri = URI.create(endpoint);
        String base = uri.getScheme() + "://" + uri.getHost();
        if (uri.getPort() > 0) {
            base += ":" + uri.getPort();
        }
        return base + "/api/v1/tasks/" + taskId;
    }

    private String resolveApiKey(Map<String, Object> executionConfig, Map<String, Object> scriptConfig, String asrConfigCode) {
        String fromConfig = firstNonBlank(
                stringValue(executionConfig.get("asr_api_key")),
                stringValue(scriptConfig.get("asrApiKey"))
        );
        if (!fromConfig.isBlank()) {
            return fromConfig;
        }
        if (asrConfigCode != null && !asrConfigCode.isBlank()) {
            var profile = llmProfileService.findByCode(asrConfigCode).orElse(null);
            if (profile != null && profile.apiKey() != null && !profile.apiKey().isBlank()) {
                return profile.apiKey().trim();
            }
        }
        String fromEnv = firstNonBlank(
                System.getenv("DASHSCOPE_API_KEY"),
                System.getenv("BAILIAN_API_KEY")
        );
        return fromEnv == null ? "" : fromEnv.trim();
    }

    private int firstPositiveInt(Integer a, Integer b, int fallback) {
        if (a != null && a > 0) return a;
        if (b != null && b > 0) return b;
        return fallback;
    }

    private Integer firstPositiveIntOrNull(Integer a, Integer b) {
        if (a != null && a > 0) return a;
        if (b != null && b > 0) return b;
        return null;
    }

    private Integer toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = stringValue(value);
        if (text.isBlank()) return null;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return converted;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(this::asMap)
                .toList();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return new String(String.valueOf(value).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        }
    }
}
