# Research: Student Dots for Homework Corrections and Feedback

**Feature**: `047-homework-grade-dots` | **Date**: 2026-08-16

## 1. Storage: new table vs columns on submissions

**Decision**: Add `student_grade_seen_at` and `student_feedback_seen_at` (`TIMESTAMPTZ NULL`) on existing `homework_submissions`. Unseen review news = either column `IS NULL`. No notifications table. Do **not** reuse `homework_targets.student_seen_at` (that flag is assignment-open, FR-018).

**Rationale**: 041 already stores unseen as null timestamps on the row that is the news. A homework review is one submission. Two columns are required so clearing all comments/annotations can dismiss feedback news without dismissing an unseen teacher finalize (FR-015). Delete/unassign already removes or orphans the submission so the icon goes away (FR-014).

**Alternatives considered**:
- Reuse `homework_targets.student_seen_at` — opening a homework already marks that seen; would mix “assigned” with “reviewed” and break FR-018.
- Single `student_review_seen_at` — cannot implement FR-015 (clear feedback vs unseen grade) without extra event-kind state.
- Generic `notifications` table — 041 rejected this; still YAGNI.

## 2. Existing rows and new submits (FR-016, FR-004)

**Decision**: Flyway `V25` backfills both new columns to `NOW()` for every existing `homework_submissions` row. Student submit (including auto-grade → `GRADED`) sets both to `NOW()` (not NULL). Teacher finalize or a later visible score/comment/annotation change sets the matching column(s) to NULL.

**Rationale**: Spec: no historical icons; auto-score on submit is not student news. Same backfill pattern as V22.

**Alternatives considered**: Leave new columns NULL on old `GRADED` rows — every past grade would light up Mi aprendizaje.

## 3. When to NULL which column

**Decision**:

| Teacher action | Effect |
|----------------|--------|
| Progress save, homework still awaiting | No change |
| Finalize (first time student can see teacher %) | `student_grade_seen_at = NULL`; also NULL `student_feedback_seen_at` if that save (or already stored) has a written comment or annotations |
| Later visible score change on already-`GRADED` | `student_grade_seen_at = NULL` |
| Written comment and/or annotations change on already-visible homework (`GRADED`, including auto-scored) | `student_feedback_seen_at = NULL` |
| No-op save (nothing student-visible changed) | No change |
| Clear all comments and annotations | `student_feedback_seen_at = NOW()` (grade column unchanged) |
| Activity / quiz review saves | No change (FR-017) |

Compare before/after on percents, feedback text, and annotated formatted answers so a mere Save does not re-notify.

**Rationale**: Clarifications: notify only when the student can see the marks; annotations alone count; one UI indicator per homework (badge/list OR the two nulls).

**Alternatives considered**: NULL both columns on every teacher save — noisy no-ops and conflates FR-015.

## 4. Mark-seen (student)

**Decision**: Mark **both** review columns `NOW()` (idempotent, only where NULL) when the student opens a **`GRADED` result**:
- Side effect of `GET /learning/homework/{assignmentId}` if that submission is `GRADED` (exercise / mixed / listening / reading result).
- Side effect of existing `POST /learning/homework/{assignmentId}/seen` if that submission is `GRADED` (WRITE / manual inline and write page already call this).

Do **not** mark review seen on: `GET /learning`, `POST /learning/units/{id}/seen`, take/pending GET, or `SUBMITTED` awaiting-teacher GET (opening awaiting work must not consume a future finalize).

Assignment mark-seen (`homework_targets.student_seen_at`) stays as today.

**Rationale**: Spec: only opening the result counts; unit open still only clears assignment news. Reuse the two student entry points that already mean “this homework is open.”

**Alternatives considered**: New `POST .../review-seen` — extra contract; the GRADED-gated side effect on existing seen/GET is smaller.

## 5. Badge and list flags

**Decision**: Keep `GET /notifications/badges` shape. `learning` becomes true if the student has any unseen **unit, quiz, homework-target assignment, or homework review** (either new column NULL). Student homework DTO `unseen` becomes true if assignment-unseen **or** review-unseen (one dot). Unit card on Mi aprendizaje already ORs child homework `unseen` in `unitGroups.ts`; keep that so a review-only unit still shows a card dot after the unit assignment is seen.

**Rationale**: Spec: same Mi aprendizaje icon; no new nav field. One homework dot for either kind of student news.

**Alternatives considered**: Separate `reviewUnseen` JSON field — clearer, but a second marker on the same row violates “one set of icons” (FR-009) unless the UI ORs them anyway.

## 6. Teacher hooks (homework only)

**Decision**: Call a small `NotificationService` helper from `HomeworkAdminService.saveFeedback` and `saveExerciseFeedback` only. Do **not** hook `ActivityAdminService` or quiz review.

**Rationale**: FR-017. Presentation activities share grading code but are out of scope.

## 7. Real-time and UI

**Decision**: Same as 041 — refresh badges on next load or client navigation. Reuse `NotificationDot`. No new i18n keys required if the existing item/nav labels already mean “unseen news”; add a review-specific aria label only if the same homework can show assignment vs review as distinct accessible names (optional; presence-only is enough).

**Rationale**: Spec: no live pop-in, no inbox.

## 8. Detecting annotations vs comments

**Decision**: “Written comment” = submission `feedback` / exercise feedback text (plain or formatted) non-empty or changed. “Annotations” = teacher-updated formatted answer/response segments that differ from the previously stored student-visible text. Either change on already-visible work NULLs `student_feedback_seen_at`.

**Rationale**: Clarification B: any student-visible mark, including annotations alone.
