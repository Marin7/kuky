# Feature Specification: Student Testimonials on Homepage

**Feature Branch**: `014-homepage-testimonials`

**Created**: 2026-07-02

**Status**: Draft

**Input**: User description: "Add student written testimonials on the homepage"

## Clarifications

### Session 2026-07-02

- Q: Can a student submit more than one testimonial? → A: One active testimonial per student — submitting a new one replaces/resets their existing one, whether it was still pending or already published.
- Q: Can a student edit or withdraw their own testimonial after submitting it? → A: View-only — the student can see their own submission's current status but cannot edit or delete it directly; to change the text they submit again, which replaces/resets the existing one per the one-active-testimonial rule.
- Q: Should the teacher be notified when a new testimonial is submitted for review? → A: Yes — an email notification, matching the existing booking-notification pattern.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Prospective Student Reads Testimonials (Priority: P1)

A visitor evaluating whether to book classes with Paula scrolls the homepage and finds a section of short, written testimonials from real students describing their experience and progress. Reading a handful of genuine, specific endorsements increases the visitor's confidence to book a class.

**Why this priority**: Social proof directly supports the site's core conversion goal (booking a class). Without it, the homepage relies solely on Paula's own claims about her teaching.

**Independent Test**: Load the homepage as a first-time, unauthenticated visitor. Confirm a dedicated testimonials section is visible with at least one written testimonial attributed to a named student, without needing to log in or navigate elsewhere.

**Acceptance Scenarios**:

1. **Given** the homepage has at least one published testimonial, **When** a visitor loads the homepage, **Then** a testimonials section is visible showing the testimonial text and the student's attribution (name/display label).
2. **Given** multiple published testimonials exist, **When** a visitor views the section, **Then** they can see more than one testimonial (via scrolling, paging, or a carousel) without leaving the homepage.
3. **Given** no testimonials have been published yet, **When** a visitor loads the homepage, **Then** the testimonials section is hidden entirely (no empty or broken-looking section is shown).

---

### User Story 2 - Student Submits Their Own Testimonial (Priority: P2)

A current student who has taken classes with Paula wants to share their own feedback in their own words, so it can potentially be featured on the homepage.

**Why this priority**: This is how testimonial content enters the system at all — without submissions there is nothing for the teacher to curate or visitors to read.

**Independent Test**: As a recognized student, submit a short testimonial through the student area. Confirm it does not appear on the public homepage until the teacher reviews and approves it.

**Acceptance Scenarios**:

1. **Given** a logged-in user with recognized student status, **When** they submit a testimonial with text, **Then** the submission is recorded and queued for teacher review — it is not immediately public.
2. **Given** a submitted testimonial pending review, **When** the teacher approves it, **Then** it becomes eligible to display on the homepage per her ordering/curation choices.
3. **Given** a submitted testimonial pending review, **When** the teacher rejects it, **Then** it never appears publicly and the student is not misled into thinking it was published.

---

### User Story 3 - Teacher Curates Which Testimonials Appear (Priority: P3)

Paula wants control over which approved student testimonials are shown publicly and in what order, so the homepage always reflects her strongest, most relevant endorsements.

**Why this priority**: Builds on approval (already required by User Story 2) with finer control — ordering and the ability to unpublish something later. The site is still safe and functional without this refinement, since nothing unapproved is ever public.

**Independent Test**: As the teacher, review a pending testimonial, approve or reject it, then reorder or unpublish an already-approved one — then confirm the homepage reflects each change without involving any other system.

**Acceptance Scenarios**:

1. **Given** a pending student submission, **When** the teacher approves it, **Then** it becomes eligible to display on the homepage.
2. **Given** existing published testimonials, **When** the teacher changes their display order or marks one as unpublished, **Then** the homepage reflects the change immediately (next load).
3. **Given** a testimonial the teacher no longer wants shown, **When** she unpublishes or rejects it, **Then** it no longer appears on the homepage.

---

### Edge Cases

- What happens when a testimonial's text is very long? The homepage display MUST truncate or otherwise keep the layout readable without breaking the page.
- What happens when a student's account is later deleted or renamed? The published testimonial text MUST remain displayable using the attribution captured at submission time.
- How does the system handle attempts to submit empty or extremely short testimonial text? Submission MUST be rejected with a clear message (see FR-010).
- What happens when a student who already has an active testimonial submits a new one? The previous testimonial's text MUST be replaced and its status reset to pending (see FR-006) — it does not create a second entry.
- What happens on very small mobile screens with several testimonials? The section MUST remain readable and navigable (e.g., swipe/scroll) without layout breakage.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The homepage MUST display a dedicated testimonials section containing one or more published student testimonials, each showing the testimonial text and a student attribution label.
- **FR-002**: The homepage MUST hide the testimonials section entirely when zero testimonials are published, rather than showing an empty section.
- **FR-003**: The teacher MUST be able to review pending submissions, edit testimonial text, reorder, approve/reject, and unpublish/remove testimonials from her admin area.
- **FR-004**: The system MUST persist each testimonial's text, the submitting student's full name as attribution, publish/pending/rejected status, and display order.
- **FR-005**: The system MUST support the following testimonial lifecycle: a logged-in student submits testimonial text, which enters a pending state; the teacher reviews each pending submission and either approves it (making it eligible for public display) or rejects it (it is never shown publicly); the teacher may also unpublish a previously-approved testimonial at any time.
- **FR-006**: Each student MUST have at most one active testimonial. Submitting a new testimonial MUST replace/reset the student's existing one (pending or already published), re-entering the pending state for teacher review.
- **FR-007**: A student MUST be able to view the current status of their own testimonial (pending/approved/rejected) but MUST NOT be able to edit or delete it directly — changing the text requires submitting again, which replaces the existing one per FR-006.
- **FR-008**: The system MUST notify the teacher by email when a student submits a new testimonial for review, so pending reviews do not go unnoticed.
- **FR-009**: Only testimonials in an approved/published state MUST be visible on the public homepage; pending or rejected testimonials MUST NOT be visible to public visitors or to other students.
- **FR-010**: The system MUST reject testimonial text submissions that are empty or below a minimum readable length, with a clear error message shown to the submitting student.
- **FR-011**: The testimonials section MUST render correctly on both desktop and mobile screen widths, matching the responsive behavior of the rest of the homepage.
- **FR-012**: Removing or unpublishing a testimonial MUST take effect on the public homepage without requiring any change outside the teacher's normal content-management workflow.

### Key Entities

- **Testimonial**: A single piece of student feedback — testimonial text, student attribution (full name), associated student account (unique — at most one testimonial per student), status (pending/approved/rejected/unpublished), display order, and submission date.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time visitor can locate and read at least one student testimonial on the homepage within one scroll of arriving on the page.
- **SC-002**: The teacher can approve a pending testimonial and see it live on the homepage in under 2 minutes, without any technical assistance.
- **SC-003**: 100% of testimonials shown publicly are ones the teacher has explicitly approved — no unreviewed content ever reaches the public homepage.
- **SC-004**: The testimonials section renders without layout issues across screen widths from 375px (small phone) to 1440px (desktop).
- **SC-005**: When zero testimonials exist, 100% of homepage loads show no trace of a broken or empty testimonials section.

## Assumptions

- The teacher (Paula) already has an admin area where she manages other public-facing content (e.g., presentations, homework); testimonial review/curation follows the same trusted-admin pattern.
- A student submitting a testimonial is treated as consenting to their full name being displayed publicly if the teacher approves it; no separate consent step is required beyond the act of submission.
- A "reasonable minimum" testimonial length (e.g., a short sentence) is enough to prevent low-effort/spam submissions; the exact character threshold is an implementation detail, not a business rule requiring specification here.
- Testimonials do not include numeric star ratings or aggregate scoring in this phase — only written text and attribution.
- Translation/localization of testimonial text is out of scope; testimonials are displayed as submitted (expected to be in Spanish or Romanian, matching the site's existing bilingual content).
- No public-facing testimonial submission form (i.e., non-students/anonymous visitors, or registered-but-not-yet-recognized users) is in scope — submission is limited to users the teacher has already recognized as students (the same population who can book classes and access learning materials); only the teacher may approve, reject, reorder, or unpublish testimonials.
