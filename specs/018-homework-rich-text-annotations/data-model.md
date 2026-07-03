# Data Model: Rich Text Formatting for Writing Homework Feedback

## Formatted Text (shared value type, not a table)

A JSON array of segments, used for both a student's Writing answer and a teacher's feedback. Not a standalone entity — it is the serialized contents of the `response_text` and `feedback` columns described below.

| Field | Type | Notes |
|---|---|---|
| `text` | string | Required, non-empty per segment. Concatenation of all segments' `text` is the visible content counted against the length limit (FR-009). |
| `color` | enum \| absent | One of: `red`, `green`, `blue`, `neutral`. Text color. |
| `highlight` | enum \| absent | One of: `yellow`, `green`, `pink`. Background color. |
| `strike` | boolean \| absent | Strikethrough. |

Validation rules:
- Array MUST NOT be empty when content is required (a submitted answer, or feedback being saved as part of a review — edge case: no blank reviews).
- Total visible text length (sum of `text` across segments) MUST NOT exceed the existing 2000-character limit, for both the answer and the feedback.
- `color`/`highlight` values outside the fixed enums are rejected (400) — this is what makes FR-008 (no scripts/links/images/arbitrary markup) true by construction, since the schema has no field that can hold them.
- `color`, `highlight`, and `strike` are independent; any subset may be present on the same segment (combinable formatting, per Clarifications).

## Writing Homework Submission (extends existing `homework_submissions` row)

Existing entity (`HomeworkSubmission.java` / `homework_submissions` table), extended — no new table.

| Field | Type | Change |
|---|---|---|
| `id` | UUID | existing |
| `userId` | UUID | existing |
| `assignmentId` | UUID | existing |
| `status` | enum (`PENDING`, `SUBMITTED`, `REVIEWED`, `GRADED`) | existing; this feature is the first to actually set `REVIEWED` |
| `responseText` | text (JSON-encoded Formatted Text) | **changed semantics**: was a raw plain string, now a JSON-encoded segment array. Existing plain-text rows are migrated to the single-segment equivalent (no formatting) so they keep rendering correctly. |
| `feedback` | text (JSON-encoded Formatted Text), nullable | **new column**. Null until the teacher reviews the submission. |
| `reviewedAt` | timestamp, nullable | **new column**. Set when `feedback` is saved and `status` becomes `REVIEWED`. |
| `submittedAt` | timestamp, nullable | existing |
| `updatedAt` | timestamp | existing |

State transitions (extends existing lifecycle, no changes to existing PENDING→SUBMITTED path):

```
PENDING --(student submits formatted answer)--> SUBMITTED --(teacher saves formatted feedback)--> REVIEWED
```

- `SUBMITTED → REVIEWED` is new; requires non-empty `feedback`.
- `REVIEWED` remains terminal: the student's `responseText` and the teacher's `feedback` are both read-only once `status = REVIEWED` (FR-007). No route exists back to `SUBMITTED` or `PENDING`.
- Only Writing (`MANUAL` format) assignments use `feedback`/`reviewedAt`; `EXERCISE`-format submissions are unaffected (they use `scorePercent`/`GRADED` as today).

## Migration (`V28`)

- `ALTER TABLE homework_submissions ADD COLUMN feedback TEXT;`
- `ALTER TABLE homework_submissions ADD COLUMN reviewed_at TIMESTAMPTZ;`
- Data backfill: for every existing row where `response_text` is non-null and not already valid JSON, wrap it as `[{"text": <existing value>}]`.

## Derived field: `needsReview`

`StudentProfileHomeworkDto` (existing DTO returned by the per-student admin profile endpoint) gains:

| Field | Type | Notes |
|---|---|---|
| `needsReview` | boolean | `true` when the assignment is MANUAL-format and `status = SUBMITTED`. Backs the per-student profile indicator (FR-010). Computed, not stored. |
