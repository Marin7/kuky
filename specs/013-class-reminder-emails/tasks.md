# Tasks: Class Reminder Emails

**Input**: Design documents from `/specs/013-class-reminder-emails/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [quickstart.md](quickstart.md)

**Tests**: Included — plan.md and quickstart.md both specify a concrete automated test strategy (JUnit 5 + Mockito unit tests against a fixed `Clock`, plus a real-DB repository test matching the existing `BookingControllerIntegrationTest` convention).

**Organization**: Tasks are grouped by user story (US1/US2 = P1, US3 = P2) per spec.md. This is a back-end-only change; no front-end tasks.

**Note on FR-005 / the late-booking edge case**: research.md establishes that `SchedulingProperties.minLeadHours=24` already makes it impossible to create a booking less than 24h before its start, so no task implements special-case handling for it — the plain "due" query naturally never encounters that scenario.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- File paths are relative to the repository root

---

## Phase 1: Setup

**Purpose**: Application-wide prerequisite for any scheduled job to run

- [X] T001 Add `@EnableScheduling` to `back-end/src/main/java/com/kuky/backend/BackEndApplication.java` so `@Scheduled` methods run

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared data layer that every user story's scheduler logic depends on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 Create Flyway migration `back-end/src/main/resources/db/migration/V24__add_booking_reminder_sent.sql` adding nullable `reminder_sent_at TIMESTAMPTZ` column to `bookings` (see [data-model.md](data-model.md))
- [X] T003 Add `reminderSentAt` field with getter/setter to `back-end/src/main/java/com/kuky/backend/scheduling/model/Booking.java`, mirroring the existing `cancelledAt` field (depends on T002)
- [X] T004 In `back-end/src/main/java/com/kuky/backend/scheduling/repository/BookingRepository.java`: add a `ReminderDueView(UUID id, String email, Instant slotStart, String zoomJoinUrl)` record, a `findBookingsDueForReminder(Instant now)` query (join `users`, `WHERE status = 'CONFIRMED' AND reminder_sent_at IS NULL AND slot_start > :now AND slot_start <= :now + 24h`, ordered by `slot_start`), and a `claimReminder(UUID id, Instant now)` method that runs `UPDATE bookings SET reminder_sent_at = :now WHERE id = :id AND reminder_sent_at IS NULL` and returns whether a row was updated (depends on T002, T003)
- [X] T005 Add `back-end/src/test/java/com/kuky/backend/scheduling/BookingReminderRepositoryTest.java` (`@SpringBootTest(webEnvironment = MOCK)`, `@ActiveProfiles("local")`, real DB via `JdbcTemplate`, matching `BookingControllerIntegrationTest`'s setup/cleanup pattern): insert a `CONFIRMED` booking with `slot_start` ~23h out and assert `findBookingsDueForReminder` returns it; call `claimReminder` and assert it returns `true` once and `false` on a second call for the same id (depends on T004)

**Checkpoint**: Foundation ready — due-booking detection and duplicate-prevention are verified against the real schema; user story implementation can now begin

---

## Phase 3: User Story 1 - Student receives a class reminder (Priority: P1) 🎯 MVP

**Goal**: The student gets an email ~24h before their confirmed class.

**Independent Test**: Seed a `CONFIRMED` booking due within the reminder window, invoke the scheduler's poll logic directly, and confirm the student's email address receives exactly one reminder containing the class date/time and join link.

### Implementation for User Story 1

- [X] T006 [US1] Add `sendReminderToStudent(String studentEmail, Instant slotStart, String joinUrl)` to `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java`, reusing the existing `sendQuietly` helper and the `FMT` UTC date formatter (same convention as `sendConfirmation`)
- [X] T007 [US1] Create `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingReminderScheduler.java` (`@Component`), injecting `Clock`, `BookingRepository`, `BookingEmailService`; add a `@Scheduled(fixedDelay = 900000)` method that calls `findBookingsDueForReminder(clock.instant())`, and for each result calls `claimReminder(id, clock.instant())` and — only if the claim succeeded — `emailService.sendReminderToStudent(...)` (depends on T004, T006, T001)
- [X] T008 [US1] Create `back-end/src/test/java/com/kuky/backend/scheduling/BookingReminderSchedulerTest.java` (pure Mockito unit test, fixed `Clock`, mocked `BookingRepository` + `BookingEmailService`, matching `AvailabilityServiceTest`'s style): assert that a due+claimable booking triggers exactly one `sendReminderToStudent` call with the correct email/time/join-link; assert that a booking whose claim fails (simulating a concurrent poll) triggers no email call; assert that an empty due-list triggers no email calls (depends on T007)

**Checkpoint**: User Story 1 is fully functional and independently testable — students receive reminders

---

## Phase 4: User Story 2 - Teacher receives a class reminder (Priority: P1)

**Goal**: The teacher gets an email ~24h before each confirmed class on their schedule, in the same poll pass as the student's.

**Independent Test**: Using the same due-booking scenario as US1, confirm the configured teacher email (`SchedulingProperties.getTeacherEmail()`) also receives exactly one reminder identifying the student and class time.

### Implementation for User Story 2

- [X] T009 [US2] Add `sendReminderToTeacher(String teacherEmail, String studentEmail, Instant slotStart, String joinUrl)` to `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java`, mirroring `sendReminderToStudent`'s pattern but identifying the student in the subject/body (same style as `sendConfirmation`'s teacher notification) (depends on T006)
- [X] T010 [US2] Inject `SchedulingProperties` into `BookingReminderScheduler` and extend its scheduled method to also call `emailService.sendReminderToTeacher(props.getScheduling().getTeacherEmail(), ...)` for each successfully-claimed booking, alongside the existing student call (depends on T007, T009)
- [X] T011 [US2] Extend `BookingReminderSchedulerTest.java` with an assertion that `sendReminderToTeacher` is called with the configured teacher email and the correct student/time/join-link for a due+claimable booking (depends on T008, T010)

**Checkpoint**: User Stories 1 AND 2 both work — every due class reminds both participants in one pass

---

## Phase 5: User Story 3 - Cancelled classes don't generate reminders (Priority: P2)

**Goal**: A class cancelled before its reminder window is reached never triggers a reminder to either party.

**Independent Test**: Seed a `CANCELLED` booking whose `slot_start` falls inside what would otherwise be the reminder window, run the poll logic, and confirm neither `sendReminderToStudent` nor `sendReminderToTeacher` is called for it.

### Implementation for User Story 3

- [X] T012 [US3] Extend `BookingReminderRepositoryTest.java` with a case: insert a `CANCELLED` booking with `slot_start` ~23h out and assert `findBookingsDueForReminder` does NOT return it, even though it falls inside the time window (depends on T005) — added proactively alongside T005's file creation as `findBookingsDueForReminder_excludesCancelledBookings`
- [X] T013 [US3] Extend `BookingReminderSchedulerTest.java` with a case: given `findBookingsDueForReminder` returns no rows (as it would for an all-cancelled set, per T012's guarantee), assert the scheduler makes zero calls to `claimReminder`, `sendReminderToStudent`, or `sendReminderToTeacher` (depends on T011)

**Checkpoint**: All user stories are independently functional — cancelled classes are confirmed never to generate reminders, at both the query and scheduler level

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T014 Run `back-end/./gradlew test --tests "*BookingReminder*"` and confirm all new tests pass
- [ ] T015 Run the manual quickstart validation in [quickstart.md](quickstart.md) (Mailpit end-to-end check) against a local `bootRun`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (scheduling must be enabled for the eventual scheduler to run) — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational
- **User Story 2 (Phase 4)**: Depends on Foundational and on US1's `BookingReminderScheduler`/`BookingEmailService` files (extends them in place — sequential, not parallel, with US1)
- **User Story 3 (Phase 5)**: Depends on Foundational (T005) and on US2 (T011) for its scheduler-level assertion
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### Within Each Phase

- T002 → T003 → T004 → T005 (migration before model before repository before repository test)
- T006 → T007 → T008 (email method before scheduler before scheduler test)
- T009 → T010 → T011 (teacher email method before scheduler extension before test extension)
- T012 depends only on T005 (same file, additive); T013 depends on T011 (same file, additive)

### Parallel Opportunities

- T001 (Setup) can run in parallel with T002 (Foundational migration) — different files, no dependency
- Beyond that, this feature's tasks are highly sequential (small number of shared files: `BookingRepository.java`, `BookingEmailService.java`, `BookingReminderScheduler.java`, and their two test files), so parallelization opportunities are limited by design — the feature is small and tightly coupled

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 — students start receiving reminders
4. **STOP and VALIDATE**: Run T008's tests independently
5. Deploy/demo if ready — teacher reminders (US2) can follow as a fast-follow since they touch the same files

### Incremental Delivery

1. Setup + Foundational → due-booking detection and dedup verified against real DB
2. US1 → student reminders live (MVP)
3. US2 → teacher reminders added to the same pass
4. US3 → cancellation guardrail explicitly verified at both query and scheduler level
5. Polish → full test run + manual Mailpit check
