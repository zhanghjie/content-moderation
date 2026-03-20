ALTER TABLE video_analysis_task
    ADD COLUMN IF NOT EXISTS analysis_type VARCHAR(32) DEFAULT 'STANDARD',
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS prompt_modules TEXT,
    ADD COLUMN IF NOT EXISTS prompt_snapshot TEXT,
    ADD COLUMN IF NOT EXISTS result_json TEXT,
    ADD COLUMN IF NOT EXISTS moderation_result VARCHAR(16),
    ADD COLUMN IF NOT EXISTS overall_confidence DOUBLE PRECISION;

CREATE INDEX IF NOT EXISTS idx_video_analysis_task_user_id ON video_analysis_task(user_id);
CREATE INDEX IF NOT EXISTS idx_video_analysis_task_analysis_type ON video_analysis_task(analysis_type);

