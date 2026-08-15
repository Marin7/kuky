---
description: "Task list for in-app activity notifications"
---

# Tasks: In-App Activity Notifications

**Input**: Design documents from `specs/041-notification-system/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/notifications-api.md](./contracts/notifications-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit per [plan.md](./plan.md) (submit → unseen; assign unit/quiz → unseen; open submission/attempt/unit/quiz → seen; editor/list GET does not mark; unassign/delete; existing rows not unseen; `USER` has no student badges). Frontend — browser checks in [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Shared UI primitive and i18n so later stories do not invent parallel dots or copy.

- [X] T001 [P] Add `notification` i18n keys (aria-labels for Panel, Tareas, Pruebas de evaluación, Mi aprendizaje, list/row marks) in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`
- [X] T002 [P] Create `front-end/src/components/NotificationDot.tsx` — presence-only mark + `aria-label`; no count
- [X] T003 [P] Add `front-end/src/lib/notifications.ts` with `BadgeSummary` (`panel`, `homework`, `quiz`, `learning`) and `getBadges()` / `markUnitSeen(unitId)` calling [contracts/notifications-api.md](./contracts/notifications-api.md)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Unseen columns, badge API, and `NotificationService` used by every story. No user-story work until this phase completes.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Create Flyway `back-end/src/main/resources/db/migration/V22__activity_unseen_flags.sql`: add `teacher_seen_at` on `homework_submissions` and `quiz_attempts`; `student_seen_at` on `unit_assignments`, `quiz_assignees`, and `homework_targets`; backfill existing submitted/assigned rows to `NOW()`; partial indexes per [data-model.md](./data-model.md)
- [X] T005 Add JDBC `back-end/src/main/java/com/kuky/backend/notification/repository/NotificationRepository.java`: badge counts; mark `teacher_seen_at` / `student_seen_at` idempotently (`WHERE seen_at IS NULL`); helpers for list-flag queries
- [X] T006 Implement `back-end/src/main/java/com/kuky/backend/notification/service/NotificationService.java` and `dto/BadgeSummary.java` — ADMIN: `homework`/`quiz`/`panel`; STUDENT: `learning`; `USER`: all false ([research.md](./research.md) §3, §5)
- [X] T007 Add `back-end/src/main/java/com/kuky/backend/notification/controller/NotificationController.java` `GET /api/v1/notifications/badges` and allow it for authenticated users in `back-end/src/main/java/com/kuky/backend/config/SecurityConfig.java`
- [X] T008 JUnit `back-end/src/test/java/com/kuky/backend/notification/NotificationServiceTest.java`: empty DB / backfilled rows → all badges false; `USER` → `learning` false; ADMIN with no unseen → `panel` false

**Checkpoint**: Boot with `local` applies V22; `GET /notifications/badges` returns four booleans; historical work is not unseen.

---

## Phase 3: User Story 1 - Teacher sees that a student turned in homework (Priority: P1) — MVP

**Goal**: Homework submit sets teacher unseen. Panel + Tareas dots (not Pruebas). Homework card + each unseen student row. Opening that student’s review/result (Tareas or profile) clears only that submission.

**Independent Test**: A submits homework; teacher sees Panel + Tareas icons; open A’s submitted work (not the editor) and those icons clear ([quickstart.md](./quickstart.md) §1).

### Implementation for User Story 1

- [X] T009 [US1] On successful submit in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkSubmissionService.java` (`submit` and `submitAnswers`), leave `teacher_seen_at` NULL for SUBMITTED/GRADED (do not notify PENDING or presentation activities)
- [X] T010 [US1] In `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` mark teacher seen on `getSubmissionDetail` and exercise-result GET; add `hasUnseenSubmissions` on `HomeworkAdminItem` and `unseen` on `AssigneeDto` / homework review-queue item / `StudentProfileHomeworkDto` (`back-end/src/main/java/com/kuky/backend/admin/dto/`)
- [X] T011 [P] [US1] Extend `front-end/src/lib/admin.ts` types for those `unseen` / `hasUnseenSubmissions` flags
- [X] T012 [US1] Fetch `getBadges()` on pathname/`auth-changed` in `front-end/src/components/SiteHeader.tsx` (Panel dot from `panel`) and `front-end/src/components/admin/AdminPanel.tsx` (Tareas tab from `homework`; Pruebas stays off until US2)
- [X] T013 [US1] Render `NotificationDot` on homework cards, review-queue rows, and assignee rows in `front-end/src/components/admin/homework/HomeworkAdminList.tsx`, `HomeworkReviewQueue.tsx`, and `HomeworkEditorPage.tsx` — opening the editor or assignee list must not clear icons
- [X] T014 [US1] Show homework-row dots on `front-end/src/routes/panel_.alumnos.$studentId.tsx`; opening that student’s submitted work (existing review/result dialogs) must hit the same GETs as T010
- [X] T015 [US1] JUnit in `back-end/src/test/java/com/kuky/backend/admin/` and/or `learning/`: auto and write submit → unseen; GET submission/exercise-result → seen; GET homework list/editor does not mark; two students on one homework are independent ([quickstart.md](./quickstart.md) §2)

**Checkpoint**: Teacher homework path works end-to-end. FR-001, FR-005–FR-006 (homework), FR-008–FR-010, FR-019, SC-001/SC-007 for homework.

---

## Phase 4: User Story 2 - Teacher sees that a student submitted a test (Priority: P1)

**Goal**: Quiz submit sets teacher unseen. Panel + Pruebas de evaluación dots (not Tareas from this event). Quiz card + each unseen attempt row. Opening that attempt (tab or profile) clears only that attempt. Mixed homework+quiz lights both tabs.

**Independent Test**: A submits a quiz; teacher sees Panel + Pruebas icons, not Tareas from this event; open A’s attempt (not the quiz editor) and those icons clear ([quickstart.md](./quickstart.md) §3–4).

### Implementation for User Story 2

- [X] T016 [US2] On quiz submit in `back-end/src/main/java/com/kuky/backend/quiz/service/QuizService.java`, set `teacher_seen_at` NULL on that attempt (`IN_PROGRESS` is not unseen)
- [X] T017 [US2] In `back-end/src/main/java/com/kuky/backend/quiz/service/QuizAdminService.java` mark teacher seen on attempt GET; add `hasUnseenAttempts` on `QuizAdminListItem`, `unseen` on `QuizAttemptListItem`, review-queue item, and `StudentQuizSummary` in `back-end/src/main/java/com/kuky/backend/quiz/dto/`
- [X] T018 [P] [US2] Extend quiz types in `front-end/src/lib/admin.ts` (and `front-end/src/lib/quiz.ts` if the student summary is shared)
- [X] T019 [US2] Pruebas tab `NotificationDot` from `badges.quiz` in `front-end/src/components/admin/AdminPanel.tsx`; dots on `front-end/src/components/quiz/admin/QuizTab.tsx`, `QuizReviewQueue.tsx`, `QuizAttemptsPanel.tsx`
- [X] T020 [US2] Quiz-row dots on `front-end/src/routes/panel_.alumnos.$studentId.tsx`; opening attempt review uses the GET from T017
- [X] T021 [US2] JUnit in `back-end/src/test/java/com/kuky/backend/quiz/`: submit → unseen; attempt GET → seen; quiz editor/list GET does not mark; homework unseen does not set `quiz` badge and vice versa; both unseen → `panel` true and both tab flags true

**Checkpoint**: Teacher quiz path works; mixed case matches FR-006 and SC-003. FR-002, FR-019 for quizzes.

---

## Phase 5: User Story 3 - Student sees newly assigned work on Mi aprendizaje (Priority: P1)

**Goal**: New unit or quiz assignment sets student unseen. Mi aprendizaje nav + unit card / quiz row. Opening the unit page or quiz page (no start/submit required) clears that item. Adding homework to an existing unit, Tareas-only assignees, activities, and grading do not notify.

**Independent Test**: Assign a new unit and a new quiz to A; A sees Mi aprendizaje + item dots; open unit then quiz without submitting; B unassigned sees nothing ([quickstart.md](./quickstart.md) §5–7).

### Implementation for User Story 3

- [X] T022 [US3] Change `back-end/src/main/java/com/kuky/backend/units/service/UnitService.java` `setAssignees` so **added** students get `student_seen_at` NULL and **remaining** rows keep `student_seen_at`; do not reset on `setHomeworks` ([research.md](./research.md) §7–8)
- [X] T023 [US3] Same insert/delete-diff (preserve `student_seen_at`) in `back-end/src/main/java/com/kuky/backend/quiz/service/QuizAdminService.java` `setAssignees`; do not notify on `HomeworkAdminService.setAssignees`
- [X] T024 [US3] Add `unseen` on student `UnitRef` via `back-end/src/main/java/com/kuky/backend/learning/service/LearningService.java` and `back-end/src/main/java/com/kuky/backend/learning/dto/UnitRef.java`; add `unseen` on student quiz list in `QuizService` / `front-end/src/lib/quiz.ts`
- [X] T025 [US3] `POST /api/v1/learning/units/{unitId}/seen` in `back-end/src/main/java/com/kuky/backend/learning/controller/LearningController.java` (idempotent; 404 `UNIT_NOT_FOUND` if not assigned) and mark student quiz seen as a side effect of `GET /quizzes/{quizId}` in `QuizService` — not on `GET /learning` or `GET /quizzes` list
- [X] T026 [US3] SiteHeader Mi aprendizaje dot from `badges.learning`; unit-card dots in `front-end/src/components/learning/LearningContent.tsx`; quiz-row dots in `front-end/src/components/learning/AssignedQuizList.tsx`; call `markUnitSeen` on mount in `front-end/src/components/learning/UnitLearningView.tsx` when `unitId` is not null (never for `/aprendizaje/otros`)
- [X] T027 [US3] JUnit in `back-end/src/test/java/com/kuky/backend/units/` and `quiz/`: new unit/quiz assign → that student’s `learning` badge; remaining assignee not re-notified; `setHomeworks` / homework `setAssignees` / activity submit do not create unseen; POST unit seen and GET quiz mark seen; GET homework take does not mark the unit; `USER` never gets `learning` true

**Checkpoint**: Student assignment path works. FR-003, FR-004, FR-007, FR-017, FR-018, SC-002.

---

## Phase 6: User Story 4 - Icons stay until the news is actually opened (Priority: P2)

**Goal**: Parent surfaces (Panel, tabs, Mi aprendizaje, editors, lists) never clear unseen. Navigation/refresh while already signed in is enough to show new icons. Unassign/delete removes leftover icons.

**Independent Test**: Two unseen homework submissions; open Panel/Tareas/editor/list only — icons remain; open one student from Tareas or profile — only that one clears ([quickstart.md](./quickstart.md) §2, §8–9).

### Implementation for User Story 4

- [X] T028 [US4] Confirm `SiteHeader` (and AdminPanel badges) refetch `getBadges()` on pathname change and `auth-changed` in `front-end/src/components/SiteHeader.tsx` so an already-signed-in teacher sees new icons without logout ([spec.md](./spec.md) US4 scenario 4)
- [X] T029 [US4] JUnit: GET `/admin/homework`, GET `/admin/homework/{id}`, GET `/admin/quizzes`, GET `/learning`, GET `/quizzes` list do **not** set any `*_seen_at`; unassign student / delete homework or quiz removes unseen (cascade) in `back-end/src/test/java/com/kuky/backend/notification/`
- [X] T030 [US4] After opening a submission/attempt/unit/quiz in the UI, refetch badges (and list flags) so parent dots disappear before leaving the page (SC-004) — homework review dialogs, `QuizReviewDialog`, `UnitLearningView` after `markUnitSeen`, quiz take page after GET

**Checkpoint**: Persistence and unassign/delete match FR-010, FR-014, SC-004, SC-006.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Docs and a full quickstart pass after all stories.

- [X] T031 [P] Update Current pages / key notes in `CLAUDE.md` to mention in-site unseen dots (Panel/Tareas/Pruebas, Mi aprendizaje); no email/inbox
- [X] T032 [P] Grep `back-end/` for activity submit/assign and homework `setAssignees` to confirm no `student_seen_at` / `teacher_seen_at` writes there
- [X] T033 Run [quickstart.md](./quickstart.md) in the browser (teacher A/B homework + quiz, student unit/quiz assign, no-notify cases, already-signed-in navigation)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS** all user stories
- **US1 (Phase 3)**: After Phase 2 — MVP
- **US2 (Phase 4)**: After Phase 2; Panel/Tareas already exist from US1; mixed-tab case needs US1 data
- **US3 (Phase 5)**: After Phase 2; SiteHeader already fetches badges from US1
- **US4 (Phase 6)**: After US1–US3 (asserts they do not over-clear)
- **Polish (Phase 7)**: After stories to ship

### User Story Dependencies

- **User Story 1 (P1)**: After Foundational — no other story
- **User Story 2 (P1)**: After Foundational — independently testable; mixed Panel case is stronger after US1
- **User Story 3 (P1)**: After Foundational — independently testable (student badges)
- **User Story 4 (P2)**: After US1–US3 — persistence/regression on those surfaces

### Parallel Opportunities

- T001, T002, T003 in parallel
- After T004: T005 can start; T006 depends on T005; T007 depends on T006
- After Foundational: US1 and US3 can proceed in parallel if staffed (different files except `SiteHeader` / `AdminPanel` — serialize those)
- US2 overlaps `AdminPanel.tsx` and `panel_.alumnos.$studentId.tsx` with US1 — do US1 first on those files, then US2
- T031 and T032 in parallel after stories

---

## Parallel Example: Setup

```text
Task: "Add notification i18n keys in front-end/src/i18n/locales/{es,en,ro}.ts"
Task: "Create NotificationDot in front-end/src/components/NotificationDot.tsx"
Task: "Add getBadges/markUnitSeen in front-end/src/lib/notifications.ts"
```

---

## Parallel Example: User Story 3 (after Foundational)

```text
Task: "UnitService.setAssignees preserve student_seen_at in back-end/.../units/service/UnitService.java"
Task: "QuizAdminService.setAssignees preserve student_seen_at in back-end/.../quiz/service/QuizAdminService.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup
2. Phase 2 Foundational
3. Phase 3 US1 (homework teacher icons)
4. **STOP and VALIDATE** [quickstart.md](./quickstart.md) §1–2

### Incremental Delivery

1. Setup + Foundational → badges API live, historical work quiet
2. US1 → teacher homework dots (MVP)
3. US2 → teacher quiz dots + mixed tabs
4. US3 → student Mi aprendizaje dots
5. US4 → persistence / unassign proofs
6. Polish → CLAUDE.md + full quickstart

---

## Notes

- [P] = different files, no incomplete dependencies
- Do not create a notifications inbox, email, websocket, or numeric count
- `NotificationDot` is the only visual marker
- Commit after each task or logical group
- Stop at any checkpoint to validate the story independently
