# Phase 0 Research: Shared Bookings

All items below were resolved from the existing codebase and the `019-extended-booking-duration` precedent rather than external research — this feature extends an established pattern, it doesn't introduce a new one.

## Decision: Single nullable `second_student_id` column, not a join table

**Rationale**: The spec caps a class at exactly two students (the booking student plus at most one more, FR-003), never a general attendee list. A `booking_students` join table would be the "correct" model for an arbitrary N, but here N is fixed at ≤2, and `bookings` already carries the booking student the same way (`user_id`). Adding one more nullable FK column mirrors the existing shape exactly and needs no new repository, no new join queries, and no new entity in the domain model.

**Alternatives considered**:
- **Join table (`booking_students`)**: rejected — would require rewriting every existing single-student query (`findByUserId`, `findConfirmedBookingIntervalsBetween`, `findUpcomingBookingsForAdmin`, etc.) to aggregate from a table, for a relationship that is never more than 2 rows. Pure YAGNI violation given the constitution's Simplicity First principle.
- **JSON/array column of student IDs**: rejected — loses the FK integrity check Postgres already gives `user_id`, and gains nothing over a second scalar FK column at cap-2.

## Decision: Per-student no-show as a second boolean column (`second_student_no_show`), not a new table

**Rationale**: Same reasoning as above — today's `no_show` is a single boolean per booking. Since attendance now needs to answer "did the booking student show up?" and "did the companion student show up?" independently (per the FR-010 clarification), the minimal extension is a second boolean, nullable and only meaningful when `second_student_id` is set. No new entity or history table is needed since there's no requirement to track no-show over time beyond the current per-class flag.

## Decision: Extend `BookingAdminController` with attach/detach endpoints, mirroring the extended-class-eligibility grant/revoke pattern

**Rationale**: `StudentAdminController` already has a proven teacher-driven grant/revoke shape (`POST`/`DELETE /api/v1/admin/users/{id}/extended-class`) for "a state the teacher toggles on a specific student." Attaching/detaching a companion student is the same shape, scoped to a booking instead of a user: `POST`/`DELETE /api/v1/admin/bookings/{id}/companion-student`. Keeping it on the existing `BookingAdminController` (which already has cancel and no-show actions for a booking) avoids introducing a new controller for a single new resource-on-a-resource action.

**Alternatives considered**:
- **New dedicated controller** (e.g. `SharedBookingAdminController`): rejected as an unnecessary split — the action is inherently "an admin operation on an existing booking," which is exactly what `BookingAdminController` already owns.
- **PATCH `/admin/bookings/{id}` with a full booking body**: rejected — no such general-purpose "edit a booking" endpoint exists today (confirmed during spec research), and introducing one to support a single field would be broader surface than needed.

## Decision: Reuse `BookingNotAllowedException.Reason` enum for the new rejection cases, rather than a new exception type

**Rationale**: The exact same enum already grew twice for the extended-duration feature (`INVALID_DURATION`, `NOT_ELIGIBLE_FOR_EXTENDED`), establishing the pattern that "reasons a booking-related action isn't allowed" belong here rather than as separate exception classes. One of the five new rejection cases maps cleanly onto an existing reason with a different message string (companion student lacks extended eligibility → existing `NOT_ELIGIBLE_FOR_EXTENDED`). The other four are new: "already has a companion student," "target is already the booking student," "nothing to detach," and "booking is cancelled or already occurred." That last one is a *new* reason (`BOOKING_NOT_ATTACHABLE`) rather than a reuse of the existing `STATE` reason (today's "already cancelled" for student self-cancellation) — reusing `STATE` would surface an `ALREADY_CANCELLED` error code even when the real reason attach failed is "this class already happened," which is a different fact a caller shouldn't have to infer from a cancellation-shaped error.

## Decision: Reuse `StudentMultiSelect` (capped to a single selection) for the attach dialog, not a new picker

**Rationale**: `front-end/src/components/admin/homework/StudentMultiSelect.tsx` already renders a searchable list of students backed by `getStudents()` (`front-end/src/lib/admin.ts:53`) for the Homework tab's student-assignment flow. Attaching a companion student needs the exact same "pick from existing students" interaction, just capped to one result instead of many. Wrapping it in a small dialog that takes only the first selected ID avoids building and maintaining a companion student-picker component.

## Decision: Reminder emails and cancellation notifications both fan out to the companion student when present

**Rationale**: SC-002 requires the companion student to receive class details "before the class starts," and the spec's cancellation clarification (Session 2026-07-03) established that a shared class's cancellation must reach both students regardless of who triggered it (FR-011). Both `BookingReminderScheduler` (the 24h-before job) and the three existing cancellation-email call sites (`cancelBooking`, `cancelBookingAsAdmin`) need the companion student's email threaded through alongside the booking student's, otherwise a shared class silently under-serves the companion student relative to what SC-002/FR-011 promise.

**Alternative considered**: Only notifying the companion student at attach-time (no reminder, no cancellation notice) — rejected, since it would leave the companion student unaware if the class is later cancelled or fails to remind them the day before, contradicting FR-011 and SC-002 as written.
