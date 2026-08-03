---

description: "Task list for new homework exercise types"
---

# Tasks: New Exercise Types

**Input**: Design documents from `specs/024-new-exercise-types/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/exercise-types-api.md](contracts/exercise-types-api.md), [quickstart.md](quickstart.md)

**Tests**: Backend JUnit per [plan.md](plan.md) (validation + `ExerciseGradingService` for new kinds). Frontend has no unit-test framework — verify via [quickstart.md](quickstart.md) in a browser. Placement-test code is out of scope.

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US5)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Schema changes every other task depends on.

- [x] T001 Write `back-end/src/main/resources/db/migration/V6__new_exercise_types.sql` per [data-model.md](data-model.md): drop/replace `homework_questions.kind` CHECK to include `MULTI_BLANK`, `DRAG_DROP`, `TABLE_FILL`, `MATCHING`; add `structure_json JSONB NOT NULL DEFAULT '{}'`; add `homework_answers.answer_json JSONB NULL`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared kind enum, JSON persistence, blank parser, DTO/API scaffolding, and frontend type extensions that all new kinds need.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Extend `back-end/src/main/java/com/kuky/backend/learning/model/QuestionKind.java` with `MULTI_BLANK`, `DRAG_DROP`, `TABLE_FILL`, `MATCHING` (do **not** change placement `QuestionKind`).
- [x] T003 [P] Add `structureJson` (String or JsonNode) to `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkQuestion.java` and `answerJson` to `HomeworkAnswer.java`.
- [x] T004 Update `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkQuestionRepository.java` to read/write `structure_json` on find/replace; keep options rows only for legacy kinds. Depends on T001, T003.
- [x] T005 Update `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkAnswerRepository.java` to read/write `answer_json`. Depends on T001, T003.
- [x] T006 [P] Create `back-end/src/main/java/com/kuky/backend/learning/service/BlankPassageParser.java` — count/split exact `___` tokens via `(?<!_)___(?!_)` per [research.md](research.md).
- [x] T007 Extend admin DTO `back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkQuestionDto.java` with `structure` (JsonNode/Map) per [contracts/exercise-types-api.md](contracts/exercise-types-api.md); map to/from `structureJson` in `HomeworkAdminService` load/save paths (validation stubs OK — per-kind rules in story phases).
- [x] T008 Extend student DTOs: `ExerciseQuestionDto` with stripped `structure`; `SubmitExerciseRequest.AnswerDto` with `answerJson`; `ExerciseResultResponse` / `QuestionResultDto` with `unitResults` per contract. Wire pass-through in `ExerciseGradingService.getExercise` / `submit` without breaking legacy kinds (new kinds may return VALIDATION_ERROR until story work). Depends on T004, T005.
- [x] T009 [P] Extend `QuestionKind` and `AdminQuestion` (`structure?`) in `front-end/src/lib/admin.ts`.
- [x] T010 [P] Extend `QuestionKind`, student question `structure`, submit `answerJson`, and `UnitResult` / `QuestionResult` in `front-end/src/lib/learning.ts`.
- [x] T011 [P] Add i18n kind labels + shared validation strings for the four new kinds in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts` under `admin.homework.questions.kind.*` (and any take-exercise copy keys needed).

**Checkpoint**: Migration applied; enum/DTOs/repos/parser ready; FE types know the new kinds. Story-specific validation, grading, and UI still TODO.

---

## Phase 3: User Story 1 - Multi-blank passage (Priority: P1) 🎯 MVP

**Goal**: Teacher authors one passage with ≥2 `___` blanks and per-blank accepted answers; student types into all blanks in one view; auto-grade with per-blank feedback and overall score.

**Independent Test**: Create EXERCISE with one MULTI_BLANK (4 blanks), assign student, submit answers, see per-blank reveal + %; one-`___` passage cannot save.

### Tests for User Story 1

- [x] T012 [P] [US1] Add/extend backend tests (e.g. `ExerciseGradingService` / admin validation test class under `back-end/src/test/java/com/kuky/backend/`) for MULTI_BLANK: accent-exact match, partial blanks → fractional question score, `<2` blanks rejected, blank/structure length mismatch rejected.

### Implementation for User Story 1

- [x] T013 [US1] In `HomeworkAdminService.validateAndMapQuestions` / `validateOptions`, validate MULTI_BLANK per [data-model.md](data-model.md) (2–20 blanks, `___` count matches `structure.blanks`, ≥1 accepted answer each, `options` empty). Depends on T006, T007.
- [x] T014 [US1] In `ExerciseGradingService`, grade MULTI_BLANK from `answer_json.blanks[]`, strip acceptedAnswers from student structure, populate `unitResults` + question score average. Depends on T006, T008.
- [x] T015 [P] [US1] Create `front-end/src/components/admin/homework/MultiBlankEditor.tsx` — prompt textarea with `___` help, dynamic accepted-answer lists synced to parsed blank count.
- [x] T016 [US1] Wire MULTI_BLANK into `QuestionListEditor.tsx` / `QuestionEditorCard.tsx` (kind select + `MultiBlankEditor`). Depends on T009, T011, T015.
- [x] T017 [P] [US1] Create `front-end/src/components/learning/MultiBlankQuestion.tsx` — render passage segments with inputs; collect `blanks[]` for submit.
- [x] T018 [US1] Integrate MULTI_BLANK into `ExerciseForm.tsx` (answerJson submit) and `ExerciseResult.tsx` (per-unit feedback). Depends on T010, T017.

**Checkpoint**: MULTI_BLANK author → take → grade → review works end-to-end. MVP deliverable.

---

## Phase 4: User Story 2 - Drag-and-drop word bank (Priority: P2)

**Goal**: Teacher authors `___` passage + bank (length = blanks, correct by blank order); student places bank items via click-to-place (+ optional HTML5 drag); shuffled bank; ID-based grading.

**Independent Test**: Author 3 blanks + 3 bank labels in blank order; student sees shuffled bank, places all, submits; wrong placement reveals expected label; bank≠blanks cannot save.

### Tests for User Story 2

- [x] T019 [P] [US2] Backend tests for DRAG_DROP: correct by bank id/index, wrong placement 0, bank length mismatch rejected, `<2` blanks rejected.

### Implementation for User Story 2

- [x] T020 [US2] Validate DRAG_DROP in `HomeworkAdminService` (blank count 2–20, bank length equals blanks, unique ids, non-empty labels, empty options). Depends on T006, T007.
- [x] T021 [US2] Grade DRAG_DROP in `ExerciseGradingService` from `answer_json.placements[]`; student DTO exposes `bank` without answer key; `unitResults` reveal expected label when wrong. Depends on T008.
- [x] T022 [P] [US2] Create `front-end/src/components/admin/homework/DragDropEditor.tsx` — passage + ordered bank inputs (N synced to `___` count).
- [x] T023 [US2] Wire DRAG_DROP into `QuestionEditorCard.tsx` / `QuestionListEditor.tsx`. Depends on T011, T022.
- [x] T024 [P] [US2] Create `front-end/src/components/learning/DragDropQuestion.tsx` — client shuffle bank; click-to-place + keyboard path; optional HTML5 drag; emit `placements[]`.
- [x] T025 [US2] Integrate DRAG_DROP into `ExerciseForm.tsx` and `ExerciseResult.tsx`. Depends on T010, T024.

**Checkpoint**: DRAG_DROP works independently; MULTI_BLANK still works.

---

## Phase 5: User Story 3 - Table fill (Priority: P2)

**Goal**: Teacher authors a conjugation-style grid (fixed vs blank cells); student types into blank cells; per-cell auto-grade with accent rules.

**Independent Test**: Author 3×1 table with blank cells + answers; student fills and submits; zero-blank table cannot save.

### Tests for User Story 3

- [x] T026 [P] [US3] Backend tests for TABLE_FILL: per-cell normalize grading, missing cell → 0, no blanks / oversize limits rejected.

### Implementation for User Story 3

- [x] T027 [US3] Validate TABLE_FILL in `HomeworkAdminService` (rectangular cells, 1–50 blanks, limits, acceptedAnswers on blanks, empty options). Depends on T007.
- [x] T028 [US3] Grade TABLE_FILL in `ExerciseGradingService` from `answer_json.cells`; strip acceptedAnswers for students; `unitResults` by blank `(r,c)` order. Depends on T008.
- [x] T029 [P] [US3] Create `front-end/src/components/admin/homework/TableFillEditor.tsx` — row/col headers, cell type toggle, accepted answers for blanks.
- [x] T030 [US3] Wire TABLE_FILL into `QuestionEditorCard.tsx` / `QuestionListEditor.tsx`. Depends on T011, T029.
- [x] T031 [P] [US3] Create `front-end/src/components/learning/TableFillQuestion.tsx` — table layout with inputs on blanks.
- [x] T032 [US3] Integrate TABLE_FILL into `ExerciseForm.tsx` and `ExerciseResult.tsx`. Depends on T010, T031.

**Checkpoint**: TABLE_FILL works independently alongside prior kinds.

---

## Phase 6: User Story 4 - Matching (Priority: P3)

**Goal**: Teacher defines left/right lists (unequal OK) + correct pairs; student pairs via click-to-pair; lists shuffled; distractors allowed; per-pair grading.

**Independent Test**: Author 3 left / 4 right with 3 pairs; student pairs and submits; unpaired expected left scores 0; no pairs cannot save.

### Tests for User Story 4

- [x] T033 [P] [US4] Backend tests for MATCHING: correct pairs, distractor misuse → 0, missing expected pair → 0, validation for dangling ids / empty pairs.

### Implementation for User Story 4

- [x] T034 [US4] Validate MATCHING in `HomeworkAdminService` (1–20 per side, ≥1 pair, unique pair endpoints, empty options). Depends on T007.
- [x] T035 [US4] Grade MATCHING in `ExerciseGradingService` from `answer_json.pairs`; student DTO has left/right only (no `pairs`); `unitResults` per authored pair order. Depends on T008.
- [x] T036 [P] [US4] Create `front-end/src/components/admin/homework/MatchingEditor.tsx` — left/right lists + pair picker.
- [x] T037 [US4] Wire MATCHING into `QuestionEditorCard.tsx` / `QuestionListEditor.tsx`. Depends on T011, T036.
- [x] T038 [P] [US4] Create `front-end/src/components/learning/MatchingQuestion.tsx` — shuffle both sides; click-to-pair / clear; keyboard accessible; emit pairs.
- [x] T039 [US4] Integrate MATCHING into `ExerciseForm.tsx` and `ExerciseResult.tsx`. Depends on T010, T038.

**Checkpoint**: All four new kinds authorable and takeable.

---

## Phase 7: User Story 5 - Existing exercise kinds still work (Priority: P3)

**Goal**: Legacy SINGLE_CHOICE / MULTI_CHOICE / FILL_BLANK and placement test remain unchanged; mixing legacy + new kinds in one EXERCISE works.

**Independent Test**: Take a pre-existing options-based exercise; scores match old behaviour. Placement admin still only shows three kinds. One homework mixes FILL_BLANK + MULTI_BLANK successfully.

### Implementation for User Story 5

- [x] T040 [US5] Confirm legacy paths in `HomeworkAdminService` / `ExerciseGradingService` ignore empty `structure_json` / null `answer_json`; add a focused regression test for FILL_BLANK + SINGLE_CHOICE grading unchanged under `back-end/src/test/java/com/kuky/backend/`.
- [x] T041 [P] [US5] Smoke-check placement `QuestionKind` / placement admin UI still lists only three kinds (no accidental shared-type pollution in `front-end` placement components).
- [x] T042 [US5] Browser-verify mixed exercise (legacy + new kind) submit/lock/review on `/aprendizaje` per [quickstart.md](quickstart.md) §5–6.

**Checkpoint**: No regressions; mixed exercises OK.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end validation and cleanup across kinds.

- [x] T043 Run full [quickstart.md](quickstart.md) scenarios (all four kinds + regression) against local `:8080` / `:8081`.
- [x] T044 [P] Lint/format touched frontend files (`npm run lint` / `npm run format` in `front-end/`) and ensure backend `./gradlew test` green for new/updated tests.
- [x] T045 [P] Review i18n completeness for new kind labels and validation messages in `es.ts` / `en.ts` / `ro.ts`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Start immediately (migration).
- **Phase 2 (Foundational)**: Depends on T001 — blocks all user stories.
- **Phase 3 (US1)**: After Phase 2 — MVP.
- **Phase 4–6 (US2–US4)**: After Phase 2; ideally after US1 patterns exist, but independently testable.
- **Phase 7 (US5)**: After at least US1 (preferably all new kinds).
- **Phase 8 (Polish)**: After desired stories complete.

### User Story Dependencies

| Story | Depends on | Notes |
|-------|------------|--------|
| US1 MULTI_BLANK | Phase 2 | No other stories |
| US2 DRAG_DROP | Phase 2 (+ BlankPassageParser from foundation) | Reuses passage parsing; independent of US1 UI |
| US3 TABLE_FILL | Phase 2 | Independent |
| US4 MATCHING | Phase 2 | Independent |
| US5 Regression | US1+ recommended | Verifies coexistence |

### Parallel Opportunities

- T002, T003, T006, T009, T010, T011 in Phase 2 (after T001 for DB-dependent work).
- Within each story: editor component [P] alongside student component [P] after backend validation/grading for that kind.
- US2 / US3 / US4 can proceed in parallel after Phase 2 if staffed (watch merge conflicts on `QuestionEditorCard.tsx` / `ExerciseForm.tsx` / `ExerciseGradingService.java`).

---

## Parallel Example: User Story 1

```text
# After T013–T014 (backend MULTI_BLANK ready):
Task: "Create MultiBlankEditor.tsx"
Task: "Create MultiBlankQuestion.tsx"
# Then sequentially wire into QuestionEditorCard / ExerciseForm / ExerciseResult
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 + Phase 2  
2. Phase 3 (MULTI_BLANK)  
3. **STOP** — validate quickstart §1  
4. Demo / ship MVP  

### Incremental Delivery

1. Foundation ready  
2. US1 MULTI_BLANK → demo  
3. US2 DRAG_DROP → demo  
4. US3 TABLE_FILL → demo  
5. US4 MATCHING → demo  
6. US5 + polish  

### Parallel Team Strategy

1. Shared: Phase 1–2  
2. Then: Dev A US1 (or finish US1 first as pattern), Dev B US3, Dev C US4; DRAG_DROP shares passage parser with US1  

---

## Notes

- Do not extend placement-test enums, tables, or UI.
- No new npm/Java dependencies — click-to-place + optional HTML5 drag only.
- Exact blank token: three underscores not adjacent to another underscore.
- Word bank: authored order = correct blank order; student shuffle is client-only.
- Commit after each task or logical group; keep `QuestionEditorCard` / `ExerciseForm` switches exhaustive for all seven kinds when done.
