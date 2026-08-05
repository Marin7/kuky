# Research: Teacher Feedback on Homework Submissions

## 1. Storage: reuse `homework_submissions.feedback` vs new column

**Decision**: Reuse existing nullable `feedback TEXT` column. No Flyway migration.

**Rationale**: Column already exists for Writing teacher comments. Graded exercises leave it `NULL` today. Storing exercise comments there avoids schema churn. Exercise API exposes a **plain string**; persistence wraps/unwraps a single FormattedText segment with no color/highlight/strike so `FormattedTextSegment.fromJson` on the learning overview keeps working for both formats.

**Alternatives considered**:
- New `teacher_comment TEXT` column — clearer separation, but unnecessary for v1 (YAGNI).
- Store raw plain string (non-JSON) in `feedback` — breaks existing `FormattedTextSegment.fromJson` on overview unless every reader becomes format-aware.

## 2. Endpoint: extend Writing `PUT …/feedback` vs new exercise endpoint

**Decision**: New `PUT /api/v1/admin/homework/submissions/{submissionId}/exercise-feedback` with `{ "feedback": "string" }`.

**Rationale**: Writing `saveFeedback` requires `SUBMITTED`, forbids already-`REVIEWED`, and sets `status = REVIEWED` + `reviewed_at`. Exercise feedback must allow repeat edits on `GRADED` without status change. A dedicated method/endpoint keeps both contracts simple and avoids branching a lifecycle-sensitive API.

**Alternatives considered**:
- Overload existing PUT with format detection — higher regression risk on Writing review.
- PATCH on exercise-result GET resource — less consistent with existing PUT feedback pattern.

## 3. List indicators without unread state

**Decision**: Expose `hasTeacherFeedback: boolean` on student `HomeworkItemResponse`, admin `AssigneeDto`, and `StudentProfileHomeworkDto`. Derive as “feedback present and non-empty after parse.” No read/unread tracking.

**Rationale**: Matches clarifications (student + teacher list indicators, no unread). Boolean keeps list payloads small and UI trivial (badge/label beside GRADED status).

**Alternatives considered**:
- Client derives from full `feedback` array — works for students if overview always includes segments, but admin assignee rows currently lack feedback payload; boolean is cheaper and explicit.
- Unread flags — explicitly out of scope.

## 4. Student visibility surface

**Decision**: Show plain-text teacher comment on the graded exercise result view (`ExerciseResponse.teacherFeedback` / `ExerciseResult`). List card only shows the indicator; opening the exercise shows the text.

**Rationale**: Students already open graded exercises for score review. Avoid duplicating long comments on the card. Overview may still carry FormattedText `feedback` for MANUAL; for EXERCISE the dedicated `teacherFeedback` string on the exercise payload is the reading surface.

## 5. Clearing feedback

**Decision**: Empty or whitespace-only save sets `feedback = NULL` (not empty JSON array). Indicators disappear.

**Rationale**: Spec treats clear as removal. `NULL` matches “no feedback” checks cleanly.

## 6. Validation & errors

**Decision**: Max 2000 characters (same as Writing visible-text limit). Over-length → `400 VALIDATION_ERROR`. Non-GRADED or non-EXERCISE submission → appropriate existing not-found / not-submitted style errors (`404`/`409` as fits current homework admin patterns). No email on save.

**Rationale**: Spec FR-006/008; align with `FormattedTextSegment.MAX_VISIBLE_LENGTH`.
