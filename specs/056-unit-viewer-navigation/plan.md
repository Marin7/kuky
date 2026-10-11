# Implementation Plan: Unit Viewer Contents & Level Navigation

**Branch**: `056-unit-viewer-navigation` | **Date**: 2026-10-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/056-unit-viewer-navigation/spec.md`

## Summary

Add a left-hand panel to the student unit page (`/aprendizaje/unidad/$unitId`) that lists the student's assigned units of the current level (teacher order) with the open unit expanded into a table of contents: every page title, each activity indented under its page (with status), and the unit's end quizzes as links. Selecting a page/activity scrolls the stacked PDF viewer to it; the entry being read is highlighted; other units open directly. Desktop: sticky sidebar. Phone: a "Ver contenido" button opening the same panel in a left sheet.

**Front-end only.** The existing `GET /api/v1/learning` response already carries page titles, activities with status, unit-end quizzes with status/unseen, and every assigned unit's level/position/unseen ([research R1](research.md#r1--does-the-student-unit-page-already-have-the-data-it-needs)). No migration, no endpoint, no back-end code.

## Technical Context

**Language/Version**: TypeScript 5 (strict), React 19

**Primary Dependencies**: TanStack Start/Router 1.x, TailwindCSS 4, Shadcn UI (`Sheet`, `Button`), `lucide-react` icons, `react-i18next`, pdf.js (existing viewer)

**Storage**: N/A (no persistence change; reads existing `LearningResponse`)

**Testing**: No front-end test runner; `npm run lint` + `npm run build` and browser verification per [quickstart.md](quickstart.md) (constitution requirement)

**Target Platform**: Modern desktop and mobile browsers (SSR via TanStack Start; the panel is rendered client-side after `getLearning()`)

**Project Type**: Web application (React front-end + Spring Boot API; only `front-end/` changes)

**Performance Goals**: No added network requests; scroll-spy work rAF-throttled (one `getBoundingClientRect` pass per frame over ≤ ~100 anchors); first page readable no later than today (SC-006)

**Constraints**: No horizontal scroll at 375 px (SC-005); must not remount the PDF viewer or activity forms when jumping (FR-004); sticky offsets clear the sticky `SiteHeader` (`top-24` / `scroll-mt-24` precedent)

**Scale/Scope**: Units up to ~60+ pages, a handful of units per level; 3 new small modules, 1 new component, edits to 5 existing files, i18n in 3 locales

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment | Status |
|-----------|------------|--------|
| **I. Simplicity First** | No new endpoint, table, dependency or abstraction layer; reuses `buildGroups`, `Sheet`, existing i18n status keys, existing `markUnitSeen`. Scroll spy and anchor helper are small, single-purpose. Rejected: dedicated outline endpoint, Shadcn `Sidebar` shell, URL-hash navigation. | ✅ Pass |
| **II. Component-Driven UI** | `UnitContentsPanel` is a named component reused in both desktop aside and phone sheet; built with Tailwind + Shadcn. Scrolling uses `scrollIntoView` / `scrollTop` on refs/ids — standard DOM APIs for scroll position, not DOM mutation (same pattern already used in `HomeworkAdminList` and `PdfThumbnailStrip`). | ✅ Pass |
| **III. Evolution-Ready Architecture** | Data still comes only through `lib/learning.ts` (`getLearning`); outline derivation is a pure module (`unitOutline.ts`), not inlined in JSX. | ✅ Pass |
| Workflow: browser verification | [quickstart.md](quickstart.md) defines 12 browser scenarios incl. phone width. | ✅ Planned |
| Workflow: no dead code | `onPageVisible` remains in use by other viewer callers; nothing removed is left commented out. | ✅ Pass |

**Post-design re-check (after Phase 1)**: unchanged — all gates pass; no Complexity Tracking entries.

## Project Structure

### Documentation (this feature)

```text
specs/056-unit-viewer-navigation/
├── plan.md              # This file
├── research.md          # Phase 0: design decisions R1–R7
├── data-model.md        # Phase 1: client-side view model (no persistence change)
├── quickstart.md        # Phase 1: browser verification scenarios
├── contracts/
│   └── ui-contract.md   # Phase 1: DOM anchors, helpers, panel props, i18n keys
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
front-end/src/
├── lib/
│   └── unitAnchors.ts                         # NEW: anchor ids + scrollToUnitAnchor (wait, smooth scroll, bounded re-align)
├── components/learning/
│   ├── unitGroups.ts                          # EDIT: add `subject` to UnitGroup
│   ├── unitOutline.ts                         # NEW: buildUnitOutline(group), levelSiblings(groups, current)
│   ├── useActiveUnitAnchor.ts                 # NEW: reading-line scroll spy
│   ├── UnitContentsPanel.tsx                  # NEW: level units + expanded current-unit outline + quiz links
│   ├── UnitLearningView.tsx                   # EDIT: two-column layout (md+), phone Sheet + "Ver contenido" button
│   ├── PresentationPdfViewerClient.tsx        # EDIT: PdfPageStack page wrapper gets id="unit-page-{n}" + scroll-mt-24
│   └── ActivityPageSlot.tsx                   # EDIT: <section> gets id="unit-activity-{id}" + scroll-mt-24
└── i18n/locales/{es,en,ro}.ts                 # EDIT: learning.units.{contents,openContents,levelHeading,noPages,activityLabel,quizLabel}

CLAUDE.md                                      # EDIT (at implement time): /aprendizaje row + Units note ("the unit page does not show them yet" → quizzes linked from the contents panel)
```

**Structure Decision**: Web application; changes are confined to `front-end/src` (learning components, one `lib` helper, locales). `back-end/` is untouched.

### Key design points

1. **Single panel, two containers** — `UnitLearningView` renders `<aside className="hidden md:block sticky top-24 ...">` with `UnitContentsPanel`, and a `md:hidden` sticky button that opens a `Sheet side="left"` containing the same component (`onNavigate` closes it). Container widens to `max-w-7xl`. ([R2](research.md#r2--layout-desktop-sidebar--phone-sheet))
2. **Anchors + jump** — ids on page wrappers and activity sections; `scrollToUnitAnchor` waits for mount, smooth-scrolls, and re-aligns once while earlier pages finish sizing. Activity forms are never remounted. ([R3](research.md#r3--jumping-to-a-page--activity-fr-004))
3. **Scroll spy** — `useActiveUnitAnchor(outline.anchorIds)` drives the highlight; the panel adjusts its own `scrollTop` to keep the active entry visible. ([R4](research.md#r4--highlighting-the-entry-in-view-fr-006))
4. **Level siblings** — `levelSiblings(buildGroups(...), current)`; non-current rows are `Link`s with `NotificationDot`; the current unit's dot is suppressed (already marked seen on open). Unit switching relies on existing router scroll restoration and the existing `markUnitSeen` effect. ([R5](research.md#r5--level-navigation--unseen-state-fr-009--fr-012))
5. **Status** — activity: icon + sr-only `learning.homework.status.*`, never a score; quiz: `quiz.status.*` + unseen dot, link to `/quizzes/$quizId`. Panel status refreshes through the existing `reload()` after submission. ([R6](research.md#r6--status-markers-fr-003-fr-004a))

## Complexity Tracking

No constitution violations — section intentionally empty.
