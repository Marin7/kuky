# Quickstart: Validate the Admin Student View Boxes

## Prerequisites

- PostgreSQL + Mailpit running (see CLAUDE.md "Local dev setup").
- Back-end: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'`
- Front-end: `cd front-end && npm run dev` → http://localhost:8080
- Signed in as the teacher (ADMIN). Test student data: at least one upcoming and one
  past confirmed class, homework in pending / submitted / graded states, one shared
  presentation, one quiz attempt `SUBMITTED` (unseen) and one `GRADED`.

## Static checks

```bash
cd front-end && npm run lint && npm run build
```

## Scenarios

Open **Panel → Alumnos → (student)** (`/panel/alumnos/<id>`).

1. **Initial load (FR-002, FR-004, SC-001)** — five boxes in order Clases · Tareas ·
   Presentaciones · Errores frecuentes · Pruebas de evaluación; no lists below them.
2. **Tareas breakdown (FR-001)** — pendientes / entregadas / completadas on three separate
   lines, counts sum to the total.
3. **Clases (FR-005)** — box shows total plus "N próximas" / "N pasadas" lines. Click →
   both sub-lists appear; toggle a past class's no-show → saved, panel stays open.
4. **Presentaciones (FR-006)** — total only; click → list with title + level.
5. **Pruebas de evaluación (FR-007, FR-009)** — total plus "por corregir" / "corregidas"
   lines; dot visible while collapsed. Click → quiz list; open the unseen attempt, save
   review → counts update, dot disappears, panel stays open (FR-011).
6. **Tareas dot (FR-009)** — with an unseen submitted homework the Tareas box shows a
   dot; reviewing it clears the dot.
7. **Independent toggles (FR-010)** — open Pruebas, then Clases → panels render
   Clases above Pruebas (box order), both open.
8. **Empty student** — student with no data: all boxes show 0, each panel shows its
   empty state.
9. **Responsive (FR-013, SC-004)** — resize to 375 px (2 columns), 768 px, desktop
   (single row of 5): no horizontal scroll, no truncated breakdown labels.
10. **Languages (FR-012)** — switch UI to English and Romanian; all new labels translated.
