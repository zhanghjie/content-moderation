# Skill 与 Policy 对齐设计说明

## 1. 目标

解决当前“Policy 编排页展示了 Skill Schema 细节，导致边界混乱”的问题，统一为：

- Skill 注册中心负责 **定义**
- Policy 编排页负责 **引用与编排**
- 页面信息一眼可读、职责清晰、避免重复定义

---

## 2. 单一职责边界

### 2.1 Skill 注册中心（定义域）

Skill 是能力定义单元，唯一来源包含：

- skillId
- name（中文名）
- type
- description
- executionConfig
- scriptConfig
- outputSchema
- inputSchema
- stateMapping
- status

结论：**Schema 只在 Skill 注册中心维护**。

### 2.2 Policy 编排页（编排域）

Policy 是编排单元，只包含：

- policyId
- name
- version
- skillPipeline（skillId 有序数组）
- config（策略参数，不含 Schema）

结论：**Policy 不允许定义或编辑 Skill Schema**。

---

## 3. 页面信息架构（直接看页面就能理解）

### 3.1 Skill 注册中心页面（定义页面）

页面职责文案：

- “在此定义 Skill 能力与执行配置”

展示内容：

- Skill 基本信息
- Prompt/脚本
- 输入输出 Schema
- 执行配置（模型等）

### 3.2 Policy 编排编辑页面（引用页面）

页面职责文案：

- “仅编排 Skill 顺序与策略参数”

节点卡片只展示：

- 节点序号
- Skill 中文名
- Skill 类型
- 执行状态（如已选中、草稿、发布）

不展示：

- 输入 Schema
- 输出 Schema
- stateMapping 字段细节

交互：

- 点击节点「查看定义」跳转 Skill 注册中心（按 skillId 定位）
- 不在 Policy 页面编辑 Schema

### 3.3 Policy 总览页面（摘要页面）

只展示：

- Policy 名称
- 前 N 个 Skill 中文名
- 流程方向

不展示 Schema 字段。

---

## 4. 统一数据契约

### 4.1 Skill 契约

```json
{
  "skillId": "behavior_feature_extract",
  "name": "主播行为特征提取",
  "type": "SEMANTIC",
  "inputSchema": {},
  "outputSchema": {},
  "stateMapping": {}
}
```

### 4.2 Policy 契约

```json
{
  "policyId": "service_quality_profile_v1",
  "name": "服务质量画像默认策略",
  "version": "v1",
  "skillPipeline": ["session_intake", "behavior_feature_extract"],
  "config": {
    "fusionStrategy": "weighted"
  }
}
```

### 4.3 后端约束

注册 Policy 时必须校验：

- skillPipeline 中 skillId 必须存在于 Skill 注册中心
- config 中禁止出现 `inputSchema/outputSchema/stateMapping`（含嵌套）

---

## 5. 视觉与文案规范

### 5.1 Policy 页禁用文案

禁止出现：

- “输入Schema（来自 Skill 注册中心）”
- “输出Schema（来自 Skill 注册中心）”
- 字段级连线语义（例如 `a.b -> c.d`）

### 5.2 Policy 页推荐文案

- “节点编排”
- “策略参数”
- “该页不维护 Skill 定义”

### 5.3 节点详情推荐项

可展示：

- Skill 中文名
- skillId
- 类型
- 描述（短文）

不可展示：

- Schema 原文 JSON

---

## 6. 页面直观理解标准（验收）

用户在不看文档时，进入页面应立刻理解：

1. Skill 注册中心 = 定义能力
2. Policy 编排页 = 排顺序 + 参数
3. Schema 不在 Policy 编辑

验收问答：

- “我在哪改输入输出 Schema？”  
  预期回答：Skill 注册中心

- “我在哪配置执行顺序？”  
  预期回答：Policy 编排页

---

## 7. 落地顺序建议

1. Policy 页移除所有 Schema 区块与文案
2. 保留“查看定义”跳转 Skill 中心
3. 总览与编辑页统一显示 Skill 中文名
4. 后端保留现有注册校验，防止边界回退

---

## 8. 一句话总结

**Skill 定义一次，Policy 只引用；页面上看不到 Schema 编辑入口，就不会再产生职责混淆。**
