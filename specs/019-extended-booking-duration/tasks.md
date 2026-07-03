---

description: "Task list template for feature implementation"
---

# Tasks: Extended Booking Duration (1.5-Hour Classes)

**Input**: Design documents from `specs/019-extended-booking-duration/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/booking-duration-api.md](contracts/booking-duration-api.md), [quickstart.md](quickstart.md)

**Tests**: Included — this repo has an established backend integration/unit test convention (`BookingControllerIntegrationTest`, `BookingServiceTest`, `AvailabilityServiceTest`, `StudentAdminControllerIntegrationTest`, etc.); new backend behavior gets matching test coverage. Frontend has no test framework (per constitution) — frontend verification is manual/browser-based via [quickstart.md](quickstart.md).

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: The schema change every other task depends on.

- [X] T001 Write `back-end/src/main/resources/db/migration/V29__extended_booking_duration.sql` per [data-model.md](data-model.md): `ALTER TABLE users ADD COLUMN extended_class_eligible BOOLEAN NOT NULL DEFAULT false;`, then `DROP INDEX bookings_active_slot_uniq;` and `ALTER TABLE bookings ADD CONSTRAINT bookings_no_overlap EXCLUDE USING gist (tstzrange(slot_start, slot_start + (duration_minutes || ' minutes')::interval, '[)') WITH &&) WHERE (status = 'CONFIRMED');`. No `UPDATE` statement needed — see data-model.md for why existing rows already satisfy both changes. **Revised during implementation**: that inline expression failed with `functions in index expression must be marked IMMUTABLE` (`timestamptz + interval` is STABLE, not IMMUTABLE, in PostgreSQL, regardless of how the interval is built). Fixed by adding a real stored `slot_end TIMESTAMPTZ NOT NULL` column (backfilled via `UPDATE`, then populated by `BookingRepository.insert()` going forward) and building the exclusion constraint from `tstzrange(slot_start, slot_end, '[)')` instead — see the updated data-model.md/research.md.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The second duration value and the `User` model change that both US1 (writes the flag) and US2 (reads the flag) depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] In `back-end/src/main/java/com/kuky/backend/config/SchedulingProperties.java`, add `extendedClassDurationMinutes` (default `90`) to the `Scheduling` inner class, with getter/setter, alongside the existing `classDurationMinutes` field (line 21).
- [X] T003 [P] In `back-end/src/main/java/com/kuky/backend/auth/model/User.java`, add `private boolean extendedClassEligible;` plus `isExtendedClassEligible()`/`setExtendedClassEligible(boolean)` accessors, following the existing `timezoneIsManual` boolean pattern (lines 19, 45-46).
- [X] T004 [P] In `back-end/src/main/java/com/kuky/backend/auth/repository/UserRepository.java`, extend `USER_MAPPER` (or equivalent row-mapping code) to map the new `extended_class_eligible` column onto `User.setExtendedClassEligible(...)`, alongside the existing column mappings.

**Checkpoint**: `User` carries the eligibility flag end-to-end at the persistence layer; the extended duration value exists in config. User stories can now begin.

---

## Phase 3: User Story 1 - Teacher grants a student permission for 1.5-hour classes (Priority: P1) 🎯 MVP

**Goal**: The teacher can grant or revoke "extended class" eligibility for an individual student from the admin panel; the flag persists, transitions are idempotent, and a notification email is sent.

**Independent Test**: From `/panel` → Students tab, grant eligibility to a test student; confirm the row reflects eligible status and a "you can now book 1.5-hour classes" email lands in Mailpit; grant again and confirm it's a no-op; revoke and confirm the row updates and a revocation email arrives. Testable via the admin endpoints alone, without US2's booking flow existing yet (mirrors how feature 012's grant flow was verified before its frontend messaging shipped).

### Tests for User Story 1

- [X] T005 [P] [US1] Write `back-end/src/test/java/com/kuky/backend/admin/StudentAdminControllerIntegrationTest.java` (extend if it already covers `/student`, else create) covering: `POST /admin/users/{id}/extended-class` grants eligibility and returns `200 { id, extendedClassEligible: true }`; calling it again on an already-eligible id is idempotent (`200`, no error); `DELETE /admin/users/{id}/extended-class` revokes and returns `200 { extendedClassEligible: false }`, idempotent the same way; `404 USER_NOT_FOUND` for an unknown id or an `ADMIN` id (both endpoints); `403 ACCESS_DENIED` for a non-admin caller. Also covers FR-013 directly here (`revokeExtendedClass_...preservesExistingBookingDuration`), since it needs no US2 code.
- [X] T006 [P] [US1] Extend `back-end/src/test/java/com/kuky/backend/auth/EmailServiceTest.java` with cases for `sendExtendedClassGrantedEmail(String toEmail)` and `sendExtendedClassRevokedEmail(String toEmail)`, asserting a `SimpleMailMessage` is sent to the given recipient with a non-empty Spanish subject/body, mirroring the existing `sendStudentGrantedEmail`/`sendStudentRevokedEmail` tests.

### Implementation for User Story 1

- [X] T007 [P] [US1] In `back-end/src/main/java/com/kuky/backend/auth/repository/UserRepository.java`, add `grantExtendedClassById(UUID id)` (`UPDATE users SET extended_class_eligible = true, updated_at = NOW() WHERE id = :id AND extended_class_eligible = false`, returning rows-affected) and `revokeExtendedClassById(UUID id)` (same shape, flipped), mirroring `promoteToStudentById`/`revokeStudentById` (lines 73-84).
- [X] T008 [P] [US1] Create `back-end/src/main/java/com/kuky/backend/admin/dto/ExtendedClassEligibilityResponse.java` — `public record ExtendedClassEligibilityResponse(UUID id, boolean extendedClassEligible) {}`, mirroring `UserRoleResponse`.
- [X] T009 [P] [US1] In `back-end/src/main/java/com/kuky/backend/auth/service/EmailService.java`, add `sendExtendedClassGrantedEmail(String toEmail)` and `sendExtendedClassRevokedEmail(String toEmail)`, following the exact `SimpleMailMessage` pattern of `sendStudentGrantedEmail`/`sendStudentRevokedEmail` (Spanish plain text, `fromAddress`, no link/token).
- [X] T010 [US1] In `back-end/src/main/java/com/kuky/backend/admin/controller/StudentAdminController.java`, add `POST /users/{id}/extended-class` and `DELETE /users/{id}/extended-class`, reusing the existing `requireGrantableOrRevocableUser(id)` guard (lines 99-106) exactly as `grantStudent`/`revokeStudent` do, then calling the new repository methods (T007) and best-effort email methods (T009, logged warning on failure, matching lines 73-79/88-93). Depends on T007, T008, T009, **T011** (same file — T011's edit to `getStudents()` must land first to avoid touching `StudentAdminController.java` concurrently).
- [X] T011 [P] [US1] **Revised during implementation**: `StudentResponse` is shared by the homework-assignment and presentation-sharing pickers (`PresentationService.java`, `UnitRepository.java`), which broke when eligibility was added to it directly. Reverted `StudentResponse` to its original shape; instead added `UserRepository.findExtendedClassEligibleStudentIds()` and `GET /admin/students/extended-class-eligible-ids` (`StudentAdminController`) returning just the eligible ids, which the Students tab merges client-side with `getStudents()`.
- [X] T012 [P] [US1] In `front-end/src/lib/admin.ts`, add `grantExtendedClass(id: string)` (`POST /users/${id}/extended-class`) and `revokeExtendedClass(id: string)` (`DELETE /users/${id}/extended-class`), returning `{ id, extendedClassEligible }`; add `extendedClassEligible: boolean` to the `Student` interface (line 36-42), per [contracts/booking-duration-api.md](contracts/booking-duration-api.md).
- [X] T013 [US1] In `front-end/src/components/admin/students/StudentsTab.tsx`, add a grant/revoke "extended class" action per row (button or toggle reflecting `s.extendedClassEligible`), calling `grantExtendedClass`/`revokeExtendedClass` and updating local state on success, mirroring the existing revoke-student button's loading/error handling (lines 26-38, 88-98). Depends on T012.
- [X] T014 [P] [US1] Add the new UI copy (grant/revoke button labels, confirm prompt, error message) to the front-end i18n locale files (`front-end/src/i18n/locales/es.ts` primary, `ro.ts`), under an `admin.students.extendedClass.*` (or similar) namespace.

**Checkpoint**: Teacher can grant/revoke extended-class eligibility from `/panel`, verifiable via the admin endpoints and Mailpit even before Phase 4's booking flow consumes the flag.

---

## Phase 4: User Story 2 - Eligible student books a 1.5-hour class (Priority: P1)

**Goal**: An eligible student can choose a 90-minute class when booking; only genuinely-open 90-minute start times are offered; the booking is created, blocks the full window, and is rejected server-side for ineligible students or invalid durations.

**Independent Test**: Grant a test student eligibility (Phase 3), open `/reservas`, switch to the 1.5-hour option, confirm the offered start times differ from the 1-hour list and only include times with 90 free contiguous minutes, book one, and confirm the reservation blocks the full window (a subsequent 1-hour or 1.5-hour request overlapping it is rejected). Separately, confirm a non-eligible student's direct `POST /bookings` with `durationMinutes: 90` is rejected with `403`.

### Tests for User Story 2

- [X] T015 [P] [US2] Extend `back-end/src/test/java/com/kuky/backend/scheduling/AvailabilityServiceTest.java`: `generateSchedule(90)` chunks each availability window in 90-minute steps and only marks a candidate window `OPEN` when no confirmed booking (of any duration) overlaps it; `generateSchedule(60)` behavior is unchanged; `validateBookable(slotStart, 90)` rejects a start time that doesn't have 90 contiguous available minutes and rejects one that overlaps an existing confirmed booking of a different duration; `validateBookable(slotStart, 60)` behavior is unchanged. **Additionally (FR-008): after cancelling a confirmed 90-minute booking (status → `CANCELLED`), `generateSchedule(90)` and `generateSchedule(60)` both mark its former window `OPEN` again — confirming `findConfirmedBookingIntervalsBetween` (T020) correctly excludes non-`CONFIRMED` rows.**
- [X] T016 [P] [US2] Extend `back-end/src/test/java/com/kuky/backend/scheduling/BookingServiceTest.java`: `createBooking(email, slotStart, 90)` for an eligible user succeeds, persists `durationMinutes = 90`, and passes `90` to the mocked `meetingProvider.create(...)` and `emailService.sendConfirmation(...)`; the same call for a non-eligible user throws `BookingNotAllowedException` with reason `NOT_ELIGIBLE_FOR_EXTENDED`; a `durationMinutes` outside `{60, 90}` throws reason `INVALID_DURATION`; a `DataIntegrityViolationException` from the repository insert (simulating the exclusion-constraint rejecting an overlap) is still translated to `SlotUnavailableException`.
- [X] T017 [P] [US2] Extend `back-end/src/test/java/com/kuky/backend/scheduling/BookingControllerIntegrationTest.java`: `POST /bookings` with `durationMinutes: 90` returns `201` for an eligible student and `403 EXTENDED_CLASS_NOT_ELIGIBLE` for a non-eligible one; `durationMinutes: 45` (or any non-60/90 value) returns `422 INVALID_DURATION`; `GET /schedule?durationMinutes=90` returns slots on the 90-minute grid; `GET /schedule?durationMinutes=45` returns `422 INVALID_DURATION`. (FR-013 is already covered by T005's `revokeExtendedClass_...preservesExistingBookingDuration` test, which needed no booking-duration code to exist.)

### Implementation for User Story 2

- [X] T018 [P] [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/exception/BookingNotAllowedException.java`, add `INVALID_DURATION` and `NOT_ELIGIBLE_FOR_EXTENDED` to the `Reason` enum (alongside `RANGE, LEAD, STATE, CUTOFF, NOT_ELIGIBLE_FOR_NO_SHOW`).
- [X] T019 [P] [US2] In `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java`, extend the `handleBookingNotAllowed` switch (per [contracts/booking-duration-api.md](contracts/booking-duration-api.md)) with `INVALID_DURATION -> 422 { "error": "INVALID_DURATION", ... }` and `NOT_ELIGIBLE_FOR_EXTENDED -> 403 { "error": "EXTENDED_CLASS_NOT_ELIGIBLE", ... }`, both with Spanish messages matching the file's existing style.
- [X] T020 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/repository/BookingRepository.java`, replace `findConfirmedSlotStartsBetween(Instant, Instant)` (lines 47-56) with `findConfirmedBookingIntervalsBetween(Instant from, Instant to)` returning a `List` of a small `record BookedInterval(Instant slotStart, int durationMinutes)`, selecting `slot_start, duration_minutes` instead of `slot_start` alone.
- [X] T021 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/service/AvailabilityService.java`: parameterize `generateSchedule(int durationMinutes)` and `validateBookable(Instant slotStart, int durationMinutes)` (removing their internal `props.getScheduling().getClassDurationMinutes()` reads); replace the `bookedStarts: Set<Instant>` overlap check (lines 61-63, 83) with a helper that, given the `BookedInterval` list from T020, returns whether a candidate `[start, start + durationMinutes)` window overlaps any existing interval; use that helper both in `generateSchedule`'s per-slot status computation and as a new explicit check inside `validateBookable` (previously implicitly relied on the DB unique index alone). Depends on T020.
- [X] T022 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/controller/ScheduleController.java`, add an optional `@RequestParam(required = false, defaultValue = "60") int durationMinutes` to `getSchedule()`, validate it's either `props.getScheduling().getClassDurationMinutes()` or `props.getScheduling().getExtendedClassDurationMinutes()` (else throw `BookingNotAllowedException.Reason.INVALID_DURATION`), and pass it to `availabilityService.generateSchedule(durationMinutes)`. Depends on T002, T021.
- [X] T023 [P] [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/dto/CreateBookingRequest.java`, add `@NotNull(message = "La duración de la clase es obligatoria.") Integer durationMinutes` alongside the existing `slotStart` field.
- [X] T024 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java`, `createBooking`: accept a `durationMinutes` parameter; validate it's one of the two configured values (else `BookingNotAllowedException.Reason.INVALID_DURATION`); if it equals the extended value, load the user first and check `user.isExtendedClassEligible()` (else `Reason.NOT_ELIGIBLE_FOR_EXTENDED`); replace `int duration = props.getScheduling().getClassDurationMinutes();` (line 55) with the validated request value; call `availabilityService.validateBookable(slotStart, duration)`; broaden `catch (DuplicateKeyException e)` (line 66) to `catch (DataIntegrityViolationException e)` so the new exclusion-constraint rejection still becomes `SlotUnavailableException`. Depends on T003, T018, T021, T023.
- [X] T025 [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/controller/BookingController.java`, pass `request.durationMinutes()` through to `bookingService.createBooking(email, request.slotStart(), request.durationMinutes())`. Depends on T023, T024.
- [X] T026 [P] [US2] In `back-end/src/main/java/com/kuky/backend/auth/dto/UserResponse.java` (and wherever `User` → `UserResponse` mapping happens, e.g. `AuthController`/`AuthService`), add `extendedClassEligible` sourced from `user.isExtendedClassEligible()`, so `getMe()`/`login()`/`register()` expose it. Depends on T003.
- [X] T027 [P] [US2] In `front-end/src/lib/scheduling.ts`, change `getSchedule()` to `getSchedule(durationMinutes: number = 60)` appending `?durationMinutes=${durationMinutes}`; change `createBooking(slotStart)` to `createBooking(slotStart: string, durationMinutes: number)` sending both fields in the body.
- [X] T028 [P] [US2] In `front-end/src/lib/auth.ts`, add `extendedClassEligible: boolean` to the `UserResponse` interface (line 15-26).
- [X] T029 [US2] In `front-end/src/components/scheduling/ScheduleView.tsx`: add duration selection state (default `60`); compute `canBookExtended = (user?.role === "STUDENT" || user?.role === "ADMIN") && user?.extendedClassEligible`; refetch the schedule via `getSchedule(selectedDuration)` when the duration changes; render a duration toggle (only interactive when `canBookExtended`, otherwise fixed to 60/hidden for the 90-minute option); pass the selected duration down to `BookingDialog`. Depends on T027, T028.
- [X] T030 [US2] In `front-end/src/components/scheduling/BookingDialog.tsx`: accept a `durationMinutes` prop, pass it to `createBooking(slot.start, durationMinutes)` (line 72); add `EXTENDED_CLASS_NOT_ELIGIBLE` and `INVALID_DURATION` entries to the `getErrorMessage` map (lines 56-65). Depends on T029.
- [X] T031 [P] [US2] Add the duration-toggle labels and the two new error messages to the front-end i18n locale files (`es.ts` primary, `ro.ts`), under the existing `schedule.*` namespace.

**Checkpoint**: An eligible student can complete a 1.5-hour booking end-to-end; a non-eligible one is blocked server-side regardless of UI; overlapping bookings across mixed durations are impossible.

---

## Phase 5: User Story 3 - Booking length is visible after booking (Priority: P2)

**Goal**: A student sees the correct start–end time range for a class, of either length, in the booking confirmation and in "My Bookings".

**Independent Test**: Book a 1-hour and a 1.5-hour class (as an eligible student); confirm the booking confirmation dialog and the "My Bookings" list both show the correct time range (not just the start time) for each.

### Implementation for User Story 3

- [X] T032 [US3] In `front-end/src/components/scheduling/BookingDialog.tsx`, update the confirmed-state copy (lines 115-123) to show a start–end time range (`formatSlotDateTime(slot.start, ...) – formatTime(booking.slotEnd, ...)` or equivalent) instead of the start time alone.
- [X] T033 [US3] In `front-end/src/components/scheduling/MyBookings.tsx`, update `formatSlot`/`BookingCard` (lines 13-22, 41) to render a `slotStart`–`slotEnd` range (both fields already exist on `BookingSummary`) instead of `slotStart` alone.

**Checkpoint**: Every booking a student can see (confirmation, My Bookings) shows its real duration.

---

## Phase 6: User Story 4 - Teacher sees class length in her schedule (Priority: P3)

**Goal**: The teacher can distinguish class lengths in her admin schedule view.

**Independent Test**: With a mix of 1-hour and 1.5-hour bookings, open `/panel` → Bookings, confirm each shows its own correct duration/end time.

### Implementation for User Story 4

- [X] T034 [US4] Verify `front-end/src/components/admin/bookings/BookingsTab.tsx:100` (`formatSlot(b.slotStart, b.slotEnd, teacherTimezone)`) and the backend query behind `AdminBookingDto.slotEnd` (in `BookingAdminController`/its repository) already reflect each booking's real `duration_minutes` once T024 lands — per [research.md](research.md) Decision 5, no code change is expected here. If the underlying query independently hardcodes a 60-minute end time rather than deriving it from `Booking.getSlotEnd()`/the stored `duration_minutes`, fix it there.

**Checkpoint**: All four user stories are independently functional; the teacher's schedule was already duration-aware and needed no new plumbing.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verification and documentation once all four stories are complete.

- [X] T035 [P] Run `cd back-end && ./gradlew test && ./gradlew build` — confirm all new/extended tests (T005, T006, T015-T017) and the full existing suite pass.
- [X] T036 [P] Run `cd front-end && npm run lint && npm run format` — confirm no lint/format regressions from T012-T014, T027-T033.
- [X] T037 Execute [quickstart.md](quickstart.md) Scenarios 1-6 manually against the running dev servers (including checking Mailpit at `http://localhost:8025` for the grant/revoke emails), per the constitution's browser-verification requirement.
- [X] T038 [P] Add a "Booking duration" (or extend the existing "Availability") bullet under **Back-end** in `CLAUDE.md` documenting the two fixed durations (60/90), the `extended_class_eligible` per-student flag and where it's granted, and the overlap-exclusion constraint replacing the old unique index — mirroring the existing terse style of that section.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T001) — BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational only. Independently testable/shippable as MVP (grant/revoke works and is verifiable via the admin endpoints + Mailpit, even before Phase 4's booking flow consumes the flag).
- **User Story 2 (Phase 4)**: Depends on Foundational only. Functionally more valuable once Phase 3 exists (someone needs to be eligible to demonstrate it end-to-end), but has no *code* dependency on Phase 3's files.
- **User Story 3 (Phase 5)**: Depends on Foundational; its display changes only become meaningful once Phase 4 produces bookings with real (non-60) durations, but touches no file Phase 4 doesn't already touch in this same session (`BookingDialog.tsx`), so sequence Phase 4 → Phase 5 in practice even though there's no hard blocking dependency on Phase 3.
- **User Story 4 (Phase 6)**: Verification-only; meaningfully checkable once Phase 4 exists (needs a real 90-minute booking to look at).
- **Polish (Phase 7)**: Depends on Phases 3-6 all being complete.

### Within Each User Story

- Tests are listed before implementation tasks (write/skim them first per repo's integration-test convention) but are not strict red-green TDD gates.
- Repository/DTO/exception tasks before the service/controller task that wires them together.
- Backend before the frontend tasks that call it.

### Parallel Opportunities

- Foundational: T002, T003, T004 (three different files, no interdependency).
- Within US1: T005, T006 [P] (tests) and T007, T008, T009 [P] (implementation, different files); T011 [P] runs alongside them too but also touches `StudentAdminController.java` — T010 waits for T007-T009 **and T011**; T012 waits for T011's backend shape; T014 is independent.
- Within US2: T015, T016, T017 [P] (tests, different files); T018, T019, T023, T026 [P] (small isolated implementation files); T020 → T021 → T022 is a strict chain; T024 waits for T018, T021, T023; T025 waits for T024; T027, T028, T031 [P] (frontend lib/i18n files); T029 waits for T027/T028; T030 waits for T029.
- Phase 7: T035, T036, T038 [P] (independent verification/doc tasks); T037 is manual and best run last.

---

## Parallel Example: User Story 1

```bash
# Tests, together:
Task: "StudentAdminControllerIntegrationTest — POST/DELETE /admin/users/{id}/extended-class"
Task: "EmailServiceTest — sendExtendedClassGrantedEmail/sendExtendedClassRevokedEmail"

# Implementation, together (before the controller task, T010):
Task: "UserRepository.grantExtendedClassById() + revokeExtendedClassById()"
Task: "ExtendedClassEligibilityResponse DTO"
Task: "EmailService.sendExtendedClassGrantedEmail()/sendExtendedClassRevokedEmail()"
Task: "StudentResponse + getStudents() extendedClassEligible field"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Complete Phase 1: Setup (T001).
2. Complete Phase 2: Foundational (T002-T004) — critical, blocks everything.
3. Complete Phase 3: User Story 1 (T005-T014).
4. **STOP and VALIDATE**: run T005/T006 tests, grant a test student eligibility via `/panel`, confirm the flag persists and emails arrive in Mailpit.
5. Deploy/demo if ready — the teacher can already curate who's eligible even before Phase 4's booking flow exists to consume it.

### Incremental Delivery

1. Setup + Foundational → the flag and the second duration value exist, nothing user-visible changes yet.
2. Add User Story 1 → teacher can grant/revoke eligibility → MVP (admin-verifiable).
3. Add User Story 2 → eligible students can actually book 1.5-hour classes, with full overlap/eligibility enforcement.
4. Add User Story 3 → booking length is visible everywhere a student looks.
5. Add User Story 4 → (verification only) confirm the teacher's view was already correct.
6. Polish → automated + manual verification, docs.

### Parallel Team Strategy

With multiple developers, once Foundational is done: Developer A takes US1 (admin CRUD), Developer B takes US2 (the larger booking-flow story) in parallel — no file overlap between them until Phase 5/6, which naturally come after.

---

## Notes

- [P] tasks touch different files and have no unmet dependency within their phase.
- The overlap-exclusion constraint (T001) and its matching application-level check (T021) are both required — the DB constraint is a correctness backstop for races, not a substitute for showing correct availability to the user.
- `AdminBookingDto`/`BookingsTab.tsx` (US4) are expected to need **no code change** — verify per T034 rather than assuming a task is missing.
- No migration ever back-fills `extended_class_eligible` — every existing row correctly defaults to `false` (nobody is eligible until the teacher grants it).
- Commit after each task or logical group; stop at any checkpoint to validate a story independently before continuing.
