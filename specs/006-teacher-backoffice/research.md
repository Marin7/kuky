# Phase 0 — Research: Teacher Backoffice — Control Panel

Feature: `006-teacher-backoffice` | Date: 2026-06-10 | Spec: [spec.md](spec.md)

This document resolves the open technical decisions for the teacher backoffice. All
four spec clarifications (admin identity, image storage, availability model, homework
migration) are treated as fixed inputs and turned into concrete design decisions below.

---

## 1. Teacher/administrator authorization

**Decision**: Add a persisted `role` column to `users` (`'STUDENT' | 'ADMIN'`, default
`'STUDENT'`). Embed the role as a claim in the JWT at token issuance; the existing
`JwtCookieAuthenticationFilter` reads the claim and grants a `ROLE_ADMIN` / `ROLE_STUDENT`
authority. `SecurityConfig` adds `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`
ahead of `anyRequest().authenticated()`. A startup `AdminBootstrap` runner promotes the
account whose email equals `app.scheduling.teacher-email` to `ADMIN` (idempotent UPDATE).

**Rationale**:
- Centralizes authorization in `SecurityConfig` (one matcher) rather than scattering
  manual checks in every admin controller — guests get 401, authenticated non-admins get 403.
- Keeping the role in the JWT preserves the filter's current statelessness (no per-request
  DB lookup), consistent with the existing rolling-session design.
- `teacher-email` already exists in `application-local.yaml` / `SchedulingProperties`, so no
  new configuration surface is introduced; the same property already designates Paula.
- The seeded promotion satisfies the clarified "promoted via migration/seed" requirement
  while remaining environment-configurable for production.

**Trade-off**: A user promoted to ADMIN takes effect on their **next login** (when a fresh
token with the role claim is issued). Acceptable: there is exactly one teacher, promoted at
bootstrap before she ever logs into the panel. Documented so it is not mistaken for a bug.

**Alternatives considered**:
- *Per-request DB lookup of role in the filter* — robust to instant promotion but adds a DB
  hit to every authenticated request; rejected as unnecessary for a single seeded admin.
- *Hardcoding the admin email in a migration* — not environment-portable; rejected.
- *Spring method security (`@PreAuthorize`)* — would require enabling global method security;
  the URL matcher is simpler and matches the existing filter-chain style.

**`/me` exposure**: `UserResponse` gains a `role` field so the frontend can conditionally
render the panel nav entry and gate the `/panel` route. This is the only auth-DTO change.

---

## 2. Availability model — replace hard-coded rules with teacher-managed data

**Decision**: Two new tables drive availability:
- `availability_rules` — the recurring weekly pattern: one row per (weekday, start_time,
  end_time) window. A weekday with no rows is fully unavailable. Multiple windows per
  weekday are multiple rows (e.g. morning + afternoon around a lunch gap).
- `availability_exceptions` — date-specific overrides: `BLOCK` (remove time on a date) or
  `OPEN` (add time on a date), each with a date and a time window.

`AvailabilityService` is refactored so that, for each day in the existing 2-week horizon, the
available windows are computed as `weekly_rules(weekday) − BLOCK_exceptions(date) + OPEN_exceptions(date)`,
then sliced into `class-duration` slots. The existing safeguards are **retained on top**:
minimum lead time, no past slots, and confirmed-booking → `BOOKED`. `validateBookable`
applies the same derived availability so direct API booking attempts are checked identically.

**Rationale**:
- A weekly pattern + exceptions is the standard scheduling model (matches Calendly-style
  tools and the clarified answer) and maps cleanly to the existing virtual-slot generator —
  only the "is this time within an available window?" predicate changes; the horizon,
  stepping, booked-overlay, and lead-time logic are reused.
- No `availability_slots` materialization is needed; slots stay virtual, preserving the
  current architecture and the FR-007 "changes reflected without redeploy" requirement (the
  generator reads the tables on each request).

**Behavior-preserving seed**: The migration seeds `availability_rules` with the current
hard-coded defaults — Monday–Friday `09:00–12:00` and `14:00–18:00` (i.e. the existing day
window split around the 12:00–14:00 lunch break, weekends excluded). On the first run after
deploy the public schedule is therefore identical to today's, and Paula edits from there.

**Lunch break / weekends**: These stop being special-cased constants. Weekends are simply
weekdays with no rules; the lunch gap is simply the absence of a midday window. The
`lunch-break-*`, `day-start`, `day-end` properties become seed inputs only (used by the
migration's seed values) and are no longer read at slot-generation time.

**Confirmed-booking protection (FR-010)**: Saving availability never touches the `bookings`
table, and slot generation already overlays `BOOKED` for any confirmed booking regardless of
availability — so a confirmed booking always survives an availability edit. The
"warn the teacher" behavior is implemented by having the availability-save response return
the list of upcoming **confirmed bookings that now fall outside** the saved availability, so
the UI can surface a non-blocking warning.

**Alternatives considered**:
- *Direct per-slot calendar toggling (materialized slots)* — rejected at clarification; also
  heavier (a row per slot) and loses recurrence.
- *Weekly pattern only, no exceptions* — rejected at clarification; cannot model holidays.

---

## 3. Image storage for slides

**Decision**: Store uploaded images as rows in a new `images` table (`id`, `content_type`,
`byte_size`, `data BYTEA`, `created_at`) and serve them via `GET /api/v1/images/{id}`
(authenticated). Slides reference an optional `image_id`. Uploads go through
`POST /api/v1/admin/images` (multipart), validated for content type
(`image/jpeg`, `image/png`, `image/webp`) and a max size (2 MB).

**Rationale**:
- "Simplicity First": storing bytes in PostgreSQL avoids introducing object-storage
  infrastructure (S3/R2, bucket credentials, signed URLs) for a single teacher with a modest
  number of slides — there is no present need that a bucket solves and a DB table does not.
- Evolution-ready: the `GET /api/v1/images/{id}` indirection is the seam. Swapping BYTEA for
  external object storage later changes only the image repository/serving code, not slide
  data, contracts, or the frontend.
- Reuses the existing JDBC + Flyway stack; no new dependency (Spring's `MultipartFile` is
  already available via `spring-boot-starter-web`).

**Trade-off**: Image bytes live in the primary database, growing its size and backups. Bounded
by the 2 MB/file limit and the small expected volume; revisit if the deck library grows large.

**Serving/authorization**: Image fetch requires authentication (covered by
`anyRequest().authenticated()`). Image ids are unguessable UUIDs and content is teaching
material, so per-image share-scoping on the raw bytes is not enforced in v1 (documented);
the share gate is enforced on the *presentation* a student can open.

**Alternatives considered**:
- *External-URL-only* — rejected at clarification (broken-link risk, no upload UX).
- *Local filesystem storage* — needs a writable volume and a path config that differs between
  local/single-instance/prod; rejected as more operational surface than DB BYTEA for now.

---

## 4. Homework: shared model → per-student assignment

**Decision**: Introduce a `homework_targets` join table (`assignment_id`, `user_id`,
unique together). A homework item is visible to a student **iff** a `homework_targets` row
links them. `LearningService` switches from `findPublishedAssignments()` (all students) to
`findAssignmentsForUser(userId)` (join through `homework_targets`), then LEFT-JOINs the
student's `homework_submissions` row exactly as today (missing row ⇒ `PENDING`, overdue
derived). `homework_submissions` is unchanged.

**Rationale**:
- Minimal additive change: the existing `homework_assignments` and `homework_submissions`
  tables, the submission upsert flow, the status lifecycle (`PENDING → SUBMITTED`, `REVIEWED`
  reserved), and the overdue derivation are all preserved. Only "who sees it" changes, and
  that is exactly what a targets table expresses.
- Satisfies FR-018 / clarification "switch fully to per-student": because visibility now
  requires a target row, nothing is shared-to-all. The three placeholder assignments seeded
  in `V5` simply have no targets, so they become teacher-owned drafts that no student sees —
  no destructive migration is required, and Paula can assign or delete them from the panel.

**Teacher view of submissions**: Admin homework listing joins `homework_assignments →
homework_targets → users` and LEFT-JOINs `homework_submissions` to show, per assignee,
status + submitted response (FR-015). `REVIEWED` remains reserved/read-only in this feature.

**Alternatives considered**:
- *Keep shared + add per-student (dual model)* — rejected at clarification; two visibility
  paths to test and reconcile.
- *Per-student copies of the assignment row* — denormalized, complicates editing instructions
  once; rejected in favor of one assignment + N targets.

---

## 5. Presentations — per-student decks vs. the existing intro

**Decision**: The new teacher-authored decks are a **separate** concept from the existing
`learning_presentation` intro blocks (the read-only "Cómo son mis clases" class-offering
intro from feature 005, which stays as-is). New tables:
- `presentations` (`id`, `title`, timestamps),
- `presentation_slides` (`id`, `presentation_id`, `heading`, `body`, `image_id?`, `sort_order`),
- `presentation_shares` (`presentation_id`, `user_id`, unique together).

A student sees a deck only if a `presentation_shares` row links them. The student learning
overview gains a list of shared decks (id + title); a deck is opened/paged via a dedicated
endpoint that returns its slides only when shared with the caller (otherwise 404).

**Rationale**:
- The 005 intro is a single global block list with different semantics (always shown to every
  student, no title/slides/sharing). Overloading it would entangle two unrelated lifecycles.
  A clean separate model keeps each simple (Simplicity First) and leaves the intro untouched.
- Sharing reuses the same join-table pattern as homework targets, keeping the codebase
  consistent.

**Scope guard (FR-026)**: slides are heading + text + optional single image, ordered. No
animations, transitions, themes, charts, or embedded media beyond one static image. Enforced
by the schema (no fields for them) and validation.

**Full-screen preview/present**: A frontend-only concern — the slide viewer component renders
one slide at a time with keyboard/next-prev paging; "presenting" during a class is Paula
screen-sharing this viewer. No realtime/remote-control server feature (per Assumptions).

---

## 6. API surface, routing, and naming

**Decision**:
- All teacher endpoints live under `/api/v1/admin/**` (role-gated). Student-facing additions
  extend the existing `/api/v1/learning/**` and add `/api/v1/images/{id}`.
- Frontend backoffice route is `/panel` (Spanish "Panel de control"), gated like
  `/aprendizaje` but additionally requiring `role === 'ADMIN'` (redirect non-admins to `/`).
- The panel uses an in-page tabbed layout (Disponibilidad / Tareas / Presentaciones) so the
  three areas are one cohesive screen (FR-004).

**Rationale**: A single `/api/v1/admin` prefix makes the security matcher trivial and the
authorization boundary obvious. `/panel` mirrors the established auth-gated route pattern
(`aprendizaje.tsx`) and the conditional-nav pattern in `SiteHeader`.

**Error codes** added to `GlobalExceptionHandler` (following the existing `{error,message}`
convention): `ACCESS_DENIED` (403), `PRESENTATION_NOT_FOUND` (404),
`STUDENT_NOT_FOUND` (404), `INVALID_IMAGE` (422, bad type/size), and reuse of
`VALIDATION_ERROR` for field validation.

---

## Summary of decisions

| Area | Decision |
|------|----------|
| Admin identity | `users.role` column; role JWT claim; `hasRole("ADMIN")` on `/api/v1/admin/**`; `AdminBootstrap` promotes `app.scheduling.teacher-email` |
| `/me` | `UserResponse` gains `role` for nav + route gating |
| Availability | `availability_rules` (weekly) + `availability_exceptions` (date BLOCK/OPEN); `AvailabilityService` refactored to derive from them; existing lead-time/booked safeguards retained; seeded to preserve current schedule |
| Booking protection | Availability saves never touch bookings; save response returns confirmed bookings now outside availability as a non-blocking warning |
| Images | `images` table (BYTEA), `POST /api/v1/admin/images`, `GET /api/v1/images/{id}`; type+2 MB limit |
| Homework | `homework_targets` join table; `LearningService` requires a target row; 005 tables/flow otherwise unchanged; placeholder seeds become unshared drafts |
| Presentations | New `presentations` / `presentation_slides` / `presentation_shares`; separate from 005 intro; student opens a deck only if shared |
| Routing | `/api/v1/admin/**` (role-gated); frontend `/panel` (admin-only, tabbed) |

No `NEEDS CLARIFICATION` items remain. No new third-party dependencies on either side.
