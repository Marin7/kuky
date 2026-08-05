---
description: "Task list for teacher plain-text feedback on graded exercises"
---

# Tasks: Teacher Feedback on Homework Submissions

**Input**: Design documents from `specs/025-homework-feedback/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/exercise-feedback-api.md](contracts/exercise-feedback-api.md), [quickstart.md](quickstart.md)

**Tests**: Backend JUnit per [plan.md](plan.md) (`HomeworkAdminService` save/update/clear, validation, Writing regression). Frontend has no unit-test framework — verify via [quickstart.md](quickstart.md) in a browser. Writing rich-feedback path must stay unchanged.

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Confirm storage approach; no schema work required.

- [x] T001 Confirm no Flyway migration is needed (reuse `homework_submissions.feedback` per [data-model.md](data-model.md) / [research.md](research.md)); note encoding rule (single plain FormattedText segment or `NULL`) in a short comment on the repository helper added in Phase 2.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared encode/decode, repository update without status change, and DTO/API scaffolding both stories need.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Add plain↔FormattedText helpers (encode single unformatted segment; decode to plain string; treat blank/`NULL` as absent) in `back-end/src/main/java/com/kuky/backend/learning/model/FormattedTextSegment.java` (or a small dedicated helper next to it), enforcing ≤ 2000 chars consistent with `MAX_VISIBLE_LENGTH`.
- [x] T003 Add `updateExerciseFeedback(UUID submissionId, String feedbackJsonOrNull)` to `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkSubmissionRepository.java` — updates `feedback` + `updated_at` only (do **not** change `status` or `reviewed_at`). Depends on T001.
- [x] T004 [P] Create `back-end/src/main/java/com/kuky/backend/admin/dto/SaveExerciseFeedbackRequest.java` with `String feedback` per [contracts/exercise-feedback-api.md](contracts/exercise-feedback-api.md).
- [x] T005 [P] Add `teacherFeedback` (`String`, nullable) to `back-end/src/main/java/com/kuky/backend/admin/dto/ExerciseSubmissionResultAdminDto.java`.
- [x] T006 Extend `getExerciseResult` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` to populate `teacherFeedback` via decode helper. Depends on T002, T005.
- [x] T007 [P] Add `hasTeacherFeedback: boolean` to `back-end/src/main/java/com/kuky/backend/admin/dto/AssigneeDto.java` and `StudentProfileHomeworkDto.java`.
- [x] T008 [P] Add `hasTeacherFeedback: boolean` to `back-end/src/main/java/com/kuky/backend/learning/dto/HomeworkItemResponse.java` and `teacherFeedback` (`String`, nullable) to `ExerciseResponse.java`.
- [x] T009 [P] Extend `ExerciseSubmissionResultAdmin` + add `saveExerciseFeedback` client in `front-end/src/lib/admin.ts`; extend assignee/profile homework types with `hasTeacherFeedback`.
- [x] T010 [P] Extend `HomeworkItem` with `hasTeacherFeedback` and exercise payload with `teacherFeedback` in `front-end/src/lib/learning.ts`.

**Checkpoint**: Helpers/repos/DTOs/FE types ready; save endpoint and UIs still TODO.

---

## Phase 3: User Story 1 - Teacher leaves plain-text feedback on a graded exercise (Priority: P1) 🎯 MVP

**Goal**: Teacher opens a GRADED exercise result, optionally saves/updates/clears plain-text feedback (≤ 2000), sees it again on reopen, and sees a feedback-present indicator on admin assignee and student-profile homework lists. Writing `PUT …/feedback` unchanged.

**Independent Test**: Open a GRADED exercise as admin → save comment → reopen → text persists; list shows indicator; clear removes it; >2000 rejected; MANUAL Writing review still works.

### Tests for User Story 1

- [x] T011 [P] [US1] Add/extend tests in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` (and/or controller integration test): save/update/clear on GRADED EXERCISE; reject non-GRADED and MANUAL; reject length > 2000 without changing prior feedback; Writing `saveFeedback` still transitions `SUBMITTED → REVIEWED`.

### Implementation for User Story 1

- [x] T012 [US1] Implement `saveExerciseFeedback` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`: require EXERCISE + GRADED; encode/clear via T002; call T003; return full exercise-result DTO with `teacherFeedback`. Depends on T002, T003, T004, T006.
- [x] T013 [US1] Add `PUT /submissions/{submissionId}/exercise-feedback` to `back-end/src/main/java/com/kuky/backend/admin/controller/HomeworkAdminController.java` wiring T012. Depends on T004, T012.
- [x] T014 [US1] Populate `hasTeacherFeedback` when mapping assignees in `HomeworkAdminService` (and any SQL/join that feeds `AssigneeDto`). Depends on T007.
- [x] T015 [US1] Populate `hasTeacherFeedback` in `back-end/src/main/java/com/kuky/backend/admin/service/StudentProfileAdminService.java` for profile homework rows. Depends on T007.
- [x] T016 [US1] Add plain-text textarea + save/clear UX to `front-end/src/components/admin/homework/ExerciseResultDialog.tsx` (load `teacherFeedback`, call `saveExerciseFeedback`, show validation errors). Depends on T009, T013.
- [x] T017 [P] [US1] Show feedback-present indicator on GRADED assignees in `front-end/src/components/admin/homework/HomeworkAdminList.tsx`. Depends on T009, T014.
- [x] T018 [P] [US1] Show feedback-present indicator on homework rows in `front-end/src/routes/panel_.alumnos.$studentId.tsx`. Depends on T009, T015.
- [x] T019 [P] [US1] Add admin i18n strings (feedback label, placeholder, save/clear, errors, list indicator) in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`.

**Checkpoint**: Teacher can save/update/clear exercise feedback and see admin list indicators. MVP deliverable.

---

## Phase 4: User Story 2 - Student sees the teacher's feedback (Priority: P2)

**Goal**: Student homework list shows a feedback indicator when present; opening the graded exercise shows the plain-text teacher comment beside the automatic result. No indicator / no empty block when absent.

**Independent Test**: As student, see list indicator for graded exercise with feedback; open result and read comment; graded exercise without feedback has neither indicator nor empty feedback section.

### Implementation for User Story 2

- [x] T020 [US2] Set `hasTeacherFeedback` in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkItems.java` when building `HomeworkItemResponse`. Depends on T008.
- [x] T021 [US2] Expose `teacherFeedback` on student exercise GET path via `ExerciseGradingService` / learning service mapping into `ExerciseResponse` in `back-end/src/main/java/com/kuky/backend/learning/`. Depends on T002, T008.
- [x] T022 [US2] Pass `teacherFeedback` into `front-end/src/components/learning/ExerciseResult.tsx` (and wire from `ExerciseForm.tsx` when status is GRADED); render plain text only when non-null/non-empty. Depends on T010, T021.
- [x] T023 [P] [US2] Show feedback-present indicator on `front-end/src/components/learning/HomeworkItemCard.tsx` when `hasTeacherFeedback` is true. Depends on T010, T020.
- [x] T024 [P] [US2] Add student-facing i18n (result feedback heading, list indicator) in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`.

**Checkpoint**: Student discovery + read path works; US1 teacher path still works.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Regression and end-to-end validation.

- [x] T025 Confirm Writing review queue/dialog and `PUT …/feedback` behavior unchanged (`HomeworkReviewDialog.tsx`, existing admin feedback tests).
- [x] T026 Run `./gradlew test` for affected admin/learning tests; fix failures.
- [ ] T027 Execute [quickstart.md](quickstart.md) browser checklist (teacher save/indicator/clear/over-length + student indicator/result + Writing smoke).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS** all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational — MVP
- **User Story 2 (Phase 4)**: Depends on Foundational; uses same storage/API fields as US1 (prefer after T012–T013 so real data exists for browser checks)
- **Polish (Phase 5)**: Depends on US1 + US2

### User Story Dependencies

- **US1 (P1)**: After Phase 2 — no dependency on US2
- **US2 (P2)**: After Phase 2 — independently testable once teacher feedback can exist (US1 save) or via seeded `feedback` JSON

### Parallel Opportunities

- Phase 2: T002, T004, T005, T007, T008, T009, T010 in parallel after/alongside T003 as noted
- US1: T011 tests parallel with early service work; T017, T018, T019 parallel after backend list fields exist
- US2: T023, T024 parallel with T022 once types exist

---

## Parallel Example: User Story 1

```bash
# After T012–T015 (backend save + list flags):
Task: "Show feedback-present indicator on GRADED assignees in front-end/src/components/admin/homework/HomeworkAdminList.tsx"
Task: "Show feedback-present indicator on homework rows in front-end/src/routes/panel_.alumnos.$studentId.tsx"
Task: "Add admin i18n strings in front-end/src/i18n/locales/es.ts, en.ts, and ro.ts"
```

---

## Parallel Example: User Story 2

```bash
# After T020–T021 (overview + exercise GET expose fields):
Task: "Show feedback-present indicator on front-end/src/components/learning/HomeworkItemCard.tsx"
Task: "Add student-facing i18n in front-end/src/i18n/locales/es.ts, en.ts, and ro.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1–2
2. Complete Phase 3 (US1) — teacher save + admin indicators
3. **STOP and VALIDATE** per US1 independent test
4. Demo if ready

### Incremental Delivery

1. Setup + Foundational → shared encode/DTOs ready
2. US1 → teacher feedback MVP
3. US2 → student visibility
4. Polish → quickstart + Writing regression

### Parallel Team Strategy

1. Shared: Phase 1–2
2. Then: Dev A finishes US1 UI while Dev B starts US2 backend DTO wiring (after T008)
3. Integrate and run quickstart

---

## Notes

- [P] = different files, no incomplete-task dependencies
- Do **not** modify Writing `PUT …/feedback` lifecycle
- No email/notifications
- Empty/whitespace save clears feedback (`NULL`) and clears indicators
- Commit after each task or logical group
