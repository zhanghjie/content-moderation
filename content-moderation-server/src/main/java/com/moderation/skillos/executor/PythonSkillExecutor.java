package com.moderation.skillos.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moderation.skillos.model.SkillContext;
import com.moderation.skillos.model.SkillDefinition;
import com.moderation.skillos.model.SkillResult;
import com.moderation.skillos.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component("pythonSkillExecutor")
@RequiredArgsConstructor
public class PythonSkillExecutor implements SkillExecutor {
    private static final String RESULT_MARKER = "__SKILL_RESULT__=";
    private static final int DEFAULT_TIMEOUT_MS = 3000;

    private final SkillRegistry skillRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public SkillResult execute(SkillContext context) {
        SkillDefinition definition = skillRegistry.get(context.getSkillId());
        Map<String, Object> scriptConfig = definition.getScriptConfig() == null ? Map.of() : definition.getScriptConfig();
        Map<String, Object> executionConfig = definition.getExecutionConfig() == null ? Map.of() : definition.getExecutionConfig();
        String scriptContent = stringValue(scriptConfig.get("content"));
        if (scriptContent.isBlank()) {
            return SkillResult.failed("脚本内容不能为空");
        }

        int timeoutMs = resolveTimeoutMs(definition, executionConfig, scriptConfig);
        String entry = stringValue(scriptConfig.get("entry"));
        if (entry.isBlank()) {
            entry = "main";
        }
        String fileName = sanitizeFileName(stringValue(scriptConfig.get("fileName")));
        if (fileName.isBlank()) {
            fileName = definition.getSkillId() + ".py";
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("python-skill-");
            Path scriptFile = tempDir.resolve(fileName);
            if (!fileName.endsWith(".py")) {
                scriptFile = tempDir.resolve(fileName + ".py");
            }
            Files.writeString(scriptFile, scriptContent, StandardCharsets.UTF_8);

            Map<String, Object> executionContext = buildExecutionContext(context, definition, scriptConfig, executionConfig);
            Path contextFile = tempDir.resolve("context.json");
            Files.writeString(contextFile, objectMapper.writeValueAsString(executionContext), StandardCharsets.UTF_8);

            Path runnerFile = tempDir.resolve("runner.py");
            Files.writeString(runnerFile, buildRunnerScript(), StandardCharsets.UTF_8);

            ProcessBuilder processBuilder = new ProcessBuilder("python3", runnerFile.toAbsolutePath().toString());
            processBuilder.directory(tempDir.toFile());
            processBuilder.redirectErrorStream(true);
            Map<String, String> env = processBuilder.environment();
            env.put("SKILL_SCRIPT_FILE", scriptFile.toAbsolutePath().toString());
            env.put("SKILL_CONTEXT_FILE", contextFile.toAbsolutePath().toString());
            env.put("SKILL_SCRIPT_ENTRY", entry);
            env.put("PYTHONUNBUFFERED", "1");
            appendCustomEnv(env, scriptConfig.get("env"));

            Process process = processBuilder.start();
            if (isPerceptionSkill(definition)) {
                process.waitFor();
            } else {
                boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return SkillResult.failed("Python 脚本执行超时: " + timeoutMs + "ms");
                }
            }

            String outputText = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return SkillResult.failed(buildFailureMessage(exitCode, outputText));
            }

            Map<String, Object> resultMap = parseResult(outputText);
            if (resultMap == null) {
                return SkillResult.failed("Python 脚本未返回可解析的 JSON 结果: " + outputText);
            }

            Map<String, Object> outputData = new LinkedHashMap<>(resultMap);
            if (!outputData.containsKey("asrResult")) {
                outputData.put("asrResult", new LinkedHashMap<>(resultMap));
            }

            boolean success = asBoolean(outputData.get("success"), true);
            String message = stringValue(outputData.get("message"));
            if (!success && message.isBlank()) {
                message = "Python 脚本执行失败";
            }
            return SkillResult.builder()
                    .success(success)
                    .message(message.isBlank() ? null : message)
                    .output(outputData)
                    .build();
        } catch (Exception e) {
            return SkillResult.failed("Python 脚本执行失败: " + e.getMessage());
        } finally {
            cleanup(tempDir);
        }
    }

    private Map<String, Object> buildExecutionContext(
            SkillContext context,
            SkillDefinition definition,
            Map<String, Object> scriptConfig,
            Map<String, Object> executionConfig
    ) {
        Map<String, Object> executionContext = new LinkedHashMap<>();
        executionContext.put("policyId", context.getPolicyId());
        executionContext.put("skillId", context.getSkillId());
        executionContext.put("input", context.getInput() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(context.getInput()));
        executionContext.put("state", context.getState() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(context.getState()));
        executionContext.put("policyConfig", context.getPolicyConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(context.getPolicyConfig()));
        executionContext.put("scriptConfig", new LinkedHashMap<>(scriptConfig));
        executionContext.put("executionConfig", new LinkedHashMap<>(executionConfig));
        executionContext.put("skill", buildSkillSnapshot(definition));
        return executionContext;
    }

    private Map<String, Object> buildSkillSnapshot(SkillDefinition definition) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("skillId", definition.getSkillId());
        snapshot.put("name", definition.getName());
        snapshot.put("type", definition.getType());
        snapshot.put("description", definition.getDescription());
        snapshot.put("tags", definition.getTags() == null ? List.of() : List.copyOf(definition.getTags()));
        snapshot.put("outputSchema", definition.getOutputSchema() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(definition.getOutputSchema()));
        snapshot.put("stateMapping", definition.getStateMapping() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(definition.getStateMapping()));
        snapshot.put("executionConfig", definition.getExecutionConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(definition.getExecutionConfig()));
        snapshot.put("scriptConfig", definition.getScriptConfig() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(definition.getScriptConfig()));
        snapshot.put("status", definition.getStatus());
        snapshot.put("timeoutMs", definition.getTimeoutMs());
        snapshot.put("version", definition.getVersion());
        return snapshot;
    }

    private String buildRunnerScript() {
        return """
                import json
                import os
                import inspect
                import re
                import types
                import sys
                import tempfile
                import traceback

                RESULT_MARKER = "%s"

                def load_json(path):
                    with open(path, 'r', encoding='utf-8') as fp:
                        return json.load(fp)

                def emit(value):
                    print(RESULT_MARKER + json.dumps(value, ensure_ascii=False, default=str), flush=True)

                def normalize_result(result):
                    if result is None:
                        return {"output": {}}
                    if isinstance(result, dict):
                        return result
                    return {"output": result}

                def normalize_option_name(name):
                    text = str(name or "").strip()
                    if not text:
                        return ""
                    text = text.replace("_", "-")
                    text = re.sub(r"([a-z0-9])([A-Z])", r"\1-\2", text)
                    return text.lower()

                def build_cli_args(context, script_file):
                    script_config = context.get("scriptConfig") or {}
                    input_data = context.get("input") or {}
                    args = [script_file]
                    video_url = None
                    if isinstance(input_data, dict):
                        video_url = input_data.get("videoUrl") or input_data.get("video_url")
                    if video_url:
                        args.extend(["--video-url", str(video_url)])
                    output_file = os.path.join(tempfile.gettempdir(), f"skill-output-{os.getpid()}-{os.path.basename(script_file)}.json")
                    args.extend(["--output", output_file])
                    for key in ("model-size", "language", "device", "compute-type", "beam-size"):
                        value = script_config.get(key)
                        if value is None:
                            value = script_config.get(key.replace("-", "_"))
                        if value is None:
                            continue
                        option = normalize_option_name(key)
                        if not option:
                            continue
                        args.extend([f"--{option}", str(value)])
                    return args, output_file

                def load_output_file(output_file):
                    if not output_file or not os.path.exists(output_file):
                        return None
                    with open(output_file, 'r', encoding='utf-8') as fp:
                        text = fp.read().strip()
                    if not text:
                        return None
                    try:
                        return json.loads(text)
                    except Exception:
                        return {"output": text}

                def configure_ffmpeg_path():
                    try:
                        import imageio_ffmpeg
                        ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
                        if ffmpeg_exe and os.path.exists(ffmpeg_exe):
                            ffmpeg_dir = tempfile.mkdtemp(prefix='ffmpeg-bin-')
                            ffmpeg_link = os.path.join(ffmpeg_dir, 'ffmpeg')
                            if not os.path.exists(ffmpeg_link):
                                try:
                                    os.symlink(ffmpeg_exe, ffmpeg_link)
                                except FileExistsError:
                                    pass
                                except OSError:
                                    with open(ffmpeg_link, 'w', encoding='utf-8') as fp:
                                        fp.write('#!/bin/sh\\nexec "{}" "$@"\\n'.format(ffmpeg_exe.replace('"', '\\"')))
                                    os.chmod(ffmpeg_link, 0o755)
                            current_path = os.environ.get('PATH', '')
                            path_parts = [part for part in current_path.split(os.pathsep) if part]
                            if ffmpeg_dir not in path_parts:
                                os.environ['PATH'] = ffmpeg_dir + (os.pathsep + current_path if current_path else '')
                    except Exception:
                        pass

                def call_entry(func, context):
                    try:
                        signature = inspect.signature(func)
                        params = list(signature.parameters.values())
                        if not params:
                            return func()
                        if len(params) == 1:
                            return func(context)
                        return func(context)
                    except (TypeError, ValueError):
                        try:
                            return func(context)
                        except TypeError:
                            return func()

                def main():
                    context = load_json(os.environ['SKILL_CONTEXT_FILE'])
                    script_file = os.environ['SKILL_SCRIPT_FILE']
                    entry_name = os.environ.get('SKILL_SCRIPT_ENTRY', 'main').strip() or 'main'
                    cli_args, output_file = build_cli_args(context, script_file)
                    sys.argv = cli_args
                    configure_ffmpeg_path()
                    module_name = '__skill__'
                    module = types.ModuleType(module_name)
                    module.__file__ = script_file
                    module.__package__ = ''
                    sys.modules[module_name] = module
                    namespace = module.__dict__
                    namespace['__name__'] = module_name
                    namespace['__file__'] = script_file
                    namespace['__package__'] = ''
                    with open(script_file, 'r', encoding='utf-8') as fp:
                        code = fp.read()
                    exec(compile(code, script_file, 'exec'), namespace, namespace)
                    entry = namespace.get(entry_name)
                    if callable(entry):
                        result = call_entry(entry, context)
                    elif callable(namespace.get('main')):
                        result = call_entry(namespace['main'], context)
                    elif 'result' in namespace:
                        result = namespace['result']
                    elif '__result__' in namespace:
                        result = namespace['__result__']
                    else:
                        result = None
                    if result is None:
                        file_result = load_output_file(output_file)
                        if file_result is not None:
                            result = file_result
                    emit(normalize_result(result))

                if __name__ == '__main__':
                    try:
                        main()
                    except Exception:
                        traceback.print_exc()
                        sys.exit(1)
                """.formatted(RESULT_MARKER);
    }

    private Map<String, Object> parseResult(String outputText) {
        if (outputText == null || outputText.isBlank()) {
            return null;
        }
        String payload = outputText;
        int markerIndex = outputText.lastIndexOf(RESULT_MARKER);
        if (markerIndex >= 0) {
            payload = outputText.substring(markerIndex + RESULT_MARKER.length()).trim();
        }
        if (payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignored) {
            try {
                return objectMapper.readValue(outputText, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private void appendCustomEnv(Map<String, String> env, Object customEnv) {
        if (!(customEnv instanceof Map<?, ?> customMap)) {
            return;
        }
        for (Map.Entry<?, ?> entry : customMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            env.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
    }

    private int resolveTimeoutMs(SkillDefinition definition, Map<String, Object> executionConfig, Map<String, Object> scriptConfig) {
        Integer timeout = toInteger(executionConfig.get("timeout"));
        if (timeout == null || timeout <= 0) {
            timeout = toInteger(scriptConfig.get("timeout"));
        }
        if (timeout == null || timeout <= 0) {
            timeout = definition.getTimeoutMs();
        }
        if (timeout == null || timeout <= 0) {
            timeout = DEFAULT_TIMEOUT_MS;
        }
        return timeout;
    }

    private boolean isPerceptionSkill(SkillDefinition definition) {
        if (definition == null || definition.getType() == null) {
            return false;
        }
        return "PERCEPTION".equalsIgnoreCase(definition.getType().trim());
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text.trim());
        }
        return defaultValue;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String buildFailureMessage(int exitCode, String outputText) {
        String text = outputText == null ? "" : outputText.trim();
        if (text.isBlank()) {
            return "Python 脚本退出，exitCode=" + exitCode;
        }
        return "Python 脚本退出，exitCode=" + exitCode + ", output=" + text;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "skill.py";
        }
        String normalized = fileName.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(slashIndex + 1);
        }
        normalized = normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (normalized.isBlank()) {
            return "skill.py";
        }
        return normalized;
    }

    private void cleanup(Path tempDir) {
        if (tempDir == null) {
            return;
        }
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
