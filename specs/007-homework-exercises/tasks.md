# Tasks: Homework Exercises & Self-Correction

**Input**: Design documents from `specs/007-homework-exercises/`

**Branch**: `007-homework-exercises` | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

**Tests**: Included — `ExerciseGradingServiceTest` and `HomeworkExerciseAdminServiceTest` are explicitly
named in plan.md's source tree and technical context.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared-state dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Exact file paths are relative to repo root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration that all other work depends on.

- [x] T001 Write `back-end/src/main/resources/db/migration/V15__create_homework_exercises.sql` — add `format VARCHAR(10) NOT NULL DEFAULT 'MANUAL'` + CHECK to `homework_assignments`; add `score_percent INT` + extend status CHECK (`+GRADED`) on `homework_submissions`; create `homework_questions`, `homework_question_options`, `homework_answers`, `homework_answer_options` tables with all columns, indexes, and foreign-key constraints per data-model.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Java enums, updated POJOs, and new repositories that every user story needs.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Create `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkFormat.java` — enum `MANUAL`, `EXERCISE`
- [x] T003 [P] Create `back-end/src/main/java/com/kuky/backend/learning/model/QuestionKind.java` — enum `SINGLE_CHOICE`, `MULTI_CHOICE`, `FILL_BLANK`
- [x] T004 Update `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkAssignment.java` — add `HomeworkFormat format` field (depends on T002)
- [x] T005 Update `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkSubmission.java` — add `Integer scorePercent` field; extend status handling to include `GRADED` (depends on T002)
- [x] T006 [P] Create `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkQuestion.java` — POJO with `id` (UUID), `assignmentId`, `position` (int), `kind` (QuestionKind), `prompt`, and a `List<QuestionOption> options` (depends on T003)
- [x] T007 [P] Create `back-end/src/main/java/com/kuky/backend/learning/model/QuestionOption.java` — POJO with `id` (UUID), `questionId`, `position` (int), `label`, `isCorrect` (boolean) (depends on T003)
- [x] T008 Create `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkAnswer.java` — POJO with `id` (UUID), `submissionId`, `questionId` (nullable), `answerText` (nullable), `score` (BigDecimal 4,3), and `List<String> selectedOptionIds` (depends on T003)
- [x] T009 Update `back-end/src/main/java/com/kuky/backend/learning/repository/ContentRepository.java` — map `format` column from `homework_assignments` into `HomeworkAssignment` in the existing `RowMapper` (depends on T004)
- [x] T010 Create `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkQuestionRepository.java` — `NamedParameterJdbcTemplate`-backed methods: `replaceQuestions(assignmentId, List<HomeworkQuestion>)` (delete all then bulk-insert questions + options in a transaction), `findByAssignment(assignmentId) → List<HomeworkQuestion>` with options eagerly loaded (depends on T006, T007)
- [x] T011 Create `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkAnswerRepository.java` — methods: `saveAll(submissionId, List<HomeworkAnswer>)` (insert into `homework_answers` + `homework_answer_options`), `findBySubmission(submissionId) → List<HomeworkAnswer>` (depends on T008)

**Checkpoint**: Foundation ready — all user story phases can begin.

---

## Phase 3: User Story 1 — Dedicated Authoring Page (Priority: P1) 🎯 MVP

**Goal**: Teacher creates and edits homework on a full-page route instead of a dialog. All existing
fields are preserved. The `HomeworkEditorDialog` is replaced and deleted. Creating a manual homework
end-to-end must work.

**Independent Test**: Log in as teacher → open `/panel` → Tareas tab → click **Nueva tarea** →
browser navigates to `/panel/tareas/nueva` (a full page, not a dialog) with all existing fields →
fill in title, pick type/level, assign a student, save → homework appears in list. Click **Editar** →
navigates to `/panel/tareas/{id}` pre-filled → change title → save → list reflects change.
**Cancelar** returns to list without saving.

### Implementation for User Story 1

- [x] T012 [P] [US1] Update `back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkAdminItem.java` — add `HomeworkFormat format` field (depends on T002)
- [x] T013 [P] [US1] Update `back-end/src/main/java/com/kuky/backend/admin/dto/CreateHomeworkRequest.java` — add `HomeworkFormat format` field with `@NotNull` (depends on T002)
- [x] T014 [P] [US1] Update `back-end/src/main/java/com/kuky/backend/admin/dto/UpdateHomeworkRequest.java` — add `HomeworkFormat format` field with `@NotNull` (depends on T002)
- [x] T015 [US1] Update `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` — persist `format` on create/update (write to `homework_assignments`); map `format` column when building `HomeworkAdminItem` in both list and single-fetch; extract or add a `findById(id)` helper that returns a fully-built `HomeworkAdminItem` (depends on T009, T012, T013, T014)
- [x] T016 [US1] Add `GET /api/v1/admin/homework/{id}` handler to `back-end/src/main/java/com/kuky/backend/admin/controller/HomeworkAdminController.java` — calls `HomeworkAdminService.findById(id)`; returns `HomeworkAdminItem`; 404 `ASSIGNMENT_NOT_FOUND` if missing (depends on T015)
- [x] T017 [US1] Update `front-end/src/lib/admin.ts` — add `format: HomeworkFormat` to `HomeworkAdminItem` type; add `getHomework(id: string): Promise<HomeworkAdminItem>` function; pass `format` in `createHomework` and `updateHomework` request bodies
- [x] T018 [US1] Create `front-end/src/components/admin/homework/HomeworkEditorPage.tsx` — full-page form with title, instructions, type select, level select, due date, format radio toggle (MANUAL / EXERCISE), and `StudentMultiSelect`; loads existing homework via `getHomework(id)` when in edit mode; calls `createHomework` / `updateHomework` + `setAssignees` on save; navigates to `/panel` on success or cancel; **no exercise questions section yet** (added in US2)
- [x] T019 [US1] Create `front-end/src/routes/panel_.tareas.nueva.tsx` — flat route file; guards `role === 'ADMIN'` (redirect non-admin to `/`, guest to `/cuenta`); renders `HomeworkEditorPage` in create mode
- [x] T020 [US1] Create `front-end/src/routes/panel_.tareas.$homeworkId.tsx` — flat route file; same guard; renders `HomeworkEditorPage` in edit mode passing `homeworkId` param
- [x] T021 [US1] Update `front-end/src/components/admin/homework/HomeworkAdminList.tsx` — replace all `HomeworkEditorDialog` open calls with `navigate('/panel/tareas/nueva')` (new) and `navigate('/panel/tareas/${id}')` (edit); add a format badge ("Ejercicio" / "Manual") to each list item
- [x] T022 [US1] Delete `front-end/src/components/admin/homework/HomeworkEditorDialog.tsx` — remove the file entirely (do not comment out; no dead code per constitution)

**Checkpoint**: US1 fully functional — teacher can create and edit manual homework via dedicated page.

---

## Phase 4: User Story 2 — Author a Self-Correcting Exercise (Priority: P2)

**Goal**: Teacher designates a homework as a self-correcting exercise, adds questions (single-choice,
multi-choice, fill-blank), records the answer key, and saves. Validation prevents invalid exercises.
Questions and the answer key round-trip exactly.

**Independent Test**: On the authoring page, toggle format to **Ejercicio autocorregible** → exercise
questions section appears → add a SINGLE_CHOICE question with 3 options (1 correct), a MULTI_CHOICE
with 4 options (2 correct), a FILL_BLANK with 1 accepted answer → try saving with no correct option
marked → blocked → fix → save → reopen via Editar → questions/options/correct flags persisted exactly.
Confirm via SQL: `SELECT q.kind, o.label, o.is_correct FROM homework_questions q JOIN homework_question_options o ON o.question_id=q.id WHERE q.assignment_id='<id>';`

### Implementation for User Story 2

- [x] T023 [P] [US2] Create `back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkQuestionDto.java` — record with `id` (nullable), `kind` (QuestionKind), `prompt`, `options: List<OptionDto>` where `OptionDto` is `record(id, label, correct)` (teacher view; includes correct flag)
- [x] T024 [US2] Update `back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkAdminItem.java` — add `List<HomeworkQuestionDto> questions` field (empty list for MANUAL, populated for EXERCISE) (depends on T023)
- [x] T025 [US2] Update `back-end/src/main/java/com/kuky/backend/admin/dto/CreateHomeworkRequest.java` and `UpdateHomeworkRequest.java` — add `List<HomeworkQuestionDto> questions` field (depends on T023)
- [x] T026 [US2] Update `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` — add exercise validation (≥1 question when EXERCISE, prompt non-blank, SINGLE_CHOICE exactly 1 correct, MULTI_CHOICE ≥1 correct, FILL_BLANK ≥1 accepted answer, MANUAL must have 0 questions) throwing `400 VALIDATION_ERROR` on failure; on valid save call `HomeworkQuestionRepository.replaceQuestions(id, questions)`; load questions+options in list and single-fetch responses (depends on T010, T023, T024, T025)
- [x] T027 [P] [US2] Create `front-end/src/components/admin/homework/QuestionEditorCard.tsx` — card UI for a single question: kind selector (SINGLE_CHOICE / MULTI_CHOICE / FILL_BLANK), prompt textarea, dynamic options/accepted-answers editor (add/remove option rows, correct toggle, label field); surface `onChange` to parent
- [x] T028 [US2] Create `front-end/src/components/admin/homework/QuestionListEditor.tsx` — manages an ordered list of `QuestionEditorCard` components; up/down reorder buttons; add-question button; remove button per card (depends on T027)
- [x] T029 [US2] Update `front-end/src/components/admin/homework/HomeworkEditorPage.tsx` — when format=EXERCISE render `QuestionListEditor` below the existing fields; include questions in the save payload (depends on T028)
- [x] T030 [US2] Update `front-end/src/lib/admin.ts` — add `questions: AdminQuestion[]` to `HomeworkAdminItem` type; add `AdminQuestion` and `AdminOption` interfaces; pass `questions` in `createHomework` and `updateHomework` request bodies

**Checkpoint**: US2 fully functional — teacher can author exercises with answer keys and validation.

---

## Phase 5: User Story 3 — Student Receives Automatic Feedback (Priority: P3)

**Goal**: Student opens a self-correcting exercise on a dedicated page, answers, submits, and
immediately sees per-question correctness + correct answers for wrong questions + overall score.
Single-submission lock enforced. Teacher sees score in the homework student list.

**Independent Test**: As student assigned an EXERCISE → `/aprendizaje` shows card with "Empezar
ejercicio" → click → navigates to `/aprendizaje/tarea/{id}` → radios / checkboxes / text fields
render → submit → immediate feedback per question + overall `%` shown → reload → exercise read-only
with same result → second PUT to `/answers` returns 409. Check via SQL:
`SELECT status, score_percent FROM homework_submissions WHERE assignment_id='<id>' AND user_id='<student>';`

### Implementation for User Story 3

- [x] T031 [P] [US3] Create `back-end/src/main/java/com/kuky/backend/learning/dto/ExerciseQuestionDto.java` — record with `id`, `kind`, `prompt`, `options: List<StudentOptionDto>` (where `StudentOptionDto` is `record(id, label)` — **no correct flag**); FILL_BLANK questions have an empty options list
- [x] T032 [P] [US3] Create `back-end/src/main/java/com/kuky/backend/learning/dto/ExerciseResponse.java` — record with `id`, `title`, `instructions`, `format` (`"EXERCISE"`), `status` (HomeworkStatus), `questions: List<ExerciseQuestionDto>`, `result: ExerciseResultResponse` (null when PENDING)
- [x] T033 [P] [US3] Create `back-end/src/main/java/com/kuky/backend/learning/dto/SubmitExerciseRequest.java` — record with `answers: List<AnswerDto>` where `AnswerDto` is `record(questionId, selectedOptionIds, answerText)`
- [x] T034 [P] [US3] Create `back-end/src/main/java/com/kuky/backend/learning/dto/ExerciseResultResponse.java` — record with `scorePercent` (int), `fullyCorrectCount` (int), `totalQuestions` (int), `questions: List<QuestionResultDto>` where `QuestionResultDto` is `record(questionId, score, correct, correctOptionIds, acceptedAnswers)`
- [x] T035 [US3] Update `back-end/src/main/java/com/kuky/backend/admin/dto/AssigneeDto.java` — add `Integer scorePercent` field (null for MANUAL submissions)
- [x] T036 [US3] Create `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` — `grade(submissionId, assignmentId, List<AnswerDto>) → ExerciseResultResponse`: load questions+options, apply per-question rules (SINGLE_CHOICE: 1 iff selected == the one correct; MULTI_CHOICE partial credit: `(C_sel + I_unsel) / N`; FILL_BLANK: `strip() + toLowerCase(Locale.ROOT)` match against accepted answers; unanswered = 0), compute `scorePercent = round(avg*100)` and `fullyCorrectCount`, write `HomeworkAnswer` rows + upsert submission to `status=GRADED` + `score_percent`, return `ExerciseResultResponse` (depends on T010, T011, T031, T032, T033, T034)
- [x] T037 [US3] Update `back-end/src/main/java/com/kuky/backend/learning/dto/HomeworkItemResponse.java` — add `HomeworkFormat format` and `Integer scorePercent` fields
- [x] T038 [US3] Update `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkItems.java` — populate `format` and `scorePercent` in the `HomeworkItemResponse` builder (depends on T037, T009)
- [x] T039 [US3] Update `back-end/src/main/java/com/kuky/backend/learning/controller/LearningController.java` — add `GET /api/v1/learning/homework/{id}` (returns `ExerciseResponse`; 404 if not assigned or format≠EXERCISE); add `PUT /api/v1/learning/homework/{id}/answers` (delegates to `ExerciseGradingService`; 400 if format≠EXERCISE; 409 if already GRADED) (depends on T036, T031, T032, T033, T034)
- [x] T040 [US3] Update `front-end/src/lib/learning.ts` — add `format: HomeworkFormat` and `scorePercent: number | null` to `HomeworkItem` type; add `getExercise(id: string): Promise<ExerciseResponse>` and `submitExercise(id: string, answers: AnswerPayload[]): Promise<ExerciseResult>` functions with the full request/response types
- [x] T041 [P] [US3] Create `front-end/src/components/learning/ExerciseResult.tsx` — displays per-question feedback: correct/incorrect indicator, correct answer revealed for wrong questions; overall percentage + fully-correct count summary ("80% — 8 de 10 preguntas correctas")
- [x] T042 [US3] Create `front-end/src/components/learning/HomeworkExercisePage.tsx` — fetches exercise via `getExercise(id)`; renders questions (RadioGroup for SINGLE_CHOICE, Checkbox for MULTI_CHOICE, Input for FILL_BLANK); submit button calls `submitExercise`; renders `ExerciseResult` after submission; shows read-only inputs + stored `ExerciseResult` when `status === 'GRADED'` (depends on T041)
- [x] T043 [US3] Create `front-end/src/routes/aprendizaje_.tarea.$homeworkId.tsx` — flat route file; guards authenticated user (redirect guest to `/cuenta`); renders `HomeworkExercisePage` with `homeworkId` param (depends on T042)
- [x] T044 [US3] Update `front-end/src/components/learning/HomeworkItemCard.tsx` — when `format === 'EXERCISE'` and `status === 'PENDING'`: render "Empezar ejercicio" link to `/aprendizaje/tarea/{id}` instead of `HomeworkSubmitDialog`; when `status === 'GRADED'`: show score badge (e.g. "Calificada — 80%")

**Checkpoint**: US3 fully functional — students take exercises and receive immediate graded feedback.

---

## Phase 6: User Story 4 — Manual Homework Unchanged (Priority: P3)

**Goal**: Manual homework continues to use the existing free-text dialog and
`PENDING → SUBMITTED → REVIEWED` lifecycle. The new exercise paths cannot be reached for MANUAL
homework.

**Independent Test**: Create a MANUAL homework, assign to student → student sees "Entregar tarea"
button (dialog, not the exercise page) → submit free-text → status becomes "Entregada" (SUBMITTED)
with no score → teacher reviews manually. A `PUT /homework/{id}/answers` on a MANUAL id returns
400 `SUBMISSION_NOT_ALLOWED`.

### Implementation for User Story 4

- [x] T045 [US4] Update `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkSubmissionService.java` — add a guard at the start of the manual submit flow: if `assignment.format == EXERCISE` throw `SubmissionNotAllowedException` (400 `SUBMISSION_NOT_ALLOWED`) so the existing `PUT /learning/homework/{id}` free-text endpoint rejects exercise assignments
- [x] T046 [US4] Verify `front-end/src/components/learning/HomeworkItemCard.tsx` (updated in T044) handles `format === 'MANUAL'` correctly: "Entregar tarea" button opens `HomeworkSubmitDialog` as before; `HomeworkSubmitDialog.tsx` itself is **not modified**

**Checkpoint**: All four user stories independently functional and non-regressing.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Unit tests and end-to-end validation.

- [x] T047 [P] Create `back-end/src/test/java/com/kuky/backend/learning/ExerciseGradingServiceTest.java` — unit tests: SINGLE_CHOICE correct (score=1), SINGLE_CHOICE wrong (score=0), MULTI_CHOICE all-correct (score=1), MULTI_CHOICE partial (expected fraction), MULTI_CHOICE all-wrong (score=0), FILL_BLANK exact match (score=1), FILL_BLANK case-insensitive match (score=1), FILL_BLANK accent-mismatch (score=0), FILL_BLANK trimmed whitespace match (score=1), unanswered question (score=0), overall `scorePercent = round(avg*100)`, `fullyCorrectCount` correct count
- [x] T048 [P] Create `back-end/src/test/java/com/kuky/backend/admin/HomeworkExerciseAdminServiceTest.java` — unit tests: EXERCISE with 0 questions rejected (FR-010), SINGLE_CHOICE with 0 correct options rejected (FR-008), SINGLE_CHOICE with 2 correct options rejected (FR-008), MULTI_CHOICE with 0 correct options rejected (FR-008), FILL_BLANK with 0 accepted answers rejected (FR-009), MANUAL with questions rejected (FR-017), valid EXERCISE passes validation and persists questions+options via `HomeworkQuestionRepository`
- [x] T049 Run end-to-end validation per `quickstart.md` — execute all four stories (dedicated page, exercise authoring, student feedback, manual regression) plus the negative/regression checks; fix any issues found

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (T001 migration) — **blocks all user stories**
- **US1 (Phase 3)**: Depends on Foundational completion
- **US2 (Phase 4)**: Depends on US1 completion (HomeworkEditorPage must exist to extend it)
- **US3 (Phase 5)**: Depends on Foundational completion; needs US2 to have exercises to take
- **US4 (Phase 6)**: Depends on US3 completion (T044 — HomeworkItemCard must handle both formats)
- **Polish (Phase 7)**: Depends on all user stories

### User Story Dependencies

- **US1 (P1)**: Can start after Phase 2 — no story dependencies
- **US2 (P2)**: Depends on US1 — HomeworkEditorPage and admin DTOs must exist
- **US3 (P3)**: Depends on Phase 2 — most backend grading work is independent; depends on US2 existing exercises
- **US4 (P3)**: Depends on US3 (HomeworkItemCard) — a single guard in HomeworkSubmissionService

### Within Each Story

- Backend DTOs before services; services before controllers
- Frontend API client (`lib/`) before components; components before route files
- Core implementation before integration

### Parallel Opportunities

- **Phase 2**: T002 and T003 run in parallel; T006 and T007 run in parallel after T003
- **Phase 3**: T012, T013, T014 run in parallel; T019 and T020 run in parallel
- **Phase 4**: T023 runs in parallel with T024/T025 preparation; T027 (QuestionEditorCard) before T028 (QuestionListEditor)
- **Phase 5**: T031, T032, T033, T034 all run in parallel; T041 (ExerciseResult) runs in parallel with backend work
- **Phase 7**: T047 and T048 run in parallel

---

## Parallel Example: Phase 2

```
# Batch 1 — enums in parallel:
Task T002: Create HomeworkFormat.java enum
Task T003: Create QuestionKind.java enum

# Batch 2 — models in parallel (after T002, T003):
Task T004: Update HomeworkAssignment.java (needs T002)
Task T005: Update HomeworkSubmission.java (needs T002)
Task T006: Create HomeworkQuestion.java (needs T003)
Task T007: Create QuestionOption.java (needs T003)
Task T008: Create HomeworkAnswer.java (needs T003)

# Batch 3 — repositories (after models):
Task T009: Update ContentRepository.java (needs T004)
Task T010: Create HomeworkQuestionRepository.java (needs T006, T007)
Task T011: Create HomeworkAnswerRepository.java (needs T008)
```

## Parallel Example: Phase 5 (US3)

```
# Batch 1 — DTOs in parallel:
Task T031: Create ExerciseQuestionDto.java
Task T032: Create ExerciseResponse.java
Task T033: Create SubmitExerciseRequest.java
Task T034: Create ExerciseResultResponse.java
Task T041: Create ExerciseResult.tsx (frontend)

# Batch 2 — service (after DTOs):
Task T036: Create ExerciseGradingService.java

# Batch 3 — controller + component (after service):
Task T039: Update LearningController.java
Task T042: Create HomeworkExercisePage.tsx
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup (T001)
2. Complete Phase 2: Foundational (T002–T011)
3. Complete Phase 3: US1 (T012–T022)
4. **STOP and VALIDATE**: Teacher can create/edit manual homework on a dedicated page; dialog is gone
5. Merge or demo — delivers cleaner authoring UX immediately

### Incremental Delivery

1. Setup + Foundational → migration applied, enums and repos ready
2. US1 (T012–T022) → dedicated authoring page; teacher uses it for all homework
3. US2 (T023–T030) → exercise authoring; teacher can build quizzes
4. US3 (T031–T044) → student takes exercises; automatic grading live
5. US4 (T045–T046) → guard prevents wrong endpoint use; manual flow confirmed unchanged
6. Polish (T047–T049) → tests green; quickstart passes

### Notes

- [P] = different files with no shared-state dependencies; safe to run concurrently
- Commit after each logical group (one story phase = one commit minimum)
- Verify each checkpoint before advancing to the next story
- When deleting `HomeworkEditorDialog.tsx` (T022): also search for any remaining import references and remove them before committing
- For FILL_BLANK grading (T036): use `value.strip().toLowerCase(Locale.ROOT)` on both sides — this preserves accents (does not strip diacritics) and achieves the required case-insensitive-but-accent-exact matching per research §5
