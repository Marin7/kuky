---
description: "Task list for multiple correct blank answers"
---

# Tasks: Multiple Correct Blank Answers

**Input**: Design documents from `specs/034-multi-correct-blanks/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/multi-correct-blanks-api.md](./contracts/multi-correct-blanks-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit for validation caps, DRAG_DROP any-of + legacy dual-read, MULTI_BLANK/DRAG_DROP multi-accepted reveal, activity grading parity (per [plan.md](./plan.md)). Frontend — browser verification via [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Shared limits used by validation, grading, and UI (no Flyway).

- [x] T001 Add shared blank/bank limit constants (max 10 accepted/correct per blank; max 30 bank items) in `back-end/src/main/java/com/kuky/backend/learning/` (e.g. `ExerciseStructureLimits.java`) and mirror numeric caps in `front-end/src/lib/` (e.g. `exerciseLimits.ts` or next to `blankTokens`) per [data-model.md](./data-model.md)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Types + dual-read helper so US1–US3 share one DRAG_DROP model. No user story work until this phase completes.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Extend `DragDropStructure` in `front-end/src/lib/admin.ts` with `blanks: { correctBankIds: string[] }[]` (keep `bank`); document legacy positional meaning in a short comment per [contracts/multi-correct-blanks-api.md](./contracts/multi-correct-blanks-api.md)
- [x] T003 [P] Implement DRAG_DROP dual-read helper in `back-end/src/main/java/com/kuky/backend/learning/` (e.g. `DragDropStructureSupport.java`): resolve blank count + per-blank `correctBankIds` from canonical `blanks[]` **or** legacy `bank[i]→blank i`; used by admin validation and both graders
- [x] T004 Confirm student strip for DRAG_DROP still exposes only `bank` (no `blanks` / keys) in `ExerciseGradingService.stripStructureForStudent` and the activity equivalent under `back-end/src/main/java/com/kuky/backend/learning/service/` — adjust only if canonical `blanks` would leak

**Checkpoint**: Frontend types accept canonical DRAG_DROP; backend can resolve correct ids for legacy and new shapes; student payloads still hide keys.

---

## Phase 3: User Story 1 - Author alternate answers for typed fill-in gaps (Priority: P1) — MVP

**Goal**: Teachers can author 1–10 accepted answers per MULTI_BLANK blank; save rejects empty/over-cap lists; student matching any accepted answer still grades correct (existing grader + cap enforcement).

**Independent Test**: Author MULTI_BLANK with two accepted answers on one blank; student submits the non-primary accepted answer → blank correct ([quickstart.md](./quickstart.md) Scenario A steps 1–5 grading part; feedback always-show is US3).

### Implementation for User Story 1

- [x] T005 [US1] Enforce 1–10 accepted answers (trim; silent dedupe by normalize key keeping first) in `normalizeAnswerList` / `validateMultiBlank` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` using T001 limits — do **not** change TABLE_FILL caps
- [x] T006 [US1] Cap add-answer UI at 10 and surface validation-friendly copy in `front-end/src/components/admin/homework/MultiBlankEditor.tsx` (disable add at max; keep ≥1 answer)
- [x] T007 [P] [US1] i18n for max-accepted-answers / blank hint strings in `front-end/src/i18n/locales/en.ts`, `es.ts`, `ro.ts`
- [x] T008 [US1] Backend tests for MULTI_BLANK accepted-answer caps and dedupe in `back-end/src/test/java/com/kuky/backend/admin/HomeworkExerciseAdminServiceTest.java` (or `HomeworkAdminServiceTest.java`)

**Checkpoint**: Typed gaps support up to 10 alternates with server+UI enforcement; FR-001/FR-002 authoring side ready (always-show feedback deferred to US3).

---

## Phase 4: User Story 2 - Author multiple correct placements for word-bank blanks (Priority: P1)

**Goal**: DRAG_DROP uses explicit `blanks[].correctBankIds` (any-of grading); bank size blankCount–30 with pure distractors; same bank id not correct on two blanks; legacy positional banks still grade equivalently; activity authoring inherits shared validation.

**Independent Test**: Author DRAG_DROP where one blank accepts two bank items (+ optional distractor); student places either accepted item → blank correct; legacy 1:1 bank still scores as before ([quickstart.md](./quickstart.md) Scenarios B–D).

### Implementation for User Story 2

- [x] T009 [US2] Rewrite `validateDragDrop` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` per [data-model.md](./data-model.md): accept canonical `bank`+`blanks.correctBankIds` or normalize legacy `{bank}` with `bank.length===blankCount`; enforce bank size, 1–10 ids/blank, unique ids across blanks, ids ∈ bank; persist canonical shape
- [x] T010 [US2] Update `gradeDragDrop` in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` to use T003 dual-read and mark blank correct iff placed id ∈ `correctBankIds` (blank count = `___` count, not `bank.length`)
- [x] T011 [P] [US2] Mirror `gradeDragDrop` any-of + dual-read in `back-end/src/main/java/com/kuky/backend/learning/service/ActivityExerciseGradingService.java`
- [x] T012 [US2] Redesign `front-end/src/components/admin/homework/DragDropEditor.tsx`: free bank add/remove/edit (≤30); per-blank multi-select of correct bank items (1–10); prevent selecting the same bank item on two blanks; stop forcing `bank.length === blankCount`
- [x] T013 [US2] Ensure student take still works when `bank.length > blankCount` in `front-end/src/components/learning/DragDropQuestion.tsx` (placement UX unchanged; unused chips stay in bank)
- [x] T014 [P] [US2] i18n for word-bank authoring (correct items per blank, distractors, bank limits) in `front-end/src/i18n/locales/en.ts`, `es.ts`, `ro.ts`
- [x] T015 [US2] Backend tests: canonical any-of grading, distractor incorrect, legacy positional equivalence, validation failures (shared id, bank>30, empty correct set) in `back-end/src/test/java/com/kuky/backend/learning/ExerciseGradingServiceTest.java` and admin test class under `back-end/src/test/java/com/kuky/backend/admin/`

**Checkpoint**: Word-bank multi-correct + distractors work end-to-end for homework; activities grade correctly; FR-003–FR-006, FR-008–FR-009 covered for authoring/grading (full always-show reveal is US3).

---

## Phase 5: User Story 3 - Student feedback shows all accepted alternatives (Priority: P2)

**Goal**: After grading, MULTI_BLANK and DRAG_DROP units with multiple accepted answers/correct ids always populate `expectedDisplay` with the full set (correct or incorrect); UI shows it; single-accepted behaviour unchanged; TABLE_FILL untouched.

**Independent Test**: Correct and incorrect multi-accepted blanks both show every accepted label in results ([quickstart.md](./quickstart.md) Scenario A feedback + Scenario B feedback).

### Implementation for User Story 3

- [x] T016 [US3] Update `gradeMultiBlank` in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` so when `acceptedAnswers.size() > 1`, `expectedDisplay` is always the full accepted list; size == 1 keeps reveal-on-incorrect-only
- [x] T017 [US3] Update `gradeDragDrop` in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` so when `correctBankIds.size() > 1`, `expectedDisplay` is always all correct labels; size == 1 keeps reveal-on-incorrect-only
- [x] T018 [P] [US3] Mirror T016–T017 reveal rules in `back-end/src/main/java/com/kuky/backend/learning/service/ActivityExerciseGradingService.java`
- [x] T019 [US3] Show `expectedDisplay` whenever non-empty (not only when `!correct`) in `front-end/src/components/learning/MultiBlankResult.tsx`; leave `TableFillResult.tsx` unchanged
- [x] T020 [US3] Adjust any shared expected-answer rendering in `front-end/src/components/learning/ExerciseResult.tsx` only if MULTI_BLANK/DRAG_DROP paths bypass `MultiBlankResult` and still hide expected on correct
- [x] T021 [P] [US3] i18n tweak if “also accepted” / unit-expected copy needs clarity for correct blanks in `front-end/src/i18n/locales/en.ts`, `es.ts`, `ro.ts`
- [x] T022 [US3] Backend tests asserting multi-accepted units include full `expectedDisplay` when correct and incorrect in `back-end/src/test/java/com/kuky/backend/learning/ExerciseGradingServiceTest.java` (and activity grading test if present)

**Checkpoint**: FR-007 / SC-003 satisfied for fill-in-gaps and word-bank; FR-010 table-fill no-regression.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end verification and safety checks.

- [ ] T023 Run [quickstart.md](./quickstart.md) Scenarios A–F in browser (homework + activity + table-fill unchanged)
- [x] T024 [P] Confirm TABLE_FILL validation/grading/reveal paths in `HomeworkAdminService` / `ExerciseGradingService` / `TableFillResult.tsx` were not modified for max-10 or always-show
- [x] T025 [P] `npm run lint` in `front-end/` and `./gradlew test` focused suites under `back-end/` for admin + exercise grading

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS** all user stories
- **US1 (Phase 3)**: After Foundational — MVP; no dependency on US2/US3
- **US2 (Phase 4)**: After Foundational — can run in parallel with US1 if staffed (different primary files); shares graders with US3 so finish US2 grading before US3 reveal edits on the same methods, or apply reveal in the same PR after any-of works
- **US3 (Phase 5)**: After US1 (for MULTI_BLANK reveal) and US2 (for DRAG_DROP reveal) recommended; can start MULTI_BLANK reveal after US1 alone
- **Polish (Phase 6)**: After desired stories complete

### User Story Dependencies

| Story | Depends on | Independently testable? |
|-------|------------|-------------------------|
| US1 Typed alternates | Phase 2 | Yes — MULTI_BLANK author + take |
| US2 Word-bank multi-correct | Phase 2 | Yes — DRAG_DROP author + take (legacy + new) |
| US3 Always-show feedback | US1 and/or US2 for data to display | Yes — once multi-accepted units exist |

### Parallel Opportunities

- T002 / T003 after T001
- T007 with T005–T006; T011 with T010; T014 with T012–T013; T018 with T016–T017; T021 with T019–T020
- US1 and US2 frontend work can proceed in parallel; coordinate on shared grader files for US3

### Parallel Example: User Story 2

```text
# After T009 lands validation:
Task: T010 gradeDragDrop in ExerciseGradingService.java
Task: T011 [P] mirror in ActivityExerciseGradingService.java

# Frontend (after T002 types):
Task: T012 DragDropEditor.tsx
Task: T013 DragDropQuestion.tsx
Task: T014 [P] i18n locales
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1–2
2. Complete Phase 3 (US1) — typed max-10 alternates
3. **STOP and VALIDATE** with quickstart Scenario A (grading; feedback may still be incorrect-only until US3)
4. Demo if ready

### Incremental Delivery

1. Setup + Foundational → types/helpers ready
2. US1 → typed alternates capped → demo
3. US2 → word-bank multi-correct + distractors + legacy safe → demo (main product gap)
4. US3 → always-show full accepted set → demo
5. Polish → full quickstart A–F

### Suggested MVP scope

**US1 only** for a minimal ship; **US1+US2** for the user-visible feature complete (word bank was the main gap); US3 is the pedagogical feedback polish.

---

## Notes

- No Flyway migration — dual-read only ([research.md](./research.md))
- Do not change TABLE_FILL behaviour (clarification / FR-010)
- Activities inherit admin validation via `HomeworkAdminService.validateAndMapQuestions`; still mirror **grading** in `ActivityExerciseGradingService`
- Commit after each task or logical group; stop at checkpoints to validate independently
