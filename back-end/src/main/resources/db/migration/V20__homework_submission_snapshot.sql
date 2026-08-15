-- Freeze submitted homework: snapshot at submit, revision token, retire referenced questions.

ALTER TABLE homework_assignments
    ADD COLUMN content_revised_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE homework_questions
    ADD COLUMN retired BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE homework_question_options
    ADD COLUMN retired BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE homework_submissions
    ADD COLUMN assignment_snapshot JSONB;

UPDATE homework_submissions s
SET assignment_snapshot = (
    SELECT jsonb_build_object(
        'title', a.title,
        'instructions', a.instructions,
        'homeworkType', a.homework_type,
        'level', a.level,
        'format', a.format,
        'audioUrl', a.audio_url,
        'audioFileId', a.audio_file_id,
        'mediaSourceKind', a.media_source_kind,
        'questions', COALESCE((
            SELECT jsonb_agg(qrow ORDER BY pos)
            FROM (
                SELECT jsonb_build_object(
                    'id', q.id,
                    'position', q.position,
                    'kind', q.kind,
                    'prompt', q.prompt,
                    'structure', COALESCE(q.structure_json, '{}'::jsonb),
                    'options', COALESCE((
                        SELECT jsonb_agg(jsonb_build_object(
                            'id', o.id,
                            'position', o.position,
                            'label', o.label,
                            'correct', o.is_correct
                        ) ORDER BY o.position)
                        FROM homework_question_options o
                        WHERE o.question_id = q.id
                    ), '[]'::jsonb)
                ) AS qrow,
                q.position AS pos
                FROM homework_questions q
                WHERE q.assignment_id = a.id
            ) qrows
        ), '[]'::jsonb)
    )
    FROM homework_assignments a
    WHERE a.id = s.assignment_id
)
WHERE s.status IN ('SUBMITTED', 'GRADED', 'REVIEWED')
  AND s.assignment_snapshot IS NULL;
