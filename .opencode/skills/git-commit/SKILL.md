---
name: git-commit
description: 将当前项目的未提交内容提交到 git，包括状态检查、变更分析和规范提交
license: MIT
compatibility: opencode
metadata:
  audience: developers
  workflow: git-operations
---

## 角色定位

我是 Git 提交助手，负责安全、规范地将代码变更提交到版本控制系统。

## 提交流程

### Step 1: 检查 Git 状态
执行 `git status` 查看：
- 已修改的文件 (modified)
- 新增的文件 (untracked)
- 已暂存的文件 (staged)
- 当前分支 (branch)
- 与远程的差异 (ahead/behind)

### Step 2: 分析变更内容
对每个变更文件执行：
- `git diff <file>` 查看具体改动
- 识别变更类型：
  - `feat`: 新功能
  - `fix`: Bug 修复
  - `docs`: 文档更新
  - `style`: 代码格式
  - `refactor`: 重构
  - `test`: 测试相关
  - `chore`: 构建/工具

### Step 3: 确认提交范围
与用户确认：
- 是否提交所有变更？
- 是否需要排除某些文件？
- 是否需要分多次提交？

### Step 4: 暂存文件
```bash
# 提交全部
git add .

# 或选择性提交
git add <file1> <file2>
```

### Step 5: 生成提交信息
遵循规范格式：
```
<type>(<scope>): <subject>

<body>

<footer>
```

**示例**:
```
feat(parser): 添加 Prompt 模板回退机制

- 实现场景和语言的默认值回退
- 优先使用精确匹配，其次回退到默认语言
- 最后回退到全局默认模板

Closes #123
```

### Step 6: 执行提交
```bash
git commit -m "type(scope): subject"
```

或打开编辑器编写详细提交信息。

### Step 7: 验证提交
- `git status` 确认工作区干净
- `git log -1` 查看最新提交
- `git show HEAD` 验证提交内容

## 提交规范

### 提交类型 (type)

| 类型 | 说明 | 示例 |
|-----|------|-----|
| `feat` | 新功能 | `feat(web): 添加用户管理页面` |
| `fix` | Bug 修复 | `fix(server): 修复空指针异常` |
| `docs` | 文档更新 | `docs: 更新 API 文档` |
| `style` | 代码格式 | `style: 格式化代码` |
| `refactor` | 重构 | `refactor(parser): 重构模板解析逻辑` |
| `test` | 测试 | `test: 添加单元测试` |
| `chore` | 构建/工具 | `chore: 更新依赖版本` |
| `perf` | 性能优化 | `perf: 优化查询性能` |
| `ci` | CI 配置 | `ci: 更新 GitHub Actions` |

### 作用域 (scope)

**后端模块**:
- `server`: 整体后端
- `parser`: 解析模块
- `planner`: 规划模块
- `executor`: 执行模块
- `critic`: 评估模块
- `controller`: 控制器
- `service`: 服务层
- `model`: 数据模型

**前端模块**:
- `web`: 整体前端
- `components`: 组件
- `pages`: 页面
- `hooks`: React Hooks
- `store`: 状态管理
- `types`: 类型定义

### 主题 (subject)
- 使用祈使句："add" 而非 "added"
- 首字母小写
- 不以句号结尾
- 长度不超过 50 字符

## 特殊场景处理

### 场景 1: 包含敏感信息
检测并排除：
- `.env` 文件
- 包含密钥、密码的文件
- 本地配置文件

处理：
```bash
git reset <sensitive-file>
echo "<file>" >> .gitignore
```

### 场景 2: 大型提交
如果变更过多，建议拆分：
```
建议：当前有 25 个文件变更，建议拆分为多个提交：
1. 重构相关 (10 个文件)
2. 新功能相关 (8 个文件)
3. Bug 修复相关 (7 个文件)

是否分多次提交？
```

### 场景 3: 冲突检测
提交前检查：
```bash
git fetch origin
git diff HEAD...origin/main
```

如有冲突，先解决冲突再提交。

### 场景 4: 提交后钩子失败
如果 pre-commit hook 失败：
- 显示失败原因
- 修复问题后重新提交
- 不跳过钩子检查

## 与其他技能协作

### 与 modification-boundary 协作
- 提交前确认变更在允许范围内
- 验证未修改禁止模块的文件

### 与 skill-manager 协作
- 报告提交状态
- 记录提交历史

## 命令参考

```bash
# 查看状态
git status

# 查看变更
git diff
git diff --staged

# 暂存文件
git add .
git add -p  # 交互式选择

# 提交
git commit -m "message"
git commit -am "message"  # 暂存并提交已跟踪文件

# 查看历史
git log --oneline -5
git show HEAD

# 撤销
git reset HEAD~1  # 撤销提交，保留变更
git commit --amend  # 修改最后一次提交
```

## 注意事项

1. **不要提交**:
   - 敏感信息（密钥、密码）
   - 本地配置文件
   - 构建产物（node_modules, dist, target）
   - IDE 配置文件（.idea, .vscode 除非团队约定）

2. **提交频率**:
   - 小步提交，频繁提交
   - 每次提交完成一个逻辑单元
   - 避免"大爆炸"式提交

3. **提交信息**:
   - 使用中文或英文保持一致
   - 清晰描述变更目的
   - 关联 Issue 编号（如适用）

4. **提交前检查**:
   - 代码已通过编译/构建
   - 测试已通过
   - 代码已格式化
   - 符合项目规范

## 安全规则

- 绝不提交包含密钥的文件
- 绝不使用 `push --force` 到主分支
- 绝不跳过 CI/CD 检查
- 绝不提交未测试的生产代码
