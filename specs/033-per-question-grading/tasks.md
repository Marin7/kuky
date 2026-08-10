---
description: "Task list for per-question homework grading"
---

# Tasks: Per-Question Homework Grading

**Input**: Design documents from `specs/033-per-question-grading/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/per-question-grading-api.md](./contracts/per-question-grading-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit for authoring mix, submit paths, combined score, finalize validations, activity parity, legacy MANUAL/EXERCISE (per [plan.md](./plan.md)). Frontend â€” browser verification via [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1â€“US4)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Schema every story depends on (`MIXED` format + `teacher_validation`).

- [x] T001 Write `back-end/src/main/resources/db/migration/V17__per_question_grading.sql` per [data-model.md](./data-model.md): add nullable `teacher_validation` VARCHAR(16) CHECK (`NULL` | `VALIDATED` | `INVALIDATED`) on `homework_answers` and `activity_answers`; replace `format` CHECK on `homework_assignments` and `activities` to include `MIXED`; backfill `format='MIXED'` where both FREE_TEXT and structured questions exist

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared enums, composition derivation, DTO/repo plumbing so authoring/submit/review stories share one model. No user story work until this phase completes.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Add `MIXED` to `HomeworkFormat` in `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkFormat.java`
- [x] T003 [P] Add `HomeworkComposition` enum (`WRITE`, `ALL_MANUAL`, `ALL_AUTO`, `MIXED`) in `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkComposition.java`
- [x] T004 [P] Add `TeacherValidation` enum (`VALIDATED`, `INVALIDATED`) in `back-end/src/main/java/com/kuky/backend/learning/model/TeacherValidation.java`
- [x] T005 [P] Implement composition + derived-format helpers (from `homeworkType`/activity + question kinds) in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkCompositionSupport.java` (or equivalent shared helper)
- [x] T006 [P] Map `teacher_validation` on homework/activity answer models + repositories under `back-end/src/main/java/com/kuky/backend/learning/`
- [x] T007 Extend create/update/detail DTOs and mappers to expose `composition` and derived `format`; ignore client-supplied `format` on write in `back-end/src/main/java/com/kuky/backend/admin/dto/` and learning DTOs (`HomeworkItemResponse`, `ActivityItemResponse`, admin homework/activity items)
- [x] T008 [P] Update TypeScript types in `front-end/src/lib/admin.ts` and `front-end/src/lib/learning.ts` for `composition`, `format: "MIXED"`, `teacherValidation`, `provisionalScorePercent`, and optional `format` on create/update per [contracts/per-question-grading-api.md](./contracts/per-question-grading-api.md)

**Checkpoint**: V17 applies; APIs can read/write `composition` / `teacherValidation` fields; format is server-derived including `MIXED`.

---

## Phase 3: User Story 1 - Teacher authors mixed question kinds (Priority: P1) â€” MVP

**Goal**: Non-Writing authoring has no Manual/Exercise radio; one question list may mix FREE_TEXT and structured kinds; Writing stays single large answer; format/composition derived on save; activity authoring parity.

**Independent Test**: Create homework with one auto + one FREE_TEXT question; save; reopen â€” both kinds persist; no format radio ([quickstart.md](./quickstart.md) Â§1).

### Implementation for User Story 1

- [x] T009 [US1] Rework `HomeworkAdminService.validateAndMapQuestions` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`: WRITE â†’ empty questions; non-WRITE â†’ â‰¥1 question, allow FREE_TEXT + structured mix; validate each kind with existing structured rules; derive and persist `format` (`MANUAL`/`EXERCISE`/`MIXED`)
- [x] T010 [US1] Ensure create/update homework ignore request `format` and set derived format/composition in `HomeworkAdminService` create/update paths under `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`
- [x] T011 [US1] Mirror authoring validation + derived format in `ActivityAdminService` under `back-end/src/main/java/com/kuky/backend/admin/service/ActivityAdminService.java`
- [x] T012 [US1] Remove Manual/Exercise radio and format-gated editor branching in `front-end/src/components/admin/homework/HomeworkEditorPage.tsx`; use a unified question list that can add FREE_TEXT or structured kinds (evolve `ManualQuestionListEditor` / `QuestionEditorCard` or add `UnifiedQuestionListEditor.tsx`)
- [x] T013 [US1] Same unified authoring UX (no format radio) in `front-end/src/components/admin/activities/ActivityEditorPage.tsx`
- [x] T014 [P] [US1] i18n for mixed-authoring labels/errors in `front-end/src/i18n/locales/es.ts`, `en.ts`, `ro.ts`
- [x] T015 [US1] Backend tests in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` (and activity tests): mixed save OK; WRITE empty questions; reject empty non-WRITE; derive `MIXED`/`EXERCISE`/`MANUAL`

**Checkpoint**: Teacher can author mixed homeworks/activities; Writing unchanged; SC-001 / FR-001â€“005, FR-014 authoring side ready.

---

## Phase 4: User Story 2 - Student completes mixed homework (Priority: P1)

**Goal**: One submit for mixed questions; auto subset graded + keys revealed; manuals stored; status `SUBMITTED` (same awaiting bucket); provisional auto-only score optional; no final combined %; all-auto still immediate `GRADED`; all-manual still `SUBMITTED` without score; activity parity.

**Independent Test**: Student submits mixed â†’ auto feedback+keys, awaiting teacher, no final % ([quickstart.md](./quickstart.md) Â§2).

### Implementation for User Story 2

- [x] T016 [US2] Extend `ExerciseGradingService` in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` to grade a structured **subset** of questions and expose helpers for provisional/combined means (do not require whole assignment `EXERCISE`)
- [x] T017 [US2] Unify non-WRITE student submit on `PUT .../homework/{id}/answers` in `HomeworkSubmissionService` / learning controller under `back-end/src/main/java/com/kuky/backend/learning/`: accept heterogeneous answers (structured + FREE_TEXT); branch on composition â€” `ALL_AUTO`â†’`GRADED`+final score; `ALL_MANUAL`â†’`SUBMITTED`; `MIXED`â†’`SUBMITTED`+per-auto scores+key reveal+null final `scorePercent`+optional `provisionalScorePercent`; WRITE stays on `PUT .../{id}`
- [x] T018 [US2] Mirror mixed/all-auto/all-manual submit in `ActivityExerciseGradingService` / `ActivityStudentService` under `back-end/src/main/java/com/kuky/backend/learning/`
- [x] T019 [US2] Update `HomeworkItems` (and activity item builders) under `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkItems.java` for mixed student views: composition, provisional vs final score, auto results with keys after submit, FREE_TEXT answers without validation yet
- [x] T020 [US2] Student UI: composition-based dispatch in `front-end/src/components/learning/HomeworkInlinePanel.tsx` (and full-page homework routes under `front-end/src/routes/` / components) â€” render mixed form (structured controls + FREE_TEXT fields); submit via `/answers`; show provisional/awaiting state
- [x] T021 [US2] Same student take/result UX for activities in `front-end/src/components/learning/ActivityPanel.tsx` (and related activity components)
- [x] T022 [P] [US2] i18n for awaiting-teacher / provisional score copy in `front-end/src/i18n/locales/{es,en,ro}.ts`
- [x] T023 [US2] Backend tests: mixed submit â†’ `SUBMITTED` + auto scores + keys; all-auto â†’ `GRADED`; all-manual â†’ `SUBMITTED`; incomplete answers rejected; no retake â€” under `back-end/src/test/java/com/kuky/backend/learning/`

**Checkpoint**: Students can complete mixed work end-to-end until teacher step; FR-006â€“008a, FR-011â€“013a covered for submit path.

---

## Phase 5: User Story 3 - Teacher validates/invalidates to finalize mixed (Priority: P1)

**Goal**: Review queue includes mixed `SUBMITTED`; teacher must validate/invalidate every FREE_TEXT answer; optional annotate + â‰¤500 note; finalize â†’ `GRADED` + combined %; re-editable; pure-manual/WRITE keep existing `REVIEWED` path without required validation; activity parity.

**Independent Test**: Validate one / invalidate one manual answer; student sees marks + combined % ([quickstart.md](./quickstart.md) Â§3).

### Implementation for User Story 3

- [x] T024 [US3] Extend `SaveHomeworkFeedbackRequest` (and answer items) in `back-end/src/main/java/com/kuky/backend/admin/dto/SaveHomeworkFeedbackRequest.java` with optional `teacherValidation` per FREE_TEXT answer per contract
- [x] T025 [US3] Implement MIXED finalize in `HomeworkAdminService.saveFeedback`: require validation on every FREE_TEXT answer; set answer scores 1/0; compute combined `score_percent`; status `SUBMITTED`â†’`GRADED`; annotations/note per 032; allow re-save after `GRADED` for mixed; leave ALL_MANUAL/WRITE on `REVIEWED` path without requiring validation
- [x] T026 [US3] Ensure admin submissions list includes MIXED `SUBMITTED` alongside pure-manual awaiting in `HomeworkAdminService` / repository queries under `back-end/src/main/java/com/kuky/backend/admin/`
- [x] T027 [US3] Mirror finalize + queue behavior in `ActivityAdminService` under `back-end/src/main/java/com/kuky/backend/admin/service/ActivityAdminService.java`
- [x] T028 [US3] Add validate/invalidate controls to `front-end/src/components/admin/homework/HomeworkReviewDialog.tsx` for MIXED; block save until all manuals decided; keep annotate + plain note; show auto results read-only
- [x] T029 [US3] Same review UX in `front-end/src/components/admin/activities/ActivityReviewDialog.tsx`
- [x] T030 [US3] Student post-finalize view: show `teacherValidation`, combined `scorePercent`, annotations/note in learning components under `front-end/src/components/learning/` (homework + activity)
- [x] T031 [P] [US3] i18n for validate/invalidate/finalize errors in `front-end/src/i18n/locales/{es,en,ro}.ts`
- [x] T032 [US3] Backend tests: missing validation rejected; combined % math (validated=1, invalidated=0 + partial auto); re-edit recalculates; pure-manual review unchanged â€” in `back-end/src/test/java/com/kuky/backend/admin/`

**Checkpoint**: Mixed finalize works; SC-002â€“003 / FR-009â€“010a, FR-017 satisfied.

---

## Phase 6: User Story 4 - Existing all-auto and all-manual keep working (Priority: P2)

**Goal**: Legacy MANUAL/EXERCISE homeworks and activities remain completable without re-authoring; converting by adding the other kind yields MIXED for new submissions.

**Independent Test**: Complete pre-existing all-auto and all-manual flows; then add opposite kind and confirm new submits follow mixed rules ([quickstart.md](./quickstart.md) Â§4, Â§6).

### Implementation for User Story 4

- [x] T033 [US4] Verify/fix list and progress UIs that still branch only on `format === "EXERCISE"` under `front-end/src/components/learning/` and admin lists â€” prefer `composition` so `MANUAL`/`EXERCISE` legacy rows and new `MIXED` behave correctly
- [x] T034 [US4] Regression tests for legacy all-auto and all-manual (and WRITE) submit/review paths in `back-end/src/test/java/com/kuky/backend/` covering migrated format values without MIXED
- [x] T035 [US4] Smoke-fix: edit legacy EXERCISE to add FREE_TEXT (and legacy MANUAL to add structured) via admin UI/API â€” new student submissions use MIXED lifecycle

**Checkpoint**: SC-004 / FR-014 continuity confirmed.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: i18n sweep, docs, full quickstart, CLAUDE homework notes if needed.

- [x] T036 [P] Align any remaining `format`-only copy/docs in `contracts/api.md` references or comments under `back-end/` / `front-end/` that contradict per-question grading
- [x] T037 [P] Update homework format notes in `CLAUDE.md` Key implementation notes (MANUAL/EXERCISE â†’ per-question / MIXED) if still describing whole-homework format as source of truth
- [x] T038 Run full [quickstart.md](./quickstart.md) browser validation (mixed author â†’ student â†’ finalize; WRITE/auto/manual regressions; activity parity; migration smoke)
- [x] T039 [P] `npm run lint` in `front-end/` and `./gradlew test` in `back-end/` â€” fix regressions from this feature

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies â€” start immediately
- **Foundational (Phase 2)**: Depends on T001 â€” **BLOCKS** all user stories
- **US1 (Phase 3)**: Depends on Phase 2 â€” MVP authoring
- **US2 (Phase 4)**: Depends on Phase 2; practically needs US1 authored mixed content (or seed via API)
- **US3 (Phase 5)**: Depends on US2 submit producing MIXED `SUBMITTED` rows
- **US4 (Phase 6)**: Depends on US1â€“US3 paths existing; focuses on regression/continuity
- **Polish (Phase 7)**: After desired stories complete

### User Story Dependencies

- **US1 (P1)**: After Foundational â€” no dependency on other stories
- **US2 (P1)**: After Foundational; needs mixed assignments (from US1 or fixtures)
- **US3 (P1)**: After US2 submit path
- **US4 (P2)**: After core paths; can overlap polish

### Parallel Opportunities

- T002â€“T004, T006, T008 in Foundational can run in parallel after T001
- T012/T013 authoring UIs after backend T009â€“T011
- T028/T029 review dialogs in parallel after T025â€“T027
- T036/T037/T039 polish items in parallel

---

## Parallel Example: Foundational

```text
Task: "Add MIXED to HomeworkFormat.java"
Task: "Add HomeworkComposition.java"
Task: "Add TeacherValidation.java"
Task: "Map teacher_validation on answer repos"
Task: "Update front-end/src/lib/admin.ts and learning.ts types"
```

---

## Parallel Example: User Story 3 UI

```text
Task: "Validate/invalidate controls in HomeworkReviewDialog.tsx"
Task: "Same review UX in ActivityReviewDialog.tsx"
Task: "i18n for validate/invalidate/finalize errors"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1â€“2 (V17 + composition plumbing)
2. Complete Phase 3 (US1 authoring)
3. **STOP and VALIDATE**: Create/reopen mixed homework in admin
4. Then US2 â†’ US3 for end-to-end value

### Incremental Delivery

1. Setup + Foundational â†’ schema/types ready
2. US1 â†’ mixed authoring MVP
3. US2 â†’ student mixed submit
4. US3 â†’ teacher finalize + combined score (full feature)
5. US4 â†’ legacy continuity confidence
6. Polish â†’ quickstart + lint/tests

### Suggested MVP scope

**US1 only** proves the model change (drop format radio, mix kinds). Ship value with **US1+US2+US3** together for the product request (mixed take + teacher validate).

---

## Notes

- [P] = different files, no incomplete-task dependencies
- [USn] maps to spec user stories 1â€“4
- Activities mirror homework in the same story phases (not a separate story)
- Commit after each task or logical group
- Avoid reintroducing homework-level Manual/Exercise as a required write field
