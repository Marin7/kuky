# Quickstart: New Exercise Types

**Feature**: `024-new-exercise-types` | **Date**: 2026-08-03

Manual validation guide after implementation. Details: [data-model.md](./data-model.md), [contracts/exercise-types-api.md](./contracts/exercise-types-api.md).

## Prerequisites

1. PostgreSQL `kuky_dev` running; Mailpit optional.
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081` (Flyway applies `V6__new_exercise_types.sql`).
3. Frontend: `npm run dev` in `front-end/` → `:8080`.
4. Admin (teacher) account and at least one STUDENT assignee.

## 1. Multi-blank passage (P1)

1. Admin → Homework → new EXERCISE → add question kind **Multi-blank**.
2. Prompt: `Hoy ___ al mercado y compro ___.` with two accepted-answer rows (`voy` / `fruta`).
3. Save, assign student. As student, open exercise: one passage, two inputs (not two separate questions).
4. Submit wrong then… (single attempt) — use a second student or recreate: submit with `voy` + `fruta`.
5. Expect per-blank feedback + overall %; status GRADED; reopen is read-only with reveal.

**Fail checks**: One `___` only → cannot save. Accent mismatch (`fruta` vs wrong accent if applicable) scores 0.

## 2. Drag-and-drop word bank (P2)

1. Author passage with 3× `___` and bank labels in blank order (e.g. `rojo`, `verde`, `azul`).
2. Student: bank appears shuffled; place each chip via click-to-place (and try drag if available); keyboard path works.
3. Submit correct placements → all units correct. Wrong placement reveals expected label.

**Fail checks**: Bank count ≠ blanks → cannot save. Extra distractor bank item not offered in UI.

## 3. Table fill (P2)

1. Author a small conjugation grid (e.g. 3 row headers × 1 column), mark most cells blank with accepted forms; one fixed cell optional.
2. Student sees table layout; types into blanks; submit → per-cell feedback.

**Fail checks**: Table with zero blanks → cannot save.

## 4. Matching (P3)

1. Author 3 left, 4 right (one distractor), define 3 pairs.
2. Student: both lists shuffled; pair via click-to-pair; submit → per-pair feedback; distractor unused is fine.

**Fail checks**: No pairs → cannot save.

## 5. Regression — legacy kinds

1. Open an existing EXERCISE that only uses SINGLE_CHOICE / MULTI_CHOICE / FILL_BLANK (or create one).
2. Student take + submit still grades as before; admin still sees options-based authoring.
3. Placement-test admin/student flows unchanged (no new kinds in that UI).

## 6. Mixed exercise

One EXERCISE with FILL_BLANK + MULTI_BLANK + MATCHING; student completes all; scores average across questions; GRADED lock applies to the whole exercise.

## Backend smoke (optional)

```bash
cd back-end
./gradlew test --tests '*ExerciseGrading*' --tests '*HomeworkAdmin*'
```

Expect new-kind grading/validation tests green once implemented.

## Done when

- All four kinds author → take → grade → review work in the browser.
- Legacy exercises and placement test unaffected.
- Checklist items in the feature spec remain satisfied.
