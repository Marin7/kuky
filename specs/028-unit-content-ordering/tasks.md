---
description: "Task list for unit content ordering (mixed presentations + homeworks)"
---

# Tasks: Unit Content Ordering

**Input**: Design documents from `specs/028-unit-content-ordering/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/unit-content-ordering-api.md](contracts/unit-content-ordering-api.md), [quickstart.md](quickstart.md)

**Tests**: Backend JUnit for reorder/membership/seed per [plan.md](plan.md). Frontend has no unit-test framework — verify via [quickstart.md](quickstart.md) in a browser.

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Frontend DnD dependency and Flyway migration every story depends on.

- [x] T001 Install `@dnd-kit/core`, `@dnd-kit/sortable`, and `@dnd-kit/utilities` in `front-end/package.json` (npm install from `front-end/`)
- [x] T002 Write `back-end/src/main/resources/db/migration/V10__unit_content_position.sql` per [data-model.md](data-model.md): add `unit_position INT NOT NULL DEFAULT 0` to `presentations` and `homework_assignments`; seed each unit as presentations (`ORDER BY updated_at DESC`) then homeworks (`ORDER BY created_at DESC`) with contiguous `0..n-1`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared DTOs, ordered content reads, and `UnitDetail.contents` shape that all stories need.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T003 [P] Add `UnitContentRef.java`, `UnitContentItem.java`, and `ReorderUnitContentsRequest.java` under `back-end/src/main/java/com/kuky/backend/units/dto/` per [contracts/unit-content-ordering-api.md](contracts/unit-content-ordering-api.md)
- [x] T004 Change `back-end/src/main/java/com/kuky/backend/units/dto/UnitDetail.java` to expose ordered `contents: List<UnitContentItem>` instead of separate `presentations` / `homeworks` lists
- [x] T005 Map `INVALID_CONTENT_ORDER` in `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java` (IllegalArgumentException or dedicated exception → 400)
- [x] T006 Update `back-end/src/main/java/com/kuky/backend/units/repository/UnitRepository.java` to load unit presentations/homeworks ordered by `unit_position` and assemble a mixed ordered content query (or merge in service) for detail mapping; stop using `updated_at`/`created_at` as the display order
- [x] T007 Update `back-end/src/main/java/com/kuky/backend/units/service/UnitService.java` `get` / create / update / set* response mapping to build `UnitDetail.contents` from ordered rows (depends on T003, T004, T006)
- [x] T008 [P] Update `UnitDetail` / content types in `front-end/src/lib/admin.ts` to match `contents[]` (`type`, `unitPosition`, nested presentation/homework); adjust any callers that read `detail.presentations` / `detail.homeworks`

**Checkpoint**: Migration applied; admin unit detail returns a single ordered `contents` array. Reorder endpoint and DnD UI still TODO.

---

## Phase 3: User Story 1 - Teacher sets a mixed learning sequence (Priority: P1) — MVP

**Goal**: Teacher sees one ordered unit content list and reorders via drag-and-drop; order persists across reload.

**Independent Test**: Open a unit with ≥2 presentations and ≥2 homeworks; drag to interleave; reload; confirm same order in UI and `GET /api/v1/admin/units/{id}`.

### Tests for User Story 1

- [x] T009 [P] [US1] Extend `back-end/src/test/java/com/kuky/backend/units/UnitRepositoryTest.java` (or add `UnitService` test) for `reorderContents`: happy-path contiguous positions; reject non-permutation with `INVALID_CONTENT_ORDER`

### Implementation for User Story 1

- [x] T010 [US1] Implement `UnitRepository.reorderContents(unitId, items)` rewriting `unit_position` `0..n-1` across both tables; validate exact member set in `UnitService.reorderContents` per [data-model.md](data-model.md) (depends on T002, T003, T005, T006)
- [x] T011 [US1] Add `PUT /api/v1/admin/units/{id}/contents/reorder` in `back-end/src/main/java/com/kuky/backend/units/controller/UnitAdminController.java` returning updated `UnitDetail` (depends on T010)
- [x] T012 [P] [US1] Add `reorderUnitContents(unitId, items)` in `front-end/src/lib/admin.ts` calling the new endpoint (depends on T008)
- [x] T013 [US1] Refactor `front-end/src/components/admin/units/UnitContentPicker.tsx` to render a **single** mixed list from `detail.contents` (type-distinguishable rows; no separate presentation/homework sections) (depends on T008)
- [x] T014 [US1] Add sortable drag-and-drop on that list with `@dnd-kit` (extract `UnitContentSortableList.tsx` under `front-end/src/components/admin/units/` if cleaner); on drag end call `reorderUnitContents`; include keyboard sensor or ▲/▼ fallback for a11y (depends on T001, T012, T013)
- [x] T015 [P] [US1] Add i18n keys for content-type labels / reorder errors under `admin.units.contents.*` in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`

**Checkpoint**: Teacher can DnD-reorder mixed unit contents and see persistence after reload. MVP deliverable.

---

## Phase 4: User Story 2 - Students follow the teacher's unit sequence (Priority: P2)

**Goal**: Student unit page shows one interleaved list of accessible items in teacher order; unassigned homeworks stay hidden.

**Independent Test**: Sequence A → HW1 → B → HW2; assign unit + HW1 only; student sees A, HW1, B in that order and not HW2.

### Implementation for User Story 2

- [x] T016 [P] [US2] Add `unitPosition` to `back-end/src/main/java/com/kuky/backend/learning/dto/SharedPresentationSummary.java` and `HomeworkItemResponse.java` (nullable/optional when unattached) per [contracts/unit-content-ordering-api.md](contracts/unit-content-ordering-api.md)
- [x] T017 [US2] Select and map `unit_position` in presentation share queries and `ContentRepository` / `LearningService.java` so overview payloads include `unitPosition` (depends on T002, T016)
- [x] T018 [P] [US2] Add `unitPosition?: number | null` on learning types in `front-end/src/lib/learning.ts`
- [x] T019 [US2] Update `front-end/src/components/learning/UnitDetailContent.tsx` to merge presentations + homework into one list sorted by `unitPosition`, render interleaved (type still clear); **remove** status-based sort as the primary order on the unit page (depends on T018)
- [x] T020 [US2] Adjust `front-end/src/components/learning/unitGroups.ts` / `UnitLearningView.tsx` only as needed to pass `unitPosition` through; leave non-unit (“Other”) surfaces type-segregated as today (depends on T019)

**Checkpoint**: Student unit view matches teacher relative order for accessible items only.

---

## Phase 5: User Story 3 - New and removed items keep a sensible sequence (Priority: P3)

**Goal**: Attach appends; detach/move preserves relative order; move-in appends on destination.

**Independent Test**: Add item → appears at end; remove middle → neighbours keep order; move to another unit → end of destination, gone from source.

### Tests for User Story 3

- [x] T021 [P] [US3] Extend backend unit tests for `setPresentations` / `setHomeworks`: append new ids; detach clears `unit_id`/`unit_position`; move between units appends on destination and removes from source without scrambling remaining order

### Implementation for User Story 3

- [x] T022 [US3] Rewrite `UnitRepository.setPresentations` / `setHomeworks` (and `UnitService`) so membership diffs **retain** relative mixed order, **append** newcomers at end, **detach** clears position, and cross-unit moves append on destination per [data-model.md](data-model.md) (depends on T006, T007)
- [x] T023 [US3] Ensure `UnitContentPicker.tsx` attach/detach flows still call existing set-presentations/homeworks APIs and refresh `contents` from the returned `UnitDetail` (depends on T013, T022)

**Checkpoint**: Attach/detach/move leave a contiguous, predictable sequence.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Docs, regression, quickstart validation.

- [x] T024 [P] Run backend tests: `cd back-end && ./gradlew test --tests '*Unit*'`
- [x] T025 Browser-validate full [quickstart.md](quickstart.md) (seed, DnD reorder, student interleave, attach/detach, access unchanged, unit-level reorder regression)
- [x] T026 [P] Sync `CLAUDE.md` Availability/Units note only if it documents unit contents as separate lists — mention mixed `unit_position` sequence if that inventory is maintained (optional doc sync)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Start immediately (T001 ∥ T002)
- **Foundational (Phase 2)**: Needs T002; T003–T008 block all stories
- **User Story 1 (Phase 3)**: Needs Foundational + T001; MVP
- **User Story 2 (Phase 4)**: Needs Foundational + T002 positions; can proceed after or in parallel with US1 UI once APIs expose positions (learning reads DB directly)
- **User Story 3 (Phase 5)**: Needs Foundational; best after US1 so picker refresh path exists
- **Polish (Phase 6)**: After desired stories complete

### User Story Dependencies

- **US1 (P1)**: After Foundational — no dependency on US2/US3
- **US2 (P2)**: After Foundational + migration seed; independent of DnD UI
- **US3 (P3)**: After Foundational; integrates with US1 picker

### Parallel Opportunities

- T001 ∥ T002
- T003 ∥ T005 ∥ T008 (after plan); T016 ∥ T018
- T012 ∥ T015 after types exist
- US1 backend (T010–T011) ∥ early US2 DTO work (T016) once migration exists
- T024 ∥ T026 during polish

---

## Parallel Example: User Story 1

```bash
# After Foundational + T001:
Task: "T009 UnitRepositoryTest reorderContents"
Task: "T012 reorderUnitContents in admin.ts"
Task: "T015 i18n admin.units.contents.*"

# Then sequential:
Task: "T010–T011 backend reorder"
Task: "T013 single list UnitContentPicker"
Task: "T014 @dnd-kit sortable + persist"
```

---

## Parallel Example: User Story 2

```bash
Task: "T016 backend learning DTO unitPosition"
Task: "T018 frontend learning.ts unitPosition"
# Then:
Task: "T017 map in repositories/LearningService"
Task: "T019–T020 UnitDetailContent interleaved list"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup (T001–T002)
2. Phase 2 Foundational (T003–T008)
3. Phase 3 US1 (T009–T015)
4. **STOP and VALIDATE**: Teacher DnD reorder + reload
5. Demo MVP

### Incremental Delivery

1. Setup + Foundational → ordered `UnitDetail.contents`
2. US1 → teacher DnD MVP
3. US2 → student interleaved unit page
4. US3 → attach/detach/move position rules
5. Polish → quickstart + tests

---

## Notes

- Do **not** reuse `homework_assignments.sort_order` for mixed unit order
- Do **not** change `PUT /admin/units/reorder` (unit-within-level)
- Access rules unchanged: reorder never reveals unassigned homeworks
- Commit after each task or logical group
- Avoid: dual presentation/homework sections in unit UIs after US1/US2
