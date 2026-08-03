# Research: New Exercise Types

**Feature**: `024-new-exercise-types` | **Date**: 2026-08-03

## 1. Storage for kind-specific payloads

**Decision**: Add nullable JSONB `structure_json` on `homework_questions` and `answer_json` on `homework_answers`. Keep the existing options / `answer_text` / `answer_options` path for SINGLE_CHOICE, MULTI_CHOICE, and FILL_BLANK unchanged.

**Rationale**: New kinds need multi-unit answers, banks, grids, and pairings that do not fit a single `answer_text` or `{label, is_correct}` option row. JSONB keeps one migration and one read/write path without proliferating tables (YAGNI). Original kinds need no data migration.

**Alternatives considered**:
- Kind-specific tables (`homework_blanks`, `homework_match_pairs`, …) — clearer relational shape, but many joins and more Flyway surface for a single-teacher product.
- Overload `homework_question_options` with encoded labels — brittle, hard to query, poor validation.
- Migrate all kinds to JSONB — unnecessary churn for working choice/fill-blank code.

## 2. Enum / kind names

**Decision**:
| Spec term | `QuestionKind` value |
|-----------|----------------------|
| Multi-blank passage | `MULTI_BLANK` |
| Drag-and-drop word bank | `DRAG_DROP` |
| Table fill | `TABLE_FILL` |
| Matching | `MATCHING` |

**Rationale**: Short, stable API strings; parallel to existing `FILL_BLANK` / `SINGLE_CHOICE` style. Placement-test `QuestionKind` stays at three values (homework-only scope).

**Alternatives considered**: `WORD_BANK`, `GAP_FILL`, `CONJUGATION_TABLE` — longer / less precise for matching and tables.

## 3. Blank token parsing (`___`)

**Decision**: A blank is exactly three consecutive underscores not adjacent to another underscore: regex `(?<!_)___(?!_)`. Each match is one blank in left-to-right order. `____` (4+) does **not** count as a blank (avoids ambiguous runs). Authoring validation requires ≥ 2 matches for `MULTI_BLANK` and `DRAG_DROP`.

**Rationale**: Matches clarification “type `___`”; exact-three avoids treating decorative long underscore lines as blanks.

**Alternatives considered**: Any run of ≥3 underscores as one blank — easier for sloppy typing, but merges `___` and `_____` unexpectedly. UI “insert blank” control — rejected in clarification.

## 4. Word-bank correct mapping

**Decision**: Teacher enters bank labels in **blank order** (bank item *i* is the correct answer for blank *i*). Bank length must equal blank count. Student sees a shuffled copy of the same items (stable IDs). Grading compares placed bank-item id to the authored item id for that blank index.

**Rationale**: Spec “aligned to blank order” + YAGNI — no separate mapping UI. Still identity-based grading so duplicate labels remain distinct items.

**Alternatives considered**: Free mapping UI (blank → any bank item) — more flexible, more authoring complexity. Extra distractor bank items — rejected in clarification.

## 5. Shuffle strategy

**Decision**: Client-side Fisher–Yates shuffle when the student mounts a not-yet-graded question (word bank list; matching left and right lists independently). Grading and reveal use item IDs, never display index. Teacher authoring/review keeps authored order. Refresh may reshuffle before submit — acceptable for homework.

**Rationale**: No server state or seed persistence; meets “always shuffle” without schema. High-stakes deterministic seed not required (not placement).

**Alternatives considered**: Deterministic hash(userId+questionId) order — stable across refresh, slightly more backend/contract work. Persist shuffle on first GET — overkill.

## 6. Drag-and-drop / matching interaction (no new deps)

**Decision**: Primary interaction is **click-to-place / click-to-pair** (select bank item or left item, then activate blank or right item; keyboard operable). Optional native HTML5 drag-and-drop as progressive enhancement on pointer devices. No `@dnd-kit` or similar.

**Rationale**: Constitution / prior plans prefer zero new frontend deps; click-to-place satisfies accessibility requirement in the spec.

**Alternatives considered**: Add `@dnd-kit` — polished DnD, dependency cost. Pointer-only HTML5 DnD — fails keyboard requirement.

## 7. Per-unit scoring and result shape

**Decision**: Each blank / blank cell / expected pair is a unit scoring 0 or 1. Question score = average of its units (same as FR-004). Extend result DTO with `unitResults[]` (index, score, correct, optional student/expected reveal strings). `fullyCorrectCount` still counts questions with score === 1. Typed units reuse FILL_BLANK normalize: trim + `toLowerCase(Locale.ROOT)`, accent-exact.

**Rationale**: Matches existing MULTI_CHOICE partial-credit pattern and post-submit reveal behaviour.

**Alternatives considered**: All-or-nothing per question — worse pedagogy for multi-blank. Separate score columns per unit in DB — unnecessary when `answer_json` + recomputed or stored question score already exist (keep storing per-question `score` as today).

## 8. Limits

**Decision**:
- MULTI_BLANK / DRAG_DROP: 2–20 blanks
- DRAG_DROP bank: length == blanks (no extras)
- TABLE_FILL: 1–12 rows, 1–12 cols, 1–50 blank cells
- MATCHING: 1–20 left, 1–20 right, ≥1 correct pair; pair endpoints must exist; at most one correct right per left that is paired

**Rationale**: Spec deferred upper bounds to planning; these bound JSON size and UI density for conjugations / short drills.

## 9. Placement test

**Decision**: Do not extend placement `QuestionKind`, admin UI, or grading. Homework-only.

**Rationale**: Clarification Q1 option A.
