# Implementation Plan: Student Common Errors

**Branch**: `052-student-common-errors` | **Date**: 2026-09-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/052-student-common-errors/spec.md`

## Summary

Add a teacher-only **Errores frecuentes** box to the admin student profile (`/panel/alumnos/$studentId`). It shows the student's all-time count of mistakes on auto-graded homework questions. Clicking it opens a full-width list, independently of the Tareas box. The list is paginated (20 entries per page) and filterable by label and period. Each entry shows the question as the student saw it, their answer next to the correct one, the homework's current labels, and a button to open the full submission.

The back end adds one read-only endpoint, `GET /api/v1/admin/students/{id}/mistakes`. It rebuilds mistakes on each request from data that already exists: frozen submission snapshots, stored per-answer scores, and current homework labels. It reuses `ExerciseGradingService`'s existing result reconstruction, so there are **no schema changes**. The front end renders each entry with the existing `QuestionResultBlock`, so every auto-graded kind displays exactly as it does in the teacher's exercise-result dialog.

## Technical Context

**Language/Version**: Java 21 (back end), TypeScript 5 strict (front end)

**Primary Dependencies**: Spring Boot 3.5, Spring Security, `NamedParameterJdbcTemplate`, Jackson; React 19, TanStack Router, TailwindCSS 4, Shadcn UI (existing `select`, `pagination`), react-i18next

**Storage**: PostgreSQL 18. **Read-only** over `homework_submissions`, `homework_answers`, `homework_assignments.labels` and the `assignment_snapshot` JSONB. No Flyway migration.

**Testing**: JUnit 5 + Mockito service unit test, plus a Spring integration test (run in CI against a throwaway Postgres; skipped locally). There is no front-end test framework, so the UI is verified manually in a browser per the constitution and [quickstart.md](quickstart.md).

**Target Platform**: Web. Admin-only route `/panel/alumnos/$studentId`, desktop and phone widths.

**Project Type**: Web application (`back-end/` + `front-end/`)

**Performance Goals**: The section loads within the normal profile load time for a student with 100 submitted homeworks (SC-005). That means about 100 submissions × about 10 questions, reconstructed in memory per request.

**Constraints**: Read-only, with no seen/notification side effects (FR-014). Admin-only (FR-002). Shows snapshot content (FR-009) and current labels. Pages hold 20 entries.

**Scale/Scope**: A single teacher. Per-student history is in the low hundreds of submissions at most. One endpoint, one service, two front-end components, i18n in `es` / `en` / `ro`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First**: Pass. No new table, cache, dependency or background job. Mistakes are derived per request from existing data, reusing the grading service's own reconstruction (research R1, R5). The only new server-side logic is filtering, sorting and paging.
- **II. Component-Driven UI**: Pass. There are two named components, `StudentMistakesBox` (the stat card) and `StudentMistakesList` (filters, summary, entries, pagination). Both are built from Shadcn primitives and the existing `QuestionResultBlock` / `QuestionCard`.
- **III. Evolution-Ready Architecture**: Pass. Fetching is isolated in `getStudentMistakes()` in `front-end/src/lib/admin.ts`. The components never call `fetch`.
- **Development Workflow**: The UI will be verified in a running browser (quickstart scenarios 1–18). The branch follows the git-extension naming.

No violations. *Re-checked after Phase 1 design: still passing. The design added no new abstractions beyond one service and two components.*

## Project Structure

### Documentation (this feature)

```text
specs/052-student-common-errors/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0: decisions R1–R11
├── data-model.md        # Phase 1: read model (no schema changes)
├── quickstart.md        # Phase 1: manual + security validation
├── contracts/
│   └── api.md           # Phase 1: GET /api/v1/admin/students/{id}/mistakes
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks, not created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/
├── admin/
│   ├── controller/StudentAdminController.java       # MODIFIED: + GET /students/{id}/mistakes (lenient String params)
│   ├── service/StudentMistakesAdminService.java     # NEW: load → derive entries → filter → sort → page; read-only
│   └── dto/StudentMistakesResponse.java             # NEW: response + nested Summary, LabelOption, Entry records
└── learning/
    ├── repository/HomeworkSubmissionRepository.java # MODIFIED: + findAutoGradedSubmittedByUser(userId)
    │                                                #   (submission + format + current labels + title,
    │                                                #    status <> PENDING, submitted_at NOT NULL,
    │                                                #    format IN (EXERCISE, MIXED), ORDER BY submitted_at DESC)
    └── service/ExerciseGradingService.java          # UNCHANGED: reuse studentQuestionsFor(), storedProvisionalResultFor()
                                                     #   (+ AssignmentSnapshot.questionsOf / applyContent, HomeworkCompositionSupport)

back-end/src/test/java/com/kuky/backend/admin/
├── StudentMistakesAdminServiceTest.java             # NEW: unit tests for derivation, numbered expansion, filters, labels, paging clamp
└── StudentAdminControllerIntegrationTest.java       # MODIFIED: 200 admin / 403 student / 404 unknown for /mistakes

front-end/src/
├── lib/admin.ts                                     # MODIFIED: MistakePeriod, StudentMistakes types, getStudentMistakes()
├── components/admin/students/
│   ├── StudentMistakesBox.tsx                       # NEW: stat card (all-time count, expand/collapse)
│   └── StudentMistakesList.tsx                      # NEW: period + label selects, summary line, entries, pagination, empty states
├── routes/panel_.alumnos.$studentId.tsx             # MODIFIED: 4-box grid (grid-cols-2 sm:grid-cols-4), erroresExpanded state,
│                                                    #   fetch mistakes alongside profile, stacked lists (Tareas, then Errores),
│                                                    #   route entry "Abrir tarea" to ExerciseResultDialog / HomeworkReviewDialog
├── components/learning/ExerciseResult.tsx           # UNCHANGED: QuestionResultBlock reused
└── i18n/locales/{es,en,ro}.ts                       # MODIFIED: admin.studentProfile.mistakes.* keys
```

**Structure Decision**: The existing web-application split. The back end follows the admin package's `controller` → `service` → `learning` repository layering, as `StudentProfileAdminService` does. The front end keeps both new components next to the existing `StudentHomeworkBreakdown` in `components/admin/students/`.

## Implementation Notes

- **Service algorithm** (research R2–R7):
  1. Resolve the student. If there is none, throw `StudentNotFoundException`.
  2. Load qualifying submission rows, newest first.
  3. For each row: get questions from the snapshot (or the current questions for legacy rows), call `storedProvisionalResultFor(questions, submission)` for auto-only results and `studentQuestionsFor(questions)`, then pair the results with questions by id. Per question, derive the answered and wrong counts from the `QuestionResultDto` itself, so no second answers query is needed:
     - numbered single choice (`SingleChoiceItems.isNumbered(q)`): answered = `unitResults.size()`, wrong = the units where `!correct`. When `unitResults` is empty, use answered = wrong = `SingleChoiceItems.itemCount(q)`, the same N-zeros fallback as `SingleChoiceItems.itemContributions`;
     - any other auto kind: answered = 1, wrong = `correct ? 0 : 1`.

     This matches `HomeworkCompositionSupport.contributions`. The unit test must assert the equivalence, including a numbered question with no stored answer. Keep entries where wrong > 0.
  4. Build the label options from all rows (every period).
  5. Apply the period filter, then the label filter (an unknown key falls back to none).
  6. Compute the summary from the filtered rows, sort, and clamp and slice the page.
- **Answer lookup**: `storedProvisionalResultFor` already loads the answers for each submission (one query per submission, up to about 100 per request). This is acceptable at this scale (SC-005). If profiling shows otherwise, a batched `findBySubmissions` is the obvious follow-up. It is not needed now (Principle I).
- **Read-only**: Do **not** call `notificationService` anywhere in the new service (FR-014, research R8).
- **Lenient params**: The controller takes `period`, `label` and `page` as optional `String`s and normalises them (contract "Lenient parsing"). There is no type-mismatch handler in `GlobalExceptionHandler`, so this avoids Spring's default error body.
- **UI state**: `period`, `labelKey` and `page` live in the profile page and are passed to `StudentMistakesList`. Any filter change resets `page` to 1. When a response has `appliedLabel === null` while a key is selected, reset the selector to "Todas" (FR-012a). Refetch on `reloadProfile()` (FR-015).
- **i18n**: New keys go under `admin.studentProfile.mistakes`: title, expand/collapse, the summary line (`{{mistakes}} errores en {{answered}} preguntas`), period options, "Todas las etiquetas", both empty states, "Abrir tarea", and page labels. Add them in all three locale files.

## Complexity Tracking

*No Constitution Check violations. This section is intentionally empty.*
