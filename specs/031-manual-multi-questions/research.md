# Research: Multi-Question Manual Homework

**Feature**: `031-manual-multi-questions` | **Date**: 2026-08-10

## 1. Storage: reuse questions tables + `FREE_TEXT` kind

**Decision**: Store manual free-text questions in existing `homework_questions` / `activity_questions` with a new kind `FREE_TEXT`. No options, `structure_json = '{}'`. Reject any non-`FREE_TEXT` kind when `format = MANUAL`. WRITE homeworks still have **zero** questions and use `response_text`.

**Rationale**: Tables, ordering (`position`), cascade delete, and admin replace-on-save already exist for EXERCISE. A parallel `homework_manual_questions` table would duplicate machinery. `FREE_TEXT` is never auto-graded, so `ExerciseGradingService` ignores it (MANUAL never calls that path).

**Alternatives considered**:
- Separate manual-question tables — clearer separation, more migration/API surface; rejected (YAGNI).
- Encode questions only in assignment JSON — breaks ordering tools and activity parity; rejected.

## 2. Per-question answers + prompt snapshot

**Decision**: Re-add nullable `answer_text TEXT` on `homework_answers` / `activity_answers` for FREE_TEXT plain answers. Add `prompt_snapshot TEXT NOT NULL` (for FREE_TEXT rows) filled at submit with the question’s prompt at that moment. Keep `question_id` FK `ON DELETE SET NULL` so deleted questions leave answers readable via snapshot. EXERCISE rows keep `answer_text` null and unused; `score` for FREE_TEXT rows stored as `0` (not graded).

**Rationale**: Spec requires plain text (not FormattedText JSON) and FR-005a snapshots without inventing a history table. `answer_text` was removed in V8 because EXERCISE stopped using it; restoring it for FREE_TEXT is the smallest typed column. Snapshot on the answer row matches “already-submitted keeps prompt.”

**Alternatives considered**:
- Store plain text in `answer_json` (`{"text":"..."}`) — works but less direct for queries/review DTOs.
- Keep using submission `response_text` as a JSON map of questionId→text — fights EXERCISE answer tables and loses FK cascade/snapshot clarity.
- Soft-delete questions instead of snapshots — larger authoring change; rejected.

## 3. WRITE vs non-WRITE MANUAL split

**Decision**: Gate multi-question MANUAL on `homework_type != 'WRITE'` (and activities have no WRITE type — all MANUAL activities are multi-question). WRITE: forbid questions; submit `{ response: FormattedText }`; review shows single rich-text response. Non-WRITE MANUAL / MANUAL activity: require ≥1 FREE_TEXT question; submit `{ answers: [{ questionId, text }] }`; `response_text` unused going forward (null after migration).

**Rationale**: Matches clarification Option A. Activities have no homework_type; treating all MANUAL activities as multi-question matches FR-009.

**Alternatives considered**: Force WRITE into multi compact fields — rejected in clarify. One large FREE_TEXT question for WRITE — unnecessary indirection while `response_text` + rich text already work.

## 4. Submit API shape

**Decision**: Extend `SubmitHomeworkRequest` (shared by homework + activity MANUAL PUT) to:

```text
{ "response": FormattedText | null, "answers": [ { "questionId": uuid, "text": string } ] | null }
```

- WRITE: `response` as today; `answers` must be null/absent.
- Multi MANUAL: `answers` required; every current question id present with non-blank trimmed text; `response` null/absent.
- Reject EXERCISE on this endpoint (unchanged).

On accept: upsert submission status SUBMITTED; replace FREE_TEXT answer rows for that submission (delete+insert like EXERCISE); set `prompt_snapshot` from live question prompt; do not write `response_text`.

**Rationale**: One endpoint already used by `ManualAnswerForm` / activity override; avoids routing multi MANUAL through the EXERCISE `/answers` grader.

**Alternatives considered**: Reuse `PUT .../answers` + `ExerciseGradingService` — mixes grading with teacher-reviewed flow; rejected. Separate new path — more surface for little gain.

## 5. Authoring validation changes

**Decision**: Change `HomeworkAdminService.validateAndMapQuestions`:

| format + type | Rule |
|---------------|------|
| MANUAL + WRITE | questions must be empty |
| MANUAL + non-WRITE (or activity MANUAL) | ≥1 question; every kind MUST be `FREE_TEXT`; prompt non-blank; no options; structure `{}` |
| EXERCISE | unchanged (no FREE_TEXT allowed) |

`ActivityAdminService` keeps calling this helper; pass a flag or overload so activities always use the non-WRITE MANUAL rule when format is MANUAL.

**Rationale**: Single validation choke point already shared; flipping the old “MANUAL cannot have questions” error is the intentional behavior change.

## 6. Legacy migration

**Decision**: In `V15` data migration:

1. For each `homework_assignments` where `format = 'MANUAL'` AND (`homework_type` IS DISTINCT FROM `'WRITE'`) AND no questions yet: insert one `FREE_TEXT` question, position 0, prompt `'Tu respuesta'`.
2. For each matching submission with non-null `response_text`: insert `homework_answers` with `answer_text` = plain text extracted from FormattedText JSON (concatenate segment `text` fields), `prompt_snapshot` = `'Tu respuesta'`, `score = 0`; then set `response_text = NULL`.
3. Same for `activities` / `activity_submissions` / `activity_questions` / `activity_answers` where `format = 'MANUAL'`.
4. WRITE rows untouched.

Empty/`null` legacy `response_text` still gets a question so new submits require an answer; no answer row until the student submits again.

**Rationale**: Satisfies FR-011 with a deterministic Spanish default prompt (site primary language). Plain-text extraction drops legacy colors on migrated short answers (acceptable; those were rare on AUDIO/READ).

**Alternatives considered**: Leave dual-read of `response_text` forever — more UI branches. Per-locale prompts — overkill for one-time migration.

## 7. Frontend UX

**Decision**:
- Admin: `ManualQuestionListEditor` (add/reorder/remove prompt-only rows) when `format === 'MANUAL' && homeworkType !== 'WRITE'` (and always for MANUAL activities).
- Student: `ManualMultiAnswerForm` — compact `<Textarea>` per question; client blocks submit if any blank; WRITE keeps `ManualAnswerForm` + rich text.
- Listening/reading/inline/activity: render instructions → audio (if any) → multi form (not a single box).
- Review dialog: list snapshot prompt + plain answer pairs; feedback editor unchanged.

**Rationale**: Matches FR-003/FR-008 and keeps WRITE UX intact.

## 8. Migration numbering

**Decision**: Flyway `V15__manual_free_text_questions.sql` (next after the existing branch `V14`; follows `V13__activity_image.sql` in the main line).

**Rationale**: Sequential migrations in `back-end/src/main/resources/db/migration/`.
