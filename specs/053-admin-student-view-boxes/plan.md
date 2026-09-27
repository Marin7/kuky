# Implementation Plan: Admin Student View — Summary Boxes for Every Section

**Branch**: `053-admin-student-view-boxes` | **Date**: 2026-09-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/053-admin-student-view-boxes/spec.md`

## Summary

Front-end-only restructuring of the admin student profile (`/panel/alumnos/$studentId`).
Every section becomes a summary box that toggles its detail panel, following the existing
Tareas / Errores pattern. A single `StudentStatBox` component renders all five boxes
(Clases · Tareas · Presentaciones · Errores frecuentes · Pruebas de evaluación) with an
optional stacked breakdown (Tareas: pendientes/entregadas/completadas; Clases:
próximas/pasadas; Pruebas: por corregir/corregidas) and an optional unseen dot (Tareas,
Pruebas). The standalone class, presentation and quiz sections move into named panel
components shown only when their box is expanded. No back-end or API changes.

## Technical Context

**Language/Version**: TypeScript 5 (strict), React 19

**Primary Dependencies**: TanStack Start / Router, TailwindCSS 4, react-i18next, existing `NotificationDot`

**Storage**: N/A (no persisted changes)

**Testing**: `npm run lint`, `npm run build`, manual browser verification per [quickstart.md](./quickstart.md) (constitution requires in-browser verification of UI changes)

**Target Platform**: Web (SSR), desktop + mobile browsers

**Project Type**: Web application (front-end only for this feature)

**Performance Goals**: No new requests; derived counts are O(n) over lists already in memory

**Constraints**: No horizontal scroll at 375 px; labels must wrap, not truncate; es/en/ro translations

**Scale/Scope**: 1 route file, 4 new components, 2 removed components, 1 helper, 3 locale files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment |
|---|---|
| I. Simplicity First | ✅ No new dependencies, no back-end work. One generic box component replaces two near-duplicate implementations rather than adding three more. |
| II. Component-Driven UI | ✅ Every box and panel is a named component (`StudentStatBox`, `StudentClassesPanel`, `StudentPresentationsPanel`, `StudentQuizzesPanel`). Tailwind only. |
| III. Evolution-Ready Architecture | ✅ Breakdown derivation lives in `lib/admin.ts` (`quizBreakdownFromList` beside `homeworkBreakdownFromList`); fetching stays in existing `lib/admin.ts` calls. |
| Workflow: browser verification | ✅ Planned via quickstart scenarios 1–10. |
| Workflow: no dead code | ✅ `StudentMistakesBox` and `StudentHomeworkBreakdown` are deleted once unused. |

**Post-design re-check**: ✅ unchanged — design adds no abstractions beyond the single shared box.

## Project Structure

### Documentation (this feature)

```text
specs/053-admin-student-view-boxes/
├── plan.md              # This file
├── research.md          # Phase 0 decisions
├── data-model.md        # View models (no persisted changes)
├── quickstart.md        # Validation scenarios
├── contracts/
│   └── ui-components.md # Component props contract
└── tasks.md             # /speckit-tasks output (not created here)
```

### Source Code (repository root)

```text
front-end/src/
├── routes/
│   └── panel_.alumnos.$studentId.tsx          # MODIFY: box grid, expanded-set state, panel order, quizzes null-while-loading
├── components/admin/students/
│   ├── StudentStatBox.tsx                     # NEW: generic toggle box (count, breakdown lines, unseen dot)
│   ├── StudentClassesPanel.tsx                # NEW: upcoming + past lists (moved), no-show toggle, Section helper
│   ├── StudentPresentationsPanel.tsx          # NEW: shared presentations list (moved)
│   ├── StudentQuizzesPanel.tsx                # NEW: quiz attempts list (moved)
│   ├── StudentMistakesBox.tsx                 # DELETE (replaced by StudentStatBox)
│   └── StudentHomeworkBreakdown.tsx           # DELETE (breakdown rendered by StudentStatBox)
├── lib/
│   └── admin.ts                               # ADD: QuizBreakdown + quizBreakdownFromList
└── i18n/locales/
    ├── es.ts                                  # ADD keys (see research R9)
    ├── en.ts                                  # ADD keys
    └── ro.ts                                  # ADD keys
```

**Structure Decision**: Existing web-app layout; all changes under `front-end/src`. Back-end untouched.

## Implementation Notes

- **Box order / panel order**: a single `BOX_ORDER: StudentBoxKey[]` constant; panels render
  by iterating it and showing those in the expanded set (FR-002, FR-010).
- **Expanded state** is independent of data; `reloadProfile()` only refetches, so open
  boxes stay open after reviews / no-show toggles (FR-011). `handleToggleNoShow` already
  refetches only the profile — keep it that way.
- **Grid**: `grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5`; box padding `p-3`
  (research R8).
- **Quizzes state**: `StudentQuizSummary[] | null`; `null` → box count `"…"`; fetch error
  → `[]` (today's behaviour).
- **Homework list** markup stays inline in the route; only the Tareas box rendering changes.
- **Errores** list (`StudentMistakesList`) and its data flow are unchanged; only its box
  switches to `StudentStatBox`.

## Complexity Tracking

No constitution violations.
