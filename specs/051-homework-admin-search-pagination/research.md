# Phase 0 Research: Homework Admin Search, Sort & Pagination

## Decision 1: Client-side vs. server-side search/sort/pagination

**Decision**: Implement search, sort, and pagination entirely client-side, computed over the full homework list the admin panel already fetches in one request via `GET /api/v1/admin/homework` (`HomeworkAdminService.list()` → `ContentRepository.findAllAssignments()`, no query params today). No backend changes.

**Rationale**:
- The clarified scale target is "low hundreds" of total homeworks (single-teacher use) — well within what a browser can filter/sort/slice instantly with `Array.filter`/`sort`/`slice`, matching SC-004 and SC-005.
- The current implementation already fetches everything and does client-side filtering (type/level/label) and sorting (alphabetical by title) with no reported performance issue — this feature extends that same pattern rather than replacing it.
- No pagination/search/sort infrastructure exists anywhere in the backend today (checked every admin controller: `ActivityAdminController`, `AudioAdminController`, `AvailabilityAdminController`, `BookingAdminController`, `HomeworkAdminController`, `ImageAdminController`, `PresentationAdminController`, `StudentAdminController`, `TestimonialAdminController` — none accept `page`/`size`/`search`/`sortBy` params, no `Page<T>`/`PageResponse<T>` DTO exists). Building that generic infrastructure for one low-scale internal list is premature per the project's Simplicity First / YAGNI constitution principle.
- FR-004 (all active filters + sort computed together before pagination) is trivially satisfied by a single `useMemo` pipeline (filter → sort → paginate) over one already-fetched array — no risk of the "page of unfiltered list" bug the requirement guards against.

**Alternatives considered**:
- **Server-side pagination** (new `page`/`size`/`search`/`sortBy`/`sortDir` query params on `GET /api/v1/admin/homework`, dynamic SQL in `ContentRepository`, a new `PageResponse<HomeworkAdminItem>` DTO): rejected as premature for the current and clarified expected scale. It's the natural next step if the homework count grows well beyond "low hundreds" or payload size becomes a real concern — at that point only `getHomework()` and the list's query-building logic would need to change, per the Evolution-Ready Architecture principle; the card rendering and per-item actions are unaffected either way.
- **Hybrid** (server-side search, client-side sort/pagination, or vice versa): rejected — splitting the filtering pipeline across two layers adds complexity without a concrete present need, and risks exactly the "page of a partially filtered list" bug FR-004 rules out.

## Decision 2: Where search/sort/page state lives

**Decision**: Local component state (`useState`) inside `HomeworkAdminList.tsx`, alongside the existing `filterType`/`filterLevel`/`filterLabel` state. Not reflected in the URL.

**Rationale**: The project already has one precedent for URL-driven state — `front-end/src/routes/panel.tsx` uses TanStack Router's `validateSearch`/`useSearch` for the active admin tab (`?tab=...`). Extending that shared route-level search schema with homework-list-specific keys (search term, sort, page) would couple an internal list's UI state to a route consumed by multiple unrelated admin tabs (Units, Presentations, Quizzes, Testimonios), for a requirement that never asks for shareable/bookmarkable list state. Local state keeps the change contained to the one component that owns it.

**Alternatives considered**:
- **URL search params on the shared `/panel` route**: rejected — no requirement asks for bookmarkable/shareable search-sort-page state, and it would risk key collisions or unnecessary re-renders across other admin tabs sharing the same route search schema.

## Decision 3: Debounced search input

**Decision**: Add a small, single-purpose `useDebouncedValue` hook under `front-end/src/hooks/`, used only by the new search input.

**Rationale**: No debounce hook exists anywhere in `front-end/src` today. Debouncing the search term (e.g., ~250ms) avoids re-running the filter/sort/paginate pipeline on every keystroke; at low-hundreds scale this is a UX-smoothness nicety rather than a strict performance necessity, but it's a small, isolated, reusable primitive consistent with Component-Driven UI (no inline debounce logic in the list component itself).

**Alternatives considered**:
- **No debouncing** (filter on every keystroke): rejected only because it's just as easy to debounce and avoids visible list "flicker" on fast typing; not a hard requirement.
- **Pulling in a debounce library** (e.g., lodash.debounce): rejected — a ~10-line hook is simpler than adding a dependency for one use site (Simplicity First).

## Decision 4: Sort implementation for title ordering

**Decision**: Reuse the existing `title.localeCompare(otherTitle, "es", { sensitivity: "base" })`-style comparator (already used today for the default alphabetical sort) for both "title A→Z" and "title Z→A" (reversed), and compare `Date` values (or ISO timestamp strings) directly for the two creation-date orders.

**Rationale**: The current code already sorts by title with Spanish-locale-aware comparison; reusing it preserves existing behavior exactly for the two title-based options instead of introducing a second, subtly different comparator.

**Alternatives considered**:
- **Plain string comparison** (`<`/`>`) for title sort: rejected — would regress the existing Spanish-locale-aware ordering (e.g., accented characters) that `localeCompare("es", ...)` already provides.

## Open questions

None remaining — all NEEDS CLARIFICATION items from the Technical Context were resolved above or were already resolved in the spec's Clarifications section.
