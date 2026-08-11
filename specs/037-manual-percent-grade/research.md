# Research: Percentage Grades for Manual Answers

**Feature**: `037-manual-percent-grade` | **Date**: 2026-08-11

## R1 — How to store teacher percentage

**Decision**: Add nullable `teacher_score_percent INT` (0–100) on `homework_answers` / `activity_answers`. When set, also set `score = teacher_score_percent / 100.0` (scale 3, HALF_UP). Drop `teacher_validation` after backfill. For WRITE (no answer rows), store nullable `teacher_score_percent` on `homework_submissions` (activities have no WRITE path).

**Rationale**: Distinguishes “unscored” (`NULL`) from “scored 0%” (`0`). Reuses existing `score` + `scorePercentFromScores` mean×100 for overall %. Matches equal-weight model already in `HomeworkCompositionSupport`.

**Alternatives considered**:
- Reuse only `score` with NULL = unscored — viable but WRITE has no answer row; still need a submission-level field; API clarity better with explicit percent.
- Keep `teacher_validation` alongside percent — rejected (dual UI / dual source of truth; FR-012).

## R2 — Partial save vs finalize

**Decision**: Extend existing `PUT .../submissions/{id}/feedback` with boolean `finalize` (default `false` if omitted → treat as progress save when any scores present but incomplete; require explicit `finalize: true` to grade).

| `finalize` | Requirements | Status |
|------------|--------------|--------|
| `false` | May include subset of `teacherScorePercent` values; validate each provided value ∈ 0–100 | Stay `SUBMITTED`; persist provided percents/annotations/note; do **not** set final `score_percent` from manual blend (leave null / unchanged provisional) |
| `true` | Every FREE_TEXT (or WRITE) must have `teacherScorePercent`; reject if any missing | → `GRADED`; compute overall `score_percent`; set `reviewed_at` / `review_model` as today |

**Rationale**: One endpoint (YAGNI); matches today’s feedback PUT; clear student-visibility rule while `SUBMITTED`.

**Alternatives considered**:
- Separate `/draft` endpoint — extra surface, rejected.
- All-or-nothing only — rejected by clarification (partial save allowed).

## R3 — Student visibility while awaiting

**Decision**: On student learning GETs, if status is `SUBMITTED` (awaiting teacher): omit `teacherScorePercent` and omit/zero-out manual `score` contribution in response; keep `scorePercent` null; allow `provisionalScorePercent` (auto-only) for MIXED as today. After `GRADED`, expose percents and overall.

**Rationale**: Clarification Q5 Option A; teachers can still see drafts on admin GET.

## R4 — Fully-correct count

**Decision**: Count answers with contribution `1.0` (auto correct or `teacher_score_percent = 100`). Partial manual scores do not increment.

**Rationale**: Clarification (validated → 100% semantics).

## R5 — Migration of historical decisions

**Decision**: Flyway backfill: `VALIDATED` → `teacher_score_percent = 100`, `INVALIDATED` → `0`, set `score` accordingly; then drop `teacher_validation` column and CHECK. Existing `score_percent` on graded submissions already matches binary mean — leave unchanged.

**Rationale**: FR-011; no teacher re-grade required.

## R6 — Homework vs activity parity

**Decision**: Same API shape and service rules in `HomeworkAdminService` and `ActivityAdminService` (mirror pattern already used for validate/invalidate). Frontend mirrors `HomeworkReviewDialog` ↔ `ActivityReviewDialog`.

**Rationale**: Spec US4 / FR-008.

## R7 — Rounding

**Decision**: Keep `HomeworkCompositionSupport.scorePercent` → `(int) Math.round(mean * 100)` (half-up for positive means).

**Rationale**: Already matches assumption; no behavior change for auto-only.

## R8 — Frontend control

**Decision**: Replace Validate/Invalidate buttons with a numeric percent input (0–100) per manual answer (and WRITE). “Save progress” vs “Finalize / Save grade” actions map to `finalize: false|true`. Show percent badges on student views when graded; remove validated/invalidated badge copy.

**Rationale**: Spec FR-001/012; simplest Shadcn Input/number control; no new libraries.
