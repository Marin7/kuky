# Data Model: Admin Homework Verify Review

**Feature**: `032-homework-verify-review` | **Date**: 2026-08-10

## ReviewModel (new enum / column)

Stored on `homework_submissions.review_model` and `activity_submissions.review_model` (`TEXT`, nullable).

| Value | Meaning |
|-------|---------|
| `NULL` | Not yet reviewed under either model (typically `PENDING` / `SUBMITTED`) |
| `LEGACY_RICH` | Reviewed before this feature with whole-submission rich `feedback`; **frozen** |
| `ANNOTATED` | Reviewed with in-place answer annotations + optional plain ≤500-char note; **re-editable** |

Migration (`V16`):
- `ALTER TABLE … ADD COLUMN review_model TEXT` with CHECK `(review_model IS NULL OR review_model IN ('LEGACY_RICH','ANNOTATED'))` on both submission tables.
- `UPDATE … SET review_model = 'LEGACY_RICH' WHERE status = 'REVIEWED' AND feedback IS NOT NULL` (MANUAL homework rows and all activity submissions that use MANUAL review — activity EXERCISE graded rows with plain exercise feedback should **not** be marked legacy rich; only rows that used the MANUAL rich feedback path. Practical rule: set `LEGACY_RICH` where `status = 'REVIEWED'` for homework MANUAL assignments / activity MANUAL presentations that store rich feedback. Activity `GRADED` + exercise feedback stays `NULL` / untouched.)

Recommended homework backfill:

```sql
UPDATE homework_submissions s
SET review_model = 'LEGACY_RICH'
FROM homework_assignments a
WHERE s.assignment_id = a.id
  AND a.format = 'MANUAL'
  AND s.status = 'REVIEWED'
  AND s.feedback IS NOT NULL;
```

Activity: same for MANUAL activity submissions with `status = 'REVIEWED'` and non-null feedback (not `GRADED`).

## HomeworkSubmission / ActivitySubmission (extended)

| Field | Change |
|-------|--------|
| `review_model` | **new** — see above |
| `feedback` | **semantics**: `LEGACY_RICH` → FormattedText rich essay (unchanged). `ANNOTATED` → single plain segment JSON or `NULL` if empty note (max 500 visible chars). |
| `response_text` | WRITE only: teacher may rewrite **marks** only; visible plain text immutable vs last stored. |
| `status` / `reviewed_at` | First ANNOTATED save: `SUBMITTED`→`REVIEWED`, set `reviewed_at`. Later ANNOTATED saves: stay `REVIEWED`, do not clear `reviewed_at`. |
| `updated_at` | Bumped on every successful review save. |

## FREE_TEXT answer_text (extended semantics)

| State | `answer_text` content |
|-------|------------------------|
| Student just submitted | Plain string |
| After ANNOTATED review (per answer) | FormattedText JSON array (may be single unstyled segment if teacher saved without marks) |

Validation on annotate save: `plainText(incoming segments) == plainText(current stored)` (current plain = join of segments if JSON, else raw string).

`prompt_snapshot` unchanged.

## Short Teacher Feedback (logical)

| Attribute | Rules |
|-----------|--------|
| text | Optional plain string |
| max length | **500** characters after strip |
| clear | Whitespace-only → store `NULL` |
| presence | Shown to student when non-null after review |

## Annotated Student Answer (logical)

| Attribute | Rules |
|-----------|--------|
| wording | Immutable during teacher annotate / re-edit |
| marks | `color` / `highlight` / `strike` — full teacher control (add/clear/change), same enums as existing FormattedText |
| WRITE | Mutates `response_text` |
| FREE_TEXT | Mutates `answer_text` (plain → FormattedText JSON) |

## State transitions

```text
PENDING → SUBMITTED          (student submit — unchanged)
SUBMITTED → REVIEWED         (first ANNOTATED save; set review_model=ANNOTATED)
REVIEWED (ANNOTATED) → REVIEWED   (teacher re-saves annotations / plain note)
REVIEWED (LEGACY_RICH)            (terminal for edits — view only)
```

EXERCISE `GRADED` + exercise-feedback path unchanged (`review_model` stays null).

## Validation rules

1. ANNOTATED save only if submission exists, MANUAL (homework WRITE or multi / activity MANUAL), status `SUBMITTED` or (`REVIEWED` and `review_model = ANNOTATED`).
2. Reject save if `review_model = LEGACY_RICH`.
3. Reject if plainText of annotated WRITE/answers differs from stored wording.
4. Reject plain feedback length > 500.
5. Annotated answer FormattedText uses existing color/highlight enums; empty feedback allowed.
6. Multi: annotate payload must cover known answer rows (by `questionId`, including null for orphan snapshot rows if editable by index/id as today).

## Relationships

```text
HomeworkAssignment (format=MANUAL)
        │
        ▼
HomeworkSubmission
        ├── review_model
        ├── response_text          (WRITE annotated FormattedText)
        ├── feedback               (LEGACY rich | ANNOTATED plain segment)
        └── homework_answers.answer_text  (FREE_TEXT plain or FormattedText JSON)
```

Same pattern for activity submissions / `activity_answers`.
