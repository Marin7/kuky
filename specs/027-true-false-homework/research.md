# Research: True/False Homework Exercises

**Feature**: `027-true-false-homework` | **Date**: 2026-08-06

## 1. Storage & grading model

**Decision**: Implement `TRUE_FALSE` as a non-structured choice kind: exactly two `homework_question_options` rows (position 0 = true, position 1 = false), exactly one `is_correct`, graded via the existing single-choice path (`selectedOptionIds` set-equality).

**Rationale**: Reuses `HomeworkAdminService` options persistence, `ExerciseGradingService.gradeSingleChoice`, student/result DTOs, and `ExerciseResult` without new JSON schemas or columns. Matches Simplicity First.

**Alternatives considered**:
- Boolean / `structure_json: { "correct": true }` — duplicates option/grading machinery and forces new answer_json shape.
- Alias as SINGLE_CHOICE with two options only — weaker validation and authoring UX; teachers could add/remove options; no dedicated kind label.

## 2. Prompt formatting vs clarification wording

**Decision**: Use the same plain `homework_questions.prompt` TEXT field as `SINGLE_CHOICE` and other exercise kinds. Do **not** introduce rich-text prompt editing in this feature.

**Rationale**: Clarification said “same rich-text formatting as other exercise question prompts.” In the current product, exercise prompts are plain text; rich text (`FormattedTextSegment`) applies to **manual** homework answers/feedback, not exercise prompts. Honoring *parity with other exercise prompts* means plain TEXT. Adding TipTap/HTML prompts across kinds is a separate cross-cutting feature.

**Alternatives considered**:
- Add rich-text prompts for TRUE_FALSE only — inconsistent and higher scope.
- Add rich-text for all exercise prompts — out of scope / YAGNI for this feature.

## 3. Fixed option order & labels

**Decision**: Always persist and display options in order true (position 0) then false (position 1). Never shuffle. Student-facing labels come from i18n (`learning.trueFalse.true` / `.false` or under `admin.homework.questions`), not from editable teacher-typed option text. Stored option `label` values are fixed canonical markers (e.g. `true` / `false`) written by the client defaults / server validation so grading remains ID-based.

**Rationale**: Spec requires fixed order and localized labels; teachers must not edit option text. Canonical stored labels avoid locale drift in the DB while UI language can change.

**Alternatives considered**:
- Store localized Spanish labels in DB — breaks when UI language is Romanian/English.
- Allow teacher-editable labels — violates “exactly two fixed choices” edge case.

## 4. Authoring UX

**Decision**: Dedicated TRUE_FALSE editor branch: rich-parity prompt field (same control as other kinds’ prompt), radio to pick which of the two fixed answers is correct, no add/remove/reorder options.

**Rationale**: Spec forbids custom options; reusing the free-form SINGLE_CHOICE options editor would invite invalid states.

**Alternatives considered**: Reuse SINGLE_CHOICE options UI with validation only — worse UX and easier to break.

## 5. Placement test & explanations

**Decision**: Homework-only; no placement `QuestionKind` change. No teacher explanation field (already clarified).

**Rationale**: Spec FR-001 and clarification session.

**Alternatives considered**: Add to placement — out of scope. Optional explanation — rejected in clarify.

## 6. Migration numbering

**Decision**: Flyway `V9__true_false_homework.sql` (next after `V8__drop_homework_answer_text.sql`).

**Rationale**: Sequential migrations in `back-end/src/main/resources/db/migration/`.
