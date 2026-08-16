# Quickstart: Homework Labels

**Feature**: `043-homework-labels` | **Date**: 2026-08-16

Manual validation guide. Contract: [homework-labels-api.md](./contracts/homework-labels-api.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- PostgreSQL `kuky_dev`; backend `local` profile (`:8081`); frontend `npm run dev` (`:8080`)
- Teacher (ADMIN) logged into `/panel` → Tareas
- Several existing homeworks (the tab should already look crowded)

## Setup

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

## Scenarios

### 1. Label on create and edit (P1)

1. New homework: set a short label (e.g. `Subjuntivo`) with type/level as usual; save.
2. Reopen: label field still shows `Subjuntivo`.
3. New homework: leave the label empty; save; reopen — field empty, homework works as today.
4. Type 41+ characters after spaces; save — rejected with a too-long message; homework not saved with a clipped label.

### 2. Filter the Homework tab (P1)

1. Give some homeworks `Subjuntivo`, others `Ser/Estar`, leave some unlabeled.
2. Label filter default (all): full list, including unlabeled.
3. Choose `Subjuntivo`: only that group. Unlabeled and `Ser/Estar` hidden.
4. Combine with type and/or level: AND logic; empty combined set shows the existing “no matches” message.
5. Filter choices are all + labels in use — **no** unlabeled option.
6. Back to all: unlabeled items return.

### 3. Badge, reuse, capitalization (P2)

1. Labeled cards show the saved label next to type/level; unlabeled cards have no label chip.
2. On another homework, pick `Subjuntivo` from labels in use (do not retype); save; it groups with the others.
3. Type `subjuntivo` on a further homework; it appears under the same filter; each card keeps its own capitalization.
4. Filter/reuse lists show **one** Subjuntivo/subjuntivo entry, not two.

### 4. Change, clear, stale filter (P2)

1. Change `Unidad 3` → `Unidad 4`; old filter excludes it; new filter includes it.
2. Clear a label: item has no badge; old label filter excludes it; it shows under all.
3. Filter to a label that exists on only one homework; delete that homework (or clear its label) and return to the tab: that label is gone from the dropdown and the list is on **all labels**, not an empty stale filter.
4. Students assigned / already submitted: scores and review unchanged after a label-only save. An open student take must **not** get a “homework was updated” reset from a label-only edit.

### 5. Out of scope (sanity)

1. Mi aprendizaje homework list/take: no label shown.
2. Student profile Tareas, review queue, unit content picker: no new label filter (picker may still work as today).
