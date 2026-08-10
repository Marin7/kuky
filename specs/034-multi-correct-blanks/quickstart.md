# Quickstart: Multiple Correct Blank Answers

**Feature**: `034-multi-correct-blanks` | **Date**: 2026-08-10

Manual browser validation after implementation. See [data-model.md](./data-model.md) and [contracts/multi-correct-blanks-api.md](./contracts/multi-correct-blanks-api.md).

## Prerequisites

1. PostgreSQL `kuky_dev` + Mailpit (usual local setup).
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081`
3. Frontend: `npm run dev` in `front-end/` → `:8080`
4. Admin (teacher) and student accounts; student has `STUDENT` role.

## Scenario A — Typed multi-blank alternates + always-show feedback

1. Admin → Homework → create/edit exercise with one **MULTI_BLANK** passage (≥2 `___`).
2. For blank 1, add two accepted answers (e.g. `el` and `la`); leave blank 2 with one.
3. Confirm UI blocks adding an 11th accepted answer.
4. Assign to student; as student, submit blank 1 as the **second** accepted answer and blank 2 correct.
5. **Expect**: blank 1 correct; result shows both accepted answers for blank 1; blank 2 behaves as today (single expected only if wrong).

## Scenario B — Word bank multi-correct + distractor

1. Author **DRAG_DROP** with 2 blanks and 3 bank words: blank 1 accepts A **or** B; blank 2 accepts C; (or leave an extra word as distractor not assigned to any blank).
2. Save succeeds; reopen admin detail — structure shows `blanks[].correctBankIds`.
3. Student places B in blank 1 and C in blank 2 → both correct; feedback for blank 1 lists A and B.
4. New attempt (or second homework): place distractor in blank 1 → incorrect; feedback lists accepted labels for blank 1.

## Scenario C — Legacy word-bank compatibility

1. Use an existing homework that still has positional DRAG_DROP (`bank` only, length = blanks) **or** insert one via API/DB from pre-feature shape.
2. Student places `bank[i]` into blank `i` → same scores as before.
3. Teacher opens editor and saves without changing answers → persisted canonical shape; scores for the same placements still match.

## Scenario D — Validation errors

1. Try save DRAG_DROP with bank item marked correct on two blanks → blocked with validation message.
2. Try bank with 31 items → blocked.
3. Try MULTI_BLANK blank with 0 accepted answers → blocked.

## Scenario E — Activity parity

1. Repeat Scenario B on a **presentation activity** question of kind DRAG_DROP.
2. **Expect**: same authoring rules, grading, and multi-accepted feedback.

## Scenario F — Table-fill unchanged

1. Open a TABLE_FILL question with multiple accepted answers on a cell.
2. **Expect**: behaviour matches pre-feature (no new always-show-on-correct requirement, no new max-10 UI unless it already existed).

## Backend smoke (optional)

```bash
cd back-end
./gradlew test --tests '*HomeworkExerciseAdmin*' --tests '*ExerciseGrading*' --tests '*ActivityExercise*'
```

Adjust test class names to whatever exists/was added for this feature.
