---

description: "Task list template for feature implementation"
---

# Tasks: Differentiate Users from Students

**Input**: Design documents from `specs/012-user-student-roles/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/user-roles-api.md](contracts/user-roles-api.md), [quickstart.md](quickstart.md)

**Tests**: Included — this repo has an established backend integration/unit test convention (18 existing test files, e.g. `BookingControllerIntegrationTest`, `PurchaseServiceTest`); new backend behavior gets matching test coverage. Frontend has no test framework (per constitution) — frontend verification is manual/browser-based via [quickstart.md](quickstart.md).

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: The schema change every other task depends on.

- [x] T001 Write `back-end/src/main/resources/db/migration/V23__add_user_role.sql` per [data-model.md](data-model.md): drop `users_role_check`, set `role` column default to `'USER'`, re-add `users_role_check` as `CHECK (role IN ('USER', 'STUDENT', 'ADMIN'))`. No `UPDATE` statement — existing rows are already `STUDENT`/`ADMIN`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The role default and the access-control gate that every user story's behavior depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] In `back-end/src/main/java/com/kuky/backend/auth/model/User.java`, change the `role` field initializer from `"STUDENT"` to `"USER"` (line 12) so new registrations default to non-student.
- [x] T003 [P] In `back-end/src/main/java/com/kuky/backend/config/SecurityConfig.java`, add `requestMatchers(...).hasAnyRole("STUDENT", "ADMIN")` rules (before the `anyRequest().authenticated()` fallthrough) for: `HttpMethod.POST, "/api/v1/bookings"`; `HttpMethod.POST, "/api/v1/purchases"`; and `"/api/v1/learning/**"` (all methods — coursework content itself is student-only, not just the submit actions). Leave `GET /api/v1/bookings`, `GET /api/v1/purchases`, `GET /api/v1/purchases/{id}/receipt`, the existing public `GET /api/v1/schedule` and `GET /api/v1/resources/**`, and everything under `/api/v1/placement/**` and `/api/v1/audio/**` untouched, per [contracts/user-roles-api.md](contracts/user-roles-api.md).
- [x] T004 [P] In `front-end/src/lib/auth.ts`, widen the `UserResponse.role` type (line 18) from `"STUDENT" | "ADMIN"` to `"USER" | "STUDENT" | "ADMIN"`.

**Checkpoint**: Role model and access gate exist. User stories can now begin.

---

## Phase 3: User Story 1 - Teacher designates a student (Priority: P1) 🎯 MVP

**Goal**: The teacher can browse registered (non-student) users in the admin panel and grant one of them student status, immediately unlocking booking/purchasing/coursework for that account.

**Independent Test**: Create a new account (defaults to `USER` per T002), confirm `POST /api/v1/bookings` returns `403` for it, have the admin call `POST /api/v1/admin/users/{id}/student`, confirm the same request now returns `201`, and confirm a "you're now a student" email arrived in Mailpit.

### Tests for User Story 1

- [x] T005 [P] [US1] Write `back-end/src/test/java/com/kuky/backend/admin/StudentAdminControllerIntegrationTest.java` covering: `GET /admin/users` returns only `role='USER'` accounts; `POST /admin/users/{id}/student` promotes a `USER` to `STUDENT` and returns `200`; calling it again on the same (now `STUDENT`) id is idempotent (`200`, no duplicate/error, per spec Acceptance Scenario US1.3); `404 USER_NOT_FOUND` for an unknown id or an `ADMIN` id; `403 ACCESS_DENIED` for a non-admin caller.
- [x] T006 [P] [US1] Write `back-end/src/test/java/com/kuky/backend/auth/EmailServiceTest.java` (new file) covering `sendStudentGrantedEmail(String toEmail)`: asserts a `SimpleMailMessage` is sent via the mock `JavaMailSender` with the given recipient and a non-empty Spanish subject/body, mirroring how `sendActivationEmail` would be tested.

### Implementation for User Story 1

- [x] T007 [P] [US1] In `back-end/src/main/java/com/kuky/backend/auth/repository/UserRepository.java`, add `findRegisteredUsers()` (`SELECT * FROM users WHERE role = 'USER' ORDER BY email`) and `promoteToStudentById(UUID id)` (`UPDATE users SET role = 'STUDENT', updated_at = NOW() WHERE id = :id AND role = 'USER'`, returning rows-affected), following the exact shape of the existing `promoteToAdminByEmail` method (lines 94-99).
- [x] T008 [P] [US1] Create `back-end/src/main/java/com/kuky/backend/admin/dto/RegisteredUserResponse.java` — `public record RegisteredUserResponse(UUID id, String email, String firstName, String lastName, String username) {}`, mirroring the existing `StudentResponse` record.
- [x] T009 [P] [US1] In `back-end/src/main/java/com/kuky/backend/auth/service/EmailService.java`, add `sendStudentGrantedEmail(String toEmail)` following the exact `SimpleMailMessage` pattern of `sendActivationEmail`/`sendPasswordResetEmail` (Spanish plain text, `fromAddress`, no link/token needed — just a notice that student access was granted).
- [x] T010 [US1] In `back-end/src/main/java/com/kuky/backend/admin/controller/StudentAdminController.java`, add `GET /admin/users` (maps `userRepository.findRegisteredUsers()` to `List<RegisteredUserResponse>`) and `POST /admin/users/{id}/student` (calls `promoteToStudentById`, throws a new `UserNotFoundException` → `404 USER_NOT_FOUND` if 0 rows affected *and* the user doesn't already have `role='STUDENT'` — treat "already a student" as success per idempotency, "no such user" as 404; then calls `emailService.sendStudentGrantedEmail`). Depends on T007, T008, T009.
- [x] T011 [P] [US1] In `front-end/src/lib/admin.ts`, add `getRegisteredUsers()` (`GET /admin/users`) and `promoteToStudent(id: string)` (`POST /admin/users/{id}/student`), typed per [contracts/user-roles-api.md](contracts/user-roles-api.md).
- [x] T012 [US1] Create `front-end/src/components/admin/students/UsersTab.tsx` — lists `role='USER'` accounts (via `getRegisteredUsers()`) with a "make student" button per row (calls `promoteToStudent`, then refetches), mirroring the loading/empty/list structure of `StudentsTab.tsx`. Depends on T011.
- [x] T013 [US1] In `front-end/src/routes/panel.tsx`, register a new "Users" admin tab rendering `UsersTab`, alongside the existing "Students" tab. Depends on T012.

**Checkpoint**: Teacher can view registered users and grant student status from `/panel`; promoted accounts can immediately book/purchase/access coursework (verified via API even before Phase 4's frontend messaging exists).

---

## Phase 4: User Story 2 - Non-student user experience (Priority: P2)

**Goal**: A non-student user can freely browse the schedule and resource listings, is clearly blocked (not silently failed) when attempting to book, purchase, or open coursework, and can still complete the placement test.

**Independent Test**: Log in as a fresh (`USER`-role) account; confirm `/reservas` and `/recursos` render their listings; confirm attempting to book or purchase shows the "ask the teacher" message instead of a raw error; confirm `/aprendizaje` shows the same message instead of coursework; confirm `/prueba-de-nivel` works normally throughout.

### Tests for User Story 2

- [x] T014 [P] [US2] Write `back-end/src/test/java/com/kuky/backend/config/SecurityConfigStudentGatingIntegrationTest.java` covering, for a `USER`-role authenticated caller: `403 ACCESS_DENIED` on `POST /api/v1/bookings`, `POST /api/v1/purchases`, and any `/api/v1/learning/**` endpoint; `200`/normal success on `GET /api/v1/schedule`, `GET /api/v1/resources/**`, `GET /api/v1/bookings`, `GET /api/v1/purchases`; and `401 UNAUTHENTICATED` for the same gated endpoints when anonymous. Also assert a `STUDENT`-role and an `ADMIN`-role caller both pass the gated endpoints (positive case backing US1/US3).

### Implementation for User Story 2

- [x] T015 [P] [US2] Create `front-end/src/components/StudentOnlyNotice.tsx` — a shared, named component rendering the FR-009 message ("El acceso de alumno lo concede la profesora." or equivalent i18n key) for use wherever a student-only action/page is blocked.
- [x] T016 [P] [US2] Add the notice copy and any new labels to the front-end i18n locale files (`front-end/src/i18n/locales/es.ts` primary, `ro.ts`), under a `studentOnly.*` (or similar) namespace.
- [x] T017 [US2] In `front-end/src/routes/reservas.tsx`, keep the schedule view open to any logged-in user, but gate the booking submission (based on `me.role !== "STUDENT" && me.role !== "ADMIN"`, and on a `403` from `POST /api/v1/bookings`) to render `StudentOnlyNotice` instead of / alongside the booking form. Depends on T004, T015, T016.
- [x] T018 [US2] In `front-end/src/routes/recursos.tsx`, keep resource listings/browsing open, but gate the purchase/unlock action the same way (role check + `403` handling → `StudentOnlyNotice`). Depends on T004, T015, T016.
- [x] T019 [US2] In `front-end/src/routes/aprendizaje.tsx`, gate the entire page on student status (`me.role !== "STUDENT" && me.role !== "ADMIN"` → render `StudentOnlyNotice` instead of the coursework content), since coursework itself is not public browsing per the clarified spec. Depends on T004, T015, T016.

**Checkpoint**: Non-student users get a clear, consistent, non-error experience across all three gated surfaces; placement test requires no changes (already open to any authenticated user via the untouched `/api/v1/placement/**` and `/prueba-de-nivel` route).

---

## Phase 5: User Story 3 - Teacher revokes student status (Priority: P3)

**Goal**: The teacher can revoke a student's status from the admin panel; the user loses student-only access going forward but keeps their history, and can be re-granted later without data loss.

**Independent Test**: Have a `STUDENT` account with an existing booking and purchase; revoke their status from `/panel`; confirm booking/purchasing/coursework are now blocked (reusing Phase 4's `StudentOnlyNotice` UX) while their existing booking and purchase remain visible in their history views; re-grant and confirm access returns.

### Tests for User Story 3

- [x] T020 [P] [US3] Extend `back-end/src/test/java/com/kuky/backend/admin/StudentAdminControllerIntegrationTest.java` (from T005) with: `DELETE /admin/users/{id}/student` demotes a `STUDENT` to `USER` and returns `200`; calling it again on the same (now `USER`) id is idempotent (`200`, no error); `404 USER_NOT_FOUND` for an unknown id or an `ADMIN` id; `403 ACCESS_DENIED` for a non-admin caller; a revoked user's pre-existing `GET /bookings` and `GET /purchases` responses are unchanged (history intact, per FR-012).
- [x] T021 [P] [US3] Extend `back-end/src/test/java/com/kuky/backend/auth/EmailServiceTest.java` (from T006) with a case for `sendStudentRevokedEmail(String toEmail)`, asserting a `SimpleMailMessage` is sent with the given recipient and appropriate Spanish subject/body.

### Implementation for User Story 3

- [x] T022 [P] [US3] In `back-end/src/main/java/com/kuky/backend/auth/repository/UserRepository.java`, add `revokeStudentById(UUID id)` (`UPDATE users SET role = 'USER', updated_at = NOW() WHERE id = :id AND role = 'STUDENT'`, returning rows-affected), same shape as `promoteToStudentById` (T007).
- [x] T023 [P] [US3] In `back-end/src/main/java/com/kuky/backend/auth/service/EmailService.java`, add `sendStudentRevokedEmail(String toEmail)` following the same pattern as `sendStudentGrantedEmail` (T009), with copy explaining student access was removed.
- [x] T024 [US3] In `back-end/src/main/java/com/kuky/backend/admin/controller/StudentAdminController.java`, add `DELETE /admin/users/{id}/student` (calls `revokeStudentById`, same idempotency/404 handling as the promote endpoint, then calls `emailService.sendStudentRevokedEmail`). Depends on T022, T023.
- [x] T025 [P] [US3] In `front-end/src/lib/admin.ts` (from T011), add `revokeStudent(id: string)` (`DELETE /admin/users/{id}/student`).
- [x] T026 [US3] In `front-end/src/components/admin/students/StudentsTab.tsx`, add a "revoke" button per row calling `revokeStudent`, then refetching (so the row moves off the Students list). Depends on T025.

**Checkpoint**: Full grant → block/unblock → revoke → re-grant lifecycle works end-to-end with no data loss, satisfying all three user stories.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification and documentation once all three stories are complete.

- [x] T027 [P] Run `cd back-end && ./gradlew test && ./gradlew build` — confirm all new/extended tests (T005, T006, T014, T020, T021) and the full existing suite pass.
- [x] T028 [P] Run `cd front-end && npm run lint && npm run format` — confirm no lint/format regressions from T004, T011–T013, T015–T019, T025–T026.
- [x] T029 Execute [quickstart.md](quickstart.md) Scenarios A–E manually against the running dev servers (including checking Mailpit at `http://localhost:8025` for the grant/revoke emails), per the constitution's browser-verification requirement.
- [x] T030 [P] Update the "Admin role" bullet under **Back-end** in `CLAUDE.md` (or add a new bullet) to document the `USER` / `STUDENT` / `ADMIN` role model and where the student-only gate lives, mirroring the existing terse style of that section.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup (T001) — BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational only. Can ship as MVP on its own (verified via API/admin panel, ahead of Phase 4's polished frontend messaging).
- **User Story 2 (Phase 4)**: Depends on Foundational only (T004 specifically). Independent of Phase 3's admin UI, but its `StudentOnlyNotice` component (T015) is reused by Phase 5.
- **User Story 3 (Phase 5)**: Depends on Foundational; its backend endpoints (T022-T024) mirror Phase 3's (T007-T010) and its frontend row action (T026) sits in the same file Phase 3 doesn't touch (`StudentsTab.tsx`, pre-existing) — no hard code dependency on Phase 3 or 4, but functionally it's tested against an account already promoted via Phase 3's flow.
- **Polish (Phase 6)**: Depends on Phases 3, 4, and 5 all being complete.

### Within Each User Story

- Tests are listed before implementation tasks (write/skim them first per repo's integration-test convention) but are not strict red-green TDD gates.
- Repository/DTO/email tasks before the controller task that wires them together.
- Backend before the frontend tasks that call it.

### Parallel Opportunities

- Foundational: T002, T003, T004 (three different files, no interdependency).
- Within US1: T005, T006 [P] (tests, different files) and T007, T008, T009 [P] (implementation, different files) — T010 waits for all three.
- Within US2: T014 [P] (test) alongside T015, T016 [P] (component + i18n) — T017-T019 wait for T015/T016.
- Within US3: T020, T021 [P] (tests) and T022, T023 [P] (implementation) — T024 waits for both.
- Phase 6: T027, T028, T030 [P] (independent verification/doc tasks); T029 is manual and best run last.

---

## Parallel Example: User Story 1

```bash
# Tests, together:
Task: "StudentAdminControllerIntegrationTest — GET /admin/users, POST /admin/users/{id}/student"
Task: "EmailServiceTest — sendStudentGrantedEmail"

# Implementation, together (before the controller task):
Task: "UserRepository.findRegisteredUsers() + promoteToStudentById()"
Task: "RegisteredUserResponse DTO"
Task: "EmailService.sendStudentGrantedEmail()"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Complete Phase 1: Setup (T001).
2. Complete Phase 2: Foundational (T002-T004) — critical, blocks everything.
3. Complete Phase 3: User Story 1 (T005-T013).
4. **STOP and VALIDATE**: run T005/T006 tests, promote a test account via `/panel`, confirm `POST /api/v1/bookings` flips from `403` to `201` for it.
5. Deploy/demo if ready — the teacher can already curate her student roster even before Phase 4's non-student messaging polish ships.

### Incremental Delivery

1. Setup + Foundational → gate exists, nothing yet visibly changes for end users besides new registrations defaulting to `USER`.
2. Add User Story 1 → teacher can grant student status → MVP.
3. Add User Story 2 → non-student users get a clear, non-broken experience instead of raw `403`s.
4. Add User Story 3 → teacher can also revoke, closing the full lifecycle.
5. Polish → automated + manual verification, docs.

---

## Notes

- [P] tasks touch different files and have no unmet dependency within their phase.
- Every gated backend endpoint change lives entirely in `SecurityConfig.java` (T003) — no `@PreAuthorize` annotations are introduced, matching the codebase's sole existing authorization pattern.
- No migration ever `UPDATE`s existing rows — grandfathering (FR-010) is a side effect of the current default already being `'STUDENT'`.
- Commit after each task or logical group; stop at any checkpoint to validate a story independently before continuing.
