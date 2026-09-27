# Research: Student Common Errors

**Feature**: `052-student-common-errors` | **Date**: 2026-09-27

No `NEEDS CLARIFICATION` items remained in the Technical Context. The decisions below came from reading the existing grading, snapshot and admin-profile code.

## R1 — Where mistakes come from: derive on read, no new table

- **Decision**: Rebuild mistakes each time from data that already exists: `homework_submissions` (status, `submitted_at`, `assignment_snapshot`), `homework_answers` (stored `score`, `answer_json`, selected options) and `homework_assignments.labels`. Add no table, column or migration.
- **Rationale**: Grading already stores a score for every auto-graded answer. `ExerciseGradingService.buildStoredResult(..., provisionalAutoOnly = true)` rebuilds the auto-only result, including per-unit `studentDisplay` / `expectedDisplay`, from the frozen snapshot. That is exactly what FR-009 (show what the student saw) and FR-003 (auto-graded questions only) need. Deriving on read meets FR-015 for free: relabelling, deleting a homework (`homework_submissions.assignment_id … ON DELETE CASCADE`) and new submissions all show up immediately. It also follows Principle I (Simplicity First).
- **Alternatives considered**:
  - A `student_mistakes` table filled on submit. Rejected: it duplicates grading output, needs a backfill for past submissions, and has to be kept in sync with relabels and deletes.
  - A SQL-only aggregation. Rejected: correctness for numbered single-choice items and the per-unit displays live in Java (`SingleChoiceItems`, `QuestionScoring`). Doing it in SQL would re-implement grading.

## R2 — Which submissions count

- **Decision**: Take the student's `homework_submissions` where `status <> 'PENDING'`, `submitted_at IS NOT NULL`, and the assignment's composition is `ALL_AUTO` or `MIXED` (format `EXERCISE` / `MIXED`). This includes `MIXED` submissions still `SUBMITTED` and awaiting the teacher, because their auto-graded answers were already scored when submitted. Only question kinds where `HomeworkCompositionSupport.isAutoGradable(kind)` is true are considered. That excludes FREE_TEXT, WRITE and all-manual homework (FR-003).
- **Rationale**: The auto part of a MIXED homework is final at submit time. Waiting for the teacher's manual grade would hide real mistakes for no reason.
- **Alternatives considered**: `GRADED` only. Rejected for the reason above.
- **Out of scope** (FR-003a): `activity_submissions` and quiz attempts are never read.

## R3 — Question shown: the snapshot, not current content

- **Decision**: Load questions with `AssignmentSnapshot.questionsOf(submission)` when a snapshot exists, and fall back to `questionRepository.findByAssignment` for pre-snapshot legacy rows. This is the same fallback `ExerciseGradingService.questionsForResult` already uses.
- **Rationale**: Covers FR-009 and the "homework edited after submission" edge case, with the same behaviour as the teacher's existing exercise-result dialog.
- **Title**: Take the homework title from the snapshot as well (via `assignmentSnapshot.applyContent`), so it matches what the student saw. **Labels** are always read from the current `homework_assignments.labels` (spec: "current labels").

## R4 — Unit of a "mistake" vs unit of a list entry (numbered single choice)

- **Decision**:
  - **Counting** (box count, summary, FR-004): one mistake per score contribution that is not fully correct, using `HomeworkCompositionSupport.contributions(question, answer)`. A numbered `(1)…(N)` single-choice question therefore contributes N answered items and one mistake per wrong item, the same way the homework score counts them. `answeredCount` counts all contributions.
  - **Listing and paging** (FR-005, FR-011): one **entry** per question row with at least one wrong contribution. A numbered question with 2 wrong items out of 5 is a single entry. The existing result block marks each item right or wrong inside it, which satisfies FR-007 ("which parts were wrong"). Pages hold 20 entries.
- **Rationale**: `QuestionResultBlock` / `NumberedInlineSingleChoiceResult` already render a numbered question as one unit with per-item marks. Splitting one question into N separate cards would mean building synthetic sub-questions in both the API and the UI, for no clear benefit to the teacher. Counts stay consistent with the homework score (SC-003).
- **Spec alignment**: The spec's "numbered single-choice items" edge case says each wrong item counts as its own mistake, and that is how counting works. The spec has been updated to say these items are *shown* in one entry.

## R5 — Rendering: reuse `QuestionResultBlock`

- **Decision**: For each entry the API returns the same two shapes the exercise-result dialog already uses: `ExerciseQuestionDto` (student-safe question: prompt, options, stripped structure) and `ExerciseResultResponse.QuestionResultDto` (score, correct option ids, selected ids, `unitResults` with `studentDisplay` / `expectedDisplay`). The front end renders each entry with the existing exported `QuestionResultBlock` (`front-end/src/components/learning/ExerciseResult.tsx`), passing `showAllAnswers`.
- **Rationale**: Every auto-graded kind (choice, true/false, blanks, drag-drop, table, matching, numbered items) already displays the student's answer next to the correct answer, with per-part marks, and has been tested in production. There is no second renderer to keep in step with it (Principle II).
- **Alternatives considered**: A new flattened `{studentAnswer: string, correctAnswer: string}` DTO. Rejected: it loses the per-part marks (FR-007) and duplicates per-kind formatting logic.

## R6 — Filtering and pagination: server-side

- **Decision**: One admin endpoint, `GET /api/v1/admin/students/{id}/mistakes?period=&label=&page=`. The server:
  1. loads the qualifying submissions (R2);
  2. computes every entry;
  3. applies the period filter (`submitted_at >= now − 30/90 days`) and the case-insensitive label filter;
  4. sorts (`submitted_at` desc, then question position);
  5. slices out page N of 20.

  It returns the page together with the summary counts, the all-time mistake count (for the collapsed box), the label options and the paging info.
- **Rationale**: Grading and snapshot reconstruction happen in Java, so the full set is computed server-side anyway. Returning one page keeps the payload small for students with a long history (SC-005). Per-student data volume is small (≈100 submissions × ≈10 questions), so computing in memory on each request is fine and needs no cache.
- **Alternatives considered**: Return everything and filter and paginate in the browser, as feature 051 does. Rejected: each entry carries full question structures, which makes the payload much heavier than 051's homework list, and the counts would still need the full set.
- **Out-of-range page**: The server clamps `page` to `[1, totalPages]` (with `totalPages ≥ 1`) and returns the page it actually served. That covers the spec edge case "current page no longer exists → last available page".
- **Label no longer present**: If the requested `label` matches none of the student's label options, the server ignores it, treats the request as "all labels" and returns `appliedLabel: null`. The UI then resets its selector (FR-012a fallback).

## R7 — Label options and case-insensitivity

- **Decision**: Label options are the distinct labels, compared case-insensitively (`toLowerCase(Locale.ROOT)` after trim), on the student's *qualifying* submissions (R2), across all time. This set does not depend on the period filter, so the dropdown stays stable while the period changes. Each option is `{key, label}`: `key` is the group key (trimmed, lower-cased) and `label` is the first spelling seen while walking submissions newest-first. This mirrors `uniqueLabels` / `labelGroupKey` in `front-end/src/lib/homeworkLabels.ts` ("first-seen wins"). Options are sorted by `key`. The client sends `label=<key>`, and the server compares keys. There is no "unlabelled" option.
- **Rationale**: Mirrors the case-insensitive grouping rules for the Homework tab label filter (spec 043 FR-010a) and this spec's FR-012a. The server lower-cases with `Locale.ROOT` and the front end with `"es"`, which give the same result for Spanish and Romanian letters. The front end only echoes back keys the server issued, so the two never need to agree on lower-casing a new string.

## R8 — Security

- **Decision**: The endpoint lives under `/api/v1/admin/**`, which `SecurityConfig` already restricts to `ADMIN` (FR-002, SC-004). No student-facing endpoint is added. If `{id}` is unknown, the endpoint returns 404 `STUDENT_NOT_FOUND` through the existing `StudentNotFoundException`, the same as `GET /students/{id}/profile`.
- **Read-only** (FR-014): The service must not call `notificationService.markHomeworkSeen` or any other mutator. Unlike `HomeworkAdminService.getExerciseResult`, it only reads. Opening a homework from an entry reuses the existing dialogs, which keep their current seen and notification behaviour.

## R9 — Opening the homework from an entry

- **Decision**: Reuse the profile page's existing routing. Format `EXERCISE` opens `ExerciseResultDialog(submissionId)`. `MIXED` opens `HomeworkReviewDialog(submissionId)`, because `getExerciseResult` rejects MIXED submissions that are not yet `GRADED`, and the Tareas list already sends MIXED to the review dialog.
- **Rationale**: FR-008 asks for "the same view already available from the student's Tareas list".

## R10 — Profile layout

- **Decision**: Add a 4th stat box to the top grid in `panel_.alumnos.$studentId.tsx`: `grid-cols-2 sm:grid-cols-4`, which is 2×2 on phones and one row of 4 on wider screens. The box has its own `erroresExpanded` state, independent of `tareasExpanded` (FR-001a). Both expanded lists render below the grid in a fixed order: the Tareas list first, then the Errores frecuentes list.
- **Data loading**: The page fetches `page=1, period=ALL, label=null` next to the profile. `allTimeMistakeCount` feeds the collapsed box. The same response is the list's first page, so expanding the box needs no further request.
- **Freshness**: Refetch on profile reload (`reloadProfile`) and whenever a filter or the page changes (FR-015).

## R11 — Testing approach

- **Back-end**: A unit test for the new service's pure logic, covering entry derivation, numbered expansion, the period/label filters, case-insensitive label options, sorting and page clamping. It follows the `StudentProfileAdminServiceTest` style with mocked repositories. A controller integration test (in the style of `StudentAdminControllerIntegrationTest`, run in CI against Postgres) checks that an admin gets 200, a student or user gets 403, and an unknown id gets 404.
- **Front-end**: No test framework is configured. Verification is manual in the browser, per the constitution (see `quickstart.md`).
