# Implementation Plan: Student Interests on Profile

**Branch**: `022-student-interests` | **Date**: 2026-08-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/022-student-interests/spec.md`

## Summary

Students with student access declare conversation interests on `/cuenta`: multi-select from a fixed product catalogue (12 codes) plus an optional 280-character free-text note. Persistence uses a new `user_interests` join table and `users.interests_note`, written via a dedicated `PUT /api/v1/auth/interests` (not folded into `PUT /profile`). The teacher sees the same data read-only on the existing admin student profile (`GET /api/v1/admin/students/{id}/profile`) to prepare classes. No catalogue admin UI, no booking/schedule surfacing, no approval workflow.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5 (`-web`, `-security`, `-jdbc`), Flyway, PostgreSQL JDBC. **No new dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next. **No new dependencies** — new `InterestSelector` (or equivalent) composed from existing Checkbox/Label/Textarea/Button primitives.

**Storage**: PostgreSQL 18. New migration `V3__student_interests.sql`:
- `users.interests_note VARCHAR(280)` (nullable).
- `user_interests (user_id, interest_code)` with PK, `ON DELETE CASCADE`, and `CHECK` listing the 12 product codes; index on `user_id`.

**Testing**:
- Backend: JUnit 5 + Mockito + AssertJ. Extend/add auth service/controller tests for role gate, catalogue validation, max 10, note length, replace/clear; admin profile assembly includes interests.
- Frontend: no test framework in this repo — visual verification per constitution (student `/cuenta`, USER hidden, admin student profile).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (existing `front-end/` + `back-end/`).

**Performance Goals**: N/A — tiny per-user row set (≤10 interest rows).

**Constraints**: Soft max 10 codes; note ≤280; write restricted to `STUDENT`/`ADMIN`; teacher view-only; public pages unchanged.

**Scale/Scope**: Single-teacher site, low tens of students; one migration, one new auth endpoint, DTO extensions, one account UI section, one admin profile section, i18n for 12 labels × 3 locales.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Join table + note column; fixed catalogue as constants + CHECK (no options admin CRUD); dedicated PUT mirrors `TimezoneSetting` independence without inventing a new domain package.
- **II. Component-Driven UI** — PASS. Named account component for interests selection; admin profile uses existing `Section` pattern on `panel_.alumnos.$studentId.tsx`.
- **III. Evolution-Ready Architecture** — PASS. Client calls live in `front-end/src/lib/auth.ts` (+ small `interests.ts` catalogue module); no fetch inlined in JSX beyond existing patterns.
- **Technology Stack** — PASS. No new dependencies.
- **Development Workflow** — PASS. Browser verification in quickstart; branch `022-student-interests`; no dead endpoints.

**Result: PASS — no violations. Complexity Tracking left empty.**

## Project Structure

### Documentation (this feature)

```text
specs/022-student-interests/
├── plan.md                         # This file
├── research.md                     # Phase 0
├── data-model.md                   # Phase 1
├── quickstart.md                   # Phase 1
├── contracts/
│   └── interests-api.md             # Phase 1
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V3__student_interests.sql

back-end/src/main/java/com/kuky/backend/
├── auth/
│   ├── InterestCatalogue.java                 # NEW: codes, max 10, max note 280
│   ├── model/User.java                        # EDIT: interestsNote
│   ├── repository/UserRepository.java         # EDIT: note in mapper; replace/find interest codes
│   ├── dto/UserResponse.java                  # EDIT: interests, interestsNote
│   ├── dto/UpdateInterestsRequest.java        # NEW
│   ├── controller/AuthController.java         # EDIT: PUT /interests
│   └── service/AuthService.java               # EDIT: updateInterests + toResponse mapping
├── admin/
│   ├── dto/StudentProfileResponse.java        # EDIT: interests, interestsNote
│   └── service/StudentProfileAdminService.java # EDIT: load interests into profile
└── common/                                    # EDIT only if new error codes need GlobalExceptionHandler mapping

front-end/src/
├── lib/
│   ├── interests.ts                            # NEW: INTEREST_CODES + InterestCode
│   ├── auth.ts                                # EDIT: UserResponse fields; updateInterests()
│   └── admin.ts                               # EDIT: StudentProfile interests fields
├── components/account/
│   └── InterestsSetting.tsx                   # NEW: multi-select + note + save (TimezoneSetting parallel)
├── routes/
│   ├── cuenta.tsx                             # EDIT: render InterestsSetting for STUDENT|ADMIN
│   └── panel_.alumnos.$studentId.tsx          # EDIT: interests Section (read-only)
└── i18n/locales/{es,en,ro}.ts                 # EDIT: interests.* codes + account/admin copy
```

**Structure Decision**: Extend existing `auth` and `admin` packages and the account/admin profile UIs. No new backend top-level package.

## Complexity Tracking

> No constitution violations to justify.
