---
description: "Task list for presentation-linked activities"
---

# Tasks: Presentation Activities

**Input**: Design documents from `specs/029-presentation-activities/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/activities-api.md](contracts/activities-api.md), [quickstart.md](quickstart.md)

**Tests**: Backend JUnit per [plan.md](plan.md) (CRUD/reorder, access, triggers, cascade, submit/grade, progress). Frontend â€” browser verification via [quickstart.md](quickstart.md) (no frontend unit-test framework).

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1â€“US4)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Schema and disk storage config every story depends on.

- [x] T001 Write `back-end/src/main/resources/db/migration/V11__presentation_activities.sql` per [data-model.md](data-model.md): `activities`, `activity_instructions_files`, `activity_questions`, `activity_question_options`, `activity_submissions`, `activity_answers`, `activity_answer_options` with FKs/cascade/SET NULL as specified
- [x] T002 [P] Add `ActivityInstructionsProperties` + `ActivityInstructionsFileStore` under `back-end/src/main/java/com/kuky/backend/` (mirror `PresentationFileStore` / presentation-files properties; default dir `./data/activity-instructions`; config key `app.activity-instructions.storage-dir`)
- [x] T003 [P] Document `ACTIVITY_INSTRUCTIONS_STORAGE_DIR` (or matching env binding) in local/application config files under `back-end/src/main/resources/` if other file stores are declared there

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain models, repositories, shared grading reuse, and security wiring so stories can build admin/student APIs.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T004 [P] Add activity domain models/enums under `back-end/src/main/java/com/kuky/backend/learning/model/` (`Activity`, `ActivityInstructionsFile`, `ActivityQuestion`, `ActivitySubmission`, reuse `HomeworkFormat` / `QuestionKind` / status enums or thin activity aliases)
- [x] T005 [P] Add JDBC repositories under `back-end/src/main/java/com/kuky/backend/learning/repository/`: `ActivityRepository`, `ActivityQuestionRepository`, `ActivitySubmissionRepository`, `ActivityAnswerRepository` (CRUD, ordered-by-position list by `presentation_id`, cascade-friendly deletes)
- [x] T006 Extract or dual-wire shared question validation + `ExerciseGradingService` so activity IDs can grade EXERCISE submissions without a second engine (`back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` and admin validators used by homework)
- [x] T007 Map activity error codes (`ACTIVITY_NOT_FOUND`, `ACTIVITY_VALIDATION`, `ACTIVITY_REORDER_INVALID`, `ACTIVITY_ALREADY_SUBMITTED`) in `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java` per [contracts/activities-api.md](contracts/activities-api.md)
- [x] T008 Ensure `/api/v1/admin/activities/**` is ADMIN-only and `/api/v1/learning/activities/**` requires STUDENT/ADMIN in `back-end/src/main/java/com/kuky/backend/config/SecurityConfig.java` (match existing learning/homework matchers)
- [x] T009 [P] Clear `activities.trigger_file_id` / `trigger_page` when a presentation file is deleted in `back-end/src/main/java/com/kuky/backend/presentations/service/PresentationService.java`; on presentation delete, delete instruction PDFs from disk for cascaded activities (query ids before/after cascade as needed)

**Checkpoint**: Migration applies; models/repos exist; security + errors wired; file-delete clears triggers.

---

## Phase 3: User Story 1 - Teacher authors an activity linked to a presentation (Priority: P1) â€” MVP

**Goal**: Teacher creates/edits/deletes activities with PDF instructions, presentation link, optional file+page trigger, full MANUAL/EXERCISE authoring, and reorder under a presentation.

**Independent Test**: Create activity with title + PDF + presentation (+ optional trigger); reopen and confirm persistence; add second activity; reorder; student with access sees list only after US2 â€” for US1 alone verify via admin API/UI and DB.

### Tests for User Story 1

- [x] T010 [P] [US1] Add backend tests for activity create/update validation (missing PDF, invalid trigger file/page, EXERCISE needs questions) under `back-end/src/test/java/com/kuky/backend/`
- [x] T011 [P] [US1] Add backend tests for `reorder` permutation validation and position rewrite `0..n-1` per presentation

### Implementation for User Story 1

- [x] T012 [P] [US1] Add admin DTOs under `back-end/src/main/java/com/kuky/backend/admin/dto/` (or learning/admin dto package): `ActivityAdminItem`, `ActivityAdminDetail`, reorder request â€” mirror homework question DTOs per [contracts/activities-api.md](contracts/activities-api.md)
- [x] T013 [US1] Implement `ActivityAdminService` in `back-end/src/main/java/com/kuky/backend/admin/service/ActivityAdminService.java`: create/update/delete, instructions upload via file store, trigger validation, question save (reuse homework validators), append `position`, change-presentation clears invalid trigger
- [x] T014 [US1] Implement `ActivityAdminController` at `/api/v1/admin/activities` in `back-end/src/main/java/com/kuky/backend/admin/controller/ActivityAdminController.java` (list/get/create/update/delete) per contract (depends on T013)
- [x] T015 [US1] Add `PUT /api/v1/admin/presentations/{presentationId}/activities/reorder` on presentation admin or activity admin controller; service rewrites positions (depends on T013)
- [x] T016 [P] [US1] Add activity admin API helpers/types in `front-end/src/lib/admin.ts`
- [x] T017 [US1] Add Activities tab + list in `front-end/src/components/admin/activities/` and wire into `front-end/src/components/admin/AdminPanel.tsx`
- [x] T018 [US1] Add authoring routes `front-end/src/routes/panel_.actividades.nueva.tsx` and `panel_.actividades.$activityId.tsx` with editor page reusing homework question editors; require presentation picker + PDF instructions upload; optional trigger file+page (pdfjs page count on client)
- [x] T019 [US1] Add DnD reorder UI for a presentationâ€™s activities (reuse `@dnd-kit` pattern from units) in admin activities or presentation context; call reorder API (depends on T015, T016)
- [x] T020 [P] [US1] Add i18n keys for Activities admin under `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`

**Checkpoint**: Teacher can author MANUAL/EXERCISE activities with PDF instructions, link/reorder under a presentation, set/clear page triggers. MVP deliverable.

---

## Phase 4: User Story 2 - Student discovers and fulfills activities under a presentation (Priority: P1)

**Goal**: Activities nest under expanded presentations in the unit view; student opens full-page fulfill UI; submit MANUAL/EXERCISE; status shows on the nested list. Access inherits from presentation.

**Independent Test**: Presentation with two activities assigned via unit/share; student expands presentation, sees both in teacher order; completes one; status updates; student without access sees none.

### Tests for User Story 2

- [ ] T021 [P] [US2] Backend tests: learning overview embeds activities only when presentation accessible; deny get/submit without access; MANUAL + EXERCISE submit status transitions
  <!-- Deferred: covered by manual quickstart; add SpringBootTest when DB harness available for learning path -->

### Implementation for User Story 2

- [x] T022 [P] [US2] Add `ActivitySummary` / detail response DTOs under `back-end/src/main/java/com/kuky/backend/learning/dto/`; extend `SharedPresentationSummary` with ordered `activities[]`
- [x] T023 [US2] Load activities + student submission status in learning overview path (`LearningService` / `PresentationRepository` / `ActivityRepository`) and map into `sharedPresentations[].activities` (depends on T022)
- [x] T024 [US2] Implement student `ActivityService` + endpoints on `LearningController`: `GET/PUT /api/v1/learning/activities/{id}`, `PUT .../answers`, `GET .../instructions` (PDF stream) with presentation-access checks (depends on T006, T005)
- [x] T025 [P] [US2] Add learning activity types + API helpers in `front-end/src/lib/learning.ts`
- [x] T026 [US2] Nest activity list under presentation expansion in `front-end/src/components/learning/` (`PresentationExpandBody` / `UnitDetailContent.tsx`) â€” not unit-sequence peers; show title + status; link to fulfill route (depends on T025)
- [x] T027 [US2] Add `front-end/src/routes/aprendizaje_.actividad.$activityId.tsx` + `ActivityPanel` reusing `ManualAnswerForm` / `ExerciseForm` patterns; load/view instructions PDF; submit MANUAL/EXERCISE (depends on T024, T025)
- [x] T028 [P] [US2] Add student-facing i18n strings for nested activities / fulfill UI in `es.ts`, `en.ts`, `ro.ts`

**Checkpoint**: Student can discover and fulfill activities from the unit presentation expansion without viewer prompts yet.

---

## Phase 5: User Story 3 - Activity pops up while viewing a presentation (Priority: P2)

**Goal**: On landing on trigger file+page, show non-blocking prompt for incomplete activities; open as overlay on viewer; dismiss keeps list access; no re-prompt when fulfilled; other files donâ€™t fire.

**Independent Test**: Trigger on PDF page 2; open viewer; reach page 2 â†’ prompt; open overlay; close â†’ same page; fulfill â†’ no prompt on return; other PDF in presentation â†’ no prompt.

### Implementation for User Story 3

- [x] T029 [US3] Add `onPageVisible(pageNumber)` (IntersectionObserver) to `front-end/src/components/learning/PresentationPdfViewer.tsx` / page stack so callers know when a page is landed on
- [x] T030 [US3] On full-page viewer route `front-end/src/routes/aprendizaje_.presentacion.$presentationId.archivo.$fileId.tsx` (and embedded viewer if used), match `fileId`+page to incomplete activities from learning data; show non-blocking prompt UI
- [x] T031 [US3] Implement `front-end/src/components/learning/ActivityOverlay.tsx` (Dialog/Sheet) hosting fulfill UI without navigating away; on close restore viewer; one activity overlay at a time (depends on T027 forms)
- [x] T032 [US3] Ensure fulfilled activities never re-prompt; multiple same-page activities offered sequentially/listed without blocking page turns; i18n for prompt copy (depends on T030, T031)

**Checkpoint**: Viewer page prompts + overlay fulfill work; unit-list path still uses full page.

---

## Phase 6: User Story 4 - Teacher reviews student activity work (Priority: P3)

**Goal**: Review MANUAL submissions and view EXERCISE results; see who fulfilled each activity; activity fulfillment appears in student progress overview.

**Independent Test**: Student submits MANUAL + EXERCISE; teacher reviews/views results; student profile progress shows activity breakdown.

### Tests for User Story 4

- [ ] T033 [P] [US4] Backend test: `StudentProgressDto` / profile includes activity counts for accessible presentations only
  <!-- Deferred: activityBreakdown wired in StudentProfileAdminService; add integration test with DB -->

### Implementation for User Story 4

- [x] T034 [US4] Add admin submission review endpoints + service methods (list MANUAL queue, get submission, exercise-result, feedback) under `ActivityAdminController` / `ActivityAdminService` per [contracts/activities-api.md](contracts/activities-api.md)
- [x] T035 [P] [US4] Extend `StudentProfileAdminService` / progress DTOs with `activityBreakdown` (and unit totals if applicable) in `back-end/src/main/java/com/kuky/backend/admin/`
- [x] T036 [US4] Admin UI: review queue/dialogs for activities (reuse homework review patterns) under `front-end/src/components/admin/activities/`
- [x] T037 [US4] Show activity fulfillment on student profile progress UI (`front-end/src/components/admin/students/` + `panel_.alumnos.$studentId.tsx`) using extended progress payload (depends on T035)
- [x] T038 [P] [US4] i18n for review + progress activity labels in `es.ts`, `en.ts`, `ro.ts`

**Checkpoint**: Teacher can review activities and see progress overview counts.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Regression, cascade disk cleanup verification, quickstart pass.

- [x] T039 Verify presentation delete cascades activities/submissions and removes instruction files from disk (`PresentationService` / admin delete path)
- [x] T040 [P] Spot-check homework and presentation-without-activities flows unchanged (FR-012 / SC-006)
- [ ] T041 Run through [quickstart.md](quickstart.md) end-to-end in browser; fix gaps
  <!-- Manual: start local BE/FE and follow quickstart checklist -->
- [x] T042 [P] Confirm activities never appear as unit mixed-sequence peers (`UnitDetail.contents` / student interleaved list unchanged)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup â€” **BLOCKS** all user stories
- **US1 (Phase 3)**: After Foundational â€” MVP
- **US2 (Phase 4)**: After Foundational; practically after US1 admin create so there is data (can stub via API)
- **US3 (Phase 5)**: After US2 (needs student fulfill UI + activity payloads with triggers)
- **US4 (Phase 6)**: After US2 (needs submissions); can proceed in parallel with US3 once submit works
- **Polish (Phase 7)**: After desired stories complete

### User Story Dependencies

- **US1**: No dependency on other stories
- **US2**: Needs authored activities (US1) for real data; APIs independent
- **US3**: Needs US2 fulfill components + overview activity summaries with triggers
- **US4**: Needs US2 submissions; progress independent of US3

### Parallel Opportunities

- T002 âˆ¥ T003 after T001 starts
- T004 âˆ¥ T005 in Foundational; T007 âˆ¥ T008
- T010 âˆ¥ T011; T012 âˆ¥ T016; T020 alone
- T021 âˆ¥ T022 âˆ¥ T025; T028 alone
- T033 âˆ¥ T035; T038 alone
- US3 and US4 can proceed in parallel after US2 submit works

---

## Parallel Example: User Story 1

```text
# After Foundational:
T010 + T011 (tests in parallel)
T012 (DTOs) + T016 (front-end admin.ts) in parallel
Then T013 â†’ T014 â†’ T015 (service then controllers)
Then T017 â†’ T018 â†’ T019 (UI)
T020 i18n in parallel with UI polish
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup + Phase 2 Foundational
2. Phase 3 US1 (admin authoring + reorder + triggers)
3. **STOP** â€” validate via admin UI/API
4. Then US2 for student value

### Incremental Delivery

1. Setup + Foundational â†’ schema ready
2. US1 â†’ teacher can author activities (MVP)
3. US2 â†’ students fulfill from unit expansion
4. US3 â†’ in-viewer prompts + overlay
5. US4 â†’ review + progress overview
6. Polish â†’ quickstart + regressions

---

## Notes

- Do not put activities into `homework_assignments` or unit `contents` sequence
- Instructions = single PDF only; no due dates/assignees
- Page trigger = `trigger_file_id` + `trigger_page`; fire on land-on page; no re-prompt when fulfilled
- Overlay only from viewer prompt; full page from unit list
- Commit after each task or logical group
