---

description: "Task list for feature implementation"
---

# Tasks: Admin Student View — Summary Boxes for Every Section

**Input**: Design documents from `specs/053-admin-student-view-boxes/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-components.md, quickstart.md

**Tests**: Not requested in the spec — no automated test tasks. Verification is lint/build plus in-browser checks per quickstart.md (constitution requires browser verification for UI changes).

**Organization**: Tasks are grouped by user story. All paths are relative to the repository root. The route file `front-end/src/routes/panel_.alumnos.$studentId.tsx` is touched by every story, so route tasks are never `[P]`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Translation keys used by every story (research R9).

- [X] T001 [P] Add Spanish keys under `admin.studentProfile` in front-end/src/i18n/locales/es.ts: `stats.quizzes: "Pruebas de evaluación"`, `classesUpcoming: "próximas"`, `classesPast: "pasadas"`, `quizzesToReview: "por corregir"`, `quizzesGraded: "corregidas"`, `expandClasses: "Ver clases"`, `collapseClasses: "Ocultar clases"`, `expandPresentations: "Ver presentaciones"`, `collapsePresentations: "Ocultar presentaciones"`, `expandQuizzes: "Ver pruebas"`, `collapseQuizzes: "Ocultar pruebas"`. Lower-case the existing `homeworkPending` / `homeworkSubmitted` / `homeworkCompleted` values ("pendientes", "entregadas", "completadas") so they read naturally after the number on a stacked line (they are only used by the Tareas breakdown — confirm with a grep before changing)
- [X] T002 [P] Add the same keys with English translations in front-end/src/i18n/locales/en.ts (e.g. "Assessment quizzes", "upcoming", "past", "to review", "graded", "Show classes"/"Hide classes", "Show presentations"/"Hide presentations", "Show quizzes"/"Hide quizzes"), keeping the key structure identical to es.ts; lower-case the homework breakdown labels likewise
- [X] T003 [P] Add the same keys with Romanian translations in front-end/src/i18n/locales/ro.ts (e.g. "Teste de evaluare", "viitoare", "trecute", "de corectat", "corectate", "Vezi clasele"/"Ascunde clasele", "Vezi prezentările"/"Ascunde prezentările", "Vezi testele"/"Ascunde testele"), keeping the key structure identical to es.ts; lower-case the homework breakdown labels likewise

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The shared box component and the route's expanded-state model that every story plugs into.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Create `StudentStatBox` in front-end/src/components/admin/students/StudentStatBox.tsx per contracts/ui-components.md: `<button type="button" aria-expanded>` with classes from the current Tareas/Errores boxes (`rounded-lg border bg-card p-3 text-center transition-colors hover:bg-muted/40 focus-visible:…`, `ring-2 ring-primary/30` when expanded); shows `count ?? "…"` (`text-2xl font-semibold`), the title (`text-xs text-muted-foreground`) with a `NotificationDot` (label `t("notification.row")`) beside it when `unseen`, then — if `breakdown` is given — one line per entry stacked vertically (`<p className="text-[11px] leading-tight">` with the value in `font-semibold` followed by the label in `text-muted-foreground`; allow wrapping, no truncation), then the expand/collapse hint (`text-[11px] text-muted-foreground`). Named export, JSDoc one-liner like sibling components
- [X] T005 Refactor front-end/src/routes/panel_.alumnos.$studentId.tsx: add `type StudentBoxKey = "classes" | "homework" | "presentations" | "mistakes" | "quizzes"` and `const BOX_ORDER: StudentBoxKey[]` in that order; replace `tareasExpanded` / `erroresExpanded` with a single `useState<Set<StudentBoxKey>>(new Set())` plus a `toggleBox(key)` helper that creates a new Set; update the Tareas button and the expanded homework/mistakes panels to read from the set; change the grid wrapper to `grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5`
- [X] T006 Replace the `<StudentMistakesBox …>` usage in front-end/src/routes/panel_.alumnos.$studentId.tsx with `<StudentStatBox title={t("admin.studentProfile.mistakes.title")} count={mistakes?.allTimeMistakeCount ?? null} expandLabel={t("admin.studentProfile.mistakes.expand")} collapseLabel={t("admin.studentProfile.mistakes.collapse")} …/>` wired to the `"mistakes"` key, then delete front-end/src/components/admin/students/StudentMistakesBox.tsx and its import

**Checkpoint**: Page renders as before (Tareas + Errores still toggle), now driven by the shared state; Errores box uses `StudentStatBox`.

---

## Phase 3: User Story 1 - Readable homework breakdown in the Tareas box (Priority: P1) 🎯 MVP

**Goal**: Tareas shows pendientes / entregadas / completadas on three separate lines, plus an unseen dot when any homework is unseen (FR-001, FR-009).

**Independent Test**: Open a student profile; the Tareas box lists the three statuses stacked vertically with correct counts; with an unseen submitted homework the box shows a dot.

- [X] T007 [US1] In front-end/src/routes/panel_.alumnos.$studentId.tsx replace the inline Tareas `<button>` with `<StudentStatBox>` for key `"homework"`: `count={profile.homeworks.length}`, `breakdown` built from `homeworkBreakdownFromList(profile.homeworks)` → `[{label: t("admin.studentProfile.homeworkPending"), value: pending}, {…homeworkSubmitted…}, {…homeworkCompleted…}]`, `unseen={profile.homeworks.some((h) => h.unseen)}`, expand/collapse labels `expandHomework` / `collapseHomework`
- [X] T008 [US1] Delete front-end/src/components/admin/students/StudentHomeworkBreakdown.tsx and remove its import from front-end/src/routes/panel_.alumnos.$studentId.tsx (grep first to confirm there are no other users)

**Checkpoint**: US1 complete and verifiable on its own.

---

## Phase 4: User Story 2 - Classes as an expandable box (Priority: P2)

**Goal**: Clases box with total + próximas/pasadas lines; expanding shows both class lists with the no-show toggle; standalone class sections removed (FR-005, FR-008).

**Independent Test**: Class lists hidden on load; box shows "N próximas" / "N pasadas"; expanding shows both lists; toggling a no-show saves and the panel stays open.

- [X] T009 [P] [US2] Create `StudentClassesPanel` in front-end/src/components/admin/students/StudentClassesPanel.tsx per contracts/ui-components.md (props `upcoming`, `past`: `StudentProfileBooking[]`, `teacherTimezone`, `onToggleNoShow`): move the `Section` helper and `formatSlot` function from the route into this file, and move the "Próximas clases" and "Clases pasadas" `<Section>` markup verbatim (Zoom link on upcoming rows, no-show toggle button on past rows calling `onToggleNoShow(b.id, !b.noShow, b.isCompanionStudent)`), wrapped in `<div className="space-y-6">`
- [X] T010 [US2] In front-end/src/routes/panel_.alumnos.$studentId.tsx replace the static Clases stat `<div>` with `<StudentStatBox>` for key `"classes"`: `count={upcoming.length + past.length}`, `breakdown=[{label: t("admin.studentProfile.classesUpcoming"), value: upcoming.length}, {label: t("admin.studentProfile.classesPast"), value: past.length}]`, labels `expandClasses` / `collapseClasses`
- [X] T011 [US2] In front-end/src/routes/panel_.alumnos.$studentId.tsx remove the standalone upcoming/past `<Section>` blocks and `formatSlot` (now in the panel); render `<StudentClassesPanel upcoming={upcoming} past={past} teacherTimezone={teacherTimezone} onToggleNoShow={handleToggleNoShow} />` in the expanded-panels area when `"classes"` is expanded. Restructure the expanded-panels area to iterate `BOX_ORDER` so panels always appear in box order (a `renderPanel(key)` switch returning each panel wrapped in `<div className="mt-4">`), keeping homework and mistakes panels unchanged

**Checkpoint**: US2 complete; US1 still works.

---

## Phase 5: User Story 3 - Shared presentations as an expandable box (Priority: P2)

**Goal**: Presentaciones box (total only) toggles the shared presentations list; standalone section removed (FR-006, FR-008).

**Independent Test**: List hidden on load; clicking the box shows title + level rows (or the empty state).

- [X] T012 [P] [US3] Create `StudentPresentationsPanel` in front-end/src/components/admin/students/StudentPresentationsPanel.tsx (prop `presentations: StudentProfilePresentation[]`), moving the list markup from the route's "Presentaciones compartidas" section: heading with count badge (`sharedPresentations`), title + level rows, `emptyPresentations` when empty
- [X] T013 [US3] In front-end/src/routes/panel_.alumnos.$studentId.tsx replace the static Presentaciones stat `<div>` with `<StudentStatBox>` for key `"presentations"` (`count={profile.presentations.length}`, no breakdown, labels `expandPresentations` / `collapsePresentations`), remove the standalone presentations `<Section>`, and add the `"presentations"` case to `renderPanel`

**Checkpoint**: US3 complete; US1–US2 still work.

---

## Phase 6: User Story 4 - New "Pruebas de evaluación" box (Priority: P2)

**Goal**: New box with total + por corregir/corregidas lines and an unseen dot; expanding shows the quiz list; standalone quiz section removed (FR-007, FR-008, FR-009).

**Independent Test**: With one SUBMITTED (unseen) and two GRADED attempts the box shows 3, "1 por corregir", "2 corregidas" and a dot; expanding shows the list; reviewing the unseen attempt updates counts, clears the dot and keeps the panel open.

- [X] T014 [P] [US4] Add `export interface QuizBreakdown { toReview: number; graded: number }` and `export function quizBreakdownFromList(quizzes: Pick<StudentQuizSummary, "status">[]): QuizBreakdown` (SUBMITTED → toReview, GRADED → graded) in front-end/src/lib/admin.ts, placed after `StudentQuizSummary`, with a one-line JSDoc like `homeworkBreakdownFromList`
- [X] T015 [P] [US4] Create `StudentQuizzesPanel` in front-end/src/components/admin/students/StudentQuizzesPanel.tsx (props `quizzes: StudentQuizSummary[]`, `onOpenAttempt(quizId, attemptId)`), moving the quiz list markup from the route verbatim: heading with count (`quiz.admin.studentQuizzes`), per-attempt card with title + `NotificationDot` when unseen, status · score, per-skill grid, view/review button; `quiz.admin.noStudentQuizzes` when empty
- [X] T016 [US4] In front-end/src/routes/panel_.alumnos.$studentId.tsx change `quizzes` state to `StudentQuizSummary[] | null` (initial `null`; fetch error → `[]` as today, in both the initial effect and `reloadProfile`); add `<StudentStatBox>` for key `"quizzes"` as the last box: `title={t("admin.studentProfile.stats.quizzes")}`, `count={quizzes?.length ?? null}`, `breakdown` from `quizBreakdownFromList(quizzes ?? [])` using `quizzesToReview` / `quizzesGraded` (omit breakdown while `quizzes` is null), `unseen={quizzes?.some((q) => q.unseen) ?? false}`, labels `expandQuizzes` / `collapseQuizzes`
- [X] T017 [US4] In front-end/src/routes/panel_.alumnos.$studentId.tsx remove the standalone quizzes `<Section>` (and the now-empty `space-y-10` wrapper), and add the `"quizzes"` case to `renderPanel` rendering `<StudentQuizzesPanel quizzes={quizzes ?? []} onOpenAttempt={(quizId, attemptId) => setOpenQuizAttempt({ quizId, attemptId })} />`; remove any now-unused imports (`NotificationDot` if only used in moved markup, etc.)

**Checkpoint**: All four stories complete.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T018 Run `npm run lint && npm run format` and `npm run build` in front-end/ and fix any errors (unused imports, types)
- [ ] T019 Verify in the browser per specs/053-admin-student-view-boxes/quickstart.md scenarios 1–10 (start the dev server with preview_start; check box order, stacked breakdowns, toggles, panel order, no-show toggle, quiz review clearing the dot, empty student, 375 px / 768 px / desktop widths with no horizontal scroll or truncated labels, es/en/ro labels); fix issues found
- [X] T020 [P] Update the `/panel` row of the "Current pages" table in CLAUDE.md to mention that the student profile shows five toggle boxes (Clases, Tareas, Presentaciones, Errores frecuentes, Pruebas de evaluación) with stacked breakdowns and unseen dots on Tareas/Pruebas

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; T001–T003 run in parallel.
- **Foundational (Phase 2)**: T004 → T005 → T006 (T005/T006 share the route file). Blocks all stories.
- **User Stories (Phases 3–6)**: Depend on Phase 2. They share the route file, so their route tasks run sequentially: US1 → US2 → US3 → US4 (priority order). US2's T011 introduces `renderPanel`, which US3/US4 extend.
- **Polish (Phase 7)**: After all stories.

### Within Each Story

- New component / helper tasks marked [P] (T009, T012, T014, T015) touch only their own files and can be written anytime after Phase 2 — even ahead of their story's route task.
- Route wiring tasks depend on their story's component task.

### Parallel Opportunities

```text
# Phase 1 together:
T001 es.ts   T002 en.ts   T003 ro.ts

# After Phase 2, all new files in parallel:
T009 StudentClassesPanel.tsx
T012 StudentPresentationsPanel.tsx
T014 lib/admin.ts (quizBreakdownFromList)
T015 StudentQuizzesPanel.tsx

# Then route wiring sequentially: T007 → T008 → T010 → T011 → T013 → T016 → T017
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 + Phase 2
2. Phase 3 (T007–T008) → verify the stacked Tareas breakdown in the browser → shippable.

### Incremental Delivery

1. Setup + Foundational → page unchanged, shared box in place
2. + US1 → readable Tareas breakdown
3. + US2 → Clases box, class sections gone
4. + US3 → Presentaciones box
5. + US4 → Pruebas de evaluación box; page fully box-driven
6. Polish → lint/build, browser verification, docs
