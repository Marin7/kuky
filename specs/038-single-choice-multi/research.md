# Research: Multi-Item Opción Única

**Feature**: `038-single-choice-multi` | **Date**: 2026-08-15

## 1. One question row vs exploding into N rows

**Decision**: Keep **one** `homework_questions` / `activity_questions` row for the visual entry. Numbered items live in `structure_json`. Scoring **expands** that row into N equal-weight contributions (one per item) wherever overall `%` and fully-correct count are computed.

**Rationale**: Spec requires one authoring/student entry (shared prompt with `(1)`…`(N)`) but N questions in the average. Exploding to N DB rows would duplicate the prompt, break `UNIQUE (submission_id, question_id)` grouping, and force a group-id just to reassemble the UI. Expanding at aggregation time matches how MULTI_BLANK already has per-unit `UnitResultDto`, without changing the “one answer row per question” unique constraint.

**Alternatives considered**:
- Insert N question rows on save — scoring would “just work”, but authoring is one card and the student must not see three copies of the prompt. Needs a grouping key; more moving parts.
- One row whose `score` is the mean of items (MULTI_BLANK pattern) — rejected by clarification: each item counts as its own question, not an internal unit of one question.

## 2. Where options live (table vs JSON)

**Decision**:
- **Classic** (no `(N)` in prompt): unchanged — `homework_question_options` / `activity_question_options`, empty `structure_json` `{}`.
- **Numbered**: options **only** in `structure_json.items[].options`; persist **zero** options-table rows (same as MATCHING / MULTI_BLANK).

On first marker: copy classic option rows onto item `(1)` (preserve labels + which is correct; new option ids are fine). On removing all markers: write item `(1)`’s options back to the options table and clear `items`.

**Rationale**: Spec forbids converting existing homeworks; dual-path avoids Flyway and leaves classic graders untouched. Structured kinds already store keys in JSONB.

**Alternatives considered**:
- Add `item_number` on the options table — Flyway + every option query; unnecessary when JSONB is the established pattern for nested keys.
- Always JSON even for classic — would rewrite every existing SINGLE_CHOICE; spec says do nothing to those rows.

## 3. Marker parsing

**Decision**: Shared parser (Java + TypeScript, same rules as `BlankPassageParser` / `blankTokens.ts`):

- Token: `\((\d+)\)` — ASCII `(` + digits + `)`. `(01)` and `(1)` are the same item (parse as `int`).
- Non-numeric `(ser)` ignored.
- Distinct values must be exactly `{1, 2, …, N}` (N ≥ 1, N ≤ 20). Repeats of the same number are the same item.
- Item order is numeric, not first-appearance order in the text.

Cap **20** matches MULTI_BLANK’s blank cap (`HomeworkAdminService` / `exerciseLimits.ts`).

**Rationale**: Spec FR-015 / FR-017; reuse the `___` token approach teachers already know. 20 is enough for a grammar drill without unbounded authoring/grading loops.

**Alternatives considered**:
- Unicode / fullwidth parentheses — YAGNI; teachers type `(1)` like the spec.
- Unbounded N — reject; same reason MULTI_BLANK is capped.

## 4. Scoring expansion (all aggregators)

**Decision**: Introduce one helper used by homework and activity grading **and** mixed finalize:

`contributions(question, answer) → List<0|1>`  

- Numbered SINGLE_CHOICE: one 0/1 per item (missing selection = invalid submit, not a 0).
- Every other auto kind: one contribution = existing `answer.score` (0–1).
- FREE_TEXT: one contribution = teacher percent / 100 (unchanged).

`structuredCount` / `totalQuestions` / `fullyCorrectCount` / `scorePercent` all use this expanded list. Persist still **one** `homework_answers` row: `answer_json` holds selections; `score` may store the mean for the row (display-only); **do not** use that mean as a single contribution.

**Rationale**: `HomeworkAdminService` mixed finalize today does `scores.add(answer.getScore())` once per row. Without expansion, a 3-item entry would be 1/N of the homework — contradicting clarification B.

**Alternatives considered**:
- N answer rows — blocked by `UNIQUE (submission_id, question_id)` unless the unique key changes (Flyway, more churn).

## 5. Student payload and submit rules

**Decision**:
- Classic: `selectedOptionIds` (one id) as today.
- Numbered: `answerJson.selections` map `"1" → optionId`, …; `selectedOptionIds` empty.
- Student GET structure: `items[].options` with `{ id, label }` only (no `correct`). Prompt still includes the `(1)` text.
- Submit **blocked** (`VALIDATION_ERROR`) if any item lacks a selection (spec FR-007). Frontend disables submit the same way.

**Rationale**: Radio-only; keys stay hidden pre-submit like other structured kinds (`stripStructureForStudent`).

**Alternatives considered**: Pack all selected ids into `selectedOptionIds` without item keys — cannot tell which id belongs to which number when labels repeat.

## 6. UI

**Decision**:
- Authoring: `QuestionEditorCard` watches the prompt; when markers parse to a valid or in-progress `{1…N}` set, show a stacked options editor per number (like `MultiBlankEditor` syncing to `___`). Hint text under the prompt (i18n es/en/ro). Incomplete sequence: keep editors visible, block save with the same server message.
- Student: one prompt (`whitespace-pre-wrap` as today), then stacked `RadioGroup`s labeled `(1)`, `(2)`, … (`ExerciseForm` / `MixedHomeworkForm`). Not inline radios replacing the token (clarification: stacked groups; text shown as authored).
- Results/review: reuse `unitResults` (index = item number − 1 or `index` = N) for per-item right/wrong + `expectedDisplay` = correct label.

**Rationale**: Clarifications already chose stacked radios + marker-driven authoring. MULTI_BLANK editor is the closest existing pattern.

**Alternatives considered**: Inline radios at each `(1)` in the passage — closer to MULTI_BLANK inputs, but the clarify session chose stacked groups with the numbers remaining in the text.

## 7. Activities and placement

**Decision**: Homework `HomeworkAdminService.validateAndMapQuestions` + `ExerciseGradingService` changes; **mirror** grading/strip/submit-complete checks in `ActivityExerciseGradingService` / `ActivityStudentService` / `ActivityAdminService` finalize. Placement SINGLE_CHOICE **untouched**.

**Rationale**: Spec FR-011 / FR-012; activities already share question DTOs and validation.

## 8. Flyway

**Decision**: **No migration.** Existing rows have no `(N)` in `prompt` and keep options-table data. No backfill.

**Rationale**: Spec FR-010.
