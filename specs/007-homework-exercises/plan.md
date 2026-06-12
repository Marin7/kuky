# Implementation Plan: Homework Exercises & Self-Correction

**Branch**: `007-homework-exercises` | **Date**: 2026-06-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-homework-exercises/spec.md`

## Summary

Enhance the existing teacher-authored homework so that:

1. **Authoring moves off the dialog onto dedicated full pages** (US1). The current
   `HomeworkEditorDialog` is replaced by full-screen routes `/panel/tareas/nueva` (create)
   and `/panel/tareas/:id` (edit), both rendering one shared `HomeworkEditorPage`. The page
   keeps every existing field (title, instructions, skill type, level, due date, assignees) and
   becomes the home for the new exercise-authoring controls. The dialog component is deleted (no
   dead code).

2. **Homework gains a `format`: `MANUAL` or `EXERCISE`** (US2/US4). `MANUAL` is the existing
   free-text-response homework, unchanged; all pre-existing rows default to `MANUAL`. An
   `EXERCISE` carries an ordered list of **questions**, each of kind `SINGLE_CHOICE` (radio,
   scored 0/1), `MULTI_CHOICE` (checkboxes, partial credit) or `FILL_BLANK` (one blank, text
   match). Choice questions have options flagged correct; fill-blank questions have an answer key
   of accepted answers. The teacher records the answer key while authoring.

3. **Students take exercises on a dedicated page and get automatic feedback** (US3). A new student
   route `/aprendizaje/tarea/:id` renders the questions (no answer key sent), and on submit the
   backend auto-grades, persists per-question scores, and returns per-question correctness plus the
   correct answer for wrong questions and an overall percentage with a fully-correct count. The
   submission is single-shot and locks read-only (status `GRADED`). Manual homework keeps its
   existing dialog + `PENDING → SUBMITTED → REVIEWED` review flow untouched.

The backend extends the existing `learning` domain (new `homework_questions`,
`homework_question_options`, `homework_answers`, `homework_answer_options` tables; `format`
column on `homework_assignments`; `score_percent` column and a new `GRADED` status on
`homework_submissions`) and the teacher-only `admin` homework service/DTOs. A new
`ExerciseGradingService` performs grading. All work mirrors the established conventions: plain JDBC
(`NamedParameterJdbcTemplate` + `RowMapper`), POJO models, `record` DTOs, current user via
`@AuthenticationPrincipal String email`, `{error,message}` errors. The frontend adds two admin
routes, one student route, the editor/taking components, and extends `lib/admin.ts` /
`lib/learning.ts`. **No new third-party dependencies.**

## Technical Context

**Language/Version**: TypeScript 5.x strict (frontend) / Java 26 (backend).

**Primary Dependencies**:
- Frontend: React 19, TanStack Start 1.x (SSR), TanStack Router (file-based), TanStack Query,
  TailwindCSS 4, Shadcn UI (Card, Button, Input, Textarea, Select, Checkbox, RadioGroup, Label),
  Vite 7. Question reordering via up/down buttons (no DnD library). **No new dependencies.**
- Backend: Spring Boot 4.0.x (`-web`, `-security`, `-jdbc`, `-validation`), Flyway, PostgreSQL
  driver, jjwt. **No new dependencies.**

**Storage**: PostgreSQL 18. New migration `V15__create_homework_exercises.sql`:
`format` column on `homework_assignments`; `score_percent` column + extended status CHECK
(`+GRADED`) on `homework_submissions`; four new tables. Reuses `homework_assignments`,
`homework_submissions`, `homework_targets`, `users`.

**Testing**:
- Backend: JUnit 5 + Spring Boot Test. Unit tests for `ExerciseGradingService` (single-choice
  exact match, multi-choice partial-credit fractions, fill-blank trim/case-insensitive/accent-exact,
  unanswered → 0, overall percent + fully-correct count), `HomeworkAdminService` (exercise
  validation: ≥1 question, single-choice exactly-one-correct, multi-choice ≥1 correct, fill-blank
  ≥1 accepted answer; persistence round-trip of questions/options). Slice/integration tests for the
  single-submission lock (second submit → 409/`SUBMISSION_NOT_ALLOWED`), answer-key never present in
  the student GET payload, and manual-homework regression (free-text flow unchanged).
- Frontend: visual verification in a running browser per the constitution (no test framework in repo).

**Target Platform**: Browser via TanStack Start SSR (frontend) + JVM server on `:8081` (backend).

**Performance Goals**: Author a 2-question exercise to save in < 5 min (SC-001); 100% of exercise
submissions return immediate feedback with zero teacher action (SC-002); grading agrees with the
answer key in 100% of defined-rule cases (SC-005). Datasets are tiny (one teacher, low-hundreds
students, a handful of questions per exercise); grading is in-memory over a few rows — well within
standard request latency.

**Constraints**:
- The answer key MUST never reach the student before submission (FR-011): the student GET endpoint
  omits `isCorrect` and accepted answers; they appear only in the post-submit result.
- A self-correcting exercise accepts exactly one submission, then locks read-only (FR-020).
- Manual homework and pre-existing homework behave exactly as today (FR-017/018); `REVIEWED` stays
  the manual-review terminal state, `GRADED` is the new auto terminal state.
- Fill-blank matching: trim + case-insensitive + accent-exact (FR-009).
- Multi-choice scoring is partial credit; single-choice and fill-blank are 0/1 (FR-013).
- All copy Spanish; dates in `"es"` locale.

**Scale/Scope**: One teacher, dozens–low-hundreds of students; exercises with a handful of
questions. 1 migration, ~4 new backend repository/service/dto clusters in `learning` + extensions to
`admin` homework, 3 new frontend routes (~6 new components), extensions to two API clients.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I — Simplicity First | ✅ PASS | Reuses the existing `homework_assignments`/`homework_submissions`/`homework_targets` tables and the per-student visibility model; adds a `format` discriminator and child tables only where structured questions genuinely require them. One unified `homework_question_options` table serves both choice options and fill-blank accepted answers (answer key), branched by question kind in the grader. No JPA, no DnD/quiz libraries, no new dependencies. Mixed auto+manual homework and multi-blank prompts are explicitly out of scope. |
| II — Component-Driven UI | ✅ PASS | `HomeworkEditorPage`, `QuestionListEditor`, `QuestionEditorCard`, `HomeworkExercisePage`, `ExerciseResult` are named Shadcn-based components; no raw DOM manipulation. The dead `HomeworkEditorDialog` is deleted, not commented out. |
| III — Evolution-Ready Architecture | ✅ PASS | All API calls stay isolated in `lib/admin.ts` / `lib/learning.ts`; grading lives behind `ExerciseGradingService`; data access behind repositories. Adding a new question kind or retake mode later needs no contract reshaping. |
| Technology Stack | ✅ PASS | Frontend and backend stacks unchanged; plain JDBC + Flyway continued. No additions. |
| Development Workflow | ✅ PASS | UI browser-verified before completion; branch `007-homework-exercises` follows convention; the replaced dialog is removed. |
| Backend location | ✅ PASS | New code under `back-end/` and `front-end/` only. |

**Gate result**: All principles satisfied. No complexity violations requiring justification —
Complexity Tracking section intentionally empty.

## Project Structure

### Documentation (this feature)

```text
specs/007-homework-exercises/
├── plan.md              # This file
├── research.md          # Phase 0 — schema shape, grading rules, answer-key hiding, status model, routing
├── data-model.md        # Phase 1 — format column, questions/options/answers tables, status/score
├── quickstart.md        # Phase 1 — end-to-end validation of US1–US4
├── contracts/
│   └── api.md           # Phase 1 — admin homework (extended) + learning exercise endpoints
├── checklists/
│   └── requirements.md  # Spec quality checklist (from /speckit-specify)
└── tasks.md             # Phase 2 — task list (/speckit-tasks output, NOT created here)
```

### Source Code (repository root)

```text
kuky/
├── front-end/
│   └── src/
│       ├── routes/
│       │   ├── panel_.tareas.nueva.tsx              # NEW: admin full-page create (renders HomeworkEditorPage)
│       │   ├── panel_.tareas.$homeworkId.tsx        # NEW: admin full-page edit (renders HomeworkEditorPage)
│       │   └── aprendizaje_.tarea.$homeworkId.tsx   # NEW: student full-page exercise take + result
│       ├── components/
│       │   ├── admin/homework/
│       │   │   ├── HomeworkAdminList.tsx            # UPDATED: "Nueva tarea"/"Editar" navigate to routes; show format badge
│       │   │   ├── HomeworkEditorPage.tsx          # NEW: full-page editor (fields + format toggle + questions)
│       │   │   ├── QuestionListEditor.tsx          # NEW: add/reorder/remove questions (EXERCISE only)
│       │   │   ├── QuestionEditorCard.tsx          # NEW: per-question kind/prompt/options/answer-key
│       │   │   ├── StudentMultiSelect.tsx          # UNCHANGED (reused)
│       │   │   └── HomeworkEditorDialog.tsx        # DELETED
│       │   └── learning/
│       │       ├── HomeworkItemCard.tsx            # UPDATED: EXERCISE → "Empezar ejercicio" link to take page; show score when GRADED
│       │       ├── HomeworkExercisePage.tsx        # NEW: renders questions + submit + result
│       │       └── ExerciseResult.tsx              # NEW: score + per-question feedback + correct answers
│       └── lib/
│           ├── admin.ts                            # UPDATED: format + questions in HomeworkAdminItem/create/update; assignee scorePercent
│           └── learning.ts                         # UPDATED: HomeworkItem.format; getExercise(id); submitExercise(id, answers)
└── back-end/
    └── src/
        ├── main/
        │   ├── java/com/kuky/backend/
        │   │   ├── learning/
        │   │   │   ├── model/
        │   │   │   │   ├── HomeworkFormat.java          # NEW enum MANUAL|EXERCISE
        │   │   │   │   ├── QuestionKind.java            # NEW enum SINGLE_CHOICE|MULTI_CHOICE|FILL_BLANK
        │   │   │   │   ├── HomeworkAssignment.java      # UPDATED: format field
        │   │   │   │   ├── HomeworkSubmission.java      # UPDATED: scorePercent field
        │   │   │   │   ├── HomeworkQuestion.java        # NEW POJO (+ options)
        │   │   │   │   ├── QuestionOption.java          # NEW POJO
        │   │   │   │   └── HomeworkAnswer.java          # NEW POJO (+ selected option ids)
        │   │   │   ├── repository/
        │   │   │   │   ├── ContentRepository.java       # UPDATED: read/write format
        │   │   │   │   ├── HomeworkQuestionRepository.java  # NEW: questions+options CRUD per assignment
        │   │   │   │   └── HomeworkAnswerRepository.java    # NEW: persist + read per-question answers
        │   │   │   ├── service/
        │   │   │   │   ├── HomeworkItems.java           # UPDATED: include format in HomeworkItemResponse
        │   │   │   │   ├── HomeworkSubmissionService.java   # UNCHANGED manual path; rejects EXERCISE
        │   │   │   │   ├── ExerciseGradingService.java  # NEW: grade + persist + build result
        │   │   │   │   └── LearningService.java         # UPDATED: expose exercise fetch
        │   │   │   ├── controller/LearningController.java   # UPDATED: GET /homework/{id}, PUT /homework/{id}/answers
        │   │   │   ├── dto/
        │   │   │   │   ├── HomeworkItemResponse.java    # UPDATED: format, scorePercent
        │   │   │   │   ├── ExerciseResponse.java        # NEW: questions w/o answer key (+ existing result if GRADED)
        │   │   │   │   ├── ExerciseQuestionDto.java     # NEW (student view: options without isCorrect)
        │   │   │   │   ├── SubmitExerciseRequest.java   # NEW: answers[]
        │   │   │   │   └── ExerciseResultResponse.java  # NEW: overall + per-question feedback
        │   │   │   └── exception/SubmissionNotAllowedException.java  # REUSED for the single-submission lock
        │   │   └── admin/
        │   │       ├── dto/
        │   │       │   ├── CreateHomeworkRequest.java   # UPDATED: format + questions[]
        │   │       │   ├── UpdateHomeworkRequest.java   # UPDATED: format + questions[]
        │   │       │   ├── HomeworkAdminItem.java       # UPDATED: format + questions[] (with answer key)
        │   │       │   ├── HomeworkQuestionDto.java     # NEW (teacher view: options with correct flag)
        │   │       │   └── AssigneeDto.java             # UPDATED: scorePercent
        │   │       └── service/HomeworkAdminService.java # UPDATED: validate + persist questions
        │   └── resources/
        │       └── db/migration/
        │           └── V15__create_homework_exercises.sql  # format col, GRADED status, score col, 4 tables
        └── test/java/com/kuky/backend/
            ├── learning/ExerciseGradingServiceTest.java
            └── admin/HomeworkExerciseAdminServiceTest.java
```

**Structure Decision**: Extend the existing two-service layout rather than introduce new packages.
The structured-question model lives inside the established `learning` domain alongside the homework
tables it extends; the teacher-only authoring stays in the existing `admin` homework service/DTOs.
Grading is isolated in one `ExerciseGradingService` so the rules (partial credit, accent-exact
match) are unit-testable in one place. The frontend uses flat `panel_`/`aprendizaje_` route files
(matching the existing `panel_.alumnos.$studentId` pattern) so authoring and taking each get a real
full page outside the tabbed panel / learning layout. No new top-level projects, infrastructure, or
dependencies.

## Complexity Tracking

> No constitution violations — section intentionally empty.
