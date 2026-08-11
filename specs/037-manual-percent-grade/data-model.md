# Data Model: Percentage Grades for Manual Answers

**Feature**: `037-manual-percent-grade` | **Date**: 2026-08-11

## Entities

### Manual answer score (persisted on answer row)

| Field | Type | Rules |
|-------|------|-------|
| `teacher_score_percent` | `INT NULL` | 0–100 inclusive when set; `NULL` = teacher has not scored this answer yet |
| `score` | `NUMERIC(4,3)` | When `teacher_score_percent` is set: `percent / 100.0` (HALF_UP, 3 dp). Used in overall mean. Unscored FREE_TEXT may remain `0` with percent `NULL` (do not treat as 0% credit until percent set). |

Applies to `homework_answers` and `activity_answers` for `FREE_TEXT` rows.

### Writing draft / grade (submission-level)

| Field | Type | Rules |
|-------|------|-------|
| `teacher_score_percent` | `INT NULL` | On `homework_submissions` only (WRITE path). Same 0–100 / NULL semantics. When finalized, also drives `score_percent` (= that value). |

Activities: no WRITE path — no submission-level percent required.

### Submission (unchanged statuses)

| Status | Meaning under this feature |
|--------|----------------------------|
| `SUBMITTED` | Awaiting teacher; may have **partial** `teacher_score_percent` values stored (teacher-only). Students must not see teacher percents. `score_percent` for overall final grade remains null. |
| `GRADED` | Every manual answer (or WRITE) has a percent; overall `score_percent` set; students see percents. |
| `PENDING` | Unsubmitted — unchanged. |
| `REVIEWED` | Legacy annotated-only path; frozen `LEGACY_RICH` rules from 032 remain; new reviews use `GRADED` + percent. |

### Overall percentage

Equal average of all question contributions:

- Auto: `score` already 0–1
- Manual: `teacher_score_percent / 100` once set
- WRITE: single `teacher_score_percent` = overall

Display: `round(mean * 100)` half-up → integer `score_percent`.

### Fully-correct count

Count of contributions equal to `1.0` (auto correct or teacher 100%). Partials excluded.

### Review extras (unchanged)

Optional annotations (`formatted` segments) and short `feedbackText` (≤500) on finalize / progress save as today.

## Relationships

```text
HomeworkSubmission 1──* HomeworkAnswer (FREE_TEXT + structured)
ActivitySubmission  1──* ActivityAnswer

teacher_score_percent lives on each FREE_TEXT answer;
WRITE uses HomeworkSubmission.teacher_score_percent instead of answer rows.
```

## State transitions

```text
SUBMITTED ──(save progress, finalize=false)──► SUBMITTED  (percents may be partial)
SUBMITTED ──(finalize=true, all percents set)──► GRADED
GRADED    ──(finalize=true, all percents set)──► GRADED    (re-edit; recalc overall)
```

Blocked: `finalize=true` with any manual answer / WRITE missing percent → `VALIDATION_ERROR`, no status change.

## Migration (V19)

1. Add `teacher_score_percent INT NULL` + CHECK `(teacher_score_percent IS NULL OR (teacher_score_percent >= 0 AND teacher_score_percent <= 100))` to `homework_answers`, `activity_answers`, and `homework_submissions`.
2. Backfill answers: `VALIDATED` → 100, `INVALIDATED` → 0; set `score = percent/100.0`.
3. Backfill WRITE graded submissions: if historical validation was stored only via `score_percent` 0/100 without answer rows, set `teacher_score_percent = score_percent` where composition/format is WRITE and status in (`GRADED`,`REVIEWED`).
4. Drop `teacher_validation` column and related CHECKs on answer tables.
5. Application code stops reading/writing `TeacherValidation`.

## Validation rules

- Percent must be whole integer 0–100 (API rejects decimals / out of range).
- Student payloads: strip `teacherScorePercent` (and hide manual graded `score` as teacher credit) while `SUBMITTED`.
- Admin payloads: always include saved percents for teacher review UI.
