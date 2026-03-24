UPDATE skill_definition
SET script_config_json = jsonb_set(
        COALESCE(script_config_json::jsonb, '{}'::jsonb),
        '{input_schema}',
        COALESCE(input_schema_json::jsonb, '{}'::jsonb),
        true
    )::text
WHERE input_schema_json IS NOT NULL
  AND trim(input_schema_json) <> '';

ALTER TABLE skill_definition
DROP COLUMN IF EXISTS input_schema_json;
