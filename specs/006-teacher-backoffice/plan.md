# Implementation Plan: Teacher Backoffice — Control Panel

**Branch**: `006-teacher-backoffice` | **Date**: 2026-06-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-teacher-backoffice/spec.md`

## Summary

Build a private, **admin-only** control panel at route `/panel` ("Panel de control") where
Paula — the single teacher/administrator — manages everything students consume as read-only:
her **availability** (which feeds the public `/reservas` schedule), the **homework** she
authors and assigns to specific students (which surfaces in each student's `/aprendizaje`
space), and lightweight **slide presentations** she builds and shares with specific students.
The panel and its `/api/v1/admin/**` API are invisible and inaccessible to students and guests.

Per the clarifications, this iteration adds a real authorization tier and three real backend
domains rather than mocks:

1. **Admin identity** — a persisted `users.role` (`STUDENT|ADMIN`), carried as a JWT claim and
   enforced by `SecurityConfig` (`hasRole("ADMIN")` on `/api/v1/admin/**`). A startup
   `AdminBootstrap` promotes the account matching `app.scheduling.teacher-email`. `/me` (and
   login/register/reset) now return `role` so the frontend can gate nav and the route.
2. **Availability** — two new tables (`availability_rules` weekly pattern +
   `availability_exceptions` date BLOCK/OPEN) replace the hard-coded working-hours/lunch/weekend
   constants as the source of public slots. `AvailabilityService` is refactored to derive
   virtual slots from these tables while **retaining** the existing min-lead-time, no-past, and
   confirmed-booking-overlay safeguards. Seeded to reproduce today's schedule exactly. Saving
   availability never mutates bookings; the save response reports confirmed bookings now outside
   availability as a non-blocking warning (FR-010).
3. **Homework targeting** — a `homework_targets` join table makes assignments per-student.
   `LearningService` switches to "visible iff a target row links the caller", retiring the
   shared-to-all model (the `V5` placeholder assignments become unshared drafts). The `V5`
   `homework_assignments` / `homework_submissions` tables and the student submit flow are
   otherwise unchanged.
4. **Presentations** — new `presentations` / `presentation_slides` / `presentation_shares` plus
   an `images` table (BYTEA, served via `GET /api/v1/images/{id}`). Decks are heading/text/
   optional-image slides only (no animations/transitions/charts), shared per student. These are
   distinct from the existing 005 `learning_presentation` intro, which is left untouched.

The backend gains an `admin` package (controllers, DTOs, services) plus an `availability`
extension of the `scheduling` domain, a `presentations` package, and image handling, all
mirroring the established `auth`/`scheduling`/`resources`/`learning` conventions (plain JDBC
with `NamedParameterJdbcTemplate` + `RowMapper`, POJO models, `record` DTOs, current user via
`@AuthenticationPrincipal String email`, `{error,message}` errors). The frontend adds a
`/panel` route (admin-gated, redirect non-admins), a `components/admin/` folder, a `lib/admin.ts`
client, and a shared-presentation viewer in `components/learning/`. `SiteHeader` becomes
role-aware so the "Panel" entry renders only for the admin. No new third-party dependencies.

## Technical Context

**Language/Version**: TypeScript 5.x strict (frontend) / Java 26 (backend, per `back-end/build.gradle`).

**Primary Dependencies**:
- Frontend: React 19, TanStack Start 1.x (SSR), TanStack Router (file-based), TanStack Query,
  TailwindCSS 4, Shadcn UI, Vite 7. **No new dependencies** — dates via `Intl.DateTimeFormat`
  `"es"` locale; slide reordering via up/down buttons or HTML5 drag (no library).
- Backend: Spring Boot 4.0.x (`-web`, `-security`, `-jdbc`, `-validation`), Flyway, PostgreSQL
  driver, jjwt. Multipart upload via the built-in `MultipartFile`. **No new dependencies**.

**Storage**: PostgreSQL 18. New migrations `V6` (user role), `V7` (availability), `V8`
(homework targets), `V9` (presentations + images). Image bytes stored as `BYTEA` (see research
§3). Reuses `users`, `bookings`, `homework_assignments`, `homework_submissions`.

**Testing**:
- Backend: JUnit 5 + Spring Boot Test. Unit tests for the refactored `AvailabilityService`
  (weekly derivation, BLOCK/OPEN exceptions, weekend = no rules, lunch-gap absence, lead/past/
  booked overlay, booking-conflict detection), `HomeworkAdminService` (create/edit/delete,
  assignee replacement, submission read), `PresentationService` (slide CRUD, reorder permutation
  validation, share gate), and `ImageService` (type/size validation). Slice/integration tests for
  admin authorization (401 guest / 403 student / 200 admin) and the per-student visibility of
  homework and shared decks.
- Frontend: visual verification in a running browser per the constitution (no test framework in repo).

**Target Platform**: Browser via TanStack Start SSR (frontend) + JVM server on `:8081` (backend).

**Performance Goals**: Paula changes availability and a student sees it on next schedule load
(SC-001/002); create-and-assign homework < 3 min (SC-003); build/preview/share a ≥5-slide deck
in one session (SC-005). All datasets are small (one teacher, low-hundreds students); every
path is well within standard request latencies. Slot generation reads a handful of rule/
exception rows per request.

**Constraints**:
- The panel is fully private: nav hidden from non-admins (FR-002), `/panel` redirects
  non-admins (FR-003), and `/api/v1/admin/**` is `hasRole("ADMIN")` (401 guest / 403 student).
- Availability changes take effect without redeploy (FR-007/008); confirmed bookings always
  survive availability edits (FR-010/SC-008).
- Homework and presentations are visible only to assigned/shared students (FR-014/024/SC-004).
- Presentation builder is deliberately minimal (FR-026): ordered heading/text/one-image slides.
- Images are teacher-uploaded, type- and size-bounded (≤ 2 MB, jpeg/png/webp) (FR-020/028).
- All copy Spanish; dates in `"es"` locale (FR-027). All state persists server-side (FR-030).

**Scale/Scope**: One teacher, dozens–low hundreds of students; a handful of availability
windows, a modest number of homework items and decks; single-instance backend. ~4 migrations,
~3 new/extended backend packages, one new frontend route with ~15 components, one admin API
client plus small learning-client additions.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I — Simplicity First | ✅ PASS | Reuses the virtual-slot generator (only the availability predicate changes), the existing homework tables/flow (adds one join table), and the JDBC/Flyway stack. Images stored in Postgres BYTEA to avoid object-storage infra until a real need exists; the `GET /images/{id}` seam keeps that swappable. No JPA, no new dependencies, no realtime presentation machinery. `REVIEWED` stays reserved. |
| II — Component-Driven UI | ✅ PASS | Panel, availability editor, homework editor + assignee picker, presentation editor, slide editor, full-screen viewer, share dialog are named Shadcn-based components; no raw DOM manipulation. |
| III — Evolution-Ready Architecture | ✅ PASS | All API calls isolated in `lib/admin.ts` / `lib/learning.ts`; backend data access behind repositories/services; image serving behind one endpoint; availability behind `AvailabilityService`. Swapping image storage or adding multi-teacher later needs no contract/UI rework. |
| Technology Stack | ✅ PASS | Frontend stack unchanged; backend continues Java 26 + Spring Boot 4 + plain JDBC + Flyway. No additions. |
| Development Workflow | ✅ PASS | UI browser-verified before completion; feature branch `006-teacher-backoffice` follows convention; no dead code (the hard-coded availability constants are removed, not commented out). |
| Backend location | ✅ PASS | New code under `back-end/` and `front-end/` only. |

**Gate result**: All principles satisfied. No complexity violations requiring justification —
Complexity Tracking section intentionally empty.

## Project Structure

### Documentation (this feature)

```text
specs/006-teacher-backoffice/
├── plan.md              # This file
├── research.md          # Phase 0 — authz, availability model, image storage, homework migration, presentations
├── data-model.md        # Phase 1 — role, availability_rules/exceptions, homework_targets, presentations/slides/shares/images
├── quickstart.md        # Phase 1 — end-to-end validation of the three stories + access control
├── contracts/
│   └── api.md           # Phase 1 — /api/v1/admin/** + auth/me + learning additions
├── checklists/
│   └── requirements.md  # Spec quality checklist (from /speckit-specify)
└── tasks.md             # Phase 2 — task list (/speckit-tasks output, NOT created here)
```

### Source Code (repository root)

```text
kuky/
├── front-end/
│   └── src/
│       ├── routes/
│       │   └── panel.tsx                          # NEW: admin-gated route (role===ADMIN else redirect /); tabbed AdminPanel
│       ├── components/
│       │   ├── SiteHeader.tsx                     # UPDATED: role-aware nav; "Panel" entry only when role===ADMIN
│       │   ├── admin/                             # NEW
│       │   │   ├── AdminPanel.tsx                 # Tabs: Disponibilidad / Tareas / Presentaciones
│       │   │   ├── availability/
│       │   │   │   ├── WeeklyAvailabilityEditor.tsx   # per-weekday windows; add/remove; save (full replace)
│       │   │   │   ├── AvailabilityExceptionList.tsx  # list + add (date, BLOCK/OPEN, window) + delete
│       │   │   │   └── BookingConflictNotice.tsx      # non-blocking warning from save response
│       │   │   ├── homework/
│       │   │   │   ├── HomeworkAdminList.tsx          # list with assignees + submission status
│       │   │   │   ├── HomeworkEditorDialog.tsx       # create/edit title/instructions/dueOn
│       │   │   │   └── StudentMultiSelect.tsx         # assignee / share picker (shared with presentations)
│       │   │   └── presentations/
│       │   │       ├── PresentationAdminList.tsx      # list + create/delete
│       │   │       ├── PresentationEditor.tsx         # title, slide list, add/reorder, share
│       │   │       ├── SlideEditorCard.tsx            # heading/body/image upload per slide
│       │   │       ├── SlidePreview.tsx               # full-screen pager (reused by student viewer)
│       │   │       └── SharePresentationDialog.tsx
│       │   └── learning/
│       │       ├── SharedPresentationsList.tsx    # NEW: decks shared with the student
│       │       └── PresentationViewer.tsx         # NEW: opens a shared deck (reuses SlidePreview)
│       └── lib/
│           ├── admin.ts                           # NEW: API client for /api/v1/admin/** + image upload
│           ├── auth.ts                            # UPDATED: UserResponse gains `role`
│           └── learning.ts                        # UPDATED: getPresentation(id); overview gains sharedPresentations
└── back-end/
    └── src/
        ├── main/
        │   ├── java/com/kuky/backend/
        │   │   ├── config/
        │   │   │   ├── SecurityConfig.java        # UPDATED: .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
        │   │   │   ├── JwtConfig.java             # UPDATED: write/read `role` claim
        │   │   │   ├── JwtCookieAuthenticationFilter.java  # UPDATED: grant ROLE_* authority from claim
        │   │   │   ├── AdminBootstrap.java        # NEW: CommandLineRunner promotes app.scheduling.teacher-email
        │   │   │   └── GlobalExceptionHandler.java # UPDATED: ACCESS_DENIED, PRESENTATION_NOT_FOUND, STUDENT_NOT_FOUND, INVALID_IMAGE
        │   │   ├── auth/
        │   │   │   ├── model/User.java            # UPDATED: role
        │   │   │   ├── repository/UserRepository.java  # UPDATED: map/save role; findStudents()
        │   │   │   ├── dto/UserResponse.java      # UPDATED: role
        │   │   │   └── service/AuthService.java   # UPDATED: include role in responses
        │   │   ├── scheduling/
        │   │   │   ├── service/AvailabilityService.java   # UPDATED: derive slots from availability tables
        │   │   │   ├── model/AvailabilityRule.java        # NEW POJO
        │   │   │   ├── model/AvailabilityException.java   # NEW POJO
        │   │   │   └── repository/AvailabilityRepository.java  # NEW: rules + exceptions CRUD, conflict query
        │   │   ├── presentations/                 # NEW package
        │   │   │   ├── model/{Presentation,Slide,PresentationShare,Image}.java
        │   │   │   ├── repository/{PresentationRepository,ImageRepository}.java
        │   │   │   ├── service/{PresentationService,ImageService}.java
        │   │   │   ├── controller/ImageController.java     # GET /api/v1/images/{id}
        │   │   │   └── exception/PresentationNotFoundException.java
        │   │   ├── admin/                         # NEW package (teacher-only controllers/dtos/services)
        │   │   │   ├── controller/{AvailabilityAdminController,HomeworkAdminController,PresentationAdminController,StudentAdminController,ImageAdminController}.java
        │   │   │   ├── dto/                        # request/response records per contract
        │   │   │   ├── service/HomeworkAdminService.java   # author/assign/edit/delete + submission read
        │   │   │   └── exception/StudentNotFoundException.java
        │   │   └── learning/
        │   │       ├── service/LearningService.java        # UPDATED: homework via targets; add sharedPresentations
        │   │       ├── repository/ContentRepository.java   # UPDATED: findAssignmentsForUser(userId)
        │   │       ├── controller/LearningController.java   # UPDATED: GET /learning/presentations/{id}
        │   │       └── dto/LearningResponse.java            # UPDATED: sharedPresentations
        │   └── resources/
        │       └── db/migration/
        │           ├── V6__add_user_roles.sql
        │           ├── V7__create_availability.sql        # + seed weekly pattern (behavior-preserving)
        │           ├── V8__create_homework_targets.sql
        │           └── V9__create_presentations.sql
        └── test/java/com/kuky/backend/
            ├── scheduling/AvailabilityServiceTest.java
            ├── admin/{HomeworkAdminServiceTest,AdminAuthorizationIntegrationTest}.java
            └── presentations/{PresentationServiceTest,ImageServiceTest}.java
```

**Structure Decision**: Extend the existing two-service layout. The backend adds a teacher-only
`admin` package whose controllers compose domain services, an `availability` extension inside the
existing `scheduling` domain (so the public schedule and admin editing share one
`AvailabilityService`), and a self-contained `presentations` package (with image handling). All
new code mirrors the established conventions (plain JDBC `NamedParameterJdbcTemplate` +
`RowMapper`, POJO models, `record` DTOs, `@AuthenticationPrincipal String email`,
`{error,message}` responses). Authorization is centralized: a single
`.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` in `SecurityConfig`, backed by a `role`
claim the JWT filter turns into a `ROLE_ADMIN` authority. The frontend adds one file-based route
`/panel` that gates on `role === 'ADMIN'` (redirecting others), a `components/admin/` folder, an
isolated `lib/admin.ts` client, and a shared-deck viewer reused between admin preview and the
student learning space; `SiteHeader` becomes role-aware. No new top-level projects,
infrastructure, or third-party dependencies are introduced.

## Complexity Tracking

> No constitution violations — section intentionally empty.
