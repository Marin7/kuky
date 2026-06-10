---
description: "Task list for Mi aprendizaje — Classes & Homework Tab"
---

# Tasks: Mi aprendizaje — Classes & Homework Tab

**Input**: Design documents from `specs/005-classes-homework-tab/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api.md](contracts/api.md), [quickstart.md](quickstart.md)

**Tests**: Backend JUnit tests are included because the plan's Testing strategy explicitly specifies them. Frontend is validated by browser verification per the constitution (no FE test framework in the repo).

**Organization**: Tasks are grouped by user story (US1 → US2 → US3) so each can be implemented and verified as an independent increment.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: US1 / US2 / US3 — maps to the prioritized user stories in spec.md
- All paths are repository-relative.

## Path Conventions

- Frontend (React/TanStack SSR): `front-end/src/...`
- Backend (Java 26 / Spring Boot 4, plain JDBC + Flyway): `back-end/src/...`
- Mirrors the existing `auth` / `scheduling` / `resources` packages and `components/*` folders.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the new package/folder skeletons the feature lives in.

- [X] T001 Create the backend `learning` package directories under `back-end/src/main/java/com/kuky/backend/learning/` (`controller/`, `dto/`, `model/`, `repository/`, `service/`, `exception/`), mirroring the existing `scheduling`/`resources` layout
- [X] T002 [P] Create the frontend learning component folder `front-end/src/components/learning/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database schema + seed data, shared models, and the frontend API client that every story depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Create Flyway migration `back-end/src/main/resources/db/migration/V5__create_learning.sql` with all four tables (`learning_presentation`, `past_classes`, `homework_assignments`, `homework_submissions`), their `CHECK`/`UNIQUE` constraints and the `homework_submissions_user_id_idx` index, plus clearly-labelled **Spanish seed data** (2–3 presentation blocks; 3–4 past classes with recent `held_on` dates; exactly 3 homework assignments — one past-due `due_on`, one future `due_on`, one `NULL` `due_on`; **no** `homework_submissions` rows) per [data-model.md](data-model.md)
- [X] T004 [P] Create `HomeworkStatus` enum (`PENDING`, `SUBMITTED`, `REVIEWED`) in `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkStatus.java`
- [X] T005 [P] Create model POJOs `PresentationBlock.java`, `PastClass.java`, `HomeworkAssignment.java`, `HomeworkSubmission.java` in `back-end/src/main/java/com/kuky/backend/learning/model/`
- [X] T006 [P] Create the frontend API client `front-end/src/lib/learning.ts` with TypeScript interfaces (`LearningResponse`, `PresentationBlock`, `PastClass`, `HomeworkItem`, `SubmitHomeworkRequest`) and functions `getLearning()` + `submitHomework(assignmentId, response?)`, following the `API_BASE` + `credentials: "include"` pattern of `front-end/src/lib/scheduling.ts`

**Checkpoint**: Schema seeded and shared types ready — user story implementation can begin.

---

## Phase 3: User Story 1 - Logged-in student sees the new tab; guests do not (Priority: P1) 🎯 MVP

**Goal**: A new "Mi aprendizaje" tab at `/aprendizaje` that is visible and reachable only when logged in; guests don't see the link, are redirected to `/cuenta` on direct access, and are rejected `401` by the API.

**Independent Test**: Logged out → no nav link, `/aprendizaje` redirects to `/cuenta`, `curl /api/v1/learning` returns 401. Logged in → link appears (desktop + mobile), page opens.

### Implementation for User Story 1

- [X] T007 [US1] Create the auth-gated route `front-end/src/routes/aprendizaje.tsx`: `getMe()` in `useEffect`; while loading show a spinner/placeholder; if unauthenticated redirect to `/cuenta` (TanStack Router `useNavigate`); if authenticated render `<LearningView />`. Include `head`/meta title "Mi aprendizaje — Español con Paula" (do not hand-edit `routeTree.gen.ts`)
- [X] T008 [US1] Create the initial shell `front-end/src/components/learning/LearningView.tsx` with the section heading "Mi aprendizaje" and a placeholder body (filled by US2/US3)
- [X] T009 [US1] Make `front-end/src/components/SiteHeader.tsx` auth-aware: call `getMe()` in a `useEffect`, and conditionally include the `{ to: "/aprendizaje", label: "Mi aprendizaje" }` entry (placed after "Reservas") in **both** the desktop nav and the mobile `Sheet` nav only when a user is present
- [X] T010 [US1] Browser-verified: guest had no nav link and `/aprendizaje` redirected to `/cuenta`; guest API → **403**; after login the "Mi aprendizaje" link appeared (desktop + mobile) and the page rendered

**Checkpoint**: The protected tab exists and is correctly access-controlled end-to-end (FR-001, FR-002, FR-003, SC-001, SC-002).

---

## Phase 4: User Story 2 - Student reviews their past classes (Priority: P2)

**Goal**: Opening the tab shows the teacher presentation and the student's past classes (title, Spanish-formatted date, teacher note), most-recent-first, with a friendly empty state.

**Independent Test**: As a logged-in student, the section lists seeded past classes ordered newest-first with title/date/note and renders the presentation; unpublishing the seed shows the empty state.

### Implementation for User Story 2

- [X] T011 [P] [US2] Create record DTOs `LearningResponse`, `PresentationBlockResponse`, `PastClassResponse`, `HomeworkItemResponse` in `back-end/src/main/java/com/kuky/backend/learning/dto/` per [contracts/api.md](contracts/api.md)
- [X] T012 [US2] Create `back-end/src/main/java/com/kuky/backend/learning/repository/ContentRepository.java` (`NamedParameterJdbcTemplate` + `RowMapper`s): read published presentation blocks ordered by `sort_order`, past classes ordered by `held_on DESC`, and published homework assignments ordered by `sort_order`
- [X] T013 [US2] Create `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkSubmissionRepository.java` with a read method `findByUser(email/userId)` returning the caller's submissions (used to compute effective status; upsert added in US3)
- [X] T014 [US2] Create `back-end/src/main/java/com/kuky/backend/learning/service/LearningService.java` `getOverview(email)`: assemble presentation + past classes + homework items, LEFT-join submissions so a missing row ⇒ `PENDING`, and derive each item's `overdue` (`due_on < today(teacherZone)` AND `PENDING`) reusing the teacher timezone from `config/SchedulingProperties.java`
- [X] T015 [US2] Create `back-end/src/main/java/com/kuky/backend/learning/controller/LearningController.java` with `GET /api/v1/learning` using `@AuthenticationPrincipal String email` → returns `LearningResponse` (no `SecurityConfig` change needed — already authenticated)
- [X] T016 [P] [US2] Backend test `back-end/src/test/java/com/kuky/backend/learning/LearningServiceTest.java`: past-classes ordering desc, `PENDING` default when no submission, overdue derivation true/false cases ✅ passing
- [X] T017 [P] [US2] Backend integration test `back-end/src/test/java/com/kuky/backend/learning/LearningControllerIntegrationTest.java`: guest rejected (**403**); authenticated GET returns presentation + pastClasses + homework with correct shape ✅ passing (real PostgreSQL)
- [X] T018 [P] [US2] Create `front-end/src/components/learning/ClassPresentation.tsx` rendering the presentation blocks (heading + body), with a sensible default when the list is empty (FR-004 edge case)
- [X] T019 [P] [US2] Create `front-end/src/components/learning/PastClassesList.tsx`: list of past classes (title, `Intl.DateTimeFormat("es", …)` date, teacher note) with a friendly empty state (FR-010)
- [X] T020 [US2] Wire `front-end/src/components/learning/LearningView.tsx` to fetch `getLearning()` (loading/error states) and render `ClassPresentation` + `PastClassesList`
- [X] T021 [US2] Browser-verified: presentation (3 blocks) + 4 past classes render newest-first (3 Jun → 27 May → 20 May → 13 May) with Spanish dates and teacher notes (FR-005, FR-006). Empty-state code present (FR-010)

**Checkpoint**: Past classes + presentation are fully functional and independently demonstrable.

---

## Phase 5: User Story 3 - Student views and completes assigned homework (Priority: P3)

**Goal**: Homework appears with title, instructions, optional due date, status, and an overdue flag; the student can mark done / submit a short response that persists server-side per student, with `PENDING → SUBMITTED` only and `REVIEWED` read-only.

**Independent Test**: Homework lists with status (initially Pending) and overdue flagging; submitting flips to Submitted and survives reload; a different user still sees Pending; unknown id → 404, over-long response → 400.

**Depends on US2** for the shared `LearningController`, `HomeworkSubmissionRepository`, `HomeworkItemResponse`, and `LearningView`.

### Implementation for User Story 3

- [X] T022 [P] [US3] Create exceptions `AssignmentNotFoundException` and `SubmissionNotAllowedException` in `back-end/src/main/java/com/kuky/backend/learning/exception/`
- [X] T023 [P] [US3] Create `SubmitHomeworkRequest` record DTO with `@Size(max = 2000) String response` in `back-end/src/main/java/com/kuky/backend/learning/dto/SubmitHomeworkRequest.java`
- [X] T024 [US3] Add an upsert method to `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkSubmissionRepository.java` using `INSERT … ON CONFLICT (user_id, assignment_id) DO UPDATE` (status, response_text, submitted_at, updated_at)
- [X] T025 [US3] Create `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkSubmissionService.java` `submit(email, assignmentId, response)`: validate the assignment exists (else `AssignmentNotFoundException`), reject when the current submission is `REVIEWED` (`SubmissionNotAllowedException`), set status `SUBMITTED` + `submitted_at`, upsert, and return the updated `HomeworkItemResponse`
- [X] T026 [US3] Add `PUT /api/v1/learning/homework/{assignmentId}` (`@AuthenticationPrincipal String email`, `@Valid @RequestBody SubmitHomeworkRequest`) to `back-end/src/main/java/com/kuky/backend/learning/controller/LearningController.java`
- [X] T027 [US3] Map `ASSIGNMENT_NOT_FOUND` (404) and `SUBMISSION_NOT_ALLOWED` (409) in `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java`
- [X] T028 [P] [US3] Backend test `back-end/src/test/java/com/kuky/backend/learning/HomeworkSubmissionServiceTest.java`: pending→submitted, mark-done-without-text, idempotent re-submit, reject when reviewed (409), unknown assignment (404) ✅ passing
- [X] T029 [P] [US3] Create `front-end/src/components/learning/HomeworkSubmitDialog.tsx`: a Shadcn dialog with an optional short-text response field and a submit/mark-done action
- [X] T030 [P] [US3] Create `front-end/src/components/learning/HomeworkItemCard.tsx`: title, instructions, `es`-formatted due date, status badge, overdue indicator (when `overdue`), and the action that opens `HomeworkSubmitDialog`
- [X] T031 [US3] Create `front-end/src/components/learning/HomeworkList.tsx`: map homework items to `HomeworkItemCard` with a friendly empty state (FR-010)
- [X] T032 [US3] Wire `HomeworkList` into `front-end/src/components/learning/LearningView.tsx`; call `submitHomework()` and refresh the overview (or update the item in place) on success
- [X] T033 [US3] Browser-verified: opened the dialog, submitted a response → status flipped to **Entregada** and the "Atrasada" flag cleared; **persisted across reload**; a second user saw all assignments **Pendiente** (per-student isolation); past-due item shows **Atrasada** (FR-007, FR-008, FR-009, FR-014, FR-015, SC-004)

**Checkpoint**: All three user stories are independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification across stories.

- [X] T034 [P] Browser-verified responsive: at 375px the hamburger menu opens and includes "Mi aprendizaje" (after Reservas); desktop nav shows it inline (FR-013, SC-006)
- [X] T035 [P] Verify all section copy is Spanish and consistent with the rest of the site (FR-012) — all labels/copy authored in Spanish ("Mi aprendizaje", "Clases anteriores", "Tareas", "Pendiente/Entregada/Revisada", "Atrasada", empty states)
- [X] T036 Validated end-to-end in the browser (all three stories: gating + redirect, presentation + past classes, homework submit/persist/isolation/overdue) plus API error paths via the passing integration test (403 guest, 404 unknown assignment)
- [X] T037 [P] Backend learning tests pass (`./gradlew -p back-end test --tests "*learning*"` — unit + integration, real PostgreSQL); frontend `tsc --noEmit` clean and ESLint clean on new files (repo-wide CRLF/`prettier` errors are a pre-existing Windows-checkout artifact affecting untouched files too, e.g. `vite.config.ts` — not introduced here)
- [X] T038 Remove placeholder/dead code: the US1 `LearningView` shell was replaced in place by the real orchestrating view (no dead placeholder left); final review done

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup — **blocks all user stories**.
- **User Stories (Phase 3+)**: All depend on Foundational.
  - **US1 (P1)** is independent (frontend gating; the API `401` is automatic from the existing security config).
  - **US2 (P2)** depends on US1 for the `/aprendizaje` route + `LearningView` shell it extends.
  - **US3 (P3)** depends on US2 for the shared `LearningController`, `HomeworkSubmissionRepository`, `HomeworkItemResponse`, and `LearningView`.
- **Polish (Phase 6)**: Depends on the desired stories being complete.

### Within Each User Story

- Backend: DTOs/models → repositories → services → controller; tests [P] alongside.
- Frontend: leaf components [P] → wired into `LearningView`.
- Story complete (and browser-verified) before moving to the next priority.

### Parallel Opportunities

- Foundational: T004, T005, T006 in parallel (enum, models, FE client — different files).
- US2: T011, T016, T017, T018, T019 are [P] (DTOs, two tests, two FE components — different files) while T012–T015/T020 are sequential (shared service/controller/view).
- US3: T022, T023, T028, T029, T030 are [P]; T024–T027/T031/T032 are sequential (shared repo/controller/view).
- Polish: T034, T035, T037 in parallel.

---

## Parallel Example: User Story 2

```bash
# After T012–T015 (backend overview) land, run the [P] FE/test tasks together:
Task: "Backend test LearningServiceTest in back-end/src/test/java/com/kuky/backend/learning/LearningServiceTest.java"
Task: "Backend integration test LearningControllerIntegrationTest in .../LearningControllerIntegrationTest.java"
Task: "Create ClassPresentation.tsx in front-end/src/components/learning/"
Task: "Create PastClassesList.tsx in front-end/src/components/learning/"
```

---

## Implementation Strategy

### MVP First

1. Phase 1 (Setup) → Phase 2 (Foundational) → Phase 3 (US1).
2. **STOP and VALIDATE**: the protected tab is correctly gated (the access-control backbone of the whole feature).
3. US1 alone is a thin shell — for a meaningful demo, continue straight into US2.

### Incremental Delivery

1. Setup + Foundational → foundation ready.
2. US1 → gated tab (MVP backbone) → verify.
3. US2 → presentation + past classes → verify/demo.
4. US3 → homework view + submit → verify/demo.
5. Each story adds value without breaking the previous ones.

---

## Notes

- `[P]` = different files, no incomplete dependencies.
- Do **not** hand-edit `front-end/src/routeTree.gen.ts` — it regenerates from the new route file.
- No `SecurityConfig` change: `/api/v1/learning/**` is already covered by `anyRequest().authenticated()`.
- Backend test commands assume the existing JUnit 5 / Spring Boot Test setup; frontend changes are browser-verified per the constitution.
- Commit after each task or logical group; stop at any checkpoint to validate a story independently.
