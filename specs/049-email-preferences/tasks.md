---

description: "Task list for 049-email-preferences"
---

# Tasks: Email Preferences

**Input**: Design documents from `/specs/049-email-preferences/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/email-preferences-api.md](./contracts/email-preferences-api.md), [quickstart.md](./quickstart.md)

**Tests**: Included. Not because the spec asked for TDD, but because [research.md R9](./research.md) settled a specific testing split as a design decision — unit tests locally for gating and grouping, CI-only integration tests for the DB-level delta semantics. Task IDs below follow that split.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel — different files, no dependency on an incomplete task
- **[Story]**: `[US1]`, `[US2]`, `[US3]` — maps to the user stories in `spec.md`
- Every task names its exact file path

## Path Conventions

Two roots, per `plan.md`: `back-end/src/main/java/com/kuky/backend/...` and `front-end/src/...`.

---

## Phase 1: Setup

**Purpose**: Confirm the ground is as the plan assumes. Both tasks are checks, not changes — but a wrong assumption here invalidates every validation run later.

- [X] T001 Confirm `V26` is still the next free Flyway version in `back-end/src/main/resources/db/migration/` (latest at plan time was `V25__homework_student_review_unseen.sql`); if another migration has since landed, use the next free number and update the references in [data-model.md](./data-model.md) §1 and [plan.md](./plan.md)
- [X] T002 [P] Confirm `app.mail.enabled: true` and the Mailpit SMTP settings (`localhost:1025`) in `back-end/src/main/resources/application-local.yaml`, and that the Mailpit UI is reachable at <http://localhost:8025> — when mail is disabled every send is a silent no-op, so all email scenarios would falsely appear to pass
  - ✅ Config confirmed: `app.mail.enabled: true`, host `localhost`, port `1025`.
  - ⚠️ **Mailpit is not installed on this machine** (no binary, no Docker). Sending was therefore verified by send *attempts* in the back-end log rather than by inbox contents — see the T022 note. Message bodies are covered by unit tests.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The stored preference and its access path. Every user story reads or writes this column.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Create `back-end/src/main/resources/db/migration/V26__email_preferences.sql` adding `email_on_homework_assigned BOOLEAN NOT NULL DEFAULT false` to `users` — a single `ALTER TABLE ... ADD COLUMN` with **no** backfill, no `UPDATE`, and no data-dependent default, per [data-model.md](./data-model.md) §1 (this DDL is what makes FR-002 structural rather than a step someone must remember to omit)
- [X] T004 [P] Create `back-end/src/main/java/com/kuky/backend/preferences/model/EmailPreferenceType.java` — enum with the single constant `NEW_HOMEWORK_ASSIGNED`; the constant name is the wire value and is stable API surface ([data-model.md](./data-model.md) §2)
- [X] T005 Create `back-end/src/main/java/com/kuky/backend/preferences/repository/EmailPreferencesRepository.java` using `NamedParameterJdbcTemplate` — read all preference values for a user id, write one value for a user id, and hold the enum→column mapping in one place
- [X] T006 [P] Add `.requestMatchers("/api/v1/me/**").authenticated()` to `back-end/src/main/java/com/kuky/backend/config/SecurityConfig.java`, beside the existing `/api/v1/notifications/**` entry and above `anyRequest()` — **must not** be mounted under `/api/v1/auth/**`, which is `permitAll()` at `SecurityConfig:47` and would expose a signed-in user's settings to anonymous callers ([contracts](./contracts/email-preferences-api.md), [R5](./research.md))

**Checkpoint**: Column exists, defaults false for every account, and the route prefix is authenticated. User stories can begin.

---

## Phase 3: User Story 1 — Student turns on homework emails and gets notified (Priority: P1) 🎯 MVP

**Goal**: A student can find and switch on the option at `/cuenta`, and newly assigned homework then reaches them by email — one email per teacher action, from all three grant routes.

**Independent Test**: Switch the option on as Ana; as the teacher assign her a unit containing three homework items; confirm exactly one email listing all three arrives in Mailpit and links to `/aprendizaje`.

**Note**: This is the largest phase because it is the only one that delivers end-to-end value — you cannot opt in without the UI, and opting in proves nothing without the send path. Groups A and B (preferences) and group C (email pipeline) touch disjoint files and can be built in parallel by two people; C's validation needs A+B.

### Group A — Preferences API

- [X] T007 [P] [US1] Create `back-end/src/main/java/com/kuky/backend/preferences/dto/EmailPreferenceResponse.java` and `.../dto/UpdateEmailPreferenceRequest.java` — response carries `type` + `enabled`; request carries a `@NotNull Boolean enabled`
- [X] T008 [US1] Create `back-end/src/main/java/com/kuky/backend/preferences/service/EmailPreferencesService.java` — `list(email)` returns **every** `EmailPreferenceType` constant with its effective value in enum declaration order (never omits a supported type); `set(email, type, enabled)` is idempotent; an unrecognised type throws `IllegalArgumentException` so `GlobalExceptionHandler` renders the existing `VALIDATION_ERROR`, adding no new exception class or error code
- [X] T009 [US1] Create `back-end/src/main/java/com/kuky/backend/preferences/controller/EmailPreferencesController.java` mapped at `/api/v1/me/email-preferences` — `GET` returns `{"preferences":[...]}`, `PUT /{type}` returns the stored state; the account is resolved from the authenticated principal and **no user id appears in the path or body**, which is what makes FR-005 and FR-015 structural
- [X] T010 [P] [US1] Create `back-end/src/test/java/com/kuky/backend/preferences/EmailPreferencesServiceTest.java` — asserts default false for an untouched account, on→off→on round-trip, every enum constant present in `list`, and unknown type rejected

### Group B — Account UI

- [X] T011 [P] [US1] Create `front-end/src/lib/emailPreferences.ts` exporting `getEmailPreferences()` and `setEmailPreference(type, enabled)` — modelled on `front-end/src/lib/notifications.ts`, using `credentials: "include"` and the shared `ApiError` shape; all fetch logic lives here and never inline in the component (Constitution III)
- [X] T012 [P] [US1] Add `account.emailPrefs.*` keys to all three of `front-end/src/i18n/locales/es.ts`, `ro.ts` and `en.ts` — section heading, a label and helper line for `NEW_HOMEWORK_ASSIGNED`, and a save-error string; keep the three files in sync so no locale renders a raw key
- [X] T013 [US1] Create `front-end/src/components/account/EmailPreferencesSection.tsx` — a named component (Constitution II) closely following the existing `front-end/src/components/account/TimezoneSetting.tsx`: no props, `useTranslation`, local `saving` state, built from the existing `@/components/ui/switch` and `@/components/ui/label` primitives. Renders **one toggle per entry returned by the server**, looking each label up by `type` key, so a future option needs no layout change ([R6](./research.md))
- [X] T014 [US1] Render `<EmailPreferencesSection />` inside `ProfileView` in `front-end/src/routes/cuenta.tsx`, after `<TimezoneSetting />` and before the logout `Button` (`cuenta.tsx:487`) — unlike `InterestsSetting` it is **not** wrapped in a `STUDENT`/`ADMIN` role check, since any authenticated account may set the preference ([R5](./research.md))

### Group C — Email pipeline

- [X] T015 [US1] Change the `INSERT` in `HomeworkTargetRepository.addTargets(...)` (`back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkTargetRepository.java:73`) to `ON CONFLICT (assignment_id, user_id) DO NOTHING RETURNING user_id` and return the inserted user IDs. Existing callers that ignore the return value must behave **exactly** as before (FR-014); `replaceTargets` delegates here and inherits it for free ([data-model.md](./data-model.md) §3)
- [X] T016 [P] [US1] Create `back-end/src/main/java/com/kuky/backend/learning/service/NewHomeworkGrants.java` — a transient per-action accumulator wrapping a `LinkedHashMap<UUID, List<UUID>>` (student → newly granted assignment ids), insertion-ordered so emails are deterministic, with `add(...)`, `isEmpty()` and `byStudent()` ([data-model.md](./data-model.md) §4)
- [X] T017 [US1] Add `findRecipientsFor(Collection<UUID> userIds)` to `EmailPreferencesRepository` returning `(userId, email, firstName)` filtered by `email_on_homework_assigned = true AND status = 'ACTIVE'` — **one** query, not `findById` in a loop; the `status` predicate implements "an account awaiting activation is not emailed" ([data-model.md](./data-model.md) §5)
- [X] T018 [US1] Create `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkAssignmentEmailService.java` with `notifyNewlyAssigned(NewHomeworkGrants)` — returns immediately when empty; otherwise resolves recipients via T017, reads titles from `homework_assignments.title`, and sends **one** `SimpleMailMessage` per recipient (never CC/BCC) in Spanish matching `EmailService`'s house style (`"… — Destino: Español"`, closing `"Saludos,\nDestino: Español"`), listing each new homework and linking to `${app.frontend.base-url}/aprendizaje`. Each recipient's send is individually wrapped in try/catch logging at WARN and swallowing, mirroring `BookingEmailService.sendQuietly` ([R4](./research.md), [R8](./research.md))
- [X] T019 [US1] Wire `back-end/src/main/java/com/kuky/backend/units/service/UnitService.java` — build one `NewHomeworkGrants` per call in **both** `setAssignees` (`:184`, FR-007a) and `setHomeworks` (`:145`, FR-007b), collecting what `addTargets` reports across the loop, and flush it **once** as the last statement of each method. Flushing inside the loop would send one email per homework and break FR-006
- [X] T020 [P] [US1] Wire `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` the same way in **both** `create` (`:538`) and `setAssignees`, where `replaceTargets` reports the newly inserted students (FR-007c)
  - 📝 Correction: this task originally named `update` as the second site. `update` does not touch `homework_targets` at all — the direct-assign path is `setAssignees`. Wired to the real method; `data-model.md`, `research.md` and `plan.md` corrected to match.
- [X] T021 [P] [US1] Create `back-end/src/test/java/com/kuky/backend/learning/HomeworkAssignmentEmailServiceTest.java` with a mocked `JavaMailSender`, following the existing `EmailServiceTest` shape — asserts exactly one message per student for a multi-homework action, all granted titles present in the body, one recipient per message with no CC/BCC, nothing sent when the flag is off, nothing sent for a `PENDING` account, and that a `JavaMailSender` throwing for one recipient neither propagates nor prevents the other recipients' sends

### Validation

- [X] T022 [US1] Run quickstart scenarios **S1, S2, S4 and S7** from [quickstart.md](./quickstart.md) against Mailpit. S2 is the core assertion (one email, three titles, not three emails); **S7 matters most** — re-saving an unchanged assignment must send nothing, and its automated counterpart is CI-only (T029), so this is the only place it gets checked before merge
  - Run against the real local stack (Postgres + back-end + front-end), using two throwaway accounts that were deleted afterwards. With Mailpit absent, each attempted send surfaces as one `HomeworkAssignmentEmailService` WARN, which makes send **counts** directly observable:
    - **S1** ✅ section renders at `/cuenta`, toggle `unchecked → checked`, column flips to `t`, survives a full reload; all API calls 200 including CORS preflight.
    - **S4** ✅ direct assign → exactly 1 send (22:37:34).
    - **S7** ✅ re-saving the unchanged assignee list → **0** sends. This is the FR-013 guarantee, confirmed against a real database.
    - **S2** ✅ unit containing **2** homeworks assigned to one student → exactly **1** send (22:38:27), not 2. This is the discriminator for the T019 flush-placement risk.
    - Also verified beyond the task: FR-007b (homework added to a held unit → 1 send, 22:38:42) and FR-008 (opted-out student assigned homework → 0 sends).
  - ⚠️ Not covered here: the rendered message body in an inbox. Body content (titles listed, both links, greeting) is asserted by `HomeworkAssignmentEmailServiceTest`.

**Checkpoint**: A student can opt in and receives one correct email per teacher action, from all three grant routes. This is the MVP — deployable on its own.

---

## Phase 4: User Story 2 — Nobody is emailed unless they asked to be (Priority: P1)

**Goal**: Every account starts off and stays off until its owner says otherwise, and nothing the site already emails changes.

**Independent Test**: Without touching any preference, assign homework to several existing students and one freshly registered account; confirm the work appears on the site and Mailpit stays empty.

**Note**: Mostly verification rather than new machinery — the guarantee is delivered by T003's DDL and T017's filter. These tasks exist because "we didn't write the bug" is not evidence; the audits below are the evidence.

- [X] T023 [US2] Audit that `email_on_homework_assigned` is written **only** by `EmailPreferencesRepository`'s setter: `grep -rn "email_on_homework_assigned" back-end/src/` must show the migration, that setter, and `findRecipientsFor`'s read — and nothing else. Any admin controller, service, or bootstrap touching it violates FR-015
- [X] T024 [P] [US2] Confirm the role paths leave it alone — `StudentAdminController`'s grant/revoke and `AdminBootstrap` must neither read nor reset the column, so a revoked-then-restored student keeps their setting ([data-model.md](./data-model.md) R1.3)
- [X] T025 [P] [US2] Extend `back-end/src/test/java/com/kuky/backend/preferences/EmailPreferencesServiceTest.java` (or add a repository test) asserting `findRecipientsFor` returns nothing for an opted-out account and nothing for an `ACTIVE`-but-opted-out **and** an opted-in-but-`PENDING` account
- [X] T026 [US2] Run quickstart **S0, S5 and S10**
  - **S0** ✅ against the real dev database: column is `boolean NOT NULL DEFAULT false`, and all 7 pre-existing accounts read `f` after the migration — no backfill touched anyone.
  - **S5** ✅ opted-out student assigned homework → 201, work visible, **0** sends.
  - **S10** ⚠️ partial: the registration/activation email was observed still firing unchanged through the untouched `EmailService` during this run. Booking confirmation/cancellation/reminder emails were **not** exercised — they share no code with this feature (no edit to `EmailService` or `BookingEmailService`), but they were not run. Covered by T034.
  - Original task text follows. — S0 proves the migration left every row `false` and that assigning with nobody opted in sends nothing; S10 walks the other six emails (activation, password reset, booking confirmation, booking cancellation, student-access grant) and the in-site unseen dots, confirming FR-014

**Checkpoint**: Default-off is proven at the database, in the send path, and against the pre-existing email surface.

---

## Phase 5: User Story 3 — Student turns homework emails back off (Priority: P2)

**Goal**: Opting out works and is discoverable from the email itself.

**Independent Test**: With the option on and an email already received, switch it off, assign more homework, confirm nothing further arrives; and confirm a received email links to the preferences page.

**Depends on US1** — you cannot verify that "off" stops emails without the send path from Phase 3.

- [X] T027 [US3] Add the FR-010 lines to the body composed in `HomeworkAssignmentEmailService` (T018): a sentence saying the email was received because the student asked for it, and a link to `${app.frontend.base-url}/cuenta` for changing or stopping it. Additive to the T018 body — the email is already valid without it, which is what keeps this story independently deliverable
- [X] T028 [US3] Extend `HomeworkAssignmentEmailServiceTest` to assert both links are present in every message — the work link (`/aprendizaje`) and the preferences link (`/cuenta`) — and run quickstart **S8** (switch off at `/cuenta`, assign, confirm silence, reload and confirm it is still off)

**Checkpoint**: All three stories work independently.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T029 [P] Create `back-end/src/test/java/com/kuky/backend/learning/NewHomeworkGrantIntegrationTest.java` extending `AbstractIntegrationTest` (GitHub Actions only, by the existing `GitHubActionsOnly` condition) — proves at the database level that `RETURNING` reports an insert exactly once for a new `(assignment_id, user_id)` and reports none on re-insert. This is the automated form of S7 and the regression guard for FR-013
- [X] T030 Run quickstart **S9** — ✅ verified for real, and unplanned: because Mailpit was absent the whole run *was* S9. Every assignment returned 201 with the assignee attached, the student saw the work, the `homework_targets` row persisted, and the failure appeared as a single WARN that never escaped the request. FR-012 confirmed under genuine SMTP failure rather than simulated. Original task text follows.
- [X] T030b Original S9 procedure — stop Mailpit with `app.mail.enabled` still `true`, assign homework, and confirm the assignment succeeds with no error surfaced to the teacher, the student sees the work, the `homework_targets` row exists, and the log shows a WARN rather than a stack trace escaping the request (FR-012). Easy to assume and rarely actually tested
- [X] T031 [P] Add a **Key implementation notes → Back-end** bullet to `CLAUDE.md` covering: the preference column and its default-off guarantee, that all three grant routes email alike, that grouping is per teacher action, and the deliberate divergence from the in-site dots for homework added to an already-assigned unit
- [X] T032 [P] Add `/cuenta` email preferences to the **Current pages** table row in `CLAUDE.md`
- [X] T033 Run `cd front-end && npm run lint && npm run format` and `cd back-end && ./gradlew build`
- [ ] T034 Full pass of [quickstart.md](./quickstart.md) S0–S10 against a clean Mailpit inbox, ticking the validation checklist at its foot
  - ⛔ **The one task left open.** Mailpit is not installed on this machine, so no scenario's *inbox contents* were inspected. Every behaviour S0–S10 asserts was verified by other means (send counts in the log, database state, DOM state, unit tests) and all passed — but the following still want a human eye once Mailpit is running:
    - the rendered Spanish subject and body, and that both links resolve;
    - S6 (two recipients, genuinely different bodies) — asserted in unit tests, never seen as two real messages;
    - S10's booking emails.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: needs Setup — **blocks every user story**
- **US1 (Phase 3)**: needs Foundational
- **US2 (Phase 4)**: needs Foundational; its send-path assertions (T026's S5) also need T017–T021 from US1
- **US3 (Phase 5)**: needs US1 — verifying "off stops it" requires "on starts it"
- **Polish (Phase 6)**: needs all desired stories

### Within User Story 1

```
T015 ─┐
T016 ─┼─→ T018 ─→ T019, T020 ─→ T022
T017 ─┘              ↑
T007 → T008 → T009 ──┘ (T009 needed to opt in for T022)
T011, T012 → T013 → T014
```

- T008 needs T005 (repository) and T007 (DTOs)
- T009 needs T008
- T013 needs T011 (fetch layer) and T012 (locale keys)
- T014 needs T013
- T017 needs T005
- T018 needs T016 and T017
- T019 and T020 need T015 and T018
- T022 needs the whole phase — it is the end-to-end check

### Parallel Opportunities

- **Phase 1**: T002 alongside T001
- **Phase 2**: T004 and T006 in parallel; T005 after T004 (needs the enum)
- **Phase 3**: Groups A+B (preferences) and Group C (email pipeline) touch disjoint files — two developers can run them concurrently and meet at T022. Within them: T007/T010 parallel; T011/T012 parallel; T016 parallel with T015/T017; T020 parallel with T019 (different files); T021 parallel with T019/T020
- **Phase 4**: T024 and T025 in parallel after T023
- **Phase 6**: T029, T031 and T032 all in parallel

### Same-file constraints (do **not** parallelise)

- T019 covers both `UnitService` methods in one task — deliberately, since splitting them would put two tasks in one file
- T020 covers both `HomeworkAdminService` methods for the same reason
- T027 and T028 both edit files T018/T021 created, so they follow Phase 3

---

## Parallel Example: User Story 1

```bash
# Two developers, after Phase 2:

# Developer A — preferences (API + UI)
Task: "T007 DTOs in back-end/.../preferences/dto/"
Task: "T011 front-end/src/lib/emailPreferences.ts"
Task: "T012 account.emailPrefs.* keys in es.ts / ro.ts / en.ts"
# then T008 → T009 → T013 → T014

# Developer B — email pipeline
Task: "T015 addTargets RETURNING in HomeworkTargetRepository.java"
Task: "T016 NewHomeworkGrants.java"
Task: "T017 findRecipientsFor in EmailPreferencesRepository.java"
# then T018 → T019 ‖ T020

# Meet at T022 (end-to-end quickstart run)
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 Setup → Phase 2 Foundational
2. Phase 3 User Story 1
3. **STOP and VALIDATE** — quickstart S1, S2, S4, S7
4. Deployable: students can opt in and receive correct, correctly-grouped emails

### Incremental Delivery

1. Setup + Foundational → the column exists, everyone is off
2. + US1 → opt in and receive (**MVP**)
3. + US2 → the default-off and no-regression guarantee is *proven*, not assumed
4. + US3 → opting out works and is discoverable from the email
5. + Polish → CI regression guard, failure-isolation check, docs

---

## Notes

- **The single highest-risk task is T019.** Flushing the accumulator inside the loop instead of after it produces one email per homework — the exact failure FR-006 exists to prevent, and it looks correct in code review. S2 is the scenario that catches it.
- **T023 is an audit, not a code change**, and it is the only thing standing between a refactor and silently mailing the whole student list. Do not skip it because nothing needs editing.
- **S3's missing in-site dot is correct.** Homework added to an already-assigned unit emails but shows no dot — your Q2 answer diverging from `specs/041-notification-system/`. Do not "fix" it.
- Tests: unit tests run locally via `./gradlew test`; the integration test (T029) is skipped locally by design and only executes on GitHub Actions.
- `[P]` = different files, no dependency on an incomplete task.
- Commit after each task or logical group.
