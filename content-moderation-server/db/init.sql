-- 数据库初始化脚本 - PostgreSQL
-- 数据库：integration
-- 执行时间：2026-03-15

-- 1. 视频分析任务表
CREATE TABLE IF NOT EXISTS video_analysis_task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    call_id VARCHAR(64) NOT NULL,
    content_id VARCHAR(64) NOT NULL,
    video_url TEXT NOT NULL,
    cover_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    trace_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_video_task_call_id ON video_analysis_task(call_id);
CREATE INDEX IF NOT EXISTS idx_video_task_status ON video_analysis_task(status);
CREATE INDEX IF NOT EXISTS idx_video_task_created_at ON video_analysis_task(created_at);

-- 2. 违规事件表
CREATE TABLE IF NOT EXISTS violation_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    task_id VARCHAR(64),
    call_id VARCHAR(64) NOT NULL,
    user_id BIGINT,
    content_id VARCHAR(64),
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

CREATE INDEX IF NOT EXISTS idx_violation_event_call_id ON violation_event(call_id);
CREATE INDEX IF NOT EXISTS idx_violation_event_task_id ON violation_event(task_id);
CREATE INDEX IF NOT EXISTS idx_violation_event_processed ON violation_event(processed);

-- 3. 内容审核记录表
CREATE TABLE IF NOT EXISTS moderation_record (
    id BIGSERIAL PRIMARY KEY,
    record_id VARCHAR(64) NOT NULL UNIQUE,
    content_id VARCHAR(64) NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    moderation_result VARCHAR(20) NOT NULL,
    violation_types TEXT,
    confidence DOUBLE PRECISION,
    reviewer_id VARCHAR(64),
    review_comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_moderation_content_id ON moderation_record(content_id);
CREATE INDEX IF NOT EXISTS idx_moderation_result ON moderation_record(moderation_result);
CREATE INDEX IF NOT EXISTS idx_moderation_created_at ON moderation_record(created_at);

-- 4. 创作者健康分汇总表
CREATE TABLE IF NOT EXISTS creator_health_score (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    health_score INT NOT NULL DEFAULT 100,
    level INT NOT NULL DEFAULT 1,
    total_violations INT NOT NULL DEFAULT 0,
    last_violation_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_creator_score_user_id ON creator_health_score(user_id);

-- 5. 健康分记录表
CREATE TABLE IF NOT EXISTS health_score_record (
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

CREATE INDEX IF NOT EXISTS idx_health_score_user_id ON health_score_record(user_id);
CREATE INDEX IF NOT EXISTS idx_health_score_created_at ON health_score_record(created_at);
CREATE INDEX IF NOT EXISTS idx_health_score_idempotency ON health_score_record(idempotency_key);

-- 6. Skill Definition 表
CREATE TABLE IF NOT EXISTS skill_definition (
    id BIGSERIAL PRIMARY KEY,
    skill_id VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    tags_json TEXT NOT NULL DEFAULT '[]',
    input_schema_json TEXT NOT NULL DEFAULT '{}',
    output_schema_json TEXT NOT NULL DEFAULT '{}',
    state_mapping_json TEXT NOT NULL DEFAULT '{}',
    execution_config_json TEXT NOT NULL DEFAULT '{}',
    script_config_json TEXT NOT NULL DEFAULT '{}',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    timeout_ms INTEGER NOT NULL DEFAULT 3000,
    version VARCHAR(32) NOT NULL DEFAULT 'v1',
    executor_bean VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_skill_definition_id ON skill_definition(id);

-- 插入测试数据（可选）
-- INSERT INTO video_analysis_task (task_id, call_id, content_id, video_url, status) 
-- VALUES ('test-task-001', 'test-call-001', 'test-content-001', 'https://example.com/test.mp4', 'PENDING');

COMMENT ON TABLE video_analysis_task IS '视频分析任务表';
COMMENT ON TABLE violation_event IS '违规事件表';
COMMENT ON TABLE moderation_record IS '内容审核记录表';
COMMENT ON TABLE creator_health_score IS '创作者健康分汇总表';
COMMENT ON TABLE health_score_record IS '健康分记录表';
