# Phase 0 Research: Homework Exercises & Self-Correction

All Technical Context items were resolvable from the existing codebase and the spec's
Clarifications; there were no open `NEEDS CLARIFICATION` markers. This document records the design
decisions that shape Phase 1.

## 1. Manual vs. exercise discriminator

**Decision**: Add a `format VARCHAR` column to `homework_assignments` with values `MANUAL`
(default) and `EXERCISE`. Pre-existing rows and the manual path use `MANUAL`.

**Rationale**: The spec mandates a binary format (a homework is either self-correcting or manual —
mixing is out of scope). A single discriminator column on the existing table is the smallest change
that lets manual homework keep its current free-text flow untouched while `EXERCISE` rows gain
child questions. `findPublishedAssignmentById` / `findAssignmentsForUser` already return the row, so
mapping one extra column is trivial.

**Alternatives considered**: A separate `homework_exercises` table 1:1 with assignments — rejected
as needless indirection (YAGNI); a boolean `is_exercise` — rejected because a named enum is clearer
and extensible if a third format ever appears.

## 2. Question + answer-key storage

**Decision**: Two tables —
- `homework_questions(id, assignment_id, position, kind, prompt)` where `kind ∈
  {SINGLE_CHOICE, MULTI_CHOICE, FILL_BLANK}`.
- `homework_question_options(id, question_id, position, label, is_correct)` serving **both**
  selectable options (choice questions) and accepted answers (fill-blank: each row is an accepted
  answer with `is_correct = true`, `label` = accepted text).

**Rationale**: Choice options and fill-blank accepted answers are both "answer-key entries attached
to a question." Unifying them into one child table keeps the schema minimal (Simplicity First) and
lets the grader and persistence treat questions uniformly, branching only on `kind`. For
`FILL_BLANK`, options are never sent to the student (they are the key); for choice questions, all
options are sent but `is_correct` is stripped.

**Alternatives considered**: Separate `question_options` and `fill_blank_answers` tables — rejected
as duplicative; a Postgres `text[]`/`jsonb` column for accepted answers — rejected because the
codebase consistently models child collections as relational rows with `RowMapper`s (no JSON
columns anywhere), and arrays would break that convention.

## 3. Student answer + score storage

**Decision**:
- Reuse `homework_submissions` as the per-student exercise record; add `score_percent INT` (nullable;
  set only for graded exercises) and extend the status CHECK to add `GRADED`.
- `homework_answers(id, submission_id, question_id, answer_text, score)` — one row per question the
  student answered, `score` a `NUMERIC(4,3)` in `[0,1]`.
- `homework_answer_options(answer_id, option_id)` — which options the student selected (choice
  questions); empty for fill-blank.

**Rationale**: The existing submission row already carries `(user_id, assignment_id)` uniqueness,
status, and `submitted_at`; extending it avoids a parallel record and keeps the manual and exercise
lifecycles in one place. Per-question rows let the teacher view each answer and let the result be
re-rendered when the student revisits a locked exercise without re-grading. Storing the per-question
`score` makes partial credit auditable.

**Alternatives considered**: Storing answers as JSON on the submission — rejected (breaks the
relational/RowMapper convention, harder for the teacher-view query); recomputing scores on every
read instead of persisting — rejected because the answer key can be edited later and FR requires
preserved scores.

## 4. Status & lifecycle

**Decision**: Submission `status` gains `GRADED`. Manual homework: `PENDING → SUBMITTED →
REVIEWED` (unchanged, teacher-driven). Exercise: a single submit transitions straight to `GRADED`
(terminal, locked). `score_percent` is populated only for `GRADED`.

**Rationale**: The spec requires a status that distinguishes "automatically graded" from "awaiting
manual review" (FR-015) and a single-submission lock (FR-020). A distinct terminal value is the
clearest signal for both the student card and the teacher list, and keeps the existing
`SUBMITTED`/`REVIEWED` semantics intact for manual homework. `HomeworkSubmissionService` (manual)
will reject `EXERCISE` assignments so the two submit paths never cross.

**Alternatives considered**: Reusing `SUBMITTED` + a separate `auto_graded` boolean — rejected as
two fields encoding one concept; auto-marking `REVIEWED` — rejected because `REVIEWED` means "the
teacher looked at it," which is misleading for machine grading.

## 5. Grading rules (authoritative)

Implemented in `ExerciseGradingService`, per the Clarifications:

- **SINGLE_CHOICE**: `score = 1` iff the student's selected set equals exactly the single correct
  option; else `0`.
- **MULTI_CHOICE (partial credit)**: `score = (C_sel + I_unsel) / N`, where `N` = total options,
  `C_sel` = correct options the student selected, `I_unsel` = incorrect options the student did
  **not** select. Range `[0,1]`.
- **FILL_BLANK**: normalize both sides with `value.strip()` then `toLowerCase(Locale.ROOT)`
  (accent-preserving — accents must still match); `score = 1` if the normalized response equals any
  normalized accepted answer, else `0`.
- **Unanswered question**: `score = 0`.
- **Overall**: `scorePercent = round( (Σ question scores / question count) * 100 )`;
  `fullyCorrectCount = count(score == 1)`; `total = question count`.

**Rationale**: Directly encodes the clarified rules. `toLowerCase` does not strip diacritics, giving
the required case-insensitive-but-accent-exact behavior with no extra library. Partial credit is a
simple symmetric "right decisions over all options" fraction that is intuitive and unit-testable.

**Alternatives considered**: Accent folding via `Normalizer` + diacritic removal — rejected because
the clarification chose accent-exact; Jaccard/F1 partial credit — rejected as overkill for a
1-on-1 tutoring tool.

## 6. Hiding the answer key from students

**Decision**: Separate DTOs per audience. Teacher-facing `HomeworkQuestionDto` includes
`options[].correct` and fill-blank accepted answers. Student-facing `ExerciseQuestionDto` includes
only `id, kind, prompt, options[](id,label)` — no `correct`, and **no options at all** for
`FILL_BLANK`. The correct answers appear only in `ExerciseResultResponse` after submission.

**Rationale**: FR-011 forbids exposing the key pre-submission. Audience-specific DTOs make the
omission structural (the field doesn't exist on the wire) rather than relying on the client to
ignore it. The result payload may reveal answers because the exercise is locked post-submit (FR-014).

## 7. Frontend routing & where students take exercises

**Decision**: Flat route files matching the existing `panel_.alumnos.$studentId.tsx` pattern:
- Admin authoring: `panel_.tareas.nueva.tsx` (create) and `panel_.tareas.$homeworkId.tsx` (edit),
  both rendering a shared `HomeworkEditorPage`; both guard `role === 'ADMIN'`.
- Student taking: `aprendizaje_.tarea.$homeworkId.tsx`, rendering `HomeworkExercisePage`; guards on
  an authenticated user.

The homework list's "Nueva tarea"/"Editar" buttons navigate to the admin routes (the
`HomeworkEditorDialog` is deleted). `HomeworkItemCard` routes `EXERCISE` homework to the take page
("Empezar ejercicio"); `MANUAL` homework keeps its existing `HomeworkSubmitDialog`.

**Rationale**: The spec requires both authoring (US1) and taking (clarified) to be dedicated full
pages. The `_`-suffixed flat files render outside the parent layout, giving a real full page, and
follow the one existing precedent in the repo so there is nothing novel to learn.

**Alternatives considered**: A modal/sheet for taking — rejected by the clarification (dedicated
page); nested layout routes — rejected because the repo's established full-page pattern is the flat
`parent_` file.

## 8. Editing an exercise after submissions exist

**Decision**: Editing questions/options does not re-grade existing `GRADED` submissions; their
`homework_answers` rows and `score_percent` are preserved. To keep persistence simple, an update to
an `EXERCISE` replaces its `homework_questions`/options (delete + reinsert within the transaction),
which is safe because `homework_answers.question_id` references are read for display only and the
preserved `score_percent` on the submission remains the source of truth for the recorded grade.

**Rationale**: Matches the spec's edge case (preserve existing scores). Full replace mirrors the
existing `replaceTargets` pattern and avoids per-question diffing. Historical answer rows may point
at deleted question ids; the teacher/student views fall back to the stored `score_percent` and the
answer text, so no foreign-key violation occurs if `homework_answers.question_id` is left
nullable-on-delete (`ON DELETE SET NULL`) — see data-model.

**Alternatives considered**: Versioning questions — rejected as overkill; blocking edits after
submissions — rejected (teacher needs to fix typos), the spec explicitly chose preserve-not-regrade.
