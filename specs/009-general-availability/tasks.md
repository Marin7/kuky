---
description: "Task list for General Availability Template"
---

# Tasks: General Availability Template

**Input**: Design documents from `specs/009-general-availability/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/availability-api.md](contracts/availability-api.md)

**Tests**: Back-end JUnit tasks are included because the plan and quickstart explicitly require extending `AvailabilityServiceTest` and the snapshot/migration logic is high-risk. Front-end verification is manual browser checks per the constitution (no FE test harness).

**Organization**: Tasks are grouped by user story (US1–US4 from spec.md) for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1–US4; Setup/Foundational/Polish have no story label
- File paths are repo-relative

## Path conventions

Web app: back-end at `back-end/src/main/java/com/kuky/backend/`, migrations at `back-end/src/main/resources/db/migration/`, tests at `back-end/src/test/java/com/kuky/backend/`, front-end at `front-end/src/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish a clean baseline before touching the availability subsystem.

- [ ] T001 Confirm baseline is green: `cd back-end && ./gradlew test` and `cd front-end && npm run lint` both pass before changes.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The per-week snapshot backbone. The public schedule and every user story depend on this; nothing student- or teacher-facing can change until availability resolves from `week_availability`.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T002 Create Flyway migration `V17__week_availability.sql` (tables `week_availability(id, slot_date, start_time, end_time, created_at)` with `end_time > start_time` check + `slot_date` index, and `materialized_weeks(week_start PK, created_at)`) in back-end/src/main/resources/db/migration/V17__week_availability.sql
- [ ] T003 [P] Create `DayWindow` model (slot_date + start/end `LocalTime`) in back-end/src/main/java/com/kuky/backend/scheduling/model/DayWindow.java
- [ ] T004 Add repository methods to back-end/src/main/java/com/kuky/backend/scheduling/repository/AvailabilityRepository.java: `findMaterializedWeekStarts(from,to)`, `insertMaterializedWeek(weekStart)` (idempotent `ON CONFLICT DO NOTHING`), `findDayWindowsBetween(from,to)`, `findDayWindows(date)`, `replaceDayWindows(date, windows)` (transactional delete+insert), `insertDayWindow(date,start,end)` (depends on T003)
- [ ] T005 Implement `ensureWeeksMaterialized(horizonStart, horizonEnd)` and re-point `generateSchedule()`, `validateBookable()`, `findConfirmedBookingsOutsideAvailability()`, and `availableIntervals()` to compute from `week_availability` (absolute windows, merge only — drop OPEN/BLOCK subtraction) in back-end/src/main/java/com/kuky/backend/scheduling/service/AvailabilityService.java (depends on T004)
- [ ] T006 Create `WeekAvailabilityBootstrap` (`CommandLineRunner`, mirrors `AdminBootstrap`) that, when `materialized_weeks` is empty, materializes the current horizon from today's effective availability (`rules ∪ OPEN − BLOCK`) so launch is behavior-preserving (FR-011) in back-end/src/main/java/com/kuky/backend/config/WeekAvailabilityBootstrap.java (depends on T005)
- [ ] T007 Map new error codes `INVALID_AVAILABILITY` and `DATE_OUT_OF_RANGE` in back-end/src/main/java/com/kuky/backend/.../GlobalExceptionHandler.java (locate via existing error-code mapping) and document them in specs/009-general-availability/contracts/availability-api.md if missing
- [ ] T008 Unit test: `ensureWeeksMaterialized` is idempotent and the bootstrap reproduces pre-feature effective windows for a seeded rules+exceptions fixture, in back-end/src/test/java/com/kuky/backend/scheduling/AvailabilityServiceTest.java (depends on T005, T006)

**Checkpoint**: Public `/api/v1/schedule` is unchanged for current weeks; per-week snapshots exist. User stories can now begin.

---

## Phase 3: User Story 1 - Define a general weekly availability template (Priority: P1) 🎯 MVP

**Goal**: Give the teacher a first-class editor for the default weekly pattern (which today has no UI).

**Independent Test**: In `/panel` → Availability → General, set Mon–Fri 09:00–18:00 / weekends off, save, reload — the template persists; an `end ≤ start` window is rejected.

- [ ] T009 [P] [US1] Create `GeneralAvailabilityEditor.tsx` — a Mon–Sun × hour grid bound to the weekly template — in front-end/src/components/admin/availability/GeneralAvailabilityEditor.tsx
- [ ] T010 [US1] Wire the editor to `getAvailability().weekly` (load) and `updateWeekly()` (save), converting the hour grid to/from `WeeklyWindow[]`, in front-end/src/components/admin/availability/GeneralAvailabilityEditor.tsx and front-end/src/lib/admin.ts (`updateWeekly` already exists) (depends on T009)
- [ ] T011 [US1] Add a "Disponibilidad general" section with explanatory copy (clarifies template vs. per-week — FR-012) rendering `GeneralAvailabilityEditor` in front-end/src/components/admin/availability/AvailabilityTab.tsx (depends on T009)
- [ ] T012 [P] [US1] Add i18n keys (general template title/description/save/saving/saved/errors) in front-end/src/i18n/locales/es.ts, ro.ts, en.ts
- [ ] T013 [US1] Verify quickstart Scenario B in a running browser (template persists; invalid window rejected) per specs/009-general-availability/quickstart.md

**Checkpoint**: The teacher can edit and persist the general template independently.

---

## Phase 4: User Story 2 - New weeks start from the general template (Priority: P1)

**Goal**: Surface each materialized week's snapshot to teacher and students so a new week shows the template automatically.

**Independent Test**: With a saved template, an in-range week that was never customized returns `days` matching the template, and `/api/v1/schedule` shows matching OPEN slots.

- [ ] T014 [P] [US2] Create `DayWindowDto(startTime, endTime)` and `DayAvailabilityDto(date, windows)` records in back-end/src/main/java/com/kuky/backend/admin/dto/
- [ ] T015 [US2] Change `AvailabilityResponse` to `{ weekly, days }` and update `getAvailability()` to call `ensureWeeksMaterialized` then return horizon days from `week_availability` in back-end/src/main/java/com/kuky/backend/admin/dto/AvailabilityResponse.java and back-end/src/main/java/com/kuky/backend/admin/controller/AvailabilityAdminController.java (depends on T014)
- [ ] T016 [US2] Update `AvailabilityResponse` type to `{ weekly, days }` and adjust `getAvailability` in front-end/src/lib/admin.ts (depends on T015)
- [ ] T017 [US2] Refactor the 14-day grid to populate selected cells from `days` (absolute windows) instead of computing from `weekly + exceptions` in front-end/src/components/admin/availability/WeeklyAvailabilityEditor.tsx (depends on T016)
- [ ] T018 [P] [US2] Unit test: a freshly materialized week's day windows equal the template; an empty template ⇒ fully unavailable week, in back-end/src/test/java/com/kuky/backend/scheduling/AvailabilityServiceTest.java
- [ ] T019 [US2] Verify quickstart Scenario C in a running browser (new/un-customized week shows template) per specs/009-general-availability/quickstart.md

**Checkpoint**: New weeks display the template; nothing has to be re-entered per week.

---

## Phase 5: User Story 3 - Customize a specific week without side effects (Priority: P2)

**Goal**: Make the per-week snapshot the editable source of truth via absolute per-day windows, replacing the BLOCK/OPEN exception machinery.

**Independent Test**: Edit one date (remove afternoon, add a morning window), save, reload — only that date changed; template and other weeks unchanged; `/api/v1/schedule` reflects it for that date only.

- [ ] T020 [P] [US3] Create `UpdateDayRequest(windows)` record in back-end/src/main/java/com/kuky/backend/admin/dto/UpdateDayRequest.java
- [ ] T021 [US3] Add `PUT /api/v1/admin/availability/days/{date}` (validate within horizon & not past → `DATE_OUT_OF_RANGE`; each `end > start` → else `INVALID_AVAILABILITY`; `replaceDayWindows`; return `{date, windows, conflicts}`) in back-end/src/main/java/com/kuky/backend/admin/controller/AvailabilityAdminController.java (depends on T020, T004)
- [ ] T022 [US3] Remove `POST /exceptions` and `DELETE /exceptions/{id}` endpoints and delete now-unused `ExceptionRequest`/`ExceptionResponse` DTOs in back-end/src/main/java/com/kuky/backend/admin/controller/AvailabilityAdminController.java and back-end/src/main/java/com/kuky/backend/admin/dto/ (depends on T017, T021)
- [ ] T023 [US3] Add `setDayAvailability(date, windows)` and remove `addException`/`deleteException` + `AvailabilityException` type in front-end/src/lib/admin.ts (depends on T016)
- [ ] T024 [US3] Rewrite the grid `save()` to send absolute windows per changed date via `setDayAvailability`, deleting `hoursToRanges`/OPEN-BLOCK delta logic, in front-end/src/components/admin/availability/WeeklyAvailabilityEditor.tsx (depends on T023, T017)
- [ ] T025 [US3] Lift per-day save `conflicts` into `AvailabilityTab` state so `BookingConflictNotice` renders them (FR-010, no auto-cancel) in front-end/src/components/admin/availability/AvailabilityTab.tsx (depends on T024)
- [ ] T026 [P] [US3] Add i18n keys for the per-week section title/description/save in front-end/src/i18n/locales/es.ts, ro.ts, en.ts
- [ ] T027 [P] [US3] Unit test: per-day edit persists and isolates to that date; other in-range weeks and the template are unchanged (FR-006), in back-end/src/test/java/com/kuky/backend/scheduling/AvailabilityServiceTest.java
- [ ] T028 [US3] Verify quickstart Scenarios D and F in a running browser (per-week isolation; conflict surfaced without cancelling) per specs/009-general-availability/quickstart.md

**Checkpoint**: Per-week customization works and is isolated; exception machinery is gone.

---

## Phase 6: User Story 4 - Template edits update only weeks not yet started (Priority: P3)

**Goal**: Guarantee FR-007 — editing the template never rewrites an already-materialized/customized week, but future weeks pick up the change.

**Independent Test**: Customize a week, change the template — the materialized week is unchanged; a not-yet-materialized future week reflects the new template once it enters the horizon.

- [ ] T029 [P] [US4] Unit test: `replaceWeekly` (template edit) leaves existing `week_availability` rows for already-materialized weeks unchanged (FR-007), in back-end/src/test/java/com/kuky/backend/scheduling/AvailabilityServiceTest.java
- [ ] T030 [P] [US4] Unit test: a not-yet-materialized future week, when materialized after a template change, reflects the updated template, in back-end/src/test/java/com/kuky/backend/scheduling/AvailabilityServiceTest.java
- [ ] T031 [US4] Verify quickstart Scenario E in a running browser per specs/009-general-availability/quickstart.md

**Checkpoint**: All four user stories independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T032 [P] Remove dead code: any `availability_exceptions` repository methods no longer referenced after T022 (keep the table + a deprecation comment for one release per research.md) in back-end/src/main/java/com/kuky/backend/scheduling/repository/AvailabilityRepository.java
- [ ] T033 [P] Update the `/panel` row in CLAUDE.md "Current pages" if availability editor description changed, and confirm error codes are listed in the relevant `contracts/api.md`
- [ ] T034 Run `cd back-end && ./gradlew build` and `cd front-end && npm run lint && npm run build`; fix any failures
- [ ] T035 Full quickstart pass (Scenarios A–F) in a running browser per specs/009-general-availability/quickstart.md, including Scenario A behavior-preservation against existing V7 data

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none.
- **Foundational (Phase 2)**: depends on Setup. **Blocks all user stories.**
- **US1 (Phase 3)**: depends on Foundational. Mostly front-end; uses existing `PUT /weekly`.
- **US2 (Phase 4)**: depends on Foundational. Adds the `days` payload the grid reads.
- **US3 (Phase 5)**: depends on Foundational + US2 (grid reads `days` before its save is rewritten; per-day PUT shares the controller).
- **US4 (Phase 6)**: depends on Foundational; verification leans on US3 for the customize step.
- **Polish (Phase 7)**: depends on all desired stories.

### Within stories

- Back-end DTO → controller → front-end type → component, then tests/verification.
- T021 (per-day PUT) and T022 (remove exceptions) must follow T017 so the grid never reads a removed shape mid-refactor.

### Parallel opportunities

- T003 ∥ (T002 first). T008 after T005/T006.
- US1 is largely independent of US2/US3 and can be built in parallel by a second developer once Foundational is done (touches `GeneralAvailabilityEditor`/`AvailabilityTab`, not the fortnight grid).
- All `[P]` test tasks (T018, T027, T029, T030) touch the same test file — treat `[P]` as "independent of other phases" but serialize edits to `AvailabilityServiceTest.java`.
- i18n tasks T012 and T026 are `[P]` (distinct keys) but edit the same three locale files — serialize the actual edits.

---

## Parallel Example: after Foundational

```text
Developer A (US1):  T009 → T010 → T011 → T012 → T013
Developer B (US2):  T014 → T015 → T016 → T017 → T018 → T019
# Then US3 (T020–T028) builds on US2's T017; US4 tests (T029–T031) build on US3.
```

---

## Implementation Strategy

### MVP scope

Foundational (Phase 2) + **US1 (Phase 3)** delivers the first visible value: a real general-template editor. However, the feature's promise only lands with **US2** (auto-seeding), so the recommended first shippable increment is **Foundational + US1 + US2** (both P1).

### Incremental delivery

1. Foundational → public schedule intact, snapshots persisted, launch behavior-preserving.
2. + US1 → teacher edits the default week.
3. + US2 → new weeks auto-start from it (P1 complete — ship).
4. + US3 → per-week customization replaces exceptions.
5. + US4 → template-edit isolation verified.

---

## Notes

- `[P]` = different files / no incomplete dependency; serialize edits when tasks share a file (noted above for the test and locale files).
- Verify back-end tests fail before implementing the corresponding logic where practical.
- Commit after each task or logical group.
- Constitution: every UI change verified in a running browser before marking the task done.
