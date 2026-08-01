# Phase 0 Research: University Student Portal

All items resolved from the clarified spec, constitution, and existing role/learning/CORS patterns. No external vendor research required.

## Decision: Add `UNIVERSITY_STUDENT` as a fourth value on `users.role` (single-column model)

**Rationale**: The app maps JWT → exactly one `ROLE_*` authority (`JwtCookieAuthenticationFilter`). Spec FR-016 requires private-student and university-student to be mutually exclusive — a single `role` column already enforces that. Extending the CHECK to `('USER','STUDENT','UNIVERSITY_STUDENT','ADMIN')` matches how `USER`/`STUDENT`/`ADMIN` were introduced (012) and needs no roles table. Grant university = set `role = 'UNIVERSITY_STUDENT'` + require `university_level`; revoke = `role = 'USER'` and clear level. Granting `STUDENT` while `UNIVERSITY_STUDENT` (or the reverse) is rejected in the service.

**Alternatives considered**:
- **Separate `is_university_student` flag beside `STUDENT` role**: rejected — allows both statuses at once unless extra guards; contradicts single-authority JWT model; more complex than a fourth role value.
- **Multi-authority JWT / roles join table**: rejected — YAGNI and would rewrite auth across the app for one cohort.

## Decision: Store university level on `users.university_level`

**Rationale**: Each university student has exactly one level (`BEGINNER` | `INTERMEDIATE`). A nullable `VARCHAR` with CHECK, meaningful only when `role = 'UNIVERSITY_STUDENT'`, mirrors `extended_class_eligible` (scalar on `users`). Clear on revoke. No separate enrollment table for v1 (single cohort, one year, out-of-scope archives).

**Alternatives considered**:
- **Enrollment history table**: rejected — multi-year archives explicitly out of scope.
- **Derive level only from content**: rejected — schedule filtering and FR-004 require an assigned level at grant time.

## Decision: Same frontend deployable; host-based university shell (separate public entry)

**Rationale**: Clarification requires a separate URL/subdomain with shared accounts, not a separate product. One TanStack Start app with a host-aware root layout keeps one codebase (Simplicity First) while giving a distinct navigation/home for university flows. Production: e.g. `universidad.kuky.es` vs `kuky.es`. Local: second hostname (e.g. `uni.localhost:8080`) or documented hosts-file entry pointing at the same Vite server.

**Alternatives considered**:
- **Second frontend package/repo**: rejected — doubles build/deploy for identical stack and shared auth.
- **Path-only `/universidad` on the private origin**: rejected — does not satisfy the clarified “separate public entry (URL/subdomain)” decision (can remain a local-dev fallback only if host routing is awkward, not the production model).
- **Fully separate accounts**: rejected — clarification chose shared accounts + registration on the university entry.

## Decision: Multi-origin CORS + parent-domain cookie for shared login

**Rationale**: Today `CorsConfig` allows a single exact origin and the `auth-token` cookie has no `Domain` (host-only). Subdomain ↔ API (or subdomain ↔ private site sharing API) needs: (1) CORS allow-list of both origins (or `allowedOriginPatterns`), (2) cookie `Domain=.kuky.es` in production so login on either entry shares the session. Local: keep host-only cookies if both shells share `localhost` with different ports/hosts carefully documented; prefer same registrable local parent if feasible.

**Alternatives considered**:
- **Token in localStorage per origin**: rejected — breaks HttpOnly cookie security model used everywhere.
- **SSO redirect broker**: rejected — YAGNI for a single-teacher site.

## Decision: Level-based availability join tables for shared catalog (not bulk `homework_targets`)

**Rationale**: Presentations/homeworks are already a global catalog; private access is per-user (`homework_targets`, `presentation_shares` / unit assignments). Spec wants teacher to “make available for a university level” without maintaining a wholly separate catalog. New joins `university_homework_availability(assignment_id, level)` and `university_presentation_availability(presentation_id, level)` grant cohort access by level. University learning APIs authorize when `role = UNIVERSITY_STUDENT` and a row exists for that student’s `university_level`. Reuse existing `homework_submissions` / exercise grading — mutual exclusivity means no dual-context collision on `(user_id, assignment_id)`.

**Alternatives considered**:
- **Bulk-insert every cohort member into `homework_targets` on grant/availability change**: rejected — fragile on level change/revoke/new enrollments; duplicates private assignment semantics.
- **Duplicate homework/presentation rows for university**: rejected — contradicts shared-catalog clarification.
- **Auto-expose entire catalog by CEFR `level` column on homework**: rejected — teacher must explicitly choose what is university-visible (FR-015); CEFR levels ≠ BEGINNER/INTERMEDIATE university tracks.

## Decision: New `university` backend package for informative content; learning reuse with university gates

**Rationale**: Schedule template/exceptions, exam dates, and news are university-only entities with no private-lesson analogue — a dedicated `com.kuky.backend.university` package keeps boundaries clear. Homework/presentation completion reuses `learning` services/repos with an access strategy that accepts university availability (avoid copying grading logic). Admin management stays under `/api/v1/admin/**` on the existing panel (teacher already works there); university shell is student/visitor-facing.

**Alternatives considered**:
- **Fork full learning stack under `/university/learning` with copy-paste services**: rejected — duplicates ExerciseGradingService and submission rules.
- **Admin UI only on university host**: rejected — teacher already has `/panel`; one admin surface is simpler.

## Decision: Public GET for schedule / exams / news; gated learning for `UNIVERSITY_STUDENT`/`ADMIN`

**Rationale**: Clarifications made informative sections publicly readable; materials/homework require login + university status. Mirror existing SecurityConfig style: public GETs listed explicitly; learning-style routes `hasAnyRole("UNIVERSITY_STUDENT","ADMIN")`. Private `STUDENT` gates unchanged. `ADMIN` can exercise university learning for support/testing (same pattern as private learning).

**Alternatives considered**:
- **Authenticated-only informative reads**: rejected — contradicts clarification C (public schedule/exams/news).

## Decision: No email on university grant/revoke

**Rationale**: Explicit clarification — unlike private `STUDENT` promote/revoke emails. Admin services must not call `EmailService` for university status changes.

## Decision: Next Flyway migration `V4__university_portal.sql`

**Rationale**: Current latest is `V3__student_interests.sql`. One migration for role CHECK widen, `university_level`, schedule/news/exam tables, and availability joins keeps deploy atomic for the feature.
