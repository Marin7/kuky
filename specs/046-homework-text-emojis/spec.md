# Feature Specification: Emojis in Homework Writing and Feedback

**Feature Branch**: `046-homework-text-emojis`

**Created**: 2026-08-16

**Status**: Draft

**Input**: User description: "Add emojis to rich text areas used for doing a homework for writing or when reviewing a homework and leaving a feedback"

## Clarifications

### Session 2026-08-16

- Q: Should teacher feedback become a formatted writing area (color, highlight, strikethrough, and emojis), or stay a comment that only gains emojis? → A: Feedback stays a comment: Paula can insert emojis there. Color, highlight, and strikethrough stay on the student’s written answer only.
- Q: How large should the in-app emoji set be? → A: Small fixed classroom set (~20–30 reactions: smiles, hearts, thumbs-up, clap, thinking, and similar). No categories or search.
- Q: Should emojis use the device’s built-in look, or custom pictures so everyone sees the same drawing? → A: Device emojis: inserted as normal characters. Appearance follows the student’s or teacher’s device.
- Q: Where does the in-app emoji control live for writing vs feedback? → A: Student writing: emoji control on the existing formatting bar. Teacher feedback: a simple emoji control next to the comment. Same classroom set in both.
- Q: Should the classroom set include skin-tone variants? → A: Default only (the usual yellow-style reactions). No skin-tone choices in the classroom set.

## User Scenarios & Testing *(mandatory)*

Students already write free-text homework answers in a formatted writing area, and the teacher already leaves a comment when reviewing a submission. Those areas support colors, highlights, and strikethrough, but there is no way to insert an emoji from the writing or review screen. Paula and her students use emojis for encouragement, tone, and quick reactions; they should be able to add them in the same place they already type.

### User Story 1 - Student inserts emojis while writing homework (Priority: P1)

A student is completing a Writing homework (or a free-text written question on a mixed homework). While composing their answer, they want to insert an emoji — a smile, a thinking face, a heart — without leaving the writing area or relying on a special keyboard. They pick an emoji from a control on the existing formatting bar (next to color, highlight, and strikethrough), it appears at the cursor, and they can keep typing. After they submit, the emoji stays in the answer the teacher later reads.

**Why this priority**: This is the student half of the request and the most common place written homework is composed. Without it, only the teacher side of the exchange would feel complete.

**Independent Test**: Open a Writing homework as a student, insert at least one emoji into the answer from the writing area, submit, then confirm the same emoji is visible when the teacher opens that submission.

**Acceptance Scenarios**:

1. **Given** a student is composing a written homework answer, **When** they insert an emoji from the formatting bar on that writing area, **Then** the emoji appears at the cursor (or replaces the current selection) and remains visible while they continue editing.
2. **Given** a student has included emojis in a draft answer, **When** they submit the homework, **Then** the stored answer keeps those emojis exactly as they appeared at submit time.
3. **Given** a student is composing a written answer, **When** they type or paste an emoji from their device keyboard, **Then** the writing area accepts it the same way as an emoji chosen in the writing area.
4. **Given** a student is answering a free-text question on a mixed homework (not only a standalone Writing homework), **When** they insert an emoji, **Then** it behaves the same as in a Writing homework answer.

---

### User Story 2 - Teacher inserts emojis when leaving homework feedback (Priority: P1)

The teacher is reviewing a submitted homework and writing a feedback comment for the student. That comment is still a comment, not a second formatted writing area: she can insert emojis (a thumbs-up, a smiley) but she does not color, highlight, or strike through the feedback text. After she saves, the student sees the same emojis in the comment.

**Why this priority**: This is the other half of the request. Feedback is where Paula’s tone and encouragement live; emojis there are as important as in the student’s writing.

**Independent Test**: Open a submitted homework as the teacher, insert at least one emoji into the feedback comment, save, then confirm the student sees that emoji when they view the reviewed homework.

**Acceptance Scenarios**:

1. **Given** the teacher is leaving feedback on a homework submission, **When** she inserts an emoji into the feedback, **Then** the emoji appears in the feedback text at the cursor (or replaces the current selection).
2. **Given** the teacher has included emojis in feedback, **When** she saves the review, **Then** the stored feedback keeps those emojis.
3. **Given** saved feedback contains emojis, **When** the student opens that homework afterward, **Then** they see the same emojis in the teacher’s feedback.
4. **Given** the teacher is reviewing either a Writing submission or an exercise submission that already has a feedback comment, **When** she inserts an emoji into that comment, **Then** it is accepted and shown later to the student.
5. **Given** the teacher is writing a feedback comment, **When** she looks at that comment field, **Then** she can insert emojis from a simple control next to the comment, and she does not get color, highlight, or strikethrough controls on the comment itself.

---

### User Story 3 - Emojis display correctly after submit and review (Priority: P2)

After a written answer or a feedback comment that contains emojis is saved, anyone who later opens that homework — the student looking at their history, the teacher reopening the review — sees those emojis rendered clearly alongside any existing formatting (color, highlight, strikethrough).

**Why this priority**: Insertion only has value if both sides see the same content later. This story closes the loop and can be verified as soon as P1 storage exists.

**Independent Test**: Submit a written answer that mixes emojis with colored or highlighted text, leave feedback that also includes emojis, then reopen both the student view and the teacher view and confirm the emojis and the formatting still match what was saved.

**Acceptance Scenarios**:

1. **Given** a submitted written answer contains emojis, **When** the teacher or the student views that answer, **Then** every inserted emoji is visible in the same positions as when it was written.
2. **Given** saved feedback contains emojis, **When** either party reopens the homework, **Then** the feedback shows those emojis unchanged.
3. **Given** a written answer uses both emojis and existing text formatting (color, highlight, or strikethrough), **When** it is viewed later, **Then** both the emojis and the formatting are preserved.

---

### Edge Cases

- What happens when inserting an emoji would exceed the existing maximum length for an answer or for feedback? The system MUST reject the extra characters, keep the previous valid content, and make it clear that the limit was reached. Emojis count toward the same visible-length limit as ordinary characters.
- What happens when a student or teacher pastes text that includes emojis copied from another app? The system MUST keep the emojis and MUST still strip unsupported styling, links, images, and scripts, matching the existing paste rules for formatted homework text.
- What happens when the teacher is only marking up the student’s existing answer (color, highlight, strikethrough) and not writing a new comment? Inserting an emoji MUST NOT add characters to the student’s original answer; emojis belong in the student’s own writing and in the teacher’s feedback comment.
- What happens for a homework that is already submitted or already reviewed (read-only)? Emoji insertion is unavailable there, consistent with those areas already being non-editable. Previously saved emojis still display.
- What happens if the writing or feedback field contains only emojis and no letters? That is valid content as long as it is within the length limit; it is not treated as empty.
- What happens on a device that has no emoji keyboard? The student or teacher can still insert an emoji from the writing or feedback area itself, by choosing from the small fixed classroom set.
- What happens if someone pastes or types an emoji that is not in the classroom set? The system MUST still keep and display it; the fixed set is what the in-app picker offers, not a whitelist that strips other emojis. This includes emojis with a different skin tone than the default classroom set.
- What happens for older answers and feedback that contain no emojis? They continue to display exactly as they do today.
- What happens if two people view the same saved emoji on different devices? Each sees their device’s drawing of that emoji. The stored character is the same; the pictures do not have to match pixel-for-pixel.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST let a student insert an emoji into a written homework answer from the same writing area they already use to compose that answer, without leaving the page. The classroom emoji control MUST sit on the existing formatting bar (color, highlight, strikethrough) for that writing area.
- **FR-002**: System MUST let a teacher insert an emoji into the feedback comment from the same review screen they already use to leave homework feedback, without leaving that screen. The feedback comment MUST remain a comment: it MUST NOT gain text color, background highlight, or strikethrough controls. A simple emoji control MUST sit next to the comment and offer the same classroom set as the student’s formatting bar.
- **FR-003**: System MUST place an inserted emoji at the current cursor position, or replace the current text selection when one exists.
- **FR-004**: System MUST accept emojis entered from the device’s own emoji keyboard or by paste, in addition to insertion from the writing or feedback area.
- **FR-005**: System MUST persist emojis in submitted written answers and in saved teacher feedback as ordinary characters so they survive reload and are visible to both the student and the teacher. The same emoji MAY look slightly different on different devices; the character itself MUST be unchanged.
- **FR-006**: System MUST render saved emojis in every place that already shows that written answer or that feedback (student learning view, teacher review, student history after review).
- **FR-007**: System MUST allow existing text formatting (text color, background highlight, strikethrough) to be used on the same answer that contains emojis; formatting MUST NOT strip emojis, and emojis MUST NOT disable formatting.
- **FR-008**: System MUST apply the same existing maximum length to content that includes emojis, counting emojis as visible content, and MUST NOT store content that exceeds that limit.
- **FR-009**: System MUST NOT allow inserting emojis into the student’s original answer while the teacher is only applying formatting to that answer; the student’s words stay unchanged except for the formatting already permitted today.
- **FR-010**: System MUST offer a single small fixed classroom set of about 20–30 emojis (reactions such as smiles, hearts, thumbs-up, clap, and thinking). The set MUST be the same for students and the teacher, MUST NOT be grouped into categories, MUST NOT include search, and MUST NOT include skin-tone variants — default appearance only.
- **FR-011**: System MUST keep emoji insertion unavailable once a written answer is submitted or once feedback is in a read-only reviewed state, matching existing edit locks.
- **FR-012**: System MUST NOT extend emoji insertion to creating homework content, quizzes, presentations, or other text fields outside written homework answers and homework review feedback.

### Key Entities

- **Written Homework Answer**: The student’s free-text response to a Writing homework or to a free-text question. May include emojis together with the formatting already supported today.
- **Teacher Feedback Comment**: The comment the teacher attaches when reviewing a homework submission. May include emojis. It is not a formatted writing area: no text color, highlight, or strikethrough on the comment. Distinct from formatting applied onto the student’s original answer.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A student composing a written homework answer can insert an emoji from the writing area in under 15 seconds without leaving that area.
- **SC-002**: A teacher leaving homework feedback can insert an emoji from the review screen in under 15 seconds without leaving that screen.
- **SC-003**: 100% of emojis present at submit or save time reappear in the same positions when the other party later views that answer or feedback. Matching means the same emoji characters, not identical drawings across different devices.
- **SC-004**: Students who already format written answers with color, highlight, or strikethrough can still apply those formats after this change, including on answers that also contain emojis.
- **SC-005**: A student or teacher using a device with no dedicated emoji keyboard can still complete the insert-emoji task on the first attempt from the writing or feedback area.

## Assumptions

- “Doing a homework for writing” means any student-composed written answer that already uses the formatted writing area: standalone Writing homework and free-text questions on mixed homework. Structured exercise answers (multiple choice, blanks, matching, and similar) are unchanged.
- “Leaving a feedback” means the teacher’s review comment on a homework submission, including Writing and exercise submissions that already have a feedback comment. It does not mean rewriting the student’s answer. The comment can include emojis; it does not become a formatted writing area with color, highlight, or strikethrough.
- Emoji insertion is in addition to the existing formatting tools on the student’s written answer; this feature does not remove or replace color, highlight, or strikethrough there, and does not add those tools to the feedback comment.
- The in-app set is a short fixed list of about 20–30 classroom reactions in their default appearance only (no skin-tone picker or extra variants). It is not a categorized or searchable catalog. Device keyboards and paste may still enter emojis that are not on that list, including other skin tones; those MUST still be stored and displayed.
- Existing length limits for written answers and feedback continue to apply; there is no separate emoji quota.
- Presentation activities, quiz attempts, homework instructions, and other authoring fields are out of scope.
- Native device emoji input and paste remain valid ways to enter emojis; the in-area insertion option exists so users are not dependent on a special keyboard. Inserted emojis are the device’s own characters, not custom pictures or a downloaded pack.
- The student’s writing area reuses its existing formatting bar for the classroom emoji control. The teacher’s feedback comment does not gain a formatting bar; it only gets a simple emoji control next to the comment. Both offer the same classroom set.
- Older submissions without emojis need no migration; they display as they do today.
