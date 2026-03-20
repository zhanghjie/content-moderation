# Content Moderation Server

AI 驱动的内容审核服务 - 提供 RESTful API 供其他服务调用

## 项目定位

**纯粹的内容风控判定服务** - 不负责业务处罚

- ✅ **职责**：识别违规内容，返回违规类型、证据、置信度
- ❌ **不负责**：扣分、封禁、用户管理、申诉处理

## 技术栈

- **JDK**: 17
- **框架**: Spring Boot 3.2.0
- **数据库**: PostgreSQL 15+
- **ORM**: MyBatis-Plus 3.5.4
- **API 文档**: SpringDoc OpenAPI 3 (Swagger)

## 快速开始

### 1. 环境准备

```bash
brew install openjdk@17
brew install postgresql@15
```

### 2. 编译项目

```bash
cd /Users/zhanghaojie/IdeaProjects/content-moderation-server
mvn clean install -DskipTests
```

### 3. 启动服务

```bash
java -jar target/content-moderation-server-1.0.0-SNAPSHOT.jar
```

### 4. 访问 API 文档

http://localhost:8080/swagger-ui.html

## API 接口

### 视频分析

```bash
POST /api/v1/video/analyze
Content-Type: application/json

{
  "callId": "call-123",
  "contentId": "content-456",
  "videoUrl": "https://example.com/video.mp4"
}
```

## 项目结构

```
content-moderation-server/
├── src/main/java/com/moderation/
│   ├── ContentModerationApplication.java  # 启动类
│   ├── controller/                        # REST 控制器
│   ├── service/                           # 业务服务
│   ├── entity/                            # 数据库实体
│   ├── mapper/                            # MyBatis Mapper
│   ├── model/                             # 请求/响应模型
│   ├── config/                            # 配置类
│   └── common/                            # 公共类
└── pom.xml
```

## 开发模式

本项目采用 **TDD（测试驱动开发）** 模式：

1. 先写失败的测试（Red）
2. 写最少的代码让测试通过（Green）
3. 重构优化（Refactor）

详细参考：`WEEKLY_TASK_DESIGN.md`

## 架构演进

| 阶段 | 目标 | 状态 |
|------|------|------|
| Phase 1 | 视频审核基础 | ✅ 完成 |
| Phase 2 | 多模态扩展 | ⏳ 计划中 |
| Phase 3 | 智能规划 | ⏳ 规划中 |

详细架构：`ARCHITECTURE_DESIGN.md`

## 联系方式

- 项目路径：`/Users/zhanghaojie/IdeaProjects/content-moderation-server`
- 包名：`com.moderation`
