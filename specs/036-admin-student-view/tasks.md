# Tasks: Admin Student View Overhaul

**Input**: Design documents from `/specs/036-admin-student-view/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: No TDD suite requested in the spec. Existing backend unit tests that assert `progress` MUST be updated as part of foundational API work.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Backend: `back-end/src/main/java/…`, tests under `back-end/src/test/java/…`
- Frontend: `front-end/src/…`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm feature workspace; no new project or dependencies

- [x] T001 Verify branch `036-admin-student-view` and design docs under `specs/036-admin-student-view/` (plan.md, spec.md, research.md, data-model.md, contracts/admin-student-profile-api.md, quickstart.md)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Remove obsolete `progress` from the profile API and frontend types; add client breakdown helper used by all stories

**⚠️ CRITICAL**: No user story UI work that assumes the new response shape should begin until this phase is complete

- [x] T002 Remove `progress` field and progress aggregation from `back-end/src/main/java/com/kuky/backend/admin/service/StudentProfileAdminService.java` (drop unused `UnitRepository` / `ActivitySubmissionRepository` deps if unreferenced)
- [x] T003 Remove `progress` from `back-end/src/main/java/com/kuky/backend/admin/dto/StudentProfileResponse.java`
- [x] T004 [P] Delete unused progress DTOs if nothing else references them: `back-end/src/main/java/com/kuky/backend/admin/dto/StudentProgressDto.java`, `UnitProgressDto.java`, `HomeworkBreakdownDto.java`, `ActivityBreakdownDto.java`
- [x] T005 Update `back-end/src/test/java/com/kuky/backend/admin/StudentProfileAdminServiceTest.java` to drop `progress` assertions and keep useful profile/homework/booking coverage
- [x] T006 Remove `progress` / `StudentProgress` / related types from `StudentProfile` in `front-end/src/lib/admin.ts`
- [x] T007 Add a pure homework-breakdown helper (pending / submitted / completed from `homeworks[].status`) in `front-end/src/lib/admin.ts` (or a small adjacent helper module under `front-end/src/lib/` / `front-end/src/components/admin/students/`) matching [data-model.md](./data-model.md) bucket rules

**Checkpoint**: `GET` student profile no longer returns `progress`; frontend types and breakdown helper ready

---

## Phase 3: User Story 1 - Teacher expands Tareas to review all homeworks (Priority: P1) 🎯 MVP

**Goal**: Collapsed Tareas shows count + pending/submitted/completed; click expands a full-width homework list under the stats row with status, score, and open-response actions (parity with today’s lower list).

**Independent Test**: Open a student with mixed homework statuses; collapsed card shows breakdown; expand shows full-width list with working review/result actions; collapse restores the card summary.

### Implementation for User Story 1

- [x] T008 [P] [US1] Add i18n strings for Tareas expand/collapse and breakdown labels (reuse or retarget former progress homework bucket keys) in `front-end/src/i18n/locales/es.ts`, `front-end/src/i18n/locales/en.ts`, `front-end/src/i18n/locales/ro.ts`
- [x] T009 [US1] Make the Tareas stats card an expandable control in `front-end/src/routes/panel_.alumnos.$studentId.tsx`: collapsed state shows total + `StudentHomeworkBreakdown` (or equivalent) from the T007 helper
- [x] T010 [US1] When expanded, render the full homework list full-width immediately below the stats grid in `front-end/src/routes/panel_.alumnos.$studentId.tsx` (pending-first sort, `StatusBadge`, score, needs-review / view-result → existing `HomeworkReviewDialog` / `ExerciseResultDialog`)
- [x] T011 [US1] Wire expand/collapse toggle state (default collapsed) and empty-state copy when `homeworks.length === 0` in `front-end/src/routes/panel_.alumnos.$studentId.tsx`
- [x] T012 [US1] Optionally extract expandable Tareas UI into `front-end/src/components/admin/students/` only if the route file stays clearer; keep behavior identical

**Checkpoint**: Tareas expand/collapse works with breakdown + list actions; lower duplicate section may still exist until US3

---

## Phase 4: User Story 2 - Teacher no longer sees the incomplete Progreso section (Priority: P1)

**Goal**: Remove the entire Progreso section (units, attended count, level chip, activity/homework breakdown under Progreso) without relocating attended/level summaries.

**Independent Test**: Open any student profile — no Progreso heading or progress-only chips; upcoming/past/presentations/interests/placement evaluation still present.

### Implementation for User Story 2

- [x] T013 [US2] Delete the Progreso `Section` and all `profile.progress` / placement-level-in-progress UI from `front-end/src/routes/panel_.alumnos.$studentId.tsx`
- [x] T014 [P] [US2] Remove unused Progreso-only i18n keys (title, empty, units, attendedClasses, level, activity breakdown labels, etc.) from `front-end/src/i18n/locales/es.ts`, `en.ts`, `ro.ts` once nothing references them
- [x] T015 [US2] Confirm past-classes no-show toggles and the separate placement evaluation block still work in `front-end/src/routes/panel_.alumnos.$studentId.tsx` (no dependency on removed attended-count)

**Checkpoint**: Progreso is gone; US1 Tareas behavior still intact

---

## Phase 5: User Story 3 - Duplicate lower Tareas section is removed (Priority: P2)

**Goal**: Exactly one homework list surface — the expandable Tareas control; remove the separate lower Tareas `Section`.

**Independent Test**: Profile with homeworks shows list only when Tareas is expanded under the stats row; no second Tareas heading further down.

### Implementation for User Story 3

- [x] T016 [US3] Remove the lower Tareas `Section` (duplicate homework list) from `front-end/src/routes/panel_.alumnos.$studentId.tsx`, keeping a single list implementation used by the expander
- [x] T017 [US3] Ensure empty-homework UX lives only on the expandable Tareas control (no orphan empty lower section) in `front-end/src/routes/panel_.alumnos.$studentId.tsx`

**Checkpoint**: Single homework surface on the profile

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup and end-to-end validation

- [x] T018 [P] Retarget or slim `front-end/src/components/admin/students/StudentHomeworkBreakdown.tsx` i18n defaults if they still point at deleted `admin.studentProfile.progress.*` keys
- [x] T019 Run backend tests: `./gradlew test --tests '*StudentProfileAdmin*'` from `back-end/`
- [x] T020 Run frontend lint/typecheck as used in the project (`npm run lint` in `front-end/`) and fix issues from this feature
- [ ] T021 Execute manual scenarios A–E in `specs/036-admin-student-view/quickstart.md` in the browser
- [x] T022 [P] Spot-check `contracts/admin-student-profile-api.md` against the final `StudentProfileResponse` / `getStudentProfile` types for drift

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS** all user stories
- **US1 (Phase 3)**: Depends on Foundational
- **US2 (Phase 4)**: Depends on Foundational; safest after or with US1 because both edit `panel_.alumnos.$studentId.tsx`
- **US3 (Phase 5)**: Depends on US1 (list must live in the expander before deleting the lower section)
- **Polish (Phase 6)**: Depends on US1–US3 complete

### User Story Dependencies

- **User Story 1 (P1)**: After Foundational — MVP
- **User Story 2 (P1)**: After Foundational; same route file as US1 → prefer sequential with US1
- **User Story 3 (P2)**: After US1 (requires expander list)

### Parallel Opportunities

- T004 parallel with T002–T003 once response field removal is clear
- T008 (i18n) parallel with early US1 layout work if keys are agreed
- T014 (i18n cleanup) parallel with T015 after Progreso JSX is gone
- T018 and T022 parallel in Polish
- US2 i18n cleanup can parallel backend T019 once FE compile is green

---

## Parallel Example: Foundational

```text
# After T002–T003 land (or in careful coordination):
Task: "Delete unused progress DTOs under back-end/.../admin/dto/"
Task: "Remove progress types from front-end/src/lib/admin.ts"   # can start once contract agreed
```

## Parallel Example: User Story 1

```text
Task: "Add i18n strings in front-end/src/i18n/locales/{es,en,ro}.ts"
Task: "Implement expandable Tareas + list in panel_.alumnos.$studentId.tsx"  # after helper T007
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1–2 (API + types + breakdown helper)
2. Complete Phase 3 (expandable Tareas)
3. **STOP and VALIDATE** quickstart scenarios B–D (Progreso/lower list may still be present)
4. Then US2 → US3 → Polish

### Incremental Delivery

1. Foundational → profile API without `progress`
2. US1 → expandable Tareas MVP
3. US2 → remove Progreso
4. US3 → remove duplicate lower Tareas
5. Polish → quickstart A–E green

### Suggested MVP scope

**Phases 1–3 (Setup + Foundational + US1)** — teacher can expand Tareas with breakdown and open responses. US2/US3 complete the overhaul and should ship in the same PR if possible (same page).

---

## Notes

- Spec did not request new automated FE tests; rely on updated backend unit tests + quickstart browser checks
- Prefer one list implementation shared by the expander (avoid copy-paste then delete in US3)
- Do not relocate attended-classes or CEFR chips when removing Progreso
- Commit after each phase or logical group when asked
