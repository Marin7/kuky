# Feature Specification: Homework Admin Search, Sort & Pagination

**Feature Branch**: `051-homework-admin-search-pagination`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "The Admin panel view of the homeworks has become very difficult to manage. I want to add a search option, I want them to be sorted by default chronologically (DESC) and we should also implement a pagination system with a view of max 20 homeworks"

## Clarifications

### Session 2026-09-27

- Q: How should the new search box interact with the Homework tab's existing type/level/label filters? → A: Search works alongside the existing filters — all active filters (type, level, label, search) combine (AND) to narrow the list.
- Q: Should the Homework tab support switching between sort orders, or is chronological-DESC the only order? → A: Add a full sort control with two sortable fields — creation date (ascending/descending) and title (ascending/descending, alphabetical) — defaulting to creation date descending.
- Q: What scale of total homeworks should this feature be designed to handle well? → A: Up to a few hundred (matches a single-teacher tutoring business's realistic growth).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find a homework quickly by name (Priority: P1)

Paula opens the Homework tab in the admin panel and types part of a homework's title into a search box. The list narrows to only the homeworks whose title matches what she typed, so she can find the one she's looking for without scrolling through the entire list.

**Why this priority**: This is the core pain point described — the list has grown too large to scan manually. Search delivers the most direct relief and is usable on its own even before sorting/pagination changes are visible.

**Independent Test**: Can be fully tested by opening the Homework tab, typing a known homework title (or part of it) into the search box, and confirming only matching homeworks are shown; clearing the box restores the full list.

**Acceptance Scenarios**:

1. **Given** the Homework tab is open with many homeworks listed, **When** Paula types a substring that matches one homework's title, **Then** only that homework (and any others matching the substring) appears in the list.
2. **Given** a search term is entered, **When** Paula clears the search box, **Then** the full homework list reappears, sorted and paginated as normal.
3. **Given** a search term that matches no homework title, **When** the search is applied, **Then** the list shows an empty-state message rather than an error or stale results.

---

### User Story 2 - See the newest homeworks first by default, or re-sort as needed (Priority: P2)

When Paula opens the Homework tab without touching any sort control, the homeworks are already ordered with the most recently created one at the top, so she doesn't have to search or scroll to find what she just authored. If she prefers a different order, she can switch to sort by title (A→Z or Z→A) or by creation date ascending, and the list re-orders accordingly.

**Why this priority**: Default ordering is a quick, low-risk win that makes the list immediately more useful, and it's a prerequisite for pagination to feel predictable (newest items land on page 1). The additional manual sort options preserve the flexibility Paula had before (alphabetical browsing) while adding the new chronological default.

**Independent Test**: Can be fully tested by opening the Homework tab fresh (no sort control touched) and confirming the first homework shown is the most recently created one, with each subsequent one older than the last; then switching the sort control to each other option and confirming the list re-orders as expected.

**Acceptance Scenarios**:

1. **Given** the sort control has not been touched, **When** the Homework tab loads, **Then** homeworks are listed from most recently created to least recently created (creation date descending).
2. **Given** Paula authors a new homework, **When** she returns to the Homework tab with the default sort still active, **Then** the newly created homework appears at the top of the default list.
3. **Given** the Homework tab is open, **When** Paula selects "title, A→Z", "title, Z→A", or "creation date, oldest first" from the sort control, **Then** the list re-orders accordingly and stays on that order until she changes it again or reloads the tab.

---

### User Story 3 - Browse the list in manageable pages (Priority: P3)

Instead of one long scrolling list, Paula sees at most 20 homeworks at a time and can move to the next or previous page to see more, so the tab stays fast and easy to scan.

**Why this priority**: Builds on search and sort — pagination matters most once the underlying result set is already narrowed and ordered sensibly. It's the most implementation-involved piece and depends on the list rendering behavior established by P1/P2.

**Independent Test**: Can be fully tested by seeding more than 20 homeworks, opening the Homework tab, and confirming exactly 20 are shown with controls to reach subsequent pages, each also capped at 20.

**Acceptance Scenarios**:

1. **Given** more than 20 homeworks exist, **When** Paula opens the Homework tab, **Then** only the first 20 (per the default sort) are shown along with a way to navigate to more.
2. **Given** Paula is on a page beyond the first, **When** she enters or changes a search term, **Then** the list returns to the first page of matching results.
3. **Given** 20 or fewer homeworks exist (or match the current search), **When** the Homework tab loads, **Then** no pagination controls are needed/shown beyond a single page.

---

### Edge Cases

- What happens when a search term matches zero homeworks? The list must show a clear "no results" state, not an empty-looking error or a frozen previous list.
- What happens when Paula is on page 3 and a search or sort change reduces the result count below what page 3 would show? The view must reset to page 1 rather than showing a blank page.
- What happens if a homework is created or deleted while Paula is browsing a page other than the first? Minor shifting of which items land on which page is acceptable; the system does not need to preserve Paula's exact scroll position across such changes.
- How does search handle leading/trailing whitespace or case differences? Matching must be case-insensitive and ignore incidental whitespace.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The admin Homework tab MUST provide a search input that filters the homework list by matching text against the homework's title, case-insensitively and on partial (substring) matches.
- **FR-001a**: The search input MUST combine (logical AND) with the tab's existing type, level, and label filters, so the visible list reflects all currently active filters together, not search alone or the existing filters alone.
- **FR-002**: The admin Homework tab MUST, by default (no sort control touched), order homeworks chronologically by creation time, most recent first.
- **FR-002a**: The admin Homework tab MUST provide a sort control offering: creation date descending (the default), creation date ascending, title ascending (A→Z), and title descending (Z→A). Selecting an option MUST re-order the full result set (not just the current page).
- **FR-003**: The admin Homework tab MUST display homeworks in pages of at most 20 items, with controls for the teacher to move between pages.
- **FR-004**: The system MUST apply all active filters (search, type, level, label) and the currently selected sort together, i.e. the paginated result set is computed from the fully filtered-and-sorted list, not a page of a partially filtered list followed by further client-side narrowing.
- **FR-005**: When a new search term is entered, an existing filter (type/level/label) is changed, the sort order is changed, or the search is cleared, the view MUST return to the first page of the (new) result set.
- **FR-006**: When the filtered result set contains no homeworks, the tab MUST display an explicit empty-results indicator distinct from the loading state.
- **FR-007**: The search, sort, and pagination behavior MUST apply only to the teacher-facing admin Homework tab list and MUST NOT change how homeworks are presented to students on `/aprendizaje`.
- **FR-008**: Existing per-homework admin actions available today (e.g., opening/editing a homework, viewing submissions) MUST remain reachable in the same way regardless of which page or search state the list is currently showing.

### Key Entities

- **Homework (admin list item)**: The existing homework record as shown in the admin panel; relevant attributes for this feature are its title (used for search matching) and creation timestamp (used for default ordering). No new attributes are introduced.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Paula can locate a specific homework by title in under 10 seconds using search, regardless of total homework count.
- **SC-002**: On opening the Homework tab with the default sort still active, the most recently created homework is visible within the first page without any additional navigation.
- **SC-003**: No single Homework tab view (page load or page navigation) presents more than 20 homeworks at once.
- **SC-004**: Changing the search term or navigating between pages updates the visible list without a full page reload perceptible to the teacher.
- **SC-005**: Search, sort, and pagination remain responsive (results appear without a noticeable delay) with a total homework count in the low hundreds.

## Assumptions

- "Search" matches against the homework's title only; matching against other fields (e.g., assigned unit name, student names) is out of scope for this feature and can be considered separately if requested.
- "Chronologically (DESC)" refers to the homework's creation time, not its due date or last-modified time.
- The 20-per-page cap is fixed for this feature (not a teacher-configurable page size).
- This feature covers only the admin Homework tab's list view; the Units, Presentations, Quizzes, and Testimonios tabs are unaffected.
- No new data is being collected or retained about the teacher's searches (e.g., no search history is persisted).
- The total homework count is expected to stay in the low hundreds for the foreseeable future (single-teacher use); the feature is not required to be designed for indefinitely growing scale beyond that.
