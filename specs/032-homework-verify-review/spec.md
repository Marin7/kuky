# Feature Specification: Admin Homework Verify Review

**Feature Branch**: `032-homework-verify-review`

**Created**: 2026-08-10

**Status**: Draft

**Input**: User description: "Enhance the admin panel to verify a homework. (1) If the student submits a homework and writes a single long line, the view from the teacher's panel requires horizontal scroll — wrap it. (2) I want to be able to 'edit' the student's response by coloring or doing a strikethrough, etc. (3) Add a small feedback box, limit to 500 chars."

## Clarifications

### Session 2026-08-10

- Q: Does the 500-character plain feedback box replace the whole-submission rich-text feedback editor for new MANUAL reviews, or sit alongside it? → A: Replace — new reviews use annotated answers + plain ≤500-char note only (legacy rich feedback stays readable on already-reviewed work)
- Q: After a MANUAL submission is marked reviewed, can the teacher change annotations or the short feedback note? → A: Yes — teacher can edit annotations and the short note anytime after review; the student always sees the latest saved version
- Q: On Writing answers that already have student formatting, what can the teacher do to those marks while annotating? → A: Full mark control — teacher may add, clear, or change any formatting marks on the answer text, but still cannot change the underlying words
- Q: When the teacher reopens a submission already reviewed with legacy rich-text whole-submission feedback, what happens? → A: Legacy stay frozen — those submissions remain view-only as stored; teacher cannot convert or re-edit them under the new annotate + plain-note model
- Q: Should MANUAL presentation activities get the same verify UX (wrap, in-place annotation, ≤500-char plain note, post-review edits for new-model reviews)? → A: Yes — full parity with MANUAL homework review

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher reads long student answers without horizontal scrolling (Priority: P1)

Paula opens a submitted manual homework in the admin review panel. The student typed (or pasted) a very long unbroken line with no spaces. Paula can read the full answer wrapped within the review dialog — she never needs to scroll sideways to see the text.

**Why this priority**: Review is blocked or painful today when answers overflow; wrapping is a small fix that immediately makes every review usable.

**Independent Test**: Submit a manual answer containing a long unbroken string (no spaces); open it in the teacher review panel; confirm the text wraps inside the panel and no horizontal scrollbar appears on the answer area.

**Acceptance Scenarios**:

1. **Given** a submitted manual answer that is one long unbroken line, **When** the teacher opens that submission in the admin review panel, **Then** the answer text wraps within the visible review area and does not require horizontal scrolling.
2. **Given** a multi-question manual submission where any per-question answer is a long unbroken line, **When** the teacher opens the review panel, **Then** each such answer wraps the same way (no horizontal scroll per answer block).
3. **Given** a Writing (WRITE) submission whose rich-text answer contains a long unbroken run of characters, **When** the teacher opens the review panel, **Then** that answer also wraps without horizontal scrolling.

---

### User Story 2 - Teacher annotates the student's answer in place (Priority: P1)

While verifying a submitted manual homework, Paula marks up the student's own answer text: she selects phrases and applies text color, background highlight, and/or strikethrough to call out mistakes and corrections. She saves the review; the annotated answer is what the student later sees as their reviewed work (same marks Paula applied).

**Why this priority**: In-place markup on the student's words is the core teaching gesture requested ("edit the student's response by coloring or strikethrough"); without it, verify remains a separate commentary wall rather than corrections on the work itself.

**Independent Test**: Open a submitted manual answer as the teacher, apply at least one color, one highlight, and one strikethrough to the student's text, save the review, reopen as teacher and as student, and confirm the same marks appear on the same words.

**Acceptance Scenarios**:

1. **Given** a student has submitted a Writing (WRITE) answer (with or without their own formatting), **When** the teacher opens it for review and applies, clears, or changes text color, background highlight, and/or strikethrough on portions of that answer, **Then** saving stores the resulting marks on the student's answer text and marks the submission reviewed.
2. **Given** a student has submitted per-question plain answers on a multi-question manual homework, **When** the teacher annotates one or more of those answers with color, highlight, and/or strikethrough and saves, **Then** each annotated answer preserves its marks and the submission is marked reviewed.
3. **Given** a submission has been reviewed with annotated answers, **When** the student opens that homework afterward, **Then** they see their answers with the teacher's colors, highlights, and strikethroughs intact.
4. **Given** the teacher is annotating, **When** they apply more than one mark type to the same selection (e.g. red text and strikethrough), **Then** those marks layer together on that selection.
5. **Given** a submission is still awaiting review, **When** the teacher opens it, **Then** they can annotate the student's answer(s) and save to mark it reviewed.
6. **Given** a submission was reviewed under the new model, **When** the teacher reopens it, changes annotations, and saves again, **Then** the updated marks replace the previous ones and the student sees the latest version on their next view (student answers remain non-editable by the student).
7. **Given** a submission was already reviewed with legacy rich-text whole-submission feedback, **When** the teacher reopens it, **Then** it remains view-only as stored (no annotation editing / no conversion to the new model).

---

### User Story 3 - Teacher leaves a short plain feedback note (Priority: P2)

Alongside annotating the student's answer, Paula can type a short plain-text comment (praise, summary, or next step) in a small feedback box capped at 500 characters. The student sees that note with their annotated answers after review.

**Why this priority**: Annotations mark specific words; a short overall note closes the review with clear guidance without needing a large rich-text feedback editor.

**Independent Test**: During review, enter a note under 500 characters, save, reopen as student and confirm the note is visible; try saving over 500 characters and confirm the save is rejected with a clear message.

**Acceptance Scenarios**:

1. **Given** the teacher is reviewing a submitted manual homework, **When** they enter plain-text feedback of at most 500 characters and save, **Then** the note is stored on that submission and shown again when the teacher or student reopens it.
2. **Given** the teacher tries to save feedback longer than 500 characters, **When** they save (first review or a later edit), **Then** the system rejects the save with a clear message and keeps the previous stored note unchanged (and does not mark reviewed if this was the first review save).
3. **Given** the teacher leaves the feedback box empty and has annotated (or not), **When** they save the review, **Then** the submission can still be marked reviewed — the short note is optional.
4. **Given** a reviewed submission has a short feedback note, **When** the student views the homework, **Then** they see the note together with their annotated answers.
5. **Given** a reviewed submission already has a short feedback note, **When** the teacher edits the note and saves, **Then** the updated note replaces the previous one and the student sees the latest text on their next view.

---

### Edge Cases

- What happens when a long line has no spaces? The review (and student post-review) view MUST still wrap or break the line so horizontal scrolling is not required.
- What happens if the teacher pastes styled text into the feedback box? The system MUST accept only plain text in the 500-character feedback box (no colors, highlights, or strikethrough there).
- What happens if the teacher pastes rich content while annotating the student answer? The system MUST keep only supported marks (text color, background highlight, strikethrough) and strip scripts, links, images, and other unsupported markup.
- What happens if the teacher tries to rewrite the student's words (insert/delete characters) while annotating? The system MUST preserve the student's original wording; annotation changes formatting marks only, not the underlying answer text.
- What happens to formatting the student already applied on a Writing answer? The teacher MAY add, clear, or change those marks freely during review (and later edits); the student's words still cannot be altered.
- What happens for auto-graded EXERCISE submissions? Unchanged — this feature applies to MANUAL homework (and MANUAL presentation activities) review only.
- What happens for submissions already reviewed with the previous rich-text whole-submission feedback? Those MUST remain viewable exactly as stored and MUST stay frozen (teacher cannot re-edit annotations or convert them to the new plain-note model). New reviews and later edits apply only to submissions first reviewed under the new model (or still awaiting that first new-model review).
- What if the teacher saves with neither annotations nor a feedback note? The system MUST still allow marking reviewed (empty optional note; annotations optional), consistent with completing a verify pass when no marks are needed.
- What if a multi-question submission has several answers and the teacher only annotates some? Unannotated answers MUST remain visible as plain (or original) text; annotated ones show their marks.
- What if the teacher revises annotations or the short note after the submission is already reviewed? The system MUST allow those edits anytime; each successful save becomes the new student-visible version. The student still cannot edit their own answers after submit/review rules already in place.
- What if the student is looking at a reviewed homework while the teacher saves a revision? The student sees the updates the next time they load or refresh that view (no live push required).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display every student answer in the admin homework review panel such that long lines wrap within the panel and the answer area does not require horizontal scrolling (including unbroken strings with no spaces).
- **FR-002**: System MUST apply the same no-horizontal-scroll wrapping when the student views their own answers after review.
- **FR-003**: System MUST allow the teacher, while reviewing a submitted MANUAL homework, to annotate the student's answer text in place using text color, background highlight, and strikethrough.
- **FR-003a**: For multi-question MANUAL submissions, System MUST allow annotation of each per-question answer independently.
- **FR-003b**: For Writing (WRITE) submissions, System MUST allow the teacher full control over formatting marks on the single student answer — add, clear, or change text color, background highlight, and strikethrough — while still forbidding changes to the underlying words (FR-005).
- **FR-004**: System MUST layer text color, background highlight, and strikethrough independently on the same selection when the teacher applies them together.
- **FR-005**: System MUST NOT allow the teacher to change the underlying characters of the student's answer during annotation — only formatting marks may be added or removed.
- **FR-006**: System MUST persist annotated answers with the submission so teacher and student see the same marks after save.
- **FR-007**: System MUST provide a small plain-text feedback box on MANUAL review, limited to 500 characters, optional when marking reviewed.
- **FR-008**: System MUST reject saves that exceed 500 characters in the feedback box with a clear validation message.
- **FR-009**: System MUST show the short plain feedback note to the student with their annotated answers once the submission is reviewed.
- **FR-010**: System MUST replace the previous whole-submission rich-text feedback editor for new MANUAL reviews with the annotated-answer + short plain feedback model.
- **FR-010a**: System MUST keep submissions already reviewed with legacy rich-text whole-submission feedback viewable as stored and frozen — the teacher MUST NOT convert or re-edit those under the new model.
- **FR-011**: System MUST offer a limited fixed palette of text and highlight colors (not an unrestricted color picker), consistent with existing homework formatting tools.
- **FR-012**: System MUST allow the teacher to edit annotated answers and the short feedback note anytime after a submission is marked reviewed **under the new model**; each successful save MUST become what the student sees on their next view. Students MUST still see answers and feedback as read-only (they cannot change annotations or the note). Legacy frozen reviews (FR-010a) are excluded.
- **FR-013**: System MUST NOT send an email when a submission is reviewed; the student discovers the result in their learning area, as today.
- **FR-014**: System MUST leave EXERCISE (auto-graded) homework feedback behavior unchanged.
- **FR-015**: System MUST apply the same review wrapping, in-place annotation, 500-character plain feedback, post-review edit rules (new-model only), and legacy-frozen behavior to MANUAL presentation activity submissions (full parity with MANUAL homework).

### Key Entities

- **Annotated Student Answer**: The student's submitted answer text (Writing or per-question) carrying formatting marks (text color, background highlight, strikethrough) as last saved by the teacher during review. Underlying wording stays the student's; marks are fully teacher-controlled after submit (including clearing marks the student had applied on Writing answers).
- **Short Teacher Feedback**: Optional plain-text note (max 500 characters) attached to a MANUAL submission (or MANUAL activity submission) at review time, shown to the student after review.
- **Manual Submission Review**: The admin verify step that moves a submitted MANUAL homework/activity to reviewed, capturing annotated answer(s) and optional short feedback.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In the admin review panel, 100% of tested student answers that previously forced horizontal scrolling (including long unbroken lines) are fully readable without horizontal scrolling.
- **SC-002**: A teacher can open a submitted MANUAL homework, apply at least one color or strikethrough to the student's answer, optionally add a short note ≤ 500 characters, and complete review in under 3 minutes.
- **SC-003**: After review, students can see their annotated answers and any short teacher note within 2 navigation steps from their learning area.
- **SC-004**: 100% of attempts to save feedback longer than 500 characters are blocked with a clear message (on first review and on later teacher edits).
- **SC-005**: For annotated reviews, 100% of applied color, highlight, and strikethrough marks on student answers are preserved identically when reopened by teacher or student, including after a teacher revises and re-saves a reviewed submission.
- **SC-006**: After a teacher edits annotations or the short note on an already-reviewed submission, the student sees the updated content within one refresh of that homework view.

## Assumptions

- Confirmed: scope is MANUAL homework review in the admin panel, plus MANUAL presentation activities with full parity; auto-graded EXERCISE feedback stays on the existing plain-feedback path.
- "Edit the student's response" means in-place annotation (color / highlight / strikethrough), not rewriting the student's words. On Writing answers, the teacher has full control over marks (add/clear/change), including marks the student originally applied.
- Confirmed: the small 500-character plain feedback box fully replaces the large rich-text whole-submission feedback editor for new MANUAL reviews; annotations carry the visual correction load.
- Confirmed: submissions already reviewed with legacy rich-text whole-submission feedback remain displayable as stored and stay frozen (no convert / no re-edit under the new model).
- Formatting tools reuse the same limited mark set already familiar from Writing homework (text color, background highlight, strikethrough) and the same fixed color palette.
- Feedback over the limit blocks completing/saving that review action (and later teacher edits on new-model reviews); an empty feedback box is allowed.
- Confirmed: after a new-model review, the teacher may revise annotations and the short note anytime; the student always sees the latest saved version (student side remains read-only).
- No email notification on review (consistent with current product behavior).
- Multi-question and Writing MANUAL answers are both in scope for wrapping and annotation.
