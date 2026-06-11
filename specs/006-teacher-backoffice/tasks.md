---
description: "Task list for Teacher Backoffice — Control Panel"
---

# Tasks: Teacher Backoffice — Control Panel

**Input**: Design documents from `specs/006-teacher-backoffice/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api.md](contracts/api.md), [quickstart.md](quickstart.md)

**Tests**: Backend unit/slice tests are included (the plan defines a JUnit test strategy and the repo already uses Spring Boot Test). Frontend is verified visually in a running browser per the constitution — no frontend test tasks.

**Organization**: Tasks are grouped by user story. Stories are independently implementable and testable. The admin-authorization tier + panel shell is the shared blocking foundation (Phase 2).

> **Incorporates `/speckit-analyze` remediations**: I1 (`HomeworkTargetRepository` lives in `learning/repository/`), U1 (explicit `BookingRepository` conflict query in T025), C1 (image-fetch authorization decision recorded in T056), plus C2/C3/D1 cleanups.

## Path Conventions

- Backend Java root: `back-end/src/main/java/com/kuky/backend/`
- Backend tests root: `back-end/src/test/java/com/kuky/backend/`
- Backend migrations: `back-end/src/main/resources/db/migration/`
- Frontend root: `front-end/src/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the new package/folder skeletons and the API-client base used by all stories.

- [X] T001 [P] Create backend package directories `admin/{controller,dto,service,exception}` and `presentations/{controller,model,repository,service,exception}` under `back-end/src/main/java/com/kuky/backend/`
- [X] T002 [P] Create frontend component folders `front-end/src/components/admin/{availability,homework,presentations}/` (empty placeholders ok)
- [X] T003 [P] Create `front-end/src/lib/admin.ts` with the shared `apiCall` helper + `API_BASE = "http://localhost:8081/api/v1/admin"` (mirror `lib/auth.ts`, `credentials: "include"`, `{error,message}` throw)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Persisted admin role + authorization tier + panel shell. Without this, no story can render or be reached.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Backend — role & authorization

- [X] T004 Create migration `back-end/src/main/resources/db/migration/V6__add_user_roles.sql` adding `role VARCHAR(20) NOT NULL DEFAULT 'STUDENT'` with `CHECK (role IN ('STUDENT','ADMIN'))` to `users`
- [X] T005 Add `role` field (default `"STUDENT"`) + getter/setter to `auth/model/User.java`, and map/save it in `auth/repository/UserRepository.java` (`USER_MAPPER` + both INSERT/UPDATE in `save`)
- [X] T006 Add `findStudents()` to `auth/repository/UserRepository.java` returning all users where `role = 'STUDENT'` (ordered by email)
- [X] T007 Add `role` to `auth/dto/UserResponse.java` (record component) and populate it everywhere `UserResponse` is built in `auth/service/AuthService.java` (login, /me lookup, reset) and `AuthController.me`
- [X] T008 Update `config/JwtConfig.java` to write a `role` claim in `generateToken(...)` and expose `extractRole(token)` (default `"STUDENT"` when claim absent); update the `generateToken` signature/callers in `AuthController` to pass the user role
- [X] T009 Update `config/JwtCookieAuthenticationFilter.java` to read the `role` claim and set the authentication authority to `ROLE_<role>` (e.g. `ROLE_ADMIN`) instead of `Collections.emptyList()`
- [X] T010 Update `config/SecurityConfig.java` to add `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` before `anyRequest().authenticated()`
- [X] T011 Create `config/AdminBootstrap.java` (`CommandLineRunner`) that idempotently promotes the user whose email equals `app.scheduling.teacher-email` to `role='ADMIN'` (no-op if user absent), logging the action
- [X] T012 ACCESS_DENIED (403) handled at the security layer in `SecurityConfig` via a JSON `AccessDeniedHandler` (plus a 401 `AuthenticationEntryPoint`) — correct for URL-based authz, which never reaches `GlobalExceptionHandler`
- [X] T013 [P] Create `StudentResponse` record in `admin/dto/StudentResponse.java` (`UUID id, String email`)
- [X] T014 Create `admin/controller/StudentAdminController.java` exposing `GET /api/v1/admin/students` → `findStudents()` mapped to `StudentResponse` (shared by US2 + US3 pickers)

### Backend — foundational test

- [X] T015 [P] Authz check (401 guest / 403 student / 200 admin) — verified against the live stack in Phase 6 (quickstart Scenario D) rather than a DB-booting `@SpringBootTest`, to keep `./gradlew test` hermetic

### Frontend — panel shell & role-aware nav

- [X] T016 [P] Update `front-end/src/lib/auth.ts`: add `role: "STUDENT" | "ADMIN"` to `UserResponse` (and `AuthResponse` if it carries role)
- [X] T017 Update `front-end/src/components/SiteHeader.tsx` to fetch the role via `getMe()` and conditionally render a `Panel` nav entry (`{ to: "/panel", label: "Panel" }`) only when `role === "ADMIN"` (desktop + mobile)
- [X] T018 Create `front-end/src/routes/panel.tsx`: auth-gated route that `getMe()`s, redirects to `/cuenta` if unauthenticated and to `/` if `role !== "ADMIN"`, otherwise renders `<AdminPanel />`; include `head` meta (Spanish title)
- [X] T019 Create `front-end/src/components/admin/AdminPanel.tsx` with a Shadcn `Tabs` shell: `Disponibilidad` / `Tareas` / `Presentaciones` (tab bodies are placeholders filled per story)

**Checkpoint**: An admin can open `/panel` (3 empty tabs); students/guests are blocked from the page and the API. Authorization test passes.

---

## Phase 3: User Story 1 — Manage availability (Priority: P1) 🎯 MVP

**Goal**: Paula edits a weekly availability pattern + date exceptions in the panel; the public `/reservas` schedule derives from it; confirmed bookings are preserved with a warning.

**Independent Test**: As admin, set a weekly window + a BLOCK exception, save; load `/reservas` and confirm slots match; remove a window over a confirmed booking and confirm the booking survives with a warning.

### Backend — schema & model

- [X] T020 [US1] Create migration `back-end/src/main/resources/db/migration/V7__create_availability.sql`: `availability_rules` (day_of_week 1–7, start_time/end_time TIME, `end_time > start_time`) and `availability_exceptions` (exception_date DATE, kind BLOCK/OPEN, start/end TIME) with indexes, plus behavior-preserving seed (Mon–Fri 09:00–12:00 and 14:00–18:00)
- [X] T021 [P] [US1] Create `scheduling/model/AvailabilityRule.java` POJO (id, dayOfWeek, startTime `LocalTime`, endTime)
- [X] T022 [P] [US1] Create `scheduling/model/AvailabilityException.java` POJO (id, date `LocalDate`, kind, startTime, endTime)
- [X] T023 [US1] Create `scheduling/repository/AvailabilityRepository.java` (`NamedParameterJdbcTemplate` + RowMappers): `findAllRules()`, `replaceWeekly(List<AvailabilityRule>)` (delete-all + insert in one tx), `findUpcomingExceptions()`, `insertException(...)`, `deleteException(UUID)`

### Backend — availability derivation (core)

- [X] T024 [US1] Refactor `scheduling/service/AvailabilityService.java` so `generateSchedule()` and `validateBookable(...)` derive available windows per day from `AvailabilityRepository` (weekly rules ∪ OPEN exceptions − BLOCK exceptions), retaining the existing horizon, lead-time, no-past, and confirmed-booking `BOOKED` overlay; remove the hard-coded day-start/day-end/lunch/weekend constants (no commented-out code)
- [X] T025 [US1] Add `findUpcomingConfirmedBookings()` (returning booking id, student email, slot start) to `scheduling/repository/BookingRepository.java`, then add `findConfirmedBookingsOutsideAvailability(...)` to `scheduling/service/AvailabilityService.java` that filters those whose start no longer falls inside saved availability (for the save-warning / FR-010)
- [X] T026 [P] [US1] Unit tests `back-end/src/test/java/com/kuky/backend/scheduling/AvailabilityServiceTest.java`: weekday windows, split windows (lunch gap), weekend = no rules, BLOCK removes time, OPEN adds time on a non-rule day, lead/past/booked overlay, and booking-conflict detection

### Backend — admin endpoints

- [X] T027 [P] [US1] Create availability admin DTOs in `admin/dto/`: `WeeklyWindowDto`, `AvailabilityResponse`, `UpdateWeeklyRequest`, `ExceptionRequest`, `ExceptionResponse`, `BookingConflictDto` (per [contracts/api.md](contracts/api.md))
- [X] T028 [US1] Create `admin/controller/AvailabilityAdminController.java`: `GET /api/v1/admin/availability`, `PUT /api/v1/admin/availability/weekly` (returns `bookingConflicts`), `POST /api/v1/admin/availability/exceptions`, `DELETE /api/v1/admin/availability/exceptions/{id}`; `@Valid` request bodies; past-date/`end<=start` → `VALIDATION_ERROR`

### Frontend — availability UI

- [X] T029 [US1] Add availability methods to `front-end/src/lib/admin.ts`: `getAvailability()`, `updateWeekly(windows)`, `addException(...)`, `deleteException(id)` with typed responses
- [X] T030 [P] [US1] Create `front-end/src/components/admin/availability/WeeklyAvailabilityEditor.tsx`: per-weekday window rows (add/remove), `"es"` weekday labels, an empty state when no windows are set (FR-029), save (full-replace) calling `updateWeekly`
- [X] T031 [P] [US1] Create `front-end/src/components/admin/availability/AvailabilityExceptionList.tsx`: list upcoming exceptions + add form (date, BLOCK/OPEN, time window) + delete; empty state
- [X] T032 [P] [US1] Create `front-end/src/components/admin/availability/BookingConflictNotice.tsx`: non-blocking warning rendering `bookingConflicts` returned by save
- [X] T033 [US1] Wire the three availability components into the `Disponibilidad` tab of `front-end/src/components/admin/AdminPanel.tsx`, surfacing `BookingConflictNotice` after a weekly save

**Checkpoint**: Editing availability in the panel changes public `/reservas`; confirmed bookings survive edits and trigger a warning. US1 is independently demoable (MVP).

---

## Phase 4: User Story 2 — Author & assign homework (Priority: P2)

**Goal**: Paula creates homework and assigns it to specific students; assigned students see/submit it in `/aprendizaje`; she reviews submissions, edits, and deletes.

**Independent Test**: Create a homework item, assign to one student; that student sees it, an unassigned one does not; the panel shows the submission.

### Backend — schema & visibility migration

- [X] T034 [US2] Create migration `back-end/src/main/resources/db/migration/V8__create_homework_targets.sql`: `homework_targets` (assignment_id, user_id, `UNIQUE`, FKs `ON DELETE CASCADE`) + indexes
- [X] T035 [US2] Add `STUDENT_NOT_FOUND` (404) mapping to `config/GlobalExceptionHandler.java` and create `admin/exception/StudentNotFoundException.java`
- [X] T036 [US2] Update `learning/repository/ContentRepository.java`: replace `findPublishedAssignments()` usage with `findAssignmentsForUser(UUID userId)` (join `homework_targets`); update `learning/service/LearningService.java` to use it so visibility is per-student (retires the shared-to-all model)

### Backend — admin homework service & endpoints

- [X] T037 [US2] Create `learning/repository/HomeworkTargetRepository.java` (co-located with the existing homework repositories): `replaceTargets(assignmentId, List<UUID> userIds)`, `findAssigneesWithSubmissions(assignmentId)` (join `homework_targets → users` LEFT JOIN `homework_submissions`)
- [X] T038 [P] [US2] Create homework admin DTOs in `admin/dto/`: `HomeworkAdminItem`, `AssigneeDto`, `CreateHomeworkRequest`, `UpdateHomeworkRequest`, `SetAssigneesRequest` (per contract)
- [X] T039 [US2] Create `admin/service/HomeworkAdminService.java`: list all assignments with assignees+submission status, create (optional assignees), edit title/instructions/dueOn, replace assignees (validate each is a known student → `StudentNotFoundException`), delete (cascades)
- [X] T040 [US2] Create `admin/controller/HomeworkAdminController.java`: `GET/POST /api/v1/admin/homework`, `PUT/DELETE /api/v1/admin/homework/{id}`, `PUT /api/v1/admin/homework/{id}/assignees`; `@Valid` bodies; `ASSIGNMENT_NOT_FOUND`/`STUDENT_NOT_FOUND` on bad ids
- [X] T041 [P] [US2] Unit tests `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java`: create+assign, edit reflected, assignee replacement, delete cascade, submission read; plus a `LearningService` test that an unassigned student does not see an assignment

### Frontend — homework UI

- [X] T042 [US2] Add homework methods to `front-end/src/lib/admin.ts`: `getStudents()`, `getHomework()`, `createHomework(...)`, `updateHomework(id,...)`, `setAssignees(id, ids)`, `deleteHomework(id)`
- [X] T043 [P] [US2] Create `front-end/src/components/admin/homework/StudentMultiSelect.tsx`: fetches `getStudents()`, multi-select of students (reused by presentations sharing)
- [X] T044 [P] [US2] Create `front-end/src/components/admin/homework/HomeworkEditorDialog.tsx`: create/edit dialog (title, instructions, optional due date, assignees via `StudentMultiSelect`)
- [X] T045 [P] [US2] Create `front-end/src/components/admin/homework/HomeworkAdminList.tsx`: list items with assignees + per-assignee status/response, edit + delete (confirm) actions, empty state
- [X] T046 [US2] Wire homework components into the `Tareas` tab of `front-end/src/components/admin/AdminPanel.tsx`

**Checkpoint**: Paula authors/assigns/reviews/edits/deletes homework; student `/aprendizaje` reflects per-student targeting. US2 works independently of US1/US3.

---

## Phase 5: User Story 3 — Build & share presentations (Priority: P3)

**Goal**: Paula builds simple slide decks (heading/text/optional image), reorders, previews full-screen, and shares with specific students who view them in `/aprendizaje`.

**Independent Test**: Build a ≥5-slide deck with an image, reorder, preview, share with one student; that student opens it, an unshared one cannot.

### Backend — schema, images, models

- [X] T047 [US3] Create migration `back-end/src/main/resources/db/migration/V9__create_presentations.sql`: `images` (content_type CHECK, byte_size ≤ 2 MB CHECK, data BYTEA), `presentations`, `presentation_slides` (image_id FK `ON DELETE SET NULL`, sort_order, indexes), `presentation_shares` (UNIQUE, FKs `ON DELETE CASCADE`)
- [X] T048 [US3] Add `PRESENTATION_NOT_FOUND` (404) and `INVALID_IMAGE` (422) mappings to `config/GlobalExceptionHandler.java`; create `presentations/exception/PresentationNotFoundException.java`
- [X] T049 [P] [US3] Create `presentations/model/` POJOs: `Image`, `Presentation`, `Slide`, `PresentationShare`
- [X] T050 [P] [US3] Create `presentations/repository/ImageRepository.java`: `insert(contentType, bytes)` → id, `findById(id)` (bytes + content type)
- [X] T051 [US3] Create `presentations/repository/PresentationRepository.java`: deck CRUD, slide CRUD, reorder (`updateSortOrders`), shares replace, `findDeckForUser(id, userId)` enforcing the share gate, list with slideCount/sharedWith counts

### Backend — services

- [X] T052 [US3] Create `presentations/service/ImageService.java`: validate content type ∈ {jpeg,png,webp} and size ≤ 2 MB (`INVALID_IMAGE`), store, fetch
- [X] T053 [US3] Create `presentations/service/PresentationService.java`: create/rename/delete deck, add/edit/delete slide (length + slide-count caps → `VALIDATION_ERROR`), reorder (validate permutation), replace shares (validate students), open-as-student (share gate → `PRESENTATION_NOT_FOUND`)
- [X] T054 [P] [US3] Unit tests `back-end/src/test/java/com/kuky/backend/presentations/PresentationServiceTest.java` (slide CRUD, reorder permutation validation, share gate) and `ImageServiceTest.java` (type/size validation)

### Backend — endpoints

- [X] T055 [P] [US3] Create presentation/image admin DTOs in `admin/dto/`: `PresentationSummary`, `PresentationDetail`, `SlideDto`, `CreatePresentationRequest`, `SlideRequest`, `ReorderRequest`, `SetSharesRequest`, `ImageUploadResponse`
- [X] T056 [US3] Create `presentations/controller/ImageController.java` (`GET /api/v1/images/{id}`, **any authenticated user** — accepted per research §3/analysis C1: image bytes are not share-scoped; ids are unguessable UUIDs reachable only via the share-gated deck endpoint) and `admin/controller/ImageAdminController.java` (`POST /api/v1/admin/images`, multipart)
- [X] T057 [US3] Create `admin/controller/PresentationAdminController.java`: list, create, get detail, rename, delete, slide add/edit/delete, `PUT .../slides/order`, `PUT .../shares` (per contract)
- [X] T058 [US3] Update `learning/dto/LearningResponse.java` + `learning/service/LearningService.java` to include `sharedPresentations` (id, title, slideCount) for the caller, and add `GET /api/v1/learning/presentations/{id}` to `learning/controller/LearningController.java` returning a deck only when shared (else `PRESENTATION_NOT_FOUND`)

### Frontend — presentation builder (admin)

- [X] T059 [US3] Add presentation + image methods to `front-end/src/lib/admin.ts`: `listPresentations()`, `createPresentation(title)`, `getPresentation(id)`, `renamePresentation`, `deletePresentation`, slide add/edit/delete, `reorderSlides`, `setShares`, `uploadImage(file)`
- [X] T060 [P] [US3] Create `front-end/src/components/admin/presentations/SlidePreview.tsx`: full-screen pager (keyboard + prev/next), renders heading/body/image via `GET /api/v1/images/{id}` (reused by the student viewer)
- [X] T061 [P] [US3] Create `front-end/src/components/admin/presentations/SlideEditorCard.tsx`: edit one slide (heading, body, image upload with type/size error handling, remove image)
- [X] T062 [P] [US3] Create `front-end/src/components/admin/presentations/SharePresentationDialog.tsx`: share a deck with students via `StudentMultiSelect`
- [X] T063 [US3] Create `front-end/src/components/admin/presentations/PresentationEditor.tsx`: title edit, slide list with add/reorder (up/down), per-slide `SlideEditorCard`, preview button (`SlidePreview`), share (`SharePresentationDialog`)
- [X] T064 [P] [US3] Create `front-end/src/components/admin/presentations/PresentationAdminList.tsx`: deck list with create/open/delete + empty state
- [X] T065 [US3] Wire presentation components into the `Presentaciones` tab of `front-end/src/components/admin/AdminPanel.tsx`

### Frontend — student viewing

- [X] T066 [US3] Add `getPresentation(id)` (and `sharedPresentations` typing on the overview) to `front-end/src/lib/learning.ts`
- [X] T067 [P] [US3] Create `front-end/src/components/learning/SharedPresentationsList.tsx` (decks shared with the student) and `front-end/src/components/learning/PresentationViewer.tsx` (opens a deck, reuses `SlidePreview`)
- [X] T068 [US3] Render `SharedPresentationsList` in `front-end/src/components/learning/LearningView.tsx`

**Checkpoint**: Paula builds/previews/shares decks; shared students view them, unshared cannot; images upload and serve. US3 works independently.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T069 [P] Update root `CLAUDE.md`: mark `/panel` as a live page, note the admin `role`, and document the admin bootstrap via `app.scheduling.teacher-email`; note that availability is now data-driven (lunch/weekends no longer special-cased)
- [X] T070 Run the full backend suite `cd back-end && ./gradlew test` and `cd front-end && npm run lint`; fix any failures
- [X] T071 Execute [quickstart.md](quickstart.md) scenarios A–D in a running browser (availability, homework, presentations, access control); confirm SC-001…SC-008 and that all panel/student copy is Spanish with `"es"`-locale dates (FR-027)
- [X] T072 [P] Remove now-dead configuration/columns: drop unused `day-start`/`day-end`/`lunch-break-*` reads if fully superseded, and retire or repurpose the now-meaningless `published`/`sort_order` columns on `homework_assignments`; verify no commented-out code remains (constitution: dead code)

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Setup; **blocks all user stories** (no panel/auth without it).
- **User Stories (Phase 3–5)**: each depends only on Foundational. After Phase 2 they can proceed in parallel or in priority order P1 → P2 → P3.
- **Polish (Phase 6)**: depends on the targeted stories being complete.

### User story independence

- **US1 (Availability)**: backend availability tables/service + admin endpoints + availability tab. No dependency on US2/US3.
- **US2 (Homework)**: homework targeting + admin endpoints + tareas tab; reuses `GET /admin/students` (foundational). No dependency on US1/US3.
- **US3 (Presentations)**: presentation/image domain + admin endpoints + presentaciones tab + student viewer; reuses `StudentMultiSelect` (built in US2) and `GET /admin/students` (foundational). If US3 is built before US2, create `StudentMultiSelect` within US3 instead.

### Within each story

- Migration → models → repository → service (+ unit tests) → controller → frontend client → components → tab wiring.

### Parallel opportunities

- Setup: T001, T002, T003 all [P].
- Foundational: T013/T015/T016 [P]; T004→T005→T006/T007/T008→T009→T010 are sequential (same auth chain).
- US1: T021/T022 [P]; T026/T027 [P]; T030/T031/T032 [P].
- US2: T038/T041 [P]; T043/T044/T045 [P].
- US3: T049/T050 [P]; T054/T055 [P]; T060/T061/T062/T064 [P]; T067 [P].

---

## Parallel Example: User Story 1

```text
# Models together:
Task T021: AvailabilityRule model
Task T022: AvailabilityException model

# Tests + DTOs together (after service refactor):
Task T026: AvailabilityServiceTest
Task T027: availability admin DTOs

# Availability UI components together:
Task T030: WeeklyAvailabilityEditor
Task T031: AvailabilityExceptionList
Task T032: BookingConflictNotice
```

---

## Implementation Strategy

### MVP first (User Story 1 only)

1. Phase 1 Setup → 2. Phase 2 Foundational (admin auth + panel shell) → 3. Phase 3 US1 → **STOP & VALIDATE** availability end-to-end on `/reservas` → demo. This alone removes Paula's dependency on a developer for schedule changes (highest-value slice).

### Incremental delivery

- Foundation ready → US1 (availability, MVP) → US2 (homework) → US3 (presentations). Each ships independently without breaking the previous.

### Parallel team strategy

- After Phase 2: Dev A → US1, Dev B → US2, Dev C → US3. Build `StudentMultiSelect` in whichever of US2/US3 lands first and reuse in the other.

---

## Notes

- `[P]` = different files, no incomplete-task dependency.
- `[USx]` maps each task to its story for traceability.
- The auth chain T004→T010 touches shared files (User, JwtConfig, filter, SecurityConfig) — keep sequential.
- Commit after each task or logical group; verify backend tests pass and UI in a browser per the constitution.
- Migrations are append-only and ordered V6→V9; never edit an applied migration.
