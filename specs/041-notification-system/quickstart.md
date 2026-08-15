# Quickstart: In-App Activity Notifications

**Feature**: `041-notification-system` | **Date**: 2026-08-15

Manual validation. Contract: [notifications-api.md](./contracts/notifications-api.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- PostgreSQL `kuky_dev`; backend `local` (`:8081`); frontend `npm run dev` (`:8080`)
- Teacher (ADMIN) and two students A and B (`STUDENT`). Flyway `V22` applied on boot.

## Setup

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

Work created **before** this feature must **not** show icons after boot (FR-016).

## Scenarios

### 1. Teacher: homework submit (P1)

1. Assign a unit (with a homework) to A if needed. A submits the homework (auto or write).
2. As teacher, load or navigate the site: **Panel** has a small icon; **Tareas** has one; **Pruebas de evaluación** does not.
3. Open Tareas: that homework card has an icon. Open the homework: A’s row has an icon. Opening the editor or the student list without opening A’s work leaves icons.
4. Open A’s submitted work (from Tareas or A’s profile). Icons for A go away. If only A had news, Panel and Tareas clear.

### 2. Teacher: two students, one homework (P1 / clarify)

1. B also submits the same homework.
2. Homework card, Tareas, and Panel stay marked. A’s row is clear (if already opened); B’s row is marked.
3. Open B’s submission: B’s row clears; homework/Tareas/Panel clear if nothing else is unseen.

### 3. Teacher: quiz submit (P1)

1. Assign a quiz to A. A submits.
2. **Panel** and **Pruebas de evaluación** icons; **Tareas** does not from this event.
3. Quiz card + A’s attempt row marked. Opening the quiz editor or attempt list does not clear. Opening A’s attempt (tab or profile) clears that attempt.

### 4. Mixed homework + quiz (P1)

1. Unseen homework and unseen quiz exist.
2. Panel has one icon; both Tareas and Pruebas de evaluación have icons.

### 5. Student: unit and quiz assign (P1)

1. Teacher assigns a **new** unit to A and a **new** quiz to A.
2. A logs in or navigates: **Mi aprendizaje** has an icon. Unit card and quiz row each have an icon.
3. Open Mi aprendizaje only: icons remain.
4. Open the unit page (do not start homework): unit card and, if it was the last unit news, that part of the nav logic updates; quiz icon remains.
5. Open the quiz page without submitting: quiz assignment icon clears.
6. B was not assigned: no icons for B.

### 6. Student: homework inside unit does not clear (clarify)

1. Unseen unit for A. A opens a homework that belongs to that unit without opening `/aprendizaje/unidad/{id}`.
2. Unit notification and Mi aprendizaje icon remain.

### 7. No notify (clarify / out of scope)

1. Add homework to a unit A already has → A gets no new icon.
2. Direct Tareas assignee change without a new unit assign → no student icon.
3. Teacher grades / comments → no student icon.
4. A starts a homework or quiz without submit → teacher gets no icon.
5. Presentation activity submit/assign → no icons.

### 8. Unassign / delete

1. Assign a quiz to A; A has not opened it. Unassign A: A’s icon is gone.
2. Delete a homework that had an unseen submission: teacher icons for it are gone.

### 9. Already signed in

1. Teacher is on Reservas. A submits. Teacher navigates to another page (or refreshes): Panel icon appears without logging out.
