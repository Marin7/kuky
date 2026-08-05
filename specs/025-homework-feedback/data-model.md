# Data Model: Teacher Feedback on Homework Submissions

## Existing entity (extended usage)

### HomeworkSubmission

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | PK |
| user_id | UUID | Student |
| assignment_id | UUID | Homework assignment |
| status | enum | `PENDING` \| `SUBMITTED` \| `REVIEWED` \| `GRADED` |
| response_text | TEXT | MANUAL answers (FormattedText JSON); unused for exercises |
| feedback | TEXT | **Writing**: FormattedText JSON when `REVIEWED`. **Exercise (this feature)**: FormattedText JSON with a single plain segment when teacher left a comment; `NULL` when none / cleared |
| score_percent | INT | Set for `GRADED` |
| submitted_at | timestamptz | |
| reviewed_at | timestamptz | Set by Writing review only; **unchanged** by exercise feedback saves |
| updated_at | timestamptz | Bumped on exercise feedback save |

No new tables or columns.

## Logical fields (API / UI)

### TeacherFeedback (exercise)

| Attribute | Rules |
|-----------|--------|
| text | Optional plain string, max **2000** characters |
| presence | Absent when `feedback` IS NULL |
| mutability | Teacher may update or clear anytime while submission is `GRADED` |
| effect on status | None — remains `GRADED` |
| effect on score | None |

Persistence encoding: `[{"text":"<plain>"}]` with no `color` / `highlight` / `strike`. Clear → SQL `NULL`.

### hasTeacherFeedback

Derived boolean for list DTOs:

- `true` when submission has non-null feedback that parses to at least one segment with non-empty visible text
- `false` otherwise (including MANUAL items without review, or GRADED with no comment)

## Relationships

```text
HomeworkAssignment (format=EXERCISE)
        │
        ▼
HomeworkSubmission (status=GRADED)
        │
        └── feedback (optional plain comment, FormattedText-encoded)
```

Writing (`format=MANUAL`) continues to use the same column with rich FormattedText and `REVIEWED` lifecycle — out of scope for behavior changes.

## Validation rules

1. Save exercise feedback only if submission exists, assignment `format = EXERCISE`, and `status = GRADED`.
2. Reject text length > 2000 (trim policy: count characters of the plain string as submitted; whitespace-only treated as clear).
3. Writing `PUT …/feedback` must remain rejected for `GRADED` / already `REVIEWED` as today.

## State transitions

Exercise feedback does **not** introduce new statuses:

```text
GRADED ──(save/update/clear feedback)──► GRADED
```

Writing path unchanged:

```text
SUBMITTED ──(save Writing feedback)──► REVIEWED
```
