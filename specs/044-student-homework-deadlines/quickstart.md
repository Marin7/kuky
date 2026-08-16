# Quickstart: Student-Based Homework Deadlines

**Feature**: `044-student-homework-deadlines` | **Date**: 2026-08-16

Manual validation guide. Contract: [student-homework-deadlines-api.md](./contracts/student-homework-deadlines-api.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- PostgreSQL `kuky_dev`; backend `local` profile (`:8081`); frontend `npm run dev` (`:8080`)
- Teacher (ADMIN) on `/panel` → Tareas
- At least two `STUDENT` accounts (A and B)

## Setup

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

## Scenarios

### 1. Assign-time date for new students only (P1)

1. Create or open a homework. Confirm there is **no** due date on the content fields (title/type/level/questions).
2. Assign A and B with an optional date (e.g. next week). Save. Each assignee row shows that date.
3. Change only B’s date on B’s row (extension). A’s date stays. Homework list card has **no** overall date.
4. Add student C via the assignee picker with a **different** assign-time date. C gets the new date; A and B keep theirs.

### 2. Student sees only their date (P1)

1. As A: Mi aprendizaje (and unit homework row if the item is in a unit) shows A’s date; overdue only if A is pending and the date is past.
2. As B: same homework shows B’s date, not A’s.
3. Assign D with **no** date. D sees no deadline and is not overdue.
4. As A, with a **past** date and still pending: overdue badge; submit still succeeds.

### 3. Clear, unassign, unit assign (P2)

1. Clear A’s date on the assignee row → A has no deadline; B unchanged.
2. Unassign A and re-assign A later → A starts with no date (previous date not restored).
3. Assign a unit to a student: granted homeworks have **no** due date and unit assign has no date field. Set dates afterwards from each homework’s assignee list.

### 4. Admin views and freeze (P2)

1. Student profile Tareas: each homework shows **that** student’s date and overdue; the date is not editable there.
2. Pending + past date → overdue on assignee row and profile; submitted/graded with a past date → not overdue.
3. Change only a due date while a student has an in-progress take: they must **not** see “homework was updated” and must not lose answers.

### 5. Out of scope (sanity)

1. Quizzes and presentation activities: no new due dates.
2. Unit assign dialog: still students only, no deadline control.
