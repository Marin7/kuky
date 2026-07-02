---

description: "Task list template for feature implementation"
---

# Tasks: Student Testimonials on Homepage

**Input**: Design documents from `specs/014-homepage-testimonials/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/testimonials-api.md](contracts/testimonials-api.md), [quickstart.md](quickstart.md)

**Tests**: Included — this repo has an established backend integration/unit test convention (e.g. `PresentationAdminControllerTest`, `HomeworkSubmissionServiceTest`); new backend behavior gets matching test coverage. Frontend has no test framework (per constitution) — frontend verification is manual/browser-based via [quickstart.md](quickstart.md).

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: The schema every other task depends on.

- [X] T001 Write `back-end/src/main/resources/db/migration/V25__create_testimonials.sql` per [data-model.md](data-model.md): `CREATE TABLE testimonials` (`id`, `user_id UUID UNIQUE REFERENCES users(id)`, `student_name`, `text`, `status` with CHECK `('PENDING','APPROVED','REJECTED','UNPUBLISHED')` default `'PENDING'`, `display_order` default `0`, `submitted_at` default `now()`, `reviewed_at` nullable) plus `idx_testimonials_status_order` on `(status, display_order)`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The shared model/enum/repository scaffold every user story builds on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Create `back-end/src/main/java/com/kuky/backend/testimonials/model/TestimonialStatus.java` — `public enum TestimonialStatus { PENDING, APPROVED, REJECTED, UNPUBLISHED }`.
- [X] T003 [P] Create `back-end/src/main/java/com/kuky/backend/testimonials/model/Testimonial.java` — a record/class with fields `id (UUID)`, `userId (UUID)`, `studentName (String)`, `text (String)`, `status (TestimonialStatus)`, `displayOrder (int)`, `submittedAt (Instant)`, `reviewedAt (Instant, nullable)`, mirroring the shape of `back-end/src/main/java/com/kuky/backend/presentations/model/Presentation.java`.
- [X] T004 Create `back-end/src/main/java/com/kuky/backend/testimonials/repository/TestimonialRepository.java` with the `NamedParameterJdbcTemplate` constructor wiring and a private row-mapper for `Testimonial`, following the shape of `back-end/src/main/java/com/kuky/backend/presentations/repository/PresentationRepository.java`. No query methods yet — each user story adds the methods it needs. Depends on T002, T003.
- [X] T005 Create `back-end/src/main/java/com/kuky/backend/testimonials/exception/TestimonialNotFoundException.java` — `public class TestimonialNotFoundException extends RuntimeException { public TestimonialNotFoundException(String message) { super(message); } }`, mirroring `PresentationNotFoundException`.
- [X] T006 In `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java`, add an `@ExceptionHandler(TestimonialNotFoundException.class)` mapping to `404` with `{"error":"TESTIMONIAL_NOT_FOUND", ...}`, following the exact shape of the existing `handlePresentationNotFound` handler. Depends on T005.

**Checkpoint**: Model, enum, repository scaffold, and error-mapping exist. User stories can now begin.

---

## Phase 3: User Story 1 - Prospective Student Reads Testimonials (Priority: P1) 🎯 MVP

**Goal**: The public homepage shows a dedicated section listing approved testimonials (text + student's full name), and hides the section entirely when none are approved.

**Independent Test**: Insert an `APPROVED` row directly (test fixture / SQL), load `/` as a logged-out visitor, confirm the section renders it; delete/empty the table and confirm the section is entirely absent.

### Tests for User Story 1

- [X] T007 [P] [US1] Write `back-end/src/test/java/com/kuky/backend/testimonials/TestimonialControllerTest.java` covering: `GET /api/v1/testimonials` is public (no auth needed), returns only `APPROVED` rows ordered by `displayOrder`, and returns `[]` (not an error) when none are approved.

### Implementation for User Story 1

- [X] T008 [P] [US1] Add `findApproved()` to `TestimonialRepository.java` (from T004): `SELECT * FROM testimonials WHERE status = 'APPROVED' ORDER BY display_order`.
- [X] T009 [P] [US1] Create `back-end/src/main/java/com/kuky/backend/testimonials/dto/TestimonialResponse.java` — `public record TestimonialResponse(String text, String studentName, int displayOrder) {}`.
- [X] T010 [US1] Create `back-end/src/main/java/com/kuky/backend/testimonials/service/TestimonialService.java` with `listApproved()` mapping `repository.findApproved()` to `List<TestimonialResponse>`. Depends on T008, T009.
- [X] T011 [US1] Create `back-end/src/main/java/com/kuky/backend/testimonials/controller/TestimonialController.java` with `GET /api/v1/testimonials` → `service.listApproved()`, per [contracts/testimonials-api.md](contracts/testimonials-api.md). Depends on T010.
- [X] T012 [P] [US1] In `back-end/src/main/java/com/kuky/backend/config/SecurityConfig.java`, add `.requestMatchers(HttpMethod.GET, "/api/v1/testimonials").permitAll()` before the `anyRequest().authenticated()` fallthrough.
- [X] T013 [P] [US1] Create `front-end/src/lib/testimonials.ts` with the `apiCall<T>` fetch wrapper (mirroring `front-end/src/lib/scheduling.ts`'s shape: `credentials: "include"`, `{error,message}` error type) and `getTestimonials()` (`GET /testimonials`) plus a `Testimonial { text, studentName, displayOrder }` interface.
- [X] T014 [US1] Create `front-end/src/components/TestimonialsSection.tsx` — fetches via `getTestimonials()`, renders a section listing each testimonial's text + student name; returns `null` (renders nothing) when the list is empty. Depends on T013.
- [X] T015 [US1] In `front-end/src/routes/index.tsx`, render `<TestimonialsSection />` between the Features and CTA sections. Depends on T014.

**Checkpoint**: At this point, User Story 1 is fully functional and testable independently (via directly-seeded data) — the homepage shows/hides approved testimonials correctly.

---

## Phase 4: User Story 2 - Student Submits Their Own Testimonial (Priority: P2)

**Goal**: A logged-in student can submit a testimonial (entering `PENDING`); resubmitting replaces their existing one; they can see their own status; the teacher is emailed on submission.

**Independent Test**: As a `STUDENT` account, submit testimonial text via `/aprendizaje`; confirm it does not appear on the public homepage (still `PENDING`); confirm Mailpit shows a notification email to the teacher; resubmit with different text and confirm only one testimonial exists for that student.

### Tests for User Story 2

- [X] T016 [P] [US2] Extend `back-end/src/test/java/com/kuky/backend/testimonials/TestimonialControllerTest.java` (from T007) with: `POST /api/v1/testimonials` returns `401` anonymous, `403` for a plain `USER`-role caller, `200` for `STUDENT`/`ADMIN`; `400 VALIDATION_ERROR` for empty/too-short text; `GET /api/v1/testimonials/me` returns `204` when the caller has never submitted, and the submitted testimonial's `status`/`text` after submitting.
- [X] T017 [P] [US2] Write `back-end/src/test/java/com/kuky/backend/testimonials/TestimonialServiceTest.java` covering: submitting creates a `PENDING` row with the student's name snapshotted; resubmitting the same user replaces the row's text and resets `status` to `PENDING` (never creates a second row, per FR-006).
- [X] T018 [P] [US2] Write `back-end/src/test/java/com/kuky/backend/testimonials/TestimonialEmailServiceTest.java` covering `sendSubmittedNotificationToTeacher(teacherEmail, studentName)`: asserts a `SimpleMailMessage` is sent via the mocked `JavaMailSender` with the given recipient, mirroring how `BookingEmailService`'s methods would be tested.

### Implementation for User Story 2

- [X] T019 [P] [US2] Add `upsertByUser(UUID userId, String studentName, String text)` (`INSERT INTO testimonials (...) VALUES (...) ON CONFLICT (user_id) DO UPDATE SET student_name = :studentName, text = :text, status = 'PENDING', submitted_at = now(), reviewed_at = NULL`) and `findByUserId(UUID userId)` to `TestimonialRepository.java` (from T004).
- [X] T020 [P] [US2] Create `back-end/src/main/java/com/kuky/backend/testimonials/dto/SubmitTestimonialRequest.java` (`@NotBlank @Size(min = ...) String text`) and `back-end/src/main/java/com/kuky/backend/testimonials/dto/MyTestimonialResponse.java` (`text`, `status`, `submittedAt`).
- [X] T021 [P] [US2] Create `back-end/src/main/java/com/kuky/backend/testimonials/service/TestimonialEmailService.java` — `sendSubmittedNotificationToTeacher(String teacherEmail, String studentName)` using `JavaMailSender` + `SimpleMailMessage` + a try/catch-and-log wrapper, following the exact shape of `back-end/src/main/java/com/kuky/backend/scheduling/service/BookingEmailService.java`'s `sendQuietly`.
- [X] T022 [US2] In `TestimonialService.java` (from T010), add `submit(String userEmail, String text)` (resolves the caller's id/full name via the existing user lookup used elsewhere, calls `repository.upsertByUser`, then calls `TestimonialEmailService.sendSubmittedNotificationToTeacher` with `SchedulingProperties.getTeacherEmail()`) and `getMyTestimonial(String userEmail)` (returns `Optional<MyTestimonialResponse>` via `repository.findByUserId`). Depends on T019, T020, T021.
- [X] T023 [US2] In `TestimonialController.java` (from T011), add `POST /api/v1/testimonials` (`@Valid @RequestBody SubmitTestimonialRequest`, calls `service.submit`) and `GET /api/v1/testimonials/me` (`204` if empty, else `MyTestimonialResponse`), per [contracts/testimonials-api.md](contracts/testimonials-api.md). Depends on T022.
- [X] T024 [P] [US2] In `SecurityConfig.java`, add `.requestMatchers(HttpMethod.POST, "/api/v1/testimonials").hasAnyRole("STUDENT", "ADMIN")` and `.requestMatchers(HttpMethod.GET, "/api/v1/testimonials/me").hasAnyRole("STUDENT", "ADMIN")`.
- [X] T025 [P] [US2] In `front-end/src/lib/testimonials.ts` (from T013), add `submitTestimonial(text: string)` (`POST /testimonials`) and `getMyTestimonial()` (`GET /testimonials/me`, handling `204` as "not submitted yet") plus a `MyTestimonial { text, status, submittedAt }` type.
- [X] T026 [US2] Create `front-end/src/components/learning/MyTestimonial.tsx` — shows the student's current status (`PENDING`/`APPROVED`/`REJECTED`/`UNPUBLISHED`) if one exists, plus a submit/resubmit text form; on a `403` response, renders the shared `StudentOnlyNotice`. Depends on T025.
- [X] T027 [US2] In `front-end/src/components/learning/LearningView.tsx`, render `<MyTestimonial />` alongside the existing `PastClassesList`/`HomeworkSubmitDialog`/`LearningContent` sections. Depends on T026.

**Checkpoint**: Students can submit/resubmit and see their own status; the teacher is notified by email; nothing appears publicly yet until Phase 5's approval exists (verifiable against Phase 3's already-built public endpoint, which correctly shows nothing for `PENDING` rows).

---

## Phase 5: User Story 3 - Teacher Curates Which Testimonials Appear (Priority: P3)

**Goal**: The teacher can review pending submissions (approve/reject), edit text, reorder, and unpublish/remove testimonials from the admin panel.

**Independent Test**: As admin, open the Testimonios tab, approve a pending submission (it appears on `/`), reorder two approved testimonials (homepage order updates), edit one's text (homepage reflects it), unpublish one (disappears from `/` but stays visible in the admin tab as `UNPUBLISHED`).

### Tests for User Story 3

- [X] T028 [P] [US3] Write `back-end/src/test/java/com/kuky/backend/admin/TestimonialAdminControllerTest.java` covering: `403` for non-admin on every endpoint; `GET /admin/testimonials` lists all statuses; `POST .../approve`, `POST .../reject`, `POST .../unpublish` transition status and set `reviewedAt`; `PUT /admin/testimonials/{id}` edits text without changing status; `PUT /admin/testimonials/reorder` reassigns `displayOrder` in the given order; `DELETE /admin/testimonials/{id}` removes the row; `404 TESTIMONIAL_NOT_FOUND` for an unknown id on any of the above.
- [X] T029 [P] [US3] Extend `back-end/src/test/java/com/kuky/backend/testimonials/TestimonialServiceTest.java` (from T017) with the admin transition methods: `approve`/`reject`/`unpublish` set the expected status and `reviewedAt`; `reorder` reassigns `displayOrder` per the given id order; `delete` removes the row; each throws `TestimonialNotFoundException` for an unknown id.

### Implementation for User Story 3

- [X] T030 [P] [US3] Add `findAll()` (`SELECT * FROM testimonials ORDER BY display_order`), `setStatus(UUID id, TestimonialStatus status)` (sets `reviewed_at = now()`), `setText(UUID id, String text)`, `reorder(List<UUID> orderedIds)` (batch `UPDATE ... SET display_order = :position`), and `delete(UUID id)` to `TestimonialRepository.java` (from T004/T019).
- [X] T031 [P] [US3] Create `back-end/src/main/java/com/kuky/backend/admin/dto/TestimonialAdminResponse.java` — `public record TestimonialAdminResponse(UUID id, String text, String studentName, TestimonialStatus status, int displayOrder, Instant submittedAt, Instant reviewedAt) {}`.
- [X] T032 [US3] In `TestimonialService.java` (from T022), add `listAll()`, `approve(UUID id)`, `reject(UUID id)`, `unpublish(UUID id)`, `updateText(UUID id, String text)`, `reorder(List<UUID> orderedIds)`, and `delete(UUID id)`, each throwing `TestimonialNotFoundException` when the id doesn't exist. Depends on T030, T031.
- [X] T033 [US3] Create `back-end/src/main/java/com/kuky/backend/admin/controller/TestimonialAdminController.java` at `/api/v1/admin/testimonials` with `GET` (list), `POST /{id}/approve`, `POST /{id}/reject`, `POST /{id}/unpublish`, `PUT /{id}` (edit text), `PUT /reorder`, `DELETE /{id}`, per [contracts/testimonials-api.md](contracts/testimonials-api.md) (no new `SecurityConfig` rule needed — already covered by the existing `/api/v1/admin/**` → `hasRole("ADMIN")` matcher). Depends on T032.
- [X] T034 [P] [US3] In `front-end/src/lib/admin.ts`, add `getAdminTestimonials()`, `approveTestimonial(id)`, `rejectTestimonial(id)`, `unpublishTestimonial(id)`, `updateTestimonialText(id, text)`, `reorderTestimonials(orderedIds)`, `deleteTestimonial(id)`, and the corresponding `AdminTestimonial` type, per [contracts/testimonials-api.md](contracts/testimonials-api.md).
- [X] T035 [US3] Create `front-end/src/components/admin/testimonials/TestimonialsTab.tsx` — a pending-review queue plus a published/unpublished list with reorder, edit, approve/reject, unpublish, and delete actions, mirroring the list/detail layout conventions of `front-end/src/components/admin/presentations`. Depends on T034.
- [X] T036 [US3] In `front-end/src/routes/panel.tsx`, register a new "Testimonios" admin tab rendering `TestimonialsTab`, alongside the existing Units/Homework/Presentations/Prueba de nivel tabs. Depends on T035.

**Checkpoint**: Full lifecycle works end-to-end — submit → notify → review → approve/reject/edit/reorder/unpublish → reflected on the public homepage — satisfying all three user stories together.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification and documentation once all three stories are complete.

- [X] T037 [P] Run `cd back-end && ./gradlew test && ./gradlew build` — confirm all new/extended tests (T007, T016-T018, T028-T029) and the full existing suite pass.
- [X] T038 [P] Run `cd front-end && npm run lint && npm run format` — confirm no lint/format regressions from T013-T015, T025-T027, T034-T036.
- [X] T039 Execute [quickstart.md](quickstart.md) Scenarios A-F manually against the running dev servers (including checking Mailpit at `http://localhost:8025` for the submission-notification email), per the constitution's browser-verification requirement.
- [X] T040 [P] Update the `/` row's description in the "Current pages" table in `CLAUDE.md` to mention the testimonials section, mirroring the terse style of that table.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T001) — BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational only. Independently testable/shippable (via directly-seeded `APPROVED` rows) even before Phases 4-5 exist.
- **User Story 2 (Phase 4)**: Depends on Foundational only. Independent of Phase 3's frontend, though its "nothing shows publicly while `PENDING`" behavior is verified against Phase 3's public endpoint.
- **User Story 3 (Phase 5)**: Depends on Foundational; functionally exercised against submissions created via Phase 4's flow and rendered via Phase 3's public endpoint, but has no hard code dependency on either.
- **Polish (Phase 6)**: Depends on Phases 3, 4, and 5 all being complete.

### Within Each User Story

- Tests are listed before implementation tasks (written/skimmed first per the repo's integration-test convention) but are not strict red-green TDD gates.
- Repository/DTO/email tasks before the service task that composes them; service before the controller task that wires it in.
- Backend before the frontend tasks that call it.

### Parallel Opportunities

- Foundational: T002, T003 [P] (different files); T005 can run alongside them.
- Within US1: T007 [P] (test) alongside T008, T009 [P] (repository method + DTO) — T010 waits for both; T012, T013 [P] (SecurityConfig + new frontend lib file) can run alongside the backend service/controller work.
- Within US2: T016, T017, T018 [P] (tests, different files); T019, T020, T021 [P] (repository methods + DTOs + email service) — T022 waits for all three; T024, T025 [P] (SecurityConfig + frontend lib) alongside T023.
- Within US3: T028, T029 [P] (tests); T030, T031 [P] (repository methods + DTO) — T032 waits for both; T034 [P] (frontend admin lib) alongside T033.
- Phase 6: T037, T038, T040 [P] (independent verification/doc tasks); T039 is manual and best run last.

---

## Parallel Example: User Story 1

```bash
# Test, alongside implementation start:
Task: "TestimonialControllerTest — public GET /api/v1/testimonials"

# Implementation, together (before the service task):
Task: "TestimonialRepository.findApproved()"
Task: "TestimonialResponse DTO"

# Frontend, alongside backend work:
Task: "SecurityConfig permitAll() matcher for GET /api/v1/testimonials"
Task: "front-end/src/lib/testimonials.ts scaffold + getTestimonials()"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Complete Phase 1: Setup (T001).
2. Complete Phase 2: Foundational (T002-T006) — critical, blocks everything.
3. Complete Phase 3: User Story 1 (T007-T015).
4. **STOP and VALIDATE**: run T007, seed an `APPROVED` row directly in the database, confirm it renders on `/` and disappears when deleted.
5. Deploy/demo if ready — the teacher can hand-seed a testimonial via direct SQL even before the submission/review UI (Phases 4-5) exists.

### Incremental Delivery

1. Setup + Foundational → schema/model exist, nothing visible yet.
2. Add User Story 1 → homepage can display (seeded) approved testimonials → MVP.
3. Add User Story 2 → students can submit their own, teacher gets notified.
4. Add User Story 3 → teacher can review/approve/reject/edit/reorder/unpublish, closing the full loop.
5. Polish → automated + manual verification, docs.

---

## Notes

- [P] tasks touch different files and have no unmet dependency within their phase.
- `testimonials` is a new, fully independent domain (own top-level package, own table) — no existing `learning`/`presentations`/`scheduling` code is modified besides `SecurityConfig.java` and `GlobalExceptionHandler.java`.
- `TestimonialRepository.java`, `TestimonialService.java`, and `TestimonialController.java` are each created once (in Phase 2 or US1) and extended by later phases — later tasks that touch the same file are sequential by nature, not marked `[P]`.
- One row per student is enforced by the `UNIQUE(user_id)` constraint from T001 plus the `upsertByUser` `ON CONFLICT` from T019 — there is no separate "check if exists then insert/update" branching logic to get wrong.
- Commit after each task or logical group; stop at any checkpoint to validate a story independently before continuing.
