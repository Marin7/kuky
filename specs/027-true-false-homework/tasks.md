---
description: "Task list for true/false homework exercise kind"
---

# Tasks: True/False Homework Exercises

**Input**: Design documents from `specs/027-true-false-homework/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/true-false-api.md](contracts/true-false-api.md), [quickstart.md](quickstart.md)

**Tests**: Backend JUnit per [plan.md](plan.md) (admin validation + `ExerciseGradingService` for TRUE_FALSE). Frontend has no unit-test framework - verify via [quickstart.md](quickstart.md) in a browser. Placement-test code is out of scope.

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1-US2)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Schema change every other task depends on.

- [x] T001 Write `back-end/src/main/resources/db/migration/V9__true_false_homework.sql` per [data-model.md](data-model.md): drop/replace `homework_questions.kind` CHECK to include `TRUE_FALSE` alongside existing kinds.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared enum, FE type unions, and i18n labels that both user stories need.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Extend `back-end/src/main/java/com/kuky/backend/learning/model/QuestionKind.java` with `TRUE_FALSE` (do **not** change placement `QuestionKind`; keep `isStructured()` false for TRUE_FALSE).
- [x] T003 [P] Add `"TRUE_FALSE"` to `QuestionKind` in `front-end/src/lib/admin.ts` and `front-end/src/lib/learning.ts`.
- [x] T004 [P] Add i18n for kind label + true/false option labels in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts` under `admin.homework.questions.kind.TRUE_FALSE` and student-facing true/false label keys per [contracts/true-false-api.md](contracts/true-false-api.md).

**Checkpoint**: Migration ready; enum/FE types/i18n know TRUE_FALSE. Validation, grading, and UI still TODO.

---

## Phase 3: User Story 1 - Author and take a true/false exercise (Priority: P1) - MVP

**Goal**: Teacher authors TRUE_FALSE questions (plain prompt + exactly one of true/false correct); student selects true or false in fixed order; auto-grade with reveal when wrong; no explanation field.

**Independent Test**: Create EXERCISE with >=3 TRUE_FALSE questions, assign student, submit answers, see per-question feedback + overall %; empty prompt or missing correct answer cannot save; unanswered scores 0.

### Tests for User Story 1

- [x] T005 [P] [US1] Add/extend backend tests under `back-end/src/test/java/com/kuky/backend/` (e.g. `HomeworkExerciseAdminServiceTest` / `ExerciseGradingServiceTest`) for TRUE_FALSE: exactly 2 options with labels `true`/`false`, exactly one correct, empty prompt rejected, wrong option count rejected; grade correct=1, wrong=0, unanswered=0; `correctOptionIds` on wrong.

### Implementation for User Story 1

- [x] T006 [US1] In `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`, validate TRUE_FALSE in `validateOptions` / `validateAndMapQuestions` per [data-model.md](data-model.md) and [contracts/true-false-api.md](contracts/true-false-api.md) (non-empty prompt; exactly 2 options; labels `true` then `false`; exactly one correct; empty `structure`). Depends on T001, T002.
- [x] T007 [US1] In `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java`, treat TRUE_FALSE like single-choice: include in `gradeQuestion` dispatch (reuse `gradeSingleChoice`), `buildStudentQuestions` `hasOptions`, and `correctOptionIds` gates so students get two options pre-submit and key reveal post-submit. Depends on T002.
- [x] T008 [P] [US1] Extend `front-end/src/components/admin/homework/questionDefaults.ts` - `defaultOptionsForKind` returns two fixed options `{ label: "true", correct: false }` then `{ label: "false", correct: false }` for TRUE_FALSE.
- [x] T009 [US1] In `front-end/src/components/admin/homework/QuestionEditorCard.tsx`, add `TRUE_FALSE` to `KINDS` and a dedicated editor branch: prompt field + radio to mark which of the two fixed answers is correct (no add/remove/reorder options); map UI to canonical option labels. Depends on T003, T004, T008.
- [x] T010 [US1] In `front-end/src/components/learning/ExerciseForm.tsx`, render TRUE_FALSE like SINGLE_CHOICE radio group using localized true/false labels in fixed order; submit via `selectedOptionIds`. Depends on T003, T004.
- [x] T011 [US1] Confirm `front-end/src/components/learning/ExerciseResult.tsx` shows correct/incorrect + `correctOptionIds` for TRUE_FALSE without a new branch (fix only if choice-result path skips unknown kinds). Depends on T007, T010.

**Checkpoint**: TRUE_FALSE author -> take -> grade -> review works end-to-end. MVP deliverable.

---

## Phase 4: User Story 2 - Mix true/false with existing exercise kinds (Priority: P2)

**Goal**: TRUE_FALSE can sit in the same EXERCISE as other auto-gradable kinds; existing homeworks without TRUE_FALSE keep working; placement test unchanged.

**Independent Test**: One EXERCISE with TRUE_FALSE + SINGLE_CHOICE (or another existing kind); student submits both graded correctly; reopen a legacy-only EXERCISE and confirm unchanged behaviour; placement admin has no TRUE_FALSE kind.

### Implementation for User Story 2

- [x] T012 [US2] Smoke-verify mixed exercise path: admin can add TRUE_FALSE alongside an existing kind in `QuestionEditorCard.tsx` / save via existing admin homework API; student `ExerciseForm.tsx` submits mixed `selectedOptionIds` / other payloads in one submit. Depends on US1 checkpoint (T006-T011).
- [x] T013 [P] [US2] Regression check: existing kinds still validate/grade in `HomeworkAdminService` / `ExerciseGradingService` (no accidental switch fall-through); confirm placement `QuestionKind` and placement admin UI unchanged.
- [x] T014 [US2] Browser-validate [quickstart.md](quickstart.md) sections 2-3 (mixed exercise + legacy/placement regression) on `:8080` / `:8081`. Depends on T012, T013.

**Checkpoint**: Mixed exercises and legacy/placement behaviour verified.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and docs hygiene.

- [x] T015 [P] Run backend tests: `cd back-end && ./gradlew test --tests '*ExerciseGrading*' --tests '*HomeworkAdmin*'`.
- [x] T016 Run full [quickstart.md](quickstart.md) browser pass (P1 author/take + fail checks + P2 mix/regression).
- [x] T017 [P] Ensure CLAUDE.md homework formats line mentions TRUE_FALSE among auto-graded kinds if that inventory is maintained (optional doc sync only; no behaviour change).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - start immediately
- **Foundational (Phase 2)**: Depends on Setup (T001) for DB acceptance of the kind; T002-T004 can proceed in parallel once planned
- **User Story 1 (Phase 3)**: Depends on Foundational (T002-T004) + migration (T001)
- **User Story 2 (Phase 4)**: Depends on US1 checkpoint (full TRUE_FALSE path)
- **Polish (Phase 5)**: Depends on US1 + US2 desired completion

### User Story Dependencies

- **User Story 1 (P1)**: After Foundational - no dependency on US2
- **User Story 2 (P2)**: After US1 - mixing/regression only makes sense once TRUE_FALSE works alone

### Within User Story 1

- Tests T005 can start in parallel with implementation but should fail until T006-T007 land
- Backend validation (T006) and grading (T007) before relying on FE end-to-end
- Defaults (T008) before editor (T009); FE types/i18n before form/result (T010-T011)

### Parallel Opportunities

- T002, T003, T004 in parallel after T001 is written (FE types do not need migration applied locally to compile)
- T005 with early T006/T007 stubs
- T008 parallel with T006/T007
- T013 parallel with T012 once US1 is done

---

## Parallel Example: User Story 1

```bash
# After foundational:
Task: "Backend TRUE_FALSE validation/grading tests"
Task: "questionDefaults.ts defaultOptionsForKind for TRUE_FALSE"
Task: "HomeworkAdminService TRUE_FALSE validation"
Task: "ExerciseGradingService TRUE_FALSE dispatch / hasOptions / correctOptionIds"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (V9 migration)
2. Complete Phase 2: Foundational (enum + FE types + i18n)
3. Complete Phase 3: User Story 1 (validate, grade, author, take, result)
4. **STOP and VALIDATE**: Author >=3 TRUE_FALSE questions, student take + submit, wrong/unanswered cases
5. Demo if ready

### Incremental Delivery

1. Setup + Foundational -> foundation ready
2. Add US1 -> MVP true/false homework
3. Add US2 -> mixed + regression confidence
4. Polish -> quickstart + test suite green

### Parallel Team Strategy

With two developers after Foundational:

- Developer A: Backend T005-T007
- Developer B: Frontend T008-T011
- Together: US2 browser validation

---

## Notes

- [P] tasks = different files, no dependencies on incomplete sibling work
- TRUE_FALSE reuses options / `selectedOptionIds` - do not add `structure_json` / `answer_json` shapes
- Canonical stored option labels are `true` / `false`; UI shows localized strings
- Exercise prompts stay plain TEXT (parity with other exercise kinds - see [research.md](research.md))
- Commit after each task or logical group
- Stop at US1 checkpoint for MVP
