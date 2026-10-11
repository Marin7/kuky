---

description: "Task list for 056-unit-viewer-navigation"
---

# Tasks: Unit Viewer Contents & Level Navigation

**Input**: Design documents from `specs/056-unit-viewer-navigation/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/ui-contract.md](contracts/ui-contract.md), [quickstart.md](quickstart.md)

**Tests**: Not requested in the spec, and the front-end has no test runner. Each story ends with a browser verification task against [quickstart.md](quickstart.md) (constitution: UI changes must be verified in a running browser).

**Organization**: Tasks are grouped by user story. **Front-end only** — every path is under `front-end/src/` unless stated; `back-end/` is not touched.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: US1 = contents panel (P1), US2 = level navigation (P2), US4 = phone layout (P2), US3 = highlight while scrolling (P3)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: i18n strings used by every story (keys from [ui-contract.md §6](contracts/ui-contract.md#6-i18n-keys-added-to-es-en-ro)).

- [X] T001 [P] Add to the student `learning.units` block (next to `unitEmpty`, ~line 531) in `front-end/src/i18n/locales/es.ts`: `contents: "Contenido"`, `openContents: "Ver contenido"`, `levelHeading: "Nivel {{level}}"`, `noPages: "Esta unidad aún no tiene páginas."`, `activityLabel: "Actividad"`, `quizLabel: "Prueba"`
- [X] T002 [P] Add the same six keys under `learning.units` in `front-end/src/i18n/locales/en.ts` ("Contents", "Show contents", "Level {{level}}", "This unit has no pages yet.", "Activity", "Quiz")
- [X] T003 [P] Add the same six keys under `learning.units` in `front-end/src/i18n/locales/ro.ts` ("Cuprins", "Vezi cuprinsul", "Nivel {{level}}", "Această unitate nu are încă pagini.", "Activitate", "Test")

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Stable DOM anchors in the PDF viewer and the anchor-id helpers — required by US1 (jump) and US3 (scroll spy).

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Create `front-end/src/lib/unitAnchors.ts` exporting `pageAnchorId(page: number) => "unit-page-" + page` and `activityAnchorId(activityId: string) => "unit-activity-" + activityId` (scroll helper is added in US1, T008)
- [X] T005 [P] In `PdfPageStack` in `front-end/src/components/learning/PresentationPdfViewerClient.tsx`, give each per-page wrapper `<div key={pageNumber} className="flex flex-col gap-3">` the attribute `id={pageAnchorId(pageNumber)}` and add `scroll-mt-24` to its className (import from `@/lib/unitAnchors`)
- [X] T006 [P] In `front-end/src/components/learning/ActivityPageSlot.tsx`, add `id={activityAnchorId(activity.id)}` and `scroll-mt-24` to the root `<section>` (keep the existing `data-activity-after-page`)

**Checkpoint**: Opening a unit renders `#unit-page-1…N` and `#unit-activity-{id}` elements; no visual change.

---

## Phase 3: User Story 1 — See and jump through a unit's contents (Priority: P1) 🎯 MVP

**Goal**: A left panel on the unit page lists every page title in order, each activity indented under its page with a status icon, and the unit's end quizzes as links; selecting a page/activity scrolls the viewer to it (FR-001–FR-005, FR-004a, FR-007, FR-013–FR-015).

**Independent Test**: [quickstart.md](quickstart.md) scenarios 1 (outline part only), 2, 3, 5, 6, 8 (panel part), 9 at desktop width.

- [X] T007 [P] [US1] Create `front-end/src/components/learning/unitOutline.ts` with the `OutlineEntry` union and `buildUnitOutline(group: UnitGroup): { entries: OutlineEntry[]; anchorIds: string[] }` per [data-model.md § UnitOutline](data-model.md#unitoutline--the-current-units-contents-built-by-buildunitoutlinegroup): take the first presentation in `group.presentations` that has a PDF file (`isPresentationPdf`), emit one `page` entry per `pageTitles[i]` (`page = i + 1`), insert the activity for that page (from `activities`, keyed by `page`) right after it as an `activity` entry (ignore activities with `page > pageTitles.length`), then append one `quiz` entry per `group.quizzes` in order; `anchorIds` = anchor ids of page + activity entries in the same order (use helpers from `@/lib/unitAnchors`)
- [X] T008 [P] [US1] Add `scrollToUnitAnchor(anchorId: string): void` to `front-end/src/lib/unitAnchors.ts` per [research R3](research.md#r3--jumping-to-a-page--activity-fr-004): poll with `requestAnimationFrame` up to ~5 s for `document.getElementById(anchorId)`; then `scrollIntoView({ block: "start", behavior: "smooth" })`; then for up to ~1.5 s after the smooth scroll settles, if the element's `getBoundingClientRect().top` differs from its `scroll-margin-top` by > 8 px, re-scroll once with `behavior: "auto"`; abort pending work on the first `wheel`, `touchstart` or `keydown` (passive listeners, removed on finish). SSR-safe (no-op when `typeof window === "undefined"`)
- [X] T009 [US1] Create `front-end/src/components/learning/UnitContentsPanel.tsx` (props per [ui-contract.md §5](contracts/ui-contract.md#5-componentslearningunitcontentspaneltsx): `groups`, `current`, `activeAnchorId`, `onNavigate?`) rendering a `<nav aria-label={t("learning.units.contents")}>` with heading `t("learning.units.levelHeading", { level: current.level })`, then the current unit's subject (marked `aria-current="page"`) and, nested beneath it, `buildUnitOutline(current).entries`: `page` → `<button>` with the page title (truncate with `title` attribute holding the full text); `activity` → indented `<button>` with a `ListChecks`-style marker, the activity title, and a status icon (`Circle` for `PENDING`, `CheckCircle2` for `SUBMITTED`/`GRADED`) plus `sr-only` text `t(\`learning.homework.status.${status}\`)` — never a score; `quiz` → `<Link to="/quizzes/$quizId" params={{ quizId }}>` with a quiz marker, title, `t(\`quiz.status.${status}\`)` and `<NotificationDot label={t("notification.item")} />` when `unseen`. Page/activity buttons call `scrollToUnitAnchor(entry.anchorId)` then `onNavigate?.()`; quiz links call `onNavigate?.()` on click. When the outline has no page entries show `t("learning.units.noPages")` (muted) before any quiz entries. The entry whose `anchorId === activeAnchorId` gets highlighted styling and `aria-current="location"` (US3 supplies the value; pass `null` until then). Siblings are added in US2 — for now render only `current` (depends on T007, T008)
- [X] T010 [US1] In `front-end/src/components/learning/UnitLearningView.tsx`, for a real unit (`unitId !== null` and `group` found): widen the container from `max-w-5xl` to `max-w-7xl`, and lay out `<div className="md:flex md:items-start md:gap-8">` with `<aside className="hidden md:block w-72 shrink-0 sticky top-24 max-h-[calc(100vh-7rem)] overflow-y-auto">` containing `<UnitContentsPanel groups={groups} current={group} activeAnchorId={null} />` and a `<div className="min-w-0 flex-1 space-y-8">` holding the existing `<h1>` and `UnitPdf` / `unitEmpty` content. Keep the "Volver a Mi aprendizaje" link above, the loading skeleton and error states unchanged, and leave the "Otros" bucket (`unitId === null`) layout exactly as today (depends on T009)
- [ ] T011 [US1] Verify in the browser (dev servers per [quickstart.md](quickstart.md)) at desktop width: scenarios 1 (current-unit outline: titles, activities indented under pages 5 and 9, quizzes last), 2 (jump to page 8 right after load lands correctly), 3 (typed activity answer survives jumping away and back), 5 (status icon flips after submit), 6 (quiz entry opens `/quizzes/{id}`), 8 (no-PDF unit shows `noPages` + its quiz), 9 (renamed page title matches); check the console has no errors

**Checkpoint**: MVP — the unit page has a working table of contents on desktop.

---

## Phase 4: User Story 2 — Move to another unit of the same level (Priority: P2)

**Goal**: The panel lists all of the student's assigned units of the current level in teacher order; the current one expanded, others collapsed as links with unseen dots (FR-009–FR-012, Clarifications Q1/Q2).

**Independent Test**: [quickstart.md](quickstart.md) scenarios 1 (sibling list, no B1 unit) and 7.

- [X] T012 [P] [US2] In `front-end/src/components/learning/unitGroups.ts`, add `subject: string | null` to `UnitGroup`; set it from `unit.subject` in `ensure(...)` and to `null` for the "other" bucket
- [X] T013 [US2] Add `levelSiblings(groups: UnitGroup[], current: UnitGroup): UnitGroup[]` to `front-end/src/components/learning/unitOutline.ts`: `groups.filter(g => g.unitId !== null && g.level === current.level)` (keeps `buildGroups` order = teacher `position`) (depends on T007, T012)
- [X] T014 [US2] In `front-end/src/components/learning/UnitContentsPanel.tsx`, replace the single current-unit row with one row per `levelSiblings(groups, current)`: the current unit (matched by `unitId`) renders its subject marked `aria-current="page"` and expanded with the outline from T009; every other unit renders collapsed as `<Link to="/aprendizaje/unidad/$unitId" params={{ unitId }} onClick={onNavigate}>` showing its `subject` and `<NotificationDot label={t("notification.item")} />` when `g.unseen`. Never show the dot on the current unit (it is marked seen on open). Units of other levels and unassigned units never appear (they are not in `groups`) (depends on T013)
- [X] T015 [US2] In `front-end/src/components/learning/UnitLearningView.tsx`, make sure switching units via the panel works without a full reload: the `markUnitSeen` effect already depends on `unitId` (keep it); key the PDF area by `unitId` (e.g. `key={unitId}` on the `min-w-0 flex-1` column) so the viewer and activity state reset for the new unit; rely on router `scrollRestoration` for starting at the top and add `window.scrollTo({ top: 0 })` in a `unitId` effect only if verification shows it does not reset (depends on T010)
- [ ] T016 [US2] Verify in the browser: scenario 1 (A2 list in teacher order, no B1 unit, current marked), scenario 7 (sibling unseen dot shown, opening it starts at top with its own outline, dot cleared there and on **Mi aprendizaje**), and a level with a single assigned unit shows just that unit with no broken links

**Checkpoint**: US1 + US2 work together; a student can move through a level without returning to Mi aprendizaje.

---

## Phase 5: User Story 4 — Use the contents on a phone (Priority: P2)

**Goal**: Below 768 px the PDF is full width; a visible "Ver contenido" control opens the same panel in a left sheet that closes after a selection (FR-008, SC-005).

**Independent Test**: [quickstart.md](quickstart.md) scenario 10.

- [X] T017 [US4] In `front-end/src/components/learning/UnitLearningView.tsx`, add `const [sheetOpen, setSheetOpen] = useState(false)` and, for real units only, a `md:hidden sticky top-20 z-30` row holding a `Button variant="outline" size="sm"` (icon `ListTree` or `PanelLeft`, label `t("learning.units.openContents")`) that opens a Shadcn `Sheet` (`@/components/ui/sheet`, `side="left"`, `SheetContent` with `SheetTitle` = `t("learning.units.contents")`, `overflow-y-auto`, width `w-[85vw] max-w-sm`) containing `<UnitContentsPanel ... onNavigate={() => setSheetOpen(false)} />`. Close the sheet in the `unitId` effect too. Because `scrollToUnitAnchor` runs before the sheet's close animation finishes, confirm the target still lands correctly; if the sheet's scroll lock interferes, defer the scroll until after close (e.g. call `onNavigate` first and run the scroll on the next `requestAnimationFrame`/after `onOpenChange(false)`) (depends on T010)
- [ ] T018 [US4] Verify in the browser at 375 px (`resize_window` preset `mobile`, then reset to `desktop`): no left column, PDF full width, no horizontal scroll (`document.documentElement.scrollWidth <= innerWidth`), button visible; open sheet → tap a page and an activity → sheet closes and target scrolls into view; tap a sibling unit → new unit opens with the sheet closed

**Checkpoint**: Feature usable on phones.

---

## Phase 6: User Story 3 — Know where I am while scrolling (Priority: P3)

**Goal**: The entry for the page/activity being read is highlighted and kept visible inside the panel (FR-006, SC-004).

**Independent Test**: [quickstart.md](quickstart.md) scenario 4.

- [X] T019 [P] [US3] Create `front-end/src/components/learning/useActiveUnitAnchor.ts` per [research R4](research.md#r4--highlighting-the-entry-in-view-fr-006): `useActiveUnitAnchor(anchorIds: string[]): string | null` — on mount, `scroll` and `resize` (passive, rAF-throttled) compute the reading line `96 + 0.3 * window.innerHeight` px and return the last id in `anchorIds` whose element's `getBoundingClientRect().top <= readingLine` (default to `anchorIds[0]`, `null` if empty); also recompute when `anchorIds` changes and via a `ResizeObserver` on `document.body` (pages resize while rendering); clean up all listeners
- [X] T020 [US3] In `front-end/src/components/learning/UnitLearningView.tsx`, call `useActiveUnitAnchor(outline.anchorIds)` for the current unit (outline from `buildUnitOutline(group)`, memoised on `group`) and pass the result as `activeAnchorId` to both the desktop and the sheet `UnitContentsPanel` instances (depends on T019, T017)
- [X] T021 [US3] In `front-end/src/components/learning/UnitContentsPanel.tsx`, keep the active entry visible: give the panel's scroll container (the desktop `<aside>`, or the `SheetContent`) a ref or locate it via `closest("[data-unit-contents-scroll]")`, and in an effect on `activeAnchorId` adjust that container's `scrollTop` so the active entry is within view (do **not** call `scrollIntoView`, which can also scroll the window); add `data-unit-contents-scroll` to the aside in `UnitLearningView.tsx` and to the `SheetContent`
- [ ] T022 [US3] Verify in the browser: scenario 4 — scrolling slowly from page 2 to page 10 moves the highlight page by page, through each activity block, with no double highlight; in a 60+ page unit the panel auto-scrolls to keep the highlight visible while the window scroll is not disturbed

**Checkpoint**: All four stories complete.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T023 [P] Update `CLAUDE.md`: in the `/aprendizaje` row of **Current pages**, describe the unit page's left contents panel (level units in teacher order, current unit expanded into page titles, activities under their page with status, end quizzes as links; phone: "Ver contenido" sheet); in the **Units = a PDF + page activities** note, replace "the unit page does not show them yet" with "the unit page links them from its contents panel (`specs/056-unit-viewer-navigation/`)"
- [ ] T024 [P] Check edge cases from the spec: very long page/activity titles truncate without breaking the panel (full text in `title` attribute and above the page); "Otros materiales" (`/aprendizaje/otros`) is unchanged (quickstart scenario 11); teacher account view behaves as today (scenario 12); light and dark themes both readable
- [X] T025 Run `npm run lint && npm run format && npm run build` in `front-end/` and fix any errors or new warnings
- [ ] T026 Run the full [quickstart.md](quickstart.md) pass (scenarios 1–12) once more on the final build and take a desktop and a mobile screenshot as proof

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none — T001–T003 in parallel.
- **Foundational (Phase 2)**: T004 first; then T005 ∥ T006. Blocks all stories.
- **US1 (Phase 3)**: after Phase 2. T007 ∥ T008 → T009 → T010 → T011.
- **US2 (Phase 4)**: after US1 (extends `UnitContentsPanel` and `unitOutline.ts`). T012 can start any time after Phase 2.
- **US4 (Phase 5)**: after US1 (T010 layout). Independent of US2 — can run in parallel with Phase 4 if done by different people (different parts of `UnitLearningView.tsx`; coordinate on that file).
- **US3 (Phase 6)**: after US1; T020 also needs T017 so both panel instances receive the value. T019 can start any time after Phase 2.
- **Polish (Phase 7)**: after all stories.

### Story completion order

```text
Setup ─► Foundational ─► US1 (MVP) ─┬─► US2 ─┐
                                    ├─► US4 ─┼─► US3 ─► Polish
                                    └────────┘
```

### Parallel opportunities

- T001, T002, T003 (three locale files).
- T005, T006 (two viewer files) after T004.
- US1: T007 (`unitOutline.ts`) ∥ T008 (`unitAnchors.ts`).
- Ahead of their phases: T012 (`unitGroups.ts`) and T019 (`useActiveUnitAnchor.ts`) touch files no other open task edits.
- Polish: T023 ∥ T024.

### Parallel example: User Story 1

```text
Task: "T007 [US1] buildUnitOutline in front-end/src/components/learning/unitOutline.ts"
Task: "T008 [US1] scrollToUnitAnchor in front-end/src/lib/unitAnchors.ts"
# then sequentially: T009 (panel) → T010 (layout) → T011 (verify)
```

---

## Implementation Strategy

### MVP first (US1 only)

1. Phase 1 + Phase 2 (anchors, i18n).
2. Phase 3 (US1) → **stop and validate** with T011: desktop table of contents with jump, statuses and quiz links.
3. Demo/ship if desired — already removes most of the scrolling pain.

### Incremental delivery

1. + US2 → level navigation (T016).
2. + US4 → phone sheet (T018). US2 and US4 are both P2; either order works.
3. + US3 → scroll-spy highlight (T022).
4. Polish → docs, edge cases, lint/build, final quickstart pass.

### Notes

- No back-end, migration, or API change in any task — if one seems necessary, stop and revisit [research R1](research.md#r1--does-the-student-unit-page-already-have-the-data-it-needs).
- Never remount `ActivityViewerPrompts` / activity panels when jumping (FR-004): only scroll.
- Commit after each phase checkpoint.
