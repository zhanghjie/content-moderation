CREATE TABLE IF NOT EXISTS policy_execution (
    id BIGSERIAL PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL UNIQUE,
    plan_id VARCHAR(64) NOT NULL,
    policy_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    plan_snapshot_json TEXT NOT NULL,
    state_json TEXT NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_policy_execution_policy_time
    ON policy_execution(policy_id, created_at DESC);

CREATE TABLE IF NOT EXISTS policy_execution_step (
    id BIGSERIAL PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL UNIQUE,
    execution_id VARCHAR(64) NOT NULL,
    step_id VARCHAR(64) NOT NULL,
    skill_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt INTEGER NOT NULL DEFAULT 1,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    input_json TEXT,
    output_json TEXT,
    error_message TEXT,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_policy_execution_step_execution
    ON policy_execution_step(execution_id, created_at);

CREATE TABLE IF NOT EXISTS policy_execution_feedback (
    id BIGSERIAL PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64),
    source VARCHAR(32) NOT NULL,
    score DOUBLE PRECISION,
    label VARCHAR(128),
    action VARCHAR(64),
    comment TEXT,
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_policy_execution_feedback_execution
    ON policy_execution_feedback(execution_id, created_at DESC);
