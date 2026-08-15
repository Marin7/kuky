# Implementation Plan: Multi-Item Opción Única

**Branch**: `038-single-choice-multi` | **Date**: 2026-08-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/038-single-choice-multi/spec.md`

## Summary

Let an **opción única** homework/activity question contain several pick-one items when the prompt includes consecutive `(1)`…`(N)` markers. The teacher configures options + one correct radio per number; the student only selects radios (stacked groups under the shared text). Each numbered item counts as its **own question** in overall `%` and fully-correct count. Questions **without** those markers — including all existing homeworks — stay classic one-list SINGLE_CHOICE. No Flyway: numbered keys live in `structure_json`; classic stays on the options table. Placement test unchanged.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, NamedParameterJdbcTemplate, Jackson JSONB. **No new libraries.** Reuse `BlankPassageParser` style for `(N)` tokens; `HomeworkCompositionSupport` for expanded averages.
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn `RadioGroup`. **No new libraries.** Mirror `MultiBlankEditor` prompt-sync.

**Storage**: PostgreSQL 18 — **no schema migration**. Classic: `homework_question_options`. Numbered: `structure_json.items`. One `homework_answers` row; `answer_json.selections`. Same for activities.

**Testing**: JUnit — marker parse; validate sequence/options; classic non-regression; grade expansion (2/3 → 67%, fullyCorrect 2, total 3); submit incomplete → `VALIDATION_ERROR`; mixed finalize expansion; activity grader parity. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Negligible — ≤20 items × small option lists per question.

**Constraints**: N = 1–20 consecutive starting at 1; radio-only student UI; no placement change; no silent score change for unmarked prompts; i18n es/en/ro; keys hidden pre-submit.

**Scale/Scope**: Single-teacher site; authoring + take + result/review for homework and presentation activities.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. No new question kind, table, or endpoint. Dual-read classic vs numbered on existing SINGLE_CHOICE; cap 20 copied from MULTI_BLANK.
- **II. Component-Driven UI** — PASS. Extend `QuestionEditorCard` + named student/result pieces (`RadioGroup` stacks); no raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Parser + contribution helper in learning services; types in `admin.ts` / `learning.ts`; activities share validation/grading mirrors.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `038-single-choice-multi`.

**Post-design re-check**: PASS — contracts document DTO/scoring deltas only; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/038-single-choice-multi/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── single-choice-multi-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/
├── learning/service/SingleChoiceMarkerParser.java     # NEW: parse (N), validate 1…N
├── learning/service/HomeworkCompositionSupport.java   # EDIT: expand contributions
├── admin/service/HomeworkAdminService.java            # EDIT: SINGLE_CHOICE numbered validate/map
├── learning/service/ExerciseGradingService.java       # EDIT: grade/strip/submit-complete/count
├── learning/service/ActivityExerciseGradingService.java
├── learning/service/ActivityStudentService.java       # EDIT: if strip/submit mirrored here
├── admin/service/ActivityAdminService.java            # EDIT: mixed finalize expansion
└── (tests under src/test/java/…)

front-end/src/
├── lib/singleChoiceMarkers.ts                         # NEW: shared parse with backend rules
├── lib/admin.ts                                       # EDIT: SingleChoiceStructure items
├── lib/learning.ts                                    # EDIT: student structure + selections
├── lib/exerciseLimits.ts                              # EDIT: MAX_SINGLE_CHOICE_ITEMS = 20
├── components/admin/homework/QuestionEditorCard.tsx   # EDIT: per-number option editors
├── components/admin/homework/SingleChoiceItemsEditor.tsx  # NEW: stacked item option lists
├── components/learning/ExerciseForm.tsx               # EDIT: stacked radios
├── components/learning/MixedHomeworkForm.tsx          # EDIT: same
├── components/learning/ExerciseResult.tsx             # EDIT: unitResults for numbered SC
└── i18n/locales/{en,es,ro}.ts                         # EDIT: prompt hint + validation copy
```

**Structure Decision**: Existing full-stack layout. Activities inherit via shared `validateAndMapQuestions` and mirrored grading. Placement packages not touched.

## Complexity Tracking

> No constitution violations requiring justification.
