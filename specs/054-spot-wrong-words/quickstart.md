# Quickstart: validating Spot the Wrong Words

## Prerequisites

- PostgreSQL `kuky_dev` and Mailpit running (see `CLAUDE.md` → Local dev setup).
- Back-end: `./gradlew bootRun --args='--spring.profiles.active=local'` (applies `V27`).
- Front-end: `npm run dev` → http://localhost:8080.
- One admin account (teacher email) and one account promoted to `STUDENT`.

## Automated checks

```bash
cd back-end && ./gradlew test --tests '*SpotWrongWords*' --tests '*ExerciseGradingServiceTest*'
cd front-end && npm run lint && npm run build
```

Expected: tokenizer vectors, scoring (found/errors), cap rejection and unit states pass; lint/build clean.

## Browser scenarios

1. **Segmentation** — In `/panel` → Tareas → new homework, add question type "Encontrar errores". Paste each input from the data-model vectors table, and check the clickable view splits words exactly as listed (punctuation and `—` not clickable).
2. **Authoring** — Text: `Ayer yo sabo que mi hermana estás en Madrid.` Click `sabo` (correction `supe`) and `estás` (correction `estaba`). Save, reopen: text, 2 marks and corrections unchanged. Saving with 0 marks shows "Marca al menos una palabra incorrecta."
3. **Edit keeps marks** — Insert `Hoy ` at the start of the text: both marks remain on `sabo`/`estás`. Change `sabo` to `sabía`: that mark disappears, `estás` stays.
4. **Preview** — Homework preview shows the text and "Encuentra los 2 errores" with no hint of which words.
5. **Student take + cap** — Assign to the student. As the student, open it: counter "0 / 2". Mark `sabo`, `mi` → "2 / 2"; clicking `Madrid` doesn't mark it and the cap hint appears; unmark `mi`, mark `estás`.
6. **Results (inline)** — Submit with `sabo` + `mi` marked. Score for the question is 50%. The text shows `sabo` as found (→ supe), `estás` as missed (→ estaba), `mi` as "marcada por error"; the three states are distinguishable without colour (icons/underline). An all-auto homework is GRADED immediately.
7. **Mixed homework** — Homework with this question + one FREE_TEXT: status stays SUBMITTED until the teacher grades the free text; overall % averages both questions equally.
8. **Cap enforced server-side** — Using the browser devtools, resend the submit with `"selected": [0,1,2]` for a 2-error question → 422 `VALIDATION_ERROR`, nothing saved.
9. **Quiz & activity** — Create the same question in a quiz (Pruebas) and in a presentation activity; take each as the student; behaviour and scoring match scenarios 5–6.
10. **Errores frecuentes** — In the student's admin profile, open "Errores frecuentes": the homework question appears once with the inline result. A fully-correct submission does not appear.
11. **Snapshot** — After the student submitted, edit the question (move a mark). The student's result and score are unchanged.
12. **Mobile** — At 375 px width, the text wraps, words are tappable without hitting neighbours, no horizontal scroll; the editor's correction fields stack.
13. **Locales** — Switch to en/ro: kind name, counter, cap hint and legend are translated.
