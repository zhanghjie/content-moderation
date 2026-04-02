ALTER TABLE video_analysis_task
    ADD COLUMN IF NOT EXISTS draft_payload_json TEXT;

