# Feature Specification: Student Common Errors

**Feature Branch**: `052-student-common-errors`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "I want to provide the teacher with a section of common errors made by a student. It should be visible from the student's view, but it's only available to the teacher. Maybe it could be a view of errors made from self-reviewing homeworks, maybe also take into account the label set by the teacher on the homework."

## Clarifications

### Session 2026-09-27

- Q: Should teacher-graded answers (free text / writing) also appear as mistakes? → A: No — automatically corrected questions only; teacher-graded answers are excluded entirely (not listed, not counted).
- Q: How should mistakes be grouped (by label, by exercise type, …)? → A: Not grouped at all — a single list of mistakes in chronological order, newest first. Labels are shown on each entry for context, not used for grouping.
- Q: Should the teacher be able to filter the list by label? → A: Yes — optional label filter ("all labels" by default, plus each label present on this student's mistakes), combined with the period filter.
- Q: Should auto-corrected presentation activities (or quizzes) also feed the list? → A: No — homework only; presentation activities and quizzes are excluded.
- Q: How should the section appear on the admin student profile? → A: As a summary box in the top stats row showing the mistake count; clicking it expands a full-width list below the row, like the Tareas box.
- Q: What should the box and section be called, given it is a plain list rather than a frequency count? → A: Keep "Errores frecuentes".
- Q: Can the Tareas and Errores frecuentes lists be expanded at the same time? → A: Yes — both can be open, stacked below the summary row, Tareas first, then Errores frecuentes.
- Q: How should a long list of mistakes be handled? → A: Numbered pages of 20 with previous / next navigation.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher reviews a student's recent mistakes (Priority: P1)

Paula opens a student's profile in the admin panel and finds a new **Errores frecuentes** (common errors) box in the top summary row, showing how many mistakes the student has made. Clicking it expands a full-width list below the summary row, just like the Tareas box. The list shows the student's mistakes on automatically corrected homework questions as one list, newest first. Each entry shows the homework it came from (with that homework's labels, e.g. "Subjuntivo", "Ser/estar"), the submission date, the question, what the student answered, and the correct answer. Reading down the list she spots the pattern — e.g. always choosing "es" where "está" is needed — and knows what to revisit in the next class. She can jump from an entry to the student's full submitted homework.

**Why this priority**: This is the whole value of the feature — the concrete wrong answers in one place instead of scattered across individual homework results.

**Independent Test**: Give a student two submitted auto-corrected homeworks: an older one labelled "Vocabulario" with 1 of 6 questions wrong and a newer one labelled "Subjuntivo" with 3 of 5 wrong. Open the student's profile and confirm the section lists 4 mistakes, the 3 from the "Subjuntivo" homework first, each with the student's answer, the correct answer, the homework title and its label, and a working link to the submission.

**Acceptance Scenarios**:

1. **Given** a student has submitted auto-corrected homeworks with wrong answers, **When** the teacher opens that student's profile, **Then** the summary row shows an Errores frecuentes box with the all-time mistake count, collapsed.
2. **Given** the box is collapsed, **When** the teacher clicks it, **Then** a full-width list opens below the summary row (under all stats boxes), and clicking again collapses it.
3. **Given** the list is open, **When** it is displayed, **Then** it lists each wrong question with the homework title, the homework's labels (if any), the submission date, the question prompt, the student's answer, and the correct answer.
4. **Given** mistakes from several homeworks, **When** the list is displayed, **Then** they appear in a single ungrouped list ordered by submission date, newest first; mistakes from the same submission keep the order in which the questions appear in the homework.
5. **Given** a multi-part question (e.g. several blanks, matching pairs, table cells) where only some parts are wrong, **When** it is listed, **Then** the entry makes clear which parts were wrong and which were right.
6. **Given** a listed mistake, **When** the teacher selects "open homework", **Then** she lands on that student's submitted homework result, the same view already available from the student's Tareas list.
7. **Given** a homework has no labels, **When** its mistakes are listed, **Then** they appear normally, simply without labels.

---

### User Story 2 - Teacher filters the mistakes by label (Priority: P2)

Before a class on the subjunctive, Paula picks "Subjuntivo" in the section's label filter and sees only the mistakes from homeworks carrying that label, still newest first. Switching back to "all labels" shows everything again.

**Why this priority**: Lets Paula use her existing labels to focus on one topic, but the unfiltered list already delivers the core value.

**Independent Test**: With the setup from Story 1, pick "Subjuntivo" and confirm only its 3 mistakes appear and the summary counts only that homework's questions; pick "all labels" and confirm all 4 mistakes return.

**Acceptance Scenarios**:

1. **Given** the student's mistakes come from homeworks with different labels, **When** the teacher opens the label filter, **Then** it offers "all labels" plus one option per label present on the student's auto-corrected submissions (capitalisation ignored), and no "unlabelled" option.
2. **Given** a label is selected, **When** the list is displayed, **Then** only mistakes from homeworks currently carrying that label appear, and the summary counts only those homeworks' questions.
3. **Given** a homework carries several labels, **When** any of them is selected, **Then** its mistakes appear.
4. **Given** "all labels" is selected, **When** the list is displayed, **Then** mistakes from unlabelled homeworks appear too.

---

### User Story 3 - Teacher narrows the period (Priority: P3)

Paula wants to know whether a student still makes a mistake, not whether they made it six months ago. She can limit the section to a recent period (last 30 days, last 90 days, or all time), and the list and summary update accordingly.

**Why this priority**: Useful for tracking progress, but the section already delivers value with all-time data.

**Independent Test**: Give a student one wrong-answer homework submitted 10 days ago and one submitted 120 days ago. Confirm "last 30 days" and "last 90 days" show only the first, and "all time" shows both.

**Acceptance Scenarios**:

1. **Given** mistakes from different dates, **When** the teacher picks a period, **Then** only mistakes from homeworks submitted within that period are counted and listed.
2. **Given** a label filter is also selected, **When** the teacher picks a period, **Then** both filters apply together.
3. **Given** the chosen filters match no auto-corrected submissions, **When** the section is displayed, **Then** it shows a clear empty state.

---

### Edge Cases

- **Student with no auto-corrected submissions**: the box shows 0 and still expands; the list shows an empty state explaining that errors appear once the student submits auto-corrected homework.
- **Student answered everything correctly**: the section says so (e.g. "Sin errores en este periodo") and still shows how many questions were answered, so "no errors" is distinguishable from "no data".
- **Mixed homework** (auto-corrected and teacher-graded questions together): only the auto-corrected questions contribute (see FR-003); teacher-graded questions are ignored.
- **Homework not yet submitted / in progress**: contributes nothing.
- **Label changed after submission**: each entry shows the homework's current labels, and the label filter uses current labels too.
- **Selected label no longer present** (e.g. the homework was relabelled or deleted while the profile was reopened): the filter falls back to "all labels" rather than leaving an empty stale filter.
- **Homework deleted**: its mistakes no longer appear (consistent with the submission itself no longer being viewable).
- **Homework edited after submission**: the mistake is shown from what the student actually saw and answered at submission time (the frozen submission), not the current version of the homework.
- **Unanswered question in a submitted homework**: counts as wrong, shown with an explicit "sin respuesta" marker instead of an empty answer.
- **Numbered single-choice items**: each wrong item counts as its own mistake in the counts, consistent with how the homework's score counts them. The items are shown together in one list entry for their question, with each item marked right or wrong.
- **Long history**: a student with many mistakes gets numbered pages of 20 (see FR-011); a page boundary may split mistakes from the same homework across two pages.
- **Current page no longer exists** (e.g. data changed and there are fewer pages when the profile is reopened): the list shows the last available page rather than an empty page.
- **Student status revoked**: the section is still available to the teacher; history is never removed.
- **Presentation activities and quizzes**: never appear and are never counted, even when auto-corrected (FR-003a).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The admin student profile MUST include an Errores frecuentes summary box in its top stats row for the student being viewed. While collapsed, the box MUST show the student's all-time mistake count (independent of the filters inside the list).
- **FR-001a**: Clicking the box MUST expand a full-width list below the summary row (under all stats boxes), pushing subsequent profile sections down; clicking again MUST collapse it. The two expandable boxes (Tareas, Errores frecuentes) MUST expand and collapse independently; when both are expanded, their lists MUST appear stacked below the summary row, Tareas first, then Errores frecuentes.
- **FR-002**: The section and its data MUST be available only to the teacher (admin). Students MUST NOT see it on their own pages, and any attempt by a non-admin to obtain this data MUST be refused.
- **FR-003**: The section MUST be built from the student's submitted answers to automatically corrected homework questions (single choice, multiple choice, true/false, fill-in-the-blanks, drag-and-drop, table fill, matching). Teacher-graded answers (free text and writing tasks) MUST NOT be included — neither listed as mistakes nor counted in any total, whatever percentage the teacher gave them.
- **FR-003a**: Only homework submissions MUST be included. Presentation activity submissions and quiz attempts MUST NOT be listed or counted.
- **FR-004**: A question MUST count as a mistake when it is not fully correct. For questions that the grading already expands into separate items (numbered single-choice items), each item MUST count as its own question, consistent with how the homework's score counts them.
- **FR-005**: Mistakes MUST be displayed as a single ungrouped list, ordered by submission date newest first; within one submission, in the order the questions appear in the homework.
- **FR-006**: Each listed mistake MUST show: homework title, the homework's current labels (none shown when it has no labels), submission date, question prompt, the student's answer, and the correct answer.
- **FR-007**: For multi-part questions, a listed mistake MUST indicate which parts were wrong.
- **FR-008**: Each listed mistake MUST link to the student's submitted homework result.
- **FR-009**: Answers and prompts MUST be shown as they were at submission time, not as the homework currently reads.
- **FR-010**: The section MUST show a one-line summary for the selected filters: number of mistakes and number of auto-corrected questions answered.
- **FR-011**: The list MUST be paginated in pages of 20 entries (one entry per question with a mistake), starting on page 1 (the 20 most recent). The teacher MUST be able to move to the previous and next page and see the current page number and total pages; previous MUST be unavailable on the first page and next on the last. Changing the label or period filter MUST return to page 1. Pagination controls MUST NOT be shown when all mistakes fit on one page.
- **FR-012**: The teacher MUST be able to limit the section to the last 30 days, last 90 days, or all time; the default MUST be all time. The summary and list MUST reflect the chosen period, based on submission date.
- **FR-012a**: The teacher MUST be able to filter the section by label. Options MUST be "all labels" (default) plus each label present on the student's submitted auto-corrected homeworks, with labels differing only by capitalisation offered once; there MUST NOT be an "unlabelled" option. Selecting a label MUST limit the summary and list to homeworks currently carrying that label (a homework with several labels matches each of them); "all labels" MUST include unlabelled homeworks. The label and period filters MUST combine. If the selected label is no longer present, the filter MUST fall back to "all labels".
- **FR-013**: The section MUST show distinct empty states for "no auto-corrected submissions" and "no mistakes for the selected filters".
- **FR-014**: The section MUST be read-only: viewing it MUST NOT change any homework, score, review, notification dot, or the student's own views.
- **FR-015**: The section MUST reflect the current data every time the profile is opened (a new submission, a relabelled homework, or a deleted homework is visible without any manual refresh step by the teacher).

### Key Entities

- **Mistake entry**: one auto-corrected question (or expanded numbered item) that a student answered not fully correctly in a submitted homework. Attributes: homework (title, current labels), submission date, question prompt, student's answer, correct answer, which parts were wrong. Derived from existing submission data; no new record is authored by anyone.
- **Homework label** (existing): teacher-only free-text labels on a homework, displayed on each entry and usable as a filter (capitalisation ignored).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From a student's profile, the teacher sees the student's most recent mistake, with the student's and the correct answer side by side, in 1 interaction (expanding the box) and without opening any individual homework.
- **SC-002**: From any listed mistake, the teacher reaches the full submitted homework in 1 interaction.
- **SC-003**: For any student, the mistakes listed and the summary counts match a manual review of their submitted auto-corrected homework results 100% of the time.
- **SC-004**: 0 students can see or retrieve the common-errors data for themselves or anyone else.
- **SC-005**: The section appears within the normal load time of the student profile for a student with 100 submitted homeworks.

## Assumptions

- "Self-reviewing homeworks" is interpreted as automatically corrected homework questions — the ones the system marks right or wrong on submit. Teacher-graded free-text and writing answers are out of scope (confirmed in Clarifications).
- Labels already exist on homeworks (a homework may have several) and remain teacher-only; here they are shown on each entry and offered as a filter. This feature does not add labels to individual questions.
- Only homework is in scope (confirmed in Clarifications). Presentation activities and quizzes are excluded, even though activities share the homework grading model.
- The teacher cannot edit, dismiss, or annotate entries in this section; it is a derived, read-only view. Tagging mistakes by hand or with automatic language analysis is out of scope.
- The section lives inside the existing admin student profile and reuses the Tareas box's expand pattern; there is no cross-student "common errors of the whole class" view in this version.
- UI text follows the rest of the admin panel (Spanish).
