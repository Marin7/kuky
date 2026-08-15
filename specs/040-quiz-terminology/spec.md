# Feature Specification: Quiz Terminology (Replace Placement Test)

**Feature Branch**: `040-quiz-terminology`

**Created**: 2026-08-15

**Status**: Draft

**Input**: User description: "Replace 'prueba de nivel' with a Quiz terminology. A quiz will be a special type containing different questions, like it can have a combination of reading-writing-grammar-listening tests. They are NOT linked with homeworks, it's just that they use the same type of questions as the homeworks. Remove all the references of prueba de nivel, there's nothing in the prod DB at the moment"

## Clarifications

### Session 2026-08-15

- Q: How are skills organized inside a quiz? → A: Flat question list with a skill on each question; optional headings when the skill changes. No separate section object.
- Q: Who can take a published quiz? → A: Only students the teacher explicitly assigns to that quiz can take it.
- Q: Can a student retake a quiz after submitting? → A: One attempt: after submit the quiz is read-only; no retake.
- Q: How is a quiz result shown? → A: Overall quiz % and fully-correct count, plus a breakdown by skill (reading / writing / grammar / listening).
- Q: After the first student submits, can the teacher still change the quiz or assign more students? → A: Teacher may keep editing questions; already-submitted attempts stay as they were at submit time.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher authors a standalone quiz with mixed skills (Priority: P1)

Paula opens the admin area and creates a **quiz** — a standalone assessment, not a homework and not attached to a class unit. She gives it a title (and optional description) and builds a single ordered list of questions. Each question has a **skill** (reading, writing, grammar, or listening). The list may mix those skills in any order — for example several reading items, then grammar items, then a listening clip with questions, then a short written response. When consecutive questions share a skill, the student take view may show a heading for that skill; there are no separate section objects to author. Each question uses the same question kinds she already uses for homework (choice, true/false, blanks, matching, drag-and-drop, table fill, free-text, and listening media). When the quiz is ready, she **assigns it to specific students**; only those students can take it.

**Why this priority**: Authoring is the foundation. Without a quiz the teacher can create and mix skills in, there is nothing for students to take and no replacement for the retired placement test.

**Independent Test**: As the teacher, create a quiz that includes at least one reading-style question, one grammar-style question, one listening question with media, and one writing/free-text question; save; reopen and confirm all questions, order, skills, and answer keys (where applicable) persisted; assign it to one student.

**Acceptance Scenarios**:

1. **Given** the teacher is in the admin area, **When** they open the quizzes section (the former “Prueba de nivel” tab), **Then** they see a list of quizzes they can create, edit, assign to students, unassign, or delete — with no placement-test or “prueba de nivel” wording.
2. **Given** the teacher is creating a quiz, **When** they add questions, **Then** they add them to one ordered list, set a skill (reading, writing, grammar, or listening) on each question, and may mix those skills in any order using the same question kinds available for homework — they do not create named sections.
3. **Given** the teacher adds a listening question, **When** they attach media, **Then** they use the same listening media options as homework (uploaded audio, audio link, YouTube, external video page) and students will play that media while answering.
4. **Given** the teacher adds an auto-gradable question, **When** they save, **Then** a complete answer key is required, using the same rules as homework for that question kind.
5. **Given** the teacher adds a writing or other free-text question, **When** they save, **Then** no answer key is required; that question is marked for teacher review after a student submits.
6. **Given** a quiz has no questions, **When** the teacher tries to assign it to a student, **Then** the system blocks assignment and asks for at least one question.
7. **Given** the teacher has saved a quiz but assigned nobody, **When** any student opens the quizzes area, **Then** that quiz does not appear and cannot be taken.
8. **Given** the teacher assigns a quiz to student A and not student B, **When** each opens the quizzes area, **Then** only student A sees that quiz as available to take.
9. **Given** the teacher tries to assign a quiz to an account that is not a student, **When** they pick assignees, **Then** only promoted students can be selected.
10. **Given** at least one student has already submitted the quiz, **When** the teacher edits questions (add, remove, reorder, or change keys) and saves, **Then** the save succeeds; the live quiz used by students who have not started reflects those edits; already-submitted attempts are unchanged.

---

### User Story 2 - Assigned student takes a quiz (Priority: P1)

An assigned student opens the site’s quizzes area (the former “Prueba de nivel” destination). They see only the quizzes the teacher assigned to them, pick one, answer every question in order — including any listening media and any written responses — and submit once. Auto-gradable answers are scored immediately. Free-text / writing answers wait for the teacher. The student sees a result for that quiz (immediate if everything was auto-gradable; awaiting teacher if any question needs review). Homework, units, and class bookings are unchanged.

**Why this priority**: This is the student-facing replacement for the placement test and the reason quizzes exist. It can be demonstrated as soon as one quiz is assigned to a student.

**Independent Test**: As a student assigned a mixed-skill quiz, open the quizzes area, take it, submit, and confirm auto questions show immediate feedback and any writing answers are stored for the teacher — without that quiz appearing as homework.

**Acceptance Scenarios**:

1. **Given** a logged-out visitor, **When** they open the quizzes area, **Then** they are asked to log in or register before seeing quizzes.
2. **Given** a logged-in student with no quiz assignments, **When** they open the quizzes area, **Then** they see an empty quizzes list (labelled as quizzes — never as “prueba de nivel” or “placement test”), not a catalogue of every quiz.
3. **Given** a logged-in account that is not a student, **When** they open the quizzes area, **Then** they cannot take any quiz (none are assignable to them).
4. **Given** a student assigned a mixed-skill quiz, **When** they open it, **Then** they see all questions in the teacher’s single list order, with the right controls for each kind (including listening playback where relevant and a writing box for free-text), and a skill heading may appear when the skill changes between consecutive questions.
5. **Given** the assigned student has answered the quiz, **When** they submit, **Then** the system accepts one submission for the whole quiz, auto-grades structured questions, and stores free-text answers for the teacher.
6. **Given** a quiz that is entirely auto-gradable, **When** the assigned student submits, **Then** they immediately see per-question feedback (including the correct answer for questions they got wrong), an overall percentage plus fully-correct count, and a percentage plus fully-correct count for each skill that appears in the quiz, with no teacher step.
7. **Given** a quiz that includes any free-text / writing question, **When** the assigned student submits, **Then** auto questions may show immediate feedback and any all-auto skills may show their skill %, but the overall result and any skill that still has unscored free-text stay “submitted / awaiting teacher” until every free-text answer has a teacher percentage, after which the student sees the combined overall and per-skill results.
8. **Given** the assigned student tries to submit with required questions unanswered, **When** they submit, **Then** the system blocks submit and asks them to complete those questions.
9. **Given** the assigned student already submitted a quiz, **When** they open it again, **Then** they see their result (or awaiting-teacher status), including overall and per-skill figures once available, against the questions they submitted — even if the teacher has since edited the live quiz — and cannot submit a second attempt.
10. **Given** a student viewing homework, units, or class progress, **When** they look for the quiz they took, **Then** it does not appear as a homework item or unit activity.
11. **Given** student A has submitted and the teacher then edits the quiz, **When** assigned student B (who has not started) opens it, **Then** student B sees the current edited questions, not student A’s submitted set.

---

### User Story 3 - Teacher reviews quiz submissions (Priority: P2)

Paula opens a quiz in admin and sees who has submitted it. For auto-only quizzes she can inspect answers, the overall score, and the per-skill breakdown. For quizzes with writing or other free-text questions she assigns a 0–100% on each free-text answer (same idea as homework), optionally annotates and leaves a short note, and finalizes so the student sees the combined overall and per-skill result.

**Why this priority**: Mixed and writing quizzes cannot finish without teacher review. Auto-only quizzes already deliver value from User Story 2; this story completes the writing/listening-plus-writing path.

**Independent Test**: Submit a mixed quiz as an assigned student (at least one auto question and one writing question, in different skills); as teacher, score the writing answer, finalize, and confirm the student then sees the combined overall percentage and a per-skill breakdown.

**Acceptance Scenarios**:

1. **Given** at least one student has submitted a quiz, **When** the teacher opens that quiz’s results, **Then** they see each submission with student name, status (graded vs awaiting review), overall scores once available, and a breakdown by skill for the skills used in that quiz.
2. **Given** a submission with free-text answers, **When** the teacher assigns a percentage on every free-text answer and finalizes, **Then** the overall quiz result is combined the same way as mixed homework (equal average of auto 0/100 and teacher %; fully-correct count only at 100%), each skill’s result is computed the same way over only that skill’s questions, and the student can see both.
3. **Given** a submission that is entirely auto-gradable, **When** the teacher opens it, **Then** it is already graded and needs no teacher action to be final.
4. **Given** the teacher tries to finalize while any free-text answer still has no percentage, **When** they save as final, **Then** the system blocks finalization.
5. **Given** a student profile in admin, **When** the teacher looks for placement-test CEFR results, **Then** that placement result block is gone; quiz results are visible as quizzes, not as a “prueba de nivel”.
6. **Given** the teacher has edited the live quiz after student A submitted, **When** the teacher opens student A’s submission, **Then** they review the questions and answers from A’s submitted set, not the current live quiz.

---

### User Story 4 - Placement test (“prueba de nivel”) is fully retired (Priority: P1)

Every student-facing, teacher-facing, and public mention of the placement test / “prueba de nivel” is gone. Navigation, the old test page, the admin tab, student-profile placement results, and help/copy no longer use that name or that product. Because there is no production data, existing placement content and attempts can be discarded rather than migrated. Bookmarks to the old test address take people to the new quizzes area.

**Why this priority**: The request is an explicit terminology and product replacement, not a rename layered on the old test. Shipping quizzes while leaving “prueba de nivel” in the product would fail the requirement.

**Independent Test**: Search the live site (navigation, admin, student profile, quizzes area, and the old test address) and confirm no “prueba de nivel” / “placement test” product copy remains; the old address lands on quizzes.

**Acceptance Scenarios**:

1. **Given** a visitor or logged-in user, **When** they look at main navigation, **Then** they see a Quizzes entry instead of “Prueba de nivel” / “Placement test”.
2. **Given** someone opens the former placement-test address, **When** the page loads, **Then** they are taken to the quizzes area (not a leftover placement-test page).
3. **Given** the teacher opens the admin panel, **When** they look at tabs, **Then** they see Quizzes instead of “Prueba de nivel”, and the old placement question-bank / CEFR-threshold / bank-transfer / writing-evaluation tools are gone.
4. **Given** a student’s admin profile, **When** the teacher views progress, **Then** there is no “has not completed the placement test” / CEFR-from-placement block.
5. **Given** the quizzes feature is live, **When** anyone uses the site in Spanish, English, or Romanian, **Then** user-visible product copy uses Quiz / Quizzes (or the language’s equivalent of that term) and does not say “prueba de nivel”, “placement test”, or equivalent leftover names.

---

### Edge Cases

- What happens if the teacher unassigns a student after they have started or submitted? The quiz disappears from that student’s available list if they have not submitted; if they already submitted they can still open their own result. New takes by that student are blocked.
- What happens if the teacher deletes a quiz? The quiz, its assignments, and its submissions are removed. There is no production placement data to preserve, and quizzes are new.
- What happens if the teacher edits questions after a student has submitted? The teacher may keep editing. Already-submitted attempts keep the questions, answers, and scores as they were at submit time. Students who have not started yet see the current live questions. A student who has started but not submitted keeps the question set they started with until they submit (so items do not vanish mid-take).
- What happens if two assigned students take the quiz at different times and the teacher edited in between? Each attempt is scored and reviewed against its own captured question set. Overall and per-skill results are comparable only when the captured sets match.
- How does the system handle a quiz that uses only one skill (e.g. grammar-only)? That is valid — mixing skills is allowed, not required. A single skill heading (or none) is enough; there is still no section object. The result shows overall figures and the same figures again for that one skill (no empty skill rows).
- What happens to a skill that has only free-text questions? That skill’s % stays awaiting teacher until those answers are scored; other all-auto skills on the same attempt may show their % immediately.
- How does the system handle a listening question with missing media? Same rule as listening homework: the question cannot be saved, and the quiz cannot be assigned, without a media source on that listening question.
- What happens if a student loses connection mid-quiz? They can reopen and continue until they submit; there is no timer in v1. Partial answers should be kept if the student navigates away and returns before submit (best-effort draft); if a draft cannot be kept, they restart the unanswered quiz — see Assumptions.
- What happens to old placement attempts in local/dev databases? They are dropped with the placement product; nothing is migrated. Production has none.
- Do quizzes affect homework due dates, unit completion, or class attendance? No.
- Can a non-student take a quiz? No — only promoted students can be assignees, and only assignees can take a quiz.

## Requirements *(mandatory)*

### Functional Requirements

#### Retire placement test

- **FR-001**: The product MUST remove the placement test / “prueba de nivel” as a feature. Students, teachers, and visitors MUST NOT see that name or that flow anywhere in the live product (navigation, pages, admin, profiles, empty states, and emails that currently mention it).
- **FR-002**: The former placement-test address MUST send the user to the quizzes area rather than rendering a placement-test page.
- **FR-003**: Placement-specific capabilities MUST NOT return under the quiz name: no CEFR auto-banding, no per-skill timed sections, no offline bank-transfer evaluation, no dedicated speaking audition appointment, and no placement question-bank-by-skill admin. Production holds no placement data, so existing placement records MUST be discarded rather than migrated.

#### Quizzes as a separate assessment type

- **FR-004**: The teacher MUST be able to create, edit, assign to students, unassign, and delete **quizzes**, including after some students have submitted. A quiz is a standalone assessment — it MUST NOT be a homework, MUST NOT be attached to a class unit, and MUST NOT appear in homework lists or unit sequences.
- **FR-005**: A quiz MUST contain a single ordered list of questions. Each question MUST have a skill of reading, writing, grammar, or listening. The list MAY mix those skills in any combination and order (including a single skill only). The product MUST NOT model quizzes as named skill sections; when consecutive questions share a skill, the take view MAY show a heading for that skill.
- **FR-006**: Quiz questions MUST use the same question kinds the platform already supports for homework (auto-gradable structured kinds and free-text / writing). Quizzes MUST NOT invent a parallel set of question types; behaviour for authoring, answering, auto-grading, answer-key hiding before submit, and teacher scoring of free-text MUST match homework for that kind.
- **FR-007**: Listening questions on a quiz MUST support the same media sources and student playback behaviour as listening homework.
- **FR-008**: The teacher MUST assign a quiz to one or more specific promoted students. Only those assigned students MUST be able to take that quiz. Unassigned students MUST NOT see it as available.
- **FR-009**: Assigning a quiz to a student MUST require the quiz to have at least one question. Saving a quiz with no questions and no assignees MAY be allowed.

#### Taking and scoring

- **FR-010**: A logged-out visitor MUST be required to log in or register before seeing quizzes. Only a promoted student who is explicitly assigned to a quiz MUST be able to take that quiz. A logged-in non-student MUST NOT be able to take quizzes. A student MUST see only quizzes assigned to them — never a catalogue of all quizzes.
- **FR-011**: Each assigned student MUST be allowed exactly one submission per quiz. After submit, that attempt MUST be read-only. The student MUST NOT retake, resubmit, or start a second attempt. The teacher MUST NOT reset an attempt to allow another take in this feature.
- **FR-012**: On submit, auto-gradable questions MUST be scored immediately using the same scoring rules as homework. The result MUST include (a) an overall percentage and fully-correct count for the whole quiz, and (b) a percentage and fully-correct count for each skill that has at least one question in the quiz. Skills with no questions MUST be omitted. Answer keys MUST stay hidden until submit. Results MUST NOT use CEFR bands.
- **FR-013**: If the quiz has any free-text / writing question, the overall result MUST stay awaiting teacher until every such question has a teacher percentage; then the overall combined result MUST follow the same mixed-homework combination rules. A skill that still has unscored free-text MUST stay awaiting on that skill’s breakdown; a skill whose questions are all auto-gradable MUST show its skill result immediately. All-auto quizzes MUST become graded on submit with no teacher step.
- **FR-014**: Assigned students MUST see their own quiz status and result (assigned/available, in progress, awaiting teacher, graded), including overall and per-skill figures once available, in the quizzes area.

#### Teacher results and copy

- **FR-015**: The teacher MUST be able to list submissions per quiz and review/score free-text answers (percentage, optional annotate, optional short note) using the same review tools as homework. The teacher MUST see the same overall and per-skill result the student will see, against the questions that student submitted (not a later live edit).
- **FR-016**: Student-facing and teacher-facing copy in Spanish, English, and Romanian MUST use Quiz terminology (e.g. Spanish navigation “Quiz” / “Quizzes” replacing “Prueba de nivel”). No leftover placement-test strings.
- **FR-017**: Homework authoring, assignment via units, student homework take/submit/grade, and presentations MUST remain unchanged by this feature.
- **FR-018**: Each quiz attempt MUST capture the question set the student took. A submitted attempt MUST keep that captured set for scoring, review, and the student’s result view even if the teacher later edits the live quiz. Students who have not started MUST see the current live questions. A student who has started but not submitted MUST keep the question set they started with until submit.

### Key Entities *(include if feature involves data)*

- **Quiz**: A standalone assessment with a title, optional description, ordered questions, and student assignees. Not a homework and not part of a unit. Availability is per assigned student, not a global published catalogue.
- **Quiz question**: One item in a quiz’s single ordered list. Has a required skill (reading, writing, grammar, or listening), a question kind shared with homework, prompt/media/options/answer key as that kind requires, and position in the quiz. Not grouped into a separate section entity.
- **Quiz assignment**: The link between one quiz and one promoted student. Only assigned students may take that quiz. The teacher adds or removes assignments.
- **Quiz attempt**: One assigned student’s single run of one quiz. Captures the question set the student took (from start through submit), plus answers, auto scores, teacher scores for free-text, status (in progress, submitted/awaiting teacher, graded), an overall result, and a per-skill result for each skill in that captured set. Later edits to the live quiz do not rewrite this attempt.
- **Skill**: One of reading, writing, grammar, listening — a required label on each quiz question so a quiz can combine those tests in one list and report a breakdown by skill. Skills do not create section objects, do not split the quiz into separately timed parts, and do not produce CEFR levels.
- **Homework** (existing, unchanged): Coursework assigned through units. Quizzes must not create, reuse, or appear as homework records.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can create a mixed-skill quiz (at least two of reading, writing, grammar, listening) and assign it to a chosen student in one session, without creating a homework.
- **SC-002**: An assigned student can find that quiz from the main navigation, complete it, and submit in a single sitting, with auto-gradable feedback visible within 3 seconds of submit. A student who was not assigned cannot take it.
- **SC-003**: 100% of identical auto-gradable answer sets on the **same captured question set** produce the same overall percentage, fully-correct count, and per-skill percentages.
- **SC-004**: After this feature ships, a site-wide copy review finds **zero** user-visible occurrences of “prueba de nivel”, “placement test”, or the old placement-test tab/page as a product name.
- **SC-005**: Opening the former placement-test address lands the user on the quizzes area in one navigation, with no extra dead-end page.
- **SC-006**: A student (or teacher reviewing that student) never sees a quiz listed as homework or as a unit activity.
- **SC-007**: A quiz that includes writing reaches a final overall result only after the teacher scores every written answer; an all-auto quiz never waits on the teacher. After finalization, both overall and per-skill figures are visible to the student and the teacher.
- **SC-008**: Existing homework take/submit/grade flows still complete successfully with no change in student or teacher steps.

## Assumptions

- **Product replacement, not a rename of the old test**: Quizzes replace the placement test. The old one-off test (fixed Reading / Audio / Grammar timed sections, CEFR bands, bank-transfer full evaluation, writing prompt + live speaking appointment) is removed. A teacher who still wants a “level” check can create a quiz that mixes those skills and assign it to the students who should take it; the product no longer runs a special placement engine.
- **No production data to migrate**: The production database has no placement-test rows. Local/dev placement data may be dropped.
- **Explicit assignment, not an open catalogue**: A quiz is available only to the promoted students the teacher assigns. It is not attached to a class unit and is not a homework. Non-students never receive assignments. There is no separate global “publish to everyone” switch in this feature — assigning a student is what makes the quiz available to them.
- **One attempt per assigned student per quiz**: Confirmed — submit once, then read-only. No student retake and no teacher reset in this feature.
- **No timers in this feature**: Quizzes are not timed. Timed per-skill sections were placement-specific and are not carried over.
- **Grading parity with homework**: Auto vs manual per question, combined percentage, fully-correct only at 100%, answer keys hidden until submit, optional annotate + short note on free-text — all reused conceptually for the **overall** quiz result and again for each **skill subset**. Quizzes add a per-skill breakdown; they do not change homework’s own rules and they do not report CEFR.
- **Draft save vs assign**: Teachers may save an incomplete quiz; assigning a student requires at least one question.
- **Edits after submissions**: Confirmed — the teacher may keep editing questions after students have submitted. Each attempt keeps the question set the student started with through submit; later live edits apply only to students who have not started yet. New assignees may still be added and take the current live quiz.
- **In-progress drafts**: Best-effort keep answers if the assigned student leaves and returns before submit; no server-side exam timer.
- **User-facing term**: The product term is **Quiz** (plural **Quizzes**) in Spanish, English, and Romanian UI, replacing “Prueba de nivel” / “Placement test”. No separate Spanish coinage (“cuestionario”, “prueba”) unless later copy-editing decides otherwise.
- **Old address**: The former placement-test path redirects to the quizzes area so existing links and bookmarks do not 404.
- **Out of scope**: attaching quizzes to units as homework; quiz due dates; CEFR reporting; certificates; class booking tied to a quiz; speaking as a quiz skill (listening/reading/writing/grammar only); anonymous (logged-out) taking; an open catalogue of quizzes for every logged-in user; retakes or teacher-reset attempts.
