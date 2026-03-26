---
name: modification-boundary
description: 限制代码修改范围，确保只修改指定模块，不影响其他模块
license: MIT
compatibility: opencode
metadata:
  audience: developers
  workflow: code-modification
---

## 角色定位

我是代码修改边界控制器，负责确保代码修改严格限制在指定范围内，防止意外修改其他模块。

## 项目模块划分

### 后端模块 (content-moderation-server)

```
content-moderation-server/
├── src/main/java/com/midust/
│   ├── parser/           # 解析模块 - Prompt 解析、模板管理
│   ├── planner/          # 规划模块 - 任务拆解、调度
│   ├── executor/         # 执行模块 - 多模态理解、要素抽取
│   ├── critic/           # 评估模块 - 结果验证、置信度评估
│   ├── controller/       # 控制器模块 - API 接口
│   ├── service/          # 服务模块 - 业务逻辑
│   ├── repository/       # 数据访问模块
│   ├── model/            # 数据模型模块
│   └── config/           # 配置模块
```

### 前端模块 (content-moderation-web)

```
content-moderation-web/
├── src/
│   ├── pages/            # 页面模块
│   ├── components/       # 组件模块
│   ├── services/         # API 服务模块
│   ├── hooks/            # React Hooks 模块
│   ├── store/            # 状态管理模块
│   ├── utils/            # 工具函数模块
│   └── types/            # 类型定义模块
```

## 修改边界规则

### 1. 明确修改目标
在开始修改前，必须确认：
- 目标模块路径
- 相关依赖模块
- 禁止修改的模块列表

### 2. 边界声明格式
```
修改范围声明：
- 允许修改：[模块 A 路径]
- 禁止修改：[模块 B 路径]
- 只读参考：[模块 C 路径]
```

### 3. 修改前检查清单
- [ ] 已确认目标模块路径
- [ ] 已列出相关依赖
- [ ] 已声明禁止修改的模块
- [ ] 已评估修改影响范围

### 4. 修改后验证
- [ ] 只修改了允许的文件
- [ ] 未修改禁止模块的任何文件
- [ ] 依赖模块仅读取未修改
- [ ] git diff 显示变更在预期范围内

## 典型使用场景

### 场景 1：只修改后端解析模块
```
修改范围声明：
- 允许修改：content-moderation-server/src/main/java/com/midust/parser/**
- 禁止修改：content-moderation-server/src/main/java/com/midust/planner/**
- 禁止修改：content-moderation-server/src/main/java/com/midust/executor/**
- 只读参考：content-moderation-server/src/main/java/com/midust/model/**
```

### 场景 2：只修改前端组件
```
修改范围声明：
- 允许修改：content-moderation-web/src/components/**
- 禁止修改：content-moderation-web/src/pages/**
- 只读参考：content-moderation-web/src/types/**
```

### 场景 3：只修改 API 接口
```
修改范围声明：
- 允许修改：content-moderation-server/src/main/java/com/midust/controller/**
- 禁止修改：content-moderation-web/**
- 只读参考：content-moderation-server/src/main/java/com/midust/service/**
```

## 工作流程

### Step 1: 接收修改请求
用户描述需要修改的功能和涉及的范围。

### Step 2: 声明修改边界
明确列出允许修改、禁止修改、只读参考的模块。

### Step 3: 执行修改
- 只修改允许范围内的文件
- 可以读取参考模块以理解上下文
- 绝不修改禁止范围内的任何内容

### Step 4: 验证变更
使用 `git diff` 验证所有变更都在允许范围内。

## 注意事项

1. **跨模块依赖**：如需修改依赖模块，必须先更新边界声明
2. **公共代码**：utils、model 等公共模块的修改需要格外谨慎
3. **配置文件**：修改配置前需确认影响范围
4. **测试文件**：修改代码时应同步考虑测试文件的更新

## 违规检测

以下情况视为边界违规：
- 修改了禁止模块的文件
- 删除了非目标模块的代码
- 改变了未授权模块的行为
- 引入了未授权的跨模块依赖

## 与 skill-manager 协作

本技能可与 `skill-manager` 配合使用：
- `skill-manager` 负责整体技能协调
- `modification-boundary` 负责具体修改范围控制
