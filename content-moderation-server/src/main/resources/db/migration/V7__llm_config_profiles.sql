CREATE TABLE IF NOT EXISTS llm_config_profile (
    id BIGSERIAL PRIMARY KEY,
    config_code VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(128) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    endpoint TEXT NOT NULL,
    model VARCHAR(128) NOT NULL,
    api_key_enc TEXT NOT NULL,
    timeout_ms INTEGER NOT NULL DEFAULT 120000,
    max_tokens INTEGER NOT NULL DEFAULT 3000,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_llm_config_profile_enabled
    ON llm_config_profile(enabled, is_default);
