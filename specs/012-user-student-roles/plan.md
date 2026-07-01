# Implementation Plan: Differentiate Users from Students

**Branch**: `012-user-student-roles` | **Date**: 2026-07-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/012-user-student-roles/spec.md`

## Summary

Today every account created via sign-up defaults to `role = 'STUDENT'` (`V6__add_user_roles.sql`), so "has an account" and "is a student" are the same thing. This feature splits them: a third role value, `USER`, becomes the default for new accounts, and only the teacher — from the admin panel — can promote a `USER` to `STUDENT` (or demote back). All accounts that already exist keep their current `STUDENT` role untouched, so nothing breaks at rollout (FR-010) — this requires **no data migration**, only a schema/default change for future rows.

Per the clarified spec: browsing the class schedule and resource listings, and the placement test, stay open to any logged-in user (`/prueba-de-nivel` and the existing public `GET` endpoints for schedule/resources are already unrestricted). Only the **actions** — creating a booking, purchasing/unlocking a resource, and anything under coursework (`/api/v1/learning/**`: units, homework, presentations) — become `STUDENT`-or-`ADMIN`-only. The teacher's own `ADMIN` role already supersedes `STUDENT` everywhere (FR-013), so gates are expressed as `hasAnyRole("STUDENT","ADMIN")`, never `hasRole("STUDENT")` alone.

The admin panel's existing "Students" tab (backed by `GET /api/v1/admin/students`, which already filters `WHERE role = 'STUDENT'`) is unaffected. It gains a sibling "Users" view listing non-student registered accounts with a "make student" action, plus a "revoke" action on the existing student list. Granting/revoking sends a plain-text email (mirroring `EmailService.sendActivationEmail`/`sendPasswordResetEmail`), per the clarified FR-014.

This is a small, additive change: one migration, one repository method + two admin endpoints, one `SecurityConfig` edit, one `EmailService` extension, and frontend updates to the role type, the two gated action flows (booking, purchase), the coursework route, and the admin Students/Users UI.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5 (`-web`, `-security`, `-jdbc`, `-mail`), Flyway, PostgreSQL driver. **No new dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next. **No new dependencies.**

**Storage**: PostgreSQL 18. New migration `V23__add_user_role.sql` widens the `users.role` CHECK constraint to `('USER','STUDENT','ADMIN')` and changes the column default to `'USER'`. No existing rows are updated — every account that exists today already has `role = 'STUDENT'`, which is exactly the grandfathering FR-010 requires, so it falls out of the current schema for free.

**Testing**:
- Backend: JUnit 5 + Spring Boot Test + `spring-security-test`. New/extended tests: `SecurityConfig` gating (anonymous/`USER`/`STUDENT`/`ADMIN` × booking-create, purchase-create, `/api/v1/learning/**`, schedule/resource browsing), `UserRepository` promote/revoke methods, `EmailService` new methods (mail sent with correct recipient/subject), admin controller tests for the new promote/revoke endpoints (403 for non-admin).
- Frontend: visual verification in a running browser per the constitution (no test framework in repo) — register a new account, confirm it can browse but not book/purchase/open coursework, promote it from `/panel`, confirm access unlocks, revoke it, confirm access locks again while history remains visible.

**Target Platform**: Browser via TanStack Start SSR (frontend, `:8080`) + JVM server on `:8081` (backend).

**Performance Goals**: N/A — access-control check adds no measurable latency (same JWT-authority check the app already does for `ADMIN`).

**Constraints**: A user's role is a single column carrying a single JWT authority (`JwtCookieAuthenticationFilter` maps it to exactly one `ROLE_*` `GrantedAuthority`) — there is no multi-role model in this codebase, so `USER`/`STUDENT`/`ADMIN` must stay mutually exclusive values of that one column rather than an additive roles/permissions table (Simplicity First). Revoking `STUDENT` status MUST NOT touch or hide existing `bookings`, `purchases`, or `homework_targets` rows (FR-012) — those stay keyed by `user_id` regardless of current role.

**Scale/Scope**: Single-teacher site, low tens of users. One migration, one repository/service extension, one `SecurityConfig` edit, two new admin endpoints, two new `EmailService` methods, one extended admin tab + one new admin tab, three touched frontend route/flows.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. No new roles/permissions table, no audit-log table, no notification-preferences system — a third value on the existing single `role` column, using the existing plain-text `EmailService` pattern. Grandfathering existing accounts requires zero migration data changes because they're already `STUDENT`.
- **II. Component-Driven UI** — PASS. The new "Users" admin view and "make/revoke student" actions are named React components following the existing `StudentsTab.tsx` pattern; no raw DOM manipulation.
- **III. Evolution-Ready Architecture** — PASS. All new data-fetching lives in `front-end/src/lib/admin.ts` (extended) and `front-end/src/lib/auth.ts` (role type widened), never inlined in components, matching existing conventions.
- **Technology Stack** — PASS. No new dependencies; reuses Spring Security role-based `authorizeHttpRequests`, `JavaMailSender`, React/TanStack/Tailwind/Shadcn.
- **Development Workflow** — PASS. UI changes verified in a running browser before completion; branch `012-user-student-roles` follows convention; no dead code.

**Result: PASS — no violations. Complexity Tracking left empty.**

## Project Structure

### Documentation (this feature)

```text
specs/012-user-student-roles/
├── plan.md              # This file (/speckit-plan output)
├── research.md          # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md         # Phase 1 output
├── contracts/
│   └── user-roles-api.md # Phase 1 output
├── checklists/
│   └── requirements.md   # /speckit-specify output
└── tasks.md               # /speckit-tasks output (NOT created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V23__add_user_role.sql            # widen role CHECK to USER/STUDENT/ADMIN; default 'USER'

back-end/src/main/java/com/kuky/backend/
├── auth/
│   ├── model/User.java                        # EDIT: default role "USER"
│   ├── dto/UserResponse.java                   # unchanged shape (role already a plain String)
│   └── repository/UserRepository.java          # ADD: findRegisteredUsers(), promoteToStudentById(), revokeStudentById()
├── admin/
│   ├── controller/StudentAdminController.java  # ADD: GET /admin/users, POST /admin/users/{id}/student, DELETE /admin/users/{id}/student
│   ├── dto/                                    # ADD: RegisteredUserResponse (id, email, name, username, role)
│   └── service/StudentProfileAdminService.java  # unchanged (already filters role='STUDENT')
├── auth/service/EmailService.java              # ADD: sendStudentGrantedEmail(), sendStudentRevokedEmail()
└── config/SecurityConfig.java                  # EDIT: add STUDENT-or-ADMIN matchers (see contracts)

back-end/src/test/java/com/kuky/backend/
├── config/SecurityConfigStudentGatingTest.java  # NEW
├── admin/StudentAdminControllerTest.java        # EXTEND
└── auth/EmailServiceTest.java                   # EXTEND (or new if none exists)

front-end/src/
├── lib/auth.ts                        # EDIT: UserResponse.role → "USER" | "STUDENT" | "ADMIN"
├── lib/admin.ts                       # EDIT: add getRegisteredUsers(), promoteToStudent(), revokeStudent()
├── components/admin/students/
│   ├── StudentsTab.tsx                 # EDIT: add "revoke" action per row
│   └── UsersTab.tsx                    # NEW: list USER-role accounts + "make student" action
├── routes/panel.tsx                    # EDIT: register the new "Users" tab
├── routes/reservas.tsx                 # EDIT: gate the booking action (not the schedule view) on role
├── routes/recursos.tsx                 # EDIT: gate purchase/unlock (not browsing) on role
├── routes/aprendizaje.tsx              # EDIT: gate the whole page (coursework, not public browsing) on role
└── components/StudentOnlyNotice.tsx    # NEW: shared "ask the teacher" message component (FR-009)
```

**Structure Decision**: Web application (existing `front-end/` + `back-end/`). No new backend domain package — this extends the existing `auth` and `admin` packages, since "student designation" is a state on the existing `User`/`users` row, not a new bounded-context entity. The frontend extends the existing admin students area and adds one small shared notice component reused across the three gated surfaces.

## Complexity Tracking

> No Constitution Check violations. No entries required.
