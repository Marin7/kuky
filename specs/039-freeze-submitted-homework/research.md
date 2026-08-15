# Research: Freeze Submitted Homework

**Feature**: `039-freeze-submitted-homework` | **Date**: 2026-08-15

## 1. How to freeze assignment content

**Decision**: Store one **JSONB `assignment_snapshot`** on `homework_submissions`, written at submit. Result and review hydrate questions, options, keys, title, instructions, and media from that snapshot. Live `homework_questions` stay the source of truth for the teacher editor and for students who have not submitted.

**Rationale**: Spec requires the submitted experience to stay exactly as it was, including in-place prompt/key edits (ids stay the same). A copy of the assignment at submit is the smallest model that does not invent a version-history UI. JSONB matches existing `structure_json` / `answer_json` usage. One column, one write at submit, no parallel question tables.

**Alternatives considered**:
- Assignment-level version table shared by students who submitted the same generation — extra entities and a version browser the spec forbids.
- Copy tables (`homework_submission_questions`) — real FKs and joins, but Flyway surface and dual write for every kind; YAGNI for a single-teacher site.
- Soft-delete / never-update live rows only — does not freeze in-place edits of the same question id.

## 2. Deletes of live questions/options after submit

**Decision**: Keep existing FKs. **`replaceQuestions` must not DELETE** a question or option that is still referenced by `homework_answers.question_id` or `homework_answer_options.option_id`. Mark those rows `retired = true` instead so live take/editor queries omit them (`WHERE retired = false`). Unreferenced leftovers still DELETE as today.

Result/review does **not** read live questions when a snapshot exists, so retired rows are only to preserve FKs and stored scores/selections.

**Rationale**: `homework_answer_options.option_id` is `ON DELETE CASCADE`; deleting a live option would wipe the student’s selected-option rows (FR-006). `homework_answers.question_id` is `ON DELETE SET NULL`, which would break matching answers to snapshot question ids. Retire-instead-of-delete is a boolean + query filter, not a history table.

**Alternatives considered**:
- Drop FKs and rely only on snapshot — looser integrity; existing graders join on `question_id`.
- `ON DELETE SET NULL` on option_id — column is part of a composite primary key and is `NOT NULL`.

## 3. Detecting a stale unsubmitted take (FR-014)

**Decision**: Add `content_revised_at TIMESTAMPTZ` on `homework_assignments`. Bump it when a `PUT /api/v1/admin/homework/{id}` changes title, instructions, type, level, media, or questions. **Do not** bump for due-date-only changes or for `PUT .../assignees`.

Student take GET returns `contentRevisedAt`. Submit (`PUT .../homework/{id}` and `PUT .../homework/{id}/answers`) requires the same timestamp. Mismatch → **`409 HOMEWORK_UPDATED`**. No write, no grade, no snapshot. Client shows the i18n message, clears in-progress form state, refetches the live homework.

Compare on the server inside the submit transaction (lock the assignment row) so a concurrent teacher save cannot sneak in after the client’s GET.

**Rationale**: Spec: message on next reload, return, or submit — not a live push. A timestamp token is enough; hashing the whole assignment is extra. Due date / assignees are operational (FR-012) and must not discard in-progress answers.

**Alternatives considered**:
- WebSocket/live notice when the teacher saves — rejected in clarify (not at the instant of save).
- Hash of questions only — would miss instruction/media edits the spec also freezes for submitted work and that unsubmitted students must pick up.

## 4. Existing submissions at ship

**Decision**: Flyway backfill: for every homework submission with status `SUBMITTED`, `GRADED`, or `REVIEWED` and null snapshot, write `assignment_snapshot` from the **current** live homework (questions + assignment fields). Do not re-grade. After that, later live edits cannot change those result/review views.

**Rationale**: Spec: keep stored answers and scores; cannot recover a lost original; freeze from this moment forward; no silent re-grade.

**Alternatives considered**:
- Leave old rows without snapshot — first teacher edit after ship would still mutate their result views (violates FR-003).
- Rebuild historical originals — impossible; data was overwritten in place.

## 5. What the snapshot contains

**Decision**: Assignment content the student took: `title`, `instructions`, `homeworkType`, `level`, `format`, `composition`, media (`audioUrl`, `audioFileId`, `mediaSourceKind`), and `questions[]` (id, position, kind, prompt, `structure_json`, options with id/position/label/`correct`). Not due date, not assignees, not teacher review fields (those stay on the submission row).

WRITE: questions array empty; still snapshot title/instructions/media/type.

**Rationale**: Spec freezes take/result/review content. Review percents/annotations remain columns on the submission/answers so the teacher can still grade (FR-009).

## 6. Result reconstruction

**Decision**: `ExerciseGradingService.storedResultFor` / mixed student GET / admin exercise-result and review GET use snapshot questions when present. Keep persisted `score_percent` and per-answer `score` / teacher percents. **Do not** call `recomputeUnitResults` against live `structure_json`. Unit-level right/wrong, if shown, is derived from the **snapshot** key plus stored `answer_json` (or skipped in favor of stored per-answer scores when that is enough). Overall % never recomputed from a new live key (FR-005).

**Rationale**: Today unit results are recomputed from live structure, which is the bug. Snapshot key is what they were graded against.

## 7. Activities and placement

**Decision**: No schema, endpoint, or UI changes for presentation activities or the placement test.

**Rationale**: Clarification: homework only.

## 8. Client behaviour

**Decision**: Remember `contentRevisedAt` in component state for the open take. On GET after navigation, if the token changed vs what this session had for that homework id, show the message and reset the form. On `409 HOMEWORK_UPDATED`, same. No `sessionStorage` draft recovery. Homework **list** cards keep the live title; opening a **submitted** item uses snapshot title/instructions/questions from the detail GET.

**Rationale**: Spec discard in-progress answers; list vs detail title assumption; no live channel.
