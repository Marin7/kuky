# Implementation Plan: Quiz Terminology (Replace Placement Test)

**Branch**: `040-quiz-terminology` | **Date**: 2026-08-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/040-quiz-terminology/spec.md`

## Summary

Remove the placement test (“prueba de nivel”) and replace it with **quizzes**: standalone mixed-skill assessments (reading / writing / grammar / listening) that reuse homework question kinds and grading, but are **not** homework and are **not** on units. The teacher assigns specific students. One attempt; snapshot at start so later live edits do not change in-progress or submitted work. Results: overall % plus per-skill breakdown (no CEFR, no timers). Old `/prueba-de-nivel` redirects to `/quizzes`.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, NamedParameterJdbcTemplate, Jackson JSONB, Flyway. **No new libraries.** Extract `QuestionScoring` from `ExerciseGradingService`; new `quiz` package.
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn. **No new libraries.** Reuse homework question editors, take forms, media playback, review widgets; new quiz list/author/take routes + i18n.

**Storage**: PostgreSQL 18 — Flyway `V21`: drop all `placement_*`; create `quizzes`, `quiz_questions`, `quiz_question_options`, `quiz_assignees`, `quiz_attempts` (`quiz_snapshot` JSONB), `quiz_answers`.

**Testing**: JUnit — drop placement tests; quiz assign/take/submit/grade; snapshot isolation after edit; per-skill %; unassign; `USER` cannot take. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Auto-gradable result within 3 seconds of submit (SC-002). Snapshot is one quiz (small).

**Constraints**: No CEFR, timers, bank-transfer, speaking appointment, due dates, unit attach, retakes, or assign emails. Listening media per question. i18n es/en/ro: product term Quiz/Quizzes.

**Scale/Scope**: Single-teacher site; admin quizzes tab + student `/quizzes`; student profile quiz attempts replace placement evaluation.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. New tables instead of overloading homework. Snapshot at start (no 409 token). Drop placement rather than feature-flag it. Shared `QuestionScoring` only because two consumers exist. No notifications/due dates/CEFR.
- **II. Component-Driven UI** — PASS. Named quiz list, author, take, result, and admin review components; reuse homework question/media/review pieces; no raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Quiz API isolated in `quiz` package and `lib/quiz.ts`; grading math shared via a dedicated helper, not inlined in views.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `040-quiz-terminology`.

**Post-design re-check**: PASS — contracts replace placement endpoints; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/040-quiz-terminology/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── quiz-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V21__quizzes_replace_placement.sql              # NEW (drop placement_* + create quiz_*)

back-end/src/main/java/com/kuky/backend/
├── placement/                                      # DELETE entire package
├── learning/service/ExerciseGradingService.java    # EDIT: delegate per-kind scores to QuestionScoring
├── learning/service/QuestionScoring.java           # NEW: shared 0–1 scoring
├── quiz/                                           # NEW package (model, repo, dto, service, controller)
├── config/SecurityConfig.java                      # EDIT: /api/v1/quizzes/** STUDENT|ADMIN
├── config/GlobalExceptionHandler.java              # EDIT: drop placement handlers; add quiz codes
└── (tests: delete Placement*; add Quiz*)

front-end/src/
├── routes/prueba-de-nivel.tsx                      # EDIT: redirect → /quizzes
├── routes/quizzes.tsx                              # NEW list
├── routes/quizzes.$quizId.tsx                      # NEW take/result
├── routes/panel_.quizzes.nueva.tsx                 # NEW author
├── routes/panel_.quizzes.$quizId.tsx               # NEW edit
├── routes/robots[.]txt.ts                          # EDIT: Disallow /quizzes
├── components/placement/                           # DELETE
├── components/admin/AdminPanel.tsx                 # EDIT: quizzes tab
├── routes/panel_.alumnos.$studentId.tsx            # EDIT: quiz attempts, drop CEFR
├── components/quiz/                                # NEW student + admin UI
├── lib/placement.ts                                # DELETE
├── lib/quiz.ts                                     # NEW
├── lib/admin.ts                                    # EDIT: drop placement client; add quiz admin
├── components/SiteHeader.tsx                       # EDIT: Quizzes nav for authed non-admin
└── i18n/locales/{en,es,ro}.ts                      # EDIT: remove placement.*; add quiz.*
```

**Structure Decision**: Existing full-stack layout. New `quiz` backend package and `components/quiz`. Placement code deleted, not wrapped.

## Complexity Tracking

> No constitution violations requiring justification.
