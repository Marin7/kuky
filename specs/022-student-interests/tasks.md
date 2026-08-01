---

description: "Task list for student interests feature implementation"
---

# Tasks: Student Interests on Profile

**Input**: Design documents from `specs/022-student-interests/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/interests-api.md](contracts/interests-api.md), [quickstart.md](quickstart.md)

**Tests**: Included for backend — matches repo convention (`AuthControllerIntegrationTest`, service unit tests). Frontend has no test framework — verify via [quickstart.md](quickstart.md) in a browser.

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Schema every other task depends on.

- [X] T001 Write `back-end/src/main/resources/db/migration/V3__student_interests.sql` per [data-model.md](data-model.md): `ALTER TABLE users ADD COLUMN interests_note VARCHAR(280);`, `CREATE TABLE user_interests (user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, interest_code VARCHAR(40) NOT NULL, PRIMARY KEY (user_id, interest_code), CONSTRAINT user_interests_code_check CHECK (interest_code IN (...12 codes...)));`, `CREATE INDEX user_interests_user_id_idx ON user_interests (user_id);`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Catalogue constants, persistence, and `UserResponse` fields that both student write (US1) and teacher read (US2) need.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Create `back-end/src/main/java/com/kuky/backend/auth/InterestCatalogue.java` with the 12 allowed codes (`TRAVEL`, `MUSIC`, `SPORTS`, `FOOD`, `CINEMA`, `READING`, `TECHNOLOGY`, `NATURE`, `ART`, `WORK`, `FAMILY`, `CULTURE`), `MAX_SELECTIONS = 10`, `MAX_NOTE_LENGTH = 280`, and helpers `isAllowed(String)`, `filterKnown(Collection<String>)`.
- [X] T003 [P] Create `front-end/src/lib/interests.ts` exporting `INTEREST_CODES` (same 12 codes) and `InterestCode` union type, plus `MAX_INTEREST_SELECTIONS = 10` and `MAX_INTERESTS_NOTE_LENGTH = 280`.
- [X] T004 [P] In `back-end/src/main/java/com/kuky/backend/auth/model/User.java`, add `private String interestsNote` with getter/setter.
- [X] T005 In `back-end/src/main/java/com/kuky/backend/auth/repository/UserRepository.java`: map `interests_note` in `USER_MAPPER` onto `User.interestsNote` (T004); add `findInterestCodesByUserId(UUID userId)` (`SELECT interest_code FROM user_interests WHERE user_id = :id ORDER BY interest_code`); add `replaceInterests(UUID userId, List<String> codes)` (delete all rows for user then batch-insert); add `updateInterestsNote(UUID userId, String note)` (`UPDATE users SET interests_note = :note, updated_at = NOW() WHERE id = :id`). Depends on T001, T004.
- [X] T006 [P] In `back-end/src/main/java/com/kuky/backend/auth/dto/UserResponse.java`, add `List<String> interests` and `String interestsNote` per [contracts/interests-api.md](contracts/interests-api.md).
- [X] T007 In `back-end/src/main/java/com/kuky/backend/auth/service/AuthService.java`, extend `toResponse(User)` to load interest codes via `userRepository.findInterestCodesByUserId`, run `InterestCatalogue.filterKnown`, and pass `interests` + `user.getInterestsNote()` into `UserResponse` (T006) so `/me`, login, and profile responses include them (empty list / null when unset). Depends on T002, T005, T006.
- [X] T008 [P] Create `back-end/src/main/java/com/kuky/backend/auth/exception/InvalidInterestsException.java` (or equivalent) carrying a reason/code for `INVALID_INTEREST` and `TOO_MANY_INTERESTS`; map both to HTTP `400` with `{"error":"…","message":"…"}` in `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java` (Spanish messages matching existing style).
- [X] T009 [P] In `front-end/src/lib/auth.ts`, extend the `UserResponse` interface with `interests: string[]` and `interestsNote: string | null`.

**Checkpoint**: Schema, catalogue, and read-path DTO mapping exist. User stories can begin.

---

## Phase 3: User Story 1 - Student selects interests on their profile (Priority: P1) 🎯 MVP

**Goal**: A student (or ADMIN) can multi-select catalogue interests and an optional ≤280-char note on `/cuenta`, save via `PUT /auth/interests`, and see the selection persist across reloads. `USER` role never sees the editor.

**Independent Test**: Log in as a student, open `/cuenta`, select several interests + a note, save, reload — same values remain. Log in as a non-student `USER` — no interests section; API returns `403`.

### Tests for User Story 1

- [X] T010 [P] [US1] Extend or add `back-end/src/test/java/com/kuky/backend/auth/AuthServiceInterestsTest.java` (mirror `AuthServiceTimezoneTest`): `updateInterests` succeeds for `STUDENT`/`ADMIN` and persists codes + note; rejects `USER` with forbidden; rejects unknown code (`INVALID_INTEREST`); rejects >10 codes (`TOO_MANY_INTERESTS`); rejects note >280 via validation; empty list + null note clears prior data; unknown stored codes are filtered on `toResponse`.
- [X] T011 [P] [US1] Extend `back-end/src/test/java/com/kuky/backend/auth/AuthControllerIntegrationTest.java`: `PUT /api/v1/auth/interests` as student returns `200` with updated `UserResponse`; as `USER` returns `403`; unauthenticated returns `401`; invalid body (unknown code / too many / note too long) returns `400` with the contracted error codes.

### Implementation for User Story 1

- [X] T012 [P] [US1] Create `back-end/src/main/java/com/kuky/backend/auth/dto/UpdateInterestsRequest.java` — `record` with `List<String> interests` and `@Size(max = 280) String interestsNote` per [contracts/interests-api.md](contracts/interests-api.md).
- [X] T013 [US1] In `back-end/src/main/java/com/kuky/backend/auth/service/AuthService.java`, add `updateInterests(String email, UpdateInterestsRequest request)`: load user; if role is not `STUDENT` or `ADMIN`, reject with `403`/`FORBIDDEN`; normalize codes (trim, distinct); validate against `InterestCatalogue` (T002) and max 10 (else T008); normalize blank note to null; in one transaction call `replaceInterests` + `updateInterestsNote` (T005); return refreshed `toResponse` (T007). Depends on T007, T008, T012.
- [X] T014 [US1] In `back-end/src/main/java/com/kuky/backend/auth/controller/AuthController.java`, add `PUT /interests` accepting `@Valid UpdateInterestsRequest`, requiring non-null `@AuthenticationPrincipal` email (same 401 pattern as `PUT /profile`), delegating to `authService.updateInterests`. Depends on T013.
- [X] T015 [P] [US1] In `front-end/src/lib/auth.ts`, add `updateInterests(data: { interests: string[]; interestsNote: string | null })` calling `PUT /profile`-sibling path `/interests` and returning `UserResponse`. Depends on T009.
- [X] T016 [P] [US1] Add i18n keys in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`: `interests.TRAVEL`…`interests.CULTURE` (12 labels), plus `account.interests*` (section title, note label, save, success, max-selection hint, empty hint).
- [X] T017 [US1] Create `front-end/src/components/account/InterestsSetting.tsx` (parallel to `TimezoneSetting.tsx`): multi-select over `INTEREST_CODES` (T003) with localized labels, optional note `Textarea` with `maxLength={280}`, enforce max 10 selections in UI, save via `updateInterests` (T015), hydrate from `user.interests` / `user.interestsNote`, show success/error. Depends on T015, T016.
- [X] T018 [US1] In `front-end/src/routes/cuenta.tsx` `ProfileView`, render `<InterestsSetting />` only when `user.role === "STUDENT" || user.role === "ADMIN"` (after the profile form / near `TimezoneSetting`); do not render for `USER`. Depends on T017.

**Checkpoint**: Students can save and reload interests on `/cuenta`. This alone is a demonstrable MVP before the teacher UI exists (teacher can still verify via `/auth/me` or DB).

---

## Phase 4: User Story 2 - Teacher reviews student interests before class (Priority: P1)

**Goal**: Paula opens a student’s admin profile and sees localized interest labels plus the optional note in a dedicated scannable section (or an explicit empty state).

**Independent Test**: As a student, save a known set of interests + note (US1). As admin, open `/panel/alumnos/{id}` and confirm the same labels and note appear without leaving the profile page.

### Tests for User Story 2

- [X] T019 [P] [US2] Extend or add a unit/integration test covering `StudentProfileAdminService.getProfile`: response includes `interests` and `interestsNote` matching stored data; empty student returns `[]` and `null` (not omitted fields). Prefer extending an existing admin profile test if one exists under `back-end/src/test/java/com/kuky/backend/admin/`.

### Implementation for User Story 2

- [X] T020 [P] [US2] In `back-end/src/main/java/com/kuky/backend/admin/dto/StudentProfileResponse.java`, add `List<String> interests` and `String interestsNote`.
- [X] T021 [US2] In `back-end/src/main/java/com/kuky/backend/admin/service/StudentProfileAdminService.java`, load interest codes + note for the student (reuse `UserRepository` interest helpers / `User.interestsNote` from T005) and pass them into `StudentProfileResponse` (T020), filtering with `InterestCatalogue.filterKnown`. Depends on T005, T020.
- [X] T022 [P] [US2] In `front-end/src/lib/admin.ts`, extend `StudentProfile` with `interests: string[]` and `interestsNote: string | null`.
- [X] T023 [P] [US2] Add i18n keys under `admin.studentProfile.interests*` in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts` (section title, empty state copy such as “No interests selected yet”).
- [X] T024 [US2] In `front-end/src/routes/panel_.alumnos.$studentId.tsx`, add a read-only `Section` near the header/stats that lists localized labels for `profile.interests` (via `t("interests." + code)`) and shows `interestsNote` when present; when both empty, show the explicit empty state from T023. Depends on T021, T022, T023.

**Checkpoint**: Teacher can prepare for class by reading interests on the student profile. US1 + US2 together deliver the full primary value.

---

## Phase 5: User Story 3 - Student clears or leaves interests empty (Priority: P2)

**Goal**: Clearing all selections and the note persists an empty state on both student and teacher UIs without errors or blocking other profile actions.

**Independent Test**: Save interests, clear all + note, save again — student UI shows unselected/empty; admin profile shows empty state (not an error). Name/profile save still does not touch interests (FR-011).

### Implementation for User Story 3

- [X] T025 [US3] Confirm/adjust `AuthService.updateInterests` (T013) and `InterestsSetting` (T017) so saving `interests: []` and `interestsNote: null` fully clears `user_interests` and nulls the note; student UI shows the empty/unselected catalogue state after reload. Depends on T013, T017.
- [X] T026 [US3] In `front-end/src/routes/cuenta.tsx` / `InterestsSetting.tsx`, verify saving name/username via existing `updateProfile` does not call `updateInterests` and does not wipe interests (FR-011); fix if the form accidentally bundles fields. Depends on T018.
- [X] T027 [US3] Re-verify admin empty state (T024) after a full clear from the student side; tighten empty-state copy if needed so note-only vs fully-empty is unambiguous (empty only when no codes **and** no note). Depends on T024, T025.

**Checkpoint**: Optional interests never block the product; clear and empty paths are clean on both sides.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end validation and consistency.

- [X] T028 [P] Run `cd back-end && ./gradlew test` and fix any failures related to `UserResponse` constructor arity changes across existing auth tests.
- [X] T029 Run [quickstart.md](quickstart.md) scenarios A–F in a browser (student save, USER hidden, teacher view, empty state, validation, profile isolation) and fix any gaps.
- [X] T030 [P] Skim that interests never appear on public routes (landing, testimonials) — no accidental reuse of `UserResponse` fields on public pages.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on T001 — **BLOCKS** all user stories
- **US1 (Phase 3)**: Depends on Foundational — MVP
- **US2 (Phase 4)**: Depends on Foundational; practically needs US1 data to demo, but API/UI can be built in parallel
- **US3 (Phase 5)**: Depends on US1 + US2 UI paths
- **Polish (Phase 6)**: Depends on desired stories being complete

### User Story Dependencies

- **US1 (P1)**: After Foundational — no dependency on US2/US3
- **US2 (P1)**: After Foundational — read path independent of student UI; needs US1 for end-to-end demo data
- **US3 (P2)**: After US1 + US2 empty/clear surfaces exist

### Parallel Opportunities

- T002, T003, T004, T006, T008, T009 can proceed in parallel after T001 (T005 needs T001+T004)
- Within US1: T010/T011, T012, T015, T016 in parallel before T013/T017/T018
- Within US2: T019, T020, T022, T023 in parallel before T021/T024
- US1 frontend (T015–T018) and US2 backend (T020–T021) can run in parallel once Foundational is done

### Parallel Example: User Story 1

```bash
# After Foundational:
Task: "AuthServiceInterestsTest in back-end/src/test/.../AuthServiceInterestsTest.java"
Task: "AuthControllerIntegrationTest interests cases"
Task: "UpdateInterestsRequest.java"
Task: "updateInterests() in front-end/src/lib/auth.ts"
Task: "i18n interests.* + account.interests* in es/en/ro"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (migration)
2. Complete Phase 2: Foundational
3. Complete Phase 3: US1 (API + `/cuenta` editor)
4. **STOP and VALIDATE**: Student can save/reload interests; USER cannot
5. Demo if ready

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → student self-service MVP
3. US2 → teacher prep view (full primary value)
4. US3 → clear/empty polish
5. Polish → quickstart A–F

### Suggested MVP scope

**US1 only** (T001–T018): students can declare interests. Teacher visibility (US2) is the natural immediate follow-up for the stated “prepare the next lecture” outcome.

---

## Notes

- [P] = different files, no incomplete-task dependencies
- Do not add admin catalogue CRUD or booking/schedule surfaces (out of scope)
- Commit after each task or logical group
- Frontend verification is browser-based per constitution
