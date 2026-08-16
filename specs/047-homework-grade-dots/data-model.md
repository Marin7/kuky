# Data Model: Student Dots for Homework Corrections and Feedback

**Feature**: `047-homework-grade-dots` | **Date**: 2026-08-16

Flyway **`V25__homework_student_review_unseen.sql`**. No new tables.

## Entities

### Homework submission (existing)

`homework_submissions` adds:

| Field | Type | Notes |
|-------|------|--------|
| `student_grade_seen_at` | timestamptz NULL | NULL after a teacher **finalize** (or a later visible percent change) until the student opens that `GRADED` result. Backfill `NOW()` for all existing rows. Student submit sets `NOW()` (auto-grade is not news). |
| `student_feedback_seen_at` | timestamptz NULL | NULL after a student-visible written comment and/or annotation change on **already-visible** work (`GRADED`), or when finalize includes comments/annotations. Set to `NOW()` when the student opens the `GRADED` result, or when the teacher clears all comments and annotations. Backfill `NOW()`; submit sets `NOW()`. |

Review unseen (one homework, one student):  
`student_grade_seen_at IS NULL OR student_feedback_seen_at IS NULL`.

Partial index: `(user_id)` WHERE either column IS NULL (learning badge + list).

`homework_targets.student_seen_at`, `unit_assignments.student_seen_at`, and `teacher_seen_at` are unchanged.

## State

```text
Student submit (any outcome, including auto GRADED)
  both columns NOW()     →  not review-unseen

Teacher progress save (still awaiting)
  columns unchanged

Teacher finalize
  student_grade_seen_at NULL
  student_feedback_seen_at NULL if comment and/or annotations are present
                     else leave feedback column as-is (already NOW())

Teacher later score change on GRADED
  student_grade_seen_at NULL

Teacher later comment/annotation change on GRADED
  student_feedback_seen_at NULL

Teacher clears all comments and annotations
  student_feedback_seen_at NOW()     →  grade unseen (if any) remains

Student opens GRADED result
  both columns NOW() where NULL

Unassign / delete homework
  target/submission gone  →  no icon
```

- Opening Mi aprendizaje or the unit does not transition review columns.
- Opening a `SUBMITTED` awaiting result does not transition review columns.
- Opening an already-seen `GRADED` result is idempotent.
- Assignment seen (`POST .../homework/{id}/seen`, unit seen POST) never writes these two columns unless the submission is `GRADED` (then the homework seen POST also marks review seen — WRITE/manual result).

## Validation

- Review unseen only after teacher-visible news, never after student submit alone.
- Awaiting homework never has review-unseen columns NULL from progress saves.
- Badge `learning` = any of: unseen unit assignment, unseen quiz assignment, unseen homework target assignment, **or** unseen homework review for this user.
- Student homework `unseen` DTO = assignment-unseen **OR** review-unseen.
- Teacher badges (`panel` / `homework` / `quiz`) unchanged.
- `USER` / non-owner students: no review icons.
- Presentation-activity and quiz submissions: columns unused / never NULLed by those admin saves.

## DTO flags

| DTO | Field | True when |
|-----|--------|-----------|
| `HomeworkItemResponse` | `unseen` | `homework_targets.student_seen_at IS NULL` **or** this user’s submission has a NULL review column |
| `UnitRef` | `unseen` | Unchanged meaning (unit assignment). Card-level dot still ORs child homework `unseen` on the client (`unitGroups`). |
| `BadgeSummary.learning` | `learning` | Existing assignment unseen **or** any review-unseen submission for this user |

No new badge JSON fields. No teacher DTO changes.
