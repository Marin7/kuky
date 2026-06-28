---
description: "Task list for Class Units (Packages)"
---

# Tasks: Class Units (Packages)

**Input**: Design documents from `/specs/010-class-units/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api.md](contracts/api.md)

**Tests**: Not requested in the spec. Per the project constitution, UI changes are
verified in a running browser ([quickstart.md](quickstart.md)) and back-end logic via
`./gradlew test`. No dedicated TDD phase is generated; an optional back-end test task
for the access UNION is included in Polish.

**Organization**: Tasks are grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1 / US2 / US3
- Exact file paths are included in each task.

## Path Conventions

Web app: back-end at `back-end/src/main/java/com/kuky/backend/`, front-end at `front-end/src/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the new package/folder skeletons the feature will fill in.

- [x] T001 Create the back-end units bounded-context package folders `controller/`, `service/`, `repository/`, `model/`, `dto/`, `exception/` under `back-end/src/main/java/com/kuky/backend/units/`
- [x] T002 [P] Create the front-end folder `front-end/src/components/admin/units/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, core unit entity/CRUD, and error wiring that ALL user stories depend on.

**âš ï¸ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T003 Create Flyway migration `back-end/src/main/resources/db/migration/V18__create_units.sql`: table `units` (id, level VARCHAR(5) NOT NULL with CEFR CHECK, subject VARCHAR(200) NOT NULL, position INT NOT NULL DEFAULT 0, created_at, updated_at) + index `units_level_position_idx (level, position)`; table `unit_assignments` (id, unit_id FKâ†’units ON DELETE CASCADE, user_id FKâ†’users ON DELETE CASCADE, created_at, UNIQUE(unit_id,user_id)) + index `unit_assignments_user_idx (user_id)`; `ALTER TABLE presentations ADD COLUMN unit_id UUID REFERENCES units(id) ON DELETE SET NULL` + index `presentations_unit_idx`; `ALTER TABLE homework_assignments ADD COLUMN unit_id UUID REFERENCES units(id) ON DELETE SET NULL` + index `homework_assignments_unit_idx` (see [data-model.md](data-model.md))
- [x] T004 [P] Create `Unit` model in `back-end/src/main/java/com/kuky/backend/units/model/Unit.java` (id, level, subject, position, createdAt, updatedAt)
- [x] T005 [P] Create `UnitNotFoundException` in `back-end/src/main/java/com/kuky/backend/units/exception/UnitNotFoundException.java`
- [x] T006 Register error codes `UNIT_NOT_FOUND` (404) and `INVALID_LEVEL` (400) in `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java`
- [x] T007 [P] Create unit DTOs in `back-end/src/main/java/com/kuky/backend/units/dto/`: `UnitSummary` (id, level, subject, position, presentationCount, homeworkCount, assignedStudentIds), `UnitDetail` (adds presentations, homeworks, assignedStudents), `CreateUnitRequest` (level, subject), `UpdateUnitRequest` (level, subject), `ReorderUnitsRequest` (level, orderedIds), `SetUnitPresentationsRequest` (presentationIds), `SetUnitHomeworksRequest` (homeworkIds), `SetUnitAssigneesRequest` (studentIds)
- [x] T008 Create `UnitRepository` in `back-end/src/main/java/com/kuky/backend/units/repository/UnitRepository.java` with core CRUD + queries: listSummaries (ordered by level, position; counts + assignedStudentIds), create (auto position = max+1 within level), findById, updateLevelSubject, delete, reorder (rewrite positions for a level), nextPosition (depends on T003, T004)
- [x] T009 [P] Add the front-end CEFR-level constant reuse + `Unit`, `UnitSummary`, `UnitDetail` types to `front-end/src/lib/admin.ts` (no functions yet)

**Checkpoint**: Schema migrated; units can be created/listed/edited/deleted/reordered at the repository level; error codes wired.

---

## Phase 3: User Story 1 - Teacher organizes content into Units (Priority: P1) ðŸŽ¯ MVP

**Goal**: A single **Units** tab where the teacher creates/edits/deletes/reorders units (level + subject + position) and attaches/detaches presentations and homeworks. Replaces the Homework and Presentations tabs.

**Independent Test**: Create an A1Â·Family unit, add a `.pptx` presentation and one homework, move the presentation to another unit, reorder units within A1 â€” all persisting across reload, with no separate Homework/Presentations tabs visible.

### Implementation for User Story 1

- [x] T010 [US1] Add unit-membership repository methods: in `back-end/src/main/java/com/kuky/backend/units/repository/UnitRepository.java` add setPresentations(unitId, ids) and setHomeworks(unitId, ids) that set `unit_id` on the listed rows and clear it on previously-attached rows omitted (single-membership move per FR-005a); add loaders for a unit's presentations/homeworks/assignees for `UnitDetail`
- [x] T011 [US1] Implement `UnitService` in `back-end/src/main/java/com/kuky/backend/units/service/UnitService.java`: list, create (validate level against CEFR set, non-blank subject), get (â†’UnitDetail), update, delete, reorder (validate orderedIds == level's id set), setPresentations, setHomeworks; throw `UnitNotFoundException`/`IllegalArgumentException(INVALID_LEVEL)` (depends on T008, T010)
- [x] T012 [US1] Implement `UnitAdminController` in `back-end/src/main/java/com/kuky/backend/units/controller/UnitAdminController.java` mapping `/api/v1/admin/units`: GET list, POST create, GET {id}, PUT {id}, DELETE {id}, PUT /reorder, PUT {id}/presentations, PUT {id}/homeworks (per [contracts/api.md](contracts/api.md)) (depends on T011)
- [x] T013 [US1] Ensure presentation/homework creation can set an owning unit (FR-005b): add optional `unitId` handling so items authored from a unit are attached on create â€” via `presentations.unit_id` and `homework_assignments.unit_id` set by the unit content endpoints (no new endpoint needed; verify `PresentationRepository`/homework admin paths leave `unit_id` intact)
- [x] T014 [P] [US1] Add unit API functions to `front-end/src/lib/admin.ts`: listUnits, createUnit, getUnit, updateUnit, deleteUnit, reorderUnits, setUnitPresentations, setUnitHomeworks (all `credentials:'include'`)
- [x] T015 [US1] Replace the `homework` and `presentations` tabs with a single `units` tab in `front-end/src/components/admin/AdminPanel.tsx` (update `VALID_TABS`, `TabsList`, `TabsContent` â†’ `<UnitsTab />`)
- [x] T016 [P] [US1] Create `UnitsTab` in `front-end/src/components/admin/units/UnitsTab.tsx`: load units, group by level ordered by position, "new unit" form (level select + subject input), render `UnitCard` list (FR-001/FR-002/FR-007)
- [x] T017 [US1] Create `UnitCard` in `front-end/src/components/admin/units/UnitCard.tsx`: show level badge + subject + position, edit level/subject, delete, reorder controls within level, expand to show contents via `UnitContentPicker` (depends on T014)
- [x] T018 [US1] Create `UnitContentPicker` in `front-end/src/components/admin/units/UnitContentPicker.tsx`: list the unit's presentations (with existing upload/replace/level controls reused from the old presentation card) and homeworks (link to the existing `/panel/tareas/...` editor), plus attach/detach pickers calling setUnitPresentations/setUnitHomeworks (FR-004/FR-005/FR-006) (depends on T014)
- [x] T019 [P] [US1] Add i18n keys `admin.tabs.units` and `admin.units.*` (title, newUnit, subjectPlaceholder, level, position, addPresentation, addHomework, empty, deleteConfirm, etc.) to `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`

**Checkpoint**: The Units tab fully manages packages and their contents; Homework/Presentations tabs are gone. US1 is independently demoable.

---

## Phase 4: User Story 2 - Teacher assigns a Unit to a student (Priority: P2)

**Goal**: Assigning a unit grants the student all its presentations (dynamically), never its homeworks; the student sees them grouped by unit in `/aprendizaje`.

**Independent Test**: Assign a unit with two presentations to a student; the student sees both (no homeworks); add a third presentation and it appears without re-assignment; unassign and access is revoked.

### Implementation for User Story 2

- [x] T020 [US2] Add assignment methods to `back-end/src/main/java/com/kuky/backend/units/repository/UnitRepository.java`: replaceAssignees(unitId, studentIds) (diff against `unit_assignments`), findAssignedStudents(unitId), validate STUDENT role (depends on T008)
- [x] T021 [US2] Add setAssignees to `UnitService` (validate each id is a STUDENT, reuse the `StudentNotFoundException` pattern) and a `PUT /api/v1/admin/units/{id}/assignees` endpoint in `UnitAdminController` returning `UnitDetail` (FR-008) (depends on T011, T012, T020)
- [x] T022 [US2] Rewrite presentation access to the UNION of legacy shares + unit assignments in `back-end/src/main/java/com/kuky/backend/presentations/repository/PresentationRepository.java`: update `isSharedWith` and `findSharedSummariesForUser` (the latter also returns the owning unit's level/subject/position for grouping) per [data-model.md](data-model.md) (FR-009/FR-010/FR-011/FR-017)
- [x] T023 [US2] Extend `SharedPresentationSummary` in `back-end/src/main/java/com/kuky/backend/learning/dto/SharedPresentationSummary.java` with an optional `unit` (level, subject, position) and map it in `back-end/src/main/java/com/kuky/backend/learning/service/LearningService.java#getOverview` (FR-015) (depends on T022)
- [x] T024 [P] [US2] Add `setUnitAssignees` to `front-end/src/lib/admin.ts` and the `unit` field on the learning `SharedPresentationSummary` type in `front-end/src/lib/learning.ts`
- [x] T025 [US2] Create `UnitAssignDialog` in `front-end/src/components/admin/units/UnitAssignDialog.tsx` (student multi-select, save via setUnitAssignees) and wire an "Assign" action into `UnitCard` (FR-008) (depends on T017, T024)
- [x] T026 [US2] Group presentations by unit (level Â· subject, ordered by position) with an "Other" group for unattached/legacy shares in `front-end/src/components/learning/SharedPresentationsList.tsx` (FR-015) (depends on T024)

**Checkpoint**: Unit assignment grants/revokes presentation access dynamically; students see them grouped by unit; no homeworks leak. US1 + US2 both work.

---

## Phase 5: User Story 3 - Teacher assigns homeworks individually (Priority: P3)

**Goal**: Homeworks in an assigned unit stay locked until the teacher assigns each one per student; the teacher manages this from within the unit.

**Independent Test**: With a unit assigned (presentations visible), confirm no homeworks appear for the student; assign one homework from the unit and confirm only it becomes available; the unit's other homeworks stay locked.

### Implementation for User Story 3

- [x] T027 [US3] Verify homework access is unaffected by unit membership: confirm `back-end/src/main/java/com/kuky/backend/learning/service/LearningService.java#getOverview` still resolves homework solely via `homework_targets` (no read of `homework_assignments.unit_id`) (FR-012/FR-013/FR-016)
- [x] T028 [US3] In `front-end/src/components/admin/units/UnitContentPicker.tsx`, surface per-homework assignment within the unit: show each homework's current assignees and provide assign/unassign reusing the existing `setHomeworkAssignees` (`PUT /api/v1/admin/homework/{id}/assignees`) flow in `front-end/src/lib/admin.ts` (FR-014) (depends on T018)
- [x] T029 [P] [US3] Add i18n keys for the in-unit homework assignment UI (`admin.units.homework.assign`, `.assignedTo`, `.unassigned`, etc.) to `front-end/src/i18n/locales/es.ts`, `en.ts`, `ro.ts`

**Checkpoint**: Homework remains teacher-gated per student and is manageable from the unit view. All three stories functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup, legacy handling, and validation across stories.

- [x] T030 Remove the now-unused `front-end/src/components/admin/homework/HomeworkTab.tsx` and `front-end/src/components/admin/presentations/PresentationsTab.tsx` (and dead imports); keep `HomeworkAdminList`/`PresentationAdminList` logic only if reused by the unit view, otherwise delete dead code per the constitution
- [x] T031 Decide legacy per-presentation share handling (research.md open item): remove the standalone "Share" control from the new UI while leaving `presentation_shares` rows/queries intact for legacy access; update `front-end/src/components/admin/presentations/SharePresentationDialog.tsx` usage accordingly
- [x] T032 [P] Update the `/panel` row description in `CLAUDE.md` ("availability editor, homework authoring, presentation builder" â†’ include the unified Units tab)
- [x] T033 [P] (Optional) Add a back-end test in `back-end/src/test/java/.../units/` covering the presentation-access UNION (legacy share OR unit assignment) and that unit assignment never grants homework
- [x] T034 Run `cd back-end && ./gradlew build` and `cd front-end && npm run lint && npm run build`; fix any failures
- [x] T035 Execute [quickstart.md](quickstart.md) scenarios Aâ€“D in the browser and confirm SC-001â€¦SC-006

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup. BLOCKS all user stories.
- **User Stories (Phase 3â€“5)**: All depend on Foundational. US1 â†’ US2 â†’ US3 in priority order; US2 and US3 reuse US1 components but each is independently testable.
- **Polish (Phase 6)**: Depends on the desired stories being complete.

### User Story Dependencies

- **US1 (P1)**: Only Foundational. Pure MVP.
- **US2 (P2)**: Foundational + reuses `UnitCard`/`lib/admin` from US1 (T017/T014) for the assign UI; back-end access change (T022) is independent of US1.
- **US3 (P3)**: Foundational + reuses `UnitContentPicker` (T018); back-end is verification-only.

### Within Each Story

- Repository â†’ Service â†’ Controller â†’ front-end lib â†’ components.
- Models before services; services before endpoints; core before integration.

### Parallel Opportunities

- Setup: T002 âˆ¥ T001.
- Foundational: T004, T005, T007, T009 are [P] (distinct files) after/with T003; T006, T008 follow.
- US1: T014, T016, T019 marked [P]; T010â€“T013 (back-end) can proceed alongside front-end T014/T016/T019.
- US2: T024 [P] alongside back-end T020â€“T023.
- US3: T029 [P] alongside T027/T028.

---

## Parallel Example: User Story 1

```bash
# Back-end and front-end tracks in parallel once Foundational is done:
Task: "T014 [P] Add unit API functions to front-end/src/lib/admin.ts"
Task: "T016 [P] Create UnitsTab in front-end/src/components/admin/units/UnitsTab.tsx"
Task: "T019 [P] Add i18n keys to front-end/src/i18n/locales/{es,en,ro}.ts"
# while:
Task: "T010 Add unit-membership repository methods in UnitRepository.java"
Task: "T011 Implement UnitService.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup â†’ 2. Phase 2 Foundational â†’ 3. Phase 3 US1.
4. **STOP and VALIDATE**: teacher can manage units + contents in one tab. Demo.

### Incremental Delivery

1. Setup + Foundational â†’ foundation ready.
2. US1 â†’ unified Units tab (MVP). 3. US2 â†’ assignment grants presentations.
4. US3 â†’ per-student homework gating from the unit. Each ships without breaking the prior.

---

## Notes

- [P] = different files, no incomplete-task dependency.
- The presentation-access UNION (T022) is the highest-risk change â€” covered by optional test T033 and quickstart Scenario D step 1 (legacy continuity, SC-006).
- Homework access is deliberately untouched (T027 is verification) to guarantee FR-012.
- Commit after each task or logical group; stop at any checkpoint to validate a story independently.
