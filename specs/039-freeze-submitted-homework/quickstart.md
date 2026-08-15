# Quickstart: Freeze Submitted Homework

**Feature**: `039-freeze-submitted-homework` | **Date**: 2026-08-15

Manual validation guide. Contract: [freeze-submitted-homework-api.md](./contracts/freeze-submitted-homework-api.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- PostgreSQL `kuky_dev`; backend `local` profile (`:8081`); frontend `npm run dev` (`:8080`)
- Teacher (ADMIN) and two students (STUDENT)
- A self-correcting homework with at least two questions (opción única is enough)

## Setup

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

## Scenarios

### 1. Submitted work stays put after an edit (P1)

1. Assign the homework to student A and student B.
2. Student A submits (note prompts, chosen option, overall %).
3. Teacher edits: change a prompt, swap the correct option, delete the other question, save.
4. Reopen student A’s result (student view and teacher exercise result): original prompts, original choice, original %, original per-question right/wrong. Deleted question still visible on that result.
5. Student B (never submitted) sees the edited homework (one question, new prompt and key).

### 2. Answer-key change does not re-grade (P1)

1. Student A’s stored % from scenario 1.
2. Teacher changes the remaining question’s correct option again.
3. Student A’s % and item right/wrong unchanged. Student B, if they submit now, is graded with the newest key.

### 3. Teacher reviews against the original (P2)

1. Use a mixed or manual homework. Student A submits.
2. Teacher edits a free-text prompt and an auto-graded option.
3. Teacher opens A’s submission: original prompts and answers. Teacher can still enter a percentage on the original free-text answer.

### 4. Stale open take (P2)

1. Student B opens the take page and selects answers (do not submit).
2. Teacher saves a content edit.
3. Student B reloads, leaves and returns, or hits submit → message that the homework was updated; form is empty; they take the current homework. No frozen copy of the old take.
4. Changing **only the due date** must not show that message or clear answers.

### 5. Activities unchanged (P2)

1. Student submits a presentation activity. Teacher edits that activity’s questions.
2. Behaviour is as today (no new freeze, no `HOMEWORK_UPDATED` on activity submit).
