# Content Moderation Server 项目设计文档

## 1. 项目概述

### 1.1 项目名称
**content-moderation-server** - 基于 Rust API 的视频内容风控服务

### 1.2 技术栈
- **后端框架**: Spring Boot 3.x
- **JDK 版本**: 17
- **数据库**: PostgreSQL 15+
- **ORM**: MyBatis-Plus
- **API 风格**: RESTful API (供 Rust 服务调用)
- **消息队列**: RabbitMQ (可选，用于异步解耦)

### 1.3 核心业务
提供视频内容分析 API，通过 AI 多模态识别主播违规行为，自动扣减健康分。

---

## 2. 架构设计

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      Rust 服务层                              │
│  (Rust Risk Client - 调用 Java API)                          │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP/REST
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   Java API 服务层                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  VideoAnalysisController                            │    │
│  │  - POST /api/v1/video/analyze                       │    │
│  │  - GET  /api/v1/video/result/{callId}               │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  HealthScoreController                              │    │
│  │  - POST /api/v1/health-score/deduct                 │    │
│  │  - GET  /api/v1/health-score/{userId}               │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   业务服务层                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │VideoAnalysis │  │HealthScore   │  │LLM Integration│      │
│  │Service       │  │Service       │  │Service        │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   数据访问层                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │VideoAnalysis │  │HealthScore   │  │Violation     │      │
│  │Mapper        │  │Mapper        │  │Mapper        │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   PostgreSQL 数据库                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │video_        │  │health_score_ │  │violation_    │      │
│  │analysis_task │  │record        │  │event         │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 服务边界

| 服务 | 职责 | 不做什么 |
|------|------|---------|
| **Rust Risk Client** | 调用 Java API、业务编排 | 不直接处理视频分析 |
| **Java API Server** | 视频分析、LLM 集成、健康分管理 | 不直接调用用户服务 |
| **PostgreSQL** | 存储分析任务、健康分记录、违规事件 | 不存储用户信息 |

---

## 3. 数据库设计

### 3.1 视频分析任务表
```sql
CREATE TABLE video_analysis_task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    call_id VARCHAR(64) NOT NULL,
    host_user_id BIGINT NOT NULL,
    video_url TEXT NOT NULL,
    cover_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING, PROCESSING, COMPLETED, FAILED
    retry_count INT DEFAULT 0,
    trace_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    error_message TEXT
);

CREATE INDEX idx_video_task_call_id ON video_analysis_task(call_id);
CREATE INDEX idx_video_task_status ON video_analysis_task(status);
CREATE INDEX idx_video_task_created_at ON video_analysis_task(created_at);
```

### 3.2 健康分记录表
```sql
CREATE TABLE health_score_record (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    score_change INT NOT NULL,
    current_score INT NOT NULL,
    violation_type VARCHAR(50) NOT NULL,
    operator_name VARCHAR(50) NOT NULL DEFAULT 'SYSTEM_VIDEO_ANALYSIS',
    content VARCHAR(255) NOT NULL,
    call_id VARCHAR(64),
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_health_score_user_id ON health_score_record(user_id);
CREATE INDEX idx_health_score_created_at ON health_score_record(created_at);
CREATE INDEX idx_health_score_idempotency ON health_score_record(idempotency_key);
```

### 3.3 违规事件表
```sql
CREATE TABLE violation_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    call_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    violation_type VARCHAR(50) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    evidence TEXT NOT NULL,
    start_sec INT NOT NULL,
    end_sec INT NOT NULL,
    prompt_version VARCHAR(20),
    model_version VARCHAR(20),
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_violation_event_call_id ON violation_event(call_id);
CREATE INDEX idx_violation_event_user_id ON violation_event(user_id);
CREATE INDEX idx_violation_event_processed ON violation_event(processed);
```

### 3.4 创作者健康分汇总表
```sql
CREATE TABLE creator_health_score (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    health_score INT NOT NULL DEFAULT 100,
    level INT NOT NULL DEFAULT 1,
    total_violations INT NOT NULL DEFAULT 0,
    last_violation_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_creator_score_user_id ON creator_health_score(user_id);
```

---

## 4. API 设计

### 4.1 视频分析 API

#### 4.1.1 发起视频分析
```
POST /api/v1/video/analyze

Request:
{
  "callId": "string",
  "videoUrl": "string",
  "coverUrl": "string (optional)",
  "hostUserId": "long",
  "locale": "string (optional, default: zh_CN)"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "string",
    "status": "PENDING|PROCESSING|COMPLETED|FAILED"
  }
}
```

#### 4.1.2 查询分析结果
```
GET /api/v1/video/result/{callId}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "callId": "string",
    "taskId": "string",
    "status": "COMPLETED",
    "violations": [
      {
        "type": "CALL_IN_BED",
        "detected": true,
        "confidence": 0.95,
        "evidence": "主播全程躺姿...",
        "startSec": 0,
        "endSec": 125
      }
    ],
    "summary": {
      "totalViolations": 2,
      "highConfidenceCount": 1,
      "primaryViolation": "CALL_IN_BED"
    }
  }
}
```

### 4.2 健康分 API

#### 4.2.1 扣减健康分
```
POST /api/v1/health-score/deduct

Request:
{
  "userId": "long",
  "violationType": "CALL_IN_BED",
  "scoreDelta": -20,
  "callId": "string",
  "evidence": "string",
  "idempotencyKey": "string"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "long",
    "previousScore": 100,
    "currentScore": 80,
    "scoreDelta": -20
  }
}
```

#### 4.2.2 查询用户健康分
```
GET /api/v1/health-score/{userId}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "long",
    "healthScore": 80,
    "level": 1,
    "totalViolations": 1,
    "lastViolationAt": "2026-03-13T10:00:00Z"
  }
}
```

---

## 5. 13 种违规类型定义

```java
public enum ViolationTypeEnum {
    ENVIRONMENT_MESSY("环境杂乱", -5),
    NOISY("声音嘈杂", -5),
    SEXUAL_ACTION("色情动作", -10),
    PUBLIC_PLACE("公共场合", -10),
    OTHER_PLATFORM_OR_OFFLINE_JOB("其他平台/实体工作", -10),
    MULTI_PERSON_CONTEXT("多人出镜", -15),
    WATCH_TV_OR_PLAY_PHONE("看电视/玩手机", -20),
    CALL_IN_BED("躺在床上通话", -20),
    SILENT_ALL_TIME("全程不说话", -30),
    NO_ONE_ON_CAMERA("无人出镜", -30),
    SLEEPING("睡觉", -30),
    BLACK_SCREEN("黑屏", -50),
    PLAY_RECORDING("播放录屏", -60);
    
    private final String description;
    private final Integer scoreDelta;
}
```

---

## 6. 项目结构

```
content-moderation-server/
├── pom.xml
├── src/main/
│   ├── java/com/risk/
│   │   ├── RustRiskApplication.java
│   │   ├── controller/
│   │   │   ├── VideoAnalysisController.java
│   │   │   └── HealthScoreController.java
│   │   ├── service/
│   │   │   ├── VideoAnalysisService.java
│   │   │   ├── HealthScoreService.java
│   │   │   └── LLMIntegrationService.java
│   │   ├── entity/
│   │   │   ├── VideoAnalysisTaskEntity.java
│   │   │   ├── HealthScoreRecordEntity.java
│   │   │   ├── ViolationEventEntity.java
│   │   │   └── CreatorHealthScoreEntity.java
│   │   ├── mapper/
│   │   │   ├── VideoAnalysisTaskMapper.java
│   │   │   ├── HealthScoreRecordMapper.java
│   │   │   ├── ViolationEventMapper.java
│   │   │   └── CreatorHealthScoreMapper.java
│   │   ├── model/
│   │   │   ├── req/
│   │   │   │   ├── VideoAnalyzeReq.java
│   │   │   │   └── HealthScoreDeductReq.java
│   │   │   ├── res/
│   │   │   │   ├── VideoAnalyzeRes.java
│   │   │   │   └── HealthScoreRes.java
│   │   │   └── dto/
│   │   │       └── ViolationDTO.java
│   │   ├── config/
│   │   │   ├── WebConfig.java
│   │   │   ├── MybatisPlusConfig.java
│   │   │   └── LLMProperties.java
│   │   └── common/
│   │       ├── BaseResult.java
│   │       ├── BizException.java
│   │       └── ViolationTypeEnum.java
│   └── resources/
│       ├── application.yml
│       └── db/migration/
│           └── V1__init_schema.sql
└── src/test/java/com/risk/
    └── controller/
        └── VideoAnalysisControllerTest.java
```

---

## 7. 实施计划

### Phase 1: 基础框架搭建 (1-2 天)
- [ ] 创建项目结构
- [ ] 配置 Spring Boot 3 + JDK 17
- [ ] 配置 PostgreSQL 连接
- [ ] 配置 MyBatis-Plus
- [ ] 创建数据库迁移脚本

### Phase 2: 实体层开发 (1 天)
- [ ] 创建 Entity 类
- [ ] 创建 Mapper 接口
- [ ] 编写单元测试

### Phase 3: 服务层开发 (2-3 天)
- [ ] 实现 VideoAnalysisService
- [ ] 实现 HealthScoreService
- [ ] 实现 LLMIntegrationService
- [ ] 集成 BytePlus API

### Phase 4: 控制器层开发 (1 天)
- [ ] 实现 VideoAnalysisController
- [ ] 实现 HealthScoreController
- [ ] 统一异常处理

### Phase 5: 测试与文档 (1 天)
- [ ] 编写集成测试
- [ ] 生成 API 文档 (OpenAPI/Swagger)
- [ ] 编写部署文档

---

## 8. 配置示例

### 8.1 application.yml
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rust_risk
    username: risk_user
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.moderation.entity
  configuration:
    map-underscore-to-camel-case: true

llm:
  provider: byteplus
  endpoint: https://ark.ap-southeast.bytepluses.com/api/v3/chat/completions
  model: seed-2-0-lite-260228
  api-key: ${LLM_API_KEY}
  timeout-ms: 120000

api:
  base-path: /api/v1
```

---

## 9. 关键代码示例

### 9.1 VideoAnalysisController
```java
@RestController
@RequestMapping("${api.base-path}/video")
@RequiredArgsConstructor
@Slf4j
public class VideoAnalysisController {
    
    private final VideoAnalysisService videoAnalysisService;
    
    @PostMapping("/analyze")
    public BaseResult<VideoAnalyzeRes> analyze(@RequestBody @Valid VideoAnalyzeReq req) {
        log.info("Video analyze request: {}", req);
        VideoAnalyzeRes result = videoAnalysisService.analyze(req);
        return BaseResult.success(result);
    }
    
    @GetMapping("/result/{callId}")
    public BaseResult<VideoAnalyzeRes> getResult(@PathVariable String callId) {
        VideoAnalyzeRes result = videoAnalysisService.getResult(callId);
        return BaseResult.success(result);
    }
}
```

### 9.2 HealthScoreService
```java
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class HealthScoreService {
    
    private final HealthScoreRecordMapper scoreRecordMapper;
    private final CreatorHealthScoreMapper creatorScoreMapper;
    
    public HealthScoreDeductRes deduct(HealthScoreDeductReq req) {
        // 1. 幂等检查
        HealthScoreRecordEntity exist = scoreRecordMapper.selectByIdempotencyKey(req.getIdempotencyKey());
        if (exist != null) {
            return buildDeductRes(exist);
        }
        
        // 2. 获取当前健康分
        CreatorHealthScoreEntity creatorScore = getOrCreateCreatorScore(req.getUserId());
        
        // 3. 计算新分数
        int newScore = Math.max(0, creatorScore.getHealthScore() + req.getScoreDelta());
        
        // 4. 保存记录
        HealthScoreRecordEntity record = new HealthScoreRecordEntity();
        record.setUserId(req.getUserId());
        record.setScoreChange(req.getScoreDelta());
        record.setCurrentScore(newScore);
        record.setViolationType(req.getViolationType());
        record.setContent(req.getEvidence());
        record.setCallId(req.getCallId());
        record.setIdempotencyKey(req.getIdempotencyKey());
        scoreRecordMapper.insert(record);
        
        // 5. 更新汇总
        creatorScore.setHealthScore(newScore);
        creatorScore.setTotalViolations(creatorScore.getTotalViolations() + 1);
        creatorScore.setLastViolationAt(LocalDateTime.now());
        creatorScoreMapper.updateById(creatorScore);
        
        return buildDeductRes(record);
    }
}
```

---

## 10. 验收标准

### 10.1 功能验收
- [ ] 可以成功发起视频分析请求
- [ ] 可以查询分析结果
- [ ] 健康分扣减正确执行
- [ ] 幂等性保证（重复请求不重复扣分）
- [ ] 13 种违规类型正确识别

### 10.2 性能验收
- [ ] 视频分析 API 响应时间 < 2 秒（不含 LLM 调用）
- [ ] 健康分扣减 API 响应时间 < 100ms
- [ ] 支持并发 100 QPS

### 10.3 质量验收
- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试通过率 100%
- [ ] API 文档完整可用

---

**文档版本**: v1.0
**创建时间**: 2026-03-13
**维护团队**: Rust Risk 项目组
