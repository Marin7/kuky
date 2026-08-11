# Feature Specification: Percentage Grades for Manual Answers

**Feature Branch**: `037-manual-percent-grade`

**Created**: 2026-08-11

**Status**: Draft

**Input**: User description: "On manual homeworks, I want the teacher to be able to set a percentage of how well was it done, instead of valid/invalid."

## Clarifications

### Session 2026-08-11

- Q: Where does percentage grading apply? → A: All manually graded answers — pure-manual, Writing, mixed free-text, and presentation activities
- Q: One score per answer, or one for the whole homework? → A: Percentage per manual answer (Writing’s one answer = overall; multi-question = equal average)
- Q: What happens to “X of Y correct” with partial credit? → A: Keep the fully-correct count; only answers at 100% count as fully correct (same as former validated → 100%)
- Q: Can the teacher save progress before scoring every answer? → A: Allow saving partial percentages; stay awaiting teacher until every manual answer has a score
- Q: What should students see while some answers are scored but not finalized? → A: Hide all teacher percentages until every answer is scored and finalized

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher grades a manual answer with a percentage (Priority: P1)

A student has submitted a manual homework (all free-text questions, Writing, or a mixed homework with free-text questions). Paula opens the submission to review. Instead of marking each manual answer only as valid or invalid, she enters how well that answer was done as a percentage from 0% to 100%. She may save progress after scoring only some answers; the submission stays awaiting teacher until every manual answer has a percentage. She can still optionally annotate the answer and leave a short feedback note. When every manual answer has a percentage and she finalizes, the homework is graded and the overall score reflects those percentages.

**Why this priority**: Replacing binary validate/invalidate with a percentage is the core teacher need; without it, partial credit on manual work is impossible.

**Independent Test**: Submit a pure-manual (or Writing) homework as a student; as teacher, enter e.g. 70% on the manual answer(s), save; confirm the submission is graded and the overall percentage matches.

**Acceptance Scenarios**:

1. **Given** a submitted homework that still needs teacher review of manual answers, **When** the teacher opens it, **Then** each manual answer (each free-text question, or the single Writing response) shows a control to enter a percentage score from 0 to 100 — not a validate/invalidate choice.
2. **Given** the teacher enters a percentage for every manual answer and saves, **When** finalization succeeds, **Then** the homework is marked graded and the overall result uses those percentages.
3. **Given** the teacher has entered percentages for only some manual answers, **When** they save progress, **Then** those percentages are stored, the submission stays awaiting teacher, and finalization does not occur until every manual answer has a percentage.
4. **Given** the teacher tries to finalize while any manual answer still has no percentage, **When** they save as final, **Then** the system blocks finalization and asks them to score every manual answer.
5. **Given** the teacher enters a value outside 0–100, **When** they try to save, **Then** the system rejects it with a clear message and does not save that value.
6. **Given** the teacher is reviewing, **When** they optionally annotate a manual answer and/or leave a short feedback note and save with all percentages set (finalize), **Then** annotations and the note are stored with the graded result (same optional behavior as today).
7. **Given** the teacher finalizes with percentages but leaves annotations and the short note empty, **When** they save, **Then** finalization still succeeds — annotations and the note remain optional; percentages are required.

---

### User Story 2 - Student sees percentage-based manual results (Priority: P1)

After the teacher has scored the manual answers with percentages, the student opens the homework and sees how well each manual answer was graded (as a percentage), any annotations and short note, and one overall homework percentage that reflects those scores (combined with auto-graded questions when present).

**Why this priority**: Students must understand partial credit instead of only “accepted” or “rejected.”

**Independent Test**: After the teacher saves percentages on a submission, open it as the student and confirm per-manual percentages and the overall score are visible (no validate/invalidate labels).

**Acceptance Scenarios**:

1. **Given** a finalized submission with teacher percentages on manual answers, **When** the student views the result, **Then** they see each manual answer’s percentage (not valid/invalid), any annotations and short note, and the overall homework percentage.
2. **Given** a Writing homework graded at e.g. 85%, **When** the student views it, **Then** that 85% is both the Writing score and the overall homework percentage.
3. **Given** a pure-manual homework with several free-text questions scored at different percentages, **When** the student views the result, **Then** they see each question’s percentage and one overall percentage that combines them equally (average of the question percentages); the fully-correct count includes only questions scored at 100%.
4. **Given** a mixed homework with auto-graded and manual questions, **When** the student views the finalized result, **Then** auto questions still show correct/incorrect (and answer keys as today), manual questions show teacher percentages, and the overall percentage combines all questions equally (each auto question contributes 100% if correct or 0% if incorrect; each manual question contributes its teacher percentage); the fully-correct count includes only auto-correct answers and manual answers scored at 100%.
5. **Given** a submission that is still awaiting teacher (including when the teacher has saved some but not all percentages), **When** the student views it, **Then** they MUST NOT see any teacher percentages or a finalized overall that includes manual scores; for mixed work they may still see auto feedback as today.

---

### User Story 3 - Teacher can revise percentages after grading (Priority: P2)

Paula realizes she was too strict or too generous. She reopens a graded submission, changes one or more manual percentages, and saves. The student always sees the latest overall and per-answer scores.

**Why this priority**: Teachers need to correct mistakes without forcing a resubmit; lower priority than first-time grading.

**Independent Test**: Finalize a submission with 60%; change it to 80% and save; as student, confirm 80% is shown.

**Acceptance Scenarios**:

1. **Given** a graded submission with manual percentages, **When** the teacher changes a percentage and saves, **Then** the stored score and overall percentage update to the new values.
2. **Given** the teacher updates percentages, **When** the student opens the result again, **Then** they see the latest percentages and overall score.

---

### User Story 4 - Mixed and presentation activities use the same percentage model (Priority: P2)

Mixed homeworks (auto + manual questions) and presentation activities that use the same question model no longer use validate/invalidate for their manual answers. Teachers enter percentages on those manual answers to finalize, consistent with pure-manual and Writing review.

**Why this priority**: Keeps grading consistent across assignment types; avoids leaving a second binary path in place.

**Independent Test**: Submit a mixed homework and a presentation activity with a free-text question; as teacher, enter percentages on the manual answers and confirm both finalize with combined overall percentages.

**Acceptance Scenarios**:

1. **Given** a mixed submission awaiting review, **When** the teacher opens it, **Then** they score each manual answer with a percentage (auto results remain as already graded).
2. **Given** all manual answers on a mixed submission have percentages and the teacher saves, **When** finalization succeeds, **Then** the overall percentage combines auto and manual contributions as in User Story 2.
3. **Given** a presentation activity submission with manual answers awaiting review, **When** the teacher grades with percentages, **Then** the same rules apply as for homework.

---

### Edge Cases

- What about a manual answer scored 70%? It contributes 70 to the overall average and MUST NOT count as fully correct in the “X of Y correct” count (only 100% does).
- What if the teacher enters 0%? Allowed — that answer contributes 0 to the overall score (same numerical effect as former “invalidated”).
- What if the teacher enters 100%? Allowed — that answer contributes fully and counts as fully correct (same meaning as former “validated”).
- What if there is only one manual answer? That percentage is the overall score for pure-manual/Writing; for mixed, it still combines with auto questions as equal parts of the whole.
- What about submissions already finalized with validate/invalidate before this change? Existing validated answers MUST appear as 100% and invalidated as 0%; teachers MAY then edit to any percentage.
- What if the teacher clears a percentage while editing a graded submission? The system MUST block re-finalization until every manual answer again has a percentage — empty is not allowed on a graded save.
- What if the teacher scores only some answers on a multi-question submission and saves progress? The system MUST store those percentages for the teacher and keep the submission awaiting teacher; finalization MUST wait until every manual answer has a percentage; students MUST NOT see those partial teacher percentages until finalize.
- What about fractional percentages (e.g. 87.5%)? Not supported — whole numbers from 0 to 100 inclusive only.
- What about all-auto homeworks? Unchanged — no teacher percentage step; they still grade fully on submit.
- What about provisional mixed scores before the teacher grades manual answers? Auto feedback may still show immediately; the final overall percentage remains provisional/incomplete until every manual answer has a percentage (same awaiting-teacher status as today).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow the teacher to assign a percentage score from 0 to 100 (inclusive, whole numbers) to each manually graded answer (each free-text question and each Writing response) instead of validate or invalidate.
- **FR-002**: System MUST allow the teacher to save partial percentages on a submission that is still awaiting review; the submission MUST remain awaiting teacher until every manual answer has a percentage.
- **FR-002a**: System MUST require a percentage on every manual answer before the submission can be finalized (or re-saved as graded).
- **FR-002b**: System MUST allow the teacher to change previously saved percentages (including on a still-awaiting submission or after grading). While the submission is still awaiting teacher, students MUST NOT see any teacher percentages (partial or complete) nor a final overall grade derived from them; students MUST see teacher percentages and the final overall only after finalization. After grading, students MUST see the latest saved scores.
- **FR-003**: System MUST reject percentage values outside 0–100 or non-whole numbers with a clear error and MUST NOT save those values.
- **FR-004**: System MUST compute the overall homework (or activity) percentage as the equal average of all question scores: for each auto-correctible question, 100 if correct and 0 if incorrect; for each manual answer, the teacher’s percentage. Writing (single answer) uses that one percentage as the overall score.
- **FR-005**: System MUST show students the per-manual percentage, any annotations, any short feedback note, and the overall percentage after finalization — and MUST NOT present validate/invalidate as the primary outcome for manual answers.
- **FR-005a**: System MUST keep a fully-correct count alongside the overall percentage: an answer counts as fully correct only when it scores 100% (auto-correctible question marked correct, or manual answer scored 100 by the teacher — same meaning as former “validated”). Answers scored below 100% MUST NOT increment that count.
- **FR-006**: System MUST keep optional annotations and optional short feedback notes on manual review; they MUST NOT be required to finalize.
- **FR-007**: System MUST allow the teacher to change previously saved manual percentages; students MUST see the latest saved scores.
- **FR-008**: System MUST apply the same percentage grading model to every manually graded answer: pure-manual homeworks, Writing homeworks, mixed homeworks (free-text questions), and presentation activities — there MUST NOT be a separate validate/invalidate path for any of these.
- **FR-009**: System MUST leave purely auto-correctible submissions unchanged (still fully graded on submit with no teacher percentage step).
- **FR-010**: System MUST treat submissions that still need teacher percentages with the same “submitted / awaiting teacher” status used today for work awaiting validate/invalidate.
- **FR-011**: System MUST migrate already-finalized validate/invalidate decisions so validated answers become 100% and invalidated answers become 0%, without changing overall scores that already matched that binary model.
- **FR-012**: System MUST remove validate/invalidate as the teacher’s required decision for manual answers going forward (percentage replaces it).

### Key Entities

- **Manual answer score**: Teacher-assigned whole-number percentage (0–100) for one free-text or Writing answer; required to finalize teacher-reviewed work.
- **Submission**: One student’s attempt; awaiting teacher while any manual answer lacks a percentage (partial teacher percentages may already be stored); graded once all required percentages are saved as final.
- **Overall percentage**: Equal average of every question’s contribution (auto 0/100 or manual teacher %) for multi-question work; equal to the Writing percentage for Writing-only work.
- **Fully-correct count**: Number of answers at 100% (auto correct or teacher 100%); partial manual scores do not count.
- **Review extras**: Optional annotations and short feedback note on manual answers (unchanged in role; still optional).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can finalize a submitted manual or Writing homework by entering percentages (instead of valid/invalid) in under 2 minutes for a typical single-answer submission.
- **SC-002**: After finalization, 100% of reviewed test cases show students a numeric percentage for each manual answer and a matching overall percentage — with no reliance on validate/invalidate labels as the grade.
- **SC-003**: For a mixed homework with N auto questions and M manual questions, the overall percentage matches the equal average of N auto scores (0 or 100 each) and M teacher percentages, displayed as a whole percent using half-up rounding, in at least 95% of verification checks.
- **SC-004**: Teachers can change a saved percentage and have the student see the updated overall score on the next view without creating a new submission, in under 1 minute in typical review.
- **SC-005**: Existing graded submissions that were validated/invalidated remain viewable after the change, with scores preserved as 100%/0% equivalents, for 100% of migrated historical decisions.

## Assumptions

- “Manual homeworks” means all work the teacher grades by hand: pure-manual free-text lists, Writing, manual questions inside mixed assignments, and equivalent presentation activities — not only a separate “Manual” homework type label (confirmed: scope is all manually graded answers).
- Percentage replaces validate/invalidate entirely for those manual answers (no dual UI of both binary and percentage).
- Each manual answer gets its own percentage; there is no separate single “whole homework slider” in addition to per-answer scores (Writing’s one answer is inherently one overall %) (confirmed).
- Percentages are whole numbers 0–100; no decimals.
- Questions contribute equally to the overall percentage (same equal-weight model as today’s combined % where validated = 100 and invalidated = 0). When the average is not a whole number, the displayed overall percentage uses standard half-up rounding to the nearest whole percent.
- Teacher may save partial percentages while the submission stays awaiting review; finalization (and graded re-save) still requires every manual answer to have a percentage. Students never see teacher percentages until finalization.
- Optional annotations and short notes stay available with the same limits as the current manual review flow.
- All-auto assignments and auto-question behavior (immediate grade, answer-key reveal rules) stay as they are.
- Historical binary decisions map cleanly to 100% / 0% and do not require the teacher to re-grade past work.
