---
description: "Task list for student homework correction and feedback dots"
---

# Tasks: Student Dots for Homework Corrections and Feedback

**Input**: Design documents from `specs/047-homework-grade-dots/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/homework-grade-dots-api.md](./contracts/homework-grade-dots-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit per [plan.md](./plan.md) (finalize → unseen; progress save does not; auto-submit does not; GRADED feedback/annotations do; awaiting comments wait; GRADED result GET/seen POST → seen; unit seen POST does not; activity/quiz saves do not; existing rows not unseen; other student has no badge). Frontend — browser checks in [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Document the existing student `unseen` flag so later stories OR in review news instead of adding a second marker.

- [X] T001 [P] Update the `unseen` JSDoc on `HomeworkItem` in `front-end/src/lib/learning.ts` to mean assignment-unseen **or** teacher-review-unseen (one `NotificationDot`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Review-unseen columns, badge query, submit-as-seen, and `NotificationService` helpers used by every story. No user-story work until this phase completes.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 Create Flyway `back-end/src/main/resources/db/migration/V25__homework_student_review_unseen.sql`: add `student_grade_seen_at` and `student_feedback_seen_at` on `homework_submissions`; backfill all existing rows to `NOW()`; partial index where either column IS NULL per [data-model.md](./data-model.md)
- [X] T003 In `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkSubmissionRepository.java` `upsert` and `upsertGraded` (and any other insert/submit upsert), set both review columns to `NOW()` on insert and on submit conflict so auto-grade/submit is never review-unseen ([research.md](./research.md) §2)
- [X] T004 Extend `back-end/src/main/java/com/kuky/backend/notification/repository/NotificationRepository.java`: include review-unseen submissions in `hasUnseenLearning`; add `findUnseenReviewHomeworkIds`; idempotent `markStudentReviewSeen` (both columns `NOW()` where NULL); `markStudentGradeUnseen` / `markStudentFeedbackUnseen` (set NULL); `markStudentFeedbackSeen` (feedback column `NOW()` for FR-015)
- [X] T005 Extend `back-end/src/main/java/com/kuky/backend/notification/service/NotificationService.java` with helpers wrapping T004 (create grade/feedback unseen, mark review seen for a user’s `GRADED` submission by assignment id, clear feedback unseen)
- [X] T006 JUnit in `back-end/src/test/java/com/kuky/backend/notification/NotificationServiceTest.java` and/or `NotificationUnseenIntegrationTest.java`: V25 backfill → `learning` not true from review; student auto and write submit → both review columns not NULL; `USER` still `learning` false

**Checkpoint**: Boot with `local` applies V25; historical grades are quiet; submit does not light Mi aprendizaje from review.

---

## Phase 3: User Story 1 - Student sees that the teacher has corrected their homework (Priority: P1) — MVP

**Goal**: Teacher finalize sets grade unseen. Student sees Mi aprendizaje + unit + homework dots. Opening the **GRADED result** (GET exercise or homework seen POST) clears that news. Progress save and auto-submit do not notify. Opening the unit does not clear review news.

**Independent Test**: A submits manual/Writing homework; teacher progress-saves (no icon); teacher finalizes; A sees Mi aprendizaje / unit / homework icons; open result → icons for this news clear ([quickstart.md](./quickstart.md) §1).

### Implementation for User Story 1

- [X] T007 [US1] In `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` `saveFeedback`, after a successful **finalize** (not progress), call `NotificationService` to NULL `student_grade_seen_at`; also NULL `student_feedback_seen_at` if that save has a written comment and/or annotations ([contracts/homework-grade-dots-api.md](./contracts/homework-grade-dots-api.md))
- [X] T008 [US1] In `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` `getExercise`, if this user’s submission is `GRADED`, mark both review columns seen (keep existing homework-target assignment seen)
- [X] T009 [US1] In `back-end/src/main/java/com/kuky/backend/learning/service/LearningService.java` `markHomeworkSeen`, if this user’s submission is `GRADED`, mark review seen; set homework `unseen` true when assignment-unseen **or** review-unseen (`unseenHomeworkIds` ∪ review homework ids from T004)
- [X] T010 [P] [US1] Confirm `front-end/src/components/learning/unitGroups.ts`, `HomeworkItemCard.tsx`, `UnitDetailContent.tsx`, `LearningContent.tsx`, and `SiteHeader.tsx` already show a single `NotificationDot` from `unseen` / `badges.learning` — no new component; after opening a GRADED result, existing `notifyBadgesChanged` / list refresh in `HomeworkInlinePanel.tsx`, `HomeworkWritePage.tsx`, `HomeworkExercisePage.tsx` (and listening/reading pages) must run so parent dots clear (SC-003)
- [X] T011 [US1] JUnit in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` and `notification/`: progress save → not review-unseen; finalize → that student’s `learning` true; GET `GRADED` exercise / POST homework seen on `GRADED` → review seen; GET while `SUBMITTED` / POST unit seen / GET `/learning` do not mark review seen; other student B has no badge ([quickstart.md](./quickstart.md) §1, §5)

**Checkpoint**: Student correction path works. FR-001, FR-003–FR-008, FR-012–FR-013, SC-001, SC-004, SC-005 (partial + auto-submit already from T003/T006).

---

## Phase 4: User Story 2 - Student sees that the teacher has left feedback on their homework (Priority: P1)

**Goal**: On already-visible (`GRADED`) homework, a written comment **or** annotations-only save sets feedback unseen (one homework indicator). Awaiting comments wait until finalize (bundled with US1). Clearing all comments/annotations dismisses feedback news only (FR-015).

**Independent Test**: Auto-scored homework + exercise feedback → A’s icons; open result → clear. Annotations only on graded free-text → same. Awaiting comment without finalize → no icon ([quickstart.md](./quickstart.md) §2–3).

### Implementation for User Story 2

- [X] T012 [US2] In `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` `saveExerciseFeedback`, if submission is `GRADED`: NULL `student_feedback_seen_at` when feedback text **changes** to non-empty; `markStudentFeedbackSeen` when cleared to empty and no other student-visible comment/annotation remains; no write if unchanged ([research.md](./research.md) §3, §8)
- [X] T013 [US2] In `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` `saveFeedback`, on already-`GRADED` saves: NULL `student_feedback_seen_at` when written comment and/or annotated formatted answers **change**; on clear-all comments and annotations call `markStudentFeedbackSeen` (do not touch `student_grade_seen_at`); awaiting (non-finalize) still must not write review columns
- [X] T014 [US2] JUnit: GRADED exercise feedback create → `learning` true; annotation-only change → true; awaiting comment without finalize → false; finalize with comment already stored → one unseen (grade and/or feedback NULL, still one list `unseen`); clear all feedback on auto-scored with no unseen grade → review quiet; clear feedback after unseen finalize → grade unseen remains (FR-015)

**Checkpoint**: Feedback path works. FR-002, FR-009–FR-010, FR-015, SC-002.

---

## Phase 5: User Story 3 - Later teacher changes become news again (Priority: P2)

**Goal**: After the student opened the result, a later visible score change or new/updated comment/annotation sets unseen again. Extra saves while still unseen stay one indicator. No-op save does not re-notify.

**Independent Test**: Open graded result (icons gone); teacher changes percent → icons return; open again; teacher updates feedback/annotations → icons return; no-op save → no icon ([quickstart.md](./quickstart.md) §4, §6.2).

### Implementation for User Story 3

- [X] T015 [US3] In `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` `saveFeedback` (already-`GRADED` re-edit), compare before/after percents vs comments/annotations: NULL only the column(s) whose student-visible value changed; if already review-unseen, further changes keep a single unseen (do not invent a second flag)
- [X] T016 [US3] JUnit in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java`: seen then percent change → grade unseen; seen then comment/annotation change → feedback unseen; save with identical percents and marks → columns unchanged; two edits while still unseen → still one `unseen` on the learning homework item

**Checkpoint**: Re-notify matches FR-011, SC-003 “do not come back for that same teacher action.”

---

## Phase 6: User Story 4 - Icons stay until the student actually opens the result (Priority: P2)

**Goal**: Mi aprendizaje / unit visit / assignment-seen POST do not clear review news. Assignment and review news are independent. Activity and quiz teacher saves never write review columns. Unassign/delete removes leftover review icons. Already-signed-in navigation shows new icons.

**Independent Test**: Unseen correction; open Mi aprendizaje and unit only — icons remain; open result — clear. New unit assign + later finalize: opening unit clears only assignment news ([quickstart.md](./quickstart.md) §5–8).

### Implementation for User Story 4

- [X] T017 [US4] JUnit in `back-end/src/test/java/com/kuky/backend/notification/NotificationUnseenIntegrationTest.java`: `POST /learning/units/{id}/seen` does not set review columns; `GET /learning` does not; unassign from unit / delete homework removes that student’s review unseen; `ActivityAdminService` feedback/finalize and quiz grade PUTs do not NULL review columns (grep + test)
- [X] T018 [P] [US4] Grep `back-end/src/main/java/com/kuky/backend/admin/service/ActivityAdminService.java` and quiz admin save paths to confirm no `student_grade_seen_at` / `student_feedback_seen_at` writes (FR-017)
- [X] T019 [US4] Confirm `front-end/src/components/SiteHeader.tsx` already refetches `getBadges()` on pathname/`auth-changed` so an already-signed-in student sees new review icons without logout; `UnitLearningView.tsx` `markUnitSeen` must not call a review-seen API ([spec.md](./spec.md) US4)

**Checkpoint**: Persistence and independence match FR-008, FR-014, FR-017, FR-018, SC-004, SC-006.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Docs and a full quickstart pass after all stories.

- [X] T020 [P] Update Current pages / homework-grading notes in `CLAUDE.md` so student Mi aprendizaje dots also cover teacher homework corrections and feedback (still no email/inbox)
- [X] T021 Run [quickstart.md](./quickstart.md) in the browser (finalize, auto-score feedback, annotations-only, awaiting wait, later edits, assignment vs review, clear feedback, activity/quiz out of scope, already-signed-in navigation)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS** all user stories
- **US1 (Phase 3)**: After Phase 2 — MVP
- **US2 (Phase 4)**: After Phase 2; shares `HomeworkAdminService.java` with US1 — do T007 before T012/T013
- **US3 (Phase 5)**: After US1–US2 (same save path, before/after compare)
- **US4 (Phase 6)**: After US1–US3 (asserts they do not over-clear)
- **Polish (Phase 7)**: After stories to ship

### User Story Dependencies

- **User Story 1 (P1)**: After Foundational — no other story
- **User Story 2 (P1)**: After Foundational — independently testable on auto-scored + exercise feedback even before US1 UI; `saveFeedback` annotation branch should follow T007
- **User Story 3 (P2)**: After US1–US2 — later-change compare lives on the same admin save
- **User Story 4 (P2)**: After US1–US3 — persistence/regression

### Parallel Opportunities

- T001 in parallel with T002
- T003 after T002 (columns must exist)
- T004 after T002; T005 after T004; T006 after T003–T005
- After Foundational: T008 and T009 can proceed in parallel (different services); T010 after T009’s DTO meaning
- T012 after T007 (same `HomeworkAdminService`); T018 can run in parallel with T017 once US1–US3 exist
- T020 in parallel with T021 prep after stories

---

## Parallel Example: Foundational (after T002)

```text
Task: "Set review columns NOW() on upsert in back-end/.../HomeworkSubmissionRepository.java"
Task: "Extend NotificationRepository review queries in back-end/.../notification/repository/NotificationRepository.java"
```

(T003 and T004 touch different files.)

---

## Parallel Example: User Story 1 (after T007)

```text
Task: "Mark review seen on GRADED GET in back-end/.../ExerciseGradingService.java"
Task: "OR review into homework unseen in back-end/.../LearningService.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup
2. Phase 2 Foundational
3. Phase 3 US1 (finalize → student dots → open result clears)
4. **STOP and VALIDATE** [quickstart.md](./quickstart.md) §1

### Incremental Delivery

1. Setup + Foundational → columns live, submits quiet, historical work quiet
2. US1 → correction dots (MVP)
3. US2 → feedback / annotations / FR-015
4. US3 → re-notify on later visible changes
5. US4 → persistence / assignment independence / out-of-scope
6. Polish → CLAUDE.md + full quickstart

---

## Notes

- [P] = different files, no incomplete dependencies
- Do not add a notifications inbox, email, websocket, numeric count, or `reviewUnseen` JSON field
- Do not hook `ActivityAdminService` or quiz review
- Reuse `NotificationDot`; homework `unseen` is the only list flag
- Commit after each task or logical group
- Stop at any checkpoint to validate the story independently
