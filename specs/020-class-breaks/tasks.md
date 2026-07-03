---

description: "Task list template for feature implementation"
---

# Tasks: Class Booking Buffer Time

**Input**: Design documents from `specs/020-class-breaks/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/schedule-buffer.md](contracts/schedule-buffer.md), [quickstart.md](quickstart.md)

**Tests**: Included — this repo has an established backend integration/unit test convention (`AvailabilityServiceTest`, `BookingServiceTest`, `BookingControllerIntegrationTest`); new backend behavior gets matching test coverage. No frontend code changes are needed for this feature (per plan.md Summary), so no frontend test/verification tasks beyond the manual quickstart pass.

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story. As plan.md and research.md establish, this feature's entire implementation is one widened comparison function plus one config field — User Story 1 carries all the code changes; User Stories 2 and 3 are satisfied by that same implementation by construction (student-agnostic check, `CONFIRMED`-only queries) and get verification tasks only.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths are relative to the repo root (`back-end/`)

---

## Phase 1: Setup

**Purpose**: Project initialization.

Not applicable — no new dependencies, no database migration (research.md Decisions 1 and 3), branch `020-class-breaks` already created. Implementation begins at Phase 2.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The configuration value every user story's implementation and tests depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T001 In `back-end/src/main/java/com/kuky/backend/config/SchedulingProperties.java`, add `bufferMinutes` (default `15`) to the `Scheduling` inner class, with getter/setter, alongside the existing `classDurationMinutes` field (line 21) — per [data-model.md](data-model.md).

**Checkpoint**: `bufferMinutes` is available via `props.getScheduling().getBufferMinutes()`. User story implementation can now begin.

---

## Phase 3: User Story 1 - Automatic breathing room between classes (Priority: P1) 🎯 MVP

**Goal**: No candidate class start/end time within 15 minutes of an existing confirmed booking's start/end is offered on the public schedule or accepted by the booking endpoint, regardless of booking duration (60/90 minutes).

**Independent Test**: Book a class ending at 10:00; confirm the schedule for that day offers no slot starting before 10:15 and no slot ending after 9:45; attempt to force-create a booking in that window via the API and confirm it's rejected with `409 SLOT_UNAVAILABLE`; confirm a slot exactly 15 minutes away is offered and bookable. Matches [quickstart.md](quickstart.md) Scenarios 1–2.

### Tests for User Story 1

- [X] T002 [P] [US1] Extend `back-end/src/test/java/com/kuky/backend/scheduling/AvailabilityServiceTest.java`: given a confirmed booking, `generateSchedule()` marks a candidate slot `UNAVAILABLE` (not `OPEN`) when it starts less than 15 minutes after the booking ends or ends less than 15 minutes before the booking starts, while a slot that actually overlaps the booking remains `BOOKED`, and a slot exactly 15 minutes away is `OPEN`; separately, `validateBookable()` throws `SlotUnavailableException` for a candidate start time 5–14 minutes from an existing confirmed booking (either direction) even though the two windows don't literally overlap, and succeeds for a candidate exactly 15 minutes away. Cover both the 60-minute and 90-minute duration cases (mixed-duration adjacency, building on feature 019's `BookedInterval` coverage).
- [X] T003 [P] [US1] Extend `back-end/src/test/java/com/kuky/backend/scheduling/BookingServiceTest.java`: `createBooking(email, slotStart, durationMinutes)` for a candidate 5–14 minutes from an existing confirmed booking throws `SlotUnavailableException` (surfacing as `409 SLOT_UNAVAILABLE` to the caller); a candidate exactly 15 minutes away succeeds.

### Implementation for User Story 1

- [X] T004 [US1] In `back-end/src/main/java/com/kuky/backend/scheduling/service/AvailabilityService.java`, add a `bufferMinutes` parameter to the private `overlapsAny(Instant start, Instant end, List<BookingRepository.BookedInterval> intervals)` helper: widen the comparison window to `[start - bufferMinutes, end + bufferMinutes)` before intersecting against each booked interval's real `[slotStart, slotStart + duration)`, per [data-model.md](data-model.md). Depends on T001.
- [X] T005 [US1] In `AvailabilityService.generateSchedule()`, keep the existing `overlapsAny(start, end, bookedIntervals, 0)` call to decide `Slot.Status.BOOKED`; when that's `false`, add a second check `overlapsAny(start, end, bookedIntervals, props.getScheduling().getBufferMinutes())` and set the status to `Slot.Status.UNAVAILABLE` when `true`, before falling through to the existing lead-time/`OPEN` branch. Depends on T004.
- [X] T006 [US1] In `AvailabilityService.validateBookable()`, widen the `findConfirmedBookingIntervalsBetween(slotStart, slotEnd)` call to `findConfirmedBookingIntervalsBetween(slotStart.minusSeconds(bufferSeconds), slotEnd.plusSeconds(bufferSeconds))` (`bufferSeconds` derived from `props.getScheduling().getBufferMinutes()`), and replace the exact `overlapsAny(slotStart, slotEnd, nearby)` call with the buffer-widened `overlapsAny(slotStart, slotEnd, nearby, bufferMinutes)`. Depends on T004.

**Checkpoint**: The buffer is enforced end-to-end — the public schedule hides near-adjacent slots and booking creation rejects them — fully functional and testable independently.

---

## Phase 4: User Story 2 - Buffer holds no matter who books the adjacent class (Priority: P2)

**Goal**: Confirm the buffer applies uniformly regardless of which student holds the adjacent booking — no student can bypass it by booking two of their own classes back-to-back.

**Independent Test**: A student with a confirmed booking ending at 11:00 attempts to book another class starting at 11:05; confirm it's rejected the same way (`409 SLOT_UNAVAILABLE`) as it would be against a different student's booking. Matches [quickstart.md](quickstart.md) Scenario 3.

### Tests for User Story 2

- [X] T007 [US2] Extend `back-end/src/test/java/com/kuky/backend/scheduling/BookingServiceTest.java`: the *same* user who holds a confirmed booking ending at time `E` is rejected (`SlotUnavailableException`) when attempting to book another class starting between `E` and `E + 15min`, proving the buffer check (T004–T006) is student-agnostic.

No implementation tasks — User Story 2 is already satisfied by the User Story 1 implementation, since `overlapsAny`/`validateBookable` intersect against every confirmed booking in range without filtering by owner (plan.md Summary, research.md Decision 2).

**Checkpoint**: User Stories 1 and 2 both verified.

---

## Phase 5: User Story 3 - Cancelled classes free up their buffer (Priority: P3)

**Goal**: Confirm a cancelled booking's buffer clears immediately for subsequent schedule reads and booking attempts.

**Independent Test**: Book two classes with a valid gap, cancel one, and confirm slots within its former buffer become bookable again (subject to any other remaining confirmed bookings). Matches [quickstart.md](quickstart.md) Scenario 4.

### Tests for User Story 3

- [X] T008 [P] [US3] Extend `back-end/src/test/java/com/kuky/backend/scheduling/AvailabilityServiceTest.java`: after cancelling a confirmed booking (status → `CANCELLED`), `generateSchedule()` no longer marks its former buffer zone `UNAVAILABLE` — the previously-blocked slots return to `OPEN` (subject to any other remaining bookings).
- [X] T009 [P] [US3] Extend `back-end/src/test/java/com/kuky/backend/scheduling/BookingControllerIntegrationTest.java`: end-to-end — book a class, cancel it, then successfully create a new booking inside the old buffer zone via `POST /bookings`.

No implementation tasks — this behavior is inherent to `findConfirmedBookingIntervalsBetween` only ever selecting `status = 'CONFIRMED'` rows, unchanged by this feature (research.md Decision 5, extended to buffer zones).

**Checkpoint**: All three user stories independently functional and verified.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification once all three stories are complete.

- [X] T010 Run `cd back-end && ./gradlew test && ./gradlew build` — confirm all new/extended tests (T002, T003, T007, T008, T009) and the full existing suite pass, including feature 019's mixed-duration overlap tests (unaffected by the added `bufferMinutes` parameter defaulting to `0` at their call sites).
- [X] T011 Execute [quickstart.md](quickstart.md) Scenarios 1–6 manually against the running dev servers, per the constitution's browser-verification requirement — confirming in particular that no frontend code change was needed (`UNAVAILABLE` slots already render correctly) and that Scenario 5 (day-window boundaries) and Scenario 6 (pre-existing close bookings) hold with no extra code.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Not applicable.
- **Foundational (Phase 2)**: No dependencies — BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational (T001) only. Independently testable/shippable as MVP — it is the entire feature's behavior change.
- **User Story 2 (Phase 4)**: Depends on Foundational + Phase 3's implementation tasks (T004–T006) existing, but adds no new implementation, only a confirming test.
- **User Story 3 (Phase 5)**: Same shape as Phase 4 — depends on Phase 3's implementation existing, adds only confirming tests.
- **Polish (Phase 6)**: Depends on Phases 3–5 all being complete.

### Within Each User Story

- Tests are listed before implementation tasks (written first per repo's integration-test convention) but are not strict red-green TDD gates.
- T004 (the widened `overlapsAny` helper) must land before T005 and T006, which are its two call sites — all three are in the same file (`AvailabilityService.java`), so they run sequentially, not in parallel.

### Parallel Opportunities

- Foundational: T001 is the only task.
- Within US1: T002, T003 [P] (tests, different files: `AvailabilityServiceTest.java` vs. `BookingServiceTest.java`); T004 → T005 → T006 is a strict same-file chain.
- Within US3: T008, T009 [P] (different files).
- Phase 6: T010 and T011 can run in either order; T011 is manual and is typically run last.

---

## Parallel Example: User Story 1

```bash
# Launch both test-extension tasks for User Story 1 together (different files):
Task: "Extend AvailabilityServiceTest.java with buffer cases for generateSchedule() and validateBookable()"
Task: "Extend BookingServiceTest.java with a buffer-rejection case for createBooking()"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (T001).
2. Complete Phase 3: User Story 1 (T002–T006) — this alone delivers the entire requested behavior.
3. **STOP and VALIDATE**: Run quickstart.md Scenarios 1–2, 5 against the MVP.
4. Deploy/demo if ready.

### Incremental Delivery

1. Complete Foundational → config ready.
2. Add User Story 1 → test independently → deploy/demo (MVP!).
3. Add User Story 2 → confirming test only → deploy/demo.
4. Add User Story 3 → confirming test only → deploy/demo.
5. Polish: full suite + manual quickstart pass.

---

## Notes

- [P] tasks = different files, no dependencies.
- [Story] label maps task to specific user story for traceability.
- No frontend files are touched anywhere in this task list (plan.md Summary) — `Slot.Status.UNAVAILABLE` already renders correctly in `SlotGrid.tsx`.
- No database migration anywhere in this task list (research.md Decisions 1 and 3).
- Commit after each task or logical group.
- Stop at any checkpoint to validate story independently.
