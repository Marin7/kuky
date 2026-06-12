# Quickstart: Homework Exercises & Self-Correction

End-to-end validation of the four user stories. Assumes the local dev setup from the root
`CLAUDE.md` (PostgreSQL 18, Mailpit optional, backend on `:8081`, frontend on `:8080`). The teacher
account is the one matching `app.scheduling.teacher-email` (promoted to `ADMIN` on startup); have at
least one `STUDENT` account too.

## Prerequisites

```bash
# Backend (from back-end/) — runs V15 migration on first start
./gradlew bootRun --args='--spring.profiles.active=local'

# Frontend (from front-end/)
npm run dev    # http://localhost:8080
```

Confirm the migration applied:
```sql
-- format column + GRADED status + new tables exist
SELECT column_name FROM information_schema.columns
 WHERE table_name='homework_assignments' AND column_name='format';
SELECT table_name FROM information_schema.tables
 WHERE table_name IN ('homework_questions','homework_question_options',
                      'homework_answers','homework_answer_options');
```

## Story 1 — Dedicated authoring page (P1)

1. Log in as the teacher → open `/panel` → **Tareas** tab.
2. Click **Nueva tarea**. Expect to navigate to `/panel/tareas/nueva` (a full page, **not** a
   dialog) showing title, instructions, type, level, due date, and assignees.
3. Fill a `MANUAL` homework (leave format = manual), assign a student, **Guardar**.
4. Expect to return to the homework list with the new item visible.
5. Click **Editar** on it → navigates to `/panel/tareas/{id}` pre-filled → change the title → save →
   list reflects the change. **Cancelar** from the page discards edits.

✅ Pass: authoring happens on a dedicated page; create/edit/cancel all behave; manual homework is
unchanged from before.

## Story 2 — Author a self-correcting exercise (P2)

1. **Nueva tarea** → switch **Formato** to *Ejercicio autocorregible*. A questions section appears.
2. Add a **SINGLE_CHOICE** question: prompt "El plural de *el lápiz*", options `los lápizes` /
   `los lápices` (mark the 2nd correct) / `los lápiz`.
3. Add a **MULTI_CHOICE** question with two correct options among four.
4. Add a **FILL_BLANK** question: prompt "Ayer yo ___ (ir) al cine", accepted answer `fui`.
5. Try to save with the single-choice question having **no** correct option → expect a blocking
   validation message (FR-008). Fix it.
6. Try to save an exercise with **zero** questions → blocked (FR-010).
7. Assign to a student, **Guardar**. Reopen via **Editar** → questions, options, and the correct
   flags persisted exactly.

Verify the key is stored:
```sql
SELECT q.kind, o.label, o.is_correct
  FROM homework_questions q JOIN homework_question_options o ON o.question_id=q.id
 WHERE q.assignment_id = '<id>' ORDER BY q.position, o.position;
```

✅ Pass: exercises with all three question kinds author, validate, and round-trip.

## Story 3 — Student automatic feedback (P3)

1. Log in as the assigned student → `/aprendizaje`. The exercise card shows an *Ejercicio* badge and
   **Empezar ejercicio**.
2. Click it → navigates to `/aprendizaje/tarea/{id}` (dedicated page). Single-choice shows **radios**,
   multi-choice shows **checkboxes**, fill-blank shows a **text field**. Confirm via DevTools/Network
   that the `GET /api/v1/learning/homework/{id}` payload contains **no** `correct` flags and **no**
   options for the fill-blank (answer key hidden — FR-011).
3. Answer: the multi-choice partially right (one correct + one wrong), fill-blank `Fui` (capitalised).
4. **Entregar** → immediately see per-question correct/incorrect marks, the **correct answer shown for
   wrong questions**, and an overall score like `80% — 4 de 5 correctas`.
5. Confirm `Fui` was accepted (case-insensitive) (FR-009) and the multi-choice shows a **partial**
   score (FR-013).
6. Reload the page → the exercise is **read-only** with the same result (single-submission lock,
   FR-020). A second `PUT …/answers` returns `409 SUBMISSION_NOT_ALLOWED`.
7. As the teacher, open the homework in the panel → the student's row shows status **Calificada** and
   the `scorePercent`; their answers are viewable (FR-016).

Verify grade persisted:
```sql
SELECT status, score_percent FROM homework_submissions
 WHERE assignment_id='<id>' AND user_id='<student>';   -- GRADED, e.g. 80
```

✅ Pass: instant per-question feedback + correct answers + overall %, accent-exact & partial-credit
grading, single-submission lock, teacher sees the score.

## Story 4 — Manual homework still manual (P3)

1. As a student, open a `MANUAL` homework (e.g. a V5-seeded one once assigned) → it uses the existing
   **dialog** with a free-text response, not the exercise page.
2. Submit text → status becomes **Entregada** (SUBMITTED), **no** automatic score; the teacher reviews
   it manually as before.
3. Confirm pre-existing homework created before this feature still opens and submits (no data loss).

✅ Pass: manual flow and the `PENDING → SUBMITTED → REVIEWED` lifecycle are unchanged.

## Regression / negative checks

- `GET /api/v1/learning/homework/{id}` for a homework **not** assigned to the caller → `404`.
- `PUT /api/v1/learning/homework/{id}/answers` on a `MANUAL` homework → `400 SUBMISSION_NOT_ALLOWED`.
- Non-admin `GET /api/v1/admin/homework/{id}` → `403 ACCESS_DENIED`; guest → `401`.
- Editing an exercise's options after a student was graded does **not** change that student's stored
  `score_percent` (research §8).

## Automated tests to run

```bash
# from back-end/
./gradlew test --tests '*ExerciseGradingServiceTest' --tests '*HomeworkExerciseAdminServiceTest'
```
Frontend: visual verification in the running browser per the constitution (the steps above).
