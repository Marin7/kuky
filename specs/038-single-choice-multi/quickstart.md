# Quickstart: Multi-Item Opción Única

**Feature**: `038-single-choice-multi` | **Date**: 2026-08-15

Manual validation guide. Contract: [single-choice-multi-api.md](./contracts/single-choice-multi-api.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- PostgreSQL `kuky_dev`; backend `local` profile (`:8081`); frontend `npm run dev` (`:8080`)
- Teacher (ADMIN) and student (STUDENT)
- An existing classic opción única homework (no `(1)` in the prompt) to prove non-migration

## Setup

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

## Scenarios

### 1. Author `(1)` `(2)` `(3)` (P1)

1. Admin → new/edit homework → add **Opción única**.
2. Prompt e.g. `Elige: (1) ser/estar (2) por/para (3) muy/mucho`.
3. Confirm **three** option editors appear (not one classic list).
4. Two options + one correct per number; save.
5. Reopen: prompt, three items, options, and correct marks intact.

### 2. Invalid sequence cannot save (P1)

1. Same question; delete `(2)` leaving `(1)` and `(3)`.
2. Save → blocked with a clear validation message.
3. Change `(3)` to `(2)` (or restore `(2)`); save succeeds.

### 3. Classic stays classic (P1)

1. Open a **pre-existing** opción única with no `(N)` in the text.
2. Student takes it: **one** radio group; score 0 or 100 as before.
3. Teacher does not need to re-save.

### 4. Student radios only (P1)

1. Student opens the three-item homework.
2. Sees the prompt **including** `(1)` `(2)` `(3)` and **three stacked radio groups** (no text fields).
3. Leave one group empty → submit blocked.
4. Select all three → submit.

### 5. Scoring expansion (P1)

1. Homework contains **only** that three-item entry.
2. Student gets 2 of 3 items right.
3. Expect **67%** and **2 of 3** fully correct (not 0% / 0 of 1, and not 67% as one question of 1).
4. Add a FREE_TEXT question on a copy of the homework; 3 items + 1 manual → **4** equal parts after the teacher scores the free-text.

### 6. Mode switch (P2)

1. Classic question with options A/B (B correct); type `(1)` into the prompt.
2. Item `(1)` should show A/B with B still correct.
3. Add `(2)`; item 2 empty until filled.
4. Remove all markers → classic list restored from item `(1)`.

### 7. Activity parity (P2)

1. Repeat scenario 1 + 4 on a presentation activity opción única.
2. Same radios, same expanded score.

### 8. Placement unchanged (P2)

1. Placement admin still has single opción única (no `(N)` item editors).
2. Taking a placement SINGLE_CHOICE is one radio group.
