# Implementation Plan: New Exercise Types

**Branch**: `024-new-exercise-types` | **Date**: 2026-08-03 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/024-new-exercise-types/spec.md`

## Summary

Extend self-correcting **homework** exercises with four auto-gradable question kinds: **MULTI_BLANK** (typed blanks in one `___` passage), **DRAG_DROP** (word bank → equal blanks), **TABLE_FILL** (conjugation-style grid), and **MATCHING** (left↔right pairs with optional distractors). Placement test unchanged. Existing SINGLE_CHOICE / MULTI_CHOICE / FILL_BLANK keep today’s options-table shape; new kinds store structure and student answers in JSONB columns, reuse accent-exact typed matching, shuffle bank/lists client-side for students, and extend grading/result DTOs with per-unit feedback.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate, Flyway, PostgreSQL. **No new dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next. **No new DnD libraries** — click-to-place (keyboard-accessible) plus native HTML5 drag where useful.

**Storage**: PostgreSQL 18 — Flyway `V6__new_exercise_types.sql`: widen `homework_questions.kind` CHECK; add `structure_json JSONB`; add `homework_answers.answer_json JSONB`. Original three kinds keep using `homework_question_options` / `answer_text` / `homework_answer_options`.

**Testing**: Backend JUnit 5 — validation + `ExerciseGradingService` cases per new kind (partial blanks, shuffle-irrelevant ID grading, accent rules, matching distractors). Frontend: browser verification per constitution (author + take + graded review for each kind).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: N/A — typical drills ≤ 6–20 units per question; one exercise submit grades in-process.

**Constraints**: Homework-only (not placement); ≥ 2 blanks for MULTI_BLANK / DRAG_DROP; bank size == blank count; max 20 blanks / 20 matching items per side / 12×12 table (≤ 50 blank cells); single submission lock unchanged; answer key hidden pre-submit.

**Scale/Scope**: Single-teacher site; one migration; enum + validation + grading switches; admin `QuestionEditorCard` + student `ExerciseForm` / `ExerciseResult`; i18n kind labels (es/en/ro).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Extend existing homework exercise pipeline (`QuestionKind`, `HomeworkAdminService`, `ExerciseGradingService`, authoring/take UIs). JSONB for new-kind payloads avoids a forest of kind-specific tables. No placement-test work. No new npm/Java deps.
- **II. Component-Driven UI** — PASS. Named React editors/renderers per kind inside existing `QuestionEditorCard` / `ExerciseForm` / `ExerciseResult` (or small sibling components they compose).
- **III. Evolution-Ready Architecture** — PASS. Types and API clients stay in `front-end/src/lib/admin.ts` and `learning.ts`.
- **Technology Stack** — PASS. Unchanged stack.
- **Development Workflow** — PASS. Quickstart lists browser checks; branch `024-new-exercise-types`.

**Post-design re-check**: PASS — contracts extend existing admin/learning homework endpoints; data model is two JSONB columns + CHECK widen; Complexity Tracking empty.

**Result: PASS — no violations.**

## Project Structure

### Documentation (this feature)

```text
specs/024-new-exercise-types/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── exercise-types-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V6__new_exercise_types.sql

back-end/src/main/java/com/kuky/backend/learning/
├── model/QuestionKind.java                    # EDIT: + MULTI_BLANK, DRAG_DROP, TABLE_FILL, MATCHING
├── model/HomeworkQuestion.java                # EDIT: structureJson
├── model/HomeworkAnswer.java                  # EDIT: answerJson
├── repository/HomeworkQuestionRepository.java # EDIT: persist/read structure_json
├── repository/HomeworkAnswerRepository.java   # EDIT: persist/read answer_json
├── service/ExerciseGradingService.java        # EDIT: grade + student DTO + result units
├── service/BlankPassageParser.java            # NEW: parse exact ___ tokens
└── dto/…                                      # EDIT: ExerciseQuestionDto, SubmitExerciseRequest,
                                               #       ExerciseResultResponse (unitResults)

back-end/.../admin/
├── service/HomeworkAdminService.java          # EDIT: validateAndMapQuestions per new kind
└── dto/HomeworkQuestionDto.java               # EDIT: structure field for new kinds

front-end/src/
├── lib/admin.ts                               # EDIT: QuestionKind + AdminQuestion.structure
├── lib/learning.ts                            # EDIT: kinds, answer payloads, unit results
├── components/admin/homework/
│   ├── QuestionListEditor.tsx                 # EDIT: kind options
│   ├── QuestionEditorCard.tsx                 # EDIT: branch editors / compose sub-editors
│   ├── MultiBlankEditor.tsx                   # NEW (or inline)
│   ├── DragDropEditor.tsx                     # NEW
│   ├── TableFillEditor.tsx                    # NEW
│   └── MatchingEditor.tsx                     # NEW
├── components/learning/
│   ├── ExerciseForm.tsx                       # EDIT: render + submit new kinds
│   ├── ExerciseResult.tsx                     # EDIT: per-unit feedback
│   ├── MultiBlankQuestion.tsx                 # NEW
│   ├── DragDropQuestion.tsx                   # NEW
│   ├── TableFillQuestion.tsx                  # NEW
│   └── MatchingQuestion.tsx                   # NEW
└── i18n/locales/{es,en,ro}.ts                 # EDIT: kind labels + validation copy
```

**Structure Decision**: Stay inside existing `learning` + `admin` homework packages and the current authoring / take-exercise UI surfaces. Placement `QuestionKind` untouched.

## Complexity Tracking

> No constitution violations requiring justification.
