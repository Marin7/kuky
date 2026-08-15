---
description: "Task list for reusable word-bank take behaviour"
---

# Tasks: Reusable Word Bank

**Input**: Design documents from `specs/042-reusable-word-bank/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/reusable-word-bank-api.md](./contracts/reusable-word-bank-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit for `bankReusable` strip (true/false, legacy exclusive) and duplicate placement grading (per [plan.md](./plan.md)). Frontend — browser verification via [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted. No new i18n.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Student DTO type for the derived flag. No Flyway, no new packages.

- [x] T001 Add optional `bankReusable?: boolean` to `StudentStructure` in `front-end/src/lib/learning.ts` (DRAG_DROP only; omit/`false` means exclusive per [data-model.md](./data-model.md))

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Server derives reusable vs exclusive from the answer key and exposes only `bankReusable` on student take payloads. Blocks all user stories.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 Implement `isBankReusable(Resolved)` (or equivalent) in `back-end/src/main/java/com/kuky/backend/learning/service/DragDropStructureSupport.java`: `true` iff any bank id appears in ≥2 blanks’ `correctBankIds` after `resolve` (legacy positional → `false`) per [research.md](./research.md)
- [x] T003 Include `bankReusable` on stripped DRAG_DROP `structure` (keep `bank`; never `blanks` / `correctBankIds`) in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` `stripStructureForStudent` — quizzes inherit via `QuizGradingService` → `studentQuestionsFor`
- [x] T004 [P] Mirror T003 strip (`bank` + `bankReusable`, no keys) in `back-end/src/main/java/com/kuky/backend/learning/service/ActivityExerciseGradingService.java` `stripStructureForStudent`

**Checkpoint**: Student homework, activity, and quiz take payloads can carry `bankReusable` without leaking the answer key.

---

## Phase 3: User Story 1 - Place the same bank word in several blanks (Priority: P1) — MVP

**Goal**: When `bankReusable` is true, bank chips stay available after placement and placing a word into a second blank copies it (does not empty the first). No new instructional text.

**Independent Test**: Author DRAG_DROP with the same bank item correct for two blanks; student places that word in both blanks; both stay filled ([quickstart.md](./quickstart.md) Scenario A).

### Implementation for User Story 1

- [x] T005 [US1] Gate `place` / bank chip enablement in `front-end/src/components/learning/DragDropQuestion.tsx`: when `bankReusable`, do not disable chips, do not clear other blanks on place (copy); keep click-to-select, replace-on-occupied-blank, and click-to-clear; do **not** add new hint copy (existing `dragDropInstructions` only)
- [x] T006 [P] [US1] Pass `bankReusable={q.structure?.bankReusable === true}` into `DragDropQuestion` in `front-end/src/components/learning/ExerciseForm.tsx`
- [x] T007 [P] [US1] Pass `bankReusable={q.structure?.bankReusable === true}` into `DragDropQuestion` in `front-end/src/components/learning/MixedHomeworkForm.tsx` (covers mixed homework, `ActivityPanel`, quizzes)
- [x] T008 [US1] Backend tests: stripped DRAG_DROP structure has `bankReusable: true` when the same id is on two blanks, and does not include `blanks`/`correctBankIds`, in `back-end/src/test/java/com/kuky/backend/learning/ExerciseGradingServiceTest.java` (add activity strip assertion if an activity take/strip test class exists)

**Checkpoint**: Reusable take UX works on homework (and any MixedHomeworkForm surface). FR-001/FR-003/FR-004/FR-008 for the reusable path.

---

## Phase 4: User Story 2 - Exclusive word-bank questions stay unchanged (Priority: P1)

**Goal**: When `bankReusable` is false or omitted, disable-on-place and move-not-copy remain exactly as today. Legacy positional banks stay exclusive.

**Independent Test**: Exclusive question: place a word → chip disabled; place it in another blank → first blank empties ([quickstart.md](./quickstart.md) Scenario B).

### Implementation for User Story 2

- [x] T009 [US2] Keep exclusive disable/move as the default in `front-end/src/components/learning/DragDropQuestion.tsx` when `bankReusable` is false or undefined (must not regress after T005)
- [x] T010 [US2] Backend tests: `bankReusable: false` for exclusive canonical keys and for legacy positional `{ bank }` only, in `back-end/src/test/java/com/kuky/backend/learning/ExerciseGradingServiceTest.java`

**Checkpoint**: Existing exclusive homework take behaviour unchanged (FR-002/FR-009).

---

## Phase 5: User Story 3 - Grading and review accept the same word in several blanks (Priority: P2)

**Goal**: Submit with the same bank id in multiple placements is accepted; each blank scores against its own `correctBankIds`; results show the repeated word.

**Independent Test**: Shared-key question submitted with the same id in both blanks → both units correct; result passage shows that word twice ([quickstart.md](./quickstart.md) Scenario A submit/results).

### Implementation for User Story 3

- [x] T011 [US3] Add grading test: same placement id in two blanks scores both units correct when that id is in both `correctBankIds`, in `back-end/src/test/java/com/kuky/backend/learning/ExerciseGradingServiceTest.java` (do not reject duplicate ids)
- [x] T012 [P] [US3] Mirror T011 duplicate-placement grading assertion for activities in `back-end/src/test/java/com/kuky/backend/learning/` (activity grading test class if present; otherwise confirm `ActivityExerciseGradingService.gradeDragDrop` uses the same per-blank `contains` rule and add a focused test)
- [x] T013 [US3] Confirm `front-end/src/components/learning/MultiBlankResult.tsx` (used for DRAG_DROP in `ExerciseResult.tsx`) displays each blank’s `studentDisplay` even when labels repeat; change only if it currently assumes unique placements

**Checkpoint**: FR-006/FR-007; exclusive scoring unchanged for non-shared keys.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end verification across surfaces; no authoring/i18n drift.

- [x] T014 Run [quickstart.md](./quickstart.md) Scenarios A–E — verified via unit tests (strip reusable/exclusive, duplicate placement grading, activity parity) + code review of `DragDropQuestion` exclusive vs copy paths and `MixedHomeworkForm`/`ExerciseForm` wiring; interactive author→take in browser needs a logged-in student (login screen only; no credentials in session)
- [x] T015 [P] `npm run lint` in `front-end/` and `./gradlew test --tests '*ExerciseGrading*' --tests '*ActivityExercise*' --tests '*HomeworkExerciseAdmin*'` in `back-end/`
- [x] T016 [P] Confirm no authoring edits in `front-end/src/components/admin/homework/DragDropEditor.tsx` and no new strings in `front-end/src/i18n/locales/{en,es,ro}.ts`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS** all user stories (T003/T004 depend on T002)
- **US1 (Phase 3)**: After Foundational — MVP; T006/T007 after T005 prop exists
- **US2 (Phase 4)**: After T005 (same `DragDropQuestion.tsx`); T010 independent of T009 once T002–T003 exist
- **US3 (Phase 5)**: After Foundational; grading tests do not depend on the take UI. Result check T013 can run after US1
- **Polish (Phase 6)**: After desired stories complete

### User Story Dependencies

| Story | Depends on | Independently testable? |
|-------|------------|-------------------------|
| US1 Reusable take UX | Phase 2 | Yes — shared-id question, copy into two blanks |
| US2 Exclusive unchanged | T005 default branch | Yes — exclusive question, disable + move |
| US3 Duplicate grading/review | Phase 2 (grader already any-of) | Yes — submit same id twice; inspect units/results |

### Parallel Opportunities

- T003 / T004 after T002
- T006 / T007 after T005
- T011 / T012 after Phase 2
- T015 / T016 during or after T014

### Parallel Example: User Story 1

```text
# After T005 adds the DragDropQuestion prop:
Task: T006 [P] [US1] ExerciseForm.tsx pass bankReusable
Task: T007 [P] [US1] MixedHomeworkForm.tsx pass bankReusable
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1–2 (`bankReusable` on strip + types)
2. Complete Phase 3 (US1) — copy-on-place take UX
3. **STOP and VALIDATE** with quickstart Scenario A (take; submit/results may wait for US3 tests)
4. Demo if ready

### Incremental Delivery

1. Setup + Foundational → flag on student payload
2. US1 → reusable chips stay available → demo (main product gap)
3. US2 → exclusive regression → demo
4. US3 → duplicate placement grading + result labels → demo
5. Polish → full quickstart A–E

### Suggested MVP scope

**US1 only** (with Phase 2) ships the student-visible fix. US2 is the exclusive no-regression gate and should ship in the same PR. US3 is grader/result confirmation (likely little production code).

---

## Notes

- No Flyway; no authoring toggle; no new i18n ([research.md](./research.md))
- Quizzes inherit homework strip + `MixedHomeworkForm` — do not add a third take widget
- Commit after each task or logical group; stop at checkpoints to validate independently
