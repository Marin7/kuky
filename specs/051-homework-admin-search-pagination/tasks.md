---

description: "Task list template for feature implementation"
---

# Tasks: Homework Admin Search, Sort & Pagination

**Input**: Design documents from `/specs/051-homework-admin-search-pagination/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/README.md, quickstart.md

**Tests**: Not included. The feature spec does not request tests/TDD, and `front-end/package.json` has no test framework configured (no Vitest/Testing Library) — per `plan.md`'s Technical Context, verification is manual in a running browser (Phase "Polish", T017, runs `quickstart.md`).

**Organization**: Tasks are grouped by user story (P1/P2/P3 from `spec.md`), in priority order. All three stories are front-end-only and touch the same file, `front-end/src/components/admin/homework/HomeworkAdminList.tsx` (per `plan.md`'s Structure Decision — no backend changes), so within each story tasks run sequentially; only genuinely separate-file tasks are marked `[P]`.

## Path Conventions

This is the existing `front-end/` + `back-end/` web app split (per `plan.md`). This feature only touches `front-end/src/`; no `back-end/` paths are involved.

---

## Phase 1: Setup

**Purpose**: Confirm the existing primitives this feature reuses are in place — no new dependencies to install (per `research.md`, no new libraries are introduced).

- [X] T001 Verify `front-end/src/components/ui/input.tsx`, `select.tsx`, and `pagination.tsx` (Shadcn primitives) exist and export the components used below; no installs needed.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented.

None required. Every story operates on the same already-existing `HomeworkAdminList.tsx` component (per `data-model.md`'s `HomeworkListViewState`), and no shared entity, endpoint, or library needs to exist first — including the one new hook (`useDebouncedValue`), which is scoped to User Story 1 below since it doesn't block US2/US3.

**Checkpoint**: Nothing to build here — proceed directly to Phase 3.

---

## Phase 3: User Story 1 - Find a homework quickly by name (Priority: P1) 🎯 MVP

**Goal**: Add a debounced title-search box to the Homework tab that narrows the list, combined (AND) with the existing type/level/label filters, with a clear "no results" state.

**Independent Test**: Open the Homework tab, type a substring of a known homework's title, confirm only matching homeworks remain; combine the search with an existing type/level/label filter and confirm the intersection is shown; clear the box and confirm the full (filtered-by-the-other-filters) list returns; search for a nonsense string and confirm an explicit empty-results message appears.

### Implementation for User Story 1

- [X] T002 [P] [US1] Create `useDebouncedValue<T>(value: T, delayMs: number): T` hook in `front-end/src/hooks/use-debounced-value.ts` (per `research.md` Decision 3).
- [X] T003 [US1] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, add `search` state (raw string, default `""`) and derive a debounced value from it via `useDebouncedValue` (~250ms).
- [X] T004 [US1] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, add a search `Input` field to the existing filter toolbar (alongside the type/level/label `Select` controls), bound to `search` state.
- [X] T005 [US1] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, extend the existing item-filtering chain (the `.filter(...)` currently checking `filterType`/`filterLevel`/`filterLabel`) to also require a case-insensitive, trimmed substring match of the debounced search value against `item.title`, so all active filters combine with logical AND (FR-001, FR-001a).
- [X] T006 [US1] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, add an explicit "no matching homeworks" empty-state message, rendered when the filtered list is empty, distinct from the existing loading state (FR-006).

**Checkpoint**: User Story 1 is fully functional and independently testable — search combines correctly with existing filters, and the empty state works.

---

## Phase 4: User Story 2 - See the newest homeworks first by default, or re-sort as needed (Priority: P2)

**Goal**: Replace the current hardcoded alphabetical-only default with a sort control offering creation-date descending (new default), creation-date ascending, title A→Z, and title Z→A — applied over the User Story 1 filtered set.

**Independent Test**: Load the tab fresh (sort control untouched) and confirm the most recently created homework is first; switch the sort control through each of the other three options and confirm the full filtered set re-orders correctly (not just the visible portion).

### Implementation for User Story 2

- [X] T007 [US2] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, add `sortBy: "createdAt" | "title"` and `sortDir: "asc" | "desc"` state, defaulting to `("createdAt", "desc")`.
- [X] T008 [US2] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, add a sort `Select` control to the toolbar with the four options (creation date ↓ default, creation date ↑, title A→Z, title Z→A), wired to `sortBy`/`sortDir`.
- [X] T009 [US2] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, replace the existing unconditional `.sort((a, b) => a.title.localeCompare(b.title, "es", ...))` call with logic branching on `sortBy`/`sortDir`: for `"createdAt"`, use the already-server-sorted (DESC) incoming array order directly, reversed for ascending; for `"title"`, reuse the existing `localeCompare(..., "es", { sensitivity: "base" })` comparator, reversed for Z→A (per `research.md` Decision 4) — applied to the Phase 3 filtered items.

**Checkpoint**: User Stories 1 and 2 are both independently functional together — search/filters plus all four sort orders behave correctly.

---

## Phase 5: User Story 3 - Browse the list in manageable pages (Priority: P3)

**Goal**: Cap the visible list at 20 items per page with navigation controls, computed from the fully filtered-and-sorted (US1 + US2) result set, resetting to page 1 whenever search, filters, or sort change.

**Independent Test**: Seed more than 20 homeworks and confirm exactly 20 show per page with working navigation; while on page 2+, change the search term/a filter/the sort order and confirm the view resets to page 1; confirm 20-or-fewer total (or matching) homeworks show no pagination controls.

### Implementation for User Story 3

- [X] T010 [US3] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, add `page` state (0-based, default `0`).
- [X] T011 [US3] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, compute `pageItems = sortedAndFilteredItems.slice(page * 20, page * 20 + 20)` and `totalPages = Math.ceil(sortedAndFilteredItems.length / 20)`, and render `pageItems` in place of the full list (FR-003, FR-004).
- [X] T012 [US3] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, render the existing `front-end/src/components/ui/pagination.tsx` component below the list, wired to `page`/`totalPages`, hidden when `totalPages <= 1`.
- [X] T013 [US3] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, add an effect that resets `page` to `0` whenever `search` (debounced), `filterType`, `filterLevel`, `filterLabel`, `sortBy`, or `sortDir` changes (FR-005).
- [X] T014 [US3] In `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, clamp `page` into `[0, max(totalPages - 1, 0)]` whenever `totalPages` shrinks below the current `page` (edge case from `spec.md`).

**Checkpoint**: All three user stories are now independently functional together — the full search → sort → paginate pipeline works end to end.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Confirm nothing outside this feature's scope regressed, then validate the whole feature end to end.

- [ ] T015 [P] Manually confirm existing per-homework admin actions (expand, edit, view assignees/submissions in `front-end/src/components/admin/homework/HomeworkAdminCard.tsx`) still work unaffected regardless of active page/search/sort/filter state (FR-008).
- [ ] T016 [P] Manually confirm student-facing `/aprendizaje` homework presentation is unaffected — no code changes expected there; spot-check only (FR-007).
- [ ] T017 Run all 10 scenarios in `specs/051-homework-admin-search-pagination/quickstart.md` end to end in a running browser (per the project constitution's Development Workflow rule requiring browser verification for all UI changes).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Empty — nothing blocks Phase 3.
- **User Story 1 (Phase 3)**: Can start immediately after Setup. No dependency on US2/US3.
- **User Story 2 (Phase 4)**: Builds on the filtered-items pipeline introduced in US1 (T005) — implement after Phase 3 for a clean incremental diff, though it does not depend on US1's UI elements existing, only on the same array being available to sort.
- **User Story 3 (Phase 5)**: Builds on the filtered-and-sorted pipeline from US1 + US2 (T005, T009) — implement after Phase 4.
- **Polish (Phase 6)**: Depends on Phases 3–5 all being complete.

### Within Each User Story

- All tasks within a story edit the same file (`HomeworkAdminList.tsx`) and are ordered to apply as a clean sequential diff: state → UI control → pipeline logic → (US1 only) empty state / (US3 only) reset & clamp behavior.

### Parallel Opportunities

- T002 (`use-debounced-value.ts`) is a new, separate file and can be done in parallel with T001.
- T015 and T016 are independent manual checks and can be done in parallel with each other.
- Because US1/US2/US3 all edit the same component file, they are **not** safely parallelizable across developers — implement in priority order (P1 → P2 → P3) as listed.

---

## Parallel Example: Setup + User Story 1 kickoff

```bash
# T001 and T002 have no dependency on each other and touch different files:
Task: "Verify Shadcn input/select/pagination primitives exist in front-end/src/components/ui/"
Task: "Create useDebouncedValue hook in front-end/src/hooks/use-debounced-value.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001).
2. Phase 2: Foundational — nothing to do.
3. Complete Phase 3: User Story 1 (T002–T006).
4. **STOP and VALIDATE**: Run quickstart.md scenarios 1–2 (search narrows the list; search combines with existing filters) independently.
5. This alone already resolves the described pain point (finding a homework by name in a long list) and can ship as the MVP.

### Incremental Delivery

1. Setup → Phase 3 (US1) → validate → ship (MVP: search).
2. Add Phase 4 (US2) → validate sort behavior → ship (adds sort control, changes the default order).
3. Add Phase 5 (US3) → validate pagination → ship (caps the view at 20/page).
4. Phase 6 (Polish) → run the full quickstart.md suite → done.

Each phase is a working, shippable increment on top of the last — none of them requires the later phases to function correctly.
