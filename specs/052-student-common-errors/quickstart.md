# Quickstart: Validate Student Common Errors

**Feature**: `052-student-common-errors`. See [contracts/api.md](contracts/api.md) and [data-model.md](data-model.md) for shapes and rules.

## Prerequisites

- PostgreSQL 18 with `kuky_dev`, and Mailpit (see `CLAUDE.md` › Local dev setup).
- Back-end: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'` (serves on :8081).
- Front-end: `cd front-end && npm run dev` (serves on :8080).
- A teacher account (`ADMIN`) and one student account (`STUDENT`).

## Seed scenario

As the teacher, in `/panel` › Tareas:

1. **HW-A** "Vocabulario 1": auto-graded, label `Vocabulario`, 6 single-choice questions.
2. **HW-B** "Subjuntivo 1": auto-graded, labels `Subjuntivo` and `unidad 3`, 5 questions: one MULTI_BLANK with 2 blanks, one numbered single choice with items `(1)…(3)`, and three TRUE_FALSE.
3. **HW-C** "Mixto": one SINGLE_CHOICE plus one FREE_TEXT, label `subjuntivo` (lower-case on purpose).
4. **HW-D** "Sin etiqueta": auto-graded, no labels, 2 questions.
5. Assign all four to the student.

As the student, in `/aprendizaje`:

- Submit HW-A with 1 wrong answer.
- Submit HW-B with 1 wrong blank, items (1) and (3) wrong, and all true/false questions right.
- Submit HW-C with the single choice wrong and any free text. Leave it awaiting teacher review.
- Submit HW-D with both questions wrong. Leave one of them unanswered if the UI allows it.

## Scenarios

| # | Action | Expected |
|---|---|---|
| 1 | Teacher opens `/panel/alumnos/<student>` | 4 stat boxes: 2×2 on phone width, one row on desktop. **Errores frecuentes** shows `7` (A:1 + B:1+2 + C:1 + D:2), collapsed. |
| 2 | Click the box | The list opens below the stats. Summary: "7 errores en 16 preguntas" (A:6 + B:1+3+3 + C:1 + D:2 = 16 auto contributions). Entries run newest-first: D, C, B, A. Within B, blank entry first, then the numbered entry, in question order. |
| 3 | Inspect the numbered entry (HW-B) | One entry; items (1) and (3) marked wrong, (2) right. |
| 4 | Inspect the HW-C entry | The single-choice mistake is shown. The FREE_TEXT answer does **not** appear anywhere (FR-003). |
| 5 | Inspect an HW-D entry with no answer | Shows the "sin respuesta" marker. There are no labels on the entry. |
| 6 | Open the Tareas box as well | Both lists open, Tareas first (FR-001a). |
| 7 | Label filter options | "Todas", "Subjuntivo" (a single option, even though `subjuntivo` and `Subjuntivo` both exist), "unidad 3", "Vocabulario". There is no "sin etiqueta" option. |
| 8 | Pick "Subjuntivo" | Only the HW-B and HW-C entries. The summary counts only those homeworks' auto contributions. Page resets to 1. |
| 9 | Period "Últimos 30 días" with a submission dated > 30 days back (set `submitted_at` in the DB for HW-A) | HW-A disappears; the summary updates. The box count is unchanged (it is all-time). |
| 10 | "Abrir tarea" on an HW-A entry | `ExerciseResultDialog` opens that submission. On an HW-C entry, `HomeworkReviewDialog` opens. |
| 11 | Edit HW-B's question text after submission, then reload | The entry still shows the text the student submitted against (snapshot). |
| 12 | Relabel HW-A to `Léxico`, then reload | The HW-A entry shows `Léxico`; the options include `Léxico` and no longer include `Vocabulario`. |
| 13 | With "Vocabulario" selected, relabel HW-A away from it and refresh the list | The filter falls back to "Todas" (FR-012a). |
| 14 | Delete HW-D, then reload | Its entries disappear and the counts drop. |
| 15 | Pagination (create > 20 wrong entries, e.g. a 25-question auto homework all wrong) | Pages of 20, "Página 1 de 2", Anterior disabled on page 1, Siguiente disabled on the last page. Changing a filter returns to page 1. No controls when there is ≤ 1 page. |
| 16 | Student with no auto-graded submissions | Box shows 0. The expanded list shows the "no auto-graded homework yet" empty state. |
| 17 | Student with every answer right | "Sin errores en este periodo" plus the answered count. |
| 18 | Viewing the section, then checking the student's `/aprendizaje` and the panel dots | Nothing has changed (FR-014): no dots cleared or created by viewing the list itself. |

## Security check

```bash
curl -i -b student-cookies.txt http://localhost:8081/api/v1/admin/students/<id>/mistakes
```

Expected: `403`. With the teacher's cookie: `200`. With a random UUID: `404 STUDENT_NOT_FOUND`.

## Automated tests

```bash
cd back-end && ./gradlew test
```

The integration tests run in CI against a throwaway Postgres and are skipped locally. The new service unit test runs locally.
