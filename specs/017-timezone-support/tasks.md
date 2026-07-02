---

description: "Task list for Timezone Support"

---

# Tasks: Timezone Support

**Input**: Design documents from `/specs/017-timezone-support/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/timezone.md](./contracts/timezone.md), [quickstart.md](./quickstart.md)

**Tests**: Not explicitly requested in the feature spec; a handful of back-end unit/regression tests are still included in Polish (Phase 6) because the plan's Constitution Check and Testing Context call for JUnit coverage of the new zone-aware formatting logic — front-end has no test framework in this repo (per plan.md Technical Context) so front-end verification is the manual `quickstart.md` walkthrough.

**Organization**: Tasks are grouped by user story (US1/US2/US3, matching spec.md's P1/P2/P3) so each can be implemented and verified independently after the shared Foundational phase.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- File paths are relative to the repository root

---

## Phase 1: Setup

**Purpose**: Confirm no new project scaffolding/dependencies are needed before touching existing code.

- [X] T001 Confirm no new dependency is required: check `back-end/build.gradle` and `front-end/package.json` still need no timezone library (per research.md — native `java.time` and `Intl.DateTimeFormat` cover every requirement); no edits expected, this is a verification checkpoint before Phase 2.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Persist and expose the student's time zone preference — required by User Story 1 (display) and User Story 2 (emails). User Story 3 does **not** depend on this phase (see Dependencies section below) and may start in parallel.

**⚠️ CRITICAL**: T012–T018 (User Story 1) and T019–T023 (User Story 2) cannot start until this phase is complete.

- [X] T002 Create Flyway migration `back-end/src/main/resources/db/migration/V27__add_user_timezone.sql` adding `users.timezone VARCHAR(64)` (nullable) and `users.timezone_is_manual BOOLEAN NOT NULL DEFAULT false`, per [data-model.md](./data-model.md).
- [X] T003 Add `timezone` (`String`) and `timezoneIsManual` (`boolean`) fields + getters/setters to `back-end/src/main/java/com/kuky/backend/auth/model/User.java` (depends on T002).
- [X] T004 In `back-end/src/main/java/com/kuky/backend/auth/repository/UserRepository.java`: map `timezone`/`timezone_is_manual` columns in `USER_MAPPER`, and add `updateTimezone(UUID id, String zone, boolean manual)` (depends on T003).
- [X] T005 [P] Add `timezone` (nullable) and `timezoneIsManual` fields to `back-end/src/main/java/com/kuky/backend/auth/dto/UserResponse.java`. Also required fixing two pre-existing `new UserResponse(...)` call sites (`AuthService.toResponse`, `PasswordResetService`) that broke on the new record arity — not anticipated in the plan.
- [X] T006 [P] Create `back-end/src/main/java/com/kuky/backend/auth/dto/UpdateTimezoneRequest.java` — record `(String zone, boolean manual)` with `@NotBlank` on `zone`, per [contracts/timezone.md](./contracts/timezone.md).
- [X] T007 Implement `AuthService.updateTimezone(String email, UpdateTimezoneRequest request)` in `back-end/src/main/java/com/kuky/backend/auth/service/AuthService.java`: validate `ZoneId.of(request.zone())`, call `UserRepository.updateTimezone`, return updated `UserResponse` (depends on T004, T005, T006).
- [X] T008 Add an `INVALID_TIMEZONE` error mapping (invalid `ZoneId`, via new `InvalidTimezoneException`) in `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java`, per the `{"error":"...", "message":"..."}` convention (depends on T007).
- [X] T009 Add `PUT /api/v1/auth/timezone` endpoint to `back-end/src/main/java/com/kuky/backend/auth/controller/AuthController.java`, calling `authService.updateTimezone` (depends on T007, T008).
- [X] T010 [P] In `front-end/src/lib/auth.ts`: extend `UserResponse` with `timezone?: string` and `timezoneIsManual: boolean`; add `updateTimezone(zone: string, manual: boolean)` calling `PUT /api/v1/auth/timezone` (depends on T009 for contract shape; can be coded in parallel).
- [X] T011 Create `front-end/src/hooks/useTimezone.ts` (camelCase, matching this repo's existing `useLanguage.ts` hook convention rather than the plan's kebab-case): detects browser zone via `Intl.DateTimeFormat().resolvedOptions().timeZone`; on mount, reads the current user via `getMe()` and, if `timezoneIsManual` is `false`, silently calls `updateTimezone(detectedZone, false)`; exposes `{ zone, ready, isManual, isFallback, setZone(zone), clearOverride() }` (depends on T010).

**Checkpoint**: Foundational preference storage, sync endpoint, and front-end hook are ready — User Story 1 and User Story 2 work can begin.

---

## Phase 3: User Story 1 - Student sees class times in their own local time (Priority: P1) 🎯 MVP

**Goal**: Public schedule, "Mis Reservas", and the booking dialog display every time in the student's own local zone (auto-detected, manually overridable), clearly labeled by region/city name.

**Independent Test**: Per spec.md — load the public schedule and "Mis Reservas" from a browser/device set to a time zone different from the teacher's, and confirm every slot/booking reflects the viewer's local time with the zone labeled (quickstart.md Scenarios 1, 2, 6).

### Implementation for User Story 1

- [X] T012 [P] [US1] In `front-end/src/components/scheduling/ScheduleView.tsx`: replace `schedule.teacherTimezone` with a new required `timezone` prop (sourced from `useTimezone()` in the parent) when passing it down to `CalendarPicker`, `TimeSlotList`, and `BookingDialog`. Also updated the `schedule.subtitle` i18n string (all 3 locales) to interpolate the resolved zone instead of the old hardcoded "hora de Madrid" text.
- [X] T013 [P] [US1] `SlotGrid.tsx` turned out to be dead code — grep confirmed it's never imported anywhere; `ScheduleView.tsx` actually renders `CalendarPicker` + `TimeSlotList` (both already zone-aware) for the slot grid, not `SlotGrid`. Left the file untouched and flagged it via `spawn_task` for a separate dead-code-removal session (out of scope for this feature) instead of fixing timezone handling in unreachable code.
- [X] T014 [P] [US1] In `front-end/src/components/scheduling/BookingDialog.tsx`: accept a `timezone` prop and add it to the existing `Intl.DateTimeFormat("es", {...})` call (both call sites of `formatSlotDateTime`).
- [X] T015 [US1] Wired `timezone={zone}` from a single `useTimezone()` call in `front-end/src/routes/reservas.tsx` into both `ScheduleView` and `MyBookings`, replacing the old `onTimezoneResolved`/`teacherTimezone` state plumbing that fed them the teacher's zone (depends on T012, T014).
- [X] T016 [P] [US1] Create `front-end/src/components/account/TimezoneSetting.tsx`: shows the current zone and auto/manual state via a Shadcn `Select` populated from `Intl.supportedValuesOf("timeZone")`; lets the student pick a different zone calling `setZone()`, or revert to auto-detection calling `clearOverride()` (depends on T011).
- [X] T017 [US1] Add `TimezoneSetting` to the profile section of `front-end/src/routes/cuenta.tsx`, plus new `account.timezone*` i18n keys in all 3 locales (depends on T016).
- [X] T018 [US1] In `front-end/src/hooks/useTimezone.ts`: added the fallback path for when zone detection throws/is unavailable — falls back to the teacher's configured zone (fetched from the existing `/api/v1/schedule` response) and exposes an `isFallback` flag so the UI can label it as an estimate, per FR-008 (depends on T011).

**Checkpoint**: User Story 1 is fully functional and independently testable (quickstart.md Scenarios 1, 2, 6).

---

## Phase 4: User Story 2 - Booking confirmations and reminders show the correct local time (Priority: P2)

**Goal**: Confirmation screen, reminder emails, and calendar invites all agree with each other and with the student's local time; teacher-facing emails show the teacher's working zone.

**Independent Test**: Per spec.md — complete a booking as a student in a non-teacher time zone and confirm the confirmation screen, reminder email, and imported calendar event all agree (quickstart.md Scenario 3).

### Implementation for User Story 2

- [X] T019 [P] [US2] In `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java`: added `formatForStudent(Instant, String studentEmail)`, a zone-labeled formatter (`dd/MM/yyyy HH:mm (zone-id)`).
- [X] T020 [P] [US2] In the same file: added `formatForTeacher(Instant)` using `app.scheduling.teacher-timezone` (always), same label format.
- [X] T021 [US2] Replaced every `FMT.format(slotStart)` call (`sendConfirmation`, both cancellation paths, `sendReminderToStudent`, `sendReminderToTeacher`) with `formatForStudent`/`formatForTeacher` as appropriate (depends on T019, T020).
- [X] T022 [US2] Deviated from the plan here: rather than threading the student's `timezone` through `BookingService.java`'s call sites (3 methods) and `BookingReminderScheduler.java`, `BookingEmailService` now injects `UserRepository` directly and looks up the student's `timezone` by the `studentEmail` parameter every method already receives — every call site already passes a student email, so no signature changes or new plumbing were needed in `BookingService`/`BookingReminderScheduler`. Falls back to the teacher's zone if the student has no synced preference yet.
- [X] T023 [P] [US2] Confirmed (no change needed): existing `back-end/src/test/java/com/kuky/backend/scheduling/IcsEventFactoryTest.java` already asserts UTC `Z`-suffixed, unlabeled output — untouched by this feature, still passing.

**Checkpoint**: User Story 2 is fully functional and independently testable (quickstart.md Scenario 3), on top of User Story 1.

---

## Phase 5: User Story 3 - Teacher's own schedule stays anchored to her working time zone (Priority: P3)

**Goal**: Admin availability editor, bookings tab, and student profile always show the teacher's configured working zone, regardless of the admin's device.

**Independent Test**: Per spec.md — load the admin availability editor and bookings tab from a device set to a different time zone and confirm times stay in the teacher's working zone (quickstart.md Scenario 4).

### Implementation for User Story 3

- [X] T024 [US3] Create `front-end/src/hooks/useTeacherTimezone.ts` (camelCase, see T011 note): fetches `teacherTimezone` from the existing public `GET /api/v1/schedule` response, falling back to `"Europe/Madrid"` if the fetch fails.
- [X] T025 [P] [US3] In `front-end/src/components/admin/availability/WeeklyAvailabilityEditor.tsx`: replaced the browser-local `today`/horizon-anchor computation with a teacher-zone-anchored one (`todayKeyInZone` + UTC-based date arithmetic, mirroring `CalendarPicker.tsx`'s existing pattern) driven by `useTeacherTimezone()` (depends on T024).
- [X] T026 [P] [US3] In `front-end/src/components/admin/bookings/BookingsTab.tsx`: replaced the hardcoded `"Europe/Madrid"` literal with `useTeacherTimezone()` (depends on T024).
- [X] T027 [P] [US3] In `front-end/src/routes/panel_.alumnos.$studentId.tsx`: replaced the hardcoded `"Europe/Madrid"` literal with `useTeacherTimezone()` (depends on T024).

**Checkpoint**: User Story 3 is fully functional and independently testable (quickstart.md Scenario 4). All three user stories now work independently.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification and regression coverage spanning all three stories.

- [X] T028 [P] Added `back-end/src/test/java/com/kuky/backend/auth/AuthServiceTimezoneTest.java` (actual package is `auth`, not `auth.service`, matching this repo's test-package convention) covering: auto-sync overwrites `timezone` when not manual, manual override persists and is returned, invalid zone id is rejected without persisting. 3/3 passing.
- [X] T029 [P] Extended `back-end/src/test/java/com/kuky/backend/scheduling/BookingEmailServiceTest.java` (actual package is `scheduling`, not `scheduling.service`) — updated the constructor call for the new `UserRepository`/`SchedulingProperties` params, added 3 new tests asserting student-facing emails use the student's zone (falling back to the teacher's zone when absent) and teacher-facing emails always use the teacher's zone. 10/10 passing.
- [X] T030 [P] Added `slotInstantsShiftCorrectlyAcrossTheOctoberDstTransition` to the existing `back-end/src/test/java/com/kuky/backend/scheduling/AvailabilityServiceTest.java`, asserting a 09:00 Madrid slot resolves to the correct UTC instant either side of the October 2026 DST transition. 11/11 passing.
- [X] T031 Verified live in the browser preview (both servers started, Flyway migration V27 applied against the real dev DB), covering all 3 user stories: public schedule now shows the browser-detected zone (Europe/Bucharest) with a clear label instead of the teacher's Europe/Madrid; registered+activated a real test account, confirmed auto-sync persisted the detected zone to `/me`, set a manual override (`America/New_York`) via the live `PUT /timezone` endpoint, confirmed the `cuenta.tsx` UI reflected it correctly (including the "won't auto-update" note) and that the schedule's calendar grid shifted its date boundary accordingly (FR-009). Separately, registered+promoted a real ADMIN account and set a real booking to a known instant (`2026-07-10T09:00:00Z`) to verify quickstart.md Scenario 4: `BookingsTab.tsx` and the student profile page (`panel_.alumnos.$studentId.tsx`) both correctly rendered **11:00** (Madrid/CEST) rather than 12:00 (which the browser's actual Bucharest zone would have produced if unfixed) — confirming FR-007/SC-005 hold even though the admin's own device zone differs from the teacher's. All test data cleaned up afterward.
- [X] T032 `./gradlew test` — full suite green (including the pre-existing integration tests, confirming migration V27 is compatible). `npm run lint` — 0 errors (33 pre-existing, unrelated `react-hooks/exhaustive-deps` warnings remain). `npx tsc --noEmit` — clean except 2 pre-existing, unrelated errors in `__root.tsx`/`sobre-mi.tsx` (SEO meta typing, not touched by this feature). `npm run format` applied to fix formatting in touched files.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup. Blocks **User Story 1** and **User Story 2** only.
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2).
- **User Story 2 (Phase 4)**: Depends on Foundational (Phase 2). Independent of User Story 1 (different files/surfaces) but naturally verified after it since both need a booked class to exist.
- **User Story 3 (Phase 5)**: Depends only on Setup (Phase 1) — **not** on Foundational (Phase 2), since it only reads the existing `teacherTimezone` config value, unrelated to the new student-preference column. Can be implemented in parallel with Phase 2/3/4.
- **Polish (Phase 6)**: Depends on all three user stories being complete.

### Parallel Opportunities

- Phase 2: T005 and T006 in parallel with each other and with T003/T004 (different files); T010 can start once T009's contract shape is agreed even if T009 isn't merged yet.
- Phase 3: T012, T013, T014 in parallel (different files); T016 in parallel with T012–T015.
- Phase 4: T019 and T020 in parallel (same file, additive methods — coordinate); T023 fully independent.
- Phase 5: Once T024 exists, T025/T026/T027 in parallel (three different files).
- Phase 5 as a whole can run in parallel with Phases 2–4 (no shared files or data dependencies).
- Phase 6: T028, T029, T030 in parallel (different test files).

---

## Parallel Example: Phase 2 (Foundational)

```bash
# After T002 (migration) and T003 (User.java) land:
Task: "Add timezone/timezoneIsManual to UserResponse.java"
Task: "Create UpdateTimezoneRequest.java"
```

## Parallel Example: User Story 1

```bash
Task: "Fix ScheduleView.tsx to source timezone from useTimezone()"
Task: "Add timezone prop to SlotGrid.tsx"
Task: "Add timezone prop to BookingDialog.tsx"
```

## Parallel Example: User Story 3

```bash
Task: "Wire WeeklyAvailabilityEditor.tsx to useTeacherTimezone()"
Task: "Wire BookingsTab.tsx to useTeacherTimezone()"
Task: "Wire panel_.alumnos.$studentId.tsx to useTeacherTimezone()"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 (Setup) and Phase 2 (Foundational).
2. Complete Phase 3 (User Story 1) — students see correct local times on the schedule and can override their zone.
3. **STOP and VALIDATE**: run quickstart.md Scenarios 1, 2, and 6.
4. This alone fixes the highest-frequency, highest-impact gap (FR-001/FR-002/FR-003).

### Incremental Delivery

1. Setup + Foundational → shared preference storage ready.
2. User Story 1 → validate → this is the MVP.
3. User Story 2 → validate → emails/calendar invites now trustworthy too.
4. User Story 3 (can be done any time after Setup, even in parallel with 1–2) → validate → admin views hardened against the "teacher travels" edge case.
5. Polish → full regression pass.

### Parallel Team Strategy

- One developer: Phase 2 (Foundational) end-to-end (back-end preference API first, then front-end hook).
- A second developer: Phase 5 (User Story 3) in parallel, since it has no dependency on Phase 2.
- Once Phase 2 lands: split Phase 3 (User Story 1) and Phase 4 (User Story 2) across two developers.
- Regroup for Phase 6 (Polish).

---

## Notes

- [P] tasks touch different files with no ordering dependency on incomplete work.
- User Story 3 is intentionally decoupled from the Foundational phase — it can be delivered first, last, or in parallel without affecting User Story 1/2 sequencing.
- No task introduces a new dependency (library or otherwise) — confirmed in Phase 1 and reaffirmed by research.md.
- Commit after each task or logical group, per repository convention (see recent commit history: one feature-sized commit per logical unit).
