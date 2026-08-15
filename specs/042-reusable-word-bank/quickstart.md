# Quickstart: Reusable Word Bank

**Feature**: `042-reusable-word-bank` | **Date**: 2026-08-15

Manual browser validation after implementation. See [data-model.md](./data-model.md) and [contracts/reusable-word-bank-api.md](./contracts/reusable-word-bank-api.md).

## Prerequisites

1. PostgreSQL `kuky_dev` + Mailpit (usual local setup).
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081`
3. Frontend: `npm run dev` in `front-end/` → `:8080`
4. Admin (teacher) and student accounts; student has `STUDENT` role.

## Scenario A — Reusable: same word in two blanks

1. Admin → Homework → **Arrastrar y soltar** with 2 `___` and a bank of at least 2 words (second word may be a distractor).
2. Mark the **same** bank word as correct for **both** blanks. Save and assign.
3. As student, open the take view.
4. **Expect**: no new instruction text (same drag-and-drop hint as exclusive questions). Place that word in blank 1; it stays available (not struck through). Place it in blank 2; **both** blanks show that word.
5. Submit. **Expect**: both blanks correct; results show the same word in both blanks.

## Scenario B — Exclusive unchanged

1. Open (or author) a word-bank question where each bank word is correct for at most one blank (today’s usual drill, with or without distractors).
2. As student, place a word in blank 1.
3. **Expect**: that chip is disabled/struck through. Place it into blank 2 → it **moves** (blank 1 empties). Clear a blank → the chip returns to the bank.

## Scenario C — Shared alternates still reusable

1. Author two blanks that both accept A **and** B (order-independent “___ y ___”).
2. **Expect**: take view is reusable (chips stay available). Student may put A in both blanks (blank 2 still scores if B is also accepted — per that blank’s key) or put A then B as today.

## Scenario D — Activity and quiz parity

1. Repeat Scenario A on a **presentation activity** DRAG_DROP question.
2. If a quiz includes DRAG_DROP, repeat Scenario A on the quiz take.
3. **Expect**: same exclusive vs reusable behaviour; no extra copy.

## Scenario E — Answer key stays hidden

1. On a reusable take, inspect the student payload (network) for the question `structure`.
2. **Expect**: `bank` + `bankReusable: true`; no `correctBankIds` / `blanks` key.

## Backend smoke (optional)

```bash
cd back-end
./gradlew test --tests '*ExerciseGrading*' --tests '*ActivityExercise*' --tests '*HomeworkExerciseAdmin*'
```
