# Implementation Plan: Mi aprendizaje — Classes & Homework Tab

**Branch**: `005-classes-homework-tab` | **Date**: 2026-06-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-classes-homework-tab/spec.md`

## Summary

Add a new **logged-in-only** navigation tab — **"Mi aprendizaje"** at route `/aprendizaje` — that gives a student a personal learning space with three areas: (1) a teacher **presentation** that introduces Paula's class offering, (2) the student's **past classes** (title, date, teacher note), and (3) **homework** assigned to the student, where each item shows a title, instructions, optional due date, and a status, and the student can mark it done / submit a short written response. Guests never see the tab (it is conditionally rendered in `SiteHeader` based on auth state) and cannot reach the page (the route redirects unauthenticated visitors to `/cuenta`, and every `/api/v1/learning/**` endpoint is server-side authenticated).

Per the clarifications, this iteration builds the **real backend** rather than front-end mocks. The backend extends the existing Java 26 / Spring Boot 4 service (plain JDBC + Flyway, no JPA) with a new `learning` package mirroring the established `auth` / `scheduling` / `resources` layout. A single Flyway migration `V5__create_learning.sql` creates and seeds four tables: `learning_presentation`, `past_classes`, `homework_assignments` (all **shared seeded definitions** shown to every authenticated student), and `homework_submissions` (**per-student state**, `UNIQUE(user_id, assignment_id)`). When listing homework for a student, the seeded assignments are LEFT-JOINed to that student's submissions so a missing row defaults to status `PENDING`; this is the "shared definitions, per-user state" model. Homework status follows **Pending → Submitted** driven only by the student; a `REVIEWED` status exists in the schema but is reserved/read-only for the future backoffice. **Overdue** is derived (due date in the past AND not yet submitted), never stored. The same tables are exactly what the future teacher backoffice will author into, so no model rework is needed later.

The frontend adds a `/aprendizaje` route (auth-gated with redirect), a `components/learning/` folder of named Shadcn components, and an isolated `lib/learning.ts` API client following the existing `lib/scheduling.ts` / `lib/resources.ts` pattern. `SiteHeader` becomes auth-aware so the protected nav entry only renders for logged-in users. No new third-party dependencies are introduced on either side.

## Technical Context

**Language/Version**: TypeScript 5.x strict (frontend) / Java 26 (backend, per `back-end/build.gradle` toolchain)

**Primary Dependencies**:
- Frontend: React 19, TanStack Start 1.x (SSR), TanStack Router (file-based), TanStack Query (wired in `__root.tsx`), TailwindCSS 4, Shadcn UI, Vite 7. **No new dependencies** — date formatting via the platform `Intl.DateTimeFormat` with the `"es"` locale (as used in scheduling).
- Backend: Spring Boot 4.0.x (`spring-boot-starter-web`, `-security`, `-jdbc`, `-validation`), Flyway, PostgreSQL driver, jjwt. **No new dependencies**.

**Storage**: PostgreSQL 18 — four new tables created by Flyway `V5__create_learning.sql` (`learning_presentation`, `past_classes`, `homework_assignments`, `homework_submissions`) plus seeded placeholder rows. Reuses the existing `users` table for the per-student submission FK.

**Testing**:
- Backend: JUnit 5 + Spring Boot Test (already present). Unit tests for `HomeworkSubmissionService` (pending→submitted upsert, idempotent re-submit while pending/submitted, reject when reviewed, response length validation, unknown assignment) and `LearningService` (overdue derivation, status defaulting to PENDING for assignments with no submission, most-recent-first ordering of past classes). Slice/integration test for `LearningController` (401 unauthenticated; authenticated overview returns presentation + past classes + homework; submit flips status and persists).
- Frontend: visual verification in a running browser per the constitution (no test framework configured in the repo).

**Target Platform**: Browser via TanStack Start SSR (frontend) + JVM server on port 8081 (backend REST API).

**Performance Goals**: A logged-in student can locate and open the section in < 10 s (SC-002) and mark homework done / submit in < 30 s with persistence verified on reload (SC-004). All lists are a handful of rows; every path is well within standard request latencies.

**Constraints**:
- The entire section is private: the nav entry is hidden from guests (FR-002), the `/aprendizaje` route redirects unauthenticated visitors to `/cuenta` (FR-003), and all `/api/v1/learning/**` endpoints require authentication (reuse existing JWT-cookie auth; covered by the existing `anyRequest().authenticated()` rule — no `permitAll` is added).
- Past classes, homework definitions, and presentation content are **backend-seeded** and **shared** across all authenticated students (FR-004, FR-005, FR-011, FR-015).
- Each student's homework submission/status is **persisted server-side per student** and isolated between students (FR-008, FR-015); state survives reload and new sessions/devices.
- Homework status lifecycle is **Pending → Submitted** student-driven; `REVIEWED` is reserved and read-only to students (FR-014). **Overdue** is derived from due date + status, never stored (FR-009).
- Homework submission is lightweight: an optional short written response (length-capped) and/or a done flag; no file uploads, grading, or feedback threads (Assumptions).
- All student-facing copy is Spanish, consistent with the rest of the site (FR-012); dates render with the `"es"` locale.
- Empty states are required for both lists even though seeded data makes them non-empty in dev (FR-010, SC-005).

**Scale/Scope**: Small — one teacher (Paula), a handful of seeded past classes / homework, dozens to low hundreds of students; single-instance backend. One new frontend route, ~5 components, one API client; one backend package, one migration.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I — Simplicity First | ✅ PASS | Minimal normalized schema: shared definitions (presentation / past classes / homework) plus one per-student state table. Definitions and per-student state are split only because the clarified "shared sample, per-user state" requirement demands it. No new dependencies, no JPA, no file storage, no grading. `REVIEWED` is a reserved enum value (one CHECK constant), not new machinery. Overdue is derived, not a stored column. |
| II — Component-Driven UI | ✅ PASS | Presentation, past-classes list, homework list, homework item, and submit form are named Shadcn-based React components; no raw DOM manipulation. |
| III — Evolution-Ready Architecture | ✅ PASS | API calls isolated in `front-end/src/lib/learning.ts`; seeded content read behind a `ContentRepository` / `LearningService`; the seeded tables are exactly the ones the future backoffice will write to (FR-011). Swapping seed data for teacher-authored rows requires no UI or contract change. |
| Technology Stack | ✅ PASS | Frontend stack unchanged; backend continues Java 26 + Spring Boot 4 + plain JDBC + Flyway. No stack additions. |
| Development Workflow | ✅ PASS | UI changes browser-verified before completion; feature branch `005-classes-homework-tab` follows the naming convention; no dead code. |
| Backend location | ✅ PASS | New code under `back-end/` and `front-end/` only. |

**Gate result**: All principles satisfied. No complexity violations requiring justification — Complexity Tracking section omitted.

## Project Structure

### Documentation (this feature)

```text
specs/005-classes-homework-tab/
├── plan.md              # This file
├── research.md          # Phase 0 — auth gating, shared-def/per-user model, status & overdue, nav visibility, seeding
├── data-model.md        # Phase 1 — learning_presentation / past_classes / homework_assignments / homework_submissions
├── quickstart.md        # Phase 1 — local validation guide
├── contracts/
│   └── api.md           # Phase 1 — REST API contract for the learning overview + homework submission
├── checklists/
│   └── requirements.md  # Spec quality checklist (from /speckit-specify)
└── tasks.md             # Phase 2 — task list (/speckit-tasks output, not created here)
```

### Source Code (repository root)

```text
kuky/
├── front-end/                                  # Existing React/TanStack SSR app
│   └── src/
│       ├── routes/
│       │   └── aprendizaje.tsx                  # NEW: auth-gated route; redirects guests to /cuenta; renders LearningView
│       ├── components/
│       │   ├── SiteHeader.tsx                   # UPDATED: auth-aware nav; show "Mi aprendizaje" → /aprendizaje only when logged in
│       │   └── learning/                        # NEW component folder
│       │       ├── LearningView.tsx             # Orchestrates fetch of GET /learning + loading/empty; lays out the three areas
│       │       ├── ClassPresentation.tsx        # Renders the teacher presentation blocks (read-only intro)
│       │       ├── PastClassesList.tsx          # Past classes (title, es-locale date, teacher note); empty state
│       │       ├── HomeworkList.tsx             # Homework items + empty state
│       │       ├── HomeworkItemCard.tsx         # One assignment: title, instructions, due date, status badge, overdue flag, action
│       │       └── HomeworkSubmitDialog.tsx     # Mark done / submit short written response
│       └── lib/
│           └── learning.ts                      # NEW: API client (getLearning, submitHomework)
└── back-end/                                     # Existing Spring Boot service
    └── src/
        ├── main/
        │   ├── java/com/kuky/backend/
        │   │   ├── learning/                     # NEW package (mirrors auth/, scheduling/, resources/ layout)
        │   │   │   ├── controller/LearningController.java   # GET /api/v1/learning ; PUT /api/v1/learning/homework/{assignmentId} (auth)
        │   │   │   ├── dto/                                 # LearningResponse, PresentationBlockResponse, PastClassResponse, HomeworkItemResponse, SubmitHomeworkRequest
        │   │   │   ├── model/PresentationBlock.java         # POJO
        │   │   │   ├── model/PastClass.java                 # POJO
        │   │   │   ├── model/HomeworkAssignment.java        # POJO
        │   │   │   ├── model/HomeworkSubmission.java        # POJO
        │   │   │   ├── model/HomeworkStatus.java            # enum PENDING, SUBMITTED, REVIEWED
        │   │   │   ├── repository/ContentRepository.java    # NamedParameterJdbcTemplate + RowMappers (presentation, past classes, assignments)
        │   │   │   ├── repository/HomeworkSubmissionRepository.java  # per-user read + upsert (ON CONFLICT)
        │   │   │   ├── service/LearningService.java         # assembles overview; LEFT-JOIN status default PENDING; derives overdue
        │   │   │   ├── service/HomeworkSubmissionService.java # validate transition (PENDING→SUBMITTED), length cap, upsert
        │   │   │   └── exception/                           # AssignmentNotFoundException, SubmissionNotAllowedException
        │   │   └── config/
        │   │       ├── SchedulingProperties.java            # REUSED: teacher timezone for the "today" boundary in overdue derivation
        │   │       └── GlobalExceptionHandler.java          # UPDATED: map ASSIGNMENT_NOT_FOUND, SUBMISSION_NOT_ALLOWED
        │   └── resources/
        │       └── db/migration/
        │           └── V5__create_learning.sql             # NEW migration + seeded placeholder content (Spanish)
        └── test/java/com/kuky/backend/
            └── learning/
                ├── HomeworkSubmissionServiceTest.java
                ├── LearningServiceTest.java
                └── LearningControllerIntegrationTest.java
```

**Structure Decision**: Extend the existing two-service layout. The backend gains a self-contained `learning` package that mirrors the established `auth` / `scheduling` / `resources` conventions (controller / dto / model / repository / service, plain JDBC repositories with `NamedParameterJdbcTemplate` + `RowMapper`, POJO models, `record` DTOs, current user via `@AuthenticationPrincipal String email`). No `SecurityConfig` change is needed: because the section is fully private, `/api/v1/learning/**` is already covered by the existing `anyRequest().authenticated()` rule (and returns 401 to guests). The frontend adds one file-based route `/aprendizaje` that gates on auth and redirects guests to `/cuenta`, a `components/learning/` folder, and an isolated `lib/learning.ts` client; `SiteHeader` is upgraded to be auth-aware so the protected link is conditionally rendered. No new top-level projects, infrastructure, or third-party dependencies are introduced.

## Complexity Tracking

> No constitution violations — section intentionally empty.
