    @Override
    public PromptTemplate resolve(String scene, String locale) {
        String safeScene = StringUtils.defaultIfBlank(scene, parserProperties.getDefaultScene());
        String safeLocale = StringUtils.defaultIfBlank(locale, parserProperties.getDefaultLocale());
        String exactKey = buildKey(safeScene, safeLocale);
        PromptTemplate template = templateMap.get(exactKey);
        if (template != null) {
            return template;
        }
        // 找不到精确模板时，回退到 scene:defaultLocale，再回退到 defaultPrompt。
        String fallbackKey = buildKey(safeScene, parserProperties.getDefaultLocale());
        return templateMap.getOrDefault(fallbackKey,
                PromptTemplate.builder()
                        .key(fallbackKey)
                        .version(1)
                        .content(PromptTemplates.VIDEO_CALL_ANALYSIS_PROMPT)
                        .build());
    }

    package com.midust.parser.prompt;

public final class PromptTemplates {

    private PromptTemplates() {
    }

    public static final String VIDEO_CALL_ANALYSIS_PROMPT = String.join("\n",
            "# 视频通话健康分违规识别指令",
            "",
            "## 任务说明",
            "你是视频通话内容分析系统。请分析视频中**主播（女方）**的违规行为，输出严格 JSON 用于健康分扣减。**只分析主播，连麦用户（男方）行为不计入。**",
            "",
            "## 核心原则",
            "1. 只分析主播（女方）",
            "2. 所有违规必须提供**具体时间证据（秒）**",
            "3. 输出严格 JSON，可直接解析",
            "4. 对 13 种违规类型逐一分析",
            "5. 使用 ASR 或音频时长统计来判断说话情况",
            "6. 主播可能在左侧或右侧画面，请根据画面内容识别",
            "7. **角色判断**：视频分为左右两部分，若一边出现男性，则另一边为女性（主播）",
            "8. **黑屏判定**：只有当主播所在侧画面全黑时，才判定为 BLACK_SCREEN",
            "",
            "## 13 种违规类型及判定规则（严格版）",
            "",
            "### 1. ENVIRONMENT_MESSY",
            "- 背景杂乱：垃圾≥3 件或杂乱面积>30%",
            "- 证据：\"start_sec 背景可见衣物堆放\"",
            "",
            "### 2. NOISY",
            "- **判定标准（满足任一即可）**：",
            "  1. 背景噪音持续≥30 秒（车辆声、喊叫声、装修声、机器声、人群喧哗）",
            "  2. ASR 识别失败率>20%（噪音导致语音识别中断）",
            "  3. **背景频繁切换**：视频中出现≥3 处**全新背景**（非同一场景的不同角度）",
            "- **排除情况**：",
            "  - 坐姿固定，仅切换手机拍摄角度≠背景切换",
            "  - 同一房间内的视角变化≠背景切换",
            "  - 轻微空调声、风扇声、键盘声不算噪音",
            "- **背景判定**：全新背景指场景发生明显变化（如室内→室外→车内→街道等）",
            "- **影响评估**：背景切换会对通话对方产生不良体验（视觉干扰、注意力分散）",
            "- **置信度要求**：噪音/背景切换必须清晰可识别",
            "- 证据：\"start_sec-end_sec 持续 XX 噪音/背景切换 X 次（场景 A→B→C）\"",
            "",
            "### 3. SEXUAL_ACTION",
            "- **判定标准**：**露点**（乳头、生殖器、肛门暴露，必须清晰可见）",
            "- **排除情况**：乳沟、腰部、大腿暴露不算露点",
            "- **重要**：不露点不算 SEXUAL_ACTION",
            "- 证据：\"start_sec 可见 XX 部位暴露，持续 X 秒\"",
            "",
            "### 4. PUBLIC_PLACE",
            "- 背景出现陌生人≥2 人，或可见商场/餐厅/街道",
            "- 证据：\"start_sec 背景可见服务员或街道\"",
            "",
            "### 5. OTHER_PLATFORM_OR_OFFLINE_JOB",
            "- 展示其他平台界面，或提及\"上班/工作\"≥2 次",
            "- 证据：\"start_sec 提及工作或可见其他平台界面\"",
            "",
            "### 6. MULTI_PERSON_CONTEXT",
            "- 画面出现多人≥2 人或背景有人走动",
            "- 证据：\"start_sec 右侧出现另一人，停留 X 秒\"",
            "",
            "### 7. WATCH_TV_OR_PLAY_PHONE",
            "- 视线偏离≥60 秒，或可见手机/电视屏幕",
            "- 证据：\"start_sec-end_sec 主播低头看手机或看电视\"",
            "",
            "### 8. CALL_IN_BED",
            "- **必须同时满足：姿态 + 床特征 + 持续时间 三个条件**",
            "",
            "**STEP 1：姿态判定（必须先判断，这是最关键的一步）**",
            "",
            "主播必须为躺姿（lying posture），满足以下至少 2 个特征：",
            "  - 身体轴线基本水平",
            "  - 背部贴床或床垫",
            "  - 头部接触枕头或床面",
            "  - 肩膀高度接近床面高度",
            "  - 上半身倾斜角度 < 30°",
            "  - 腿部伸展（躺姿时腿部通常伸展，非弓着/弯曲）",
            "",
            "**⚠️ 身体部分可见时的判断原则**：",
            "当主播只露出头部和部分身体（如只有上半身、只有头部等）时：",
            "  - **无法确认完整姿态 → 必须判定 NOT_CALL_IN_BED**",
            "  - 可通过辅助信息推测，但**不能作为判定依据**：",
            "    - 拍摄角度（俯拍/仰拍/平视）",
            "    - 主播眼神方向（看向镜头/看向侧面）",
            "    - 背景特征（床品/家具等）",
            "  - **关键原则**：姿态无法确认 = 不判定违规",
            "",
            "**坐姿的典型特征（出现以下任意情况直接判定 NOT_CALL_IN_BED）**：",
            "  - 上半身竖直或接近竖直",
            "  - 腿弓着/膝盖弯曲明显",
            "  - 臀部接触座椅/床面，上半身直立",
            "  - 靠在椅子/沙发/墙上",
            "  - 盘腿或弯腰",
            "  - 上半身倾斜角度 > 30°",
            "  - 坐在床上或床边",
            "  - 半躺但明显在坐姿状态",
            "",
            "**⚠️ 重要警告**：",
            "  - 背景有床单/被子/枕头 ≠ 主播躺在床上",
            "  - 必须根据主播身体姿态判断，而非背景物品",
            "  - 如果主播上半身竖直或腿弓着，即使背景全是床品，也必须判定 NOT_CALL_IN_BED",
            "  - 如果无法看清主播完整姿态，即使背景有床品，也必须判定 NOT_CALL_IN_BED",
            "",
            "**STEP 2：床的视觉证据（必须满足）**",
            "画面中必须持续可见至少一个床特征：枕头、被子、床单、床头板",
            "床特征必须稳定存在，而非短暂出现",
            "",
            "**⚠️ 床特征的识别标准（严格）**：",
            "  - **枕头**：必须有明确的枕头形状（长方形/方形轮廓），不能仅凭颜色判断",
            "  - **被子**：必须有明显的褶皱/厚度/覆盖感，不能仅凭颜色/图案判断",
            "  - **床单**：必须有平整铺展的床面特征，能看到床的边缘或褶皱",
            "  - **床头板**：必须有明显的床头板结构",
            "  - **禁止推断**：紫色/白色/粉色背景 ≠ 枕头/被子，必须有明确形状特征",
            "  - **关键原则**：颜色/图案不能作为床特征的依据，必须有明确的物体形态",
            "",
            "**STEP 3：持续时间**",
            "躺姿 + 床特征 持续超过视频总时长 50%",
            "",
            "**严格排除情况（任何出现均不算 CALL_IN_BED）**：",
            "  - 主播坐在床上聊天",
            "  - 主播靠在床头坐着",
            "  - 主播盘腿坐在床上",
            "  - 主播坐在椅子上，背景有床",
            "  - 主播腿弓着/膝盖弯曲明显",
            "  - 主播上半身竖直或接近竖直",
            "  - 主播只露出头部/部分身体，无法确认完整姿态",
            "  - **仅凭颜色/图案判断床特征（如紫色背景≠枕头）**",
            "  - 床只短暂出现在背景",
            "  - 画面频繁切换",
            "  - 只看到床单/被子但看不到主播姿态",
            "  - 主播姿态无法确认",
            "",
            "**判断核心**：明确躺姿 + 可见床特征 + 持续 > 50% 时长，三者缺一不可",
            "**关键规则**：姿态无法确认 = NOT_CALL_IN_BED",
            "- 证据：\"start_sec-end_sec 可见枕头（长方形轮廓）/被子（褶皱厚度），主播躺姿（身体轴线水平/背部贴床/腿部伸展），占比 X%\"",
            "",
            "### 9. SILENT_ALL_TIME",
            "- 主播语音时长<10% 视频总时长，或说话次数<3 次",
            "- 证据：\"start_sec-end_sec ASR 统计语音 X 秒，占比 Y%\"",
            "",
            "### 10. BLACK_SCREEN",
            "- **判定标准**：主播所在侧画面完全黑屏/全黑，无任何可见内容（包括背景、物体等）",
            "- **持续时间要求**：黑屏必须持续 > 5 秒才算违规",
            "- **排除情况**：",
            "  - 通话结束/挂断时的短暂黑屏（通常在视频末尾几秒）是正常现象，不算违规",
            "  - 黑屏出现在视频最后 5 秒内，且之前主播正常出镜 → 不判定为 BLACK_SCREEN",
            "  - 短暂黑屏 < 5 秒 → 不判定为 BLACK_SCREEN",
            "- **角色判断**：",
            "  - 若男性出现在画面中 → 另一侧为女性（主播），只分析主播侧是否黑屏",
            "  - 若男性侧黑屏但主播侧有内容 → 不判定为 BLACK_SCREEN",
            "  - 若两侧都黑屏 → 判定主播为 BLACK_SCREEN",
            "- **证据**：\"start_sec-end_sec 主播侧画面全黑，无任何可见内容，持续 X 秒\"",
            "",
            "### 11. NO_ONE_ON_CAMERA",
            "- **判定标准**：主播侧画面有背景内容（如房间、墙壁、家具等），但主播（女方）未出镜",
            "- **排除情况**：",
            "  - 完全黑屏不属于 NO_ONE_ON_CAMERA，应归类为 BLACK_SCREEN",
            "  - 男性侧无人出镜不计入违规（只分析主播）",
            "- **证据**：\"start_sec-end_sec 主播侧可见 XX 背景，但无主播出镜\"",
            "",
            "### 12. SLEEPING",
            "- **判定标准**：主播闭眼 + 无明显动作/互动",
            "- **持续时间要求**：",
            "  - 视频时长 ≥ 60 秒：闭眼持续 > 60 秒",
            "  - 视频时长 < 60 秒：闭眼占视频时长 > 80%",
            "- **辅助判断特征**：",
            "  - 主播躺姿或靠姿",
            "  - 几乎不说话（SILENT_ALL_TIME）",
            "  - 无眼神交流、无表情变化",
            "  - 身体静止、无手势动作",
            "- **排除情况**：",
            "  - 主播闭眼但明显在思考/休息，有互动回应 → 不算",
            "  - 短暂闭眼（如眨眼、闭眼休息几秒）→ 不算",
            "- **证据**：\"start_sec-end_sec 主播闭眼，无明显动作，持续 X 秒/占比 X%\"",
            "",
            "### 13. PLAY_RECORDING",
            "- 音画不同步>5 秒，画面静止>60 秒，可见播放进度条",
            "- 证据：\"start_sec 可见播放进度条\"",
            "",
            "## 误判防范（重要）",
            "1. **CALL_IN_BED**：必须同时满足躺姿+床特征+持续>50%。坐姿（即使背景有床）≠躺在床上，姿态判定优先",
            "2. **NOISY**：必须是**持续的、清晰可识别**的噪音，轻微背景声不算",
            "3. **SILENT_ALL_TIME**：短暂停顿≠全程不说话，要看总语音时长占比",
            "4. **SLEEPING**：闭眼+无动作+无互动。短视频（<60秒）闭眼占比>80%也算，结合躺姿/不说话等特征综合判断",
            "5. **主播 vs 背景**：只分析主播行为，背景物品/声音不计入主播违规",
            "6. **CALL_IN_BED vs NOISY**：背景嘈杂≠躺在床上，两者是独立的违规类型",
            "7. **BLACK_SCREEN**：通话结束时的短暂黑屏是正常现象，黑屏需持续>5秒且非视频末尾才算违规",
            "8. **BLACK_SCREEN vs NO_ONE_ON_CAMERA**：完全黑屏无任何内容=BLACK_SCREEN；有背景但无主播=NO_ONE_ON_CAMERA",
            "9. **男性侧 vs 女性侧**：男性侧黑屏/无人≠主播违规，只分析女性（主播）侧",
            "",
            "## 输出规则（严格 JSON）",
            "1. 仅输出 JSON",
            "2. 所有 key 用双引号",
            "3. 时间用秒数",
            "4. 置信度 0.0-1.0",
            "5. 对每个 detected=true 的违规记录 start_sec、end_sec、evidence",
            "6. **如果违规条件满足，即使画面动作微弱也必须标记**",
            "",
            "## JSON Schema",
            "{",
            "  \"video_duration_sec\": 180,",
            "  \"violations\": [",
            "    {",
            "      \"type\": \"ENVIRONMENT_MESSY|NOISY|SEXUAL_ACTION|PUBLIC_PLACE|",
            "OTHER_PLATFORM_OR_OFFLINE_JOB|MULTI_PERSON_CONTEXT|",
            "WATCH_TV_OR_PLAY_PHONE|CALL_IN_BED|SILENT_ALL_TIME|",
            "NO_ONE_ON_CAMERA|SLEEPING|BLACK_SCREEN|PLAY_RECORDING\",",
            "      \"detected\": true,",
            "      \"confidence\": 0.85,",
            "      \"evidence\": \"具体证据描述，包含时间点\",",
            "      \"start_sec\": 30,",
            "      \"end_sec\": 90",
            "    }",
            "  ],",
            "  \"summary\": {",
            "    \"total_violations\": 2,",
            "    \"high_confidence_count\": 1,",
            "    \"primary_violation\": \"CALL_IN_BED\",",
            "    \"overall_confidence\": 0.85",
            "  }",
            "}",
            "",
            "## 可选字段：health_score_adjustment（建议输出）",
            "{",
            "  \"health_score_adjustment\": {",
            "    \"target_user_role\": \"host\",",
            "    \"is_female_host_target\": true,",
            "    \"events\": [",
            "      {",
            "        \"risk_type\": \"ENVIRONMENT_MESSY\",",
            "        \"type\": \"违规行为\",",
            "        \"score_delta\": -5,",
            "        \"evidence\": \"背景杂乱，地面可见大量衣物和垃圾\",",
            "        \"confidence\": 0.82,",
            "        \"start_sec\": 12,",
            "        \"end_sec\": 38",
            "      }",
            "    ],",
            "    \"total_score_delta\": -5,",
            "    \"confidence\": 0.82,",
            "    \"reason\": \"检测到主播环境杂乱，按规则扣分\"",
            "  }",
            "}",
            "",
            "risk_type 必须使用以下枚举名之一：",
            "ENVIRONMENT_MESSY, NOISY, SEXUAL_ACTION, PUBLIC_PLACE,",
            "OTHER_PLATFORM_OR_OFFLINE_JOB, MULTI_PERSON_CONTEXT,",
            "WATCH_TV_OR_PLAY_PHONE, CALL_IN_BED, SILENT_ALL_TIME,",
            "BLACK_SCREEN, NO_ONE_ON_CAMERA, SLEEPING, PLAY_RECORDING",
            "",
            "## 示例 1：检测到违规",
            "{",
            "  \"video_duration_sec\": 64,",
            "  \"violations\": [",
            "    {",
            "      \"type\": \"SILENT_ALL_TIME\",",
            "      \"detected\": true,",
            "      \"confidence\": 0.95,",
            "      \"evidence\": \"全程 64 秒，主播说话仅 3 秒，占比 4.7%\",",
            "      \"start_sec\": 0,",
            "      \"end_sec\": 64",
            "    }",
            "  ],",
            "  \"summary\": {",
            "    \"total_violations\": 1,",
            "    \"high_confidence_count\": 1,",
            "    \"primary_violation\": \"SILENT_ALL_TIME\",",
            "    \"overall_confidence\": 0.95",
            "  }",
            "}",
            "",
            "## 示例 2：未检测到违规",
            "{",
            "  \"video_duration_sec\": 64,",
            "  \"violations\": [],",
            "  \"summary\": {",
            "    \"total_violations\": 0,",
            "    \"high_confidence_count\": 0,",
            "    \"primary_violation\": null,",
            "    \"overall_confidence\": 1.0",
            "  }",
            "}",
            "",
            "## 分析流程",
            "1. 观看完整视频",
            "2. **判断角色位置**：识别男性/女性分别在左侧还是右侧",
            "3. **分析背景连续性**：观察背景是否稳定一致，场景切换频率",
            "4. 逐一比对 13 种违规类型（只分析女性/主播侧）",
            "5. 使用 ASR 或音频统计判断说话时长",
            "6. 为每个 detected=true 的违规记录时间戳和证据",
            "7. 输出严格 JSON",
            "",
            "## 角色判断规则（重要）",
            "| 场景 | 判定 | 是否分析违规 |",
            "|------|------|-------------|",
            "| 左侧男性 + 右侧女性 | 右侧为女主播 | 只分析右侧 |",
            "| 左侧女性 + 右侧男性 | 左侧为女主播 | 只分析左侧 |",
            "| 仅一侧有人（男性） | 另一侧为女主播（可能黑屏/无人） | 分析另一侧 |",
            "| 仅一侧有人（女性） | 该侧为女主播 | 只分析该侧 |",
            "| 两侧都黑屏 | 女主播侧黑屏 | 判定 BLACK_SCREEN |",
            "| 两侧都无人 | 无法判断，默认不分析 | 不判定违规 |",
            "",
            "**重要**：男性侧黑屏/无人出镜≠主播违规，必须确认是女性（主播）侧才计入违规",
            "",
            "## 背景连续性分析（CALL_IN_BED 判定关键）",
            "| 背景特征 | 在床上 | 背景嘈杂/其他 |",
            "|----------|--------|---------------|",
            "| 背景稳定性 | 稳定一致，场景固定 | 可能频繁切换/变化 |",
            "| 床的特征 | 枕头/被子持续可见 | 看不到床的特征 |",
            "| 主播姿态 | 头部枕在枕头上 | 可能靠在椅子上 |",
            "| 场景切换 | 有限（床上视角） | 可能多处切换 |",
            "",
            "**重要**：如果背景频繁切换/变化，即使主播姿态像躺着，也不能判定为 CALL_IN_BED",
            ""
    );
}

package com.midust.parser.llm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.midust.parser.ParserProperties;
import com.midust.parser.model.LlmParseRequest;
import com.midust.parser.model.LlmParseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 HTTP 的大模型客户端实现（使用 BytePlus SDK 模式）。
 * 当配置了 api-key 时自动启用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpLlmClient implements LlmClient {

    private final ParserProperties parserProperties;

    @Override
    public LlmParseResponse parseVideo(LlmParseRequest request) {
        ParserProperties.Llm llm = parserProperties.getLlm();
        if (llm == null || StringUtils.isBlank(llm.getApiKey())) {
            return LlmParseResponse.builder().success(false).content("llm api key is blank").raw("").build();
        }

        // 使用 chat/completions 端点，支持 video_url 输入
        String endpoint = "https://ark.ap-southeast.bytepluses.com/api/v3/chat/completions";
        
        // 构建请求体（参考 curl 命令格式）
        Map<String, Object> payload = buildChatCompletionPayload(request);
        
        String raw;
        try {
            raw = httpPostWithTimeout(endpoint, payload, llm.getApiKey(), 120000);
        } catch (Exception e) {
            log.error("byteplus chat completions request failed, callId:{}", request.getCallId(), e);
            return LlmParseResponse.builder()
                    .success(false)
                    .content("byteplus request failed: " + e.getMessage())
                    .raw(JSON.toJSONString(payload))
                    .build();
        }

        String content = extractChatCompletionContent(raw);
        boolean success = StringUtils.isNotBlank(content);
        log.info("byteplus llm parse done, callId:{}, success:{}, contentPreview:{}", request.getCallId(), success,
                success ? StringUtils.abbreviate(content, 200) : "");
        return LlmParseResponse.builder()
                .success(success)
                .content(content)
                .raw(StringUtils.defaultString(raw, JSON.toJSONString(payload)))
                .build();
    }

    /**
     * 构建 Chat Completions API 的请求体（支持 video_url）
     * 参考 curl 格式：
     * {
     *   "model": "seed-1-6-250915",
     *   "messages": [{
     *     "role": "user",
     *     "content": [
     *       {"type": "video_url", "video_url": {"url": "...", "fps": 5}},
     *       {"type": "text", "text": "..."}
     *     ]
     *   }],
     *   "max_tokens": 4096
     * }
     */
    private Map<String, Object> buildChatCompletionPayload(LlmParseRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", request.getModel());
        payload.put("messages", Collections.singletonList(buildUserMessage(request)));
        payload.put("max_tokens", 8192);  // 增加 max_tokens，防止 JSON 被截断
        return payload;
    }

    private Map<String, Object> buildUserMessage(LlmParseRequest request) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        List<Map<String, Object>> contentList = new ArrayList<>();

        // 1. 添加视频 URL（type: video_url）
        if (StringUtils.isNotBlank(request.getVideoUrl())) {
            Map<String, Object> videoPart = new HashMap<>();
            videoPart.put("type", "video_url");
            Map<String, Object> videoUrlObj = new HashMap<>();
            videoUrlObj.put("url", request.getVideoUrl());
            videoUrlObj.put("fps", 5); // 每秒 5 帧
            videoPart.put("video_url", videoUrlObj);
            contentList.add(videoPart);
        }

        // 2. 添加提示词文本
        if (StringUtils.isNotBlank(request.getPrompt())) {
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", request.getPrompt());
            contentList.add(textPart);
        }

        message.put("content", contentList);
        return message;
    }

    /**
     * 发送 HTTP POST 请求，带超时配置
     */
    private String httpPostWithTimeout(String url, Map<String, Object> payload, String apiKey, int timeout) throws Exception {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            if (StringUtils.isNotBlank(apiKey)) {
                httpPost.setHeader("Authorization", "Bearer " + apiKey);
            }

            String jsonBody = JSON.toJSONString(payload);
            httpPost.setEntity(new StringEntity(jsonBody, "UTF-8"));

            log.debug("HTTP POST to: {}, body: {}", url, StringUtils.abbreviate(jsonBody, 500));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    String responseBody = EntityUtils.toString(entity, "UTF-8");
                    log.debug("HTTP Response: status={}, body={}", statusCode, StringUtils.abbreviate(responseBody, 500));

                    if (statusCode == 200) {
                        return responseBody;
                    } else {
                        throw new Exception("HTTP request failed with status: " + statusCode + ", body: " + responseBody);
                    }
                } else {
                    throw new Exception("HTTP request failed with no response entity, status: " + statusCode);
                }
            }
        }
    }

    /**
     * 从 Chat Completions API 响应中提取内容
     */
    private String extractChatCompletionContent(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        try {
            JSONObject root = JSON.parseObject(raw);
            JSONArray choices = root.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return "";
            }

            for (int i = 0; i < choices.size(); i++) {
                JSONObject choice = choices.getJSONObject(i);
                if (choice == null) {
                    continue;
                }

                JSONObject message = choice.getJSONObject("message");
                if (message == null) {
                    continue;
                }

                String content = message.getString("content");
                if (StringUtils.isNotBlank(content)) {
                    return content;
                }
            }
        } catch (Exception e) {
            log.warn("parse byteplus chat completions response failed, raw:{}", raw, e);
        }
        return "";
    }

    /**
     * 从 Responses API 响应中提取内容（旧版本，保留兼容）
     */
    @Deprecated
    private String extractContent(String raw) {
        return extractChatCompletionContent(raw);
    }
}

