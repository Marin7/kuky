# Implementation Plan: Reusable Word Bank

**Branch**: `042-reusable-word-bank` | **Date**: 2026-08-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/042-reusable-word-bank/spec.md`

## Summary

Arrastrar y soltar (DRAG_DROP) take view currently treats each bank chip as single-use (`place` clears other blanks; placed chips disable). Authoring already allows the same bank item on several blanks; grading already scores per-blank any-of. This feature detects **reusable-bank** mode when any bank id appears in ≥2 blanks’ `correctBankIds`, exposes that as a student-safe boolean `bankReusable` (never the answer key), and switches `DragDropQuestion` to copy-on-place with chips left enabled. Exclusive questions stay exactly as today. No authoring UI, i18n, or schema change. Homework, activities, and quizzes share the same take component and strip path.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Jackson JSONB `structure_json`, existing `DragDropStructureSupport`. **No new libraries.**
- Frontend: React 19, `DragDropQuestion` used by `ExerciseForm` and `MixedHomeworkForm` (homework, activities, quizzes). **No new libraries.**

**Storage**: PostgreSQL 18 — **no schema migration**. Stored `structure_json` unchanged. Student DTO `structure` for DRAG_DROP gains `bankReusable: boolean`.

**Testing**: JUnit — `bankReusable` true/false on stripped student structure; duplicate placement ids grade per-blank; exclusive strip remains `false`. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Negligible — scan ≤20 blanks × ≤10 ids per question when stripping.

**Constraints**: Answer key still stripped pre-submit (FR-008); no new student copy (clarification); exclusive UX unchanged; authoring unchanged; no Flyway.

**Scale/Scope**: One question kind on existing take surfaces; single-teacher site.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. One derived boolean + one prop on the existing take widget. No new question kind, toggle, or table. Authoring already allows shared ids.
- **II. Component-Driven UI** — PASS. Evolve named `DragDropQuestion`; callers pass `bankReusable`. No raw DOM. No new instructional strings.
- **III. Evolution-Ready Architecture** — PASS. Detection in `DragDropStructureSupport`; student types in `learning.ts`; strip in existing homework/activity student DTO builders (quizzes already reuse homework strip).
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `042-reusable-word-bank`.

**Post-design re-check**: PASS — contract adds `bankReusable` only; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/042-reusable-word-bank/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── reusable-word-bank-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/
├── learning/service/DragDropStructureSupport.java   # EDIT: isBankReusable(Resolved)
├── learning/service/ExerciseGradingService.java     # EDIT: strip DRAG_DROP + bankReusable
├── learning/service/ActivityExerciseGradingService.java  # EDIT: same strip
└── (tests: ExerciseGradingServiceTest, activity strip if present)

front-end/src/
├── lib/learning.ts                                  # EDIT: StudentStructure.bankReusable?
├── components/learning/
│   ├── DragDropQuestion.tsx                         # EDIT: exclusive vs copy-on-place
│   ├── ExerciseForm.tsx                             # EDIT: pass bankReusable
│   └── MixedHomeworkForm.tsx                        # EDIT: pass bankReusable (homework, activities, quizzes)
```

**Structure Decision**: Existing full-stack layout. Quizzes inherit via `QuizGradingService` → `ExerciseGradingService.studentQuestionsFor` and `MixedHomeworkForm`. No migrations, no i18n files.

## Complexity Tracking

> No constitution violations requiring justification.
