# Data Model: Unit Viewer Contents & Level Navigation

**Feature**: `056-unit-viewer-navigation` | **Date**: 2026-10-10

**No persistence changes.** No migration, no new table/column, no API change. Everything below is a **client-side view model** derived from the existing `LearningResponse` (`front-end/src/lib/learning.ts`).

## Source data (existing, unchanged)

| Type | Fields used | Notes |
|------|-------------|-------|
| `UnitRef` | `id`, `level`, `subject`, `position`, `unseen` | Level is `A1`–`C2`; `position` orders units within a level. |
| `SharedPresentationSummary` | `id`, `files[]`, `unit`, `activities[]`, `pageTitles[]` | Unit-owned PDF; `pageTitles[i]` is page `i + 1`. |
| `ActivitySummary` | `id`, `title`, `page`, `status` | `status ∈ PENDING \| SUBMITTED \| GRADED`; at most one per page. |
| `UnitQuizSummary` | `id`, `title`, `unit`, `status`, `unseen` | `status ∈ AVAILABLE \| IN_PROGRESS \| SUBMITTED \| GRADED`; already in teacher order. |
| `UnitGroup` (`unitGroups.ts`) | `unitId`, `level`, `label`, `position`, `unseen`, `presentations`, `quizzes` | **Change**: add `subject: string \| null` (null for the "Otros" bucket). |

## Derived view model (new, front-end only)

### `UnitOutline` — the current unit's contents (built by `buildUnitOutline(group)`)

```text
UnitOutline
├── entries: OutlineEntry[]        // in reading order
└── anchorIds: string[]            // entries' anchor ids, same order (for the scroll spy)

OutlineEntry =
  | { kind: "page",     page: number, title: string,                         anchorId: "unit-page-{page}" }
  | { kind: "activity", page: number, activityId: string, title: string,
      status: ActivitySummary["status"],                                     anchorId: "unit-activity-{activityId}" }
  | { kind: "quiz",     quizId: string, title: string,
      status: UnitQuizSummary["status"], unseen: boolean }                   // a link, no anchor
```

**Rules**

- Page entries: one per `pageTitles[i]`, `page = i + 1`, in ascending page order (FR-001). Page count = `pageTitles.length` (synced with the PDF on every upload).
- Activity entry: inserted **immediately after** its page entry (Clarification Q3, FR-002). An activity whose `page` exceeds `pageTitles.length` cannot occur (409 `ACTIVITIES_OUT_OF_RANGE` on upload) and is ignored defensively.
- Quiz entries: appended after the last page/activity, in `group.quizzes` order (FR-004a). Present even when the unit has no PDF.
- Titles come from the same fields the viewer renders (`pageTitles`, `activity.title`), so FR-005 holds by construction.
- A unit has at most one PDF; if `group.presentations` were ever > 1, only the first PDF is outlined (matching what is viewable).

### `LevelNavigation` — sibling units

```text
LevelNavigation
├── level: string                  // e.g. "A2" (panel heading "Nivel A2")
└── units: { unitId, subject, unseen, isCurrent }[]   // teacher order
```

**Rules**

- `units` = `buildGroups(...)` filtered to `unitId !== null && level === current.level` (already sorted by `position`) (FR-009, FR-011).
- `isCurrent` unit renders expanded with the `UnitOutline`; others render collapsed as links (Clarification Q2).
- `unseen` is shown for non-current units only; the current unit is marked seen on open (FR-012).

### UI state (component-local)

| State | Owner | Lifecycle |
|-------|-------|-----------|
| `activeAnchorId` | `useActiveUnitAnchor` | Recomputed on scroll/resize; initial = first anchor. |
| `sheetOpen` | `UnitLearningView` (phone) | `false` on load; set `false` after an entry is selected or the unit changes. |
