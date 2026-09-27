# Quickstart: Validating Homework Admin Search, Sort & Pagination

Front-end-only change. No backend rebuild or migration needed (see `plan.md`).

## Prerequisites

1. Back-end running (`local` profile) per repo root `CLAUDE.md`:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```
2. Front-end dev server:
   ```bash
   cd front-end && npm run dev
   ```
3. Log in as the teacher account (the one matching `app.scheduling.teacher-email`, promoted to `ADMIN` by `AdminBootstrap`).
4. Ensure more than 20 homeworks exist under **Panel → Tareas**. If fewer than 20 exist, author additional homeworks via the panel's "Create homework" flow (title only needs to be distinct enough to test search) until the count exceeds 20 — this is required to exercise pagination (User Story 3).

## Validation scenarios

Map each scenario back to `spec.md`'s acceptance scenarios / functional requirements.

1. **Search narrows the list (FR-001, US1)**
   - Type a substring of one known homework's title into the new search box.
   - Expect: only homeworks whose title contains that substring (case-insensitive) remain visible.
   - Clear the box → full list (subject to any active filters/sort/page) returns.

2. **Search combines with existing filters (FR-001a)**
   - Set the type and/or level filter to a specific value, then also enter a search term.
   - Expect: only homeworks matching *both* the filter and the search term appear.

3. **Default sort (FR-002, US2)**
   - On a fresh load (sort control untouched), confirm the most recently created homework appears first.
   - Author one new homework, return to the tab: it should now be first.

4. **Manual sort options (FR-002a, US2)**
   - Switch the sort control through: creation date ascending, title A→Z, title Z→A.
   - Expect: the list re-orders correctly for each, and re-ordering applies to the full result set (verify by checking page 2 also reflects the new order, not just page 1).

5. **Pagination (FR-003, US3)**
   - With more than 20 homeworks (and no search/filter active), confirm exactly 20 appear per page, with controls to reach further pages.
   - Confirm no page shows more than 20 items.

6. **Filter/sort/search resets to page 1 (FR-005)**
   - Navigate to page 2 or later, then change the search term, a filter, or the sort order.
   - Expect: the view returns to page 1 of the new result set (not a blank or out-of-range page).

7. **Empty results state (FR-006)**
   - Enter a search term that matches no homework title.
   - Expect: a clear "no results" message, distinct from any loading indicator.

8. **Combined filters + search + sort + pagination together (FR-004)**
   - Apply a type filter, a search term, and a non-default sort simultaneously, with enough matches to span 2+ pages.
   - Expect: page 2 shows the *next* matching-and-sorted items, not a naive continuation of the unfiltered list.

9. **Per-homework actions still work (FR-008)**
   - From page 2 (or with a search/filter active), open/expand a homework card and confirm existing actions (edit, view assignees/submissions) work exactly as before.

10. **Students unaffected (FR-007)**
    - Log in as (or check) a student account on `/aprendizaje`; confirm homework presentation there is unchanged by this feature.

## Expected outcome

All 10 scenarios pass with no full-page reloads between interactions (SC-004), and the list stays responsive throughout (SC-005) given the low-hundreds scale this feature targets.
