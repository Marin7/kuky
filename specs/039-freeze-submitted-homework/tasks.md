---
description: "Task list for freeze submitted homework"
---

# Tasks: Freeze Submitted Homework

**Input**: Design documents from `specs/039-freeze-submitted-homework/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/freeze-submitted-homework-api.md](./contracts/freeze-submitted-homework-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit for snapshot-on-submit, live-edit isolation, retire-vs-delete, stale token `409 HOMEWORK_UPDATED`, due-date-only (no bump), backfill scores unchanged (per [plan.md](./plan.md)). Frontend — browser verification via [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted. Activities are out of scope (do not add snapshot/token there).

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Client types for the revision token every take/submit path will send.

- [X] T001 [P] Add optional `contentRevisedAt: string` on pending homework take/detail types in `front-end/src/lib/learning.ts`
- [X] T002 [P] Add `contentRevisedAt` to submit payloads used by `submitHomework`, `submitExercise`, and `submitHomeworkAnswers` in `front-end/src/lib/learning.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, models, retire-instead-of-delete, snapshot persistence, and `HOMEWORK_UPDATED`. No user story work until this phase completes.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Create Flyway `back-end/src/main/resources/db/migration/V20__homework_submission_snapshot.sql`: `homework_assignments.content_revised_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`; `homework_questions.retired BOOLEAN NOT NULL DEFAULT false`; `homework_question_options.retired BOOLEAN NOT NULL DEFAULT false`; `homework_submissions.assignment_snapshot JSONB`; backfill snapshot from current live homework for status in (`SUBMITTED`,`GRADED`,`REVIEWED`) without changing scores ([data-model.md](./data-model.md))
- [X] T004 [P] Add `contentRevisedAt` on `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkAssignment.java`; `retired` on `HomeworkQuestion.java` and `QuestionOption.java`; `assignmentSnapshot` (JSON string or node) on `HomeworkSubmission.java`; map columns in `ContentRepository.java` / question and submission repositories
- [X] T005 Change `replaceQuestions` in `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkQuestionRepository.java`: live queries `retired = false`; leftover referenced questions/options set `retired = true` instead of DELETE; unreferenced leftovers still DELETE ([research.md](./research.md) §2)
- [X] T006 [P] Create `HomeworkUpdatedException` in `back-end/src/main/java/com/kuky/backend/learning/exception/HomeworkUpdatedException.java` and map it to `409 HOMEWORK_UPDATED` in `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java` per [contracts/freeze-submitted-homework-api.md](./contracts/freeze-submitted-homework-api.md)
- [X] T007 Add snapshot serialize/parse helper (live assignment + non-retired questions → JSONB shape in [data-model.md](./data-model.md)) in `back-end/src/main/java/com/kuky/backend/learning/service/AssignmentSnapshot.java` (or equivalent next to `HomeworkItems.java`)
- [X] T008 Persist and bump `content_revised_at` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` `update`: bump when title, instructions, type, level, media, or questions change; **do not** bump for due-date-only; **do not** bump in `setAssignees`; never rewrite existing `assignment_snapshot`

**Checkpoint**: Migration applies; live editor still loads non-retired questions; `HOMEWORK_UPDATED` is a registered 409; snapshot helper can round-trip a homework.

---

## Phase 3: User Story 1 - Teacher edits homework without touching existing submissions (Priority: P1) — MVP

**Goal**: Teacher save updates the live homework only. Already-submitted rows keep answers, scores, and a snapshot taken at submit (or backfill). Deleting a question/option after submit retires it instead of wiping answer links.

**Independent Test**: Student A submits; teacher changes prompt, swaps correct option, deletes another question; A’s stored answers/scores/snapshot unchanged ([quickstart.md](./quickstart.md) §1–2).

### Implementation for User Story 1

- [X] T009 [US1] On successful first submit, write `assignment_snapshot` from live homework in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` and `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkSubmissionService.java` (WRITE / manual / mixed `/answers`); lock assignment row so concurrent teacher save cannot interleave ([research.md](./research.md) §3)
- [X] T010 [US1] Confirm `HomeworkAdminService.update` does not re-grade, clear answers, or mutate `assignment_snapshot` / teacher review fields; extend `back-end/src/test/java/com/kuky/backend/admin/HomeworkUpdatePreservesSubmissionsIntegrationTest.java` (or add `HomeworkSnapshotIsolationTest.java`): after PUT, snapshot JSON and `score_percent` unchanged; deleted option still linked via retired row + snapshot
- [X] T011 [US1] Unit/integration: retire referenced question/option on replace in `back-end/src/test/java/com/kuky/backend/learning/repository/` (or admin service test) — unreferenced leftover still deleted

**Checkpoint**: Teacher can edit after submit without changing stored scores or snapshot. FR-001, FR-002, FR-005, FR-006, FR-007.

---

## Phase 4: User Story 2 - Submitted student still sees the homework they actually took (Priority: P1)

**Goal**: Student result/review GET after submit hydrates title, instructions, media, questions, and per-question feedback from `assignment_snapshot`, not live questions. Overall % and item right/wrong stay the stored values (no `recomputeUnitResults` against live structure).

**Independent Test**: Submit, note prompts/choices/%; teacher edits key; student reopens — same prompts, choices, % ([quickstart.md](./quickstart.md) §1–2).

### Implementation for User Story 2

- [X] T012 [US2] When status is submitted/graded/reviewed and snapshot is present, map questions/title/instructions/media from snapshot in `back-end/src/main/java/com/kuky/backend/learning/service/LearningService.java` / `HomeworkItems.java` for `GET /api/v1/learning/homework/{id}` (list cards may keep live title)
- [X] T013 [US2] Change `storedResultFor` / `buildStoredResult` in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` to use snapshot questions; do not recompute unit results against live `structure_json`; keep persisted `score_percent` and per-answer scores ([research.md](./research.md) §6)
- [X] T014 [US2] Confirm `front-end/src/components/learning/ExerciseResult.tsx` (and mixed/manual result views) render the GET payload without fetching live homework questions separately
- [X] T015 [US2] Test: after live key/prompt change, student GET still returns snapshot prompts and original scores in `back-end/src/test/java/com/kuky/backend/learning/` (extend `HomeworkSubmissionServiceTest.java` or add snapshot result test)

**Checkpoint**: Student result view is frozen. FR-003, FR-008.

---

## Phase 5: User Story 3 - Teacher reviews a past submission against the original homework (Priority: P2)

**Goal**: Admin submission detail, exercise-result, and feedback save/re-edit use the snapshot questions. Teacher can still set percents/annotations on those frozen answers.

**Independent Test**: Mixed/manual submit; teacher edits prompts; opening that submission shows original prompts; teacher can still enter a percentage ([quickstart.md](./quickstart.md) §3).

### Implementation for User Story 3

- [X] T016 [US3] Hydrate `getExerciseResult` / `saveFeedback` / submission detail from snapshot in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` (do not `questionRepository.findByAssignment` for review when snapshot exists)
- [X] T017 [P] [US3] Ensure `front-end/src/components/admin/homework/HomeworkReviewDialog.tsx` and `front-end/src/components/admin/homework/ExerciseResultDialog.tsx` use the submission GET payload (snapshot-backed), not a parallel live-homework fetch
- [X] T018 [US3] Test: admin GET submission after live edit still shows original prompts; saving a teacher percent does not rewrite `assignment_snapshot` — `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` (or new review snapshot test)

**Checkpoint**: Review/result for the teacher matches what the student took. FR-009.

---

## Phase 6: User Story 4 - Students who have not submitted yet get the updated homework (Priority: P2)

**Goal**: Unsubmitted takes use live non-retired questions. `contentRevisedAt` on GET; submit must echo it or `409 HOMEWORK_UPDATED`. Client shows the update message, discards in-progress answers, refetches. Due-date-only and assignees do not bump the token.

**Independent Test**: Open take, teacher content-edits, student reload/return/submit → message + empty current homework. Due-date-only does not reset ([quickstart.md](./quickstart.md) §4).

### Implementation for User Story 4

- [X] T019 [US4] Expose `contentRevisedAt` on pending take `GET /api/v1/learning/homework/{id}` via `HomeworkItemResponse` in `back-end/src/main/java/com/kuky/backend/learning/dto/HomeworkItemResponse.java` and `LearningService.java` / `HomeworkItems.java`
- [X] T020 [US4] Require `contentRevisedAt` on `SubmitHomeworkRequest` and `SubmitExerciseRequest` in `back-end/src/main/java/com/kuky/backend/learning/dto/`; compare inside submit transaction in `HomeworkSubmissionService.java` and `ExerciseGradingService.java`; mismatch/omitted → `HomeworkUpdatedException`
- [X] T021 [P] [US4] Pass `contentRevisedAt` from take state in `front-end/src/lib/learning.ts` submit helpers and in `front-end/src/components/learning/ExerciseForm.tsx`, `MixedHomeworkForm.tsx`, `ManualAnswerForm.tsx`, `ManualMultiAnswerForm.tsx`
- [X] T022 [US4] On `HOMEWORK_UPDATED` or GET token change for the same homework id: show message, clear form state, refetch live homework in those take components
- [X] T023 [P] [US4] i18n copy for the homework-updated message in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`
- [X] T024 [US4] Tests: matching token submits and writes snapshot; stale token 409 and no snapshot; due-date-only PUT does not bump `content_revised_at`; assignees PUT does not bump — `back-end/src/test/java/com/kuky/backend/learning/` and admin update test

**Checkpoint**: Unsubmitted students always take live content; stale open takes reset with a message. FR-004, FR-012, FR-013, FR-014.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Activity non-regression and end-to-end browser checks.

- [X] T025 Confirm presentation activity submit/review paths are unchanged (no `contentRevisedAt`, no snapshot) in `back-end/src/main/java/com/kuky/backend/learning/service/ActivityStudentService.java` / `ActivityExerciseGradingService.java` and existing activity tests ([quickstart.md](./quickstart.md) §5, FR-010, FR-011)
- [ ] T026 Run [quickstart.md](./quickstart.md) in the browser (two students, edit after submit, stale take message, due-date-only, activity sanity)
- [X] T027 [P] `./gradlew test` in `back-end/` and `npm run lint` in `front-end/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS** all user stories
- **US1 (Phase 3)**: Depends on Foundational — MVP
- **US2 (Phase 4)**: Depends on US1 snapshot-on-submit (T009)
- **US3 (Phase 5)**: Depends on US1 snapshot; can proceed in parallel with US2 after T009
- **US4 (Phase 6)**: Depends on Foundational token/bump (T006, T008); can proceed in parallel with US2/US3
- **Polish (Phase 7)**: After US1–US4

### User Story Dependencies

- **User Story 1 (P1)**: After Phase 2 — snapshot write + edit isolation
- **User Story 2 (P1)**: After US1 snapshot exists — student GET/result from snapshot
- **User Story 3 (P2)**: After US1 snapshot exists — admin review from snapshot
- **User Story 4 (P2)**: After Phase 2 — live take + stale token; independent of result hydration

### Parallel Opportunities

- T001 / T002 (frontend types)
- T004 / T006 (models vs exception)
- T014 / T017 (student vs admin UI payload checks)
- T021 / T023 (forms vs i18n)
- US2 and US3 after T009
- US4 after T008 in parallel with US2/US3

---

## Parallel Example: Foundational

```text
Task: "Add model fields in HomeworkAssignment.java / HomeworkQuestion.java / HomeworkSubmission.java"
Task: "Create HomeworkUpdatedException + GlobalExceptionHandler mapping"
```

## Parallel Example: User Story 4

```text
Task: "Pass contentRevisedAt from ExerciseForm / MixedHomeworkForm / ManualAnswerForm / ManualMultiAnswerForm"
Task: "i18n homework-updated copy in es.ts, en.ts, ro.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup
2. Phase 2 Foundational
3. Phase 3 US1 — snapshot on submit, edits do not mutate it
4. **STOP and VALIDATE**: [quickstart.md](./quickstart.md) §1–2 (stored scores/snapshot)

### Incremental Delivery

1. Setup + Foundational
2. US1 → submitted data isolated (MVP)
3. US2 → student sees frozen result
4. US3 → teacher reviews frozen copy
5. US4 → unsubmitted take live + stale message
6. Polish / quickstart §4–5

---

## Notes

- [P] = different files, no incomplete dependencies
- Do **not** add snapshot/`contentRevisedAt` to activities or placement
- List title may stay live; submitted **detail** uses snapshot
- Commit after each task or logical group
- Verify tests fail only if writing them first; this list follows implementation-then-test like 038
