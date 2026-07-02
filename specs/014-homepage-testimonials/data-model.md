# Phase 1 Data Model: Student Testimonials on Homepage

New migration: `back-end/src/main/resources/db/migration/V25__create_testimonials.sql`.

```sql
CREATE TABLE testimonials (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL UNIQUE REFERENCES users(id),
    student_name   VARCHAR(200) NOT NULL,
    text           TEXT NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'UNPUBLISHED')),
    display_order  INT NOT NULL DEFAULT 0,
    submitted_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at    TIMESTAMPTZ
);

CREATE INDEX idx_testimonials_status_order ON testimonials (status, display_order);
```

`UNIQUE(user_id)` is what enforces "at most one active testimonial per student" (FR-006) at the schema level — resubmission is an `UPDATE` of the existing row, never a second `INSERT`.

`student_name` is a **snapshot**, captured from the `users` row at submit/resubmit time — not derived via join — so a later rename or account deletion can't alter or break an already-published testimonial (Edge Cases).

---

## Entity: `Testimonial`

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK. |
| `user_id` | UUID, UNIQUE, FK → `users.id` | One row per student; enforces FR-006. |
| `student_name` | VARCHAR(200) | Snapshot of the student's full name at submission time (attribution shown publicly). |
| `text` | TEXT | The testimonial body. Validated non-empty / minimum length at the service layer (FR-010), not by a DB constraint (matches existing codebase style — validation lives in `@Valid` request DTOs / service checks, not SQL `CHECK`s on text length). |
| `status` | VARCHAR(20) | `PENDING` \| `APPROVED` \| `REJECTED` \| `UNPUBLISHED`. See state machine below. |
| `display_order` | INT | Teacher-controlled ordering among `APPROVED` testimonials on the homepage (FR-003). Default `0`; reorder endpoint reassigns sequential values, mirroring `reorderUnits`. |
| `submitted_at` | TIMESTAMPTZ | Reset to `now()` on every (re)submission. |
| `reviewed_at` | TIMESTAMPTZ, nullable | Set when the teacher transitions away from `PENDING`. Null while `PENDING`. |

**Java model** (`back-end/src/main/java/com/kuky/backend/testimonials/model/Testimonial.java`): a plain record/class mirroring the table columns, used internally by the repository/service layer (not returned directly from controllers — DTOs shape the public/admin/own-status responses per FR-004's differing needs).

**Enum** (`back-end/src/main/java/com/kuky/backend/testimonials/model/TestimonialStatus.java`):

```java
public enum TestimonialStatus {
    PENDING, APPROVED, REJECTED, UNPUBLISHED
}
```

**State transitions** (see [research.md](research.md) §4 for rationale):

```
(submit / resubmit) ──────────────────────────────► PENDING
PENDING     ──(teacher approves)──────────────────► APPROVED
PENDING     ──(teacher rejects)───────────────────► REJECTED
APPROVED    ──(teacher unpublishes)───────────────► UNPUBLISHED
UNPUBLISHED ──(teacher re-approves)───────────────► APPROVED
REJECTED    ──(teacher approves)──────────────────► APPROVED
(any status) ──(student resubmits)────────────────► PENDING  (text + student_name overwritten, submitted_at reset, reviewed_at cleared)
```

Only `status = 'APPROVED'` rows are visible via the public read endpoint (FR-009).

## Repository methods (`TestimonialRepository`, new)

| Method | Query shape | Purpose |
|--------|-------------|---------|
| `upsertByUser(userId, studentName, text)` | `INSERT ... ON CONFLICT (user_id) DO UPDATE SET student_name=:studentName, text=:text, status='PENDING', submitted_at=now(), reviewed_at=NULL` | Submit/resubmit (FR-006, FR-005). |
| `findApproved()` | `SELECT * FROM testimonials WHERE status = 'APPROVED' ORDER BY display_order` | Public homepage list (FR-001, FR-009). |
| `findByUserId(userId)` | `SELECT * FROM testimonials WHERE user_id = :userId` | Student's own status view (FR-007). |
| `findAll()` | `SELECT * FROM testimonials ORDER BY display_order` | Admin review queue + published list (FR-003). |
| `setStatus(id, status)` | `UPDATE testimonials SET status = :status, reviewed_at = now() WHERE id = :id` | Approve/reject/unpublish/re-approve. |
| `setText(id, text)` | `UPDATE testimonials SET text = :text WHERE id = :id` | Teacher edits wording (FR-003). |
| `reorder(orderedIds)` | Batch `UPDATE ... SET display_order = :position WHERE id = :id` | Teacher reorders published testimonials (FR-003), mirrors `reorderUnits`. |
| `delete(id)` | `DELETE FROM testimonials WHERE id = :id` | Teacher hard-removes a testimonial (FR-003's "remove"). |

## DTOs

- **`TestimonialResponse`** (public, `testimonials/dto`): `{ text, studentName, displayOrder }` — no `id`/`status`/`userId` exposed publicly.
- **`MyTestimonialResponse`** (student-own-status, `testimonials/dto`): `{ text, status, submittedAt }`.
- **`SubmitTestimonialRequest`** (`testimonials/dto`): `{ text: String }`, `@NotBlank` + `@Size(min=...)` validation (FR-010).
- **`TestimonialAdminResponse`** (`admin/dto`): `{ id, text, studentName, status, displayOrder, submittedAt, reviewedAt }` — full shape for the review queue.

## No changes to

- `users` table — read-only source for `student_name` at submission time; no new column added there.
- Any existing `learning`, `presentations`, `scheduling` tables — testimonials is a fully independent domain with a single new table.
