# Feature Specification: General Availability Template

**Feature Branch**: `009-general-availability`

**Created**: 2026-06-27

**Status**: Draft

**Input**: User description: "Add a general availability option for teacher. A calendar which will always be available or unavailable. Make the existing calendar be the source of truth, but when a new week comes, this calendar will always start with the general calendar availability."

## Summary

Today the teacher manages availability through a single recurring weekly pattern that applies identically to every week, plus per-date exceptions. This feature separates two distinct concerns:

1. A **general availability calendar** — a reusable weekly template (which weekday/time windows are available by default) that represents "how a normal week looks."
2. A **week-specific calendar** — the concrete availability for each individual upcoming week, which is what students actually book against and which the teacher can tweak per week.

The week-specific calendar is the **source of truth** for bookings. When a brand-new week first becomes bookable, its starting point is copied from the general calendar. From then on, the teacher's edits to that week are independent: they persist, they don't change the general template, and they don't affect other weeks.

## Clarifications

### Session 2026-06-27

- Q: How should each week's bookable calendar relate to the general template? → A: Snapshot per week — a week copies the general template into its own stored, editable calendar the first time it enters the bookable range; later template edits never alter already-seeded weeks.
- Q: How far ahead should the teacher be able to view and customize specific weeks? → A: Keep the current rolling bookable window (~2 weeks); the general template covers everything beyond it.
- Q: Should the teacher have an explicit "reset this week to the general template" action? → A: No — out of scope for this version; the teacher re-edits a week manually to undo changes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher defines a general weekly availability template (Priority: P1)

The teacher opens the admin panel and sets, for each day of the week, the time windows in which she is generally available (or marks a day fully unavailable). This is the "default week" that future weeks will inherit. She saves it once and does not have to re-enter it every week.

**Why this priority**: This is the foundation of the feature — without a general template there is nothing to seed new weeks from. It also directly replaces the existing recurring-pattern editor, so it must exist for the rest of the feature to make sense.

**Independent Test**: Can be fully tested by setting a general weekly pattern (e.g. Mon–Fri 09:00–18:00 with a lunch gap, weekends off), saving it, reloading the panel, and confirming the saved template is shown. Delivers value on its own: the teacher has a clearly labelled default schedule.

**Acceptance Scenarios**:

1. **Given** the teacher is on the availability editor, **When** she sets time windows for several weekdays and saves, **Then** the general template is persisted and shown on reload.
2. **Given** a weekday with no time windows in the general template, **When** the template is saved, **Then** that weekday is treated as fully unavailable by default.
3. **Given** the teacher enters a window whose end time is not after its start time, **When** she tries to save, **Then** the save is rejected with a clear validation message and nothing is persisted.

---

### User Story 2 - New weeks automatically start from the general template (Priority: P1)

As time advances and a new week first enters the bookable range, that week's concrete availability is initialized to match the current general template — without the teacher having to do anything. Students see the general availability for that week, and the teacher sees a per-week calendar already pre-filled.

**Why this priority**: This is the core promise of the request ("when a new week comes, this calendar will always start with the general calendar availability"). It removes the recurring manual work of re-declaring availability for every new week.

**Independent Test**: Can be tested by defining a general template, advancing to (or simulating) a week that had no prior customization entering the bookable range, and confirming the new week's bookable slots exactly match the general template at that moment.

**Acceptance Scenarios**:

1. **Given** a general template of Mon–Fri 09:00–18:00 and a week that has never been customized, **When** that week becomes bookable, **Then** its available slots match the general template for that week.
2. **Given** the general template marks Saturday unavailable, **When** a new week is seeded, **Then** that week has no Saturday availability unless later customized.
3. **Given** a new week is seeded from the general template, **When** the teacher views that week in the panel, **Then** the per-week calendar is pre-filled with the general windows ready to adjust.

---

### User Story 3 - Teacher customizes a specific week without affecting the template or other weeks (Priority: P2)

For a particular upcoming week, the teacher needs to deviate from her normal schedule (e.g. she blocks Wednesday afternoon, or opens a Saturday morning). She edits that week's calendar directly. Those changes are the source of truth for that week's bookings, persist across reloads, and leave both the general template and all other weeks unchanged.

**Why this priority**: This is what makes the week-specific calendar the "source of truth." Without it, the feature is just a renamed recurring pattern. It is P2 because Stories 1 and 2 already deliver a working seeded-default experience.

**Independent Test**: Can be tested by seeding a week from the template, editing that week (add/remove a window), reloading, and confirming the edit persisted while the general template and a different week remained as before.

**Acceptance Scenarios**:

1. **Given** a seeded week, **When** the teacher removes availability for one afternoon and saves, **Then** that afternoon is no longer bookable for that week only.
2. **Given** a seeded week, **When** the teacher opens an extra window on a normally-off day and saves, **Then** that window becomes bookable for that week only.
3. **Given** the teacher customized one week, **When** she views the general template and a different upcoming week, **Then** neither reflects the one-week customization.

---

### User Story 4 - Editing the general template updates only weeks not yet started (Priority: P3)

The teacher revises her general availability (e.g. she stops working Mondays going forward). Weeks that have already begun or were already customized keep their existing source-of-truth calendars; only weeks that have not yet been seeded pick up the new general pattern when they enter the bookable range.

**Why this priority**: This protects already-published and already-customized weeks from being silently overwritten, which is the natural reading of "the existing calendar is the source of truth." It is P3 because it is a refinement of the seeding rule rather than a standalone capability.

**Independent Test**: Can be tested by seeding a week, changing the general template, and confirming the already-seeded week is unchanged while a not-yet-seeded future week reflects the new template once it enters the bookable range.

**Acceptance Scenarios**:

1. **Given** a week has already been seeded (or customized), **When** the teacher changes the general template, **Then** that week's bookable availability is unchanged.
2. **Given** the teacher changed the general template, **When** a later, not-yet-seeded week enters the bookable range, **Then** that week is seeded from the updated template.

---

### Edge Cases

- **Existing bookings vs. seeding**: When a week is seeded or the general template is changed, any already-confirmed bookings remain valid; the teacher is warned if a confirmed booking now falls outside the week's availability (consistent with current conflict-warning behavior).
- **First rollout / migration**: The current single recurring weekly pattern is adopted as the initial general template so behavior is preserved on day one, and any already-bookable weeks keep their current availability.
- **Empty general template**: If the general template has no windows at all, newly seeded weeks start fully unavailable until the teacher customizes them.
- **Customizing a week back to the default**: The teacher can edit a customized week so its windows again match the general template; this remains a valid (if redundant) source-of-truth state.
- **Overlapping windows within a day**: Overlapping or adjacent windows on the same day in either the general template or a week are treated as a single merged available range.
- **Past time within the current week**: Slots in the current week that are already in the past (or inside the minimum booking lead time) remain non-bookable regardless of template or week settings.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let the teacher define and save a general weekly availability template consisting of time windows per weekday, where a weekday with no windows means unavailable by default.
- **FR-002**: The system MUST persist the general template and display it to the teacher on subsequent visits.
- **FR-003**: The system MUST maintain a separate concrete availability calendar for each individual bookable week, which is the authoritative source of truth for which slots students can book in that week.
- **FR-004**: The system MUST initialize a week's concrete calendar by taking a one-time snapshot copy of the current general template the first time that week becomes bookable, when the week has no prior customization. After this snapshot the week is independent of the template.
- **FR-005**: The system MUST allow the teacher to edit any current or upcoming bookable week's concrete calendar independently, and MUST persist those edits. The set of weeks the teacher can view and customize is exactly the weeks within the existing rolling bookable window; weeks beyond it are not yet materialized and are governed solely by the general template until they enter the window.
- **FR-013**: The system is NOT required to provide a one-click "reset week to general template" action; reverting a customized week is done by manual re-editing. (Out of scope for this version.)
- **FR-006**: Edits to a specific week MUST NOT change the general template or any other week's calendar.
- **FR-007**: Edits to the general template MUST NOT alter the calendar of any week that has already been seeded or customized; they MUST only affect weeks seeded after the change.
- **FR-008**: The public/student-facing schedule MUST reflect each week's concrete calendar (the source of truth), not the general template directly.
- **FR-009**: The system MUST validate that every time window has an end time later than its start time, in both the general template and week-specific calendars, and reject invalid windows with a clear message.
- **FR-010**: When seeding a new week or applying availability changes, the system MUST surface any existing confirmed booking that now falls outside the week's availability so the teacher can resolve it, without automatically cancelling it.
- **FR-011**: On first release, the system MUST preserve current behavior by treating the existing recurring weekly pattern as the initial general template and keeping currently bookable weeks' availability unchanged.
- **FR-012**: The general template and the per-week calendars MUST clearly be distinguishable to the teacher so she understands which one she is editing and what the effect will be.

### Key Entities *(include if feature involves data)*

- **General Availability Template**: The teacher's default weekly pattern — a set of available time windows keyed by weekday. Acts as the seed/default for future weeks. Single, shared, editable.
- **Week Availability (concrete calendar)**: The authoritative availability for one specific calendar week within the bookable range. Created by copying the general template when the week first becomes bookable, then independently editable. One per bookable week.
- **Booking**: An existing confirmed reservation tied to a specific date/time slot; relevant here because seeding and edits must not invalidate it silently and conflicts must be surfaced.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The teacher can set her normal weekly availability one time and have it automatically apply to every newly bookable week without re-entering it.
- **SC-002**: 100% of newly seeded, never-customized weeks present availability identical to the general template in effect when they were seeded.
- **SC-003**: A per-week customization persists across reloads and is reflected in what students can book, while leaving the general template and every other week unchanged in 100% of cases.
- **SC-004**: Changing the general template never alters an already-seeded or already-customized week (0% unintended overwrites).
- **SC-005**: After rollout, the schedule students see for already-bookable weeks is identical to what it was before the change (no regression on launch).
- **SC-006**: The teacher can distinguish and reach both "edit my default week" and "edit this specific week" in the panel without external guidance.

## Assumptions

- **Granularity**: The general template uses the same weekday + time-window granularity as today's recurring pattern; slot length and minimum booking lead time are governed by existing scheduling settings and are unchanged.
- **Bookable range**: The set of weeks that are bookable at any time continues to be governed by the existing rolling horizon (~2 weeks); "a new week comes" means a week newly entering that horizon. This feature does not change how far ahead bookings are allowed, and the teacher customizes only weeks within that window (confirmed in Clarifications).
- **Seeding timing**: A week is seeded from the general template as a one-time snapshot when it first enters the bookable horizon, after which it is independent (confirmed in Clarifications). This is the interpretation of "always start with the general calendar availability."
- **Override semantics**: Per-week customization replaces the seeded values for that week; the existing per-date exception concept (block/open a specific date) is subsumed by direct editing of the relevant week's calendar.
- **Timezone**: All availability continues to be interpreted in the teacher's configured timezone.
- **Authorization**: Only the teacher/admin can edit the general template and week calendars; students only read the resulting schedule.
- **Migration**: Existing weekly rules and any future-dated exceptions are carried over so the launch is behavior-preserving.
