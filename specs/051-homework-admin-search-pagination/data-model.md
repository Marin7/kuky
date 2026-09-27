# Phase 1 Data Model: Homework Admin Search, Sort & Pagination

No database schema changes, migrations, or backend DTO changes. This feature introduces one new client-side-only view-state shape; the underlying data entity is the existing `HomeworkAdminItem` (unchanged).

## Existing entity (reference only, unchanged)

**HomeworkAdminItem** (`back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkAdminItem.java`) — as returned today by `GET /api/v1/admin/homework`:

| Field | Type | Used by this feature for |
|---|---|---|
| `id` | UUID | React list key / navigation to per-homework actions (unchanged) |
| `title` | String | Search matching (FR-001) and title sort (FR-002a) |
| `homeworkType` | enum string | Existing type filter (now combined per FR-001a) |
| `level` | enum string | Existing level filter (now combined per FR-001a) |
| `labels` | List\<String\> | Existing label filter (now combined per FR-001a) |
| *(all other fields: `instructions`, `format`, `composition`, `questions`, `audioUrl`, `audioFileId`, `audioFileName`, `mediaSourceKind`, `assignees`, `hasUnseenSubmissions`)* | — | Unused by search/sort/pagination; rendered as before by `HomeworkAdminCard` |

Note: `HomeworkAdminItem` does not carry a `createdAt` field, even though the underlying `homework_assignments` table has one. The backend response already arrives pre-sorted `ORDER BY created_at DESC` (`ContentRepository.findAllAssignments()`), so the client-side default sort (creation date descending) can be achieved by trusting the array's incoming order without needing the timestamp itself. The "creation date ascending" option is simply that same incoming order reversed. Because of this, no DTO change is needed for this feature — see `research.md` Decision 1 and the Assumptions/edge case noted under Key Entities below.

## New client-side view state (not persisted, not sent to the backend)

**HomeworkListViewState** — local component state in `HomeworkAdminList.tsx`:

| Field | Type | Notes |
|---|---|---|
| `search` | string | Raw search box value; debounced before use (see research.md Decision 3) |
| `filterType` | `HomeworkType \| "ALL"` | Existing field, unchanged |
| `filterLevel` | `HomeworkLevel \| "ALL"` | Existing field, unchanged |
| `filterLabel` | string (`"ALL"` or a label key) | Existing field, unchanged |
| `sortBy` | `"createdAt" \| "title"` | New — which column drives ordering |
| `sortDir` | `"asc" \| "desc"` | New — direction for the selected `sortBy` |
| `page` | number (0-based) | New — current page index into the filtered-and-sorted result |

**Derived (computed, not stored)**:
- `filteredItems`: the fetched `HomeworkAdminItem[]` after applying `filterType`, `filterLevel`, `filterLabel`, and `search` (case-insensitive substring match on `title`, trimmed).
- `sortedItems`: `filteredItems` ordered per `sortBy`/`sortDir` (creation-date order = incoming array order, or its reverse; title order = `localeCompare` per research.md Decision 4).
- `pageItems`: `sortedItems.slice(page * 20, page * 20 + 20)`.
- `totalPages`: `Math.ceil(sortedItems.length / 20)`.

**Invariants / transitions**:
- Changing `search`, `filterType`, `filterLevel`, `filterLabel`, or `sortBy`/`sortDir` resets `page` to `0` (FR-005).
- `page` is clamped to `[0, max(totalPages - 1, 0)]` whenever `totalPages` shrinks below the current `page` (edge case in spec.md).
- Default state on mount: `search = ""`, all filters `"ALL"`, `sortBy = "createdAt"`, `sortDir = "desc"`, `page = 0`.
