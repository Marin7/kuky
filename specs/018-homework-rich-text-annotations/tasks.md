---

description: "Task list template for feature implementation"
---

# Tasks: Rich Text Formatting for Writing Homework Feedback

**Input**: Design documents from `/specs/018-homework-rich-text-annotations/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api.md](contracts/api.md), [quickstart.md](quickstart.md)

**Tests**: Backend test tasks are included — this repo has an established JUnit 5/Spring Boot Test convention for every prior feature, and `research.md`'s Testing approach explicitly calls out the behaviors to cover. No frontend test tasks are included — the repo has no frontend test framework today (confirmed in research), and per the constitution's Development Workflow rule, frontend changes are verified manually in a running browser (see Polish phase and `quickstart.md`).

**Organization**: Tasks are grouped by user story (from `spec.md`) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on another incomplete task in the same batch)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths are included in every task description

---

## Phase 1: Setup

**Purpose**: Confirm the environment is ready before touching the schema

- [X] T001 Confirm local dev baseline: PostgreSQL 18 `kuky_dev` running and Flyway at `V27` before adding `V28`; run `./gradlew flywayMigrate --args='--spring.profiles.active=local'` to verify a clean baseline (per `quickstart.md` Prerequisites/Setup). No new dependencies are added anywhere in this feature (per `research.md` Decisions 1–2) — do not add a rich-text editor or HTML-sanitizer library to `back-end/build.gradle` or `front-end/package.json`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The shared `FormattedText` data model and editor/viewer components that every user story depends on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 [P] Add Flyway migration `back-end/src/main/resources/db/migration/V28__add_homework_feedback.sql`: add nullable `feedback TEXT` and `reviewed_at TIMESTAMPTZ` columns to `homework_submissions`; backfill existing non-JSON `response_text` values by wrapping them as `[{"text": "<existing value>"}]` so old plain-text answers keep rendering correctly. Per `data-model.md` Migration section.
- [X] T003 [P] Create `FormattedTextSegment` value type + validator in `back-end/src/main/java/com/kuky/backend/learning/model/FormattedTextSegment.java`: fields `text` (required, non-empty), `color` (enum: `red`, `green`, `blue`, `neutral`, optional), `highlight` (enum: `yellow`, `green`, `pink`, optional), `strike` (boolean, optional). Include a static validator that rejects an empty segment list, empty `text` in any segment, unknown enum values, and a concatenated visible-text length over 2000 chars. Per `data-model.md` Formatted Text section and `contracts/api.md`.
- [X] T004 Add `feedback` (`List<FormattedTextSegment>`, nullable) and `reviewedAt` (`Instant`, nullable) fields to `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkSubmission.java` (depends on T003).
- [X] T005 [P] Create front-end `FormattedText` types + helpers in `front-end/src/components/learning/richtext/types.ts`: a `Segment` type mirroring the backend enums (`color`: `red`/`green`/`blue`/`neutral`; `highlight`: `yellow`/`green`/`pink`; `strike`: boolean), a `FormattedText = Segment[]` type, and a `visibleLength(segments)` helper for the 2000-char counter.
- [X] T006 [P] Build `FormattingToolbar` component in `front-end/src/components/learning/richtext/FormattingToolbar.tsx`: buttons for the 4 text colors, 3 highlight colors, and a strikethrough toggle, applying to the caller-supplied current selection (depends on T005).
- [X] T007 Build `RichTextEditor` component in `front-end/src/components/learning/richtext/RichTextEditor.tsx`: a selection-tracking editor (native `textarea` selection APIs, no `contentEditable`) operating directly on a `Segment[]` value, rendering `FormattingToolbar`, allowing color/highlight/strike to be combined on the same selection, and stripping any pasted content down to plain text before it enters the segment array (depends on T005, T006).
- [X] T008 [P] Build `RichTextViewer` component in `front-end/src/components/learning/richtext/RichTextViewer.tsx`: read-only render of a `Segment[]` value to styled `<span>` elements reflecting color/highlight/strike (depends on T005).

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 - Teacher gives formatted feedback on a Writing submission (Priority: P1) 🎯 MVP

**Goal**: A teacher can find a Writing submission awaiting feedback (via a cross-student queue and a per-student profile indicator), write feedback using color/highlight/strikethrough, save it, and have the submission move to `REVIEWED`.

**Independent Test**: Open a `SUBMITTED` writing submission via the queue or the student profile indicator, write feedback combining all three formatting types, save it; confirm the submission's status becomes `REVIEWED` with `reviewedAt` set and it no longer appears in the queue or as a pending indicator.

### Tests for User Story 1

- [X] T009 [P] [US1] Add tests to `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java`: review queue returns only MANUAL/`SUBMITTED` submissions; saving feedback rejects an empty segment array (`400`), rejects feedback whose visible text exceeds 2000 chars (`400`, via the shared `FormattedTextSegment` validator — same rule as the student-submit path), accepts a single segment with `color`, `highlight`, and `strike` all set simultaneously (combinable formatting, FR-006a) and preserves all three on read-back, rejects re-reviewing an already-`REVIEWED` submission (`409 ALREADY_REVIEWED`), rejects reviewing a still-`PENDING` submission (`409 NOT_SUBMITTED`), and on success sets `status = REVIEWED` and `reviewedAt`. Also assert a segment `text` containing markup-like content (e.g. `<script>alert(1)</script>`) is accepted and stored verbatim as inert plain text (never interpreted as markup) — this is what guarantees SC-004 server-side, independent of any client-side paste handling.
- [X] T010 [P] [US1] Create `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminControllerIntegrationTest.java` covering the 3 new endpoints on the existing `HomeworkAdminController` end-to-end: `ADMIN`-only auth enforcement, happy path, and the `400`/`409` error responses above.

### Implementation for User Story 1

- [X] T011 [P] [US1] Create `HomeworkReviewQueueItemDto` (`submissionId`, `studentId`, `studentName`, `assignmentTitle`, `submittedAt`) in `back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkReviewQueueItemDto.java`.
- [X] T012 [P] [US1] Create `HomeworkSubmissionAdminDto` (`submissionId`, `studentId`, `studentName`, `assignmentTitle`, `status`, `response`, `feedback`, `submittedAt`, `reviewedAt`) in `back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkSubmissionAdminDto.java`.
- [X] T013 [P] [US1] Create `SaveHomeworkFeedbackRequest` (`feedback: List<FormattedTextSegment>`) in `back-end/src/main/java/com/kuky/backend/admin/dto/SaveHomeworkFeedbackRequest.java`.
- [X] T014 [P] [US1] Create `AlreadyReviewedException` and `NotSubmittedException` in `back-end/src/main/java/com/kuky/backend/learning/exception/`.
- [X] T015 [US1] Add `findSubmittedManualQueue()`, `findDetailById(submissionId)`, and `updateFeedback(submissionId, feedbackJson, reviewedAt)` to `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkSubmissionRepository.java` (depends on T004).
- [X] T016 [US1] Add `getReviewQueue()`, `getSubmissionDetail(submissionId)`, and `saveFeedback(submissionId, feedback)` to `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`. `saveFeedback` MUST run `feedback` through the same `FormattedTextSegment` validator used by the student-submit path (T003/T028) — rejecting empty arrays, invalid enum values, and visible text over 2000 chars (FR-009 applies to feedback too, not just the student's answer) — before enforcing the terminal-`REVIEWED` and not-yet-`SUBMITTED` transition rules (depends on T011, T012, T013, T014, T015).
- [X] T017 [US1] Add `GET /api/v1/admin/homework/submissions`, `GET /api/v1/admin/homework/submissions/{submissionId}`, and `PUT /api/v1/admin/homework/submissions/{submissionId}/feedback` to `back-end/src/main/java/com/kuky/backend/admin/controller/HomeworkAdminController.java` (depends on T016).
- [X] T018 [P] [US1] Register `AlreadyReviewedException` → `409 ALREADY_REVIEWED` and `NotSubmittedException` → `409 NOT_SUBMITTED` handlers in `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java` (depends on T014).
- [X] T019 [P] [US1] Add a `needsReview` boolean to `back-end/src/main/java/com/kuky/backend/admin/dto/StudentProfileHomeworkDto.java` and compute it (true when MANUAL-format and `status = SUBMITTED`) in `back-end/src/main/java/com/kuky/backend/admin/service/StudentProfileAdminService.java` (depends on T004). Add corresponding cases to the existing `back-end/src/test/java/com/kuky/backend/admin/StudentProfileAdminServiceTest.java` asserting `needsReview` is `true` only for MANUAL+`SUBMITTED` homeworks and `false` otherwise (`PENDING`, `REVIEWED`, `GRADED`, or `EXERCISE`-format).
- [X] T020 [P] [US1] Add `getHomeworkReviewQueue()`, `getHomeworkSubmission(id)`, and `saveHomeworkFeedback(id, feedback)` API functions and matching types to `front-end/src/lib/admin.ts` (depends on T005).
- [X] T021 [P] [US1] Build `HomeworkReviewQueue` component (lists submissions awaiting review, opens `HomeworkReviewDialog` on click) in `front-end/src/components/admin/homework/HomeworkReviewQueue.tsx` (depends on T020).
- [X] T022 [P] [US1] Build `HomeworkReviewDialog` component (renders the student's answer via `RichTextViewer`, feedback via `RichTextEditor`, save action, surfaces `400`/`409` errors) in `front-end/src/components/admin/homework/HomeworkReviewDialog.tsx` (depends on T007, T008, T020).
- [X] T023 [US1] Wire `HomeworkReviewQueue` into `front-end/src/components/admin/homework/HomeworkTab.tsx` above the existing assignment list (depends on T021, T022).
- [X] T024 [US1] Add a `needsReview` indicator and a "review" action to each homework row in `front-end/src/routes/panel_.alumnos.$studentId.tsx`, opening `HomeworkReviewDialog` (depends on T019, T022).

**Checkpoint**: User Story 1 is fully functional and independently testable — the teacher can discover, review, and give formatted feedback on a submission through either discoverability path.

---

## Phase 4: User Story 2 - Student formats their own answer while writing it (Priority: P2)

**Goal**: A student can apply color, highlight, and strikethrough (independently or combined) to their own Writing homework answer while composing it, and the formatting survives submission and reload.

**Independent Test**: Open the Writing homework, apply all three formatting types (combined) to the draft answer, submit, reload the page, and confirm the same formatting is preserved.

### Tests for User Story 2

- [X] T025 [P] [US2] Add tests to `back-end/src/test/java/com/kuky/backend/learning/HomeworkSubmissionServiceTest.java`: `submit()` persists a formatted segment array correctly, rejects invalid `color`/`highlight` enum values (`400`), rejects a segment array whose visible text exceeds 2000 chars (regardless of markup size), and still blocks resubmission once `REVIEWED` (existing rule, unchanged).

### Implementation for User Story 2

- [X] T026 [P] [US2] Change `response` from `String` to `List<FormattedTextSegment>` in `back-end/src/main/java/com/kuky/backend/learning/dto/SubmitHomeworkRequest.java` (depends on T003).
- [X] T027 [P] [US2] Change `response` to `List<FormattedTextSegment>` and add a `feedback: List<FormattedTextSegment>` field to `back-end/src/main/java/com/kuky/backend/learning/dto/HomeworkItemResponse.java` (depends on T003).
- [X] T028 [US2] Update `submit()` in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkSubmissionService.java` to validate (via T003's validator) and JSON-serialize the segment array before persisting (depends on T026).
- [X] T029 [US2] Update `upsert(...)` in `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkSubmissionRepository.java` to store/read the JSON-encoded segment array in `response_text` (depends on T028).
- [X] T030 [US2] Update `front-end/src/lib/learning.ts` so `submitHomework()`'s request/response types use `FormattedText` instead of `string` (depends on T005).
- [X] T031 [US2] Replace the `<Textarea>` in `front-end/src/components/learning/ManualAnswerForm.tsx` with `RichTextEditor`, and update its `localStorage` draft-autosave format from a plain string to the segment array, keeping the existing 2000-visible-char counter (depends on T007, T030).

**Checkpoint**: User Stories 1 and 2 both work independently.

---

## Phase 5: User Story 3 - Student reviews their formatted submission and feedback together (Priority: P3)

**Goal**: Once a submission is reviewed, the student sees their own original formatted answer and the teacher's formatted feedback together, both read-only.

**Independent Test**: After a submission has been reviewed (via User Story 1), open it from the homework history and confirm both the original formatted answer and the teacher's formatted feedback render correctly, with no edit controls available.

### Tests for User Story 3

- [X] T032 [P] [US3] Add a test to `back-end/src/test/java/com/kuky/backend/learning/LearningControllerIntegrationTest.java`: the learning overview response includes a non-null `feedback` field once a submission is `REVIEWED`, and both `response` and `feedback` round-trip as valid segment arrays.

### Implementation for User Story 3

- [X] T033 [US3] Ensure the learning overview builder includes the `feedback` field for MANUAL homework items wherever `HomeworkItemResponse` is assembled in `back-end/src/main/java/com/kuky/backend/learning/service/LearningService.java` (depends on T027).
- [X] T034 [US3] Update `front-end/src/components/learning/HomeworkWritePage.tsx` to render the student's own answer and (when present) the teacher's feedback via `RichTextViewer`, and to disable all editing controls once `status === "REVIEWED"` (depends on T008, T030).

**Checkpoint**: All three user stories are independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish rounding out the feature after all three stories are functional

- [X] T035 [P] Add i18n strings for the review queue, review dialog, formatting toolbar, and `needsReview` indicator labels to `front-end/src/i18n/locales/en.ts`, `front-end/src/i18n/locales/es.ts`, and `front-end/src/i18n/locales/ro.ts`.
- [X] T036 [P] Harden the paste handler in `front-end/src/components/learning/richtext/RichTextEditor.tsx` to confirm all pasted content (from Word, Google Docs, etc.) is reduced to plain text with no foreign styling, links, or images before entering the segment model (edge case from `spec.md`).
- [X] T037 Run all four `quickstart.md` validation scenarios end-to-end in a running browser (student formatting, teacher review, student viewing feedback, edge cases) and confirm the Mailpit inbox at `:8025` is unchanged after a review is saved (FR-012).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories.
- **User Stories (Phase 3+)**: All depend on Foundational phase completion.
  - US1 (P1) has no dependency on US2/US3 and delivers the MVP on its own.
  - US2 (P2) touches the student-submit path only (different files from US1's review path) and can proceed in parallel with US1 once Foundational is done.
  - US3 (P3) depends on US1 (needs a reviewed submission with feedback to exist) and reuses `learning.ts` changes from US2 (T030) — implement after both.
- **Polish (Phase 6)**: Depends on all three user stories being complete.

### Within Each User Story

- Tests are written alongside (and validate) the implementation tasks in the same phase.
- DTOs/exceptions before services; services before controllers; backend endpoints before the frontend calls that consume them.
- Shared `richtext/` components (Foundational) before any story-specific UI that uses them.

### Parallel Opportunities

- All Foundational tasks marked [P] (T002, T003, T005) can start together; T006/T008 can then run together once T005 is done; T007 needs T006 first.
- Once Foundational is done, US1 and US2 can be worked on in parallel (different files, no shared dependency beyond Foundational).
- Within US1: T011–T014 in parallel; T018/T019/T020 in parallel once their single dependency (T014/T004/T005 respectively) is done; T021/T022 in parallel once T020 (and T007/T008) are done.
- Within US2: T026/T027 in parallel; T025 (test) can be written alongside them.

---

## Parallel Example: User Story 1

```bash
# Once T004 (Foundational) and T005 (Foundational) are done, launch together:
Task: "Create HomeworkReviewQueueItemDto in back-end/.../admin/dto/HomeworkReviewQueueItemDto.java"
Task: "Create HomeworkSubmissionAdminDto in back-end/.../admin/dto/HomeworkSubmissionAdminDto.java"
Task: "Create SaveHomeworkFeedbackRequest in back-end/.../admin/dto/SaveHomeworkFeedbackRequest.java"
Task: "Create AlreadyReviewedException and NotSubmittedException in back-end/.../learning/exception/"

# Once T020 is done, launch together:
Task: "Build HomeworkReviewQueue in front-end/src/components/admin/homework/HomeworkReviewQueue.tsx"
Task: "Build HomeworkReviewDialog in front-end/src/components/admin/homework/HomeworkReviewDialog.tsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational (blocks everything).
3. Complete Phase 3: User Story 1.
4. **STOP and VALIDATE**: run `quickstart.md` scenario 2 (teacher review) — this alone delivers the core, previously-nonexistent value (teacher feedback on Writing homework).

### Incremental Delivery

1. Setup + Foundational → foundation ready.
2. Add User Story 1 → validate independently → the MVP: teacher can review and give formatted feedback.
3. Add User Story 2 → validate independently → student can format their own answer.
4. Add User Story 3 → validate independently → student sees both sides together.
5. Polish: i18n, paste hardening, full quickstart pass.

---

## Notes

- [P] tasks touch different files and have no incomplete dependency within their own batch.
- [Story] labels map every task back to `spec.md`'s prioritized user stories for traceability.
- No frontend automated tests exist in this repo; frontend verification is manual (running browser + `quickstart.md`), per the constitution's Development Workflow rule.
- Commit after each task or logical group.
- Avoid: vague tasks, same-file conflicts marked [P], cross-story dependencies that break independent testability (US1 and US2 are intentionally independent of each other; only US3 depends on US1's output).
