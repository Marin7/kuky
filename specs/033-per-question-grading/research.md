# Research: Per-Question Homework Grading

**Feature**: `033-per-question-grading` | **Date**: 2026-08-10

## 1. How is “auto vs manual” represented per question?

**Decision**: Use existing `QuestionKind` — `FREE_TEXT` ⇒ manual; any structured kind (`SINGLE_CHOICE`, `MULTI_CHOICE`, `TRUE_FALSE`, `MULTI_BLANK`, `DRAG_DROP`, `TABLE_FILL`, `MATCHING`) ⇒ auto-correctible. No new `grading_kind` column.

**Rationale**: Spec maps 1:1 onto today’s kinds; authoring already configures answer keys only for structured kinds. YAGNI — avoid a second orthogonal flag that can disagree with `kind`.

**Alternatives considered**:
- Explicit `grading_kind` column — redundant with `FREE_TEXT` vs structured; risk of inconsistent rows.
- Keep assignment `format` as the source of truth — rejected by clarified spec (remove homework-level Manual/Exercise).

## 2. What happens to assignment-level `format`?

**Decision**: Stop requiring `format` on create/update. On save, **derive and store** `format` as `MANUAL` | `EXERCISE` | `MIXED` (expand CHECK):
- `WRITE` homework type → `MANUAL` (questions empty; single `response_text`)
- Only `FREE_TEXT` questions → `MANUAL`
- Only structured questions → `EXERCISE`
- Both `FREE_TEXT` and structured → `MIXED`

API responses also expose derived `composition`: `WRITE` | `ALL_MANUAL` | `ALL_AUTO` | `MIXED` for UI dispatch (preferred over raw `format`).

**Rationale**: Keeps list filters and gradual client migration simple; `MIXED` makes awaiting-review queues and student panels unambiguous. Derivation on write prevents stale values.

**Alternatives considered**:
- Drop `format` column entirely — more churn across DTOs/lists; deferred.
- Soft default that still accepts client `format` — conflicts with “no homework-level choice”; ignore client `format` if sent.

## 3. Submission status lifecycle for mixed

**Decision**:

| Composition | On submit | Terminal |
|-------------|-----------|----------|
| ALL_AUTO | `GRADED` (+ `score_percent`) | `GRADED` (locked answers; exercise feedback rules unchanged) |
| ALL_MANUAL / WRITE | `SUBMITTED` | `REVIEWED` via existing annotate/note review (no required validate/invalidate) |
| MIXED | `SUBMITTED` (auto answers scored; keys revealed; **no final** `score_percent`) | Teacher finalize → `GRADED` with combined `score_percent`; `review_model=ANNOTATED` when annotations/note used; validations re-editable after (FR-017) |

**Rationale**: Clarification Q4 — mixed shares awaiting status with pure-manual (`SUBMITTED`). Clarification Q1 — final combined % implies a scored terminal state → `GRADED`. Pure-manual stays `REVIEWED` without forced validate/invalidate (FR-012).

**Alternatives considered**:
- New `PARTIALLY_GRADED` status — rejected by clarification (no distinct status).
- Mixed finalize → `REVIEWED` with score — workable, but `GRADED` better matches “has percentage” and student exercise-result mental model.
- Keep mixed answers locked forever after `GRADED` with no re-edit — conflicts with FR-017; allow re-save of validations/annotations/note after `GRADED` for mixed only.

## 4. Combined score formula

**Decision**: Equal weight per question. `score_percent = round(mean(questionScores) * 100)` where each question contributes `0..1`:
- Structured: existing `ExerciseGradingService` per-kind score (partial credit preserved).
- Manual: `VALIDATED` → `1.0`; `INVALIDATED` → `0.0`.
- `fullyCorrectCount` = count of questions with score `== 1.0` (same presentation as today’s exercises: `"80% — 8 of 10…"`).

Before mixed finalize: may expose **provisional** auto-only mean as `provisionalScorePercent` (nullable); **omit** final `scorePercent` (or null) until finalize (FR-010a).

**Rationale**: Matches clarification Q1; reuses existing mean-of-questions math.

**Alternatives considered**:
- Auto-only % forever with validations as badges — rejected by clarification B.
- Weight manuals differently — not specified; equal weight is simplest.

## 5. Student submit API shape

**Decision**: Unify non-WRITE submit onto `PUT /api/v1/learning/homework/{id}/answers` (and activity mirror) accepting a heterogeneous `answers[]`:
- Structured items: existing option/json payloads.
- `FREE_TEXT` items: `{ questionId, text }` (plain, non-empty, length cap as today).

`PUT /api/v1/learning/homework/{id}` remains **WRITE-only** (`response` FormattedText).

Reject submit if any question unanswered. Branch service logic on derived composition (not client `format`).

**Rationale**: One take page / one submit for mixed; WRITE stays special. Deprecate MANUAL-only use of bare `PUT /{id}` with `answers` (accept during transition then remove).

**Alternatives considered**:
- Two parallel submits merged client-side — fragile partial state.
- New `/submit` path — unnecessary alias.

## 6. Teacher finalize / validate API

**Decision**: Extend existing `PUT /api/v1/admin/homework/submissions/{id}/feedback` (activity mirror) for mixed:
- Require `answers[].teacherValidation` ∈ `VALIDATED` | `INVALIDATED` for every FREE_TEXT answer when composition is `MIXED`.
- Optional per-answer `formatted` annotations + optional `feedbackText` (032 rules).
- On success for MIXED: persist validations, recompute combined `score_percent`, set status `GRADED`, set `reviewed_at`, `review_model` as today for annotations.
- Pure MANUAL/WRITE: existing behavior (`REVIEWED`); ignore/forbid `teacherValidation` if sent (or accept no-op).
- ALL_AUTO: continue using exercise-result / exercise-feedback endpoints; no validate flow.

**Rationale**: Reuses 032 review dialog; adds only the required decisions for mixed.

**Alternatives considered**:
- Separate `/validate` endpoint — extra surface; rejected for YAGNI.
- Require validate/invalidate on pure-manual — rejected by FR-012 / clarification.

## 7. Answer-key reveal on mixed submit

**Decision**: On MIXED submit, return auto-question results with keys stripped-in (same as ALL_AUTO graded view) immediately; status remains `SUBMITTED`. Clarification Q5.

**Rationale**: No retake; matches exercise UX; manuals still pending.

## 8. Authoring validation changes

**Decision**: Replace format-gated rules in `HomeworkAdminService.validateAndMapQuestions`:
- WRITE: `questions` must be empty (unchanged).
- Non-WRITE / activities: ≥1 question; each question validated by its own kind (FREE_TEXT: prompt only; structured: existing option/structure rules). **Allow mix.**
- Derive and persist `format` including `MIXED`.

**Rationale**: Spec FR-001–FR-004.

## 9. Migration / backfill

**Decision**: `V17__per_question_grading.sql`:
1. Add `teacher_validation VARCHAR(16) NULL` CHECK (`VALIDATED`,`INVALIDATED`) on `homework_answers` and `activity_answers`.
2. Drop old format CHECKs; add CHECK including `MIXED`.
3. Backfill `format`: if assignment/activity has both FREE_TEXT and structured questions → `MIXED`; else leave MANUAL/EXERCISE as today (already consistent).
4. No change to existing submissions’ statuses.

**Rationale**: Zero behavioral change for legacy all-manual / all-auto until teachers add a mixed question.

## 10. Frontend dispatch

**Decision**: Prefer `composition` (or question-kind inspection) over `format === "EXERCISE"`:
- Authoring: remove Manual/Exercise radios; one list with kind picker including FREE_TEXT + structured.
- Student: single form rendering per-question controls; WRITE pages unchanged.
- Review: if `MIXED` && `SUBMITTED`/`GRADED`, show auto results + validate controls; if `ALL_MANUAL`/`WRITE`, existing review; if `ALL_AUTO`, exercise result dialog.

**Rationale**: Clarifications A/B for authoring and review toolkit.

## 11. Presentation activities

**Decision**: Full parity — same schema/API/UI changes on activity tables and admin/student surfaces (no WRITE type on activities).

**Rationale**: FR-016; established 029/031/032 pattern.
