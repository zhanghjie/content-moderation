WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY call_id, content_id, analysis_type, COALESCE(user_id, -1)
               ORDER BY created_at DESC, id DESC
           ) AS rn
    FROM video_analysis_task
    WHERE status IN ('PENDING', 'PROCESSING')
)
DELETE FROM video_analysis_task t
USING ranked r
WHERE t.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_video_task_active_biz_key
    ON video_analysis_task(call_id, content_id, analysis_type, COALESCE(user_id, -1))
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE TABLE IF NOT EXISTS prompt_module (
    id BIGSERIAL PRIMARY KEY,
    analysis_type VARCHAR(32) NOT NULL,
    code VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (analysis_type, code)
);

CREATE INDEX IF NOT EXISTS idx_prompt_module_analysis_type ON prompt_module(analysis_type);

CREATE TABLE IF NOT EXISTS prompt_module_set (
    id BIGSERIAL PRIMARY KEY,
    analysis_type VARCHAR(32) NOT NULL UNIQUE,
    default_modules TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
