---
description: "Task list for student-based homework deadlines"
---

# Tasks: Student-Based Homework Deadlines

**Input**: Design documents from `specs/044-student-homework-deadlines/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/student-homework-deadlines-api.md](./contracts/student-homework-deadlines-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit for assign-time date (new rows only), per-row update/clear, per-student overdue, and due-date writes must not bump `content_revised_at` (per [plan.md](./plan.md)). Frontend — browser verification via [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Client types for per-student dates. No new packages.

- [x] T001 Drop `dueOn` from `HomeworkAdminItem`; add `dueOn: string | null` on `Assignee`; pass optional `dueOn` on `createHomework` (initial assignees) and `setAssignees` (newly added only) in `front-end/src/lib/admin.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Move `due_on` onto `homework_targets`, drop it from `homework_assignments`, and keep the app compiling with student reads from the target row. Blocks all user stories.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 Create Flyway `back-end/src/main/resources/db/migration/V24__homework_target_due_on.sql`: `ADD COLUMN homework_targets.due_on DATE`; `DROP COLUMN homework_assignments.due_on` per [data-model.md](./data-model.md) (use the next free version if V24 is taken)
- [x] T003 [P] Remove `dueOn` from `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkAssignment.java`
- [x] T004 Stop mapping `due_on` on assignment SELECT/INSERT/UPDATE in `back-end/src/main/java/com/kuky/backend/learning/repository/ContentRepository.java` (depends on T002, T003)
- [x] T005 [P] Persist and read `due_on` on `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkTargetRepository.java`: write `due_on` only on INSERT of new targets (`ON CONFLICT DO NOTHING` must not overwrite existing dates); add `dueOn` to `AssigneeView` and `StudentAssignmentView` per [research.md](./research.md) §2
- [x] T006 [P] Remove `ha.due_on` from `findHomeworks` in `back-end/src/main/java/com/kuky/backend/units/repository/UnitRepository.java`
- [x] T007 [P] Remove `due_on` from homework INSERTs in `back-end/src/main/resources/db/dev/full_seed.sql`
- [x] T008 [P] Add shared overdue helper (due date present, before teacher-zone today, status `PENDING`) in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkDueDates.java` per [research.md](./research.md) §4
- [x] T009 Pass the calling student’s target `dueOn` into `HomeworkItems.toResponse` (stop using assignment `getDueOn`) in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkItems.java`, `LearningService.java`, and `HomeworkSubmissionService.java`; load `t.due_on` for that user from `ContentRepository` / `HomeworkTargetRepository` (depends on T003, T005, T008)
- [x] T010 [P] Drop `dueOn` from `HomeworkAdminItem` and `UpdateHomeworkRequest`; keep `dueOn` on `CreateHomeworkRequest` as **initial-assignee** date; add optional `dueOn` to `SetAssigneesRequest`; add `dueOn` to `AssigneeDto` in `back-end/src/main/java/com/kuky/backend/admin/dto/` per [contracts/student-homework-deadlines-api.md](./contracts/student-homework-deadlines-api.md)
- [x] T011 Create/update/list mapping without homework `dueOn`; `create` and `setAssignees` apply body `dueOn` only to newly inserted targets in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` and `HomeworkAdminController.java` (`PUT .../assignees` passes `request.dueOn()`) (depends on T004, T005, T010)
- [x] T012 Update `withAssignees` / `HomeworkAdminItem` construction (no assignment `dueOn`) in `back-end/src/main/java/com/kuky/backend/units/service/UnitService.java` (depends on T006, T010)

**Checkpoint**: App compiles against V24. Admin homework JSON has no overall `dueOn`. Assignees expose `dueOn`. `PUT .../assignees` with `dueOn` stamps **new** rows only. Student learning `dueOn` comes from the target (null until US1 writes). Unit assign still inserts targets with null `due_on`.

---

## Phase 3: User Story 1 - Teacher sets a due date for a specific student (Priority: P1) — MVP

**Goal**: Teacher assigns students with an optional date for those newly added; homework content has no overall due-date field; assignee rows show each student’s date.

**Independent Test**: Assign A with a date and B with none; A’s row shows the date, B’s does not; editor content has no shared due date ([quickstart.md](./quickstart.md) Scenario 1 steps 1–2).

### Implementation for User Story 1

- [x] T013 [P] [US1] Add assign-time due-date copy (label next to assignees, optional hint) in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`
- [x] T014 [US1] Remove the content due-date field from `front-end/src/components/admin/homework/HomeworkEditorPage.tsx`; add optional date next to `StudentMultiSelect`; pass it to `createHomework` / `setAssignees` only (not `updateHomework`) (FR-004, FR-016)
- [x] T015 [P] [US1] Show each assignee’s `dueOn` (no overall date) on `front-end/src/components/admin/homework/HomeworkAssigneeList.tsx` (FR-012 display; edit comes in US3)
- [x] T016 [US1] Tests: create with `assigneeIds` + `dueOn` stamps those targets; later `PUT .../assignees` with a new student + different `dueOn` does not change existing rows; empty assignees ignores `dueOn` in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` and/or `HomeworkAdminControllerIntegrationTest.java` (FR-004, FR-005)

**Checkpoint**: Teacher can give new assignees a date without a homework-wide field. FR-001, FR-004, FR-016.

---

## Phase 4: User Story 2 - Student sees only their own deadline (Priority: P1)

**Goal**: Each student sees only their target date; overdue is derived from that date and pending status; submit after due still works.

**Independent Test**: A past+pending overdue; B future not overdue; D no date not overdue; A can still submit ([quickstart.md](./quickstart.md) Scenario 2).

### Implementation for User Story 2

- [x] T017 [US2] Tests: two students on the same homework with different `homework_targets.due_on` get isolated `dueOn`/`overdue` in `back-end/src/test/java/com/kuky/backend/learning/LearningServiceTest.java` (FR-007, FR-008)
- [x] T018 [US2] Tests: overdue uses the student’s target date; past due + pending is overdue; submitted is not; late submit still accepted in `back-end/src/test/java/com/kuky/backend/learning/HomeworkSubmissionServiceTest.java` (FR-008, FR-009)
- [x] T019 [P] [US2] Confirm student UI still binds `item.dueOn` / `item.overdue` (no homework-level date) in `front-end/src/components/learning/HomeworkItemCard.tsx` and `front-end/src/components/learning/UnitDetailContent.tsx`

**Checkpoint**: Student payloads are per-student. FR-007, FR-008, FR-009.

---

## Phase 5: User Story 3 - Teacher changes one student’s deadline (Priority: P2)

**Goal**: Change or clear one assignee’s date from the homework assignee list without affecting others, submissions, or freeze.

**Independent Test**: Change only A’s date (including clear); B unchanged; in-progress take is not reset ([quickstart.md](./quickstart.md) Scenarios 3–4 freeze).

### Implementation for User Story 3

- [x] T020 [US3] Add `UpdateAssigneeDueOnRequest` and `PUT /api/v1/admin/homework/{id}/assignees/{userId}/due-on` in `back-end/src/main/java/com/kuky/backend/admin/dto/UpdateAssigneeDueOnRequest.java`, `HomeworkAdminController.java`, `HomeworkAdminService.java`, and `HomeworkTargetRepository.java` (update that row only; 404 if not assigned; do not bump `content_revised_at`) per [contracts/student-homework-deadlines-api.md](./contracts/student-homework-deadlines-api.md)
- [x] T021 [P] [US3] Add `updateAssigneeDueOn` in `front-end/src/lib/admin.ts`
- [x] T022 [US3] Per-row date control (set/change/clear) on `front-end/src/components/admin/homework/HomeworkAssigneeList.tsx` calling `updateAssigneeDueOn` (FR-003, FR-014)
- [x] T023 [US3] Rewrite due-date-only freeze coverage: per-row due-on (and assignees `dueOn` for new students) must not bump `content_revised_at` or trigger `HOMEWORK_UPDATED` in `back-end/src/test/java/com/kuky/backend/learning/HomeworkFreezeSubmittedIntegrationTest.java`; drop content-PUT `dueOn` from `HomeworkUpdatePreservesSubmissionsIntegrationTest.java` (FR-011)
- [x] T024 [US3] Tests: update/clear one student’s date; other assignees unchanged; unknown assignee 404 in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminControllerIntegrationTest.java` (FR-003, FR-010)

**Checkpoint**: Extensions are per student. FR-003, FR-010, FR-011, FR-014.

---

## Phase 6: User Story 4 - Teacher sees each student’s deadline in admin views (Priority: P2)

**Goal**: Assignee rows and student profile Tareas show that student’s date and overdue; homework list cards have no overall date; profile is view-only.

**Independent Test**: Two assignees, one overdue; profile matches; card has no shared date; profile cannot edit ([quickstart.md](./quickstart.md) Scenario 4 steps 1–2).

### Implementation for User Story 4

- [x] T025 [US4] Add derived `overdue` on `AssigneeDto`; add `dueOn` + `overdue` on `StudentProfileHomeworkDto` and map them in `HomeworkAdminService.java`, `StudentProfileAdminService.java`, and `HomeworkTargetRepository.StudentAssignmentView` using `HomeworkDueDates` in `back-end/src/main/java/com/kuky/backend/` (FR-012, FR-013)
- [x] T026 [P] [US4] Remove overall due date from `front-end/src/components/admin/homework/HomeworkAdminCard.tsx` (FR-015)
- [x] T027 [US4] Show view-only `dueOn` / overdue on Tareas rows in `front-end/src/routes/panel_.alumnos.$studentId.tsx`; extend `StudentProfileHomework` in `front-end/src/lib/admin.ts`; no date editor on the profile (FR-013, FR-014)
- [x] T028 [P] [US4] Confirm unit assign has no due-date field in `front-end/src/components/admin/units/UnitAssignDialog.tsx` and `back-end/src/main/java/com/kuky/backend/units/service/UnitService.java` (`addTargets` stays null `due_on`) (FR-005a)

**Checkpoint**: Teacher can see per-student dates everywhere she reviews assignment/progress. FR-012–015, FR-005a.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Browser validation, i18n, lint/tests.

- [ ] T029 Run [quickstart.md](./quickstart.md) Scenarios 1–5 in the browser (`:8080` / `:8081`)
- [ ] T030 [P] `npm run lint` in `front-end/` and `./gradlew test --tests '*Homework*'` in `back-end/`
- [x] T031 [P] Assignee-row and profile overdue/due-date strings in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts` (reuse `learning.homework.overdue` / `dueOn` where it already fits)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup types for later UI; **BLOCKS** all user stories. T004 after T002+T003; T009 after T003/T005/T008; T011 after T004/T005/T010; T012 after T006+T010
- **US1 (Phase 3)**: After Foundational — T014 after T001+T013+T011; T015 after T001
- **US2 (Phase 4)**: After Foundational (student read path T009); tests can insert `homework_targets.due_on` directly
- **US3 (Phase 5)**: After US1 assignee list exists (T015); T022 after T020+T021
- **US4 (Phase 6)**: After Foundational; T027 after T025; card T026 independent of profile
- **Polish (Phase 7)**: After desired stories complete

### User Story Dependencies

| Story | Depends on | Independently testable? |
|-------|------------|-------------------------|
| US1 Assign-time date + no overall field | Phase 2 | Yes — create/assign API + editor |
| US2 Student isolation / overdue | Phase 2 (T009) | Yes — tests can seed target `due_on` without US1 UI |
| US3 Per-row change/clear | US1 list display | Yes — PUT due-on + one row |
| US4 Admin visibility | Phase 2; overdue helper | Yes — profile/card even if row edit waits |

### Within Each User Story

- Schema/repository before service before controller
- API client types before editor
- Per-row endpoint before assignee-list editor
- Story complete before moving to the next priority when sharing `HomeworkAssigneeList.tsx` / `HomeworkEditorPage.tsx`

### Parallel Opportunities

- T003, T005, T006, T007, T008 after T002
- T013 i18n while T016 tests are written
- T015 list display vs T014 editor
- T021 admin.ts vs T020 backend
- T026 card vs T028 unit-assign sanity
- T030 and T031 during/after T029

---

## Parallel Example: Foundational

```bash
# After T002 migration exists:
Task: "Remove dueOn from HomeworkAssignment.java"
Task: "homework_targets due_on in HomeworkTargetRepository.java"
Task: "Drop ha.due_on from UnitRepository.findHomeworks"
Task: "HomeworkDueDates.java helper"
Task: "Admin DTO dueOn moves (Item/Update/Create/SetAssignees/AssigneeDto)"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: assign two students with mixed dates; no overall field
5. US2 should follow in the same increment so students see those dates

### Incremental Delivery

1. Setup + Foundational → column moved; student JSON reads target date
2. US1 → teacher can set dates when assigning (MVP)
3. US2 → students see the right date / overdue
4. US3 → extensions per row
5. US4 → profile + cards + overdue on admin
6. Each story keeps previous behaviour

### Parallel Team Strategy

With two people after Phase 2: A does US1 editor + assign API tests; B does US2 learning tests (different files). US3/US4 both touch `HomeworkAssigneeList.tsx` — serialize those.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to US1–US4
- No overall+override hybrid; no unit-assign date; no profile editor; no bulk overwrite of existing dates
- Commit after each task or logical group
- Stop at any checkpoint to validate the story independently
