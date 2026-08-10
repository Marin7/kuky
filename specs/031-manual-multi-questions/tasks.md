---
description: "Task list for multi-question manual homework"
---

# Tasks: Multi-Question Manual Homework

**Input**: Design documents from `specs/031-manual-multi-questions/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/manual-multi-questions-api.md](./contracts/manual-multi-questions-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit for authoring validation, multi MANUAL submit, snapshot retention, WRITE/EXERCISE regression (per [plan.md](./plan.md)). Frontend — browser verification via [quickstart.md](./quickstart.md) (no frontend unit-test framework).

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Schema + data migration every story depends on.

- [x] T001 Write `back-end/src/main/resources/db/migration/V15__manual_free_text_questions.sql` per [data-model.md](./data-model.md) and [research.md](./research.md): widen `homework_questions.kind` and `activity_questions.kind` CHECKs with `FREE_TEXT`; add `answer_text` + `prompt_snapshot` to `homework_answers` and `activity_answers`; migrate legacy non-WRITE MANUAL homework/activity (seed one `FREE_TEXT` question `"Tu respuesta"`, move plain text from `response_text` into answers, null `response_text`; leave WRITE untouched)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain/enum/DTO/repository + shared MANUAL authoring validation so US1–US3 can build on one choke point.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Add `FREE_TEXT` to `back-end/src/main/java/com/kuky/backend/learning/model/QuestionKind.java` (not structured; document that EXERCISE must not use it)
- [x] T003 [P] Add `answerText` + `promptSnapshot` fields/accessors on `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkAnswer.java` and `ActivityAnswer.java` (or activity answer model equivalent)
- [x] T004 [P] Extend `HomeworkAnswerRepository` / `ActivityAnswerRepository` under `back-end/src/main/java/com/kuky/backend/learning/repository/` to map/persist `answer_text` and `prompt_snapshot`
- [x] T005 Update `HomeworkAdminService.validateAndMapQuestions` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` per contract: MANUAL+WRITE → empty questions; MANUAL non-WRITE / activity MANUAL → ≥1 all `FREE_TEXT` (prompt required, no options, `{}` structure); EXERCISE → reject `FREE_TEXT`; ensure `ActivityAdminService` uses the non-WRITE MANUAL rule for MANUAL activities
- [x] T006 [P] Add `FREE_TEXT` to `QuestionKind` unions in `front-end/src/lib/admin.ts` and `front-end/src/lib/learning.ts`
- [x] T007 Update failing/outdated unit expectations in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` (and activity validation tests if any) for the new MANUAL question rules; add focused cases for WRITE empty vs AUDIO ≥1 FREE_TEXT vs EXERCISE rejecting FREE_TEXT

**Checkpoint**: V15 applies; `FREE_TEXT` exists end-to-end in enums; admin validation enforces WRITE vs multi MANUAL; repos can store plain answers + snapshots.

---

## Phase 3: User Story 1 - Teacher authors several free-text questions (Priority: P1) — MVP

**Goal**: Teacher can create/edit non-WRITE MANUAL homeworks (and MANUAL activities) with an ordered list of FREE_TEXT prompts; WRITE stays question-less.

**Independent Test**: Create MANUAL AUDIO with instructions, audio, and two FREE_TEXT questions; reopen editor and confirm order. WRITE homework saves with no question list. MANUAL activity saves with ≥1 FREE_TEXT prompt.

### Implementation for User Story 1

- [x] T008 [P] [US1] Persist MANUAL FREE_TEXT questions on create/update in `HomeworkAdminService` (stop clearing questions for all MANUAL) and return them on admin GET DTOs under `back-end/src/main/java/com/kuky/backend/admin/`
- [x] T009 [P] [US1] Same for MANUAL activities in `back-end/src/main/java/com/kuky/backend/admin/service/ActivityAdminService.java` (map/save FREE_TEXT questions like homework)
- [x] T010 [P] [US1] Create `front-end/src/components/admin/homework/ManualQuestionListEditor.tsx` — ordered prompt-only list (add / reorder / remove); no kind picker, no options
- [x] T011 [US1] Wire `ManualQuestionListEditor` into `front-end/src/components/admin/homework/HomeworkEditorPage.tsx` when `format === "MANUAL" && homeworkType !== "WRITE"`; send questions on save; keep WRITE with empty questions and EXERCISE on existing `QuestionListEditor`
- [x] T012 [US1] Wire the same editor into `front-end/src/components/admin/activities/ActivityEditorPage.tsx` for MANUAL format
- [x] T013 [P] [US1] Add i18n strings for manual question authoring in `front-end/src/i18n/locales/es.ts`, `en.ts`, `ro.ts`

**Checkpoint**: Teacher can author multi FREE_TEXT MANUAL homework + activity; WRITE/EXERCISE authoring unchanged.

---

## Phase 4: User Story 2 - Student answers each question (Priority: P1)

**Goal**: Student sees instructions → media → compact plain-text fields per question; must answer all; submit stores per-question answers + prompt snapshots. WRITE keeps rich-text single answer.

**Independent Test**: Open MANUAL AUDIO with two questions; confirm layout order; blank submit blocked; full submit persists answers under each prompt. WRITE page still uses large rich-text editor.

### Implementation for User Story 2

- [x] T014 [P] [US2] Extend `SubmitHomeworkRequest` and related DTOs under `back-end/src/main/java/com/kuky/backend/learning/dto/` with `answers: [{ questionId, text }]` per [contracts/manual-multi-questions-api.md](./contracts/manual-multi-questions-api.md)
- [x] T015 [US2] Update `HomeworkSubmissionService` in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkSubmissionService.java`: WRITE path keeps `response`; multi MANUAL validates all current questions answered (non-blank), upserts submission, replaces FREE_TEXT answer rows with `answer_text` + `prompt_snapshot`, leaves `response_text` null
- [x] T016 [US2] Mirror multi MANUAL submit in `ActivityStudentService` (`back-end/src/main/java/com/kuky/backend/learning/service/ActivityStudentService.java`)
- [x] T017 [US2] Expose `questions` + `answers` (with snapshots) on student homework/activity item responses via `HomeworkItems` / activity DTOs under `back-end/src/main/java/com/kuky/backend/learning/`
- [x] T018 [P] [US2] Update `front-end/src/lib/learning.ts` types + `submitHomework` / activity submit helpers for `answers[]`
- [x] T019 [P] [US2] Create `front-end/src/components/learning/ManualMultiAnswerForm.tsx` — compact plain `<Textarea>` per question; client-side require all non-empty; submit `answers[]`
- [x] T020 [US2] Use `ManualMultiAnswerForm` on `HomeworkListeningPage.tsx`, `HomeworkReadingPage.tsx`, and `HomeworkInlinePanel.tsx` for non-WRITE MANUAL (layout: instructions → audio if any → questions); keep `ManualAnswerForm` for WRITE only
- [x] T021 [US2] Switch MANUAL activity student UI (overlay/panel under `front-end/src/components/learning/` or activities) to `ManualMultiAnswerForm`
- [x] T022 [P] [US2] Add student-facing i18n (validation “answer every question”, labels) in `front-end/src/i18n/locales/{es,en,ro}.ts`
- [x] T023 [US2] Add/adjust backend tests for multi MANUAL submit (all-required, WRITE still optional/rich-text path) under `back-end/src/test/java/com/kuky/backend/learning/`

**Checkpoint**: Students can complete multi-question MANUAL homework/activity; WRITE unchanged; blank answers rejected.

---

## Phase 5: User Story 3 - Teacher reviews per-question answers (Priority: P2)

**Goal**: Review UI shows each prompt snapshot + plain answer; feedback remains whole-submission rich text; snapshots survive question deletion after submit.

**Independent Test**: Submit multi MANUAL as student; open admin review; see each Q+A; save feedback → REVIEWED. Remove a question from the homework; prior submission still shows snapshot prompt.

### Implementation for User Story 3

- [x] T024 [US3] Extend `HomeworkSubmissionAdminDto` (+ activity review DTO if separate) under `back-end/src/main/java/com/kuky/backend/admin/dto/` with `answers: [{ questionId, promptSnapshot, text }]` and populate in `HomeworkAdminService` review detail (WRITE keeps `response`)
- [x] T025 [US3] Update `front-end/src/components/admin/homework/HomeworkReviewDialog.tsx` (and activity review UI if any) to render per-question snapshot/answer pairs; keep feedback editor as today
- [x] T026 [P] [US3] Update admin types in `front-end/src/lib/admin.ts` for review payload
- [x] T027 [US3] Backend test: after submit, delete a FREE_TEXT question, review DTO still returns `prompt_snapshot` + `answer_text` for that answer (`back-end/src/test/java/com/kuky/backend/admin/`)
- [x] T028 [P] [US3] Review-related i18n in `front-end/src/i18n/locales/{es,en,ro}.ts` if new labels needed

**Checkpoint**: Teacher can review multi-question MANUAL submissions end-to-end; snapshots work after question removal.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Regression, seeds, docs alignment, quickstart pass.

- [x] T029 [P] Update `back-end/src/main/resources/db/dev/full_seed.sql` MANUAL sample to include FREE_TEXT question(s) + answer row shape if it still uses only `response_text` for non-WRITE
- [x] T030 [P] Ensure EXERCISE grading paths ignore/reject FREE_TEXT (`ExerciseGradingService` / admin EXERCISE validation already covered in T005/T007 — add assertion if missing)
- [ ] T031 Run browser validation scenarios 1–7 in [quickstart.md](./quickstart.md) (author, student layout, review, snapshot, migration smoke, activity parity, EXERCISE/WRITE regression)
- [x] T032 [P] Sync `contracts/api.md` or learning notes only if the repo keeps a living aggregate API doc that lists homework submit shapes (skip if none)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on T001 — **blocks** all user stories
- **US1 (Phase 3)**: Depends on Phase 2 — MVP authoring
- **US2 (Phase 4)**: Depends on Phase 2; practically needs US1 authored questions (or migrated legacy) to demo — can still unit-test submit with seeded questions
- **US3 (Phase 5)**: Depends on US2 submit producing answer rows
- **Polish (Phase 6)**: After US1–US3 desired scope

### User Story Dependencies

- **US1**: After foundational — no dependency on US2/US3
- **US2**: After foundational; integrates with US1 data but independently testable with fixtures
- **US3**: After US2 submit path exists

### Parallel Opportunities

- T002–T004, T006 in Phase 2 can proceed in parallel after T001
- T008–T010, T013 in US1 parallelizable
- T014, T018–T019, T022 in US2 parallelizable after contracts/DTOs land
- T026, T028 in US3 parallelizable with review UI work

---

## Parallel Example: User Story 1

```text
T010 ManualQuestionListEditor.tsx
T008 HomeworkAdminService persist FREE_TEXT questions
T009 ActivityAdminService persist FREE_TEXT questions
T013 i18n authoring strings
# then T011–T012 wire editors
```

---

## Parallel Example: User Story 2

```text
T014 SubmitHomeworkRequest DTO
T018 learning.ts client types
T019 ManualMultiAnswerForm.tsx
# then T015–T017 services/DTOs, T020–T021 page wiring
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1–2 (migration + validation)
2. Complete Phase 3 (US1 authoring)
3. **STOP and VALIDATE**: Teacher can save MANUAL AUDIO with multiple FREE_TEXT questions
4. Then US2 → US3 → polish / quickstart

### Incremental Delivery

1. Setup + Foundational → schema + validation ready
2. US1 → authoring MVP
3. US2 → student multi-answer experience (core product ask)
4. US3 → review + snapshots
5. Polish → seeds, quickstart, regressions

---

## Notes

- [P] = different files, no incomplete-task dependency
- Do not change EXERCISE student/grade UX except rejecting FREE_TEXT at authoring
- WRITE must never show `ManualQuestionListEditor` or `ManualMultiAnswerForm`
- Commit after each logical group; validate at story checkpoints
