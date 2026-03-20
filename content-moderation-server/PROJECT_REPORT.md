# Content Moderation Server 项目生成报告

## 项目信息

- **项目名称**: content-moderation-server
- **版本**: 1.0.0-SNAPSHOT
- **JDK 版本**: 17
- **框架**: Spring Boot 3.2.0
- **数据库**: PostgreSQL 15+
- **生成时间**: 2026-03-13
- **项目路径**: /Users/zhanghaojie/IdeaProjects/rust-risk

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 主框架 |
| MyBatis-Plus | 3.5.4 | ORM 框架 |
| PostgreSQL Driver | 42.6.0 | 数据库驱动 |
| Flyway | 内置 | 数据库迁移 |
| Lombok | 1.18.30 | 代码简化 |
| FastJSON2 | 2.0.40 | JSON 处理 |
| SpringDoc | 2.3.0 | API 文档 |

## 项目结构

```
content-moderation-server/
├── pom.xml                                    # Maven 配置
├── README.md                                  # 项目说明
├── PROJECT_DESIGN.md                          # 设计文档
├── .gitignore                                 # Git 忽略文件
└── src/main/
    ├── java/com/risk/
    │   ├── RustRiskApplication.java           # 启动类
    │   ├── common/                            # 公共类
    │   │   ├── BaseResult.java                # 统一返回结果
    │   │   └── ViolationTypeEnum.java         # 违规类型枚举
    │   ├── config/                            # 配置类
    │   │   ├── LLMProperties.java             # LLM 配置
    │   │   └── MybatisPlusConfig.java         # MyBatis-Plus 配置
    │   ├── controller/                        # 控制器
    │   │   ├── VideoAnalysisController.java   # 视频分析 API
    │   │   └── HealthScoreController.java     # 健康分 API
    │   ├── entity/                            # 实体类
    │   │   ├── VideoAnalysisTaskEntity.java   # 视频分析任务
    │   │   ├── HealthScoreRecordEntity.java   # 健康分记录
    │   │   ├── ViolationEventEntity.java      # 违规事件
    │   │   └── CreatorHealthScoreEntity.java  # 创作者健康分
    │   ├── mapper/                            # Mapper 接口
    │   │   ├── VideoAnalysisTaskMapper.java
    │   │   ├── HealthScoreRecordMapper.java
    │   │   ├── ViolationEventMapper.java
    │   │   └── CreatorHealthScoreMapper.java
    │   ├── model/                             # 数据模型
    │   │   ├── req/                           # 请求模型
    │   │   │   ├── VideoAnalyzeReq.java
    │   │   │   └── HealthScoreDeductReq.java
    │   │   ├── res/                           # 响应模型
    │   │   │   ├── VideoAnalyzeRes.java
    │   │   │   ├── HealthScoreDeductRes.java
    │   │   │   └── HealthScoreRes.java
    │   │   └── dto/                           # DTO
    │   │       └── ViolationDTO.java
    │   ├── service/                           # 服务接口
    │   │   ├── VideoAnalysisService.java
    │   │   └── HealthScoreService.java
    │   └── service/impl/                      # 服务实现
    │       ├── VideoAnalysisServiceImpl.java
    │       └── HealthScoreServiceImpl.java
    └── resources/
        ├── application.yml                    # 配置文件
        └── db/migration/                      # 数据库迁移
            └── V1__init_schema.sql            # 初始化脚本
```

## 已实现功能

### 1. 视频分析 API
- ✅ POST /api/v1/video/analyze - 发起视频分析
- ✅ GET /api/v1/video/result/{callId} - 查询分析结果

### 2. 健康分 API
- ✅ POST /api/v1/health-score/deduct - 扣减健康分
- ✅ GET /api/v1/health-score/{userId} - 查询健康分

### 3. 数据库表
- ✅ video_analysis_task - 视频分析任务表
- ✅ health_score_record - 健康分记录表
- ✅ violation_event - 违规事件表
- ✅ creator_health_score - 创作者健康分汇总表

### 4. 核心功能
- ✅ 幂等性保证（通过 idempotencyKey）
- ✅ 事务管理（@Transactional）
- ✅ 自动填充（createdAt/updatedAt）
- ✅ 统一返回结果（BaseResult）
- ✅ API 文档（Swagger UI）

## 待实现功能

### Phase 2: LLM 集成
- [ ] BytePlus API 集成
- [ ] 提示词模板管理
- [ ] LLM 响应解析
- [ ] 违规结果存储

### Phase 3: 完善业务
- [ ] 视频分析完整流程
- [ ] MQ 消息发布
- [ ] 错误处理机制
- [ ] 日志记录完善

### Phase 4: 测试与部署
- [ ] 单元测试
- [ ] 集成测试
- [ ] Docker 镜像
- [ ] 部署文档

## API 使用示例

### 1. 发起视频分析

```bash
curl -X POST http://localhost:8080/api/v1/video/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "callId": "call-123456",
    "videoUrl": "https://example.com/video.mp4",
    "hostUserId": 10001,
    "locale": "zh_CN"
  }'
```

### 2. 扣减健康分

```bash
curl -X POST http://localhost:8080/api/v1/health-score/deduct \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "violationType": "CALL_IN_BED",
    "scoreDelta": -20,
    "callId": "call-123456",
    "evidence": "主播全程躺姿",
    "idempotencyKey": "unique-key-123"
  }'
```

### 3. 查询健康分

```bash
curl http://localhost:8080/api/v1/health-score/10001
```

## 13 种违规类型

| 类型 | 描述 | 扣分 |
|------|------|------|
| ENVIRONMENT_MESSY | 环境杂乱 | -5 |
| NOISY | 声音嘈杂 | -5 |
| SEXUAL_ACTION | 色情动作 | -10 |
| PUBLIC_PLACE | 公共场合 | -10 |
| OTHER_PLATFORM_OR_OFFLINE_JOB | 其他平台/实体工作 | -10 |
| MULTI_PERSON_CONTEXT | 多人出镜 | -15 |
| WATCH_TV_OR_PLAY_PHONE | 看电视/玩手机 | -20 |
| CALL_IN_BED | 躺在床上通话 | -20 |
| SILENT_ALL_TIME | 全程不说话 | -30 |
| NO_ONE_ON_CAMERA | 无人出镜 | -30 |
| SLEEPING | 睡觉 | -30 |
| BLACK_SCREEN | 黑屏 | -50 |
| PLAY_RECORDING | 播放录屏 | -60 |

## 编译验证

```bash
cd /Users/zhanghaojie/IdeaProjects/rust-risk
JAVA_HOME=/Users/zhanghaojie/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home
mvn clean compile
```

**编译状态**: ✅ SUCCESS

## 下一步操作

1. **配置数据库**
   ```bash
   createdb rust_risk
   psql -c "CREATE USER risk_user WITH PASSWORD 'risk_password';"
   psql -c "GRANT ALL PRIVILEGES ON DATABASE rust_risk TO risk_user;"
   ```

2. **配置环境变量**
   ```bash
   export DB_PASSWORD=risk_password
   export LLM_API_KEY=your-byteplus-api-key
   ```

3. **启动服务**
   ```bash
   mvn spring-boot:run
   ```

4. **访问 API 文档**
   - Swagger UI: http://localhost:8080/swagger-ui.html

## 注意事项

1. **JDK 版本**: 必须使用 JDK 17，Spring Boot 3 不支持 JDK 8
2. **Validation**: 使用 Jakarta Validation（不是 javax）
3. **数据库**: PostgreSQL 15+，不支持 MySQL
4. **幂等性**: 健康分扣减必须提供唯一 idempotencyKey
5. **事务**: 所有数据变更操作都有事务保证

## 项目亮点

1. ✅ **架构清晰**: Controller-Service-Mapper 分层明确
2. ✅ **代码规范**: 使用 Lombok 简化代码，统一命名规范
3. ✅ **数据一致性**: 事务管理 + 幂等性保证
4. ✅ **可扩展性**: 预留 LLM 集成接口
5. ✅ **文档完善**: 设计文档 + README + API 文档

---

**生成完成时间**: 2026-03-13 14:04
**项目状态**: 基础框架完成，可开始业务开发
