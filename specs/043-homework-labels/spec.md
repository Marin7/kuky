# Feature Specification: Homework Labels

**Feature Branch**: `043-homework-labels`

**Created**: 2026-08-16

**Status**: Draft

**Input**: User description: "Introduce the concept of labels. A teacher can add a label to a homework; it can be any string, but not too long and it will help filter on the homeworks tab which is getting very big at the moment"

## Clarifications

### Session 2026-08-16

- Q: Should labels that differ only by capitalization count as the same? → A: Ignore capitalization. They count as one label for filtering and reuse; each homework still shows the text that was saved.
- Q: What happens when the label being filtered on disappears (last homework unlabeled or deleted)? → A: Automatically return to “all labels” so the list is populated again.
- Q: Should the Homework tab offer a filter for homeworks with no label? → A: No. Only “all labels” plus labels in use. Some homeworks will not have a label; those appear when no specific label is selected.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher labels a homework (Priority: P1)

Paula is creating or editing a homework in the admin panel. Alongside the existing details (title, type, level, due date, and so on) she can give the homework an optional short label — free text she chooses, such as a unit nickname, a grammar topic, or a course group. The label is saved with the homework. She can leave it blank; homework without a label stays valid.

**Why this priority**: Filtering is useless until homeworks can carry a label. This is the smallest change that introduces the concept and unblocks every other story.

**Independent Test**: Create a new homework with a short label and save; reopen it and confirm the label is still there. Create another homework with no label and confirm it saves normally.

**Acceptance Scenarios**:

1. **Given** Paula is creating a new homework, **When** she enters a short label and saves, **Then** the homework is stored with that label.
2. **Given** Paula is creating a new homework, **When** she leaves the label empty and saves, **Then** the homework is stored with no label and works as it does today.
3. **Given** an existing homework, **When** she opens it to edit, **Then** she sees the current label (or an empty field if it has none) and can change it before saving.
4. **Given** she types a label longer than the allowed maximum, **When** she tries to save, **Then** the save is rejected and she is told the label is too long (the homework is not saved with a truncated label).

---

### User Story 2 - Teacher filters the Homework tab by label (Priority: P1)

Paula opens the admin Homework tab (Tareas), which already lists many homeworks and can be narrowed by type and level. She can also filter by label. Choosing a label shows only homeworks that have that label. She can clear the label filter to see the full list again, including homeworks that have no label. There is no separate “unlabeled” filter — some homeworks will never have a label, and those simply appear in the unfiltered list. The label filter works together with the existing type and level filters: all selected filters apply at once.

**Why this priority**: The stated problem is that the Homework tab has grown too large to scan. Label filtering is the outcome this feature exists to deliver.

**Independent Test**: With several homeworks labeled “Subjuntivo”, others labeled “Ser/Estar”, and some unlabeled, choose “Subjuntivo” and confirm only those items appear; combine with a type or level filter and confirm both constraints apply; choose “all labels” and confirm the full list returns.

**Acceptance Scenarios**:

1. **Given** homeworks with mixed labels, **When** Paula opens the Homework tab with no label filter, **Then** every homework still appears (subject only to any type/level filter already set).
2. **Given** at least one homework labeled “Subjuntivo” and another labeled “subjuntivo”, **When** she filters the Homework tab by that label, **Then** both homeworks are shown and no homework with a different label (or no label) is shown.
3. **Given** a label filter is active, **When** she also filters by type and/or level, **Then** the list includes only homeworks that match **all** selected filters.
4. **Given** a label filter that matches nothing under the current type/level filters, **When** the list updates, **Then** she sees a clear empty state (the same kind of “no homework matches these filters” message already used for type/level), not a broken page.
5. **Given** a label filter is active, **When** she clears it (back to “all labels”), **Then** the list again includes homeworks regardless of label, including those with no label (still respecting type/level if set).
6. **Given** some homeworks have no label, **When** she looks at the label filter choices, **Then** she sees “all labels” plus each label currently in use, and she does **not** see a dedicated unlabeled choice.

---

### User Story 3 - Teacher sees and reuses labels on the list (Priority: P2)

On the Homework tab list, each labeled homework shows its label so Paula can tell groups apart at a glance, without opening the editor. When she assigns a label, she can type a new one or pick a label already used on another homework, so spelling stays consistent and the filter list does not fill with near-duplicates.

**Why this priority**: Visible labels make the filter trustworthy (she can see why an item appeared). Reusing existing wording keeps the filter list small. Both are valuable once P1 works, but the tab is already usable if she only types labels and filters by wording.

**Independent Test**: Label two homeworks “Indicativo”; confirm both list cards show that label. On a third homework, pick “Indicativo” from labels already in use instead of retyping; confirm the filter then groups all three together. Label a fourth “indicativo” by typing; confirm it appears under the same filter and the reuse list still shows a single Indicativo/indicativo choice.

**Acceptance Scenarios**:

1. **Given** a homework with a label, **When** Paula views it on the Homework tab, **Then** that label is visible on the item (alongside existing type/level badges).
2. **Given** a homework with no label, **When** she views it on the list, **Then** no label badge is shown (the item is otherwise unchanged).
3. **Given** at least one homework already has the label “Indicativo”, **When** she edits another homework, **Then** she can choose “Indicativo” from labels already in use rather than typing it again.
4. **Given** she wants a wording that does not exist yet, **When** she types a new short label and saves, **Then** that new label is stored and becomes available to reuse on later homeworks.
5. **Given** a homework already labeled “Indicativo”, **When** she types “indicativo” on another homework and saves, **Then** both appear under the same filter choice; each card still shows the capitalization that was saved on that homework.

---

### User Story 4 - Teacher changes or removes a label (Priority: P2)

Paula can change a homework’s label or clear it. Students’ work on that homework is unaffected. If she clears the last use of a label, that label no longer appears as a filter choice.

**Why this priority**: Labels will be wrong or obsolete; being able to fix them is required for the filter to stay useful. It can ship right after assign + filter.

**Independent Test**: Change a labeled homework to a different label and confirm the old filter no longer includes it and the new filter does. Clear the label and confirm it appears under “all labels” and not under the old label.

**Acceptance Scenarios**:

1. **Given** a homework labeled “Unidad 3”, **When** Paula changes the label to “Unidad 4” and saves, **Then** the Homework tab shows “Unidad 4” on that item; filtering by “Unidad 3” no longer includes it; filtering by “Unidad 4” does.
2. **Given** a homework with a label, **When** she clears the label and saves, **Then** the homework has no label, it no longer appears when filtering by the old label, and it does appear again under “all labels”.
3. **Given** only one homework used the label “Temporal” and the Homework tab is filtered to that label, **When** she removes that label (or deletes that homework), **Then** “Temporal” is no longer offered as a filter choice and the tab returns to “all labels” so the remaining homeworks are visible again.
4. **Given** students have already been assigned the homework or have submitted it, **When** she changes or clears the label, **Then** assignment, submissions, scores, and review are unchanged.

---

### Edge Cases

- **Whitespace-only label**: A label that is only spaces is treated as no label (same as leaving the field empty). Leading and trailing spaces on a real label are ignored.
- **Too long**: A label above the maximum length cannot be saved; the teacher is told it is too long.
- **Any characters**: Letters, numbers, spaces, punctuation, and accents are allowed (Spanish wording such as “¿Ser o estar?” is valid) as long as the length limit is respected.
- **Last homework with a selected filter label**: If Paula is filtering by a label and then edits/deletes so that no remaining homework has that label, the tab returns to “all labels” (the list is populated again) and the stale label is no longer offered. This is different from a still-used label that matches nothing under the current type/level filters, which keeps the empty filtered state.
- **Duplicate wording**: Two homeworks belong to the same filter value when their labels match after trim, ignoring capitalization. Surrounding spaces are ignored. Accents and different letters still distinguish labels (`Gramática` ≠ `Gramatica`; `sí` ≠ `si`). Each homework still displays the exact text that was saved.
- **Existing homeworks**: Every homework created before this feature has no label. Unlabeled homeworks (old or new) appear under “all labels” and are omitted when a specific label is selected. Remaining unlabeled is expected, not something the tab must help her hunt down.
- **Homework tab only**: Label filter and label badges apply to the admin Homework tab list. Other places that list homeworks (unit content picker, student profile Tareas, student learning area) do not gain a label filter in this feature; they may ignore labels entirely.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The teacher MUST be able to set an optional label on a homework when creating it and when editing it.
- **FR-002**: A label MUST be free text of the teacher’s choosing, not chosen from a fixed built-in vocabulary (beyond offering labels already in use for reuse).
- **FR-003**: A label MUST have a maximum length of 40 characters after trimming. Longer values MUST be rejected with a clear message; the system MUST NOT silently truncate.
- **FR-004**: An empty or whitespace-only label MUST be stored as no label.
- **FR-005**: Leading and trailing whitespace MUST be removed before storing a label.
- **FR-006**: The admin Homework tab MUST let the teacher filter the homework list by label, with choices limited to: all homeworks regardless of label, and each specific label currently in use.
- **FR-006a**: The Homework tab MUST NOT offer a dedicated filter for homeworks with no label. Unlabeled homeworks MUST appear when “all labels” is selected and MUST NOT appear when a specific label is selected.
- **FR-007**: The label filter MUST combine with the existing type and level filters so that a homework appears only when it matches every active filter.
- **FR-008**: When a label that is still in use is selected and the combined type/level filters match no homeworks, the Homework tab MUST show the existing-style empty filtered state.
- **FR-008a**: When the teacher is filtering by a label and that label ceases to be used on any homework (last homework unlabeled or deleted), the Homework tab MUST switch back to “all labels” and MUST NOT leave her on an empty stale filter.
- **FR-009**: Each homework item on the admin Homework tab MUST show its label when it has one, and MUST NOT show a label placeholder when it has none.
- **FR-010**: When setting a label, the teacher MUST be able to reuse a label already stored on another homework without retyping it, and MUST still be able to type a new label. Reuse suggestions MUST list one entry per label group (capitalization ignored), not one entry per distinct capitalization.
- **FR-010a**: Labels that differ only by capitalization MUST be treated as the same label for filtering and reuse. Each homework MUST still display the exact label text that was saved on it.
- **FR-011**: Changing or clearing a homework’s label MUST NOT change student assignment, submissions, scores, or review.
- **FR-012**: A label MUST disappear from filter choices and from reuse suggestions once no remaining homework uses it.
- **FR-013**: Students MUST NOT be required to see or act on labels; this feature is for the teacher’s Homework tab.
- **FR-014**: Only the teacher (admin) MUST be able to set, change, or clear homework labels.

### Key Entities

- **Homework**: Existing teacher-authored assignment. Gains one optional short **label** used only for teacher organization and filtering. A homework has either one label or none.
- **Label**: A short free-text name the teacher attaches to a homework (for example a topic, unit nickname, or group). It is not a separate catalog the teacher manages on its own; it exists only as text on homeworks. Two stored values are the same label when they match after trim, ignoring capitalization; accents and other character differences still make them distinct.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can add or change a homework’s label as part of the normal create/edit save, in under 30 seconds, without a separate workflow.
- **SC-002**: After choosing one label on the Homework tab, 100% of listed homeworks have that label (including those whose saved text differs only by capitalization) and 0% of homeworks with a different label (or no label) remain visible.
- **SC-003**: On a Homework tab with dozens of items, the teacher can reduce the visible list to one labeled subset with a single filter choice, without scanning the full list.
- **SC-004**: Applying a label filter together with type and/or level never shows a homework that fails any of the selected filters.
- **SC-005**: Homeworks with no label remain fully usable (create, edit, assign, student take, review); labels are optional for every homework.
- **SC-006**: After the last homework using a given label is unlabeled or deleted, that label no longer appears as a filter choice, and if that label was the active filter the tab returns to showing homeworks under “all labels” without a further click.

## Assumptions

- One optional label per homework (the request describes “a label”, not several tags on the same homework).
- Maximum length is 40 characters after trim — long enough for a Spanish topic or unit nickname, short enough to show as a badge on the list.
- Labels are teacher-facing organization only. Students do not see labels on Mi aprendizaje, and other admin surfaces (unit picker, student profile Tareas, review queue) are unchanged except that saving a label from the homework editor persists for the Homework tab.
- Quizzes, presentations, units, and bookings are out of scope.
- There is no separate “manage labels” screen, no label colors, and no required vocabulary; unused labels simply disappear.
- Filter matching and reuse grouping ignore capitalization after trim. Accents still matter, so the teacher must reuse (or retype with the same letters and accents) when she wants homeworks grouped. Each homework keeps the capitalization she saved.
- Existing type and level filters stay as they are; this feature adds a label filter beside them, it does not replace them.
- Existing homeworks start unlabeled. Some homeworks will remain unlabeled by design; that is valid, not an incomplete state the UI must isolate.
- The label filter has no “unlabeled” choice — only “all labels” plus labels currently in use.
- The current teacher role (admin panel access) is the only actor who authors homework, so no extra permission model is needed beyond “teacher can, students cannot.”
