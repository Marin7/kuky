# Research: Sentence-Starter Answer

> **2026-10-17**: model answers (R2 snapshot field, R4 limit, R7 display, R8 editor rows, R9 stripping) were removed from the feature; the teacher only writes starters. The decisions below are kept as the record of what was first built.

All Technical Context unknowns were resolved by reading the existing question pipeline
(FREE_TEXT manual questions and the 054 SPOT_WRONG_WORDS addition). No external research
was needed.

## R1 — New kind vs. a FREE_TEXT variant

- **Decision**: New `QuestionKind.SENTENCE_STARTER`, plus `QuestionKind.isManual()` (true for
  `FREE_TEXT` and `SENTENCE_STARTER`). Every site that today means "manually graded" by
  `kind == FREE_TEXT` switches to `kind.isManual()`; FREE_TEXT-specific validation stays
  keyed on `FREE_TEXT`.
- **Rationale**: The kind drives the editor, the student input, validation and rendering;
  a FREE_TEXT with an optional structure would leak "starters?" checks into every FREE_TEXT
  path and break the "FREE_TEXT has `{}` structure" invariant enforced in
  `HomeworkAdminService.validateAndMapQuestions`. A single `isManual()` predicate makes the
  ~20 lifecycle/composition/scoring sites (`HomeworkCompositionSupport`,
  `HomeworkSubmissionService`, `ActivityStudentService`, admin review in
  `HomeworkAdminService`/`ActivityAdminService`, `QuestionScoring`/`ActivityExerciseGradingService`
  "not auto-graded" guards) pick the new kind up for free.
- **Alternatives**: FREE_TEXT + `structure.starters` (rejected above); auto-graded kind
  (rejected — spec says manual).

## R2 — Answer storage and how the "begins with" guarantee is enforced

- **Decision**: The client sends only the **continuations** (`answerJson: {"continuations":[…]}`
  on the unified `/answers` path; `continuations` on `ManualAnswerDto` for the ALL_MANUAL path).
  The server composes each sentence with `SentenceStarters.compose(starter, continuation)` and
  stores the existing manual-answer columns:
  - `answer_text` = the composed sentences joined by `\n` (one line per starter, teacher order);
  - `prompt_snapshot` = the prompt (as FREE_TEXT);
  - `answer_json` = `{"starters":[…],"modelAnswers":[…],"continuations":[…]}` — a snapshot of
    the question's structure at submit time plus the raw continuations.
- **Rationale**: Composing server-side makes FR-010/SC-002 true by construction regardless of
  client. Keeping the full sentences in `answer_text` means the existing teacher annotation
  (which rewrites `answer_text` as formatted segments), `%`, note, finalize and unseen-dot flows
  work unchanged. The `answer_json` snapshot gives every view (student result, teacher review,
  homework and activity alike) the starters and model answers of the version answered (FR-012),
  without depending on the assignment snapshot (activities have none).
- **Alternatives**: client-composed text validated by `startsWith` (weaker, duplicated rules);
  one `homework_answers` row per starter (would break one-% per question and the
  one-answer-per-question unique constraints).

## R3 — Joining starter and continuation

- **Decision**: Starter is normalised **at save time** in the validator: strip, then remove a
  trailing `...`/`…` and trailing whitespace (so "porque..." is stored as "porque"). Compose =
  `starter + (continuation starts with one of , . ; : ! ? … ) ? "" : " ") + continuation.strip()`;
  internal newlines in a continuation are collapsed to spaces (one line per sentence).
  Front-end mirrors `compose` only for preview; the server is authoritative.
- **Rationale**: One normalisation point; the student UI shows "porque" followed by the input
  (placeholder "…"), satisfying the ellipsis edge case.

## R4 — Length limits

- **Decision**: 1–10 starters, each 1–80 chars after normalisation; model answer ≤ 300 chars
  (optional); continuation 1–500 chars; **composed answer ≤ 2,000 chars** (same as
  `MAX_PLAIN_ANSWER_CHARS` / `FormattedTextSegment.MAX_VISIBLE_LENGTH`).
- **Rationale**: Teacher annotation re-validates the answer through
  `FormattedTextSegment.validate` (2,000 visible chars). Without the total cap, 10 × 500-char
  continuations could be submitted but never annotated. Spec FR-009 updated accordingly.

## R5 — Drafts

- **Decision**: No device draft. FREE_TEXT **questions** (in `MixedHomeworkForm` and
  `ManualMultiAnswerForm`) have no draft today — only WRITE homework (`ManualAnswerForm`) does.
  Spec FR-011 and US1 scenario 5 corrected to parity with FREE_TEXT questions.
- **Rationale**: Simplicity First; the spec's assumption was based on the WRITE draft.

## R6 — Student forms and submit paths

- **Decision**: One new component `SentenceStarterInput` (fixed starter + input per line, 500-char
  counter) used by both forms:
  - `MixedHomeworkForm` (MIXED; homework, activities) — new answer-state field `continuations`,
    payload `answerJson: { continuations }`, rendered inside the manual-answers section.
  - `ManualMultiAnswerForm` (ALL_MANUAL; homework, activities) — question prop gains
    `kind`/`structure`; payload `continuations`.
  Both treat the question as "manual" via a shared `isManualKind()` in `lib/learning.ts`.
- **Rationale**: These are the two places FREE_TEXT questions are answered today; ALL_MANUAL is
  common for this exercise (a page with only sentence starters).

## R7 — Rendering answers with the starter distinguished (FR-013/FR-014)

- **Decision**: `ManualAnswerViewDto` gains nullable `starters` and `modelAnswers` (from the
  `answer_json` snapshot; `modelAnswers` null whenever teacher scores are stripped, i.e. before
  finalisation for the student). New `SentenceStarterAnswerView` splits the plain or annotated
  text into lines and renders the leading starter of each line in semibold (segment split at a
  character offset when annotated); under each line, the model answer when present. Used by
  the student result sections (both forms) and the read-only parts of `HomeworkReviewDialog` /
  `ActivityReviewDialog`. The annotation editor itself stays the existing plain editor over
  the full text.
- **Alternatives**: encoding the starter as a styled segment in `answer_text` (rejected — would
  collide with teacher annotation colours).

## R8 — Authoring, availability and quizzes

- **Decision**: `QuestionEditorCard` offers the kind (new `SentenceStarterEditor`: list of
  starter + optional model answer rows, add/remove/reorder, live limits); `HomeworkPreview`
  renders the student input. Quizzes: `QuizAdminService.mapQuestions` rejects the kind with
  "Este tipo de pregunta no está disponible en las pruebas." and the quiz editor hides it
  (new `excludedKinds` prop on `QuestionEditorCard`); the `quiz_questions` CHECK is not widened.
  Activities: prompt optional for the kind (extends `freeTextPromptOptional`), as for FREE_TEXT.
- **Rationale**: FR-001/FR-016; activity pages already carry the instructions.

## R9 — Student structure exposure

- **Decision**: `ExerciseGradingService.stripStructureForStudent` (and the activity grader's
  copy) returns `{ "starters": [...] }` — model answers removed. FREE_TEXT student DTOs keep `{}`.
- **Rationale**: FR-014 (model answers hidden until finalised); starters are needed to render
  the inputs.

## R10 — Errores frecuentes and counting

- **Decision**: No change: `HomeworkCompositionSupport.isAutoGradable` returns false for manual
  kinds via `isManual()`, so the kind is excluded from `StudentMistakesAdminService` and
  contributes the teacher % in `contributions` like FREE_TEXT.
