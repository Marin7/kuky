# Research: Quiz Terminology (Replace Placement Test)

**Feature**: `040-quiz-terminology` | **Date**: 2026-08-15

## 1. Drop placement vs adapt it into quizzes

**Decision**: Delete the placement product. Flyway `V21` drops all `placement_*` tables (prod has no rows). Remove `com.kuky.backend.placement`, front-end `placement/` + `/prueba-de-nivel`, admin tab, student-profile CEFR block, and i18n `placement.*`. New `quiz` package and `/quizzes` UI.

**Rationale**: Spec FR-001–FR-003: not a rename. Placement is timed sections + CEFR + bank-transfer writing + speaking appointment — none of that is in scope. Empty prod DB makes a hard cut cheap.

**Alternatives considered**: Keep placement tables and add a `type` flag; rename copy only. Rejected — leftover CEFR/timer/writing-eval machinery would violate FR-003 and YAGNI.

## 2. Quiz tables vs reuse homework rows

**Decision**: Separate tables (`quizzes`, `quiz_questions`, `quiz_question_options`, `quiz_assignees`, `quiz_attempts`, `quiz_answers`). Quizzes MUST NOT be homework rows or unit content (FR-004). Question **kinds**, `structure_json`, options, and listening media **kinds** match homework.

**Rationale**: Spec forbids linking quizzes to homework/units. Sharing `homework_assignments` would leak quizzes into unit sequences and homework lists (SC-006).

**Alternatives considered**: Discriminator on homework (`format=QUIZ`). Rejected — assignment via units, due dates, WRITE composition, and progress counters would all need exceptions.

## 3. Attempt snapshot timing

**Decision**: Capture `quiz_snapshot` JSONB when the student **starts** (first take GET creates `IN_PROGRESS`). Submit grades that snapshot. Live teacher edits apply only to students who have not started. No `409` / content-token on submit (unlike homework freeze).

**Rationale**: Spec FR-018: in-progress keeps the started set so questions do not vanish mid-take; submitted stays as at submit. Start-time snapshot is one write and avoids the homework `HOMEWORK_UPDATED` flow.

**Alternatives considered**: Snapshot only at submit + 409 if edited during take (homework pattern). Rejected — spec wants the in-progress student to keep their started questions, not restart. Version table. Rejected — JSONB per attempt is enough (same as V20 homework).

## 4. Grading reuse

**Decision**: Extract a small pure `QuestionScoring` helper from `ExerciseGradingService` (per-kind 0–1 scores, numbered SINGLE_CHOICE units, FREE_TEXT unscored until teacher %). `ExerciseGradingService` and new `QuizGradingService` both call it. Overall % and fully-correct count use the same combination rules as mixed homework; per-skill figures are the same rules on the snapshot subset with that `skill`.

**Rationale**: Concrete second consumer of the same kinds (constitution: extract when a present need exists). Duplicating 038 numbered-item math would drift.

**Alternatives considered**: Call `ExerciseGradingService` directly. Rejected — it is wired to homework repositories. Copy-paste scoring. Rejected — drift risk.

## 5. Listening media

**Decision**: Media lives **per quiz question** (`media_source_kind`, `audio_url`, `audio_file_id`) with the same four kinds as homework (`AUDIO_URL`, `UPLOADED_FILE`, `VIDEO_PAGE`, `YOUTUBE`). Required when `skill = LISTENING`. Playback reuses existing student media components and `GET /api/v1/audio/{id}`.

**Rationale**: A mixed quiz cannot use one assignment-level clip the way AUDIO homework does. Placement already stored audio per question.

**Alternatives considered**: Assignment-level media only. Rejected — mixed grammar + several listenings. Skill inferred from media. Rejected — clarify chose an explicit skill on every question.

## 6. Access and routes

**Decision**:
- Student API `/api/v1/quizzes/**` — `hasAnyRole("STUDENT","ADMIN")` plus assignee check (ADMIN still cannot take unless assigned; teacher uses admin API).
- Admin API `/api/v1/admin/quizzes/**` — `ADMIN`.
- Front: `/quizzes` list, `/quizzes/$quizId` take/result. `/prueba-de-nivel` redirects to `/quizzes`. Nav: Quizzes for authenticated non-admin users (replacing the old test destination). Admin tab `quizzes` replaces `placement`. Author pages `/panel/quizzes/nueva` and `/panel/quizzes/$quizId` (mirror `tareas`).

**Rationale**: Spec assignment-only + STUDENT-only assignees. Not under `/learning/**` so coursework and quizzes stay separate.

**Alternatives considered**: Open catalogue under `anyRequest().authenticated()` like old placement. Rejected — clarify Q2 option C. Path `/quiz` singular. `/quizzes` matches the list resource.

## 7. Writing questions

**Decision**: Writing is `FREE_TEXT` questions with `skill = WRITING` (same manual % / annotate / note as mixed homework). No quiz-level WRITE composition and no rich-text-only “one big answer” homework type.

**Rationale**: Spec is a mixed question list. WRITE-as-homework-type is a different shape.

**Alternatives considered**: Allow a WRITE-only quiz. Unnecessary — one FREE_TEXT question covers it.

## 8. Notifications, due dates, CEFR

**Decision**: None. No assign email, no due date, no CEFR. Out of spec.

**Rationale**: YAGNI.

## 9. Unassign / delete

**Decision**: Unassign: drop assignee row; delete `IN_PROGRESS` attempt; keep `SUBMITTED`/`GRADED` attempts so the student and teacher can still open that result. Delete quiz: `ON DELETE CASCADE` everything (spec: quizzes are new, no prod data).

**Rationale**: Spec edge cases.

## 10. Error codes

**Decision**: New codes `QUIZ_NOT_FOUND`, `QUIZ_NOT_ASSIGNED`, `QUIZ_ALREADY_SUBMITTED`. Reuse `VALIDATION_ERROR`, `UNAUTHENTICATED`, `ACCESS_DENIED`. Remove `PLACEMENT_NOT_FOUND` and section codes with the placement package.
