# Skill OS（Skill 注册工厂 + Policy 执行引擎）设计文档

## 1. 目标

构建一个工程化的 Skill OS，满足以下原则：

- Skill 只承载能力定义与执行实现
- Policy 预定义执行路径（Pipeline）
- Execution Engine 不做策略决策，只按 Policy 顺序执行
- 支持前后端联动配置与运行

## 2. 后端设计

### 2.1 核心模型

- SkillDefinition：定义 skillId、类型、输入输出 schema、executorBean、版本等
- PolicyDefinition：定义 policyId、skillPipeline、配置项 config、版本
- ExecutionState：跨 Skill 的共享状态容器，基于 ConcurrentHashMap
- SkillContext：执行上下文，承载 state、input、policyConfig
- SkillResult：Skill 执行结果（success/output/message）
- SkillExecutionTrace：单个 Skill 的耗时、状态、输出、跳过信息

### 2.2 注册中心

- SkillRegistry
  - register：注册或更新 SkillDefinition
  - list：查询 Skill 清单
  - get：按 skillId 获取定义
  - 默认内置 7 个视频风控 Skill（role_detect ~ violation_aggregate）
- PolicyRegistry
  - register：注册或更新 PolicyDefinition
  - list：查询 Policy 清单
  - get：按 policyId 获取定义
  - 默认内置策略 video_risk_v1

### 2.3 执行引擎

- PolicyExecutionEngine.execute(policyId, input)
  - 加载 PolicyDefinition
  - 初始化 ExecutionState（写入 input）
  - 遍历 skillPipeline
  - 通过 executorBean 从 Spring 容器获取 SkillExecutor
  - 构造 SkillContext 并执行
  - 将输出写回 state[skillId]
  - 记录 SkillExecutionTrace（耗时/成功/跳过）
  - 汇总 PolicyExecuteResult 返回

### 2.4 可选能力（已落地）

- Skill 执行日志与耗时：通过 traces 输出
- Skill 开关控制：Policy config 支持 enableCallInBedCheck / enableBlackScreenCheck
- 简单 if 跳过：支持 disabledSkills、skillSwitches、skipIfStateMissing

### 2.5 API 设计

- Skill
  - POST /api/skills/register
  - GET /api/skills/list
- Policy
  - POST /api/policy/register
  - GET /api/policy/list
- 执行
  - POST /api/execute

## 3. 前端设计

### 3.1 页面一：Skill 注册工厂

- 功能
  - 创建 Skill
  - 编辑 Skill（点击列表回填）
  - 查看 Skill 列表
  - 选择 executorBean
- 路由：/settings/skills
- API：src/api/skillos.ts 中 registerSkill/listSkills

### 3.2 页面二：Policy 配置中心

- 左侧：Policy 列表（名称、版本、Skill 数量）
- 中间：Pipeline 排序区
  - 添加 Skill
  - 拖拽排序
  - 删除 Skill
- 右侧：Policy 配置
  - 违规开关
  - 阈值参数
- 操作与结果
  - 保存 Policy
  - 运行 Policy
  - 查看 Execution State 与 Trace
- 路由：/settings/policies
- API：src/api/skillos.ts 中 registerPolicy/listPolicies/executePolicy

## 4. 最小可用视频风控流程

默认策略 video_risk_v1：

1. role_detect
2. video_parse
3. asr
4. semantic_analysis
5. violation_call_in_bed
6. violation_black_screen
7. violation_aggregate

输入示例：

```json
{
  "policyId": "video_risk_v1",
  "input": {
    "videoUrl": "https://demo/video_call_black.mp4",
    "transcript": "来床上聊，黑屏画面很多"
  }
}
```

可得到跨 Skill 流转的 state 与最终风险聚合结果。

## 5. 目录变更

- 后端新增
  - com.moderation.skillos.model
  - com.moderation.skillos.executor
  - com.moderation.skillos.registry
  - com.moderation.skillos.engine
  - controller/SkillOsController
  - model/req（SkillRegisterReq、PolicyRegisterReq、PolicyExecuteReq）
  - model/res（SkillListRes、PolicyListRes、PolicyExecuteRes）
- 前端新增
  - src/api/skillos.ts
  - src/views/settings/SkillFactory.vue
  - src/views/settings/PolicyCenter.vue
  - 路由与侧边导航入口扩展

## 6. 原则符合性说明

- 未实现“自动选择 Skill”
- 未实现“AI 决策流程”
- 执行路径完全由 Policy 的 skillPipeline 决定
- Execution Engine 只做执行与状态记录
