ALTER TABLE policy_definition
ADD COLUMN IF NOT EXISTS execution_input_json TEXT NOT NULL DEFAULT '{}';
