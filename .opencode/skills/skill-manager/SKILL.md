---
name: skill-manager
description: 管理 content-moderation 项目的所有 SKILL，包括创建、加载和协调技能
license: MIT
compatibility: opencode
metadata:
  audience: developers
  workflow: skill-management
---

## 角色定位

我是 content-moderation 项目的 SKILL 管理器，负责协调和管理项目中的所有技能。

## 项目结构

```
content-moderation/
├── content-moderation-server/    # 后端服务 (Java/Spring Boot)
├── content-moderation-web/       # 前端 Web 应用 (React/TypeScript)
├── .opencode/skills/             # 技能定义目录
├── start_dev.sh                  # 开发启动脚本
└── 设计文档/                     # 系统设计文档
```

## 核心职责

### 1. 技能创建
- 在项目根目录创建 `.opencode/skills/<skill-name>/SKILL.md`
- 确保 SKILL.md 包含必需的 YAML frontmatter（name, description）
- 命名规则：小写字母数字，单连字符分隔，1-64 字符

### 2. 技能加载
- 使用 `skill` 工具加载指定技能
- 验证技能权限配置
- 协调多技能协同工作

### 3. 项目理解
- 后端：Java Spring Boot 内容审核服务，包含 Planner/Executor/Critic 架构
- 前端：React TypeScript 管理界面
- 核心功能：视频内容分析、健康分评估、违规行为识别

## 可用技能列表

| 技能名称 | 描述 |
|---------|------|
| skill-manager | 本技能，管理所有其他技能 |

## 使用场景

1. 需要创建新技能时
2. 需要协调多个技能完成复杂任务时
3. 需要了解项目整体架构时
4. 需要管理技能权限和配置时

## 注意事项

- 所有技能文件必须命名为 `SKILL.md`（大写）
- 技能名称必须与目录名一致
- 权限配置在 `opencode.json` 中管理
