CREATE TABLE IF NOT EXISTS project_definition (
    project_id VARCHAR(128) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_project_definition_status
    ON project_definition(status);

INSERT INTO project_definition (project_id, name, description, status)
VALUES
    ('video_moderation', '视频治理项目', '用于视频内容安全与治理分析', 'ACTIVE'),
    ('live_risk', '直播风控项目', '用于直播场景的风险分析与审核', 'ACTIVE')
ON CONFLICT (project_id) DO NOTHING;
