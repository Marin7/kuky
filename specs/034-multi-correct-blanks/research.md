# Research: Multiple Correct Blank Answers

**Feature**: `034-multi-correct-blanks` | **Date**: 2026-08-10

## 1. DRAG_DROP structure: positional bank vs explicit blank → ids

**Decision**: New canonical shape stores both `bank[]` and `blanks[]`, where each blank has `correctBankIds: string[]` (1–10 ids referencing `bank[].id`). Bank length is independent of blank count within `[blankCount, 30]` (must be ≥ blank count so each blank can have a distinct primary correct item; may be larger for alternates/distractors). Grading: blank correct iff placed id ∈ that blank’s `correctBankIds`.

**Rationale**: Spec requires any-of matching and pure distractors; positional `bank[i]→blank i` cannot express either. Explicit ids mirror MATCHING’s identity-based pairs and keep shuffle-safe grading.

**Alternatives considered**:
- Keep positional + parallel `alternateIds` on bank items — awkward for “which blank?”, harder distractors.
- Grade by label text equality — breaks distinct items with same label (existing 024 edge case); rejected.
- Require bank size == sum of correct designations — forbids pure distractors (clarification A).

## 2. Legacy compatibility (FR-008)

**Decision**: **Dual-read, no Flyway rewrite.** When grading/validating DRAG_DROP:
- If `structure.blanks` is a non-empty array with `correctBankIds`, use new rules (blank count must equal `___` count).
- Else **legacy**: blank count = `bank.length` (= `___` count); blank `i` accepts exactly `[bank[i].id]`.

On next successful teacher save of a question, `validateDragDrop` always persists the new shape (normalize legacy into `blanks[i].correctBankIds = [bank[i].id]` when client still sends old shape, or accept explicit new shape from updated editor).

**Rationale**: Zero migration risk for existing homework/activity JSON; scores unchanged until teacher re-authors intentionally.

**Alternatives considered**:
- One-shot SQL rewrite of all `structure_json` — unnecessary churn; hard to test every row.
- Version field in structure — YAGNI; presence of `blanks` is enough.

## 3. Typed MULTI_BLANK: what actually changes?

**Decision**: Keep `structure.blanks[].acceptedAnswers[]`. Add validation: after trim/dedupe-by-normalize, **1–10** answers per blank. Feedback: if `acceptedAnswers.size() > 1`, populate `unitResults[].expectedDisplay` with **all** accepted strings whether the unit is correct or not; if size == 1, keep today’s reveal-on-incorrect-only behaviour (FR-007 MAY).

**Rationale**: Authoring UI already supports multiple answers; product gap is caps + always-show pedagogy from clarifications. Table-fill explicitly out of scope (clarification B) — do **not** change `gradeTableFill` reveal or caps.

**Alternatives considered**:
- Also cap/always-show TABLE_FILL — rejected by clarification.
- Always show even single accepted answers — noisier for simple drills; FR-007 allows MAY for singles.

## 4. Authoring UX for word-bank multi-correct

**Decision**: Redesign `DragDropEditor`:
1. **Bank list** — add/remove/edit items (2–30; at least `blankCount` recommended by validation ≥ blankCount).
2. **Per blank** — multi-select (checkboxes) of bank items as correct; disable items already claimed by another blank; require ≥1 per blank; max 10 per blank.
3. Default when blank count grows: assign the first unused bank item (or create a new empty bank row) as sole correct for the new blank — same spirit as today’s 1:1 sync, without forcing bank.length === blankCount.

Student `DragDropQuestion` unchanged in interaction model (place one item per blank); only bank may show more chips than blanks.

**Rationale**: Closest to MultiBlankEditor’s per-blank alternate list; makes distractors and alternates explicit without a second “mode”.

**Alternatives considered**:
- Keep 1:1 bank rows labeled “blank N” plus optional alternate fields — confuses distractors.
- Drag items onto blank slots in authoring — heavier UI; deferred.

## 5. Activity parity

**Decision**: Edit `HomeworkAdminService.validateDragDrop` / MULTI_BLANK caps once (activities already call `validateAndMapQuestions`). Mirror grading/reveal changes in both `ExerciseGradingService` and `ActivityExerciseGradingService` (today they duplicate structured graders).

**Rationale**: Spec FR-009; avoids drift between homework and presentation activities.

**Alternatives considered**: Extract shared grader helper — nice cleanup but optional; only if duplication becomes painful during implement (YAGNI unless touch count is high).

## 6. Feedback UI

**Decision**: Backend fills `expectedDisplay` with all accepted labels when multi; frontend `MultiBlankResult` shows the expected block whenever `expectedDisplay.length > 0`, not only when `!correct`. Single-accepted incorrect blanks still get one expected label as today.

**Rationale**: Clarification Q1 — teach full alternate set even after a correct synonym.

**Alternatives considered**: Separate “also accepted” chip only when correct — more UI complexity for same information.

## 7. Caps summary

| Limit | Value | Where enforced |
|-------|-------|----------------|
| Accepted typed answers / blank | 1–10 | `normalizeAnswerList` / MULTI_BLANK validate (+ UI disable add) |
| Correct bank ids / blank | 1–10 | `validateDragDrop` (+ UI) |
| Bank items / DRAG_DROP question | blankCount…30 (and ≥2 blanks) | `validateDragDrop` |
| Same bank id on two blanks | Forbidden | `validateDragDrop` |
| TABLE_FILL accepted list | Unchanged | No change |

**Rationale**: Spec clarifications Q3–Q4; blankCount lower bound for bank ensures each blank can own a distinct correct item under the no-share rule.
