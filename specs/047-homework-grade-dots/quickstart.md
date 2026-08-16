# Quickstart: Student Dots for Homework Corrections and Feedback

**Feature**: `047-homework-grade-dots` | **Date**: 2026-08-16

Manual validation. Contract: [homework-grade-dots-api.md](./contracts/homework-grade-dots-api.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- PostgreSQL `kuky_dev`; backend `local` (`:8081`); frontend `npm run dev` (`:8080`)
- Teacher (ADMIN) and two students A and B (`STUDENT`). Flyway `V25` applied on boot.

## Setup

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

Grades and comments created **before** this feature must **not** show student review icons after boot (FR-016).

## Scenarios

### 1. Student: teacher finalizes a correction (P1)

1. Assign a unit with a Writing or free-text homework to A. A submits.
2. As teacher, save **progress** only (not finalize). As A, load the site: **no** new Mi aprendizaje icon from this review.
3. As teacher, finalize percents. As A, load or navigate: **Mi aprendizaje** has an icon; the unit card and that homework have icons.
4. Open Mi aprendizaje and the unit but not the homework result: icons remain.
5. Open that homework’s result (unit inline or homework page). Icons for this news go away. If it was the last student news, Mi aprendizaje clears.

### 2. Student: teacher leaves feedback on auto-scored work (P1)

1. A submits an auto-graded exercise (no student icon from submit).
2. As teacher, save exercise feedback text. As A, navigate: Mi aprendizaje + unit + homework icons.
3. Open the result: icons clear.
4. Repeat with **annotations only** (no written comment) on a graded free-text/Writing item: same icons; they clear on opening the result.

### 3. Awaiting comments wait until finalize (clarify)

1. A submits manual work. Teacher saves a comment/annotation without finalizing.
2. A navigates: no review icon.
3. Teacher finalizes (comment still there): one set of icons, not two.

### 4. Later changes (P2)

1. After A opened the graded result (icons gone), teacher changes a percent and saves.
2. A navigates: icons return until A reopens the result.
3. After A opened again, teacher updates feedback or annotations: icons return again.
4. While still unseen, extra teacher saves on the same homework: still one indicator.

### 5. Independent of assignment dots (P2 / FR-018)

1. Teacher assigns A a **new** unit (assignment unseen) and later finalizes a homework in that unit.
2. A sees Mi aprendizaje marked. Opening the **unit** clears only assignment news; homework (and unit card if that homework is still unseen) stay marked until A opens the **result**.

### 6. Clear feedback / no-op / other student

1. Auto-scored homework, teacher adds feedback (A unseen), then deletes the feedback before A opens: A’s review icon is gone.
2. Teacher opens a graded homework and saves with no score/comment/annotation change: A gets no new icon.
3. B never sees A’s review icons. Teacher Panel/Tareas dots are unchanged by grading.

### 7. Out of scope

1. Finalize or comment on a **presentation activity** → no student review icon.
2. Quiz scoring/comments → no student review icon from this feature.
3. Auto-grade on submit with no later teacher mark → no student review icon.

### 8. Already signed in

1. A is on another page. Teacher finalizes. A navigates (or refreshes): Mi aprendizaje icon appears without logging out.
