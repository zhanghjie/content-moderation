-- V2__add_health_score_tables.sql - 添加健康分相关表
-- 执行时间：2026-03-15

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

-- 添加注释
COMMENT ON TABLE video_analysis_task IS '视频分析任务表';
COMMENT ON TABLE violation_event IS '违规事件表';
COMMENT ON TABLE moderation_record IS '内容审核记录表';
COMMENT ON TABLE creator_health_score IS '创作者健康分汇总表';
COMMENT ON TABLE health_score_record IS '健康分记录表';
