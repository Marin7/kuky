# Feature Specification: Spanish Teacher Presentation Site

**Feature Branch**: `001-teacher-landing-page`

**Created**: 2026-06-08

**Status**: Draft

**Input**: User description: "Create an initial landing page with all the assets I've already placed in the repository, generated from Lovable. This will start as a website for a Spanish teacher, named Paula, just a presentation page, but it will evolve by adding Account Management, Classes/Materials that the students can work on, Scheduling assistant for scheduling classes when the teacher is available (using Calendly)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - First Impression (Priority: P1)

A Romanian student visiting the site for the first time lands on the homepage and
immediately understands who Paula is, what she offers, and why the service is
specifically designed for them. They see Paula's photo, a clear headline about
Spanish lessons for Romanian speakers, and a primary call-to-action to book a
class.

**Why this priority**: This is the core purpose of the entire site. Without a
compelling first impression, every other feature is irrelevant.

**Independent Test**: Open the homepage on a fresh browser session. A tester
unfamiliar with the site should be able to answer "Who is this for?", "What is
being offered?", and "What should I do next?" within 5 seconds — without
scrolling.

**Acceptance Scenarios**:

1. **Given** a visitor arrives at the homepage, **When** the page loads,
   **Then** they see Paula's name, her photo, a headline mentioning Spanish
   lessons for Romanian students, and at least two CTAs (book a class / learn
   more about Paula).

2. **Given** a visitor views the features section, **When** they scroll past
   the hero, **Then** they see three key differentiators: personalised method,
   Romanian-specific approach, and included materials.

3. **Given** a visitor reaches the bottom CTA section, **When** they read it,
   **Then** they see a prominent "book now" action that leads toward the
   scheduling flow.

---

### User Story 2 - Learn About the Teacher (Priority: P2)

A prospective student wants to build trust before committing to a class. They
navigate to the "About me" page to read Paula's background, teaching philosophy,
levels covered, and other reassuring details.

**Why this priority**: Trust is the second barrier after initial discovery.
Students need to feel confident in their teacher before booking.

**Independent Test**: Navigate to the About Me page directly. A tester should
be able to confirm Paula's teaching levels, the class modality (online), the
languages spoken, and her general teaching approach — all without leaving
that page.

**Acceptance Scenarios**:

1. **Given** a visitor clicks "About Me" in the navigation or the homepage
   CTA, **When** the page loads, **Then** they see Paula's photo, a personal
   introduction, and a summary panel of key facts (levels, modality, languages,
   focus).

2. **Given** a visitor reads the bio, **When** they finish, **Then** the page
   includes a clear path forward (e.g., link to book a class).

---

### User Story 3 - Discover Future Features (Priority: P3)

A visitor navigating the site encounters the navigation bar and placeholder pages
for Classes, Bookings, and Account. Even though these sections are not yet
functional, the visitor understands that these capabilities are coming and the
site has a clear roadmap.

**Why this priority**: Sets expectations and reduces confusion — visitors should
not feel they've hit a dead end when clicking navigation items.

**Independent Test**: Click each navigation item (Classes, Bookings, Account).
Each placeholder page should clearly communicate the section's purpose and that
it is coming soon or requires a future step.

**Acceptance Scenarios**:

1. **Given** a visitor clicks "Classes" in the navigation, **When** the page
   loads, **Then** they see a placeholder that names the section and describes
   what will be available there.

2. **Given** a visitor clicks "Bookings", **When** the page loads, **Then**
   they see a placeholder describing the scheduling feature (Calendly
   integration coming).

3. **Given** a visitor clicks "My Account", **When** the page loads, **Then**
   they see a placeholder describing the account management feature.

---

### Edge Cases

- What happens when the site is visited on a small mobile screen — does the
  hero layout remain readable and the navigation accessible?
- How does the site behave when Paula's photo fails to load — is there a
  graceful fallback?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The site MUST display Paula's name and photo prominently on the
  homepage hero section.
- **FR-002**: The homepage MUST communicate that the service targets Romanian
  students learning Spanish.
- **FR-003**: The homepage MUST include at least two distinct calls-to-action:
  one leading toward class booking and one leading to the About Me page.
- **FR-004**: The homepage MUST list the three key service differentiators
  (personalised method, Romanian-specific approach, included materials).
- **FR-005**: The site MUST include a persistent navigation bar with links to
  all five sections: Home, About Me, Classes, Bookings, My Account.
- **FR-006**: The About Me page MUST display Paula's teaching levels (A2–C2),
  class modality (online), languages (Spanish and Romanian), and pedagogical
  focus (conversation and grammar).
- **FR-007**: The Classes, Bookings, and My Account pages MUST each display a
  clearly labelled placeholder that names the section and describes its future
  purpose.
- **FR-008**: Every page MUST include a footer with copyright and site identity.
- **FR-009**: The site MUST be usable on both desktop and mobile screen sizes.

### Key Entities

- **Teacher Profile**: Paula's identity — name, photo, bio, teaching levels,
  class modality, languages, teaching focus.
- **Service Differentiators**: Three value propositions shown on the homepage
  (cards with title and description).
- **Navigation Links**: Five top-level sections (Home, About Me, Classes,
  Bookings, My Account) — three of which are placeholders in this phase.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time visitor can identify the site's purpose (Spanish
  lessons tailored for Romanian students) within 5 seconds of the homepage
  loading, without scrolling.
- **SC-002**: A visitor can reach Paula's full bio in a single navigation step
  from the homepage.
- **SC-003**: Every page (including placeholders) contains at least one visible
  call-to-action or onward navigation link.
- **SC-004**: The site loads and renders correctly on screen widths from 375 px
  (small phone) to 1440 px (desktop).
- **SC-005**: All five navigation links are accessible and lead to the correct
  pages with no broken routes.

## Assumptions

- The primary audience is Romanian-speaking students at Spanish levels A2 through
  C2; the site language is Spanish.
- Classes are conducted entirely online — no in-person sessions are in scope.
- Paula's photo (`teacher.jpg`) is the only media asset for this phase.
- Calendly integration is planned for the Bookings section but is out of scope
  for this initial phase.
- Account Management (login, profile, preferences) is planned but out of scope
  for this phase.
- Classes/Materials catalogue (course listing, downloadable resources) is planned
  but out of scope for this phase.
- No backend or authentication is required for the static presentation site.
