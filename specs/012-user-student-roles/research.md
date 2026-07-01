# Phase 0 Research: Differentiate Users from Students

No open `NEEDS CLARIFICATION` markers remained after `/speckit-clarify` and the Technical Context above. This document records the design decisions made while surveying the existing codebase, so Phase 1 can proceed directly to data model and contracts.

## 1. How to represent "student" as a distinct state

**Decision**: Add a third value, `USER`, to the existing single `users.role` column (currently `CHECK (role IN ('STUDENT', 'ADMIN'))`, default `'STUDENT'`). New default becomes `'USER'`. Designating a student sets `role = 'STUDENT'`; revoking sets it back to `role = 'USER'`.

**Rationale**: `role` is already a single column, and `JwtCookieAuthenticationFilter` (back-end/src/main/java/com/kuky/backend/config/JwtCookieAuthenticationFilter.java:40-41) maps it to exactly one `GrantedAuthority` (`"ROLE_" + role`) baked into the JWT. There is no multi-role/permissions model anywhere in the codebase (confirmed: no `@PreAuthorize`, no roles-join-table). Introducing a separate `is_student` boolean alongside `role` would create two overlapping sources of truth for the same concept the constitution's Simplicity First principle warns against. A three-valued single column is the minimal change consistent with the existing architecture.

**Alternatives considered**:
- *Separate `student_designations` table (grant history with timestamps/granted-by)* — rejected as speculative; the spec doesn't require an audit trail, and it would need its own migration, repository, and consistency-with-`role`-claim logic. Can be added later if a real need for a history/audit feature arises (YAGNI).
- *Boolean `is_student` column alongside `role`* — rejected: would require encoding two independent facts (admin-ness, student-ness) into one JWT authority claim design that was built for exactly one value, forcing either a second claim (bigger JWT/filter change) or a derived-role hack. The three-valued column keeps the existing single-authority JWT model intact.

## 2. How to gate student-only actions

**Decision**: Extend `SecurityConfig.filterChain` (back-end/src/main/java/com/kuky/backend/config/SecurityConfig.java:46-52) with method-specific `requestMatchers(...).hasAnyRole("STUDENT", "ADMIN")` rules for: `POST /api/v1/bookings` (create booking), `POST /api/v1/purchases` (purchase/unlock), and `/api/v1/learning/**` (all coursework: overview, homework submit/answers, presentation file). `GET /api/v1/bookings`, `GET /api/v1/purchases`, and `GET /api/v1/purchases/{id}/receipt` (a user's own history) stay under the existing `.anyRequest().authenticated()` fallthrough — history must remain visible after revocation (FR-012).

**Rationale**: Matches the codebase's existing, sole pattern for role gating — the `/api/v1/admin/**` → `hasRole("ADMIN")` matcher — rather than introducing `@PreAuthorize` annotations, which are used nowhere else in ~15 controllers. Centralizing in `SecurityConfig` keeps all authorization rules in one auditable place. `hasAnyRole("STUDENT","ADMIN")` (not `hasRole("STUDENT")` alone) is required because the teacher's `ADMIN` account must retain full access per FR-013, and a single-role JWT means an admin's authority is literally `ROLE_ADMIN`, never `ROLE_STUDENT`.

**Alternatives considered**:
- *`@PreAuthorize` on individual controller methods* — rejected: inconsistent with the rest of the codebase (zero existing usages), and would scatter authorization logic across `BookingController`, `PurchaseController`, and `LearningController` instead of the one place engineers already check.
- *Gate entire `/api/v1/learning/**`, `/api/v1/bookings/**`, `/api/v1/purchases/**` path prefixes* — rejected for bookings/purchases specifically, because it would also block `GET` (viewing your own history), violating FR-012. Coursework (`/api/v1/learning/**`) *is* gated as a whole prefix because, per the clarified spec, coursework content itself (not just a submit action) is student-only — there's no "browse-only" mode for units/homework/presentations the way there is for the public schedule/resource listings.

## 3. Grandfathering existing accounts (FR-010)

**Decision**: No data migration needed. Every account that exists today already has `role = 'STUDENT'` (the current default). The new migration only changes the column's `DEFAULT` and widens the `CHECK` constraint; it does not `UPDATE` any existing row.

**Rationale**: This is the simplest possible way to satisfy "all existing accounts keep uninterrupted access" — it falls out of the current schema state rather than requiring a backfill script, and there is zero risk of the migration accidentally touching production data.

## 4. Grant/revoke email notifications (FR-014)

**Decision**: Add `sendStudentGrantedEmail(String toEmail)` and `sendStudentRevokedEmail(String toEmail)` to the existing `EmailService` (back-end/src/main/java/com/kuky/backend/auth/service/EmailService.java), following the exact pattern of `sendActivationEmail`/`sendPasswordResetEmail`: `SimpleMailMessage`, Spanish plain-text copy, `${app.mail.from}` sender. No link/token needed (unlike activation/reset) — just a plain notice.

**Rationale**: Reuses the existing `JavaMailSender` bean and Mailpit-backed local dev setup (already documented in CLAUDE.md) with zero new infrastructure. Plain text matches every other transactional email the app sends today; introducing HTML templates for just this feature would be scope creep.

## 5. Admin UI for promoting/revoking

**Decision**: Add a new `GET /api/v1/admin/users` endpoint returning accounts with `role = 'USER'` (mirroring `GET /api/v1/admin/students`'s `WHERE role = 'STUDENT'` query in `UserRepository.findStudents()`), surfaced in a new `UsersTab.tsx` component that mirrors `StudentsTab.tsx`'s list layout, with a "make student" button per row. `StudentsTab.tsx` gets a matching "revoke" button. Both call new endpoints: `POST /api/v1/admin/users/{id}/student` (grant) and `DELETE /api/v1/admin/users/{id}/student` (revoke).

**Rationale**: Directly reuses the existing list-of-users UI pattern (`StudentsTab.tsx`, `StudentLink.tsx`) and the existing `/api/v1/admin/students` query style, minimizing new code and keeping the admin panel visually consistent.
