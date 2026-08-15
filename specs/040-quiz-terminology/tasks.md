---
description: "Task list for quiz terminology (replace placement test)"
---

# Tasks: Quiz Terminology (Replace Placement Test)

**Input**: Design documents from `specs/040-quiz-terminology/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/quiz-api.md](./contracts/quiz-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit per [plan.md](./plan.md) (assign/take/submit/grade, snapshot isolation, per-skill %, unassign, `USER` cannot take). Frontend — browser checks in [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted. Homework/activities stay unchanged except extracting `QuestionScoring`.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Shared types and i18n keys so later stories do not invent parallel vocabularies.

- [X] T001 [P] Add `quiz` i18n namespaces (nav, empty list, admin tab, skills READING/WRITING/GRAMMAR/LISTENING, statuses) in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts` — product term Quiz/Quizzes; do not reuse `placement.*` keys
- [X] T002 [P] Create student/admin TypeScript types and empty API stubs in `front-end/src/lib/quiz.ts` matching [contracts/quiz-api.md](./contracts/quiz-api.md) (`QuizListItem`, take DTO with `skill` + media, result `skills[]`, `status`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Drop placement schema/code, add quiz tables and scoring helper, wire auth and errors. No user story work until this phase completes.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Create Flyway `back-end/src/main/resources/db/migration/V21__quizzes_replace_placement.sql`: `DROP` all `placement_*` tables CASCADE; create `quizzes`, `quiz_questions`, `quiz_question_options`, `quiz_assignees`, `quiz_attempts` (`quiz_snapshot JSONB NOT NULL`), `quiz_answers` per [data-model.md](./data-model.md)
- [X] T004 [P] Add quiz models in `back-end/src/main/java/com/kuky/backend/quiz/model/` (`Quiz`, `QuizQuestion`, `QuizQuestionOption`, `QuizAssignee`, `QuizAttempt`, `QuizAnswer`, `QuizSkill`, `QuizAttemptStatus`)
- [X] T005 [P] Add JDBC repositories in `back-end/src/main/java/com/kuky/backend/quiz/repository/` (CRUD quiz + questions/options with retire-vs-delete like homework V20; assignees replace-set; attempts unique `(quiz_id,user_id)`; answers)
- [X] T006 Extract per-kind 0–1 scoring (including numbered SINGLE_CHOICE units) from `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` into `back-end/src/main/java/com/kuky/backend/learning/service/QuestionScoring.java`; leave homework persistence in `ExerciseGradingService` ([research.md](./research.md) §4)
- [X] T007 [P] Add `QuizNotFoundException`, `QuizNotAssignedException`, `QuizAlreadySubmittedException` in `back-end/src/main/java/com/kuky/backend/quiz/exception/` and map to `QUIZ_NOT_FOUND` / `QUIZ_NOT_ASSIGNED` / `QUIZ_ALREADY_SUBMITTED` in `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java`; remove placement exception handlers (`PLACEMENT_NOT_FOUND`, section codes)
- [X] T008 Restrict `/api/v1/quizzes/**` to `hasAnyRole("STUDENT","ADMIN")` in `back-end/src/main/java/com/kuky/backend/config/SecurityConfig.java` ([research.md](./research.md) §6)
- [X] T009 Delete `back-end/src/main/java/com/kuky/backend/placement/` and `back-end/src/test/java/com/kuky/backend/placement/`; remove leftover imports of placement types elsewhere in `back-end/`

**Checkpoint**: Boot with `local` profile applies V21; placement API is gone; `QuestionScoring` is callable; quiz tables exist empty.

---

## Phase 3: User Story 1 - Teacher authors a standalone quiz with mixed skills (Priority: P1) — MVP

**Goal**: Paula creates/edits a quiz as one ordered question list with a skill on each question, listening media per listening question, homework question kinds, and assigns specific `STUDENT`s. Unpublished = no assignees (and/or no questions). Not a homework, not on a unit.

**Independent Test**: Create a quiz with reading, grammar, listening (media), and writing questions; assign student A not B; only A would see it once US2 exists. Block assign with zero questions; reject non-student assignees ([quickstart.md](./quickstart.md) §2).

### Implementation for User Story 1

- [X] T010 [US1] Implement `QuizAdminService` in `back-end/src/main/java/com/kuky/backend/quiz/service/QuizAdminService.java`: create/update title+description+questions (skill required; LISTENING requires media kinds from homework; auto kinds need keys; FREE_TEXT no key); retire-vs-delete questions; **do not** rewrite existing `quiz_snapshot`; PUT assignees only `STUDENT` and only if ≥1 live question
- [X] T011 [US1] Add `QuizAdminController` in `back-end/src/main/java/com/kuky/backend/quiz/controller/QuizAdminController.java` for `GET/POST /admin/quizzes`, `GET/PUT/DELETE /admin/quizzes/{id}`, `PUT /admin/quizzes/{id}/assignees` per [contracts/quiz-api.md](./contracts/quiz-api.md)
- [X] T012 [P] [US1] Add admin API client functions in `front-end/src/lib/admin.ts` (list/create/get/update/delete quiz, set assignees) calling `/admin/quizzes`
- [X] T013 [US1] Build admin list + tab: `front-end/src/components/quiz/admin/QuizTab.tsx` (or `QuizAdminList.tsx`) and replace the `placement` tab in `front-end/src/components/admin/AdminPanel.tsx` with `quizzes`
- [X] T014 [US1] Author pages `front-end/src/routes/panel_.quizzes.nueva.tsx` and `front-end/src/routes/panel_.quizzes.$quizId.tsx` plus `front-end/src/components/quiz/admin/QuizEditorPage.tsx`: reuse `QuestionEditorCard` / `QuestionListEditor` / `AudioSourceEditor` / `StudentMultiSelect` from `front-end/src/components/admin/homework/`; add required skill select per question; per-question media when skill is LISTENING
- [X] T015 [US1] JUnit: assign validation (no questions, non-student), save mixed skills, live edit does not mutate existing snapshots in `back-end/src/test/java/com/kuky/backend/quiz/QuizAdminServiceTest.java`

**Checkpoint**: Teacher can author and assign a mixed-skill quiz from the panel. FR-004–FR-009, FR-016 (admin tab). Homework list/units unchanged.

---

## Phase 4: User Story 2 - Assigned student takes a quiz (Priority: P1)

**Goal**: Assigned students see only their quizzes, start captures a snapshot, submit once, auto-grade via `QuestionScoring`, overall + per-skill figures, FREE_TEXT awaits teacher. Login required. Not shown as homework.

**Independent Test**: A takes the mixed quiz and submits; auto skills show % immediately; writing awaits; B does not see it; `USER` cannot take; second submit 409 ([quickstart.md](./quickstart.md) §3, §6).

### Implementation for User Story 2

- [X] T016 [US2] Implement snapshot helper + `QuizGradingService` in `back-end/src/main/java/com/kuky/backend/quiz/service/` using `QuestionScoring`; overall combination rules = mixed homework; per-skill = same rules on snapshot subset; ALL_AUTO → `GRADED`; any FREE_TEXT → `SUBMITTED` ([data-model.md](./data-model.md), FR-012, FR-013)
- [X] T017 [US2] Implement `QuizService` in `back-end/src/main/java/com/kuky/backend/quiz/service/QuizService.java`: list assigned; GET take starts `IN_PROGRESS` + writes `quiz_snapshot` (idempotent); IN_PROGRESS returns snapshot not live; SUBMITTED/GRADED return snapshot + scores; unassigned without submitted attempt → `QUIZ_NOT_ASSIGNED`
- [X] T018 [US2] Add `QuizController` in `back-end/src/main/java/com/kuky/backend/quiz/controller/QuizController.java`: `GET /quizzes`, `GET /quizzes/{id}`, `PUT /quizzes/{id}/answers` per [contracts/quiz-api.md](./contracts/quiz-api.md)
- [X] T019 [P] [US2] Implement `front-end/src/lib/quiz.ts` fetch helpers (`listMyQuizzes`, `getQuiz`, `submitQuizAnswers`)
- [X] T020 [US2] Student list `front-end/src/routes/quizzes.tsx` and take/result `front-end/src/routes/quizzes.$quizId.tsx` with `front-end/src/components/quiz/`; reuse `MixedHomeworkForm` / `ExerciseForm` / result views; show skill headings when skill changes; hide keys until submit
- [X] T021 [US2] Add Quizzes to `front-end/src/components/SiteHeader.tsx` for authenticated non-admin users (`/quizzes`); empty list copy when none assigned
- [X] T022 [US2] JUnit in `back-end/src/test/java/com/kuky/backend/quiz/`: assignee-only list; snapshot-at-start isolation (edit live, in-progress still old questions); all-auto GRADED + skill breakdown; FREE_TEXT stays SUBMITTED; second submit `QUIZ_ALREADY_SUBMITTED`; `USER` 403

**Checkpoint**: Assigned student can complete an all-auto or mixed quiz. FR-010–FR-014, FR-018. SC-002, SC-003, SC-006.

---

## Phase 5: User Story 3 - Teacher reviews quiz submissions (Priority: P2)

**Goal**: Paula lists attempts, reviews against the **snapshot**, sets 0–100% on every FREE_TEXT (optional annotate + ≤500-char note), finalizes combined overall + per-skill. Student profile shows quizzes not CEFR.

**Independent Test**: Score A’s writing; A sees final overall and per-skill; teacher opening A after a live edit still sees A’s snapshot ([quickstart.md](./quickstart.md) §4–5).

### Implementation for User Story 3

- [X] T023 [US3] Extend `QuizAdminService` + `QuizAdminController` with `GET /admin/quizzes/{id}/attempts`, `GET .../attempts/{attemptId}` (snapshot-backed), `PUT .../attempts/{attemptId}/review` (block finalize if any FREE_TEXT lacks percent) per [contracts/quiz-api.md](./contracts/quiz-api.md)
- [X] T024 [US3] Recompute overall + per-skill on finalize in `QuizGradingService` (equal average of auto 0/100 and teacher %; fully-correct only at 100%); persist `GRADED`
- [X] T025 [US3] Admin review UI `front-end/src/components/quiz/admin/QuizAttemptsPanel.tsx` (and review dialog): reuse homework review percent/annotate/note; always render snapshot questions
- [X] T026 [US3] Replace placement evaluation on `front-end/src/routes/panel_.alumnos.$studentId.tsx` with `GET /admin/students/{id}/quizzes` (add that endpoint on `QuizAdminController` or student admin) — quiz attempts only, no CEFR
- [X] T027 [US3] JUnit: finalize blocked until all FREE_TEXT percents; review payload is snapshot not live quiz after an edit

**Checkpoint**: Mixed quiz can reach GRADED with overall + skills. FR-015, SC-007.

---

## Phase 6: User Story 4 - Placement test (“prueba de nivel”) is fully retired (Priority: P1)

**Goal**: Zero user-visible placement/prueba de nivel product copy. Old URL redirects. Admin/student surfaces use Quiz terminology.

**Independent Test**: [quickstart.md](./quickstart.md) §1 — `/prueba-de-nivel` → `/quizzes`; nav and admin tab; grep es/en/ro for leftover product strings.

### Implementation for User Story 4

- [X] T028 [P] [US4] Change `front-end/src/routes/prueba-de-nivel.tsx` to redirect to `/quizzes` (login gate unchanged: `/quizzes` requires auth)
- [X] T029 [P] [US4] Delete `front-end/src/components/placement/`, `front-end/src/lib/placement.ts`, and `placement` keys from `front-end/src/i18n/locales/{es,en,ro}.ts`
- [X] T030 [US4] Update `front-end/src/routes/robots[.]txt.ts` Disallow `/quizzes` (and keep or redirect old path); ensure `SiteHeader` / footer / SEO do not say “Prueba de nivel”
- [X] T031 [US4] Grep `front-end/` and `CLAUDE.md` for `prueba de nivel`, `placement test`, `/placement`, `PlacementAuthoring`; fix leftover product copy (keep `placements` drag-drop field names)
- [X] T032 [US4] Update Current pages + homework/placement bullets in `CLAUDE.md` to describe Quizzes (`/quizzes`, admin Quizzes tab, assigned students, no CEFR)

**Checkpoint**: Site-wide copy review finds zero placement product names. FR-001–FR-003, SC-004, SC-005.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Unassign/delete behaviour, homework regression, quickstart.

- [X] T033 Unassign: delete `IN_PROGRESS` attempts; keep `SUBMITTED`/`GRADED` so result GET still works — `QuizAdminService` + test in `back-end/src/test/java/com/kuky/backend/quiz/`
- [X] T034 [P] Confirm homework take/submit/grade still passes existing tests after `QuestionScoring` extract (`back-end/src/test/java/com/kuky/backend/learning/`)
- [X] T035 [P] After `npm run dev`, TanStack regenerates `front-end/src/routeTree.gen.ts` — do not hand-edit; verify `/quizzes` and `/panel/quizzes/$quizId` exist
- [ ] T036 Run [quickstart.md](./quickstart.md) scenarios 1–7 in the browser (constitution: visual check, not only unit tests)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS** all user stories
- **US1 (Phase 3)**: After Foundational — MVP
- **US2 (Phase 4)**: After Foundational; needs US1 quizzes to assign in practice, but API take can be tested with SQL/seeded rows
- **US3 (Phase 5)**: After US2 submit path exists
- **US4 (Phase 6)**: After Foundational (backend placement already gone); can run in parallel with US1–US3 on frontend files except `AdminPanel.tsx` (US1 owns the tab swap)
- **Polish (Phase 7)**: After US1–US3 (US4 can finish in parallel with polish)

### User Story Dependencies

- **User Story 1 (P1)**: After Phase 2 — no other stories
- **User Story 2 (P1)**: After Phase 2; uses quizzes created in US1
- **User Story 3 (P2)**: After US2 (needs submitted attempts)
- **User Story 4 (P1)**: After Phase 2; parallel with US1 if `AdminPanel.tsx` tab is coordinated (US1 T013 vs US4 T029)

### Parallel Opportunities

- T001 / T002
- T004 / T005 / T007 (after T003 for repos that need schema in tests)
- T012 vs T010/T011
- T028 / T029
- T034 / T035

---

## Parallel Example: User Story 1

```text
T012 admin.ts client          (parallel with T010–T011 backend)
T013 QuizTab + AdminPanel     (after T012)
T014 editor pages             (after T012; reuse homework editors)
T015 QuizAdminServiceTest     (after T010)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup
2. Phase 2 Foundational (V21 + delete placement backend)
3. Phase 3 US1 — teacher can author and assign
4. **STOP**: demo admin Quizzes tab
5. Then US2 (students can take) before the feature is student-complete

### Incremental Delivery

1. Setup + Foundational → placement API/DB gone
2. US1 → teacher MVP
3. US2 → student take (first user-facing quiz)
4. US3 → writing/mixed finalize
5. US4 → copy/nav/redirect polish (do not ship without this)
6. Polish → unassign + quickstart

### Suggested MVP scope

**US1 + Foundational** is the teacher-only MVP. **US1 + US2** is the smallest student-complete slice. **US4 must ship with US2** so `/prueba-de-nivel` is not a dead placement page.

---

## Notes

- [P] = different files, no incomplete dependencies
- Do not edit `front-end/src/routeTree.gen.ts` by hand
- Do not put quizzes under `/api/v1/learning/**` or homework tables
- Drag-drop answer field `placements` is unrelated — do not rename
- Commit after each task or logical group
- Next command: `/speckit-implement`
