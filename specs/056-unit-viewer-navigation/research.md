# Research: Unit Viewer Contents & Level Navigation

**Feature**: `056-unit-viewer-navigation` | **Date**: 2026-10-10

No `NEEDS CLARIFICATION` remained after `/speckit-clarify`. The items below record the design decisions taken while grounding the spec in the existing code.

## R1 — Does the student unit page already have the data it needs?

- **Decision**: Yes. No back-end change; the panel is built entirely from the existing `GET /api/v1/learning` response (`LearningResponse`) that `UnitLearningView` already loads.
- **Rationale**: `LearningService` already returns, per student:
  - `sharedPresentations[]` — each unit-owned PDF with `unit` (`id`, `level`, `subject`, `position`, `unseen`), `activities[]` (`id`, `title`, `page`, `status`) and `pageTitles[]` (one per page, index 0 = page 1).
  - `unitQuizzes[]` — each quiz linked to the end of one of the student's units with `unit`, `status` (`AVAILABLE`/`IN_PROGRESS`/`SUBMITTED`/`GRADED`) and `unseen`, ordered by `u.level, u.position, q.unit_position` (`QuizRepository.findUnitQuizzesForUser`), i.e. already in the teacher's order.
  - Both lists are scoped to `unit_assignments` for the student, so FR-011 (assigned units only, Clarification Q1) holds by construction.
  - `buildGroups()` (`components/learning/unitGroups.ts`) already groups them per unit and sorts by `level`, then `position` — exactly the level-navigation order.
- **Alternatives considered**: a dedicated `GET /learning/units/{id}/outline` endpoint — rejected (Simplicity First): it would duplicate data the page already has.
- **Consequence**: the level list shows the same units as **Mi aprendizaje** (a unit assigned but with neither a PDF nor quizzes appears in neither place — consistent with today).

## R2 — Layout: desktop sidebar + phone sheet

- **Decision**: `UnitLearningView` becomes a two-column layout from the `md` breakpoint (768 px, same as `useIsMobile`): a left `<aside>` (`w-72`, `sticky top-24`, `max-h-[calc(100vh-7rem)]`, own `overflow-y-auto`) and the PDF column (`min-w-0 flex-1`). The page container widens from `max-w-5xl` to `max-w-7xl` so the PDF keeps roughly its current width. Below `md`, the aside is hidden and a sticky "Contenido" button opens the same panel in the existing Shadcn `Sheet` (`side="left"`); selecting an entry closes the sheet, then scrolls.
- **Rationale**: one `UnitContentsPanel` component rendered in two containers satisfies FR-007/FR-008 and Clarification Q2 (single panel: level units + current unit expanded). Switching with Tailwind classes (`hidden md:block` / `md:hidden`) rather than `useIsMobile()` avoids a first-render flash and SSR mismatch. `top-24`/`scroll-mt-24` follows the existing precedent in `UnitDetailContent.tsx` for clearing the sticky `SiteHeader`.
- **Alternatives considered**: Shadcn `Sidebar` primitive — rejected: built for app-shell navigation (provider, cookie state, keyboard shortcut), far more than needed; a collapsible desktop panel — deferred (outstanding low-impact item from clarify; not required by any FR).

## R3 — Jumping to a page / activity (FR-004)

- **Decision**: Add stable DOM anchors in the viewer: the per-page wrapper in `PdfPageStack` gets `id="unit-page-{n}"`, and `ActivityPageSlot`'s `<section>` gets `id="unit-activity-{activityId}"`; both get `scroll-mt-24`. A small helper `scrollToUnitAnchor(id)` (new `lib/unitAnchors.ts`) waits for the element to exist (PDF may still be loading), calls `scrollIntoView({ block: "start", behavior: "smooth" })`, then re-aligns once with `behavior: "auto"` if the target's top drifts more than 8 px while earlier pages finish sizing — for up to ~1.5 s, and aborts on user `wheel`/`touchstart`/`keydown`.
- **Rationale**: `PdfSinglePage` sets each page's height only after `pdf.getPage()` resolves, so earlier pages can still grow after the jump (spec edge case "pages still rendering"). The bounded re-align fixes that without pre-measuring every page. Plain anchors keep in-progress activity answers intact (no navigation, no remount — FR-004).
- **Alternatives considered**: URL hash (`#page-8`) with router hash scrolling — rejected: TanStack Router's hash scrolling fires before lazy PDF layout settles and would add history entries per click; pre-sizing all pages from page 1's aspect ratio — rejected: PDFs can mix page sizes, and it changes shared viewer code for little gain.

## R4 — Highlighting the entry in view (FR-006)

- **Decision**: A `useActiveUnitAnchor(anchorIds)` hook: on `scroll`/`resize` (rAF-throttled, passive), pick the last anchor whose `getBoundingClientRect().top` is above a reading line at `header offset + 30 %` of the viewport height; default to the first anchor. Anchors include pages and activities, so the activity entry is highlighted while its block is the item being read. The panel keeps the active entry visible by adjusting its own `scrollTop` (never `scrollIntoView`, which could also scroll the window).
- **Rationale**: the existing `onPageVisible` (IntersectionObserver at ≥ 45 % of the page) only covers pages, misses activity blocks, and can report nothing for tall pages on short viewports. A reading-line scroll spy is deterministic and meets SC-004 (≥ 95 % match).
- **Alternatives considered**: reusing `onPageVisible` — rejected for the reasons above; one IntersectionObserver per anchor with `rootMargin` — viable but harder to reason about with mixed heights; no measurable benefit at ≤ ~100 anchors.

## R5 — Level navigation & unseen state (FR-009 – FR-012)

- **Decision**: Level units = `buildGroups(...)` filtered to `unitId !== null && level === current.level` (already sorted). Each other unit is a `<Link to="/aprendizaje/unidad/$unitId">` showing its **subject** (the panel heading shows "Nivel A2", so repeating the level per row is redundant) and a `NotificationDot` when `group.unseen`. The current unit's dot is suppressed locally because `UnitLearningView` already calls `markUnitSeen(unitId)` on open. `UnitGroup` gains a `subject` field.
- **Rationale**: same source and same unseen semantics as **Mi aprendizaje**; opening another unit runs the existing `markUnitSeen` effect (keyed on `unitId`), so FR-012 needs no new code path. `createRouter({ scrollRestoration: true })` starts the new unit at the top (FR-010); the PDF viewer remounts because it is keyed by presentation id.
- **Alternatives considered**: label "A2 · Subject" per row (as on the cards) — rejected as redundant under the level heading; the spec's Assumption is updated to say "labelled by subject under a level heading".

## R6 — Status markers (FR-003, FR-004a)

- **Decision**: Activity entries show an icon + screen-reader text from the existing keys `learning.homework.status.{PENDING|SUBMITTED|GRADED}`: `Circle` for `PENDING`, `CheckCircle2` for `SUBMITTED`/`GRADED` (no score, no grade wording — the activity block itself hides its badge once `GRADED`). Quiz entries show their status text from `quiz.status.{…}` (same as `AssignedQuizList`) and a `NotificationDot` when `unseen`; they link to `/quizzes/$quizId`.
- **Rationale**: reuses existing vocabulary/i18n; activities intentionally never expose grading to students (spec 055).
- **Status freshness**: `ActivityViewerPrompts` already calls `onActivitiesChanged` → `UnitLearningView.reload()` after a submission, which refetches `LearningResponse`, so the panel status updates without extra wiring.

## R7 — Testing approach

- **Decision**: No automated front-end tests (the front-end has no test runner configured); verification follows `quickstart.md` in a running browser (constitution: UI changes must be verified in a browser). `npm run build` + `npm run lint` must pass. No back-end change → no new integration tests.
