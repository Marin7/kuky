# Feature Specification: Teacher Feedback on Homework Submissions

**Feature Branch**: `025-homework-feedback`

**Created**: 2026-08-05

**Status**: Draft

**Input**: User description: "When a teacher views a homework submission, add the option to leave a feedback, just a simple text"

## Clarifications

### Session 2026-08-05

- Q: Which homework submissions get the new plain-text teacher feedback? → A: Auto-graded exercise submissions only (Writing feedback stays as today)
- Q: How do students discover that teacher feedback exists? → A: Homework list shows an indicator when teacher feedback exists (no unread/new state)
- Q: What is the maximum length for plain-text teacher feedback on exercises? → A: 2000 characters (same as Writing homework feedback)
- Q: Should the teacher see a list indicator when exercise feedback already exists? → A: Yes — admin/student homework lists show an indicator when exercise feedback already exists

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher leaves plain-text feedback on a graded exercise (Priority: P1)

A student has completed an auto-graded exercise homework. The teacher opens that submission to review how the student did, and optionally writes a short plain-text comment (praise, correction note, or encouragement). After saving, the feedback stays attached to that submission so the teacher can see it again later.

**Why this priority**: Auto-graded exercises already show scores and per-question results to the teacher, but there is no way to leave a personal comment. Writing (free-text) homework already supports teacher feedback; this story closes that gap for exercise submissions with the simplest useful form — plain text.

**Independent Test**: Open a graded exercise submission as the teacher, enter a short text comment, save it, reopen the same submission, and confirm the comment is still there.

**Acceptance Scenarios**:

1. **Given** a student has a graded exercise submission, **When** the teacher opens it and enters plain-text feedback then saves, **Then** the feedback is stored on that submission and shown again when the teacher reopens it.
2. **Given** a graded exercise submission with no feedback yet, **When** the teacher opens it, **Then** they see an optional text field for feedback and can close without saving without being forced to leave a comment.
3. **Given** a graded exercise submission that already has feedback, **When** the teacher edits the text and saves again, **Then** the updated feedback replaces the previous text.
4. **Given** a graded exercise submission that already has feedback, **When** the teacher looks at that student's homework list (or equivalent admin list of the student's exercises), **Then** the item shows a clear indicator that feedback already exists.
5. **Given** a graded exercise submission with no feedback, **When** the teacher looks at that list, **Then** the item does not show a feedback-present indicator.

---

### User Story 2 - Student sees the teacher's feedback (Priority: P2)

After the teacher has left feedback on an exercise submission, the student sees an indicator on that item in their homework list. Opening the homework shows the teacher's plain-text comment alongside the automatic score and answer review they already get today.

**Why this priority**: Feedback only delivers learning value if the student can read it. This closes the loop without changing how auto-grading already works.

**Independent Test**: As a student, open a graded exercise that has teacher feedback and confirm the plain-text comment is visible next to the existing result view.

**Acceptance Scenarios**:

1. **Given** the teacher has saved feedback on a graded exercise, **When** the student views that homework result, **Then** they see the teacher's plain-text feedback together with their score and per-question review.
2. **Given** a graded exercise with no teacher feedback, **When** the student views the result, **Then** the result view looks as it does today (no empty feedback section required).
3. **Given** the teacher has saved feedback on a graded exercise, **When** the student looks at their homework list, **Then** that item shows a clear indicator that teacher feedback is available.
4. **Given** a graded exercise with no teacher feedback, **When** the student looks at their homework list, **Then** that item does not show a feedback indicator.

---

### Edge Cases

- What happens if the teacher tries to save feedback that exceeds 2000 characters? The system MUST reject the save with a clear message and keep the previous feedback unchanged.
- What happens if the teacher clears all text and saves? The system MUST treat that as removing feedback (submission has no teacher comment), not as an error, and any feedback-present indicators on student and teacher homework lists for that item MUST disappear.
- What happens for Writing (manual free-text) homework? Existing teacher feedback for Writing remains unchanged; this feature does not replace or simplify that flow.
- What happens if feedback is saved while the student is already looking at the result? The student sees the feedback the next time they load or refresh that result view (no live push required).
- What happens for submissions that are not yet graded or not yet submitted? Feedback applies only to graded exercise submissions the teacher can already view.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow a teacher, while viewing a graded exercise homework submission, to enter optional plain-text feedback and save it against that submission.
- **FR-002**: System MUST persist teacher feedback so it is available whenever the teacher reopens the same submission.
- **FR-003**: System MUST allow the teacher to update or clear previously saved feedback on a graded exercise submission.
- **FR-004**: System MUST show saved teacher feedback to the student when they view that graded exercise result.
- **FR-004a**: System MUST show a visible indicator on the student's homework list for each graded exercise that has teacher feedback; items without feedback MUST NOT show that indicator. The indicator does not track read/unread state.
- **FR-004b**: System MUST show a visible indicator on the teacher's admin view of a student's homework (or equivalent exercise list) for each graded exercise that already has teacher feedback; items without feedback MUST NOT show that indicator.
- **FR-005**: System MUST NOT require feedback in order for an exercise to remain graded or for the student to see their automatic score.
- **FR-006**: System MUST enforce a maximum length of 2000 characters on teacher feedback text and reject saves that exceed it.
- **FR-007**: System MUST accept only plain text for this feedback (no rich formatting tools such as colors, highlights, or strikethrough).
- **FR-008**: System MUST NOT send an email when feedback is saved; the student discovers it by visiting their learning area, consistent with existing homework review behavior.
- **FR-009**: System MUST leave the existing Writing (manual) homework feedback flow unchanged.

### Key Entities

- **Exercise Submission**: A student's completed auto-graded homework attempt, already carrying score and per-question results. Extended with optional teacher plain-text feedback.
- **Teacher Feedback**: A simple text comment the teacher attaches to a graded exercise submission. Visible to that student; optional and editable by the teacher.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can open a graded exercise submission, leave a short plain-text comment, and save it in under 1 minute.
- **SC-002**: 100% of saved teacher feedback on exercise submissions is visible to the corresponding student on their next visit to that homework result, and those items show a feedback indicator on the homework list.
- **SC-003**: Teachers can update or remove feedback on a previously commented exercise without affecting the automatic score or graded status.
- **SC-004**: Students with no teacher feedback continue to see their graded result exactly as before (no regression in the existing result experience).

## Assumptions

- Scope is auto-graded exercise submissions only (confirmed). Writing (manual) homework already has a teacher feedback path and is out of scope for this change.
- Feedback is optional commentary, not a grade override and not a status transition — the exercise stays graded as today.
- Feedback is plain text only, matching the request for "just a simple text" and avoiding another rich-text editor for this path.
- The same admin surfaces the teacher already uses to view exercise results are where feedback is entered (no new review queue required for exercises). Those lists also show which graded exercises already have feedback.
- A maximum of 2000 characters applies to this feedback, matching Writing homework feedback.
- No email or push notification when feedback is saved, matching Writing homework review behavior.
- Only the teacher (admin) can write feedback; students can read it but not reply in this version.
