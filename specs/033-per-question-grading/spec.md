# Feature Specification: Per-Question Homework Grading

**Feature Branch**: `033-per-question-grading`

**Created**: 2026-08-10

**Status**: Draft

**Input**: User description: "Change the way homeworks work from auto-correctible vs manual to each question inside the homework being either auto-correctible or manual. In case of this mix, the final result of the homework will have to be decided by the teacher which will either validate or invalidate the manual responses."

## Clarifications

### Session 2026-08-10

- Q: How should the overall score work for mixed homeworks? → A: Combined percentage — each manual answer counts as a full question (validated = correct, invalidated = incorrect) in one overall % with the auto questions
- Q: What happens to the homework-level Manual vs Exercise choice? → A: Remove homework-level Manual/Exercise — non-Writing homeworks are just a question list; each question is auto or manual; Writing stays its own single large manual answer
- Q: During mixed review, is validate/invalidate enough? → A: Validate/invalidate required to finalize, plus optional annotate + short feedback on manual answers (parity with pure-manual review)
- Q: Status while a mixed homework awaits teacher validation? → A: Same as pure-manual awaiting review — one “submitted / awaiting teacher” status for both pure-manual and mixed
- Q: When should students see correct answers for auto questions on a mixed homework? → A: Reveal auto keys immediately on submit — manual answers still await teacher; combined % stays provisional/finalized later

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher authors a homework with mixed question kinds (Priority: P1)

Paula creates or edits a homework as an ordered list of questions. There is no longer a homework-level Manual vs Exercise switch for non-Writing work — for each question she chooses whether it is auto-correctible (structured question with an answer key the system can grade) or manual (free-text answer the teacher must judge). She can build a homework that is all auto-correctible, all manual, or a mix of both in one assignment. Writing (WRITE) remains a separate homework form: a single large free-text answer that is always treated as manual.

**Why this priority**: Moving the auto vs manual decision from the whole homework to each question is the core change; without authoring support, students and review cannot use mixed homeworks.

**Independent Test**: Create a homework with at least one auto-correctible question and one manual free-text question; save; reopen the editor and confirm both questions keep their kind, order, and content.

**Acceptance Scenarios**:

1. **Given** the teacher is creating or editing a non-Writing homework, **When** they add questions, **Then** they can mark each question as either auto-correctible or manual independently — without first choosing a homework-level Manual or Exercise mode.
2. **Given** the teacher adds an auto-correctible question, **When** they configure its type and answer key, **Then** the system stores it as auto-correctible and requires a complete answer key before save (same rules as today’s self-correcting questions).
3. **Given** the teacher adds a manual question, **When** they enter a prompt, **Then** the system stores it as manual free-text (no answer key) and does not auto-grade that question.
4. **Given** a homework contains both auto-correctible and manual questions, **When** the teacher saves, **Then** the homework is stored as one assignment with that mixed question list (no requirement to split into two homeworks or pick a “Mixed” mode).
5. **Given** a Writing (WRITE) homework, **When** the teacher authors it, **Then** it remains a dedicated single large free-text response treated as manual (not authored as a per-question auto/manual list).
6. **Given** the teacher tries to save a non-Writing homework with no questions, **When** they save, **Then** the system blocks save and requires at least one question.

---

### User Story 2 - Student completes a mixed homework in one submission (Priority: P1)

A student opens a homework that mixes auto-correctible and manual questions. They answer every question on one page/flow, then submit once. Auto-correctible answers are graded immediately by the system. Manual answers are stored for the teacher. The homework is not fully finished until the teacher decides on the manual responses.

**Why this priority**: Students must be able to take mixed homeworks as a single task; this is the student-facing value of the authoring change.

**Independent Test**: As a student, open a mixed homework, answer all questions, submit, and confirm auto questions show immediate correctness/score while manual answers are saved and the homework still awaits teacher decision.

**Acceptance Scenarios**:

1. **Given** a mixed homework with auto-correctible and manual questions, **When** the student opens it, **Then** they see all questions in order with the right answer controls for each kind (structured controls for auto; free-text for manual).
2. **Given** the student has answered every question, **When** they submit, **Then** the system accepts one submission for the whole homework, auto-grades the auto-correctible questions, and stores the manual answers for teacher review.
3. **Given** a student tries to submit with any required question unanswered, **When** they submit, **Then** the system blocks submit and asks them to complete every question.
4. **Given** the student just submitted a mixed homework, **When** they view the result, **Then** they see immediate feedback for auto-correctible questions including correct/incorrect and the answer key for wrong auto questions (same reveal behavior as all-auto), see that manual answers are awaiting teacher validation, and see the same “submitted / awaiting teacher” status used for pure-manual work — a provisional auto-only score may be shown, but the final overall homework percentage is not complete until the teacher validates or invalidates every manual answer.
5. **Given** a homework that is entirely auto-correctible, **When** the student submits, **Then** the homework is fully graded immediately with no teacher step (same outcome as today’s self-correcting exercises).
6. **Given** a homework that is entirely manual (or Writing), **When** the student submits, **Then** the submission awaits teacher review as today (no auto score).
7. **Given** a mixed submission and a pure-manual submission both awaiting the teacher, **When** the teacher looks at review queues or status lists, **Then** both appear under the same awaiting-teacher/submitted status (no separate “partially graded” status for mixed).

---

### User Story 3 - Teacher validates or invalidates manual responses to finalize a mixed homework (Priority: P1)

Paula opens a submitted mixed homework. She sees the student’s auto-graded results and each manual answer. For every manual response she chooses **validate** (accept) or **invalidate** (reject). She may optionally annotate those manual answers (color, highlight, strikethrough) and leave a short plain feedback note, using the same review tools as pure-manual review. When she has decided on all manual responses and saves, the homework’s final combined result is set and the student can see that outcome (including any annotations and note).

**Why this priority**: Without teacher validation of manual parts, mixed homeworks cannot reach a final result — this is the explicit requirement for the mixed case.

**Independent Test**: Submit a mixed homework as a student; as teacher, validate one manual answer and invalidate another, optionally annotate and add a short note; save; reopen as student and confirm the final combined score, decisions, and any annotations/note.

**Acceptance Scenarios**:

1. **Given** a student has submitted a mixed homework, **When** the teacher opens it for review, **Then** they see auto-graded question results and each manual question paired with the student’s answer.
2. **Given** the teacher is reviewing a mixed submission, **When** they mark each manual response as validated or invalidated and save, **Then** the homework reaches a final result (including a combined overall percentage) and is no longer awaiting teacher decision.
2a. **Given** the teacher is reviewing a mixed submission, **When** they annotate one or more manual answers and/or enter an optional short plain feedback note (within the same limits as pure-manual review) and save with all validate/invalidate decisions set, **Then** those annotations and the note are stored and shown to the student with the finalized result.
2b. **Given** the teacher finalizes with validate/invalidate decisions but leaves annotations and the short note empty, **When** they save, **Then** finalization still succeeds — annotations and the note are optional; validate/invalidate are required.
3. **Given** the teacher tries to finalize while any manual response still has no validate/invalidate choice, **When** they save as final, **Then** the system blocks finalization and asks them to decide on every manual response.
4. **Given** a mixed homework has been finalized, **When** the student opens it, **Then** they see the auto-graded results, for each manual response whether the teacher validated or invalidated it (plus any annotations on those answers), any short feedback note, and one overall percentage that treats each validated manual answer as fully correct and each invalidated manual answer as incorrect alongside the auto questions.
5. **Given** a purely auto-correctible submission, **When** it is already graded, **Then** the teacher does not need to validate/invalidate anything for it to be final.
6. **Given** a purely manual (or Writing) submission, **When** the teacher reviews it, **Then** existing review behavior continues to apply (mark reviewed with feedback/annotations as already supported); validate/invalidate of mixed manual responses is not required to replace that flow for pure-manual work.

---

### User Story 4 - Existing all-auto and all-manual homeworks keep working (Priority: P2)

Homeworks that were created under the old whole-homework Manual vs Exercise model remain usable after migration: former Exercise homeworks behave as all auto-correctible questions; former Manual (non-Writing) homeworks behave as all manual questions. All-auto homeworks still auto-grade on submit. All-manual and Writing homeworks still go through teacher review. Teachers can later edit an old homework and add the other question kind if they want a mix — without re-selecting a homework-level mode.

**Why this priority**: Migration and continuity protect live classes; the new model must not break current assignments.

**Independent Test**: Open an existing auto-only and an existing manual-only homework as student and teacher; confirm submit/grade/review still work; optionally convert one by adding a question of the other kind and confirm mixed behavior applies.

**Acceptance Scenarios**:

1. **Given** an existing self-correcting (all auto) homework, **When** a student submits, **Then** it still grades immediately and reaches a final result without teacher validation.
2. **Given** an existing manual homework, **When** a student submits and the teacher reviews, **Then** the previous manual review path still completes successfully.
3. **Given** an existing all-auto or all-manual homework, **When** the teacher adds a question of the other kind and saves, **Then** new submissions follow the mixed finalize rules.

---

### Edge Cases

- What if the teacher removes a question after students have submitted? Already-submitted work MUST keep a snapshot of prompts and answers (including question kind) so review and student view still make sense; unfinished work follows the current question list.
- What if a mixed homework has only one manual question and several auto questions? Finalize still requires a validate/invalidate decision on that single manual response.
- What if the teacher invalidates all manual responses? The homework still becomes final; those invalidated answers count as incorrect in the combined overall percentage (together with auto-question results) — there is no separate hidden pass/fail beyond that score and the visible validate/invalidate marks.
- What if the teacher wants to change a validate/invalidate decision after finalizing? The teacher MAY update those decisions later; the student always sees the latest saved decisions.
- What about answer keys on auto questions before the student submits? Students MUST NOT see answer keys for auto-correctible questions until after they submit (same privacy rule as today).
- What about answer keys on auto questions after a mixed submit but before teacher finalizes? Students MUST see auto feedback and answer keys for auto questions immediately on submit (same as all-auto); that early reveal MUST NOT unlock retakes, and MUST NOT imply the combined homework percentage is final.
- What about presentation activities? MANUAL/EXERCISE activities follow the same per-question kind model and mixed finalize rules for parity with homework.
- What if the homework is Writing only? Remains a single large manual answer with the existing pure-manual review path (not mixed validate/invalidate unless the teacher has also attached other question kinds — Writing stays single-answer manual).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST treat auto-correctible vs manual as a property of each question (or of the single Writing answer), not as a homework-level Manual vs Exercise mode. Non-Writing homeworks MUST NOT require the teacher to choose Manual or Exercise for the whole assignment.
- **FR-001a**: System MUST keep Writing (WRITE) as a distinct homework form with a single large free-text manual response (not a per-question auto/manual list).
- **FR-002**: System MUST allow a non-Writing homework to contain only auto-correctible questions, only manual questions, or any mix of both in one ordered list.
- **FR-003**: System MUST require auto-correctible questions to have a complete answer key and structured type suitable for automatic grading before the homework can be saved.
- **FR-004**: System MUST require manual questions to have a clear prompt and accept free-text student answers (no auto answer key).
- **FR-005**: System MUST keep Writing (WRITE) as a single large free-text manual response (not forced into a compact multi-question list) and MUST NOT expose per-question auto/manual kind controls on Writing homeworks.
- **FR-006**: System MUST let the student answer all questions of a homework in one attempt and submit them together.
- **FR-007**: System MUST auto-grade only auto-correctible questions on submit and MUST NOT auto-grade manual or Writing answers.
- **FR-008**: System MUST, for a submission that includes at least one manual answer and at least one auto-correctible question (mixed), leave the homework awaiting teacher decision until every manual response is validated or invalidated.
- **FR-008a**: System MUST represent mixed submissions that still need teacher decisions with the same student/teacher “submitted / awaiting review” status used for pure-manual (and Writing) submissions — not a distinct “partially graded” primary status, and not a fully graded status until finalize.
- **FR-009**: System MUST allow the teacher to mark each manual response on a mixed submission as validated or invalidated, and MUST require a decision on every such response before the mixed homework’s result is finalized.
- **FR-009a**: System MUST allow the teacher, during mixed review, to optionally annotate manual answers (color, highlight, strikethrough) and optionally leave a short plain feedback note under the same rules/limits as pure-manual review; annotations and the note MUST NOT be required to finalize.
- **FR-010**: System MUST, once a mixed submission is finalized, present the student with (a) auto-graded results for auto questions, (b) validate/invalidate outcomes for each manual response, (c) any teacher annotations and short feedback note on the manual portion, and (d) one combined overall percentage (plus fully-correct count) in which each manual answer counts as one full question — validated = correct, invalidated = incorrect — scored together with the auto-correctible questions.
- **FR-010a**: System MUST NOT treat the combined overall percentage as final for a mixed submission until every manual response has been validated or invalidated; before that, any shown score MUST be clearly provisional (auto portion only) or omitted as the final homework score.
- **FR-011**: System MUST fully finalize purely auto-correctible submissions on submit without a teacher validate/invalidate step.
- **FR-012**: System MUST keep purely manual and Writing submissions on the existing teacher review path (review / feedback as already supported), without requiring the mixed validate/invalidate finalize flow.
- **FR-013**: System MUST hide auto-correctible answer keys from students until after they submit.
- **FR-013a**: System MUST, on mixed (and all-auto) submit, immediately reveal auto-question feedback including answer keys for incorrect auto answers under the same rules as today’s self-correcting exercises; this reveal MUST occur even while the mixed submission still awaits teacher validate/invalidate, and MUST NOT allow the student to change answers.
- **FR-014**: System MUST migrate existing Exercise homeworks to all auto-correctible questions and existing Manual (non-Writing) homeworks to all manual questions so they continue to behave as all-auto or all-manual under the per-question model, without a leftover homework-level Manual/Exercise switch.
- **FR-015**: System MUST retain snapshots of question prompts, kinds, and answers for already-submitted work when the teacher later edits or removes questions.
- **FR-016**: System MUST apply the same per-question kind model and mixed finalize rules to presentation activities that use homework-like question formats.
- **FR-017**: System MUST allow the teacher to change validate/invalidate decisions, annotations, and the short feedback note on a finalized mixed submission; students MUST see the latest saved version.

### Key Entities

- **Homework (or Activity)**: An assignable piece of work. Non-Writing homeworks are an ordered list of questions whose grading kinds are set per question (all-auto, all-manual, or mixed are derived, not chosen as a top-level Manual/Exercise mode). Writing is a distinct form with one large manual answer.
- **Question**: A single item with a **grading kind** — auto-correctible (structured + answer key) or manual (free-text) — plus prompt/content specific to its type.
- **Submission**: One student’s answers to all questions in a homework/activity; may be fully graded (all auto), awaiting review (all manual / Writing, **or** mixed still missing validate/invalidate decisions — same awaiting-teacher status), or finalized after mixed validation (combined score complete).
- **Manual response decision**: Teacher’s validate or invalidate mark for one manual answer on a mixed submission; required for every manual answer before the mixed result is final.
- **Auto-graded result**: Per-question correctness for auto-correctible answers (and, before mixed finalization, the provisional auto-only portion of the score).
- **Combined overall score**: After mixed finalization, one percentage (and fully-correct count) over all questions: auto answers keep their graded correctness; each manual answer contributes as one full question (validated = correct / invalidated = incorrect).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can create a non-Writing homework with both an auto-correctible question and a manual question and save it successfully in under 5 minutes without creating two separate assignments and without choosing a homework-level Manual/Exercise/Mixed mode.
- **SC-002**: After submitting a mixed homework, 100% of students see immediate feedback on auto-correctible questions, a clear “awaiting teacher” state for manual answers using the same submitted/awaiting-review status as pure-manual work, and not a premature final combined percentage.
- **SC-003**: Teachers can finalize a mixed submission by validating or invalidating every manual response in one review session; students then see those decisions and one combined overall percentage (manual validated/invalidated counted with auto questions) on the next view without a status inconsistency.
- **SC-004**: 100% of previously all-auto and all-manual homeworks remain completable end-to-end after the change (submit → grade or review → visible outcome) without teacher re-authoring.
- **SC-005**: In usability checks, teachers correctly identify which questions will be auto-graded vs teacher-judged before publishing, on first try for at least 9 out of 10 sample homeworks.

## Assumptions

- “Validate” means the teacher accepts that manual answer as satisfactory (counts as correct in the combined score); “invalidate” means they reject it as unsatisfactory (counts as incorrect). Both marks are visible to the student once the mixed homework is finalized.
- The mixed homework’s “final result” is one combined overall percentage (and fully-correct count) over all questions, built from auto-graded correctness plus validate/invalidate on each manual answer — not a separate hidden pass/fail threshold beyond that score and the visible marks.
- Purely manual and Writing work keeps today’s review experience (including annotation and short-feedback review capabilities). Mixed submissions use that same optional annotate + short-note toolkit, and additionally require validate/invalidate on every manual response before the combined result is final.
- Auto-correctible question types remain the existing structured kinds teachers already use (choice, blanks, matching, etc.); this feature does not invent new auto question formats.
- Manual questions remain free-text prompts (compact answers for multi-question manuals; large rich-text for Writing).
- Presentation activities that already mirror homework formats are in scope for the same per-question kind and mixed finalize behavior.
- Existing homeworks migrate in place: former Exercise → all questions auto-correctible; former Manual (non-Writing) → all questions manual; Writing unchanged. The homework-level Manual vs Exercise choice is removed from authoring for non-Writing work.
- Mixed submissions awaiting validate/invalidate share the same primary “submitted / awaiting teacher” status as pure-manual submissions (no separate partially-graded status).
- On mixed submit, auto answer keys are revealed immediately (no retake), while the combined overall percentage waits for teacher validate/invalidate.
- Single submission, no retake after submit, remains the rule for graded/submitted work.
- Skill/type labels used for organizing homework content (e.g. listening, reading) may still exist where they describe the activity, but they do not encode auto vs manual grading — that lives only on each question (or on Writing as always-manual).
