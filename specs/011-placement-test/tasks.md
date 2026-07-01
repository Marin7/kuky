# Tasks: Placement Test for Potential Students

**Input**: Design documents from `specs/011-placement-test/`

**Branch**: `011-placement-test` | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

**Tests**: Included — `PlacementGradingServiceTest`, `PlacementScoringServiceTest`, `PlacementFlowIntegrationTest`, and `PlacementAdminServiceTest` are named in plan.md's source tree and technical context.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared-state dependencies)
- **[Story]**: Which user story this task belongs to (US1–US3)
- Exact file paths are relative to repo root. Backend root: `back-end/src/main/java/com/kuky/backend/placement/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration all other work depends on.

- [x] T001 Write `back-end/src/main/resources/db/migration/V19__create_placement_test.sql` — create the eight `placement_*` tables (`placement_config`, `placement_questions`, `placement_question_options`, `placement_attempts`, `placement_attempt_sections`, `placement_answers`, `placement_answer_options`, `placement_writing_submissions`) with all columns, indexes, and FKs per [data-model.md](data-model.md). CHECK constraints: Skill on `placement_questions.skill`/`placement_attempt_sections.skill`; QuestionKind on `placement_questions.kind`; AttemptStatus on `placement_attempts.status`; the `A1..C2` CefrLevel CHECK applies **only** to `placement_questions.cefr_level` (an authored item tag) — leave `placement_attempts.overall_cefr` and `placement_attempt_sections.cefr_level` as unconstrained `VARCHAR(2) NULL` since they may also hold the computed value `A0` ("below A1"). Seed one default `placement_config` row (limits 600/480/420, threshold 60, empty texts). Reuse `audio_files`/`bookings`/`users` (no alters).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Java enums, POJO models, repositories, exceptions, and front-end service scaffolding every story needs.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Create `model/Skill.java` — enum `READING`, `LISTENING`, `GRAMMAR`
- [x] T003 [P] Create `model/CefrLevel.java` — enum `A1`,`A2`,`B1`,`B2`,`C1`,`C2` (note: `A0`/"below A1" is a computed result string, not an enum value)
- [x] T004 [P] Create `model/QuestionKind.java` — enum `SINGLE_CHOICE`, `MULTI_CHOICE`, `FILL_BLANK` (placement-local, decoupled from `learning`)
- [x] T005 [P] Create `model/AttemptStatus.java` — enum `IN_PROGRESS`, `COMPLETED`
- [x] T006 [P] Create `model/PlacementConfig.java` — POJO: section time limits, `passThresholdPercent`, `writingPrompt`, `paymentInstructions`, `updatedAt`
- [x] T007 [P] Create `model/PlacementQuestionOption.java` — POJO: `id`, `questionId`, `position`, `label`, `isCorrect`
- [x] T008 Create `model/PlacementQuestion.java` — POJO: `id`, `skill`, `position`, `kind`, `prompt`, `cefrLevel`, `audioUrl`, `audioFileId`, `active`, `List<PlacementQuestionOption> options` (depends on T002, T003, T004, T007)
- [x] T009 [P] Create `model/PlacementAttempt.java` — POJO: `id`, `userId`, `status`, `startedAt`, `completedAt`, `overallCefr` (depends on T005)
- [x] T010 [P] Create `model/PlacementAttemptSection.java` — POJO: `id`, `attemptId`, `skill`, `startedAt`, `deadlineAt`, `submittedAt`, `scorePercent`, `cefrLevel` (depends on T002)
- [x] T011 [P] Create `model/PlacementAnswer.java` — POJO: `id`, `attemptSectionId`, `questionId` (nullable), `answerText`, `score` (BigDecimal 4,3), `List<UUID> selectedOptionIds`
- [x] T012 [P] Create `model/PlacementWritingSubmission.java` — POJO: `id`, `userId`, `body`, `promptSnapshot`, `submittedAt`
- [x] T013 Create `repository/PlacementConfigRepository.java` — `NamedParameterJdbcTemplate` read/update of the singleton row (depends on T006)
- [x] T014 Create `repository/PlacementQuestionRepository.java` — find active questions by skill (with options), full admin fetch, insert/update/delete, reorder (depends on T008)
- [x] T015 Create `repository/PlacementAttemptRepository.java` — attempts + `placement_attempt_sections` CRUD: find/create in-progress attempt, find section by `(attempt, skill)`, start section (stamp `deadline_at`), finalize section, complete attempt (depends on T009, T010)
- [x] T016 Create `repository/PlacementAnswerRepository.java` — bulk save answers + selected options for a section; load by section (depends on T011)
- [x] T017 Create `repository/PlacementWritingRepository.java` — insert submission; find latest + history by user (depends on T012)
- [x] T018 [P] Create exceptions in `exception/`: `PlacementNotFoundException` (404), `SectionAlreadySubmittedException` (409), `SectionNotStartedException` (400)
- [x] T019 Map the new exceptions to `{error,message}` codes (`PLACEMENT_NOT_FOUND`, `SECTION_ALREADY_SUBMITTED`, `SECTION_NOT_STARTED`, `VALIDATION_ERROR`) in the existing `GlobalExceptionHandler` (depends on T018)
- [x] T020 [P] Create `front-end/src/lib/placement.ts` — module scaffold: shared TS types (Skill, CefrLevel, section/question DTOs) and a typed `fetch` helper with `credentials: 'include'` (mirrors `lib/learning.ts`)
- [x] T021 [P] Add a `placement.*` i18n namespace skeleton to the front-end locale files (`es` primary, `en`)

**Checkpoint**: Backend domain skeleton + FE service module exist; user stories can begin.

---

## Phase 3: User Story 1 - Free self-assessment of Spanish level (Priority: P1) 🎯 MVP

**Goal**: A logged-in user takes three timed auto-graded sections and instantly gets per-skill CEFR levels + overall + score breakdown.

**Independent Test**: Seed a few questions per skill (T030), log in, open `/prueba-de-nivel`, complete the three timed sections (test warn-then-allow, timer auto-submit, refresh-resume), and confirm per-skill CEFR + overall display within ~3 s. Anonymous access redirects to `/cuenta`.

### Tests for User Story 1 ⚠️ (write first, ensure they fail)

- [x] T022 [P] [US1] `back-end/src/test/java/com/kuky/backend/placement/PlacementGradingServiceTest.java` — single-choice exact 0/1, multi-choice partial credit (`rightDecisions/optionCount`), fill-blank trim+lowercase+accent-exact, unanswered → 0
- [x] T023 [P] [US1] `back-end/src/test/java/com/kuky/backend/placement/PlacementScoringServiceTest.java` — per-skill CEFR banding (research §2): cumulative ≥ threshold walk, exactly-60% boundary, all-fail → `A0`, overall = weakest skill, identical answers → identical levels
- [x] T024 [P] [US1] `back-end/src/test/java/com/kuky/backend/placement/PlacementFlowIntegrationTest.java` — anonymous → 401 `UNAUTHENTICATED`; student GET `/placement/test` never includes `isCorrect`/`cefrLevel`/accepted answers; submit after `deadline_at` grades sent answers with blanks incorrect; second submit of a section → 409 `SECTION_ALREADY_SUBMITTED`

### Implementation for User Story 1

- [x] T025 [P] [US1] Create student DTOs in `dto/`: `PlacementTestResponse`, `SectionDto`, `StudentQuestionDto`, `StudentOptionDto`, `StartSectionResponse`, `SubmitSectionRequest`, `SectionResultResponse`, `AttemptResultResponse` (records; no answer key in student-facing types)
- [x] T026 [US1] Create `service/PlacementGradingService.java` — grade one question per kind (mirror `ExerciseGradingService` rules), per-section score percent + per-question results (depends on T014, T016)
- [x] T027 [US1] Create `service/PlacementScoringService.java` — map per-skill graded items to a CEFR level via the cumulative-threshold rule; derive overall = weakest (depends on T013)
- [x] T028 [US1] Create `service/PlacementService.java` — `getTest` (student view, answer key hidden, current attempt/section state + remaining time), `startAttempt` (find-or-create in-progress), `startSection` (stamp `deadline_at` from config), `submitSection` (single-submission lock, server-authoritative deadline, grade via T026, band via T027, finalize), `getResult` (aggregate, set overall + complete attempt) (depends on T015, T016, T026, T027, T025)
- [x] T029 [US1] Create `controller/PlacementController.java` — `GET /api/v1/placement/test`, `POST /api/v1/placement/attempts`, `POST /api/v1/placement/attempts/{id}/sections/{skill}/start`, `POST .../submit`, `GET /api/v1/placement/attempts/{id}/result`; current user via `@AuthenticationPrincipal String email` (depends on T028)
- [x] T030 [US1] Add a dev-only seed script `back-end/src/main/resources/db/dev/placement_seed.sql` (sample questions across skills/levels) and document running it for manual US1 validation (NOT a Flyway migration)
- [x] T031 [P] [US1] Create `front-end/src/components/placement/PlacementQuestion.tsx` — render `SINGLE_CHOICE` (RadioGroup), `MULTI_CHOICE` (Checkboxes), `FILL_BLANK` (Input); for Listening, an `<audio controls>` player sourced from `audioUrl` or `GET /api/v1/audio/{audioFileId}` (replayable)
- [x] T032 [US1] Create `front-end/src/components/placement/SectionRunner.tsx` — countdown to `deadlineAt` (Progress bar), warn-then-allow submit listing unanswered questions, silent auto-submit on expiry, resume from server `deadlineAt` on mount (depends on T031, T035)
- [x] T033 [P] [US1] Create `front-end/src/components/placement/PlacementResult.tsx` — per-skill CEFR cards, overall estimate, per-section score breakdown, CTA to the full evaluation
- [x] T034 [P] [US1] Create `front-end/src/components/placement/PlacementIntro.tsx` — login-gated intro explaining the three timed sections + "start" button
- [x] T035 [US1] Add US1 functions to `front-end/src/lib/placement.ts` — `getTest`, `startAttempt`, `startSection`, `submitSection`, `getResult` (depends on T020)
- [x] T036 [US1] Create route `front-end/src/routes/prueba-de-nivel.tsx` — `getMe()` in `useEffect` (redirect to `/cuenta` when anonymous), orchestrate Intro → SectionRunner (per skill) → Result; `seo()` head (depends on T032, T033, T034, T035)
- [x] T037 [US1] Add US1 `placement.*` strings to `es`/`en` locale files (intro, timer, warn dialog, result labels) (depends on T021)

**Checkpoint**: With seeded questions, US1 is fully functional and independently testable (the MVP).

---

## Phase 4: User Story 2 - Pursue the full evaluation (Priority: P2)

**Goal**: A logged-in student sees static bank-transfer instructions, submits a Writing response (no payment gate), and books a normal appointment via `/reservas`.

**Independent Test**: As a student, open the full-evaluation panel → bank-transfer text shows with no payment form; submit Writing → stored & confirmed without any gate; "book appointment" CTA opens `/reservas` and books like a regular class.

### Tests for User Story 2 ⚠️

- [x] T038 [P] [US2] Add to `PlacementFlowIntegrationTest.java` — `POST /placement/writing` stores a submission visible to the teacher; writing submit succeeds with **no** payment/order check; `GET /placement/full-evaluation` returns instructions + prompt + the user's latest submission and exposes **no** payment order/status/amount field

### Implementation for User Story 2

- [x] T039 [P] [US2] Create DTOs in `dto/`: `FullEvaluationResponse` (paymentInstructions, writingPrompt, mySubmission), `SubmitWritingRequest` (body), `WritingSubmissionDto`
- [x] T040 [US2] Extend `service/PlacementService.java` — `getFullEvaluation` (config text + writing prompt + latest submission), `submitWriting` (validate non-empty, snapshot prompt, persist) (depends on T017, T013, T039)
- [x] T041 [US2] Extend `controller/PlacementController.java` — `GET /api/v1/placement/full-evaluation`, `POST /api/v1/placement/writing` (depends on T040)
- [x] T042 [P] [US2] Create `front-end/src/components/placement/FullEvaluationPanel.tsx` — render static bank-transfer instructions, a Writing `Textarea` + submit, and a "book appointment" button linking to `/reservas` (no payment UI)
- [x] T043 [US2] Add US2 functions to `front-end/src/lib/placement.ts` — `getFullEvaluation`, `submitWriting` (depends on T020)
- [x] T044 [US2] Wire `FullEvaluationPanel` into `prueba-de-nivel.tsx` after the result (reachable via the US1 result CTA) (depends on T042, T043, T036)
- [x] T045 [US2] Add US2 `placement.*` strings to `es`/`en` (payment instructions heading, writing form, booking CTA) (depends on T021)

**Checkpoint**: US1 + US2 both work independently; revenue path (offline) + writing + booking complete.

---

## Phase 5: User Story 3 - Teacher authoring & review (Priority: P2)

**Goal**: The teacher authors questions/config in `/panel` and reviews a student's per-skill results + Writing together in the student-detail view.

**Independent Test**: As admin, add questions per skill (validation enforced), edit time limits/prompt/payment text; then open a student who took the test and see their CEFR results + Writing submission side by side.

### Tests for User Story 3 ⚠️

- [x] T046 [P] [US3] `back-end/src/test/java/com/kuky/backend/placement/PlacementAdminServiceTest.java` — authoring validation (single-choice exactly 1 correct, multi-choice ≥1 correct, fill-blank ≥1 accepted answer, Listening at most one audio source); config update round-trip; question reorder; student-evaluation aggregation shape

### Implementation for User Story 3

- [x] T047 [P] [US3] Create admin DTOs in `dto/`: `AdminQuestionDto` (full, with answer key + `cefrLevel`), `UpsertQuestionRequest`, `PlacementConfigDto`, `UpdateConfigRequest`, `ReorderQuestionsRequest`, `StudentEvaluationResponse` (result + writing list)
- [x] T048 [US3] Create `service/PlacementAdminService.java` — config read/update; question create/update/delete with per-kind validation; reorder; `getStudentEvaluation` (latest completed attempt + writing history) (depends on T013, T014, T015, T017, T047)
- [x] T049 [US3] Create `controller/PlacementAdminController.java` — `/api/v1/admin/placement/**`: config GET/PUT, questions GET/POST/PUT/DELETE, reorder PUT, `students/{id}/evaluation` GET (ADMIN role) (depends on T048)
- [x] T050 [P] [US3] Add admin functions to `front-end/src/lib/admin.ts` (or a `placement` admin section in `lib/placement.ts`) — config, question CRUD, reorder, student-evaluation (depends on T020)
- [x] T051 [P] [US3] Create `front-end/src/components/placement/admin/PlacementAuthoring.tsx` (+ sub-components) — question editor (kind, prompt, options/answer key, CEFR tag, audio upload reusing the existing admin audio upload), config form (time limits, threshold, writing prompt, payment text), up/down reorder
- [x] T052 [US3] Add a "Prueba de nivel" tab to `front-end/src/routes/panel.tsx` rendering `PlacementAuthoring` (depends on T050, T051)
- [x] T053 [US3] Extend `front-end/src/routes/panel_.alumnos.$studentId.tsx` — show the student's placement per-skill CEFR result + Writing submission(s) together (depends on T050)
- [x] T054 [US3] Add US3 admin `placement.*` strings to `es`/`en` (depends on T021)

**Checkpoint**: All three user stories independently functional; teacher can fully operate the feature.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [x] T055 [P] Add the new error codes (`PLACEMENT_NOT_FOUND`, `SECTION_ALREADY_SUBMITTED`, `SECTION_NOT_STARTED`, `VALIDATION_ERROR`) to `contracts/api.md`
- [x] T056 [P] Update `CLAUDE.md` "Current pages" table — add `/prueba-de-nivel` and the `/panel` "Prueba de nivel" tab
- [x] T057 Run `cd front-end && npm run lint && npm run format` and fix issues
- [x] T058 Run `cd back-end && ./gradlew test build` — all placement tests green
- [x] T059 Execute [quickstart.md](quickstart.md) Scenarios A–D in a running browser (constitution: visual verification before done). All four scenarios verified end-to-end in a live browser (admin authoring, timed auto-graded test with per-skill CEFR, offline-payment full evaluation, teacher review). Verification surfaced and fixed two real bugs:
  1. **Race condition** in `PlacementService.startSection` (check-then-insert on `(attempt, skill)` was not atomic) — a duplicate/concurrent start call hit the DB's unique constraint and, compounded by `GlobalExceptionHandler`'s generic `DuplicateKeyException` handler mislabeling it as a booking `SLOT_UNAVAILABLE` error, left the section's countdown stuck at 0:00 client-side. Fixed by catching the duplicate-key conflict and falling back to the existing row (idempotent start); added a concurrent-call regression test (`startSection_concurrentDuplicateCalls_bothSucceedWithSameDeadline`).
  2. **Stale closure** in `SectionRunner.tsx`'s auto-submit timer — `doSubmit`'s default answer payload read `answers` from the closure captured when the deadline effect was set up (mount time), not the latest render, so a section that auto-submitted on timeout silently dropped any answers given after mount. Fixed with an `answersRef` kept in sync via effect, read by `doSubmit` instead of the closed-over state.
  Also fixed a pre-existing strict-mode TypeScript violation in `PlacementResult.tsx` (untyped `t` parameter) surfaced by `tsc --noEmit`, and added the `placement.section.startError` i18n string (all 3 locales) for genuine (non-race) start failures.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Phase 1 — **blocks all user stories**.
- **User Stories (Phase 3–5)**: all depend on Phase 2. US1 is the MVP; US2 and US3 depend only on Foundational (US3's student-review view is most useful after US1/US2 produce data, but is independently testable with seeded data).
- **Polish (Phase 6)**: after the desired user stories.

### User Story Dependencies

- **US1 (P1)**: Foundational only. Independently testable via the dev seed (T030).
- **US2 (P2)**: Foundational only. Its UI is reached from the US1 result page (T044 depends on T036), but the backend writing/full-evaluation path is independent.
- **US3 (P2)**: Foundational only. Authoring is independent; the student-review view reads US1/US2 data.

### Within Each User Story

- Tests first (write failing) → DTOs → repositories (Phase 2) → services → controllers → front-end components → route wiring → i18n.

### Parallel Opportunities

- Phase 2: T002–T007, T009–T012, T018, T020, T021 are `[P]` (distinct files).
- US1 tests T022–T024 run in parallel; components T031/T033/T034 in parallel.
- US2 and US3 backend can be built in parallel by different developers once Phase 2 is done.

---

## Parallel Example: User Story 1 tests

```bash
Task: "PlacementGradingServiceTest in back-end/src/test/java/com/kuky/backend/placement/PlacementGradingServiceTest.java"
Task: "PlacementScoringServiceTest in back-end/src/test/java/com/kuky/backend/placement/PlacementScoringServiceTest.java"
Task: "PlacementFlowIntegrationTest in back-end/src/test/java/com/kuky/backend/placement/PlacementFlowIntegrationTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1 (migration) → Phase 2 (foundational) → Phase 3 (US1) + dev seed.
2. **STOP and VALIDATE**: take the timed test end-to-end, confirm per-skill CEFR + timer authority + answer-key hiding.
3. Demo the free self-assessment — it delivers value alone (lead generation).

### Incremental Delivery

1. Setup + Foundational → ready.
2. US1 → free auto-graded test (MVP).
3. US2 → offline-payment full evaluation (writing + booking).
4. US3 → teacher authoring (replaces the dev seed) + student review.
5. Polish → docs, lint/format, build, quickstart verification.

---

## Notes

- `[P]` = different files, no dependencies.
- The dev seed (T030) lets US1 ship and be validated before US3 authoring exists; US3 then makes authoring first-class and the seed can be retired.
- No new third-party dependencies; reuse `audio_files` (audio), `bookings` (appointment), existing auth/admin/notification infrastructure.
- No payment order/status/amount is created anywhere (FR-009/FR-012) — verify in T038.
- Commit after each task or logical group; verify backend tests fail before implementing.
