---
name: "db-persistence-verifier"
description: "验证“持久化”是否真实落到数据库而非内存/缓存/静态文件。Invoke when 用户质疑保存不生效、重启后丢失、怀疑走缓存或旧服务未更新时。"
---

# DB 持久化校验器

## 适用场景

- 用户反馈“保存后刷新/重启丢失”、或“每次启动又回到默认值”
- 用户质疑数据来源是缓存/内存/静态文件，而不是数据库
- 需要对“Policy/Skill/配置”等管理类数据做端到端持久化验真

## 目标

用一组可复现的步骤证明：

1. 写入操作成功落库（数据库层可见）
2. 读取操作来自数据库（重启后仍一致）
3. 不依赖内存缓存/Redis/静态文件回填

## 前置条件

- 后端服务可访问（例如 localhost:8080 或通过前端代理的 localhost:3000/api）
- 能获取数据库连接信息（JDBC URL、用户名、库名）或至少能在同机执行 SQL 查询
- 测试过程中避免修改/删除现有业务 Skill 与 Policy（如需测试，请使用临时 policyId，并在结束后清理）

## 操作步骤（推荐）

### Step 1：构造可识别的“唯一输入”

- 生成一个不会误判的 marker，例如：
  - `marker = "db_check_" + 当前时间戳`
- 将 marker 放入待持久化字段中（例如 `executionInput.marker`）

### Step 2：通过接口写入（Write）

以 Policy 的执行输入为例，调用注册接口写入：

- `POST /api/policy/register`
- Body 示例：
  - `policyId`: 选择一个临时值（如 `db_check_policy_v1`）
  - `executionInput`: 包含 `marker`

记录证据：

- 完整请求体
- 完整响应体（code/message/data）

### Step 3：通过接口立即读取（Read-API）

调用读取接口确认 marker 被回读：

- `GET /api/policy/list`（或详情接口）

判定规则：

- 返回对象中必须包含 `executionInput.marker == marker`
- 若返回缺失 `executionInput`，优先判定为“后端未更新/DTO未透传/Mapper字段映射缺失”，而不是“没落库”

### Step 4：直接查库验证（Read-DB）

直接执行 SQL（以 PostgreSQL 为例）：

```sql
SELECT policy_id, execution_input_json
FROM policy_definition
WHERE policy_id = 'db_check_policy_v1';
```

判定规则：

- `execution_input_json` 中能看到 marker
- 若 DB 无该记录，但接口读到 marker：高度怀疑内存缓存/静态文件
- 若 DB 有记录但接口读不到 marker：高度怀疑接口未走 DB / 字段映射丢失 / 读取被旧进程服务覆盖

### Step 5：重启服务后再次读取（Read-After-Restart）

- 停止并确认端口释放
- 重启后再次调用：
  - `GET /api/policy/list`
  - 再执行一次 Step 4 的 SQL

判定规则：

- 重启后接口读到的 marker 与 DB 一致，才能判定“持久化真实落库”

### Step 6：排除缓存与静态文件路径（Code-Audit）

在代码层做最小排除检查（不依赖运行态）：

- 检索是否存在 Redis 依赖/配置
- 检索是否存在将配置写入本地 JSON/YAML 文件并回读
- 检索 Registry 类是否存在 in-memory Map 作为读取来源（并确认是否仅作为 fallback）

输出证据：

- 关键类的读取分支（例如 SQL read mode 的判定）
- 关键 Mapper 的字段映射（避免 `SELECT *` 导致字段回读缺失）

## 输出模板（交付给用户）

- 结论：`✅ 已确认落库` / `❌ 未落库（原因：...）` / `⚠️ 落库但接口未回读（原因：...）`
- 证据：
  - 写入接口：请求/响应摘要
  - 读取接口：返回中 marker 位置
  - 直接查库：SQL + 查询结果摘要
  - 重启后：接口与 DB 二次一致性结果
- 排除项：
  - Redis/缓存：是否存在
  - 静态文件：是否存在
  - 内存 Map：是否存在且是否参与读路径

