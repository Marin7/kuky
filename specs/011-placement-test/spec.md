# Feature Specification: Placement Test for Potential Students

**Feature Branch**: `011-placement-test`

**Created**: 2026-06-30

**Status**: Draft

**Input**: User description: "I want to create a difficulty test for potential students. A page with a proper test for all 5 types: Reading, Audio and Grammar (all 3 auto-correctible) and a separate section for writing and speaking which will require an account to be created and a separate payment to be made (bank transfer for now)."

## Clarifications

### Session 2026-06-30

- Q: How is the speaking response captured for the paid evaluation? → A: A short scheduled live session between student and teacher (reusing the existing booking / live-meeting flow), not an asynchronous recording.
- Q: When a visitor takes the free auto-graded test and later pays, how is their result tied into the combined paid evaluation? → A: Require login first — a visitor must create/log in to an account before taking any section, so every attempt is already owned by an account (no anonymous attempts, no claiming).
- Q: Should the auto-graded test (Reading / Audio / Grammar) be timed? → A: Per-section time limits — each section is independently timed and auto-submits when its limit expires.
- Q: How should unanswered auto-graded questions be handled at submission? → A: Warn, then allow — on manual submit, show a confirmation listing unanswered questions; once confirmed (or when a section's timer expires), blanks are scored as incorrect.
- Q: What proficiency result should the auto-graded test report? → A: Per-skill CEFR levels — a separate A1–C2 level for each of Reading, Listening, and Grammar, plus a derived overall estimate.
- Q: Should the app handle the Writing/Speaking payment in-app (orders, statuses, admin confirmation)? → A: No — payment is fully offline; the app only shows static bank-transfer instructions and tracks no order, status, reference, or amount.
- Q: What gates access to the Writing test and the evaluation appointment? → A: Nothing — fully trust-based; any logged-in student can submit the Writing test and book a normal appointment, and the teacher enforces payment offline.
- Q: How are the speaking audition and the results delivered? → A: In one normal booked appointment (existing `/reservas` flow) where the teacher conducts the live audition, presents the Writing results, and discusses the overall outcome.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Free self-assessment of Spanish level (Priority: P1)

A prospective student who is unsure of their Spanish level creates a free account, opens a dedicated test page, works through the Reading, Listening (Audio), and Grammar sections — each independently timed — and immediately receives a per-skill CEFR level (one A1–C2 level per skill) plus a derived overall estimate. This gives them a concrete reason to consider booking classes and shows the teacher's professionalism.

**Why this priority**: This is the lead-generation core of the feature and the only part that delivers value entirely on its own. Auto-graded sections require no human effort and can be shipped, measured, and demonstrated independently. Without it, the paid sections have nothing to attach to.

**Independent Test**: Can be fully tested by registering/logging in, answering the three timed auto-graded sections, submitting, and confirming per-skill CEFR levels and score breakdown are shown instantly — no payment, no teacher involvement.

**Acceptance Scenarios**:

1. **Given** a logged-in user on the test page, **When** they answer all Reading, Audio, and Grammar questions and submit, **Then** the system shows a per-skill CEFR level (Reading, Listening, Grammar), a derived overall estimate, and a per-section score breakdown immediately.
2. **Given** a user with one or more unanswered questions, **When** they submit a section manually, **Then** the system warns them and lists the unanswered questions, and on confirmation submits the section with those blanks scored as incorrect.
3. **Given** a logged-in user taking a timed section, **When** the section's time limit expires, **Then** the section auto-submits with answered questions graded and unanswered ones scored as incorrect, without a warning prompt.
4. **Given** a user who has just received their auto-graded result, **When** they view the result, **Then** they are presented with a clear invitation to start the full Writing + Speaking evaluation for a more complete, teacher-led assessment.
5. **Given** an Audio question, **When** the user plays the associated clip, **Then** the audio plays reliably and can be replayed before answering.
6. **Given** a logged-out visitor, **When** they open the test page, **Then** they are required to register / log in before starting any section.

---

### User Story 2 - Pursue the full evaluation: pay offline, submit Writing, book an appointment (Priority: P2)

A logged-in student who wants the full evaluation reads the bank-transfer instructions shown on the page, makes the transfer entirely outside the app, completes the Writing test in-app, and books a normal appointment with the teacher through the existing scheduling flow. The app collects and tracks no payment — the transfer is handled offline and the teacher verifies it herself.

**Why this priority**: This is the path to revenue (which arrives via the offline transfer) and the bridge to the live evaluation. It depends on the free test (P1) and on the existing account and booking systems. It is deliberately lightweight — no in-app payment machinery.

**Independent Test**: Can be tested by, as a logged-in student, viewing the static bank-transfer instructions, submitting a Writing response (stored and visible to the teacher), and booking an appointment through the normal flow — with no payment step anywhere in the app.

**Acceptance Scenarios**:

1. **Given** a logged-in student who finished the auto-graded test, **When** they open the full-evaluation section, **Then** they see static bank-transfer payment instructions and a way to submit the Writing test.
2. **Given** a logged-in student on the Writing section, **When** they write and submit their response, **Then** it is stored and becomes visible to the teacher.
3. **Given** a logged-in student, **When** they choose to book the evaluation appointment, **Then** an appointment is created through the existing normal booking flow, identical to booking a regular class.
4. **Given** any logged-in student using the full-evaluation section, **When** they submit Writing or book the appointment, **Then** the app neither collects nor records any payment (no order, status, reference, or amount) and imposes no payment-based gate.

---

### User Story 3 - Teacher delivers the evaluation in a live appointment (Priority: P2)

Ahead of the booked appointment, the teacher reviews the student's submitted Writing and their auto-graded per-skill results in the admin area. During the live appointment she conducts the speaking audition, presents the Writing results, and discusses the overall outcome with the student.

**Why this priority**: This realizes the value the student paid for (offline) and is what differentiates the full evaluation from the free test. It depends on US2 (Writing submitted and appointment booked).

**Independent Test**: Can be tested by, as the teacher, opening a student's evaluation in the admin area and seeing their auto-graded per-skill results and Writing submission together, then holding the booked appointment to conduct the audition and present results.

**Acceptance Scenarios**:

1. **Given** a student who submitted the Writing test and completed the auto-graded test, **When** the teacher opens that student's evaluation in the admin area, **Then** she sees the per-skill CEFR results and the full Writing submission together in one place.
2. **Given** a booked evaluation appointment, **When** the teacher holds it, **Then** she conducts the live speaking audition and presents the Writing and overall results to the student during the session.
3. **Given** the evaluation appointment is a normal booking, **When** it is scheduled, rescheduled, or cancelled, **Then** it behaves exactly like a regular class booking (same rules and notifications).

---

### Edge Cases

- What happens when a user refreshes the page or loses connection during a timed section? (The section timer is tracked server-side and keeps running; on return the user resumes with the remaining time, and if it has elapsed the section is treated as auto-submitted — see Assumptions.)
- What happens when a section's time limit expires? (The section auto-submits: answered questions are graded, unanswered ones are scored as incorrect, with no warning prompt.)
- How does the system handle a student who books the evaluation appointment without having paid? (Not enforced in-app — the teacher verifies the bank transfer offline before holding the audition; access is trust-based.)
- How does the system handle a student who submits the Writing test but never books an appointment? (The submission simply waits, visible to the teacher; nothing happens automatically.)
- What happens if the student misses or needs to reschedule the evaluation appointment? (It behaves exactly like a normal class booking — same rescheduling/cancellation rules and notifications.)
- How are ties or borderline scores between two levels resolved in the auto-graded estimate? (A documented, deterministic banding rule applies.)
- What happens if the same account takes the free test multiple times? (Each attempt produces its own result; no limit enforced in v1.)

## Requirements *(mandatory)*

### Functional Requirements

#### Free auto-graded test

- **FR-001**: The system MUST require an authenticated account before a user can take any section of the test; a logged-out visitor MUST be directed to register / log in first. The three auto-graded sections — Reading, Audio (Listening), and Grammar — are free of charge (no payment) once logged in.
- **FR-002**: The system MUST support auto-gradable question formats for these sections (e.g., single-choice, multiple-choice, and fill-in-the-blank), consistent with the platform's existing exercise capabilities.
- **FR-003**: Audio questions MUST let the user play (and replay) an audio clip before answering.
- **FR-004**: Upon completion of the auto-graded sections, the system MUST compute and display a per-skill CEFR level (a separate A1–C2 level for each of Reading, Listening, and Grammar), a derived overall estimate, and a per-section score breakdown immediately, without human involvement.
- **FR-005**: Each per-skill CEFR level MUST be derived from a deterministic, documented scoring/banding rule so the same answers always yield the same levels.
- **FR-006**: Each auto-graded section MUST be independently timed with its own time limit and a visible countdown; when a section's time limit expires it MUST auto-submit, grading answered questions and scoring unanswered ones as incorrect, with no warning prompt. The deadline itself MUST be computed and stored server-side (`started_at + limit`) so that refreshing or editing the client cannot extend it; a submit received after the deadline is still accepted and grades exactly the answers sent, scoring the rest as incorrect (this is a self-assessment lead-in, not a proctored exam — the guarantee is "no extra time," not anti-cheating).
- **FR-007**: On a manual section submit with one or more unanswered questions, the system MUST warn the user and list the unanswered questions, and only on confirmation submit the section with those blanks scored as incorrect.
- **FR-008**: After showing the auto-graded result, the system MUST present a clear call to action to start the full Writing + Speaking evaluation.

#### Full evaluation (offline payment, Writing, live appointment)

- **FR-009**: The system MUST display static bank-transfer payment instructions for the full evaluation; it MUST NOT collect, process, or integrate any payment method in-app.
- **FR-010**: Any logged-in student MUST be able to submit a written response to the Writing prompt; the response MUST be stored and made visible to the teacher. Submission MUST NOT be gated by any payment check (trust-based access).
- **FR-011**: A logged-in student MUST be able to book the evaluation appointment through the platform's existing normal booking flow, identical to booking a regular class; booking MUST NOT be gated by any payment check.
- **FR-012**: The system MUST NOT track any payment order, status, reference, or amount for the evaluation; receipt of the bank transfer is verified by the teacher offline.
- **FR-013**: The evaluation results MUST be delivered during the booked appointment, in which the teacher conducts the live speaking audition, presents the Writing results, and discusses the overall outcome. The app's role is to make the auto-graded results and the Writing submission available to the teacher before and during the appointment.

#### Administration

- **FR-014**: The teacher/admin MUST be able to author the auto-graded questions for Reading, Audio (Listening), and Grammar, and edit the Writing prompt and the bank-transfer instructions text. Speaking has no authored content — it is conducted live during the appointment.
- **FR-015**: The teacher/admin MUST be able to view, for a given student, their auto-graded per-skill results together with their Writing submission, and to see the list of students who have submitted a Writing response.

### Key Entities *(include if feature involves data)*

- **Placement Test**: The overall assessment definition; groups the five sections and the rules used to estimate a level.
- **Section**: One of Reading, Audio (Listening), Grammar, Writing, Speaking; carries its question/prompt set, whether it is auto-graded or human-reviewed, and (for auto-graded sections) its time limit.
- **Question / Item**: An individual auto-graded item (Reading/Audio/Grammar) with its options and correct answer; or a prompt (Writing/Speaking) the student responds to.
- **Test Attempt**: A single run-through by a logged-in user; holds their answers/responses, per-section timing state (start time, remaining/elapsed time, submitted), and links to the resulting evaluation.
- **Auto-graded Result**: The computed score per section and the derived per-skill CEFR levels (Reading, Listening, Grammar) plus an overall estimate for an attempt.
- **Written Response**: The student's submitted written artifact for the Writing section, visible to the teacher; no payment gate or review-status machinery in v1.
- **Evaluation Appointment**: The booked live session where the teacher conducts the speaking audition and presents results — a normal booking that reuses the platform's existing appointment/booking entity (not a new entity, and not gated by payment in-app).
- **Account / User**: The existing student account; every Test Attempt and Written Response is owned by an account. The teacher acts via the existing admin role.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time user can complete the three timed auto-graded sections within their per-section limits (total typically under 20 minutes) and immediately see their per-skill CEFR levels.
- **SC-002**: The auto-graded result appears within 3 seconds of submission, with no manual step.
- **SC-003**: 100% of identical answer sets produce the same per-skill CEFR levels (deterministic scoring).
- **SC-004**: A user can go from their auto-graded result to seeing the bank-transfer instructions and submitting the Writing test in no more than 3 steps.
- **SC-005**: The full-evaluation flow collects no payment in the app — there is no in-app payment step, order, or status to build or maintain.
- **SC-006**: A logged-in student can submit the Writing test and book the evaluation appointment with no payment-based gate blocking them.
- **SC-007**: Before and during the appointment, the teacher can view the student's per-skill CEFR results and full Writing submission together in one place.
- **SC-008**: The number of students who submit the Writing test or book an evaluation appointment after completing the free test is trackable (free → full-evaluation funnel).

## Assumptions

- **Proficiency scale**: Levels are expressed on the standard CEFR scale (A1–C2), the conventional framework for Spanish-language proficiency, reported per skill (Reading, Listening, Grammar) with a derived overall estimate. The exact band thresholds will be defined during planning.
- **Account required for the entire test**: A visitor must register / log in before taking any section; the three auto-graded sections are free of charge but not anonymous. Because every attempt is owned by an account, the paid Writing/Speaking evaluation links to the same account with no anonymous-attempt claiming.
- **Single full-evaluation offering**: The full evaluation (Writing test + live speaking audition + results discussion) is one offering, paid for by a single offline bank transfer. It is not split into separately sold Writing vs Speaking products.
- **Payment is fully offline**: No online/card payment integration and no in-app payment tracking in v1. The app only shows static bank-transfer instructions; the teacher verifies receipt herself before holding the audition.
- **Trust-based access**: Submitting the Writing test and booking the evaluation appointment are open to any logged-in student; there is no in-app gate tying them to payment. The teacher enforces payment offline.
- **Speaking + results delivered in one normal appointment**: The speaking audition and the presentation of Writing/overall results happen within a single regular booked appointment (existing `/reservas` flow). It is not an asynchronous recording and not a separate gated session; rescheduling/cancellation behave like any class booking.
- **Reuse of existing platform capabilities**: The feature reuses the existing account/auth system, the existing admin role for the teacher, the existing auto-graded exercise mechanics (single/multi choice, fill-blank), and the existing booking/appointment flow.
- **Results delivered live, no refunds in-app**: Results are presented live in the appointment rather than stored as an in-app written report; recording an in-app written assessment and any refund handling are out of scope for v1.
- **Per-section timing is server-authoritative**: Each auto-graded section has its own time limit tracked server-side; leaving or refreshing does not pause the timer, and an elapsed section is treated as auto-submitted. The specific limits per section will be set during planning.
- **Notifications**: Student and teacher notifications reuse the platform's existing email/notification mechanism.
