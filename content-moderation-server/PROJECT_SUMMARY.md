# Content Moderation Server 项目报告

## 项目定位

**纯粹的内容风控判定服务** - 不负责健康分扣减

- ✅ **职责**：识别违规内容，返回违规类型、证据、置信度
- ❌ **不负责**：扣分、封禁、用户管理、申诉处理

## 系统架构

```
Rust 服务 (业务决策层)
    ↓ HTTP/REST
Java API Server (内容风控层)
    ↓
PostgreSQL (数据存储层)
```

## 技术栈

| 组件 | 版本 |
|------|------|
| JDK | 17 |
| Spring Boot | 3.2.0 |
| PostgreSQL | 15+ |
| MyBatis-Plus | 3.5.4 |
| Flyway | 内置 |
| SpringDoc | 2.3.0 |

## 数据库表

| 表名 | 说明 |
|------|------|
| video_analysis_task | 视频分析任务 |
| violation_event | 违规事件 |
| moderation_record | 内容审核记录 |

## API 接口

| 接口 | 路径 | 说明 |
|------|------|------|
| 视频分析 | POST /api/v1/video/analyze | 发起视频分析 |
| 查询结果 | GET /api/v1/video/result/{callId} | 查询分析结果 |
| 图片审核 | POST /api/v1/moderation/image | 图片内容审核 |
| 文本审核 | POST /api/v1/moderation/text | 文本内容审核 |

## 13 种违规类型

ENVIRONMENT_MESSY, NOISY, SEXUAL_ACTION, PUBLIC_PLACE, 
OTHER_PLATFORM_OR_OFFLINE_JOB, MULTI_PERSON_CONTEXT, 
WATCH_TV_OR_PLAY_PHONE, CALL_IN_BED, SILENT_ALL_TIME, 
NO_ONE_ON_CAMERA, SLEEPING, BLACK_SCREEN, PLAY_RECORDING

## 判定结果

| 结果 | 说明 | Rust 服务处理 |
|------|------|-------------|
| PASS | 无违规 | 正常通过 |
| REJECT | 明确违规 | 直接处罚 |
| REVIEW | 疑似 | 人工审核 |

## 项目状态

✅ 基础框架完成
⏳ LLM 集成待开发
⏳ 测试待编写

## 编译状态

**BUILD SUCCESS** - 15 个源文件编译通过
