# Implementation Plan: Student Testimonials on Homepage

**Branch**: `014-homepage-testimonials` | **Date**: 2026-07-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/014-homepage-testimonials/spec.md`

## Summary

Logged-in students (`STUDENT`/`ADMIN` role) can submit one written testimonial about their experience; it starts `PENDING` and is invisible to everyone but the author until the teacher reviews it from the admin panel. The teacher can approve (making it publicly visible), reject, edit the text, reorder, or unpublish an already-approved one. The public homepage shows a dedicated section listing only `APPROVED` testimonials with the student's full name, and hides the section entirely when none are approved. A student has at most one active testimonial — submitting again replaces the existing one and resets it to `PENDING`; they can see their own status but can't edit/withdraw directly. The teacher gets a plain-text email when a new testimonial is submitted, reusing the existing `JavaMailSender`/`SchedulingProperties` teacher-email plumbing already used for booking notifications.

This is a new, small, self-contained domain (mirroring how `presentations` is its own top-level package rather than living under `learning`, because — unlike coursework — testimonials are read by the public, not just students): one migration, one model/repository/service, one public controller (read + submit + own-status), one admin controller (review/edit/reorder), one `SecurityConfig` addition, one new email method, and frontend additions to the homepage, the student learning area, and the admin panel.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5 (`-web`, `-security`, `-jdbc`, `-mail`), Flyway, PostgreSQL driver. **No new dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next. **No new dependencies.**

**Storage**: PostgreSQL 18. New migration `V25__create_testimonials.sql` creates a `testimonials` table (one row per student, `UNIQUE(user_id)`, snapshotted `student_name`, `status` CHECK constraint, `display_order`).

**Testing**:
- Backend: JUnit 5 + Spring Boot Test + `spring-security-test`. New tests: `TestimonialServiceTest` (submit/resubmit/approve/reject/unpublish transitions, one-per-student replace semantics), `TestimonialControllerTest` (public GET only returns `APPROVED`, submit requires `STUDENT`/`ADMIN`, validation rejects empty/too-short text), `TestimonialAdminControllerTest` (403 for non-admin, review/edit/reorder endpoints), `SecurityConfig` gating test extension for the new matcher.
- Frontend: visual verification in a running browser per the constitution (no test framework in repo) — submit a testimonial as a student, approve it as admin, confirm it appears on the public homepage; confirm the section is absent with zero approved testimonials.

**Target Platform**: Browser via TanStack Start SSR (frontend, `:8080`) + JVM server on `:8081` (backend).

**Performance Goals**: N/A — a handful of rows, no pagination or caching needed (single-teacher site).

**Constraints**: One active testimonial per student is enforced at the schema level (`UNIQUE(user_id)`), so resubmission is an `UPDATE`, never an `INSERT` — there is no history of past testimonial text kept (Simplicity First: the spec doesn't ask for an edit history). Attribution (`student_name`) is captured as a snapshot at submission/resubmission time, not joined live from `users`, so a later name change or account deletion doesn't retroactively alter or break an already-published testimonial (per the spec's Edge Cases).

**Scale/Scope**: Single-teacher site, low tens of students. One migration, one new backend package (`testimonials`), one new admin controller, one new `SecurityConfig` matcher, one new email method, one new homepage section component, one new student-area component, one new admin panel tab.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. No history/audit table, no star ratings, no photo upload, no localization pipeline — a single table with a status column, reusing the existing plain-text `JavaMailSender` pattern and the existing `SchedulingProperties.getTeacherEmail()` rather than inventing new config.
- **II. Component-Driven UI** — PASS. New `TestimonialsSection.tsx` (homepage), `MyTestimonial.tsx` (student area, composed into `LearningView`), and a new `TestimonialsTab.tsx` (admin panel) are named, reusable React components; no raw DOM manipulation.
- **III. Evolution-Ready Architecture** — PASS. All new data-fetching lives in new/extended service modules (`front-end/src/lib/testimonials.ts` for public/student calls, `front-end/src/lib/admin.ts` extended for review/reorder), never inlined in components.
- **Technology Stack** — PASS. No new dependencies; reuses Spring Security `authorizeHttpRequests`, `JavaMailSender`, React/TanStack/Tailwind/Shadcn.
- **Development Workflow** — PASS. UI changes verified in a running browser before completion; branch `014-homepage-testimonials` follows convention; no dead code.

**Result: PASS — no violations. Complexity Tracking left empty.**

## Project Structure

### Documentation (this feature)

```text
specs/014-homepage-testimonials/
├── plan.md              # This file (/speckit-plan output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── testimonials-api.md  # Phase 1 output
├── checklists/
│   └── requirements.md  # /speckit-specify output
└── tasks.md              # /speckit-tasks output (NOT created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V25__create_testimonials.sql       # new table: testimonials

back-end/src/main/java/com/kuky/backend/
├── testimonials/
│   ├── model/Testimonial.java                  # NEW
│   ├── model/TestimonialStatus.java             # NEW enum: PENDING, APPROVED, REJECTED, UNPUBLISHED
│   ├── repository/TestimonialRepository.java    # NEW: upsert-by-user, findApproved, findAll, setStatus, setText, reorder
│   ├── service/TestimonialService.java          # NEW: submit/resubmit, approve/reject/unpublish, edit, reorder, public list
│   ├── service/TestimonialEmailService.java     # NEW: sendSubmittedNotificationToTeacher()
│   ├── dto/TestimonialResponse.java             # NEW (public shape: text, studentName, displayOrder)
│   ├── dto/MyTestimonialResponse.java           # NEW (own-status shape: text, status, submittedAt)
│   ├── dto/SubmitTestimonialRequest.java        # NEW
│   └── controller/TestimonialController.java    # NEW: GET /api/v1/testimonials, POST /api/v1/testimonials, GET /api/v1/testimonials/me
├── admin/
│   ├── controller/TestimonialAdminController.java  # NEW: GET list, PUT text, POST approve/reject/unpublish, PUT reorder, DELETE
│   └── dto/TestimonialAdminResponse.java            # NEW (admin shape: id, text, studentName, status, displayOrder, submittedAt)
└── config/SecurityConfig.java                       # EDIT: add matchers (see contracts)

back-end/src/test/java/com/kuky/backend/
├── testimonials/TestimonialServiceTest.java        # NEW
├── testimonials/TestimonialControllerTest.java      # NEW
├── admin/TestimonialAdminControllerTest.java        # NEW
└── config/SecurityConfigTestimonialsGatingTest.java # NEW (or extend existing gating test)

front-end/src/
├── lib/testimonials.ts                 # NEW: getTestimonials(), getMyTestimonial(), submitTestimonial()
├── lib/admin.ts                        # EDIT: add testimonial review/edit/reorder/delete calls + types
├── components/TestimonialsSection.tsx  # NEW: homepage section, hidden when list is empty
├── components/learning/MyTestimonial.tsx  # NEW: submit form + own status, composed into LearningView
├── components/learning/LearningView.tsx  # EDIT: render <MyTestimonial />
├── components/admin/testimonials/TestimonialsTab.tsx  # NEW: review queue + published list + reorder
├── routes/index.tsx                    # EDIT: render <TestimonialsSection />
└── routes/panel.tsx                    # EDIT: register the new "Testimonios" tab
```

**Structure Decision**: Web application (existing `front-end/` + `back-end/`). New top-level backend package `testimonials/`, mirroring the existing `presentations/` package precedent for a small, self-contained content domain — not nested under `learning/`, since testimonials (unlike coursework) are read by the general public via a `permitAll()` endpoint, not gated behind `STUDENT`/`ADMIN`. The admin-side controller/DTOs stay under the existing `admin` package, matching every other admin-authored-content feature (`PresentationAdminController`, `HomeworkAdminController`).

## Complexity Tracking

> No Constitution Check violations. No entries required.
