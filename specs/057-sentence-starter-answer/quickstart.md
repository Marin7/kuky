# Quickstart: Sentence-Starter Answer — validation

## Prerequisites

- PostgreSQL + Mailpit running (see CLAUDE.md); back-end on `:8081` with `local` profile (Flyway applies `V33`).
- `npm run dev` in `front-end/` → http://localhost:8080.
- Teacher account (ADMIN) and one student account with `STUDENT` role.

## Automated checks

```bash
./gradlew test --tests '*SentenceStarters*'
```
(`SentenceStartersTest`: starter normalisation, compose spacing/punctuation, limits, count mismatch.)

```bash
npm run lint && npm run build
```

## Browser scenarios

1. **Author (homework, ALL_MANUAL)** — Panel → Tareas → new homework → add question "Completa la frase"; prompt "Lee el texto…"; starters `porque...`, `para`, `por`. Preview shows `porque` + input ×3. Save, reopen → starters normalised (`porque`), order kept. Remove all starters → save refused.
2. **Answer (ALL_MANUAL)** — as the student, open the homework: three lines, starter not editable. Submit with one empty line → "Debes responder a todas las preguntas." Fill all, submit → status awaiting correction; shown sentences begin with their starters.
3. **Correct** — as teacher, open the submission: each line's starter semibold; annotate, 80 %, note, finalize. As student: unseen dot on Mi aprendizaje; result shows sentences (starters bold), 80 %, note.
4. **MIXED homework** — add a single-choice + a sentence-starter question; student submits; auto part graded, sentence-starter awaits %; combined % after finalize.
5. **Unit page activity** — on `/panel/unidades/$unitId`, mark a page as activity with one sentence-starter question and no prompt (allowed). Student: answering in the block after the page; next pages unlock after submit; teacher sees it in the activity review (Unidades dot).
6. **Quiz** — quiz editor does not offer the kind; a crafted API save with it → 422.
7. **Version safety** — after a submission, rename/remove a starter in the homework; the student's result and teacher review still show the original starters.
8. **Phone width (375 px)** — starter and input stay together, no horizontal scroll; 500-char counter visible.
9. **Locales** — switch es/en/ro: kind name and editor labels translated.
