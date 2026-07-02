# Phase 0 Research: Student Testimonials on Homepage

No open `NEEDS CLARIFICATION` markers remained after `/speckit-clarify`. This document records the design decisions made while surveying the existing codebase, so Phase 1 can proceed directly to data model and contracts.

## 1. Where the new domain lives

**Decision**: A new top-level backend package `com.kuky.backend.testimonials` (model/repository/service/dto/controller), plus a `TestimonialAdminController`/DTOs under the existing `admin` package for review actions.

**Rationale**: The codebase already has a precedent for "small, admin-curated content domain" in `com.kuky.backend.presentations`, which is its own top-level package rather than nested under `learning`. Testimonials are closer to that shape than to coursework: unlike `learning/**` (entirely gated to `STUDENT`/`ADMIN`), the testimonials *read* endpoint must be public (`permitAll()`) for the homepage, while *submission* is student-gated and *review* is admin-only. Nesting under `learning` would misrepresent it as coursework and force an awkward mix of public/gated rules inside one package.

**Alternatives considered**: Nesting under `learning/testimonials/` — rejected because `learning/**` is a single `SecurityConfig` path-prefix matcher gated to `STUDENT`/`ADMIN`; carving out a public exception inside it would fight the existing matcher instead of adding one alongside it.

## 2. Data model: one row per student, not a submission log

**Decision**: `testimonials` table with `UNIQUE(user_id)` — one row per student. Submitting again is an `UPDATE` (upsert), not an `INSERT`. Resubmission resets `status` to `PENDING` and `submitted_at` to now.

**Rationale**: Directly implements the clarified answer ("one active testimonial per student — a new submission replaces/resets the existing one"). This also mirrors the existing `HomeworkSubmissionService`/`homework_targets` upsert pattern already in the codebase (`upsert(userId, assignmentId, status, response, timestamp)`), so the shape is familiar rather than novel.

**Alternatives considered**: An append-only submissions log with a "latest" pointer — rejected as speculative (YAGNI); the spec doesn't ask for submission history, and the `UNIQUE(user_id)` + `UPDATE` approach is strictly simpler while satisfying every acceptance scenario.

## 3. Attribution is a snapshot, not a live join

**Decision**: `testimonials.student_name` stores the student's full name **at submission time**, captured from the `users` row when the `INSERT`/`UPDATE` happens. The public/admin read paths never join `users` for the display name.

**Rationale**: The spec's edge case is explicit: "What happens when a student's account is later deleted or renamed? The published testimonial text MUST remain displayable using the attribution captured at submission time." A live join would break this guarantee (a renamed or deleted account would silently change or null out a previously-published testimonial's byline). A snapshot column is the simplest way to satisfy the requirement without a soft-delete or history mechanism on `users`.

**Alternatives considered**: Live `JOIN users ON testimonials.user_id = users.id` — rejected, contradicts the stated edge-case requirement directly.

## 4. Status enum and transitions

**Decision**: New enum `TestimonialStatus { PENDING, APPROVED, REJECTED, UNPUBLISHED }` (own class, not reusing `HomeworkStatus` — different domain, different transitions). Transitions:

```
(submit)      → PENDING
PENDING       → APPROVED | REJECTED         (teacher review)
APPROVED      → UNPUBLISHED                  (teacher unpublish)
UNPUBLISHED   → APPROVED                     (teacher re-publish)
REJECTED      → APPROVED                     (teacher changes their mind)
(resubmit, any status) → PENDING             (replaces text, per one-per-student rule)
```

Only `APPROVED` is visible on the public homepage (FR-009).

**Rationale**: Mirrors the shape of `HomeworkStatus` (`PENDING, SUBMITTED, REVIEWED, GRADED`) as a precedent for "small status enum + service-layer transition methods," per the exploration of `HomeworkSubmissionService`. A dedicated enum (rather than reusing `HomeworkStatus`) keeps the two domains' vocabularies independent, since "submitted"/"graded" don't apply to testimonials and "approved"/"unpublished" don't apply to homework.

**Alternatives considered**: A boolean `published` flag instead of a 4-state enum — rejected because it can't distinguish "never reviewed" (`PENDING`) from "reviewed and rejected" (`REJECTED`) from "was published, now hidden" (`UNPUBLISHED`), all of which the spec's User Stories 2 and 3 require distinguishing (e.g., a rejected student must not be misled into thinking their submission is still pending).

## 5. Security matchers

**Decision**: Extend `SecurityConfig.filterChain` (`back-end/src/main/java/com/kuky/backend/config/SecurityConfig.java:46-55`) with:
- `GET /api/v1/testimonials` → `permitAll()` (public homepage read, approved-only filtering done in the service layer)
- `POST /api/v1/testimonials` and `GET /api/v1/testimonials/me` → `hasAnyRole("STUDENT","ADMIN")`

`/api/v1/admin/testimonials/**` needs **no new rule** — it already falls under the existing `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` matcher.

**Rationale**: Matches the codebase's sole authorization pattern (centralized `SecurityConfig` path matchers, no `@PreAuthorize` anywhere). `hasAnyRole("STUDENT","ADMIN")` (not `hasRole("STUDENT")` alone) is required because the teacher's own `ADMIN` JWT authority never includes `ROLE_STUDENT`, exactly as already established for `/api/v1/bookings` and `/api/v1/learning/**`.

**Alternatives considered**: None — this is a direct, mechanical extension of an existing, unambiguous pattern.

## 6. Email notification

**Decision**: New `TestimonialEmailService` with one method, `sendSubmittedNotificationToTeacher(String teacherEmail, String studentName)`, using `JavaMailSender` + `SimpleMailMessage` + the existing `sendQuietly`-style try/catch-and-log-on-failure wrapper, following `BookingEmailService`'s exact shape. The teacher's address is resolved via the existing `SchedulingProperties.getTeacherEmail()` bean (already injected into `BookingService`, `ScheduleController`, etc.) — no new config property.

**Rationale**: Reuses 100% existing mail infrastructure (`JavaMailSender` bean, Mailpit local dev setup, `SchedulingProperties`) with zero new configuration. Plain text matches every other transactional email in the app; the spec only asks for a teacher notification, not a student-facing email, so no second method is added (Simplicity First — avoid speculative notification types).

**Alternatives considered**: A dedicated `app.testimonials.notify-email` property — rejected as redundant; the teacher's email is already a single well-known config value (`SchedulingProperties`), and testimonials have exactly one recipient for notifications (the teacher), same as bookings.

## 7. Frontend placement

**Decision**:
- Public homepage: new `TestimonialsSection.tsx` rendered in `front-end/src/routes/index.tsx` between the Features and CTA sections, following the existing inline-`<section>` composition style of that file. Fetches via a new `front-end/src/lib/testimonials.ts` service module (same `apiCall<T>` wrapper shape as `scheduling.ts`/`learning.ts`).
- Student submission/status: new `MyTestimonial.tsx` composed into the existing `LearningView.tsx` (which already composes `PastClassesList`, `HomeworkSubmitDialog`, `LearningContent`) — reuses the page's existing `STUDENT`/`ADMIN` gating in `aprendizaje.tsx`, so no new route-level auth logic is needed.
- Admin review: new `TestimonialsTab.tsx` registered in `panel.tsx` alongside the existing Units/Homework/Presentations/Prueba de nivel tabs; calls added to the existing `front-end/src/lib/admin.ts` (which already accumulates every admin feature area in one file, per its own header comment).

**Rationale**: Every placement follows an existing, working precedent exactly — no new routing, auth, or file-organization patterns introduced.

**Alternatives considered**: A dedicated `front-end/src/lib/admin-testimonials.ts` module split out from `admin.ts` — rejected; `admin.ts` already holds every other admin feature area (students, bookings, availability, homework, presentations, units, placement) as one file with section-comment dividers, so splitting just this one out would be inconsistent, not simpler.
