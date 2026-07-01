# Implementation Plan: Placement Test for Potential Students

**Branch**: `011-placement-test` | **Date**: 2026-06-30 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/011-placement-test/spec.md`

## Summary

A new **placement test** lets a logged-in prospective student self-assess their Spanish level and, if they want a full evaluation, pay offline and book a normal appointment.

1. **Free auto-graded test** (US1, P1). A new student-facing page presents three independently-timed sections — **Reading**, **Audio (Listening)**, **Grammar** — built from the same auto-gradable question kinds the platform already supports (`SINGLE_CHOICE` 0/1, `MULTI_CHOICE` partial credit, `FILL_BLANK` trim+lowercase+accent-exact). Each section has a server-authoritative countdown; on manual submit with blanks the user is warned then allowed (blanks scored incorrect), and on timer expiry the section auto-submits silently. On completion the system returns a **per-skill CEFR level** (A1–C2 for each of Reading, Listening, Grammar) plus a derived overall estimate and a per-section score breakdown — all deterministic, no human involvement.

2. **Full evaluation: offline payment, Writing, booking** (US2, P2). The result page shows **static bank-transfer instructions** (no payment integration, no order/status tracking), lets any logged-in student submit a free-text **Writing** response (stored, teacher-visible, no payment gate), and links to the existing `/reservas` booking flow to book the evaluation appointment. The speaking "audition" and results delivery happen **inside that one normal appointment** — there is **no new booking entity** and **no speaking schema**.

3. **Teacher authoring & review** (US3, P2). The admin `/panel` gains a placement tab to author questions/options per section, edit per-section time limits, the writing prompt, and the bank-transfer text. The teacher can view, per student, their per-skill CEFR results alongside their Writing submission (surfaced in the existing student-detail admin view) so she can run the live appointment.

The backend adds a new **`placement` domain** that mirrors the established conventions rather than overloading the assignment/target-bound `learning` tables: plain JDBC (`NamedParameterJdbcTemplate` + `RowMapper`), POJO models, `record` DTOs, current user via `@AuthenticationPrincipal String email`, `{error,message}` error bodies. It **reuses** the generic `audio_files` table + `GET /api/v1/audio/{id}` for listening clips and the existing `bookings`/`MeetingProvider` flow for the appointment. A new `PlacementGradingService` applies the same grading rules as `ExerciseGradingService`, and a `PlacementScoringService` maps per-skill scores to CEFR bands. The frontend adds one student route, extends `/panel` and the student-detail admin page, adds placement components, and a `lib/placement.ts` service module. **No new third-party dependencies.**

## Technical Context

**Language/Version**: TypeScript 5.x strict (frontend) / Java 21 (backend).

**Primary Dependencies**:
- Frontend: React 19, TanStack Start 1.x (SSR), TanStack Router (file-based), TailwindCSS 4, Shadcn UI (Card, Button, Input, Textarea, RadioGroup, Checkbox, Label, Progress for the timer bar), Vite 7, react-i18next. Question reordering in the admin authoring UI via up/down buttons (no DnD library). **No new dependencies.**
- Backend: Spring Boot 3.5.1 (`-web`, `-security`, `-jdbc`, `-validation`, `-mail`), Flyway, PostgreSQL driver, jjwt 0.12.6. **No new dependencies.**

**Storage**: PostgreSQL 18. New migration `V19__create_placement_test.sql` adds eight `placement_*` tables (config singleton, questions, question options, attempts, attempt sections, answers, answer options, writing submissions). Reuses `audio_files`, `bookings`, `users`. No changes to existing tables.

**Testing**:
- Backend: JUnit 5 + Spring Boot Test. Unit tests for `PlacementGradingService` (single-choice exact, multi-choice partial-credit, fill-blank normalization, unanswered → 0) and `PlacementScoringService` (deterministic per-skill CEFR banding incl. boundary cases; identical answers → identical levels). Slice/integration tests for: section timer authority (submit after `deadline_at` grades unanswered as incorrect; client cannot extend time), answer key never present in the student GET payload, login required (401 `UNAUTHENTICATED` when anonymous), writing submission stored & teacher-visible, and trust-based access (writing submit / booking never blocked by a payment check).
- Frontend: visual verification in a running browser per the constitution (no test framework in repo).

**Target Platform**: Browser via TanStack Start SSR (frontend, `:8080`) + JVM server on `:8081` (backend).

**Performance Goals**: Auto-graded result rendered < 3 s after submit (SC-002); 100% deterministic per-skill levels (SC-003); full three-section test completable within per-section limits, total typically < 20 min (SC-001).

**Constraints**: Section deadlines MUST be computed and stored server-side so refresh/client edits cannot grant extra time (FR-006) — a late submit is still accepted and grades only what was sent; no payment method, order, status, reference, or amount may be introduced anywhere (FR-009, FR-012); Writing submission and appointment booking MUST NOT be gated by any payment check (FR-010, FR-011, trust-based).

**Scale/Scope**: Single-teacher site, low concurrency (tens of students). One new student route, ~2 admin surfaces extended, eight new tables, one backend domain.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. No payment machinery is built (explicitly excluded). The speaking assessment reuses the existing `bookings` flow with **no new entity** and **no schema**. Listening audio reuses the existing generic `audio_files` table. A new `placement` domain is introduced rather than contorting the assignment/`homework_targets`-bound `learning` tables, because placement questions are global (not assigned per student) and need per-section timing + per-skill CEFR — concrete present needs, not speculation. See Complexity Tracking (no violations to justify).
- **II. Component-Driven UI** — PASS. All UI is named, reusable React components under `src/components/placement/` plus Shadcn primitives; no raw DOM manipulation. The countdown timer is a component.
- **III. Evolution-Ready Architecture** — PASS. All data-fetching lives in `src/lib/placement.ts` (and extends `lib/admin.ts`), never inlined in components, matching the existing `lib/learning.ts` / `lib/scheduling.ts` pattern. Backend follows the established layered domain structure.
- **Technology Stack** — PASS. No new dependencies; reuses React 19 / TanStack / Tailwind / Shadcn (frontend) and Spring Boot 3.5 plain-JDBC (backend).
- **Development Workflow** — PASS. UI changes verified in a running browser before completion; feature branch `011-placement-test` follows the naming convention; no dead code.

**Result: PASS — no violations. Complexity Tracking left empty.**

## Project Structure

### Documentation (this feature)

```text
specs/011-placement-test/
├── plan.md              # This file (/speckit-plan output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── placement-api.md # Phase 1 output
├── checklists/
│   └── requirements.md  # /speckit-specify output
└── tasks.md             # /speckit-tasks output (NOT created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/placement/
├── controller/
│   ├── PlacementController.java         # student-facing: GET test, start/submit sections, result, writing
│   └── PlacementAdminController.java    # /api/v1/admin/placement/** authoring + config
├── service/
│   ├── PlacementService.java            # attempts, section timing, writing submission, student views
│   ├── PlacementGradingService.java     # per-question grading (mirrors ExerciseGradingService rules)
│   ├── PlacementScoringService.java     # per-skill score → CEFR banding + overall estimate
│   └── PlacementAdminService.java       # question/option/config CRUD + validation
├── repository/
│   ├── PlacementQuestionRepository.java
│   ├── PlacementAttemptRepository.java
│   ├── PlacementAnswerRepository.java
│   ├── PlacementWritingRepository.java
│   └── PlacementConfigRepository.java
├── model/                               # Skill, CefrLevel, PlacementQuestion, QuestionOption,
│                                        # PlacementAttempt, AttemptSection, PlacementAnswer,
│                                        # WritingSubmission, PlacementConfig (POJOs)
├── dto/                                 # record DTOs (student view hides answer key; admin view full)
└── exception/                           # PlacementNotFoundException, SectionStateException, ...

back-end/src/main/resources/db/migration/
└── V19__create_placement_test.sql

back-end/src/test/java/com/kuky/backend/placement/
├── PlacementGradingServiceTest.java
├── PlacementScoringServiceTest.java
└── PlacementFlowIntegrationTest.java     # timer authority, auth gate, answer-key hiding, writing visibility

front-end/src/routes/
├── prueba-de-nivel.tsx                    # NEW student route → /prueba-de-nivel (test + result + full-eval)
├── panel.tsx                              # EXTENDED: add "Prueba de nivel" admin tab
└── panel_.alumnos.$studentId.tsx          # EXTENDED: show placement results + writing for a student

front-end/src/components/placement/
├── PlacementIntro.tsx                     # login-gated intro + start
├── SectionRunner.tsx                      # one timed section: countdown + questions + warn-then-allow submit
├── PlacementQuestion.tsx                  # renders SINGLE/MULTI/FILL_BLANK + audio player
├── PlacementResult.tsx                    # per-skill CEFR + overall + breakdown + CTA
├── FullEvaluationPanel.tsx                # bank-transfer text + Writing form + "book appointment" CTA
└── admin/                                 # PlacementAuthoring (questions/options/config/writing/payment text)

front-end/src/lib/
└── placement.ts                           # NEW typed fetch service (credentials:'include')
```

**Structure Decision**: Web application (existing `front-end/` + `back-end/`). The backend adds one self-contained `placement` domain package following the same controller/service/repository/model/dto/exception layering as `learning` and `scheduling`. The frontend adds one route, two extended surfaces, a component folder, and one service module — mirroring how feature 007 (homework exercises) was structured.

## Complexity Tracking

> No Constitution Check violations. No entries required.
