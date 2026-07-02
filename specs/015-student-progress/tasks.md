---

description: "Task list template for feature implementation"
---

# Tasks: Student Progress on Teacher Profile View

**Input**: Design documents from `specs/015-student-progress/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/progress-api.md](contracts/progress-api.md), [quickstart.md](quickstart.md)

**Tests**: Included — this repo has an established backend test convention: Mockito-style service unit tests (e.g. `HomeworkAdminServiceTest`) and real-database `@SpringBootTest(...) @ActiveProfiles("local")` repository/controller integration tests (e.g. `BookingReminderRepositoryTest`, `StudentAdminControllerIntegrationTest`). New backend behavior gets matching coverage in the same style. Frontend has no test framework (per constitution) — frontend verification is manual/browser-based via [quickstart.md](quickstart.md).

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: The new response-shape building blocks every later task assembles.

- [X] T001 [P] Create `back-end/src/main/java/com/kuky/backend/admin/dto/UnitProgressDto.java` — `public record UnitProgressDto(UUID unitId, String subject, String level, int totalHomeworks, int completedHomeworks, boolean complete) {}`.
- [X] T002 [P] Create `back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkBreakdownDto.java` — `public record HomeworkBreakdownDto(int pending, int submitted, int completed) {}`.
- [X] T003 [P] Create `back-end/src/main/java/com/kuky/backend/admin/dto/StudentProgressDto.java` — `public record StudentProgressDto(List<UnitProgressDto> units, HomeworkBreakdownDto homeworkBreakdown, int attendedClasses) {}`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Wire the `progress` field end-to-end (empty, before real computation exists) and fix a pre-existing gap that would otherwise silently break FR-009.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 In `back-end/src/main/java/com/kuky/backend/admin/service/StudentProfileAdminService.java:35-37`, drop the `.filter(u -> "STUDENT".equals(u.getRole()))` from the `getProfile` user lookup (keep just `userRepository.findById(studentId).orElseThrow(...)`). **Why**: today the endpoint 404s the instant a student's role is revoked, which would silently violate FR-009 ("progress figures MUST remain accurate after a student's STUDENT role is revoked") no matter what the rest of this feature does — this is a one-line prerequisite fix, discovered while designing the feature, not a new behavior.
- [X] T005 In the same file, add a `progress` field to the object built by `getProfile`: for now, populate it with a hardcoded empty value (`new StudentProgressDto(List.of(), new HomeworkBreakdownDto(0, 0, 0), 0)`) and add the matching parameter to `back-end/src/main/java/com/kuky/backend/admin/dto/StudentProfileResponse.java`'s record. Depends on T003, T004.
- [X] T006 [P] In `front-end/src/lib/admin.ts`, add `UnitProgress`, `HomeworkBreakdown`, and `StudentProgress` interfaces (mirroring the new backend DTOs) and add `progress: StudentProgress` to the existing `StudentProfile` interface, per [contracts/progress-api.md](contracts/progress-api.md).
- [X] T007 In `front-end/src/routes/panel_.alumnos.$studentId.tsx`, add a new "Progreso" section using the page's existing `Section` component, positioned above the existing bookings section, rendering an explicit "sin actividad todavía" empty state whenever `progress.units.length === 0` and all of `homeworkBreakdown`'s counts and `attendedClasses` are `0` (FR-008). Depends on T005, T006.

**Checkpoint**: `progress` exists end-to-end and always renders (as an empty state, since the value is still hardcoded) without errors — ready for User Story 1 to fill in real computation.

---

## Phase 3: User Story 1 - Teacher sees an at-a-glance progress summary (Priority: P1) 🎯 MVP

**Goal**: The student profile shows real curriculum completion, a homework status breakdown, an attended-classes count, and the student's CEFR level — all derived from existing data, no schema change.

**Independent Test**: Open the profile of a student with 2 assigned units (one fully graded, one not), a mix of homework statuses, and several past confirmed bookings; confirm the Progreso section shows accurate unit completion, a 3-bucket homework breakdown, and the attended-classes count, matching [spec.md](spec.md) US1's acceptance scenarios.

### Tests for User Story 1

- [X] T008 [P] [US1] Create `back-end/src/test/java/com/kuky/backend/units/UnitRepositoryTest.java` (real-DB style, mirroring `back-end/src/test/java/com/kuky/backend/scheduling/BookingReminderRepositoryTest.java`'s `@SpringBootTest` + `@ActiveProfiles("local")` shape) covering `findProgressForStudent`: a unit whose homework is filed under it (`homework_assignments.unit_id`) but never targeted at the student via `homework_targets` contributes `0` to that unit's totals (doesn't inflate); a unit with 2 targeted homeworks, 1 `REVIEWED`, returns `totalHomeworks=2, completedHomeworks=1`; a unit with no targeted homeworks returns `totalHomeworks=0` and still appears in the result (not silently dropped).
- [X] T009 [P] [US1] Create `back-end/src/test/java/com/kuky/backend/admin/StudentProfileAdminServiceTest.java` (Mockito-style, mirroring `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java`'s mocked-repository shape) covering `getProfile`'s new `progress` assembly: `homeworkBreakdown` groups `PENDING`→pending, `SUBMITTED`→submitted, `REVIEWED`/`GRADED`→completed; `attendedClasses` counts only bookings with `status="CONFIRMED"` and a past `slotStart`; a student with no units/homeworks/bookings gets an all-zero `progress`.

### Implementation for User Story 1

- [X] T010 [US1] Add `findProgressForStudent(UUID studentId)` to `back-end/src/main/java/com/kuky/backend/units/repository/UnitRepository.java`, returning a new `record UnitProgressView(UUID unitId, String subject, String level, int totalHomeworks, int completedHomeworks)` via the query in [data-model.md](data-model.md) (`unit_assignments` → `units` → `homework_assignments` → `homework_targets` → `homework_submissions`, `GROUP BY` unit, ordered by `position`). Depends on T008.
- [X] T011 [US1] In `StudentProfileAdminService.java` (from T005), add three private helpers — `computeUnitProgress(UUID studentId)` (maps `unitRepository.findProgressForStudent` rows to `UnitProgressDto`, setting `complete = totalHomeworks > 0 && completedHomeworks == totalHomeworks`), `computeHomeworkBreakdown(List<StudentProfileHomeworkDto>)`, and `computeAttendedClasses(List<StudentProfileBookingDto>)` (counts `status.equals("CONFIRMED") && slotStart.isBefore(Instant.now())` — no-show not considered yet, added in US3) — and wire all three into `getProfile()`, replacing the T005 hardcoded empty `StudentProgressDto`. Depends on T009, T010.
- [X] T012 [US1] In `front-end/src/routes/panel_.alumnos.$studentId.tsx`'s Progreso section (from T007), replace the empty-only placeholder with real rendering: the unit list (subject/level + a complete/in-progress `StatusBadge`), the three homework-breakdown counts, and the attended-classes count; the T007 empty state remains for students with genuinely nothing. Depends on T011.
- [X] T013 [US1] In the same section, display the student's CEFR level by reusing the page's existing `placement` state (already fetched via `getStudentPlacementEvaluation`) — show `placement.result.overallCefr` or a "sin resultado" fallback when null (FR-005; no backend change). Depends on T012.

**Checkpoint**: User Story 1 is fully functional and independently testable — the Progreso section shows accurate curriculum, homework, attendance, and level data for any student.

---

## Phase 4: User Story 2 - Teacher spots homework needing attention (Priority: P2)

**Goal**: The homework breakdown counts (already delivered by US1) are proven correct across status transitions and given a dedicated, reusable presentation component per the constitution's Component-Driven UI principle.

**Independent Test**: Move one homework through `PENDING → SUBMITTED → REVIEWED`, reloading the profile after each step, and confirm the breakdown counts move between buckets exactly as [spec.md](spec.md) US2's acceptance scenarios describe.

### Tests for User Story 2

- [X] T014 [P] [US2] Extend `back-end/src/test/java/com/kuky/backend/admin/StudentProfileAdminServiceTest.java` (from T009) with a dedicated case for FR-003's Acceptance Scenario 2: a homework's status changing from `SUBMITTED` to `REVIEWED` between two `getProfile()` calls moves it from the `submitted` count to the `completed` count on the second call (no caching).

### Implementation for User Story 2

- [X] T015 [US2] Extract the homework-breakdown display (built inline in T012) into a new named component `front-end/src/components/admin/students/StudentHomeworkBreakdown.tsx` (props: `{ pending: number, submitted: number, completed: number }`), rendering three labeled counts, and use it from the Progreso section in `panel_.alumnos.$studentId.tsx`. Depends on T012. (Note: built directly into T012's rendering pass — `admin/students/` matches the existing convention for student-profile-specific components better than the flat `admin/` path originally sketched.)

**Checkpoint**: The homework breakdown is verified correct across every status transition and rendered via a dedicated, reusable component — both stories are independently demonstrable.

---

## Phase 5: User Story 3 - Teacher reviews curriculum and attendance progress (Priority: P3)

**Goal**: Attendance becomes correctable — the teacher can mark (and unmark) a past confirmed class as a no-show, and the attended-classes count reflects it.

**Independent Test**: On a student with several past confirmed bookings, mark one as a no-show and confirm `attendedClasses` decreases by one and the booking shows the flag; revert it and confirm the count goes back up; attempt to mark a future or cancelled booking and confirm it's rejected — per [spec.md](spec.md) US3's acceptance scenarios 3-4 and FR-012.

### Tests for User Story 3

- [X] T016 [P] [US3] Create `back-end/src/test/java/com/kuky/backend/scheduling/BookingServiceTest.java` (Mockito-style, mirroring `HomeworkAdminServiceTest`'s mocked-repository shape) covering the new `setNoShow`: succeeds for a past `CONFIRMED` booking; throws `BookingNotFoundException` for an unknown id; throws `BookingNotAllowedException` with `Reason.NOT_ELIGIBLE_FOR_NO_SHOW` for a future booking and for a `CANCELLED` one (FR-012), for both `noShow=true` and `noShow=false`.
- [X] T017 [P] [US3] Create `back-end/src/test/java/com/kuky/backend/admin/BookingAdminControllerIntegrationTest.java` (real-DB style, mirroring `StudentAdminControllerIntegrationTest`'s `@ActiveProfiles("local")` shape) covering `PUT /api/v1/admin/bookings/{id}/no-show`: `204` and the flag flips for a past confirmed booking; `422 BOOKING_NOT_ELIGIBLE_FOR_NO_SHOW` for an ineligible booking; `403` for a non-admin caller.

### Implementation for User Story 3

- [X] T018 [US3] Write `back-end/src/main/resources/db/migration/V26__add_booking_no_show.sql`: `ALTER TABLE bookings ADD COLUMN no_show BOOLEAN NOT NULL DEFAULT FALSE;`.
- [X] T019 [P] [US3] Add `private boolean noShow;` + getter/setter to `back-end/src/main/java/com/kuky/backend/scheduling/model/Booking.java`, and map the `no_show` column (`b.setNoShow(rs.getBoolean("no_show"));`) in `BookingRepository.BOOKING_MAPPER` (`back-end/src/main/java/com/kuky/backend/scheduling/repository/BookingRepository.java`). Depends on T018.
- [X] T020 [P] [US3] Add `setNoShow(UUID id, boolean noShow)` to `BookingRepository.java` (`UPDATE bookings SET no_show = :noShow WHERE id = :id`), mirroring the existing `markCancelled` method's shape. Depends on T018.
- [X] T021 [P] [US3] Add `NOT_ELIGIBLE_FOR_NO_SHOW` to the `Reason` enum in `back-end/src/main/java/com/kuky/backend/scheduling/exception/BookingNotAllowedException.java`.
- [X] T022 [US3] Add `setNoShow(UUID bookingId, boolean noShow)` to `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingService.java`, mirroring `cancelBookingAsAdmin`'s shape (`:147-166`): look up the booking (`BookingNotFoundException` if missing), validate `"CONFIRMED".equals(booking.getStatus()) && booking.getSlotStart().isBefore(Instant.now())` else throw `BookingNotAllowedException(Reason.NOT_ELIGIBLE_FOR_NO_SHOW)`, then call `bookingRepository.setNoShow(bookingId, noShow)`. Depends on T016, T019, T020, T021.
- [X] T023 [P] [US3] In `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java`'s existing `handleBookingNotAllowed` switch, add a `NOT_ELIGIBLE_FOR_NO_SHOW` case mapping to `422 UNPROCESSABLE_ENTITY` / `{"error":"BOOKING_NOT_ELIGIBLE_FOR_NO_SHOW", ...}`.
- [X] T024 [P] [US3] Create `back-end/src/main/java/com/kuky/backend/admin/dto/SetNoShowRequest.java` — `public record SetNoShowRequest(boolean noShow) {}`.
- [X] T025 [US3] Add `PUT /{id}/no-show` to `back-end/src/main/java/com/kuky/backend/admin/controller/BookingAdminController.java`, accepting `@RequestBody SetNoShowRequest`, calling `bookingService.setNoShow(id, request.noShow())`, returning `204 No Content` (mirroring the existing `cancelBooking` method's `ResponseEntity<Void>` shape), per [contracts/progress-api.md](contracts/progress-api.md). Depends on T017, T022, T023, T024.
- [X] T026 [US3] Add `noShow: boolean` to `back-end/src/main/java/com/kuky/backend/admin/dto/StudentProfileBookingDto.java`, populate it in `StudentProfileAdminService.toBookingDto`, and update `computeAttendedClasses` (from T011) to additionally require `!booking.noShow()`. Depends on T019, T011.
- [X] T027 [P] [US3] In `front-end/src/lib/admin.ts`, add `noShow: boolean` to the `StudentProfileBooking` interface and a new `setBookingNoShow(bookingId: string, noShow: boolean): Promise<void>` (`PUT /admin/bookings/${bookingId}/no-show`), same `apiCall<T>` wrapper shape as `cancelBooking`.
- [X] T028 [US3] In `panel_.alumnos.$studentId.tsx`'s existing past-bookings list, add a no-show toggle control on each row (enabled only for `status === "CONFIRMED"` and a past `slotStart`), calling `setBookingNoShow` and re-fetching the profile on success. Depends on T026, T027.

**Note**: The endpoint was switched from `PATCH` to `PUT` during implementation — `CorsConfig`'s `allowedMethods` didn't include `PATCH` (only `GET/POST/PUT/DELETE/OPTIONS`), and no endpoint anywhere else in the codebase uses `PATCH` either. `PUT` matches the existing convention for single-field updates (`PUT /presentations/{id}/level`, `PUT /admin/testimonials/{id}`), so the controller/frontend/tests/docs were all updated to `PUT` rather than widening the shared CORS config for a one-off method.

**Checkpoint**: All three user stories work together — the teacher sees an accurate, correctable attendance count alongside curriculum and homework progress, satisfying the full requirement set (FR-001 through FR-012).

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification and documentation once all three stories are complete.

- [X] T029 [P] Run `cd back-end && ./gradlew test && ./gradlew build` — confirm T008, T009, T014, T016, T017 and the full existing suite pass.
- [X] T030 [P] Run `cd front-end && npm run lint && npm run format` — confirm no lint/format regressions from T006, T007, T012, T013, T015, T027, T028.
- [X] T031 Execute [quickstart.md](quickstart.md) Scenarios A-G manually against the running dev servers, per the constitution's browser-verification requirement.
- [X] T032 [P] Update the `/panel` row's description in the "Current pages" table in `CLAUDE.md` to mention the student-progress summary and no-show marking, mirroring the terse style of that table.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T001-T003) — BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational only. Independently testable/shippable as the MVP.
- **User Story 2 (Phase 4)**: Depends on Foundational; its test (T014) and component-extraction task (T015) both build on US1's already-delivered breakdown logic/rendering (T011, T012) — functionally an overlap-and-verify phase, not a blocker for US3.
- **User Story 3 (Phase 5)**: Depends on Foundational; its `computeAttendedClasses` update (T026) edits code US1 wrote (T011), so it must follow Phase 3, but has no dependency on Phase 4.
- **Polish (Phase 6)**: Depends on Phases 3, 4, and 5 all being complete.

### Within Each User Story

- Tests are listed before implementation tasks (written/skimmed first per the repo's convention) but are not strict red-green TDD gates.
- Repository/DTO tasks before the service task that composes them; service before the controller/endpoint task.
- Backend before the frontend tasks that call it.

### Parallel Opportunities

- Setup: T001, T002, T003 [P] (different files).
- Foundational: T006 [P] (frontend lib) can run alongside T004-T005 (backend); T007 waits for both.
- Within US1: T008, T009 [P] (different test files, different repos/mocks) — T010, T011 wait on their respective tests.
- Within US2: T014 is solo (extends an existing file); T015 is solo (new component, but depends on T012).
- Within US3: T016, T017 [P] (tests); T019, T020, T021 [P] (model/repo/exception, different files, all depend only on T018) — T022 waits for all three; T023, T024 [P] (exception handler + new DTO) alongside T022; T027 [P] (frontend lib) can run alongside T018-T026 (backend).
- Phase 6: T029, T030, T032 [P] (independent verification/doc tasks); T031 is manual and best run last.

---

## Parallel Example: User Story 3

```bash
# Tests, alongside implementation start:
Task: "BookingServiceTest — setNoShow eligibility rules"
Task: "BookingAdminControllerIntegrationTest — PUT /admin/bookings/{id}/no-show"

# Implementation, together (after the migration, before the service task):
Task: "Booking.java + BookingRepository.BOOKING_MAPPER — map no_show column"
Task: "BookingRepository.setNoShow(id, noShow)"
Task: "BookingNotAllowedException.Reason.NOT_ELIGIBLE_FOR_NO_SHOW"

# Frontend, alongside backend work:
Task: "admin.ts — StudentProfileBooking.noShow + setBookingNoShow()"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Complete Phase 1: Setup (T001-T003).
2. Complete Phase 2: Foundational (T004-T007) — critical, blocks everything, and fixes the revoked-student lookup gap.
3. Complete Phase 3: User Story 1 (T008-T013).
4. **STOP and VALIDATE**: run T008-T009, then manually check a seeded student's profile shows accurate curriculum/homework/attendance/level data.
5. Deploy/demo if ready — attendance in this MVP slice is simply "all past confirmed bookings" (identical to the post-US3 formula while `no_show` stays at its default `FALSE`), so US1 alone is already a complete, correct increment.

### Incremental Delivery

1. Setup + Foundational → `progress` field exists, always renders (empty state).
2. Add User Story 1 → real curriculum/homework/attendance/level data renders → MVP.
3. Add User Story 2 → homework-breakdown correctness is explicitly verified across transitions, and gets a reusable component.
4. Add User Story 3 → the teacher can correct attendance (mark/unmark no-shows), closing the full requirement set.
5. Polish → automated + manual verification, docs.

---

## Notes

- [P] tasks touch different files and have no unmet dependency within their phase.
- This feature extends three existing domains (`scheduling`, `units`, `admin`) rather than introducing a new one — no new top-level backend package.
- User Story 2 substantially overlaps with what User Story 1 already delivers (both are anchored on the same `homeworkBreakdown` field); its phase is intentionally small — dedicated transition-correctness test coverage plus a component-extraction refactor — rather than duplicating US1's work. This mirrors the spec's own framing of US1 as the comprehensive "at-a-glance" MVP.
- User Story 3 is the one genuinely new write capability in this feature (no-show marking) and is the only phase touching the database schema (T018).
- T004 (drop the `STUDENT`-role filter in `getProfile`) is a small, explicitly-scoped fix required by FR-009, discovered while designing this feature — it is not a general-purpose refactor of student-lookup logic elsewhere in the codebase.
- `StudentProfileAdminService.java` is edited by T004, T005, T011, and T026 — each depends on the previous, so none of those four are marked `[P]` despite all being "small" changes.
- Commit after each task or logical group; stop at any checkpoint to validate a story independently before continuing.
