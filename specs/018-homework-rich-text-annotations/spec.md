# Feature Specification: Rich Text Formatting for Writing Homework Feedback

**Feature Branch**: `018-homework-rich-text-annotations`

**Created**: 2026-07-02

**Status**: Draft

**Input**: User description: "When addressing a "Writing" kind of homework which requires actual input from the teacher, I want the teacher to be able to color the text or the background, maybe strikethroughs, etc. Same concept for the student doing the homework as well, if possible"

## Clarifications

### Session 2026-07-02

- Q: How should the teacher find Writing submissions that need her feedback? → A: Both — a cross-student queue lists all submissions awaiting feedback, and the same indicator also appears on each student's profile page.
- Q: Can multiple formatting types be combined on the same piece of text (e.g. colored AND struck-through)? → A: Yes — text color, background highlight, and strikethrough are independent attributes that can be freely layered on the same selection.
- Q: When a teacher finishes reviewing a Writing submission, should the student be notified by email, or only see it next time they check the app? → A: In-app only — no email is sent; the student sees the reviewed feedback next time they visit their learning area.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher gives formatted feedback on a Writing submission (Priority: P1)

A student has submitted their answer to a Writing (free-text) homework assignment. The teacher opens the submission, writes feedback, and uses formatting tools to color specific words, highlight phrases with a background color, and strike through incorrect text to visually call out mistakes and corrections. The teacher saves the feedback, which marks the submission as reviewed. The student can then see the feedback exactly as formatted.

**Why this priority**: This is the core of the request. Today there is no way for the teacher to leave any feedback on a Writing submission at all — this story delivers the first working version of that capability, with formatting as the tool that makes the feedback pedagogically useful (distinguishing corrections, praise, and suggestions at a glance instead of a wall of plain text).

**Independent Test**: Can be fully tested by having a teacher open a submitted Writing homework, apply at least one color, one highlight, and one strikethrough to feedback text, save it, and confirming the student's view renders the same formatting.

**Acceptance Scenarios**:

1. **Given** a student has submitted a Writing homework answer, **When** the teacher opens it and writes feedback with colored text, a highlighted phrase, and a struck-through word, **Then** saving the feedback stores all three formatting types and marks the submission as reviewed.
2. **Given** a submission has been reviewed with formatted feedback, **When** the student views their submission afterward, **Then** the feedback displays with the same colors, highlights, and strikethroughs the teacher applied.
3. **Given** a submission is still awaiting review, **When** the teacher looks for work to review, **Then** the teacher can find that submission both from a queue listing all students' submissions awaiting feedback and from an indicator on that student's own profile page.

---

### User Story 2 - Student formats their own answer while writing it (Priority: P2)

While composing their answer to a Writing homework assignment, a student wants to color, highlight, or strike through parts of their own text — for example to mark a self-correction or emphasize a key phrase — using the same simple formatting tools available to the teacher.

**Why this priority**: This is the secondary, "nice to have" half of the request ("same concept for the student"). It extends the same toolset to the student's side of the exchange but is not required for the teacher-feedback loop to deliver value on its own.

**Independent Test**: Can be fully tested by having a student open a Writing homework, apply color/highlight/strikethrough formatting to portions of their draft answer, submit it, and confirming the formatting is preserved in what gets stored and later displayed.

**Acceptance Scenarios**:

1. **Given** a student is composing an answer to a Writing homework assignment, **When** they select text and apply a color, a highlight, or a strikethrough, **Then** the formatting is visibly applied in the editor before submission.
2. **Given** a student has formatted parts of their draft answer, **When** they submit the homework, **Then** the stored submission preserves the formatting exactly as it appeared when submitted.

---

### User Story 3 - Student reviews their formatted submission and feedback together (Priority: P3)

After a Writing homework has been reviewed, the student revisits their homework history and sees both their own originally formatted answer and the teacher's formatted feedback side by side, so they can understand exactly what was corrected and why.

**Why this priority**: This closes the loop and makes the value of Stories 1 and 2 visible after the fact, but it depends on both being implemented first.

**Independent Test**: Can be fully tested by having a student navigate to their homework history after a submission has been reviewed and confirming both their original formatted answer and the teacher's formatted feedback render correctly together.

**Acceptance Scenarios**:

1. **Given** a Writing submission has been reviewed, **When** the student opens it from their homework history, **Then** they see their original formatted answer and the teacher's formatted feedback together, with all formatting intact.

---

### Edge Cases

- What happens when a student's formatted answer or a teacher's formatted feedback exceeds the maximum allowed text length? The system MUST count only the visible text toward the limit, not the formatting itself.
- What happens when a teacher or student pastes text copied from another application (e.g. Word, Google Docs) that carries its own rich formatting? The system MUST strip any styling other than the supported color, background color, and strikethrough, and must not carry over embedded links, images, scripts, or other markup.
- What happens if a teacher tries to mark a submission as reviewed without entering any feedback text? The system MUST require at least some feedback content before a submission can be marked reviewed.
- What happens once a submission has been marked reviewed — can the student edit their original answer, or the teacher edit their feedback? Both become read-only after review, consistent with how reviewed/graded homework already behaves elsewhere in the product.
- What happens if a student tries to edit their answer after it has already been submitted (before review)? This MUST continue to be blocked, as it is today.
- What happens for homework types other than Writing (e.g. auto-graded exercises)? This feature does not apply — formatting tools are only available for Writing (free-text) homework answers and feedback.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow a teacher, while reviewing a submitted Writing homework answer, to write feedback text and apply text color, background color (highlight), and strikethrough formatting to any portion of it.
- **FR-002**: System MUST allow the teacher to save that formatted feedback against the submission it belongs to, and record that the submission has been reviewed.
- **FR-003**: System MUST render a teacher's saved feedback back to both the teacher and the student with the original colors, highlights, and strikethroughs preserved exactly.
- **FR-004**: System MUST allow a student, while composing their answer to a Writing homework assignment (before submitting it), to apply text color, background color, and strikethrough formatting to any portion of their own answer text.
- **FR-005**: System MUST render a student's formatted answer back to both the student and the reviewing teacher with the formatting preserved exactly.
- **FR-006**: System MUST offer a limited, consistent set of preset colors for text and background/highlight formatting rather than unrestricted free-form color choice.
- **FR-006a**: System MUST allow text color, background highlight, and strikethrough to be applied independently and in combination to the same piece of text (e.g. a word that is both colored and struck through).
- **FR-007**: System MUST prevent a student from editing or reformatting their answer once it has been submitted, and MUST prevent a teacher from editing their feedback once the submission has been marked reviewed.
- **FR-008**: System MUST sanitize all formatted content submitted by either a teacher or a student so that only the supported formatting (text color, background color, strikethrough) can be stored — no scripts, links, images, or other markup are permitted.
- **FR-009**: System MUST enforce a maximum length on both the student's answer and the teacher's feedback, measured by visible text content rather than any underlying formatting markup.
- **FR-010**: System MUST give the teacher a way to identify Writing homework submissions that are awaiting feedback, both through a single queue listing every such submission across all students, and through an indicator on each individual student's profile page.
- **FR-011**: System MUST let a student, once their submission has been reviewed, view their own original formatted answer together with the teacher's formatted feedback from their homework history.
- **FR-012**: System MUST NOT send an email notification when a submission is reviewed; the student learns of the feedback by visiting their learning area, consistent with how other homework statuses are surfaced today.

### Key Entities

- **Writing Homework Submission**: A student's answer to a free-text ("Writing") homework assignment. Now carries formatted text (supporting color, background color, and strikethrough) instead of plain text, alongside its existing status (awaiting submission, submitted and awaiting feedback, reviewed).
- **Teacher Feedback**: The formatted commentary a teacher attaches to a submission when reviewing it. Becomes visible to the student once saved, and read-only for the teacher thereafter.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can open a submitted Writing homework, write feedback that includes at least one color, highlight, or strikethrough, and mark it reviewed in under 3 minutes.
- **SC-002**: 100% of color, background-color, and strikethrough formatting applied by a teacher or student is preserved unchanged whenever that content is reloaded or viewed by the other party.
- **SC-003**: Students can locate and view their teacher's feedback on a reviewed Writing homework submission within 2 navigation steps from their learning area.
- **SC-004**: No unsupported markup (such as scripts, links, or arbitrary HTML) ever survives into stored feedback or answer content, verified through testing of pasted and hand-crafted input.

## Assumptions

- Teacher feedback is separate formatted commentary attached to a submission, not an in-place edit of the student's original answer text. The student's original formatted answer always remains unmodified and is shown alongside the teacher's feedback, rather than the teacher marking up the original text directly.
- Formatting is limited to a small fixed palette of preset text and background/highlight colors, not a full custom color picker, to keep the toolset simple and visually consistent across submissions.
- The existing maximum length for a student's Writing answer continues to apply, counted on visible text rather than formatting markup; the same limit is assumed for teacher feedback.
- Once a submission is marked reviewed, both the student's original answer and the teacher's feedback become permanently read-only, matching how other reviewed/graded homework already behaves.
- This feature is scoped to the Writing (free-text) homework submission-and-feedback exchange only. It does not extend to homework instructions, presentation content, or auto-graded exercise questions.
- The teacher will access submissions awaiting feedback through the existing admin-facing homework/student areas, extended to support this review step, since no such review workflow exists today.
