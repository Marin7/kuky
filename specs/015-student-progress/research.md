# Phase 0 Research: Student Progress on Teacher Profile View

No open `NEEDS CLARIFICATION` markers remained after `/speckit-clarify`. This document records the design decisions made while surveying the existing codebase, so Phase 1 can proceed directly to data model and contracts.

## 1. Homework breakdown: aggregate in Java, no new query

**Decision**: Reuse the existing `HomeworkTargetRepository.findAssignmentsForStudent(studentId)` query (already called by `StudentProfileAdminService.getProfile`, `back-end/src/main/java/com/kuky/backend/admin/service/StudentProfileAdminService.java:43-45`) and group its results into three counts (pending / submitted / completed) in the service layer.

**Rationale**: The data is already fetched for the existing homework list on the profile — a student has at most a few dozen homeworks, so counting in Java is simpler than a second SQL aggregate query (Simplicity First / YAGNI). `REVIEWED` and `GRADED` are merged into one "completed" bucket per the clarification answer, since a given homework's `format` (`MANUAL` vs `EXERCISE`) only ever reaches one of the two terminal statuses (`CLAUDE.md`), so showing them separately would always leave one column at zero for any single homework.

**Alternatives considered**: A `GROUP BY status` SQL query — rejected as redundant; it would just re-fetch data already in memory for the homework list section.

## 2. Unit completion: one new SQL query, joined through `homework_targets`

**Decision**: A new `UnitRepository` method, `findProgressForStudent(UUID studentId)`, returns one row per unit assigned to the student with `totalHomeworks`/`completedHomeworks` counts, computed via:

```sql
SELECT u.id AS unit_id, u.subject, u.level, u.position,
       COUNT(t.id) AS total_homeworks,
       COUNT(t.id) FILTER (WHERE COALESCE(s.status, 'PENDING') IN ('REVIEWED','GRADED')) AS completed_homeworks
FROM unit_assignments ua
JOIN units u ON u.id = ua.unit_id
LEFT JOIN homework_assignments ha ON ha.unit_id = u.id
LEFT JOIN homework_targets t ON t.assignment_id = ha.id AND t.user_id = ua.user_id
LEFT JOIN homework_submissions s ON s.assignment_id = ha.id AND s.user_id = ua.user_id
WHERE ua.user_id = :studentId
GROUP BY u.id, u.subject, u.level, u.position
ORDER BY u.position
```

**Rationale**: `V18__create_units.sql` (lines 33-34) explicitly documents that `homework_assignments.unit_id` is "organisational grouping only... NEVER consulted for student access (only `homework_targets` governs access)." A naive `COUNT(ha.id)` grouped by unit would count homeworks that are filed under the unit but never actually assigned to *this* student, overstating both the total and (if the student happens to have no submission) making a unit look incomplete for homework it was never given. Routing the count through `LEFT JOIN homework_targets ... AND t.user_id = ua.user_id` means only homeworks the student is actually assigned to are counted, while the unit itself still appears (with `total_homeworks = 0`) via the outer joins even when none of its homeworks are targeted at the student — satisfying the "unit with no completion criteria yet" edge case without a separate code path.

**Alternatives considered**: Counting `homework_assignments.unit_id` directly without the `homework_targets` join — rejected because it would contradict the existing, explicitly-documented access model and produce incorrect completion percentages.

## 3. Attendance requires a new `no_show` column — the existing `status` field isn't enough

**Decision**: Add `no_show BOOLEAN NOT NULL DEFAULT FALSE` to `bookings` via a new migration (`V26__add_booking_no_show.sql`, following the same single-column `ALTER TABLE` style as `V24__add_booking_reminder_sent.sql`). "Attended" = `status = 'CONFIRMED' AND slot_start < now() AND no_show = FALSE`.

**Rationale**: `bookings.status` only has two values, `CONFIRMED` and `CANCELLED` (`V3__create_bookings.sql:11`) — there's no way today to tell a class that happened from one where the student didn't show. The clarification answer requires the teacher to be able to correct this, so a dedicated boolean (defaulting to "attended", per the clarified default-attended-unless-flagged behavior) is the minimal schema change. It's a plain column on the existing table, not a new entity — a no-show is a property of a specific past booking, not an independent record with its own lifecycle.

**Alternatives considered**: A separate `booking_attendance` table — rejected as unnecessary indirection for a single boolean tied 1:1 to an existing row (YAGNI); a third `status` value (`NO_SHOW`) — rejected because `status` already drives cancellation logic (`CANCELLED` excludes a booking from reminders, availability, etc. — see `BookingRepository`), and conflating "didn't show up" with "was cancelled" would require auditing every existing `status = 'CONFIRMED'` check in the codebase instead of adding one independent, additive column.

## 4. No-show is set through a booking-scoped admin endpoint, not a student-profile-scoped one

**Decision**: Add `PUT /api/v1/admin/bookings/{id}/no-show` to the existing `BookingAdminController` (`back-end/src/main/java/com/kuky/backend/admin/controller/BookingAdminController.java`), body `{ "noShow": true|false }`, delegating to a new `BookingService.setNoShow(bookingId, noShow)` that mirrors the existing `cancelBookingAsAdmin` method (`back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java:147-166`): look up the booking, validate eligibility (`CONFIRMED` and in the past), then update. A new `BookingNotAllowedException.Reason.NOT_ELIGIBLE_FOR_NO_SHOW` reuses the existing exception/handler pattern (`GlobalExceptionHandler:109-121`), returning `422 BOOKING_NOT_ELIGIBLE_FOR_NO_SHOW` for a future or cancelled booking (FR-012).

**Rationale**: A booking's attendance is a property of the booking itself, addressable by its own id — the student-profile page is just where the teacher happens to trigger the action from. Placing it on the existing booking controller (rather than nesting it under `/admin/students/{id}/...`) matches how `cancelBooking` already works there, and needs no new security matcher (`/api/v1/admin/**` already requires `ADMIN`).

**Alternatives considered**: Nesting the endpoint under `/api/v1/admin/students/{studentId}/bookings/{bookingId}/no-show` — rejected; `studentId` is redundant (the booking id is already globally unique and already resolves to its student), and every other booking mutation (`DELETE .../bookings/{id}`) is addressed by booking id alone.

## 5. Frontend placement

**Decision**: Extend the existing `StudentProfile`/`StudentProfileBooking` interfaces and `getStudentProfile` consumer in `front-end/src/routes/panel_.alumnos.$studentId.tsx` — add a new "Progreso" `<Section>` (using the page's existing `Section`/`StatusBadge` components, lines 49-87) above the existing bookings/homework lists, and a small no-show toggle rendered inline on each row of the existing "past bookings" list. A new `setBookingNoShow(id, noShow)` call is added to `front-end/src/lib/admin.ts` alongside the other booking/admin calls, following the same `apiCall<T>` pattern as `cancelBooking`. The CEFR level shown in the progress section reuses the `placement` state the page already fetches via `getStudentPlacementEvaluation` (line 117-119) — no new fetch.

**Rationale**: Every piece follows an existing, working precedent — no new routing, no new fetch pattern, no new component library. Toggling no-show re-fetches the profile (`getStudentProfile`) on success, matching the simplest existing refresh pattern on this page rather than introducing optimistic local state.

**Alternatives considered**: A separate `/panel/alumnos/{id}/progreso` sub-route — rejected; the spec's assumption is explicit that progress is a section on the existing profile page, not a new page.
