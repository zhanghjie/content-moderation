ALTER TABLE skill_definition
    ADD COLUMN IF NOT EXISTS id BIGINT;

CREATE SEQUENCE IF NOT EXISTS skill_definition_id_seq;

ALTER SEQUENCE skill_definition_id_seq OWNED BY skill_definition.id;

ALTER TABLE skill_definition
    ALTER COLUMN id SET DEFAULT nextval('skill_definition_id_seq');

WITH ordered_rows AS (
    SELECT
        skill_id,
        ROW_NUMBER() OVER (ORDER BY created_at ASC, skill_id ASC) AS rn
    FROM skill_definition
    WHERE id IS NULL
)
UPDATE skill_definition s
SET id = ordered_rows.rn
FROM ordered_rows
WHERE s.skill_id = ordered_rows.skill_id;

SELECT setval(
    'skill_definition_id_seq',
    COALESCE((SELECT MAX(id) FROM skill_definition), 1),
    EXISTS (SELECT 1 FROM skill_definition)
);

ALTER TABLE skill_definition
    ALTER COLUMN id SET NOT NULL;

ALTER TABLE skill_definition
    DROP CONSTRAINT IF EXISTS skill_definition_pkey;

ALTER TABLE skill_definition
    ADD CONSTRAINT skill_definition_pkey PRIMARY KEY (id);

ALTER TABLE skill_definition
    ADD CONSTRAINT skill_definition_skill_id_key UNIQUE (skill_id);
