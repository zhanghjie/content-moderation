# DeerFlow 2.0 代码与设计分析

> 分析对象：`bytedance/deer-flow` 2.0
>
> 分析目标：梳理其 README、核心目录、关键类与关键文件的逻辑与设计思想，并评估对当前 `content-moderation` 系统的可借鉴价值，最终形成升级建议。

## 1. 结论先行

DeerFlow 不是一个单纯的“聊天机器人项目”，而是一个**面向复杂任务的 agent harness**：

- **入口层**：Nginx 统一反向代理，把流式对话、网关 API、前端统一到一个服务入口。
- **运行时层**：LangGraph 负责主 agent 执行、状态持久化、流式事件、线程管理。
- **能力层**：技能（skills）、工具（tools）、MCP、沙箱、记忆、子 agent、IM 渠道。
- **交互层**：Next.js 前端 + LangGraph SDK，支持线程列表、流式消息、附件、Todo、状态展示。

对你当前的 `content-moderation` 系统来说，它**最有价值的不是某个具体工具实现，而是下面这些设计思想**：

1. **状态与执行分离**：线程状态、检查点、文件系统、运行结果分别管理。
2. **能力分层**：agent runtime、管理 API、UI、外部渠道解耦。
3. **配置驱动**：模型、工具、沙箱、技能、记忆都通过配置与反射加载。
4. **可插拔中间件链**：把横切能力拆成 middleware，而不是塞进单一 executor。
5. **可观测与可恢复**：流式事件、历史 checkpoint、thread search、artifact 下载、memory reload。

如果你的系统当前仍然是“**单层 planner + executor**”，那 DeerFlow 的设计能提供非常明确的升级方向：

- 把执行链拆成更细的中间件/阶段。
- 给每一步执行建立标准化 state、artifact、checkpoint。
- 把技能/工具从硬编码改成可配置、可热加载的模块。
- 给前端提供更稳定的线程历史、状态回放、结果消费接口。

---

## 2. README 级别的设计思想分析

### 2.1 总 README 的核心信息

`README.md` 传递出的产品定位非常清晰：

- DeerFlow 2.0 是重写版本，不复用 1.x 代码。
- 目标不是“研究助手”本身，而是“super agent harness”。
- 核心卖点是：
  - sub-agents
  - memory
  - sandbox
  - skills
  - long-running tasks
  - IM channels
  - LangSmith tracing

这意味着它的设计重心在**运行平台**，不是单一 prompt 工程。

#### 对当前系统的启发

你当前的内容审核系统，如果只是把“审核策略”当成一个串行规则链，通常会遇到这些问题：

- 每个步骤之间耦合太强。
- 结果结构不统一，前端难消费。
- 失败时缺少恢复/重试点。
- 无法在中途插入诊断、人工确认、分支执行。

DeerFlow 的价值在于：它展示了如何把一个 agent 系统组织成**可扩展执行平台**。

### 2.2 README 中最有参考价值的内容

#### 2.2.1 配置驱动模型

README 里把模型配置写得非常完整：

- `use` 用模块路径反射实例化。
- 支持 OpenAI / Anthropic / DeepSeek / Gemini / CLI-backed provider。
- `supports_thinking`、`supports_vision`、`supports_reasoning_effort` 都是能力声明。
- `base_url`、`output_version`、`when_thinking_enabled.extra_body` 等细节完全由配置决定。

**可借鉴点**：

- 你们也应该把“策略步骤的能力声明”从代码里抽出来。
- 例如：
  - 支持同步/异步
  - 支持人工确认
  - 支持幂等重试
  - 支持状态写入
  - 支持分支执行

#### 2.2.2 Sandbox / skills / memory / channels

README 把这四个能力明确为平台的一部分：

- sandbox：代码执行环境。
- skills：可扩展工作流。
- memory：跨会话记忆。
- channels：外部消息入口。

这比一般应用只做“前端 + API”更像一个**运行时平台**。

#### 2.2.3 安全声明

README 专门强调：

- 不建议直接暴露公网。
- 要做 IP allowlist、认证网关、网络隔离。
- 说明它的设计默认就是高权限执行环境。

对你的系统来说，这点尤其重要：

- 只要存在“审核结果驱动执行”或“自动触发外部动作”，就必须有明确的权限边界。
- 不要把“执行能力”直接暴露给前端或外部接口而无鉴权。

---

## 3. 后端目录与关键文件分析

> 以下分析重点放在真正决定行为的文件；大量技能文档、图片资源属于内容资产，按模式归类。

### 3.1 `backend/app/gateway/app.py`

这是 FastAPI 网关入口。

#### 职责

- 启动时加载全局配置。
- 初始化 LangGraph runtime。
- 启动 IM channel service。
- 注册各类 router：models、mcp、memory、skills、artifacts、uploads、threads、agents、suggestions、channels、runs 等。
- 提供健康检查。

#### 设计思想

- 网关只负责“管理面”和“非 agent 操作”。
- 真正的 agent 请求并不在这个 app 里直接执行，而是交给 LangGraph runtime。
- 通过 lifespan 统一处理启动/关闭逻辑。

#### 对当前系统的启发

你们可以考虑类似的分层：

- **Policy Execution API**：负责提交请求、查看状态、获取结果、取消/重试。
- **Policy Runtime**：负责真正执行 skill / step / plugin。
- **Admin API**：负责配置、版本、审计、能力开关。

这样比把所有逻辑塞到一个 controller/service 里更清晰。

---

### 3.2 `backend/app/gateway/routers/models.py`

#### 职责

- 从配置中列出模型。
- 按名称查模型。
- 不暴露敏感字段，只输出前端需要的 metadata。

#### 设计思想

- 前端消费的是“可展示模型信息”，不是完整 provider config。
- 配置和呈现分离。

#### 可借鉴点

如果你们有多种审核引擎 / 模型 / policy profile，建议也提供类似的“只读模型视图”：

- `id`
- `displayName`
- `description`
- `capabilities`
- `enabled`

而不是直接把底层类名和实现细节暴露给前端。

---

### 3.3 `backend/app/gateway/routers/memory.py`

#### 职责

- 提供 memory data 的读取、重载、清空、导入、导出。
- 提供 fact 的创建、更新、删除。
- 返回统一的 schema。

#### 设计思想

- 把记忆作为独立数据产品，而不是 agent 内部黑盒。
- 支持手工维护和程序写入。
- 对错误做稳定映射。

#### 对当前系统的启发

如果你们未来要保留：

- 审核知识库
- 用户偏好
- 历史决策经验
- 风险标签

那么应把这些内容做成**单独的数据层**，而不是散落在执行 state 里。

---

### 3.4 `backend/app/gateway/routers/skills.py`

#### 职责

- 列出技能。
- 查询单个技能。
- 更新技能启用状态。
- 从 `.skill` archive 安装技能。

#### 设计思想

- 技能是可发现、可启停、可安装的资源。
- 技能状态通过 `extensions_config.json` 持久化。
- 热更新通过 reload 配置实现。

#### 关键点

- `load_skills()` 读取 `skills/public` 和 `skills/custom`。
- skill 具备 `name/description/license/category/enabled`。
- 支持外部 `.skill` 打包安装。

#### 对当前系统的启发

这部分非常值得借鉴。你们当前如果是把审核步骤写死在代码里，可以升级为：

- **skill definition**：每个审核能力模块一份声明式配置或 markdown。
- **skill registry**：统一注册、启停、版本管理。
- **skill installer**：支持上传/导入第三方审核技能包。

这样可以把“新的内容审核能力”从发版变成配置或安装。

---

### 3.5 `backend/app/gateway/routers/threads.py`

这是非常关键的文件。

#### 职责

- 创建 thread。
- 删除 thread。
- 搜索 thread。
- 读取 thread state。
- 更新 thread state。
- 读取 thread history。
- 同步 Store 与 Checkpointer。

#### 核心思想

它把线程数据分成两层：

1. **Store**：用于快速列表、检索、轻量 metadata。
2. **Checkpointer**：用于真实 state、历史、恢复、分支。

并且采用“懒迁移”：

- 如果某些 thread 只存在于 checkpointer，也会在搜索时补回 store。
- 这说明系统考虑了兼容老数据和渐进式演进。

#### 为什么重要

这是 DeerFlow 最值得你们学习的地方之一。

你当前系统的执行记录如果只存在于单一表/单一 state，很容易出现：

- 列表查询慢。
- 历史回放困难。
- 线程恢复不稳定。
- 前端无法稳定显示“当前结果 / 历史步骤 / 分支”。

#### 建议迁移模式

你们可以拆成：

- **Execution Index**：用于快速查找政策执行实例。
- **Execution Checkpoint**：保存每一步中间 state。
- **Execution Snapshot**：用于前端消费和恢复。

---

### 3.6 `backend/app/gateway/routers/uploads.py`

#### 职责

- 上传文件。
- 安全命名。
- 保存到 thread 对应目录。
- 必要时转换为 markdown。
- 写入 sandbox 可见路径。

#### 设计思想

- 文件上传是线程级资源，不是全局资源。
- 上传后还要进入 sandbox/agent 可访问的路径。
- 兼容本地与容器沙箱。

#### 对当前系统的启发

如果你的审核系统要支持：

- 上传图片、视频、文本、音频
- 自动生成中间结果
- 保留证据链

那就应该把“上传文件”设计成**线程附属资源**，并建立：

- host path
- virtual path
- artifact url
- type metadata

这会显著提升可追踪性。

---

### 3.7 `backend/app/gateway/routers/runs.py`

#### 职责

- 提供 stateless stream / wait 接口。
- 自动创建临时 thread。
- 通过 checkpointer 读取最终状态。

#### 设计思想

- 支持既有 thread 模式，也支持“临时 run”。
- 同时支持 SSE 流式输出和阻塞等待。

#### 对当前系统的启发

如果你们的审核场景有：

- 单次策略请求
- 可流式展示的长链路审核
- 前端需要实时看到中间结果

那么应同时提供：

- `stream`：实时增量输出。
- `wait`：批处理或同步消费。

---

### 3.8 `backend/app/gateway/routers/suggestions.py`

#### 职责

- 基于最近消息生成 follow-up suggestions。
- 输出 JSON 数组。

#### 设计思想

- 把“下一步建议”作为独立服务能力。
- 对模型输出做严格解析和清洗。

#### 可借鉴点

对于内容审核系统，也可以做：

- 下一步人工复核建议
- 补充证据建议
- 风险升级建议
- 推荐人工复查字段

这类“辅助决策”很适合独立服务化。

---

### 3.9 `backend/app/gateway/routers/channels.py`

#### 职责

- 查询 IM channel 状态。
- 重启 channel。

#### 设计思想

- 外部入口通过 service 管理，不直接耦合到 agent runtime。
- channel 是可管理资源。

#### 对当前系统的启发

如果未来审核结果要接入：

- 企业 IM 通知
- Webhook
- 邮件
- 工单系统

建议也做成独立 channel/service，而不是写死在审核逻辑里。

---

### 3.10 `backend/app/channels/manager.py`

这是 IM 到 agent 的桥接核心。

#### 职责

- 消费 inbound message。
- 路由到 LangGraph thread。
- 判断是否复用 thread。
- 调用 `runs.wait` 或 `runs.stream`。
- 处理命令（`/new`、`/status`、`/models`、`/memory`、`/help`）。
- 处理附件解析与回复。
- 控制并发和错误。

#### 设计思想

1. **消息总线**：入站、出站分开。
2. **线程复用**：同一 chat/topic 可复用 thread。
3. **流式与非流式分流**：不同 channel 支持不同体验。
4. **附件白名单**：只允许 outputs 目录的产物作为可回传附件。

#### 对当前系统的启发

如果你的系统未来有“审核任务流转”或“人工介入流程”，这个模式非常适合：

- 输入事件进入 bus。
- 执行引擎处理。
- 结果发回 UI / IM / webhook。

尤其适合把“审核任务”、“复核任务”、“告警任务”统一成 event-driven 结构。

---

## 4. Harness 层分析：真正的核心在这里

### 4.1 `backend/packages/harness/deerflow/agents/factory.py`

#### 职责

- 纯参数创建 agent。
- 根据 features 装配 middleware。
- 自动注入 tool / middleware。
- 支持 plan mode。
- 支持 `extra_middleware` 的插槽式插入。

#### 设计思想

- 把 agent 构建拆成“基础工厂 + feature assembly”。
- middleware 顺序是强约束。
- 允许外部扩展，但不破坏核心顺序。

#### 关键亮点

- `ClarificationMiddleware` 必须最后执行。
- `DanglingToolCallMiddleware`、`LoopDetectionMiddleware`、`ToolErrorHandlingMiddleware` 等横切能力集中在链条中。
- `plan_mode` 通过 `TodoMiddleware` 注入。

#### 对当前系统的直接启发

这是你们升级执行链的最好参考：

- 现在如果是单一 `DefaultPlanExecutor`，可以考虑拆成：
  - 前置校验 middleware
  - 规则/技能执行 middleware
  - 错误转换 middleware
  - 状态落库 middleware
  - 结果标准化 middleware
  - 人工确认 middleware
  - 最终收尾 middleware

这样就不必把所有逻辑堆在 executor 里。

---

### 4.2 `backend/packages/harness/deerflow/agents/lead_agent/agent.py`

#### 职责

- 解析运行配置。
- 选择模型。
- 组装 middleware。
- 注入运行 metadata。
- 创建 lead agent。

#### 设计思想

- 模型选择是 runtime 决策，不是编译时固定。
- thinking / vision / reasoning_effort 由模型能力与请求上下文共同决定。
- bootstrap 与 normal flow 分开。

#### 对当前系统的启发

如果你的系统未来可能出现：

- 根据任务类型选不同 policy executor
- 根据风险等级选不同审批链
- 根据租户/业务线选不同技能集

那就应该把“执行器选择”和“运行参数”分开，而不是写死在一个工厂方法里。

---

### 4.3 `backend/packages/harness/deerflow/agents/thread_state.py`

#### 职责

定义 thread state 的结构与 reducer：

- `sandbox`
- `thread_data`
- `title`
- `artifacts`
- `todos`
- `uploaded_files`
- `viewed_images`

#### 设计思想

- state 是结构化的。
- artifact 和 viewed_images 用 reducer 合并。
- `viewed_images` 支持清空语义。

#### 对当前系统的启发

你们的执行状态也应该显式结构化：

- `finalResult`
- `stepResults`
- `warnings`
- `evidence`
- `artifacts`
- `todoState`
- `intermediateState`

这样前端/审计/回放/重试都会更容易。

---

### 4.4 `backend/packages/harness/deerflow/skills/loader.py`

#### 职责

- 定位 skills 根目录。
- 扫描 `public` 和 `custom`。
- 解析 `SKILL.md`。
- 从 extensions config 读取启用状态。
- 支持 enabled_only 筛选。

#### 设计思想

- 技能以文件目录形式组织，便于 Git 管理。
- 启用状态来自配置，而不是硬编码。
- 支持扩展目录和动态发现。

#### 对当前系统的启发

强烈建议借鉴这一点：

- 每个审核能力 / 复核策略都可以做成一个 skill 包。
- skill 包包含：
  - 描述
  - 输入要求
  - 输出 schema
  - 依赖工具
  - 版本号
  - 启用状态

这能大幅提高可维护性。

---

### 4.5 `backend/packages/harness/deerflow/sandbox/sandbox_provider.py`

#### 职责

- 定义 sandbox provider 抽象。
- 以单例方式缓存 provider。
- 支持 reset / shutdown / set。

#### 设计思想

- sandbox 是运行时资源，需要统一生命周期管理。
- provider 可替换。

#### 对当前系统的启发

如果审核过程涉及：

- OCR 引擎
- 视频分析引擎
- 规则引擎
- 外部大模型

就不应该直接在业务代码里 new 对象，而应该做成 provider abstraction，便于：

- 测试替换
- 环境切换
- 灰度发布
- 故障切换

---

### 4.6 `backend/packages/harness/deerflow/sandbox/tools.py`

这是整个仓库里最值得学习的安全控制文件之一。

#### 职责

- 实现 `bash`、`ls`、`read_file`、`write_file`、`str_replace` 等工具。
- 处理虚拟路径与实际路径映射。
- 处理 local sandbox 与 aio sandbox 的差异。
- 做路径 traversal 防护。
- 做输出路径 masking。

#### 设计思想

1. **虚拟路径层**：agent 看到的是 `/mnt/user-data/...`，不是 host path。
2. **本地模式安全边界**：`allow_host_bash` 默认关闭。
3. **读写分离**：skills 和 ACP workspace 有更严格的权限规则。
4. **路径校验与遮蔽**：避免 host 路径泄漏。
5. **runtime-aware 工具**：工具从 runtime state 中拿 thread_data 和 sandbox。

#### 对当前系统的启发

如果你的系统需要让“技能”访问文件或证据材料，强烈建议参考这个模式：

- 给每个执行实例一个虚拟工作目录。
- 不直接暴露 host 文件路径。
- 对写操作做严格白名单。
- 对输出做脱敏/遮蔽。

这对内容审核尤其重要，因为通常会涉及原始素材、证据链、用户隐私。

---

## 5. 前端目录与关键文件分析

### 5.1 `frontend/package.json`

#### 观察

- 技术栈很现代：Next.js 16 + React 19 + Tailwind 4 + shadcn/radix。
- `@langchain/langgraph-sdk` 用于和后端流式交互。
- 包含大量 rich UI 组件和 AI elements。
- 支持 demo / check / typecheck / lint / format。

#### 对当前系统的启发

如果你们要做审核控制台，DeerFlow 的前端思路非常值得参考：

- 统一状态管理。
- 流式渲染。
- 线程历史、消息、任务、附件、设置分区清晰。

---

### 5.2 `frontend/src/core/api/api-client.ts`

#### 职责

- 封装 LangGraph Client。
- 对 `runs.stream` / `joinStream` 做兼容性适配。

#### 设计思想

- 前端对 SDK 版本兼容做了一层薄适配。
- 避免业务代码直接依赖底层细节。

#### 启发

你们也应该在前端和后端 SDK 之间放一层适配层，避免 UI 直接绑定到低层 API 细节。

---

### 5.3 `frontend/src/core/threads/hooks.ts`

这是前端最关键的交互逻辑之一。

#### 职责

- `useThreadStream`：管理流式对话。
- optimistic messages。
- 文件上传。
- thread 创建、完成、错误处理。
- `useThreads`：线程分页搜索。
- `useDeleteThread`：删除线程并同步前端缓存。
- `useRenameThread`：通过 updateState 改标题。

#### 设计思想

1. **流式消息与乐观 UI**：用户输入后立即看到本地消息，等服务端回包再合并。
2. **线程元数据即时同步**：标题更新时直接更新 query cache。
3. **文件上传前置**：先上传附件再 submit。
4. **模式驱动 context**：flash / thinking / pro / ultra 映射到运行上下文。

#### 对当前系统的启发

如果你们做审核控制台，建议也做：

- 乐观提交。
- 进度条 / 中间步骤流式更新。
- 执行结果缓存更新。
- 附件/证据先上传后执行。

---

### 5.4 `frontend/src/app/workspace/chats/page.tsx`

#### 职责

- 展示线程列表。
- 搜索线程标题。
- 跳转到 thread 详情。

#### 设计思想

- 线程列表页很纯粹，只做展示与过滤。
- 标题来自 thread.values.title。

#### 对当前系统的启发

如果你们有 policy execution list / review task list，应该也做成类似的页面：

- 支持搜索。
- 支持按更新时间排序。
- 支持状态筛选。
- 支持快速打开详情。

---

### 5.5 `frontend/src/app/workspace/chats/[thread_id]/page.tsx`

#### 职责

- 渲染线程详情页。
- 提交消息。
- 显示消息列表、Todo、附件按钮、导出按钮、标题。
- 根据当前模式控制输入框。

#### 设计思想

- 一个 thread 是一个完整工作区。
- 顶部是状态与动作，底部是输入，中间是消息历史。
- 没有把所有功能揉成单个大组件，而是拆成多个子组件。

#### 对当前系统的启发

如果你们有“审核详情页”，可以参考这个分区：

- 顶部：任务状态、策略、风险等级、动作按钮。
- 中间：执行历史、证据、模型输出、人工操作记录。
- 底部：补充输入、重新执行、人工批注。

---

### 5.6 `frontend/src/components/workspace/chats/use-thread-chat.ts`

#### 职责

- 从 URL 获取 thread_id。
- 新线程时生成 UUID。
- 支持 mock 参数。

#### 设计思想

- 路由即状态。
- 新建线程和已有线程的逻辑在 hook 中统一处理。

---

### 5.7 `frontend/src/components/workspace/chats/use-chat-mode.ts`

#### 职责

- 根据 URL 参数确定输入框初始内容。
- skill mode 时自动填充提示。

#### 设计思想

- 模式化入口，简化用户操作。

#### 对当前系统的启发

如果你们有不同审核工作流模式，也可以通过 URL / 参数预置输入：

- 图片审核
- 视频审核
- 文本审核
- 申诉复核
- 人工复核模式

---

### 5.8 `frontend/src/core/threads/utils.ts`

#### 职责

- thread path 生成。
- 消息文本提取。
- thread title 获取。

#### 设计思想

- 纯函数做数据转换，UI 更干净。

---

## 6. 目录级别的“文件/类”归类结论

### 6.1 后端核心分层

#### A. 网关层 `backend/app/gateway`

负责：

- 配置展示
- 管理 API
- 线程和状态 CRUD
- 上传/导出
- 轻量业务入口

#### B. 渠道层 `backend/app/channels`

负责：

- IM 入口
- 消息总线
- channel service
- inbound/outbound 适配

#### C. Harness 层 `backend/packages/harness/deerflow`

负责：

- agent 构建
- middleware
- memory
- sandbox
- skills
- models
- runtime state
- client SDK

### 6.2 前端核心分层

#### A. `core`

- API 封装
- 线程 / memory / models / skills / i18n / settings / uploads / artifacts

#### B. `components/workspace`

- 聊天主界面
- 消息列表
- 输入框
- Todo
- 设置页
- 工具按钮

#### C. `app`

- 路由入口
- workspace 页面
- mock/demo 页面

---

## 7. 对当前 content-moderation 系统的升级建议

> 结合你当前系统“单层 Planner + Executor”架构现状，下面是最有落地价值的升级方向。

### 建议 1：把执行链从“单一 executor”升级成“分层 runtime + middleware chain”

#### 现状问题

当前如果是：

- `PolicyDefinition -> skillPipeline`
- `DefaultPolicyPlanner -> ExecutionPlan.steps`
- `DefaultPlanExecutor -> 顺序执行`

那么问题通常是：

- 每个 step 之间耦合高。
- 横切逻辑（日志、错误映射、状态写入、重试、告警）分散在各处。
- 不容易插入人工确认、分支、动态决策。

#### 推荐演进

把执行链拆成：

1. **Plan assembly**：根据 policy 生成 step graph。
2. **Preflight middleware**：参数检查、权限检查、资源准备。
3. **Step execution middleware**：执行单个 skill。
4. **State reducer middleware**：统一写 execution state。
5. **Error normalization middleware**：统一错误码与错误结构。
6. **Artifact middleware**：统一收集证据、输出、附件。
7. **Finalization middleware**：形成 `finalResult`、审计记录。

#### 收益

- 更容易扩展。
- 更容易测试。
- 更容易插入人工审核。
- 更容易支持失败恢复与重放。

---

### 建议 2：把 skill 从“代码里的 class”升级成“声明式 skill 包”

#### 借鉴 DeerFlow 的方式

每个 skill 包可以有：

- `SKILL.md`：说明能力、输入输出、限制、依赖。
- `manifest.yaml/json`：启用状态、版本、分类、权限。
- `scripts/`：必要脚本。
- `references/`：规则、模板、示例。

#### 适用于内容审核的 skill 示例

- 视频内容识别
- OCR 提取
- 音频 ASR
- 违规分类
- 风险解释生成
- 人工复核建议生成
- 申诉理由分析

#### 收益

- 策略能力可热插拔。
- 审核知识可版本化。
- 业务部门可参与维护能力文档。

---

### 建议 3：引入 execution state 标准化

#### 推荐状态结构

参考 DeerFlow 的 `ThreadState` / `ThreadResponse` 思路，建议你们的执行状态至少包含：

- `policyId`
- `runId`
- `currentStep`
- `stepResults`
- `artifacts`
- `warnings`
- `finalResult`
- `humanReviewRequired`
- `auditTrail`
- `error`
- `retryable`

#### 好处

- 前端可以稳定展示。
- 后端可以持久化 checkpoint。
- 审计日志更完整。
- 重试和恢复更容易。

---

### 建议 4：把“线程 / 会话 / 审核实例”三者分开

DeerFlow 很强调 thread 概念。你们也建议拆成：

- **conversation/session**：面向用户交互。
- **execution instance**：一次具体策略执行。
- **artifact bundle**：执行产生的证据和文件。

不要把所有东西都挂在一个对象上。

---

### 建议 5：做一个“网关层”统一管理模型、策略、技能、记忆、附件

可以模仿 DeerFlow 的 gateway：

- `GET /api/models`
- `GET /api/policies`
- `GET /api/skills`
- `GET /api/executions/{id}`
- `POST /api/executions/{id}/files`
- `DELETE /api/executions/{id}`
- `GET /api/memory`
- `PUT /api/memory`

这样前端不会直接碰执行引擎内部实现。

---

### 建议 6：加入 checkpoint / history / replay

这是 DeerFlow 极强的能力之一。

#### 你们可以参考

- 每个 step 结束写 checkpoint。
- 支持回看某个 step 的输入输出。
- 支持从 checkpoint 重放。
- 支持从中间步骤 branching。

#### 适用场景

- 审核争议复盘。
- 模型误判分析。
- 人工复核追踪。
- 策略版本对比。

---

### 建议 7：把附件和证据链做成一等公民

参考 DeerFlow 的 uploads/artifacts 设计：

- 上传文件有 thread scope。
- 结果文件有 outputs scope。
- 前端可直接下载。
- 后端做路径安全校验。

对内容审核系统尤其重要，因为经常涉及：

- 图片/视频切片
- OCR 文本
- ASR 文本
- 违规片段截图
- 人工审核备注

---

### 建议 8：把“辅助决策”做成独立服务

参考 `suggestions`：

- 风险总结
- 下一步建议
- 人工复核提示
- 申诉处理建议
- 补充证据建议

这类能力不应塞进主执行器，否则会污染主链路。

---

### 建议 9：建立配置版本与热升级机制

DeerFlow 的 `config_version`、`config-upgrade` 很值得借鉴。

#### 建议实现

- `config_version` 字段。
- 启动时检测版本差异。
- 提供自动升级脚本。
- 保留 `.bak`。
- 对新增字段做默认补齐。

#### 收益

- 减少线上配置漂移。
- 升级可控。
- 兼容旧配置。

---

### 建议 10：把安全边界前置

参考 DeerFlow 对 sandbox / path / host bash 的处理，你们也应该：

- 明确哪些步骤允许执行外部动作。
- 任何文件路径都做白名单或虚拟化。
- 任何高危动作都支持 guardrail / approval。
- 任何输出都可审计。

对于内容审核系统，这一点尤其重要，因为一旦接入外部模型或自动化处理，风险边界必须前置。

---

## 8. 推荐的落地优先级

### Phase 1：立刻可做

- 标准化 execution state。
- 把 step output / finalResult / artifact 统一写入 state。
- 补充线程/执行实例列表与详情查询接口。
- 前端增加执行历史/结果消费能力。

### Phase 2：结构升级

- 把 executor 拆成 middleware chain。
- 引入 skill registry / skill package。
- 引入 checkpoint / history / replay。
- 增加文件附件和证据管理。

### Phase 3：平台化

- 网关层统一管理模型、技能、策略、记忆。
- 加入外部 channel / webhook / IM 通知。
- 引入 guardrails / approval / audit trail。
- 做配置版本与热升级。

---

## 9. 适配你当前系统的直接建议

结合你当前系统已经存在的“单层 Planner + Executor”现状，我建议下一步最值得做的是：

1. **把每一步 skill 的输出标准化**。
2. **把 execution state 变成强 schema**，而不是散乱 Map。
3. **把 plan/executor 的横切逻辑拆成 middleware**。
4. **让技能/策略可配置化、可启停、可版本化**。
5. **把结果回放和审计能力做出来**。

如果只能选一个最重要的升级方向：

> **优先把“单层执行器”升级成“可插拔、可恢复、可审计的执行运行时”。**

这会对后续的策略扩展、错误恢复、前端展示和审计追踪带来最大收益。

---

## 10. 总结

DeerFlow 的核心价值不是某个模型适配器，而是它把 agent 系统做成了一个**平台级运行时**：

- 配置驱动
- 线程化状态
- 技能化扩展
- 沙箱化执行
- 流式交互
- 记忆与回放
- 统一网关

对于你的内容审核系统，这些思想非常适合用于下一阶段升级。

如果你愿意，我下一步可以继续帮你做两件事中的任意一件：

- **方案一**：把这份分析进一步改写成你们项目内部的《架构升级提案》。
- **方案二**：直接基于你当前 `content-moderation` 代码，给出一份“如何落地 DeerFlow 思路”的具体改造清单（到类/接口级别）。
