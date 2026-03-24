CREATE TABLE IF NOT EXISTS skill_definition (
    skill_id VARCHAR(128) PRIMARY KEY,
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS policy_definition (
    policy_id VARCHAR(128) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    skill_pipeline_json TEXT NOT NULL DEFAULT '[]',
    config_json TEXT NOT NULL DEFAULT '{}',
    version VARCHAR(32) NOT NULL DEFAULT 'v1',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
