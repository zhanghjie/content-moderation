-- V1__init_schema.sql - 初始化数据库 schema

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
    task_id VARCHAR(64) NOT NULL,
    call_id VARCHAR(64) NOT NULL,
    content_id VARCHAR(64) NOT NULL,
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
