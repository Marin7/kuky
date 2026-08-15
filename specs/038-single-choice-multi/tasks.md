---
description: "Task list for multi-item opción única (numbered (1)…(N) markers)"
---

# Tasks: Multi-Item Opción Única

**Input**: Design documents from `specs/038-single-choice-multi/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/single-choice-multi-api.md](./contracts/single-choice-multi-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit for marker parse, validation, classic non-regression, grade expansion, incomplete submit, mixed finalize, activity parity (per [plan.md](./plan.md)). Frontend — browser verification via [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Shared frontend limits and TypeScript shapes every story uses. No Flyway.

- [x] T001 Add `MAX_SINGLE_CHOICE_ITEMS = 20` in `front-end/src/lib/exerciseLimits.ts`
- [x] T002 [P] Add numbered SINGLE_CHOICE structure types (`items[].number`, `items[].options` with `id`/`label`/`correct`) in `front-end/src/lib/admin.ts` per [contracts/single-choice-multi-api.md](./contracts/single-choice-multi-api.md)
- [x] T003 [P] Add student `structure.items` (no `correct`) and submit `answerJson.selections` map types in `front-end/src/lib/learning.ts` per [contracts/single-choice-multi-api.md](./contracts/single-choice-multi-api.md)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Marker parser and score-contribution expansion used by authoring, take, and grading. No user story work until this phase completes.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T004 Implement `SingleChoiceMarkerParser` in `back-end/src/main/java/com/kuky/backend/learning/service/SingleChoiceMarkerParser.java`: regex `\((\d+)\)`, `(01)`≡`(1)`, ignore non-numeric parentheses, distinct numbers must be exactly `{1…N}` with N in 1–20, item order numeric
- [x] T005 [P] Implement matching parse/validate helpers in `front-end/src/lib/singleChoiceMarkers.ts` (same rules as T004; reuse from `blankTokens.ts` style)
- [x] T006 Add `contributions(question, answer)` (or equivalent) on `HomeworkCompositionSupport` in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkCompositionSupport.java`: numbered SINGLE_CHOICE → N times 0/1; other kinds → one existing score; do **not** treat `answer.score` mean as a single contribution for numbered entries
- [x] T007 [P] Unit-test the parser in `back-end/src/test/java/com/kuky/backend/learning/service/SingleChoiceMarkerParserTest.java`: `(1)(2)(3)` valid; `(1)(3)` invalid; `(ser)` ignored; `(01)`≡`(1)`; N>20 invalid

**Checkpoint**: Parsers agree on 1…N; composition helper can expand item scores; types compile.

---

## Phase 3: User Story 1 - Teacher authors several pick-one items by numbering the text (Priority: P1) — MVP

**Goal**: Prompt `(1)`…`(N)` switches the editor to per-number option lists; save persists `structure_json.items` and zero options-table rows; no markers stays classic; first marker copies classic options onto `(1)`; gaps cannot save. Activities reuse the same editor/validation.

**Independent Test**: Author `(1)(2)(3)` with two options each, save, reopen intact; existing unmarked opción única still one list ([quickstart.md](./quickstart.md) §1–3, §6).

### Implementation for User Story 1

- [x] T008 [US1] Extend `validateAndMapQuestions` / `validateOptions` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`: if prompt has markers, require consecutive 1…N, each item ≥2 options and exactly one correct, persist `structure_json.items` and empty options; if no markers, classic options + `{}` (ignore stale `items`); first-marker copy and all-markers-removed restore per [data-model.md](./data-model.md)
- [x] T009 [US1] Create `front-end/src/components/admin/homework/SingleChoiceItemsEditor.tsx`: stacked per-number option rows (add/remove option, mark exactly one correct), synced to `structure.items`
- [x] T010 [US1] Wire prompt watching in `front-end/src/components/admin/homework/QuestionEditorCard.tsx`: valid/in-progress `(N)` → show `SingleChoiceItemsEditor` instead of classic option list; first `(1)` copies current `options` onto item 1; extra numbers start empty; removing all markers restores classic `options` from item 1
- [x] T011 [P] [US1] i18n prompt hint + sequence/options validation copy in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`
- [x] T012 [US1] Admin tests in `back-end/src/test/java/com/kuky/backend/admin/HomeworkExerciseAdminServiceTest.java` (and/or `HomeworkAdminServiceTest.java`): numbered save/reload; gap `(1)(3)` rejected; classic unmarked still requires one correct option; lone `(1)` is numbered mode

**Checkpoint**: Teacher can author numbered opción única on homework (and activities via shared `QuestionListEditor` / `validateAndMapQuestions`). FR-001–005, FR-010, FR-015–017.

---

## Phase 4: User Story 2 - Student answers by selecting a radio per numbered item (Priority: P1)

**Goal**: Student sees the authored prompt (markers visible) and one stacked radio group per number; submit blocked until every number has a selection; classic unmarked questions stay one radio group. Keys hidden pre-submit.

**Independent Test**: Open a `(1)(2)(3)` homework; three radio groups; empty group blocks submit; all selected stores per-number answers ([quickstart.md](./quickstart.md) §4).

### Implementation for User Story 2

- [x] T013 [US2] Strip numbered SINGLE_CHOICE structure for students in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` `stripStructureForStudent` / `buildStudentQuestions`: emit `items[].options` `{id,label}` only; `options` array empty; do not treat classic SINGLE_CHOICE as structured
- [x] T014 [US2] Reject submit when any numbered item lacks a selection (`VALIDATION_ERROR`) in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` (and mixed `HomeworkSubmissionService` / `HomeworkItems` submit path if that is the live PUT `/answers` entry)
- [x] T015 [US2] Render stacked `RadioGroup`s per `structure.items` in `front-end/src/components/learning/ExerciseForm.tsx`; keep classic `selectedOptionIds` path when `items` absent; submit `answerJson.selections`
- [x] T016 [P] [US2] Same stacked radios + selections payload in `front-end/src/components/learning/MixedHomeworkForm.tsx`
- [x] T017 [US2] Mirror student strip + incomplete-submit checks in `back-end/src/main/java/com/kuky/backend/learning/service/ActivityExerciseGradingService.java` and `back-end/src/main/java/com/kuky/backend/learning/service/ActivityStudentService.java`
- [x] T018 [US2] Tests: incomplete numbered submit rejected; student GET has no `correct` flags — `back-end/src/test/java/com/kuky/backend/learning/ExerciseGradingServiceTest.java` (and activity equivalent if present)

**Checkpoint**: Students can complete numbered drills by radio only. FR-006–007, FR-011 (take path).

---

## Phase 5: User Story 3 - Student and teacher see per-item auto-feedback (Priority: P1)

**Goal**: Each item auto-grades 0/1; wrong items reveal the correct label; overall `%` and fully-correct count treat each item as its own question (2/3 → 67%, 2 of 3). Mixed finalize and activities use the same expansion. Classic unmarked scores unchanged.

**Independent Test**: Only that three-item entry, 2 correct → 67% and 2 of 3; mixed with one FREE_TEXT → four equal parts ([quickstart.md](./quickstart.md) §5, §7).

### Implementation for User Story 3

- [x] T019 [US3] Grade numbered SINGLE_CHOICE in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java`: per-item 0/1 from `answerJson.selections` vs `structure.items`; fill `unitResults`; persist one answer row (`selections` + mean `score` snapshot); expand `structuredCount` / `fullyCorrect` / `scorePercent` via T006 (not the mean as one contribution)
- [x] T020 [US3] Use expanded contributions in mixed finalize in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` and `back-end/src/main/java/com/kuky/backend/admin/service/ActivityAdminService.java` (replace single `answer.getScore()` add for numbered SINGLE_CHOICE)
- [x] T021 [US3] Mirror numbered grading + expansion in `back-end/src/main/java/com/kuky/backend/learning/service/ActivityExerciseGradingService.java`
- [x] T022 [US3] Show per-item right/wrong + expected label on wrong items from `unitResults` in `front-end/src/components/learning/ExerciseResult.tsx` (and mixed/review surfaces that already render auto results)
- [x] T023 [P] [US3] Confirm teacher review of graded numbered entries shows each item + student radio in existing homework/activity review UI under `front-end/src/components/admin/` (extend only if the shared result DTO is not already rendered)
- [x] T024 [US3] Grading tests in `back-end/src/test/java/com/kuky/backend/learning/ExerciseGradingServiceTest.java`: 2 of 3 → 67% / fullyCorrect 2 / total 3; all correct → 100% / 3 of 3; classic SINGLE_CHOICE still 0 or 100 as one contribution; mixed 3 items + one other question = 4 contributions
- [x] T025 [P] [US3] Placement non-regression: no `(N)` item editors or structure in `front-end/src/components/placement/` and `back-end/src/main/java/com/kuky/backend/placement/` (grep; add a focused test only if a placement SINGLE_CHOICE test already exists to extend)

**Checkpoint**: FR-008–009, FR-012–014, SC-003; scoring matches clarification B.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Classic data untouched, i18n complete, quickstart in the browser.

- [x] T026 [P] Regression: existing unmarked opción única fixtures still load/grade in `back-end/src/test/java/com/kuky/backend/` (extend `HomeworkUpdatePreservesSubmissionsIntegrationTest` or `ExerciseGradingServiceTest` classic cases — FR-010 / SC-005)
- [ ] T027 Run [quickstart.md](./quickstart.md) browser scenarios §1–8 against local `:8080`/`:8081` (constitution: visual verification)
- [x] T028 [P] Add a one-line note to `CLAUDE.md` homework grading that numbered opción única `(1)`…`(N)` items each count as a full question (only if the shipped behavior matches the plan)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on T001–T003 types/limits — **BLOCKS** all user stories
- **US1 (Phase 3)**: After Phase 2 — MVP (authoring)
- **US2 (Phase 4)**: After Phase 2; needs US1 persist shape so student GET has `items` (T008)
- **US3 (Phase 5)**: After US2 submit payload exists (T014–T016)
- **Polish (Phase 6)**: After desired stories complete

### User Story Dependencies

- **US1 (P1)**: Foundation only — MVP
- **US2 (P1)**: Needs T008 numbered `structure_json` on save
- **US3 (P1)**: Needs US2 selections payload + US1 items key

### Parallel Opportunities

- T002 / T003 after T001
- T005 parallel with T004; T007 after T004
- T011 i18n parallel with T009–T010
- T015 / T016 student UI parallel after T013
- T021 activity grader parallel with T019 once helper T006 exists
- T025 placement grep parallel with T024

---

## Parallel Example: Phase 2

```bash
Task: "SingleChoiceMarkerParser.java (T004)"
Task: "singleChoiceMarkers.ts (T005)"
# Then T006 contributions helper, T007 parser tests
```

---

## Parallel Example: User Story 2

```bash
Task: "ExerciseForm stacked radios (T015)"
Task: "MixedHomeworkForm stacked radios (T016)"
# After T013 student strip
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (types + cap)
2. Complete Phase 2: Foundational (parser + contribution helper)
3. Complete Phase 3: US1 (author + save numbered opción única)
4. **STOP and VALIDATE** via quickstart §1–3, §6
5. Demo if ready

### Incremental Delivery

1. Setup + Foundational → parse/validate/score helpers ready
2. US1 → teacher can author `(1)(2)(3)` (MVP)
3. US2 → student radios + complete submit
4. US3 → per-item grade + expanded homework %
5. Polish → classic regression + full quickstart

### Parallel Team Strategy

1. Together: Phase 1–2
2. Then: Dev A US1 editor/validation; Dev B US2 student take after T008; Dev C US3 grading expansion after T006/T014

---

## Notes

- `[P]` tasks = different files, no dependencies on incomplete work
- `[US#]` maps to spec user stories
- Activities share `QuestionListEditor` + `HomeworkAdminService.validateAndMapQuestions` — do not duplicate an activity authoring editor
- Do not add Flyway; do not change placement `QuestionKind`
- Commit after each task or logical group
- Stop at checkpoints to validate independently
