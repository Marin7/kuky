# Phase 0 Research: Mi aprendizaje — Classes & Homework Tab

All open questions from the spec were resolved during `/speckit-clarify` (Session 2026-06-10). This document records the resulting technical decisions and the one item the spec deferred to planning (the tab label/route). No `NEEDS CLARIFICATION` markers remain.

---

## Decision 1 — Tab label and route

- **Decision**: Label the nav entry **"Mi aprendizaje"**; mount it at route **`/aprendizaje`**, placed after **Reservas** and before **Mi cuenta** in the primary nav.
- **Rationale**: "Mi aprendizaje" (my learning) clearly communicates a personal space spanning both classes and homework, parallels the existing Spanish nav labels ("Sobre mí", "Recursos", "Reservas", "Mi cuenta"), and the `/aprendizaje` slug matches the file-based routing convention used by every other page.
- **Alternatives considered**: "Mis clases" (too close to the existing "Reservas"/booking concept and ignores homework); "Mi panel"/"Dashboard" (anglicism and too generic for a Spanish teaching site).

## Decision 2 — Where access control is enforced

- **Decision**: Enforce on **three layers**: (a) `SiteHeader` conditionally renders the "Mi aprendizaje" link only when authenticated; (b) the `/aprendizaje` route checks auth client-side and **redirects guests to `/cuenta`**; (c) every `/api/v1/learning/**` endpoint is **server-side authenticated** and returns `401` to guests.
- **Rationale**: Hiding the link (a) is UX; the redirect (b) stops direct-URL access (FR-003); server auth (c) is the real security boundary (FR-002) and prevents API abuse regardless of the UI. Because the whole section is private, the backend needs **no new `SecurityConfig` rule** — `/api/v1/learning/**` already falls under the existing `anyRequest().authenticated()` clause. This is strictly simpler than `resources`, which had to add a `permitAll` for its public catalogue.
- **Alternatives considered**: Server-only enforcement (would flash a broken/empty page to guests before the 401); a TanStack `beforeLoad` SSR guard reading the cookie (the auth cookie is `HttpOnly` and the app already standardizes on client-side `getMe()` in route components — matching that pattern keeps things consistent and avoids SSR cookie plumbing).

## Decision 3 — Modeling "shared definitions, per-student state"

- **Decision**: Store homework **definitions** once in `homework_assignments` (shared, seeded) and each student's **state** in `homework_submissions` with `UNIQUE(user_id, assignment_id)`. The overview query **LEFT JOINs** assignments to the caller's submissions; a missing submission row is presented as status **`PENDING`** with no response. Past classes and presentation are shared-only (no per-student table) this iteration.
- **Rationale**: Directly implements the clarified requirement (FR-015) with the minimal schema. No submission rows are seeded, so every student starts with all homework `PENDING` and isolated from others; a row is created lazily on first submit (upsert). This mirrors the `entitlements` per-user pattern from `resources` without copying definitions per user.
- **Alternatives considered**: Seeding a submission row per (student × assignment) — wasteful and needs backfill for new signups; a single denormalized table mixing definition + state — breaks the "shared definition" requirement and duplicates instructions per student.

## Decision 4 — Homework status lifecycle and overdue

- **Decision**: `HomeworkStatus` enum = `PENDING`, `SUBMITTED`, `REVIEWED`, persisted as `VARCHAR` with a `CHECK` constraint. The API only allows the student to transition **`PENDING` → `SUBMITTED`** (and idempotent re-submit while `PENDING`/`SUBMITTED`); it **rejects** any attempt once `REVIEWED` (`409 SUBMISSION_NOT_ALLOWED`). `REVIEWED` is set only by the future backoffice. **Overdue** is computed in `LearningService` as `dueDate != null && dueDate < today(teacherZone) && status == PENDING` and returned as a boolean field — never stored.
- **Rationale**: Encodes FR-014/FR-009 exactly. Keeping `REVIEWED` in the schema now makes the future backoffice forward-compatible without a migration. Deriving overdue avoids a stored field that would drift as the clock advances. Reusing the teacher timezone (`app.scheduling.teacher-timezone`, default `Europe/Madrid`) for the "today" boundary keeps date semantics consistent with the scheduling feature.
- **Alternatives considered**: A stored `overdue` boolean (goes stale, needs a job); a separate `done` boolean independent of status (redundant with `SUBMITTED`); allowing free transitions (would let a student overwrite a teacher's `REVIEWED`).

## Decision 5 — Teacher presentation content source

- **Decision**: Serve presentation content from a seeded **`learning_presentation`** table as an ordered list of blocks (`heading`, `body`, `sort_order`), read-only to students and returned inside the same `GET /api/v1/learning` payload.
- **Rationale**: Matches the clarified "backend-seeded" decision (Q4) and keeps the future backoffice able to author/reorder blocks by writing the same table. Bundling it into the single overview response avoids an extra round-trip for a one-screen section.
- **Alternatives considered**: Static front-end copy (rejected during clarification); a single-row single-`TEXT` blob (less flexible for the backoffice to structure into headed sections; an ordered block list costs nothing more here).

## Decision 6 — API shape: one overview GET + one submit mutation

- **Decision**: `GET /api/v1/learning` returns the whole section for the caller (`presentation`, `pastClasses`, `homework`). `PUT /api/v1/learning/homework/{assignmentId}` upserts the caller's submission (optional `response`) and returns the updated homework item. Both authenticated.
- **Rationale**: The section is a single screen, so one aggregate GET keeps the frontend simple (one fetch, one loading/empty path) and mirrors how `reservas`/`recursos` views hydrate. A `PUT` on the assignment id is idempotent and naturally upserts the per-user submission. Returning the updated item lets the UI refresh in place without a full refetch (and the view can also refetch the overview if preferred).
- **Alternatives considered**: Separate GETs per area (more round-trips, no benefit at this scale); `POST .../submissions` (less idempotent than a `PUT` keyed by assignment id).

## Decision 7 — Conditional nav visibility in `SiteHeader`

- **Decision**: Make `SiteHeader` auth-aware by calling `getMe()` in a `useEffect` and including the "Mi aprendizaje" entry only when a user is present; render the rest of the nav unchanged. While auth is resolving, the protected link is simply absent (no layout shift beyond the link appearing).
- **Rationale**: `SiteHeader` lives in `__root.tsx` and is the only place the nav is defined, so it is the correct single point to gate the link (FR-002). Reusing `getMe()` keeps auth logic in `lib/auth.ts` and consistent with `reservas`/`recursos`.
- **Alternatives considered**: Lifting auth state into `__root.tsx` and passing it down (more plumbing than needed for one link); a global auth context/provider (worth doing eventually, but YAGNI for a single conditional entry — noted as a future refactor).

## Decision 8 — No new dependencies; dates via `Intl`

- **Decision**: Reuse the platform `Intl.DateTimeFormat("es", …)` for past-class dates and homework due dates, matching the scheduling UI. No date library, no backend additions.
- **Rationale**: Constitution "Simplicity First"; the rest of the app already formats Spanish dates this way.

---

## Summary of resolved unknowns

| Topic | Resolution |
|-------|-----------|
| Tab label / route | "Mi aprendizaje" / `/aprendizaje` (Decision 1) |
| Access control layers | Hidden link + route redirect + server `401` (Decision 2) |
| Shared vs per-user data | `homework_assignments` shared + `homework_submissions` per-user (LEFT JOIN → default `PENDING`) (Decision 3) |
| Status lifecycle / overdue | `PENDING→SUBMITTED` student-driven; `REVIEWED` reserved; overdue derived (Decision 4) |
| Presentation content | Seeded `learning_presentation` blocks in the overview payload (Decision 5) |
| API shape | One `GET /learning` + `PUT /learning/homework/{id}` (Decision 6) |
| Nav visibility | Auth-aware `SiteHeader` via `getMe()` (Decision 7) |
| Dependencies | None added; `Intl` for dates (Decision 8) |
