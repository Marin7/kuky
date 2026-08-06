# Implementation Plan: True/False Homework Exercises

**Branch**: `027-true-false-homework` | **Date**: 2026-08-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/027-true-false-homework/spec.md`

## Summary

Add a new auto-gradable homework exercise question kind **TRUE_FALSE**: a rich-parity statement prompt with exactly two fixed choices (true then false, never shuffled), one correct answer, graded like single-choice via existing option rows. No new JSON structure shape, no placement-test changes, no teacher explanation field. Implementation extends `QuestionKind`, widens the DB `kind` CHECK, tightens authoring validation to exactly two options / one correct, and adds a dedicated authoring UI (no free-form option editing) plus a student take branch that reuses single-choice selection/submit.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate, Flyway, PostgreSQL. **No new dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next. **No new dependencies.**

**Storage**: PostgreSQL 18 — Flyway `V9__true_false_homework.sql`: widen `homework_questions.kind` CHECK to include `'TRUE_FALSE'`. Answer key stored as two rows in `homework_question_options` (same as SINGLE_CHOICE). `structure_json` stays `{}`; student answers use `selected_option_ids` / `homework_answer_options` (not `answer_json`).

**Testing**: Backend JUnit 5 — `HomeworkAdminService` validation (exactly 2 options, exactly 1 correct, reject empty prompt) + `ExerciseGradingService` correct/incorrect/unanswered. Frontend: browser verification (author → assign → take → grade → review; mix with another kind; regression on existing kinds).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: N/A — binary grade per question; existing submit path.

**Constraints**: Homework-only (not placement); fixed option order true→false; no shuffle; no explanation field; prompts use the same plain `prompt` TEXT field as other exercise kinds (see research); single-submission lock unchanged; answer key hidden pre-submit.

**Scale/Scope**: Single-teacher site; one migration; enum + validation + grading switches; admin `QuestionEditorCard` + student `ExerciseForm`; i18n kind + true/false labels (es/en/ro).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Model TRUE_FALSE as a constrained choice kind on the existing options pipeline; do not add boolean columns, structure JSON, or rich-text prompt infrastructure.
- **II. Component-Driven UI** — PASS. Small dedicated authoring branch (or sibling) inside `QuestionEditorCard`; student render reuses RadioGroup pattern from SINGLE_CHOICE inside `ExerciseForm`.
- **III. Evolution-Ready Architecture** — PASS. Types stay in `front-end/src/lib/admin.ts` and `learning.ts`.
- **Technology Stack** — PASS. Unchanged stack.
- **Development Workflow** — PASS. Quickstart lists browser checks; branch `027-true-false-homework`.

**Post-design re-check**: PASS — contracts extend existing admin/learning homework endpoints; data model is CHECK widen only; Complexity Tracking empty.

**Result: PASS — no violations.**

## Project Structure

### Documentation (this feature)

```text
specs/027-true-false-homework/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── true-false-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V9__true_false_homework.sql

back-end/src/main/java/com/kuky/backend/learning/
├── model/QuestionKind.java                    # EDIT: + TRUE_FALSE
├── service/ExerciseGradingService.java        # EDIT: grade + student options + correctOptionIds
└── (dto/repository unchanged in shape)

back-end/.../admin/
└── service/HomeworkAdminService.java          # EDIT: validateOptions for TRUE_FALSE

front-end/src/
├── lib/admin.ts                               # EDIT: QuestionKind + "TRUE_FALSE"
├── lib/learning.ts                            # EDIT: QuestionKind + "TRUE_FALSE"
├── components/admin/homework/
│   ├── QuestionEditorCard.tsx                 # EDIT: KINDS + fixed two-option editor
│   └── questionDefaults.ts                    # EDIT: defaultOptionsForKind
├── components/learning/
│   └── ExerciseForm.tsx                       # EDIT: TRUE_FALSE render (≈ SINGLE_CHOICE)
└── i18n/locales/{es,en,ro}.ts                 # EDIT: kind + true/false labels
```

**Structure Decision**: Stay inside existing `learning` + `admin` homework packages and current authoring / take-exercise UI surfaces. Placement `QuestionKind` untouched. `ExerciseResult` needs no kind-specific branch if options + `correctOptionIds` work as for SINGLE_CHOICE.

## Complexity Tracking

> No constitution violations requiring justification.
