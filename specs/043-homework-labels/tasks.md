---
description: "Task list for homework labels"
---

# Tasks: Homework Labels

**Input**: Design documents from `specs/043-homework-labels/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/homework-labels-api.md](./contracts/homework-labels-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit for normalize/persist (`trim`, empty→null, max 40, round-trip) and label-only update must not bump `content_revised_at` (per [plan.md](./plan.md)). Frontend — browser verification via [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Client type for the optional admin-only label. No new packages.

- [x] T001 Add optional `label: string | null` to `HomeworkAdminItem` and pass `label` on create/update JSON in `front-end/src/lib/admin.ts` (`createHomework`, `updateHomework`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Persist one optional label on `homework_assignments`, expose it on admin homework JSON, reject too-long after trim, and keep freeze unrelated. Blocks all user stories.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 Create Flyway `back-end/src/main/resources/db/migration/V24__homework_label.sql`: `ALTER TABLE homework_assignments ADD COLUMN label VARCHAR(40)` (nullable; existing rows unlabeled) per [data-model.md](./data-model.md)
- [x] T003 [P] Add `label` getter/setter on `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkAssignment.java`
- [x] T004 [P] Add `label` to `back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkAdminItem.java`, `CreateHomeworkRequest.java`, and `UpdateHomeworkRequest.java` (do **not** put `@Size(max = 40)` on the raw JSON string; optional `@Size(max = 80)` request-size guard is allowed) per [contracts/homework-labels-api.md](./contracts/homework-labels-api.md)
- [x] T005 Map `label` on SELECT / INSERT / UPDATE in `back-end/src/main/java/com/kuky/backend/learning/repository/ContentRepository.java` (`createAssignment` / `updateAssignment` / row mapper)
- [x] T006 Normalize and persist `label` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`: trim; blank → `null`; after trim length > 40 → `IllegalArgumentException` (existing `VALIDATION_ERROR`); include `label` on `toItem`; **do not** treat label as `contentChanged` (label-only PUT must not bump `content_revised_at`); do **not** add `label` to `assignment_snapshot` per [research.md](./research.md)

**Checkpoint**: `POST`/`PUT`/`GET /api/v1/admin/homework` round-trip a trimmed label; empty label stores `null`; 41+ characters after trim return 400; student learning JSON still has no `label`.

---

## Phase 3: User Story 1 - Teacher labels a homework (Priority: P1) — MVP

**Goal**: Teacher can set or leave blank an optional short label when creating or editing a homework; it persists and reloads on the editor.

**Independent Test**: Create with `Subjuntivo`, reopen — field still set. Create with empty label — saves as today. 41+ characters — rejected, not truncated ([quickstart.md](./quickstart.md) Scenario 1).

### Implementation for User Story 1

- [x] T007 [P] [US1] Add editor/filter i18n strings (`labelLabel`, `labelPlaceholder`, `allLabels`, too-long if shown client-side) in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`
- [x] T008 [US1] Add optional label field (free text, clearable) on `front-end/src/components/admin/homework/HomeworkEditorPage.tsx`; load `hw.label`; pass it to `createHomework` / `updateHomework`; place it with type/level/due date
- [x] T009 [US1] Tests: create/update persist trim, whitespace-only → null, max 40 accepted, 41 rejected with `VALIDATION_ERROR` (no silent truncate) in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` and/or `HomeworkAdminControllerIntegrationTest.java`

**Checkpoint**: Labels can be saved and reopened. FR-001, FR-003, FR-004, FR-005. Filter/badges not required yet.

---

## Phase 4: User Story 2 - Teacher filters the Homework tab by label (Priority: P1)

**Goal**: Homework tab filter choices are “all labels” plus each label in use (case-insensitive groups, accents distinct). Combined with type and level (AND). No unlabeled choice. Empty combined set uses existing `noTasksFiltered`.

**Independent Test**: Mixed `Subjuntivo` / `Ser/Estar` / unlabeled; filter `Subjuntivo` hides the others; all-labels shows unlabeled again; no unlabeled option in the dropdown ([quickstart.md](./quickstart.md) Scenario 2).

### Implementation for User Story 2

- [x] T010 [US2] Add `labelGroupKey` (trim + `toLocaleLowerCase('es')`) and `uniqueLabels` (one display spelling per key, first-seen) in `front-end/src/lib/homeworkLabels.ts` per [research.md](./research.md) §3
- [x] T011 [US2] Add label `Select` beside type/level in `front-end/src/components/admin/homework/HomeworkAdminList.tsx`: `ALL` + `uniqueLabels(items)`; AND with existing type/level filters; unlabeled items only when `ALL`; do **not** add an unlabeled option; keep `admin.homework.noTasksFiltered` for empty combined results (FR-006, FR-006a, FR-007, FR-008)

**Checkpoint**: Tab can be narrowed by label. FR-006/FR-006a/FR-007/FR-008. Badges and combobox reuse can wait for US3.

---

## Phase 5: User Story 3 - Teacher sees and reuses labels on the list (Priority: P2)

**Goal**: Labeled cards show the stored label; unlabeled cards have no chip. Editor offers labels already in use (one row per case-fold group) and still allows a new string. `Subjuntivo` and `subjuntivo` share one filter/reuse entry; each card keeps its saved capitalization.

**Independent Test**: Two cards labeled `Indicativo`; third picks it from reuse; fourth types `indicativo`; one filter choice groups all four ([quickstart.md](./quickstart.md) Scenario 3).

### Implementation for User Story 3

- [x] T012 [US3] Show a label badge on each card when `item.label` is set (no placeholder when null) in `front-end/src/components/admin/homework/HomeworkAdminList.tsx` (alongside type/level chips) (FR-009)
- [x] T013 [US3] Replace the US1 text field with a combobox (`Popover` + `Command`, same idea as `front-end/src/components/admin/units/AddContentCombobox.tsx`) on `front-end/src/components/admin/homework/HomeworkEditorPage.tsx`: type a new label or pick from `uniqueLabels` of `getHomework()`; one option per group; saving typed `indicativo` stores that exact text (FR-010, FR-010a)

**Checkpoint**: List is scannable by badge; reuse avoids duplicate filter entries. FR-009, FR-010, FR-010a.

---

## Phase 6: User Story 4 - Teacher changes or removes a label (Priority: P2)

**Goal**: Change or clear a label without touching submissions. When the active filter’s last homework loses that label, the tab returns to all labels and the stale option disappears.

**Independent Test**: Retag Unidad 3 → 4; clear a label; delete/clear the last homework of a filtered label → dropdown drops it and list is on all labels; student take is not reset by a label-only save ([quickstart.md](./quickstart.md) Scenario 4).

### Implementation for User Story 4

- [x] T014 [US4] If the selected label group key is no longer in `uniqueLabels(items)`, reset the label filter to `ALL` in `front-end/src/components/admin/homework/HomeworkAdminList.tsx` (FR-008a, FR-012)
- [x] T015 [US4] Integration test: label-only PUT leaves `content_revised_at` and existing submission scores/snapshots unchanged in `back-end/src/test/java/com/kuky/backend/admin/HomeworkUpdatePreservesSubmissionsIntegrationTest.java` or `HomeworkAdminControllerIntegrationTest.java` (FR-011)
- [x] T016 [P] [US4] Confirm `label` is absent from student DTOs (`HomeworkItemResponse`, exercise take) in `back-end/src/main/java/com/kuky/backend/learning/dto/` and from `front-end/src/lib/learning.ts`; review-queue DTOs stay unlabeled (FR-013, FR-014)

**Checkpoint**: Labels are editable metadata only. FR-008a, FR-011, FR-012, FR-013, FR-014.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end checks; no student-facing leak; i18n complete.

- [ ] T017 Run [quickstart.md](./quickstart.md) Scenarios 1–5 in the browser (`:8080` / `:8081`, teacher on Tareas)
- [x] T018 [P] `npm run lint` in `front-end/` and `./gradlew test --tests '*HomeworkAdmin*'` in `back-end/`
- [x] T019 [P] Confirm no new label filter in `front-end/src/components/admin/units/UnitContentPicker.tsx`, `front-end/src/routes/panel_.alumnos.$studentId.tsx`, and `front-end/src/components/admin/homework/HomeworkReviewQueue.tsx` (they may ignore `HomeworkAdminItem.label`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS** all user stories (T005 depends on T002/T003; T006 depends on T004/T005)
- **US1 (Phase 3)**: After Foundational — MVP persist; T008 after T001+T007
- **US2 (Phase 4)**: After Foundational (list already returns `label` via T006); T011 uses T010
- **US3 (Phase 5)**: After US1 editor exists (T008); T012 can follow US2 list; T013 upgrades T008 field
- **US4 (Phase 6)**: T014 after T011; T015 after T006
- **Polish (Phase 7)**: After desired stories complete

### User Story Dependencies

| Story | Depends on | Independently testable? |
|-------|------------|-------------------------|
| US1 Save label on create/edit | Phase 2 | Yes — editor round-trip, too-long reject |
| US2 Filter tab by label | Phase 2 (`label` on list items) | Yes — even with a raw JSON label and no badge |
| US3 Badge + reuse combobox | US1 field; US2 helper | Yes — badges and pick-existing |
| US4 Clear / stale filter / freeze | US2 filter; Phase 2 persist | Yes — retag, clear, last-label reset, no freeze bump |

### Within Each User Story

- Schema/model before repository before service
- API client types before editor
- Filter helper before list Select
- Story complete before moving to the next priority when sharing the same file (`HomeworkEditorPage.tsx`, `HomeworkAdminList.tsx`)

### Parallel Opportunities

- T003 and T004 (model vs DTOs)
- T007 i18n while T006 service is in progress
- T016 student-DTO audit vs T014/T015
- T018 and T019 during/after T017

---

## Parallel Example: Foundational + US1 types

```bash
# After T002 migration exists:
Task: "Add label on HomeworkAssignment.java"
Task: "Add label on admin DTOs (Item, Create, Update)"

# After T001:
Task: "i18n strings in es.ts / en.ts / ro.ts"
# then editor T008
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: save/reload label; too-long rejected
5. Filtering is the product goal — ship US2 next in the same increment if possible

### Incremental Delivery

1. Setup + Foundational → admin JSON has `label`
2. US1 → teacher can tag homework (MVP persist)
3. US2 → tab is actually smaller to scan (P1 outcome)
4. US3 → badges + reuse
5. US4 → clear, stale filter, freeze safety
6. Each story keeps previous behaviour

### Parallel Team Strategy

With two people after Phase 2: A does US1 editor + tests; B does US2 helper + list filter (different files until US3/US4 touch `HomeworkAdminList.tsx` / `HomeworkEditorPage.tsx`).

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to US1–US4
- No labels table, no `?label=` query, no unlabeled filter, no student `label` field
- Commit after each task or logical group
- Stop at any checkpoint to validate the story independently
