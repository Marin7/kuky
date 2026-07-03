---

description: "Task list template for feature implementation"
---

# Tasks: Shared Bookings (Companion Student on a Class)

**Input**: Design documents from `specs/021-shared-bookings/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/companion-student-api.md](contracts/companion-student-api.md), [quickstart.md](quickstart.md)

**Tests**: Included — this repo has an established backend integration/unit test convention (`BookingControllerIntegrationTest`, `BookingServiceTest`, `BookingAdminControllerIntegrationTest`, etc.); new backend behavior gets matching test coverage. Frontend has no test framework (per constitution) — frontend verification is manual/browser-based via [quickstart.md](quickstart.md).

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: The schema change every other task depends on.

- [X] T001 Write `back-end/src/main/resources/db/migration/V30__shared_bookings.sql` per [data-model.md](data-model.md): `ALTER TABLE bookings ADD COLUMN second_student_id UUID REFERENCES users(id) ON DELETE SET NULL;`, `ALTER TABLE bookings ADD COLUMN second_student_no_show BOOLEAN;`, `ALTER TABLE bookings ADD CONSTRAINT bookings_second_student_distinct CHECK (second_student_id IS NULL OR second_student_id <> user_id);`, `CREATE INDEX bookings_second_student_id_idx ON bookings (second_student_id) WHERE second_student_id IS NOT NULL;`. No backfill needed — every existing row correctly gets `NULL` for both new columns.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The `Booking` model/row-mapping change and the new rejection-reason vocabulary that both US1 (attach) and US3 (detach) depend on, plus the widened `AdminBookingDto` every admin-facing story needs.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] In `back-end/src/main/java/com/kuky/backend/scheduling/model/Booking.java`, add `private UUID companionStudentId;` and `private Boolean companionStudentNoShow;` fields with accessors, alongside the existing `userId`/`noShow` fields.
- [X] T003 [P] In `back-end/src/main/java/com/kuky/backend/scheduling/repository/BookingRepository.java`, extend the row mapper (`BOOKING_MAPPER` or equivalent used by `findById`/`findByUserId`/`insert`) to read `second_student_id` and `second_student_no_show` onto the new `Booking` fields (T002).
- [X] T004 [P] In `back-end/src/main/java/com/kuky/backend/scheduling/exception/BookingNotAllowedException.java`, add five new values to the `Reason` enum: `COMPANION_ALREADY_ATTACHED`, `COMPANION_SAME_AS_BOOKING_STUDENT`, `COMPANION_NOT_STUDENT`, `COMPANION_NOT_ATTACHED`, `BOOKING_NOT_ATTACHABLE` — alongside the existing `RANGE, LEAD, STATE, CUTOFF, NOT_ELIGIBLE_FOR_NO_SHOW, INVALID_DURATION, NOT_ELIGIBLE_FOR_EXTENDED`.
- [X] T005 [P] In `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java`, extend the `handleBookingNotAllowed` switch (per [contracts/companion-student-api.md](contracts/companion-student-api.md)) with: `COMPANION_ALREADY_ATTACHED -> 409`, `COMPANION_SAME_AS_BOOKING_STUDENT -> 409`, `COMPANION_NOT_STUDENT -> 422`, `COMPANION_NOT_ATTACHED -> 404`, `BOOKING_NOT_ATTACHABLE -> 409`, each with a Spanish message matching the file's existing style. Depends on T004.
- [X] T006 [P] In `back-end/src/main/java/com/kuky/backend/admin/dto/AdminBookingDto.java`, add nullable fields `companionStudentId`, `companionStudentEmail`, `companionStudentFirstName`, `companionStudentLastName`, `companionStudentUsername`, `companionStudentNoShow`, per [contracts/companion-student-api.md](contracts/companion-student-api.md).
- [X] T007 In `back-end/src/main/java/com/kuky/backend/scheduling/repository/BookingRepository.java`, extend the SQL behind `findUpcomingBookingsForAdmin(Instant from)` with a second `LEFT JOIN users` (aliased) on `bookings.second_student_id`, selecting that joined user's `id, email, first_name, last_name, username` and `bookings.second_student_no_show` alongside the existing primary-student columns, mapping them onto the new `AdminBookingDto` fields (T006) — `NULL` when no companion student is attached. Depends on T006.

**Checkpoint**: `Booking` carries both new fields end-to-end at the persistence layer; the admin view is ready to display a companion student once one exists; the rejection vocabulary for attach/detach exists. User stories can now begin.

---

## Phase 3: User Story 1 - Teacher attaches a companion student to an existing class (Priority: P1) 🎯 MVP

**Goal**: The teacher can attach exactly one additional student to an existing, upcoming, confirmed booking, with all the validation from FR-002 through FR-005 and FR-014 enforced server-side; the attached student is notified.

**Independent Test**: As a student, book a class normally. As the teacher, attach a second (already-registered) student to that booking from `/panel` → Bookings tab; confirm the booking now shows both students, and Mailpit shows a class-details email sent to the companion student. Attempting to attach a third student, the same student twice, a non-student user, or an ineligible student to an extended-duration class are all rejected server-side.

### Tests for User Story 1

- [X] T008 [P] [US1] Extend `back-end/src/test/java/com/kuky/backend/scheduling/BookingServiceTest.java` with `attachCompanionStudent(...)` cases: succeeds and persists `second_student_id` for a valid `STUDENT` target on a `CONFIRMED`, future, single-student booking; throws `Reason.BOOKING_NOT_ATTACHABLE` for a `CANCELLED` booking and for one whose `slotStart` has passed; throws `Reason.COMPANION_ALREADY_ATTACHED` when `second_student_id` is already set; throws `Reason.COMPANION_SAME_AS_BOOKING_STUDENT` when the target equals `user_id`; throws `Reason.COMPANION_NOT_STUDENT` for a target whose role isn't `STUDENT`; throws `Reason.NOT_ELIGIBLE_FOR_EXTENDED` when the booking's `durationMinutes` is the extended length and the target lacks `extendedClassEligible`; on success, calls `emailService.sendCompanionStudentAttached(...)` with the target's email and the booking's date/time/join details, and does **not** call any email method addressed to the booking student (FR-015).
- [X] T009 [P] [US1] Write `back-end/src/test/java/com/kuky/backend/admin/BookingAdminControllerIntegrationTest.java` (extend if a file covering `/admin/bookings` already exists, else create) covering: `POST /admin/bookings/{id}/companion-student` with a valid `{ "studentId": ... }` returns `200` with an `AdminBookingDto` whose `companionStudentId`/`companionStudentEmail`/etc. are populated; `404 BOOKING_NOT_FOUND` for an unknown booking id; `404 USER_NOT_FOUND` for an unknown student id; `409 BOOKING_NOT_ATTACHABLE`, `409 COMPANION_ALREADY_ATTACHED`, `409 COMPANION_SAME_AS_BOOKING_STUDENT`, `422 COMPANION_NOT_STUDENT`, `403 EXTENDED_CLASS_NOT_ELIGIBLE` for their respective setups; `403 ACCESS_DENIED` for a non-admin caller.
- [X] T010 [P] [US1] Extend `back-end/src/test/java/com/kuky/backend/scheduling/BookingEmailServiceTest.java` with a case for `sendCompanionStudentAttached(String toEmail, UUID bookingId, Instant slotStart, int durationMinutes, String joinUrl)`, asserting an email is sent to the given recipient with the class's date/time/join link and (per [research.md](research.md)) an ICS attachment, mirroring `sendConfirmation`'s structure.

### Implementation for User Story 1

- [X] T011 [P] [US1] In `back-end/src/main/java/com/kuky/backend/scheduling/repository/BookingRepository.java`, add `setCompanionStudentId(UUID bookingId, UUID studentId)` (`UPDATE bookings SET second_student_id = :studentId WHERE id = :bookingId`), mirroring `updateZoomDetails`'s shape. Depends on T003.
- [X] T012 [P] [US1] Create `back-end/src/main/java/com/kuky/backend/admin/dto/AttachCompanionStudentRequest.java` — `public record AttachCompanionStudentRequest(@NotNull UUID studentId) {}`.
- [X] T013 [P] [US1] In `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java`, add `sendCompanionStudentAttached(String toEmail, UUID bookingId, Instant slotStart, int durationMinutes, String joinUrl)`, reusing `buildIcs(...)` and the same date/time-formatting helpers as `sendConfirmation`, addressed only to the companion student (no teacher/primary-student copy). Uses the real `bookingId` as the ICS UID (not a random one) so a later cancellation's CANCEL event correctly replaces it in the student's calendar.
- [X] T014 [US1] In `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java`, add `attachCompanionStudent(UUID bookingId, UUID studentId)`: load the booking (404 `BookingNotFoundException` if absent) and the target user (404 `UserNotFoundException` if absent) via their existing repositories; validate in order — booking is `CONFIRMED` and `slotStart` is in the future (else `Reason.BOOKING_NOT_ATTACHABLE`), `second_student_id` is currently null (else `Reason.COMPANION_ALREADY_ATTACHED`), `studentId != booking.getUserId()` (else `Reason.COMPANION_SAME_AS_BOOKING_STUDENT`), target's role is `STUDENT` (else `Reason.COMPANION_NOT_STUDENT`), and if `booking.getDurationMinutes()` equals the extended duration, target's `extendedClassEligible` is true (else `Reason.NOT_ELIGIBLE_FOR_EXTENDED`) — then call `bookingRepository.setCompanionStudentId(bookingId, studentId)` (T011) and `bookingEmailService.sendCompanionStudentAttached(...)` (T013). Depends on T004, T011, T013.
- [X] T015 [US1] In `back-end/src/main/java/com/kuky/backend/admin/controller/BookingAdminController.java`, add `POST /bookings/{id}/companion-student` accepting `AttachCompanionStudentRequest` (T012), calling `bookingService.attachCompanionStudent(id, request.studentId())` (T014) and returning the refreshed `AdminBookingDto` (re-fetch via the same query T007 extended, or construct from the updated `Booking` + target `User`). Depends on T012, T014.

**Checkpoint**: Teacher can attach a companion student to any eligible booking from the API; the admin view reflects it; the companion student is notified. This alone is a demonstrable MVP even before US2's own-bookings visibility or US3's detach exist.

---

## Phase 4: User Story 2 - Companion student sees and joins the shared class (Priority: P2)

**Goal**: The attached companion student sees the class in their own "My Bookings," can join it, and can cancel it themselves — with the same cancellation notifying both students regardless of who triggered it, and the same 24h-before reminder reaching both.

**Independent Test**: With a booking from US1's flow (a companion student already attached), log in as that companion student, confirm the class appears in their own upcoming classes with join details, cancel it, and confirm the class disappears from both students' views with both receiving a cancellation email.

### Tests for User Story 2

- [X] T016 [P] [US2] Extend `back-end/src/test/java/com/kuky/backend/scheduling/BookingServiceTest.java`: `listForUser(companionStudentEmail)` includes a booking where that user is only `second_student_id`, with `isCompanionStudent = true` on its `BookingSummary`, alongside their own primary bookings (`isCompanionStudent = false`); `cancelBooking(companionStudentEmail, bookingId)` succeeds (ownership check passes for the companion student, not just the primary), sets `status = CANCELLED`, and triggers a cancellation email to **both** the primary and companion student's addresses (plus the teacher); `cancelBooking` by the booking student on a shared booking still also notifies the companion student.
- [X] T017 [P] [US2] Extend `back-end/src/test/java/com/kuky/backend/scheduling/BookingControllerIntegrationTest.java`: `GET /bookings` as the companion student returns the shared booking; `DELETE /bookings/{id}` as the companion student on a shared booking returns `204` and the booking is no longer returned by `GET /bookings` for either student afterward.
- [X] T018 [P] [US2] Write `back-end/src/test/java/com/kuky/backend/scheduling/BookingReminderSchedulerTest.java` (create if it doesn't exist) covering: `findBookingsDueForReminder(now)`'s `ReminderDueView` carries a non-null `companionStudentEmail` when the due booking has a companion student attached, and `null` when it doesn't; `BookingReminderScheduler`'s scheduled run sends the reminder to both the primary and companion student's email when both are present on a due booking, and only to the primary when there's no companion student (unchanged from today).

### Implementation for User Story 2

- [X] T019 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/repository/BookingRepository.java`, widen `findByUserId(UUID userId)` to `WHERE user_id = :userId OR second_student_id = :userId`, ordered by `slot_start` as today. Depends on T003.
- [X] T020 [P] [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/dto/BookingSummary.java`, add `boolean isCompanionStudent`.
- [X] T021 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java`, `listForUser(String userEmail)`: when mapping each `Booking` to a `BookingSummary` (T020), set `isCompanionStudent = booking.getCompanionStudentId() != null && booking.getCompanionStudentId().equals(user.getId())`. Depends on T019, T020.
- [X] T022 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java`, `cancelBooking(String userEmail, UUID bookingId)`: widen the ownership check from `b.getUserId().equals(user.getId())` to also accept `b.getCompanionStudentId() != null && b.getCompanionStudentId().equals(user.getId())`, keeping the existing 24h-cutoff rule unchanged for whichever student calls it. Depends on T002.
- [X] T023 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java`, extend `sendCancellation(...)` and `sendCancellationByTeacher(...)` with an overload accepting an optional companion-student email, sending the same cancellation content to that address too when present. Depends on T013 (shares the file; sequenced after).
- [X] T024 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java`, thread the booking's `second_student_id` (resolved to an email, if present) through to `bookingEmailService.sendCancellation(...)`/`sendCancellationByTeacher(...)` at both call sites (`cancelBooking`, `cancelBookingAsAdmin`). Depends on T022, T023.
- [X] T025 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/repository/BookingRepository.java`, extend the `ReminderDueView` record with a nullable `companionStudentEmail` field, and extend `findBookingsDueForReminder(Instant now)`'s SQL with a `LEFT JOIN users` on `bookings.second_student_id`, selecting that user's `email` alongside the existing primary-student columns (mirroring the join pattern from T007). Depends on T003.
- [X] T026 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingReminderScheduler.java`, for each due booking (T025), send the existing reminder to the booking student's email as today, and additionally to `companionStudentEmail` when it's present. Depends on T025.
- [X] T027 [P] [US2] In `front-end/src/lib/scheduling.ts`, add `isCompanionStudent: boolean` to the `BookingSummary` type, per [contracts/companion-student-api.md](contracts/companion-student-api.md).
- [X] T028 [US2] In `front-end/src/components/scheduling/MyBookings.tsx`, render a small indicator (e.g. a badge or subtitle) on any booking where `isCompanionStudent` is true, distinguishing "you're joining this class" from a normal own booking; the existing cancel button/flow needs no change since `DELETE /bookings/{id}` already succeeds for the companion student (T022). Depends on T027.

**Checkpoint**: The companion student has full parity with the booking student for viewing, cancelling, and being reminded about a shared class; cancellation from either side reaches both students.

---

## Phase 5: User Story 3 - Teacher removes a companion student added by mistake (Priority: P3)

**Goal**: The teacher can detach a companion student from a booking without cancelling the class, reverting it to a normal single-student booking; independent per-student no-show tracking is also wired up here since it depends on the same "is there a companion student" state.

**Independent Test**: With a shared booking from US1, detach the companion student from `/panel` → Bookings tab; confirm the booking reverts to showing only the booking student, and the removed student no longer sees the class in their own "My Bookings." Separately, mark the primary and companion student's no-show flags independently and confirm they don't affect each other.

### Tests for User Story 3

- [X] T029 [P] [US3] Extend `back-end/src/test/java/com/kuky/backend/scheduling/BookingServiceTest.java`: `detachCompanionStudent(bookingId)` clears `second_student_id` and `second_student_no_show` on a booking that has one, and leaves `user_id`/`status`/all other fields untouched; throws `Reason.COMPANION_NOT_ATTACHED` when called on a booking with no companion student. `setNoShow(bookingId, true, BOOKING_STUDENT)` and `setNoShow(bookingId, true, COMPANION)` set only their respective column, independently, on a shared booking; `setNoShow(bookingId, ..., COMPANION)` throws `Reason.COMPANION_NOT_ATTACHED` when the booking has no companion student; calling `setNoShow` with no `studentRole` (existing single-arg call sites, if any remain) defaults to `BOOKING_STUDENT` and behaves exactly as before this feature.
- [X] T030 [P] [US3] Extend `back-end/src/test/java/com/kuky/backend/admin/BookingAdminControllerIntegrationTest.java`: `DELETE /admin/bookings/{id}/companion-student` returns `204` and clears the fields from a subsequent `GET /admin/bookings`; `404 COMPANION_NOT_ATTACHED` when there's nothing to detach; `PUT /admin/bookings/{id}/no-show` with `{ "noShow": true, "studentRole": "COMPANION" }` returns `204` and only the companion student's flag changes; omitting `studentRole` still targets the booking student as it does today.

### Implementation for User Story 3

- [X] T031 [P] [US3] In `back-end/src/main/java/com/kuky/backend/scheduling/repository/BookingRepository.java`, add `clearCompanionStudentId(UUID bookingId)` (`UPDATE bookings SET second_student_id = NULL, second_student_no_show = NULL WHERE id = :bookingId`). Depends on T003.
- [X] T032 [P] [US3] In `back-end/src/main/java/com/kuky/backend/scheduling/dto/SetNoShowRequest.java`, add an optional `studentRole` field (e.g. `String studentRole` or a small enum `BOOKING_STUDENT`/`COMPANION`), defaulting to `BOOKING_STUDENT` when absent.
- [X] T033 [US3] In `back-end/src/main/java/com/kuky/backend/scheduling/repository/BookingRepository.java`, add `setCompanionStudentNoShow(UUID id, boolean noShow)` so `BookingService.setNoShow` (T034) can target either column. Depends on T003.
- [X] T034 [US3] In `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java`: add `detachCompanionStudent(UUID bookingId)` — throws `Reason.COMPANION_NOT_ATTACHED` if `second_student_id` is already null, else calls `bookingRepository.clearCompanionStudentId(bookingId)` (T031); extend `setNoShow(UUID bookingId, boolean noShow, StudentRole target)` (or equivalent) to route to the booking-student or companion column (T033), throwing `Reason.COMPANION_NOT_ATTACHED` if `target == COMPANION` and no companion student is attached. Depends on T031, T032, T033.
- [X] T035 [US3] In `back-end/src/main/java/com/kuky/backend/admin/controller/BookingAdminController.java`, add `DELETE /bookings/{id}/companion-student` calling `bookingService.detachCompanionStudent(id)` (T034) and returning `204`; update the existing `PUT /bookings/{id}/no-show` handler to pass `request.studentRole()` (T032) through to `bookingService.setNoShow(...)`. Depends on T034.
- [X] T036 [P] [US3] In `front-end/src/lib/admin.ts`: add `attachCompanionStudent(bookingId: string, studentId: string)` (`POST /bookings/${bookingId}/companion-student`) and `detachCompanionStudent(bookingId: string)` (`DELETE /bookings/${bookingId}/companion-student`); extend the `AdminBooking` type with the nullable `secondStudent*` fields (T006); extend `setBookingNoShow(id, noShow, studentRole?)` to send the optional `studentRole` field.
- [X] T037 [US3] Create `front-end/src/components/admin/bookings/AttachCompanionStudentDialog.tsx`, reusing `StudentMultiSelect` (`front-end/src/components/admin/homework/StudentMultiSelect.tsx`) capped to a single selection (submit only `selected[0]`), calling `attachCompanionStudent` (T036) on confirm. Depends on T036.
- [X] T038 [US3] In `front-end/src/components/admin/bookings/BookingsTab.tsx`: show the companion student's name when `companionStudentId` is present, alongside an action to open `AttachCompanionStudentDialog` (T037) when absent, or "remove companion student" (calling `detachCompanionStudent`) when present. **Revised during implementation**: `BookingsTab.tsx` only ever lists *upcoming* confirmed bookings (its own description says so) and never had a no-show toggle to begin with — no-show only applies to *past* classes, and that toggle already lived on the student profile page (`panel_.alumnos.$studentId.tsx`) instead. Discovered mid-implementation that `getStudentProfile`'s booking list (backed by the now-widened `findByUserId`, T019) could return a booking where the viewed student is the *second* student, but `StudentProfileAdminService.toBookingDto` always reported the *primary's* `no_show` flag and the page always called `setBookingNoShow` without a role — silently flipping the wrong student's attendance. Fixed by adding `isCompanionStudent` to `StudentProfileBookingDto` (computed per-viewed-student in `toBookingDto`) and threading it through to `setBookingNoShow(id, noShow, isCompanionStudent ? "COMPANION" : "BOOKING_STUDENT")` in the profile page — this is where "the per-student no-show toggle" actually lives, not in `BookingsTab.tsx`.
- [X] T039 [P] [US3] Add the new UI copy (attach/detach dialog labels, confirm prompts, per-student no-show labels) to the front-end i18n locale files (`front-end/src/i18n/locales/es.ts` primary, `ro.ts`, and `en.ts` for consistency — a third locale not anticipated when this task was written), under an `admin.bookings.secondStudent.*` namespace.

**Checkpoint**: All three user stories are independently functional — attach (US1), the companion student's own view/cancellation/reminders (US2), and detach plus per-student no-show (US3).

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification and documentation once all three stories are complete.

- [X] T040 [P] Run `cd back-end && ./gradlew test && ./gradlew build` — confirm all new/extended tests (T008-T010, T016-T018, T029-T030) and the full existing suite pass.
- [X] T041 [P] Run `cd front-end && npm run lint && npm run format` — confirm no lint/format regressions from T027-T028, T036-T039.
- [X] T042 Execute [quickstart.md](quickstart.md) Scenarios 1-6 manually against the running dev servers. **Revised during implementation**: Mailpit wasn't running in this session, so email delivery itself couldn't be visually confirmed in its web UI — instead confirmed via backend logs that `BookingEmailService` attempted to send to the correct recipient each time (caught `MailConnectException`, logged as a warning, exactly the existing best-effort pattern) and via the actual browser flow: teacher attached a companion student (dialog → both students shown on the booking), the companion student saw "Te has unido a esta clase" in their own bookings and could join, the companion student cancelling correctly cancelled the whole class for both (verified from both students' own views), and the teacher could detach a companion student in one click, reverting the booking to single-student. Also verified per-student no-show independence directly via the admin profile API: marking the booking student's no-show left the companion student's `noShow`/`isCompanionStudent` flags untouched.
- [X] T043 [P] Add a "Shared bookings" bullet under **Back-end** in `CLAUDE.md` documenting the `second_student_id`/`second_student_no_show` columns, the teacher-only attach/detach endpoints, and that either student can cancel a shared class for both — mirroring the existing terse style of that section.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T001) — BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational only. Independently testable/shippable as MVP (attach works and is verifiable via the admin endpoint + Mailpit, even before US2's own-view or US3's detach exist).
- **User Story 2 (Phase 4)**: Depends on Foundational only, and functionally needs a booking with a companion student already attached to demonstrate — in practice sequence after Phase 3, though it has no *code* dependency on Phase 3's files beyond the schema/model Foundational already provides.
- **User Story 3 (Phase 5)**: Same relationship — needs a shared booking to detach/mark-no-show-on, so sequence after Phase 3 in practice, but touches different files than Phase 4 (no code dependency between US2 and US3).
- **Polish (Phase 6)**: Depends on Phases 3-5 all being complete.

### Within Each User Story

- Tests are listed before implementation tasks (write/skim them first per repo's integration-test convention) but are not strict red-green TDD gates.
- Repository/DTO/exception tasks before the service/controller task that wires them together.
- Backend before the frontend tasks that call it.

### Parallel Opportunities

- Foundational: T002, T003 sequenced (T003 needs T002's fields to map); T004, T005, T006 [P] (three different files); T007 depends on T006.
- Within US1: T008, T009, T010 [P] (tests, different files); T011, T012, T013 [P] (different files, no interdependency); T014 waits for T004, T011, T013; T015 waits for T012, T014.
- Within US2: T016, T017, T018 [P] (tests, different files); T019 → T021 (same file, sequential edits) while T020 [P] runs alongside; T022 is a separate edit in the same service file as T021 — sequence T021 then T022 then T024 to avoid overlapping hunks; T023 touches the email service file that T013 (US1) also touches — sequence after T013; T025 → T026 is a strict chain (repository then scheduler); T027 [P] (frontend lib) then T028.
- Within US3: T029, T030 [P] (tests, different files); T031, T032, T033 [P] (different files) then T034 (depends on all three) then T035; T036 [P] (frontend lib) then T037 then T038; T039 [P] (i18n, independent).
- Phase 6: T040, T041, T043 [P] (independent verification/doc tasks); T042 is manual and best run last.

---

## Parallel Example: User Story 1

```bash
# Tests, together:
Task: "BookingServiceTest — attachCompanionStudent validation and notification cases"
Task: "BookingAdminControllerIntegrationTest — POST /admin/bookings/{id}/companion-student"
Task: "BookingEmailServiceTest — sendCompanionStudentAttached"

# Implementation, together (before the service task, T014):
Task: "BookingRepository.setCompanionStudentId()"
Task: "AttachCompanionStudentRequest DTO"
Task: "BookingEmailService.sendCompanionStudentAttached()"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Complete Phase 1: Setup (T001).
2. Complete Phase 2: Foundational (T002-T007) — critical, blocks everything.
3. Complete Phase 3: User Story 1 (T008-T015).
4. **STOP and VALIDATE**: run T008/T009/T010 tests, attach a companion student to a real booking via the admin endpoint, confirm the email lands in Mailpit and `GET /admin/bookings` shows both students.
5. Deploy/demo if ready — the teacher can already share a class between two students even before US2's companion-student self-view/reminders or US3's detach/no-show exist.

### Incremental Delivery

1. Setup + Foundational → the columns and rejection vocabulary exist, nothing user-visible changes yet.
2. Add User Story 1 → teacher can attach a companion student → MVP (admin-verifiable).
3. Add User Story 2 → the companion student has full parity (sees it, joins it, can cancel it, gets reminded) and cancellation reaches both students.
4. Add User Story 3 → teacher can correct a mistaken attachment and track attendance per student.
5. Polish → automated + manual verification, docs.

### Parallel Team Strategy

With multiple developers, once Foundational is done: Developer A takes US1 (attach + notify), Developer B takes US2 (companion student's own view + cancellation + reminders) — both need a real attached booking to demo against each other's work, so light coordination is expected even though the files don't overlap; Developer C can start US3 (detach + no-show) in parallel since it touches yet another set of files.

---

## Notes

- [P] tasks touch different files and have no unmet dependency within their phase.
- `second_student_no_show` is only ever meaningful while `second_student_id` is set; detaching (T034) always clears both together so no stale attendance flag survives a detach.
- No migration ever back-fills `second_student_id` — every existing row correctly defaults to `NULL` (no booking has a companion student until the teacher attaches one).
- `BOOKING_NOT_ATTACHABLE` is a new, distinct reason from the existing `STATE` (used for "already cancelled" during a student's own cancellation) — see [research.md](research.md) for why the two aren't merged.
- The reminder-scheduler extension (T025, T026) was added after an `/speckit-analyze` pass caught that `plan.md`/`research.md` committed to it but the original task list omitted it — keeping docs and tasks consistent.
- `sendCompanionStudentAttached` (T013) takes the real `bookingId` (not a random UUID) as its ICS UID, discovered as a correctness fix during implementation — otherwise a later cancellation's CANCEL event wouldn't match the companion student's calendar entry.
- **Correctness fix discovered during T038** (not in the original task list): widening `findByUserId` (T019) meant `StudentProfileAdminService.getProfile` (the `/panel` → student profile page, unrelated to `BookingsTab.tsx`) could now return a booking where the *viewed* student is the companion student — but `toBookingDto` always reported the *primary's* `no_show` flag, and the profile page always called `setBookingNoShow` without a role. Fixed by adding `isCompanionStudent` to `StudentProfileBookingDto` (computed per-viewed-student) and threading it through to the correct `studentRole` on the frontend; added two regression tests to `StudentProfileAdminServiceTest`. This is where "the per-student no-show toggle" (T038) actually lives.
- Commit after each task or logical group; stop at any checkpoint to validate a story independently before continuing.
