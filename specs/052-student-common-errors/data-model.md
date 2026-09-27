# Data Model: Student Common Errors

**Feature**: `052-student-common-errors` | **Date**: 2026-09-27

**No schema changes.** No migrations, tables or columns are added. Everything below is a **read model** built on each request from existing data (research R1).

## Source data (existing, read-only)

| Table / column | Used for |
|---|---|
| `homework_submissions` (`id`, `user_id`, `assignment_id`, `status`, `submitted_at`, `assignment_snapshot`) | Qualifying submissions: `user_id = :studentId`, `status <> 'PENDING'`, `submitted_at IS NOT NULL`. The snapshot gives the frozen title and questions. |
| `homework_assignments` (`format`, `homework_type`, `labels`, `title`) | Composition filter (`EXERCISE` / `MIXED` only), **current** labels, and the title fallback when there is no snapshot. |
| `homework_answers` (`question_id`, `score`, `answer_json`, selected options) | Per-question stored score and the student's raw answer. |
| `homework_questions` / `homework_question_options` | Question fallback only, for legacy submissions without a snapshot. |

Deleting a homework cascades to its submissions (`ON DELETE CASCADE`), so its mistakes disappear, as required by the spec's edge case.

## Read model

### MistakeEntry — one list row

One auto-graded question in one qualifying submission with **at least one** score contribution that is not fully correct (research R4).

| Field | Type | Source / rule |
|---|---|---|
| `submissionId` | UUID | `homework_submissions.id`. Used to open the homework. |
| `assignmentId` | UUID | `homework_submissions.assignment_id` |
| `homeworkTitle` | string | Title from the snapshot when present, otherwise the current title |
| `homeworkFormat` | `EXERCISE` \| `MIXED` | `homework_assignments.format`. Decides which dialog opens (research R9). |
| `labels` | string[] | **Current** `homework_assignments.labels`, exactly as saved; empty when there are none |
| `submittedAt` | instant | `homework_submissions.submitted_at` |
| `questionPosition` | int | 1-based position of the question within the homework (secondary sort key) |
| `question` | `ExerciseQuestionDto` | Student-safe question built from the **snapshot** question: prompt, options, stripped structure |
| `result` | `QuestionResultDto` | Stored score, `correct`, correct/selected option ids, `unitResults` (student vs expected display per part) |
| `mistakeCount` | int | Contributions of this question that are not fully correct: 1 for normal kinds, 1…N for numbered single choice |

**Qualifies as an entry when:**
- `HomeworkCompositionSupport.isAutoGradable(question.kind)`, and
- at least one of `HomeworkCompositionSupport.contributions(question, answer)` is not fully correct. A missing answer row counts as score 0, which is wrong; the result block shows it as "sin respuesta".

**Ordering:** `submittedAt` descending, then `questionPosition` ascending.

### MistakeSummary — for the current filters

| Field | Type | Rule |
|---|---|---|
| `mistakeCount` | int | Sum of `mistakeCount` over the entries that match the filters |
| `answeredCount` | int | Number of auto-graded contributions (all of them, right or wrong) in the qualifying submissions that match the filters |

### LabelOption

| Field | Type | Rule |
|---|---|---|
| `key` | string | `trim().toLowerCase(Locale.ROOT)`. The group identity. |
| `label` | string | First spelling seen, walking submissions newest-first |

The option set comes from **all** qualifying submissions (every period), so it stays the same when the period changes. Options are sorted by `key`. There is no unlabelled option.

### Filters and paging (request)

| Parameter | Values | Default | Rule |
|---|---|---|---|
| `period` | `ALL`, `DAYS_30`, `DAYS_90` | `ALL` | `DAYS_n`: keeps `submittedAt >= now − n days`. An unrecognised value falls back to `ALL`. |
| `label` | a `LabelOption.key` | none (= all labels) | Keeps submissions whose current labels contain that key. An unknown key is treated as none, and the response sets `appliedLabel = null`. |
| `page` | int ≥ 1 | 1 | Clamped to `[1, max(1, totalPages)]`; the response returns the page actually served |

Page size is fixed at **20 entries**.

## State transitions

None. The feature is read-only (FR-014). An entry's life is the life of its submission: it appears on submit, changes labels when the homework is relabelled, and disappears when the homework is deleted.
