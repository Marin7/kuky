# Quickstart: Percentage Grades for Manual Answers

**Feature**: `037-manual-percent-grade` | **Date**: 2026-08-11

Manual validation guide. Contract: [manual-percent-grade-api.md](./contracts/manual-percent-grade-api.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- PostgreSQL `kuky_dev` running; backend with `local` profile (Flyway applies `V19__manual_percent_grade.sql`)
- Frontend `npm run dev` on `:8080`; backend `:8081`
- Teacher (ADMIN) and student (STUDENT) accounts; at least one MIXED homework (auto + FREE_TEXT), one WRITE or ALL_MANUAL, and one activity with FREE_TEXT

## Setup

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

## Scenarios

### 1. Teacher finalizes with a percentage (P1)

1. As student, submit a WRITE or ALL_MANUAL homework.
2. As teacher, open admin review — confirm **percent input** (no Validate/Invalidate).
3. Enter e.g. `70` on each manual answer; **Finalize**.
4. Expect status `GRADED`, overall `%` matches (WRITE: 70; multi: equal average).
5. As student, open result — see `70%` (not validated/invalidated badges).

### 2. Partial save stays private (P1)

1. Submit a multi FREE_TEXT (or MIXED) homework as student.
2. As teacher, score only one answer at `80`, **Save progress** (`finalize: false`).
3. Expect status still awaiting / `SUBMITTED`.
4. As student, reload — **no** teacher percents; MIXED may still show auto provisional only.
5. As teacher, score remaining answers, **Finalize** — student then sees all percents + overall.

### 3. Mixed combined score (P1)

1. MIXED homework: 1 auto correct + 1 FREE_TEXT scored `50` → overall `75` after finalize.
2. Fully-correct count = 1 (only the auto / 100% answers).

### 4. Re-edit after grade (P2)

1. Change a graded percent from `60` → `80`, finalize again.
2. Student sees updated overall without resubmitting.

### 5. Activity parity (P2)

1. Same partial + finalize flow on a presentation activity with FREE_TEXT.
2. Expect identical percent behavior.

### 6. Migration smoke

1. Pre-existing graded submission that was validated/invalidated shows `100` / `0` percents and the same overall `%` as before migration.

## Automated checks (optional)

```bash
# back-end/
./gradlew test --tests '*HomeworkAdmin*' --tests '*ActivityAdmin*' --tests '*Composition*'
```

## Pass criteria

- No validate/invalidate controls in teacher UI for new reviews
- Students never see teacher % while awaiting
- Finalize blocked until every manual answer has 0–100
- Overall % = equal average with half-up display; fully-correct only at 100%
