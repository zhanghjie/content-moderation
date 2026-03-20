WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY content_id
               ORDER BY created_at DESC, id DESC
           ) AS rn
    FROM video_analysis_task
    WHERE status IN ('PENDING', 'PROCESSING')
      AND content_id IS NOT NULL
)
DELETE FROM video_analysis_task t
USING ranked r
WHERE t.id = r.id
  AND r.rn > 1;

DROP INDEX IF EXISTS uq_video_task_active_biz_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_video_task_active_biz_key
    ON video_analysis_task(content_id)
    WHERE status IN ('PENDING', 'PROCESSING')
      AND content_id IS NOT NULL;
