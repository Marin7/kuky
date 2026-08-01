---
description: "Task list for university student portal implementation"
---

# Tasks: University Student Portal

**Input**: Design documents from `specs/023-university-portal/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/university-api.md](contracts/university-api.md), [quickstart.md](quickstart.md)

**Tests**: Backend JUnit tasks included where they lock security/role and public vs gated access (repo convention). Frontend has no test framework — verify via [quickstart.md](quickstart.md) in a browser on the university host.

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) so each story can be implemented and tested independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: US1…US6 maps to spec user stories
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Scaffold packages, config knobs, and empty university frontend surface.

- [X] T001 Create backend package directories under `back-end/src/main/java/com/kuky/backend/university/` (`dto`, `repository`, `service`, `controller`) and `back-end/src/test/java/com/kuky/backend/university/` per [plan.md](plan.md)
- [X] T002 [P] Create frontend directories `front-end/src/components/university/`, `front-end/src/components/admin/university/`, and `front-end/src/lib/university.ts` (export placeholder types/helpers)
- [X] T003 [P] Add config placeholders in `back-end/src/main/resources/application.yaml` and `application-local.yaml` for multi-origin CORS list and optional `app.cookie.domain` (empty locally) per [contracts/university-api.md](contracts/university-api.md) ops section; document env vars in comments matching existing style

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, role model, shared auth/CORS/cookie, SecurityConfig stubs, and host-aware shell — required before any user story.

**⚠️ CRITICAL**: No user story work begins until this phase is complete.

- [X] T004 Write `back-end/src/main/resources/db/migration/V4__university_portal.sql` per [data-model.md](data-model.md): widen `users.role` CHECK to include `UNIVERSITY_STUDENT`; add `users.university_level` with CHECKs; create `university_schedule_sessions`, `university_schedule_exceptions`, `university_exam_dates`, `university_news_items`, `university_homework_availability`, `university_presentation_availability`
- [X] T005 [P] In `back-end/src/main/java/com/kuky/backend/auth/model/User.java`, add `universityLevel` field with getters/setters
- [X] T006 [P] In `back-end/src/main/java/com/kuky/backend/auth/dto/UserResponse.java`, add `universityLevel` and document `role` may be `UNIVERSITY_STUDENT`
- [X] T007 In `back-end/src/main/java/com/kuky/backend/auth/repository/UserRepository.java`, map `university_level` in the user row mapper; add helpers to set role+level and clear level (used by grant/revoke). Depends on T004, T005
- [X] T008 In `back-end/src/main/java/com/kuky/backend/auth/service/AuthService.java`, map `universityLevel` into `UserResponse` from `User` (null unless university role). Depends on T006, T007
- [X] T009 [P] In `front-end/src/lib/auth.ts`, extend `UserResponse.role` union with `"UNIVERSITY_STUDENT"` and add `universityLevel: "BEGINNER" | "INTERMEDIATE" | null`
- [X] T010 Update `back-end/src/main/java/com/kuky/backend/config/CorsConfig.java` to accept multiple allowed origins (comma-separated property or list) including private + university front origins
- [X] T011 Update auth cookie creation (login/logout/`JwtCookieAuthenticationFilter` refresh) to set `Domain` from `app.cookie.domain` when non-empty, keeping host-only when empty for local
- [X] T012 Update `back-end/src/main/java/com/kuky/backend/config/SecurityConfig.java`: `permitAll` for `GET /api/v1/university/schedule`, `/exams`, `/news`; reserve `/api/v1/university/learning/**` for `hasAnyRole("UNIVERSITY_STUDENT","ADMIN")`; keep existing `STUDENT` matchers **without** adding `UNIVERSITY_STUDENT`; leave `/api/v1/admin/university/**` under existing `/admin/**` ADMIN rule
- [X] T013 [P] Add `ROLE_CONFLICT` (and any new university error codes needed later) mapping in `back-end/src/main/java/com/kuky/backend/config/GlobalExceptionHandler.java` following existing `{"error","message"}` style
- [X] T014 Implement host-aware shell selection in `front-end/src/routes/__root.tsx` (or equivalent layout): detect university host via env/runtime hostname; render distinct university nav shell vs private shell (university nav: schedule, exams, news, learning, account — **no** 1-1 booking). Wire a minimal university home route placeholder under a university route group

**Checkpoint**: Migration applies; `/auth/me` can represent `UNIVERSITY_STUDENT`; CORS/cookie ready for two origins; university shell distinguishable; SecurityConfig stubs in place.

---

## Phase 3: User Story 1 — Teacher grants university-student access (Priority: P1) 🎯 MVP

**Goal**: Teacher can grant/revoke `UNIVERSITY_STUDENT` with a required level; mutual exclusion with `STUDENT`; no email; gated learning blocked until granted; public informative readable.

**Independent Test**: Register on university entry → materials/homework blocked → admin grants with level → learning unlocks for that level; grant while `STUDENT` rejected; no grant/revoke email in Mailpit.

### Implementation for User Story 1

- [X] T015 [US1] Implement grant/revoke/change-level service methods (no `EmailService` calls) with mutual-exclusion checks against `STUDENT`/`ADMIN` in `back-end/src/main/java/com/kuky/backend/admin/service/` (extend existing student admin service or add `UniversityStudentAdminService.java`)
- [X] T016 [US1] Add admin endpoints in `back-end/src/main/java/com/kuky/backend/admin/controller/StudentAdminController.java` (or dedicated controller): `GET /admin/university/students`, `POST /admin/users/{id}/university-student`, `DELETE /admin/users/{id}/university-student`, `PUT /admin/users/{id}/university-level` per [contracts/university-api.md](contracts/university-api.md)
- [X] T017 [US1] Extend existing `POST/DELETE /admin/users/{id}/student` guards to reject `UNIVERSITY_STUDENT` targets with `409 ROLE_CONFLICT` in the same admin controller/service
- [X] T018 [P] [US1] Add backend tests for grant/revoke, mutual exclusion, and no-email behavior under `back-end/src/test/java/com/kuky/backend/admin/` (and/or university package)
- [X] T019 [P] [US1] Add client helpers in `front-end/src/lib/admin.ts` for university roster grant/revoke/level APIs
- [X] T020 [US1] Build admin UI to list university students and grant/revoke/set level (new tab or section under `front-end/src/routes/panel.tsx` + `front-end/src/components/admin/university/`) reusing Users/Students patterns; ensure grant requires level
- [X] T021 [US1] On university shell, gate materials/homework routes: require `role === "UNIVERSITY_STUDENT" || role === "ADMIN"`; show clear “ask the teacher” notice for others (new `front-end/src/components/university/` notice component). Schedule/exams/news remain reachable without university role
- [X] T022 [US1] Ensure university-entry registration/login reuse existing account flows (`front-end` university routes → `/cuenta` equivalent or shared auth components) without implying university or private student status

**Checkpoint**: US1 MVP — teacher-controlled university access works; mutual exclusion enforced; learning gated; informative still open.

---

## Phase 4: User Story 2 — Level-based weekly schedule (Priority: P1)

**Goal**: Public schedule from weekly template + dated exceptions; anonymous/full labeled view; university students see only their level.

**Independent Test**: Seed 5 beginner + 2 intermediate sessions + one exception; anonymous sees all labeled; beginner university student sees only beginner (+ exception).

### Implementation for User Story 2

- [X] T023 [P] [US2] Create JDBC repositories for `university_schedule_sessions` and `university_schedule_exceptions` under `back-end/src/main/java/com/kuky/backend/university/repository/`
- [X] T024 [US2] Implement `UniversityScheduleService` merging template + exceptions and choosing `FULL_LABELED` vs `LEVEL_FILTERED` viewer mode per [contracts/university-api.md](contracts/university-api.md)
- [X] T025 [US2] Implement public `GET /api/v1/university/schedule` in `back-end/src/main/java/com/kuky/backend/university/controller/` (optional `from`/`to` query)
- [X] T026 [US2] Implement admin CRUD endpoints for sessions and exceptions under `/api/v1/admin/university/schedule/…`
- [X] T027 [P] [US2] Add backend tests for merge rules (cancel wins; extra appears; level filter) under `back-end/src/test/java/com/kuky/backend/university/`
- [X] T028 [P] [US2] Add `getUniversitySchedule` (and admin schedule helpers) in `front-end/src/lib/university.ts` / `admin.ts`
- [X] T029 [US2] Build university schedule page UI in `front-end/src/components/university/` + university route; show labeled full timetable for guests / non-university; filter for `UNIVERSITY_STUDENT`
- [X] T030 [US2] Build admin schedule editor UI in `front-end/src/components/admin/university/` (template sessions by level + exceptions)

**Checkpoint**: US2 — public and level-filtered schedule work with admin authoring.

---

## Phase 5: User Story 3 — Exam dates and news (Priority: P2)

**Goal**: Public cohort-wide exam dates and news; teacher CRUD with published flag.

**Independent Test**: Publish exam + news as admin; readable without login on university entry; unpublished hidden.

### Implementation for User Story 3

- [X] T031 [P] [US3] Create repositories/DTOs for `university_exam_dates` and `university_news_items` under `back-end/src/main/java/com/kuky/backend/university/`
- [X] T032 [US3] Implement public `GET /api/v1/university/exams` and `GET /api/v1/university/news` (published only; news newest first)
- [X] T033 [US3] Implement admin CRUD under `/api/v1/admin/university/exams` and `/api/v1/admin/university/news` including publish/unpublish
- [X] T034 [P] [US3] Add backend tests for public published-only filtering under `back-end/src/test/java/com/kuky/backend/university/`
- [X] T035 [P] [US3] Add frontend API helpers in `front-end/src/lib/university.ts` and `admin.ts`
- [X] T036 [US3] Build university exams + news pages in `front-end/src/components/university/` + routes (public, no auth gate)
- [X] T037 [US3] Build admin exams + news editors in `front-end/src/components/admin/university/`

**Checkpoint**: US3 — informative exams/news live publicly with admin control.

---

## Phase 6: User Story 4 — Class materials and homework (Priority: P2)

**Goal**: Shared catalog availability by university level; university students view presentations and complete MANUAL/EXERCISE homework via reused learning pipelines.

**Independent Test**: Make an existing 1-1 presentation + homework available for BEGINNER; beginner university student completes homework; intermediate does not see those items as theirs.

### Implementation for User Story 4

- [X] T038 [P] [US4] Create repositories for `university_homework_availability` and `university_presentation_availability` under `back-end/src/main/java/com/kuky/backend/university/repository/`
- [X] T039 [US4] Implement availability admin replace/list service + `PUT/GET /api/v1/admin/university/levels/{level}/homeworks` and `…/presentations` per contract
- [X] T040 [US4] Implement `GET /api/v1/university/learning` overview listing available presentations/homework for the caller’s `universityLevel` (ADMIN: require `level` query) in university learning controller/service
- [X] T041 [US4] Wire university homework take/submit and presentation file download endpoints under `/api/v1/university/learning/**` that **delegate** to existing learning/grading services after availability checks (do not require `homework_targets` / private shares). Touch `back-end/src/main/java/com/kuky/backend/learning/` only as needed for shared access helpers
- [X] T042 [P] [US4] Add backend tests: availability grant, university submit MANUAL/EXERCISE, 404 when not available, `STUDENT` still cannot call university learning without university role
- [X] T043 [P] [US4] Add frontend helpers in `front-end/src/lib/university.ts` for learning overview + submit/download
- [X] T044 [US4] Build university learning UI (materials list + homework list/take/submit) in `front-end/src/components/university/` reusing private learning UX patterns where practical; keep gated behind university role (T021)
- [X] T045 [US4] Build admin availability UI to attach existing homeworks/presentations to BEGINNER/INTERMEDIATE in `front-end/src/components/admin/university/`

**Checkpoint**: US4 — shared catalog usable for university cohort by level.

---

## Phase 7: User Story 5 — Teacher manages university portal content (Priority: P2)

**Goal**: Cohesive admin experience covering roster, schedule, exams, news, and availability; level changes reflected for students.

**Independent Test**: From `/panel` university admin area alone, create sessions, exception, exam, news, and availability for both levels; verify student/public views update.

### Implementation for User Story 5

- [X] T046 [US5] Consolidate university admin sections into a clear `/panel` University tab structure in `front-end/src/routes/panel.tsx` (roster, schedule, exams, news, content availability) with i18n labels in `front-end/src/i18n/locales/{es,en,ro}.ts`
- [X] T047 [US5] Ensure change-level from admin immediately affects schedule filter and learning lists on next student fetch (verify end-to-end; fix caching/stale client state if any)
- [X] T048 [US5] Add empty states and validation messages in admin university UIs (required level on grant, exception CANCEL vs EXTRA fields, publish toggles)
- [X] T049 [P] [US5] Smoke-test admin APIs coverage gaps with additional backend tests if any CRUD path lacks assertions under `back-end/src/test/java/com/kuky/backend/`

**Checkpoint**: US5 — teacher can operate the full university content surface from admin.

---

## Phase 8: User Story 6 — Separation from the private 1-1 experience (Priority: P3)

**Goal**: Distinct university entry/nav; shared accounts; university role ≠ private STUDENT privileges; private site unchanged for STUDENT.

**Independent Test**: University host has no booking nav; register/login on university host; university-only user blocked from private aprendizaje/booking actions; private STUDENT blocked from university learning until swapped via revoke+grant rules.

### Implementation for User Story 6

- [X] T050 [US6] Finalize university vs private route groups and nav so university shell never links to `/reservas` booking or private `/aprendizaje`; private shell unchanged
- [X] T051 [US6] Verify `SecurityConfig` private matchers still exclude `UNIVERSITY_STUDENT` from `POST /bookings` and `/api/v1/learning/**`; add regression test in `back-end/src/test/java/com/kuky/backend/config/`
- [X] T052 [US6] Confirm university learning matchers reject `STUDENT`/`USER`; add regression test alongside T051
- [X] T053 [US6] Document local two-host setup (hosts file / `uni.localhost`) and prod env (`CORS_ALLOWED_ORIGINS`, `COOKIE_DOMAIN`, university hostname) in `specs/023-university-portal/quickstart.md` if anything drifted during implementation
- [X] T054 [US6] Browser pass: same account login on both entries; role-appropriate access only (per quickstart scenarios 2, 6)

**Checkpoint**: US6 — clear product separation with shared auth.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: i18n, quickstart validation, cleanup.

- [X] T055 [P] Complete university student + admin copy in `front-end/src/i18n/locales/{es,en,ro}.ts` (notices, nav, empty states, errors)
- [X] T056 Run full [quickstart.md](quickstart.md) validation checklist in browser (public informative, grant/revoke, schedule+exception, availability homework, mutual exclusion, revoke)
- [X] T057 [P] Run `./gradlew test` in `back-end/` and fix regressions from role CHECK / SecurityConfig changes
- [X] T058 Remove temporary placeholders/dead routes from university scaffold; ensure no private booking CTAs remain on university pages

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup** → **Phase 2 Foundational** (blocks all stories)
- **US1 (Phase 3)** → first deliverable MVP after foundation
- **US2–US4** can proceed after foundation; **US2** does not require US1 for anonymous schedule, but level-filtered student view needs US1 grant
- **US3** independent of US2 after foundation
- **US4** needs US1 for a real university student actor; availability admin can be built in parallel
- **US5** depends on US1–US4 admin pieces existing (consolidation)
- **US6** after shells + SecurityConfig story work (best after US1 + foundation host work)
- **Polish** after desired stories complete

### User Story Dependencies

| Story | Depends on | Independently testable? |
|-------|------------|-------------------------|
| US1 | Foundation | Yes — grant/revoke + gates |
| US2 | Foundation (+ US1 for filtered student view) | Yes — public schedule without US1 |
| US3 | Foundation | Yes |
| US4 | Foundation + US1 (student actor) | Yes with granted test user |
| US5 | US1–US4 admin surfaces | Yes as consolidation pass |
| US6 | Foundation + US1 gates | Yes as separation/regression pass |

### Parallel Opportunities

- T002/T003 (setup); T005/T006/T009; T010 vs T013; T023/T031/T038 repositories across stories once foundation lands
- After foundation: US2 public schedule and US3 exams/news can proceed in parallel with US1 admin roster
- T018/T027/T034/T042 tests parallel within their stories

---

## Parallel Example: After Foundation

```bash
# Developer A — US1 roster + gates
Task: T015–T022 university grant/revoke + admin UI + FE gate

# Developer B — US2 schedule
Task: T023–T030 schedule repos/API/UI

# Developer C — US3 exams/news
Task: T031–T037 exams/news repos/API/UI
```

---

## Implementation Strategy

### MVP First (US1 only)

1. Phase 1 Setup + Phase 2 Foundational  
2. Phase 3 US1 (grant/revoke, gates, admin roster, university registration)  
3. **STOP** — validate US1 independent test  
4. Demo teacher-controlled university access on the university host  

### Incremental Delivery

1. Foundation → US1 (MVP)  
2. US2 schedule → demo informative timetable  
3. US3 exams/news → demo public info hub  
4. US4 materials/homework → demo learning  
5. US5 admin polish → US6 separation hardening → Polish/quickstart  

### Suggested MVP scope

**US1 only** (plus Foundational): university role, mutual exclusion, no email, gated learning notice, public informative routes reachable, admin grant/revoke with level.

---

## Notes

- Do **not** email on university grant/revoke (unlike private STUDENT).  
- Do **not** add `UNIVERSITY_STUDENT` to private `STUDENT` SecurityConfig matchers.  
- Prefer availability joins over bulk `homework_targets`.  
- Commit after each task or logical group; validate at story checkpoints.  
- Format: every task uses `- [ ]`, Task ID, optional `[P]`, story label for US phases, and a concrete file path.
