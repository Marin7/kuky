# Quickstart: Rich Text Formatting for Writing Homework Feedback

## Prerequisites

- Local dev environment set up per root `CLAUDE.md` (PostgreSQL 18 `kuky_dev`, Mailpit, back-end on `:8081`, front-end on `:8080`).
- A `STUDENT`-role test account and the `ADMIN` (teacher) account (`AdminBootstrap` promotes the `app.scheduling.teacher-email` account on startup).
- At least one existing MANUAL-format homework assignment assigned to the test student (via `/panel` → Units tab, or reuse a fixture from `specs/007-homework-exercises/`).

## Setup

```bash
./gradlew flywayMigrate --args='--spring.profiles.active=local'   # applies V28
./gradlew bootRun --args='--spring.profiles.active=local'
# separate shell:
cd front-end && npm run dev
```

## Validation scenarios

### 1. Student formats their answer (User Story 2)

1. Log in as the student, open `/aprendizaje`, open the Writing homework assignment.
2. Type an answer, select a word, apply a text color; select another word, apply a highlight; select a third, apply strikethrough — confirm all three can be combined on overlapping selections.
3. Submit. Reload the page — confirm the formatting is still visible exactly as applied (proves `FormattedText` round-trips through `PUT /api/v1/learning/homework/{assignmentId}` and back via `GET /api/v1/learning`).

### 2. Teacher reviews and gives formatted feedback (User Story 1)

1. Log in as the teacher (admin). From `/panel`, confirm the submitted Writing homework appears both in the cross-student review queue and as an indicator on that student's profile page (`GET /api/v1/admin/homework/submissions?status=SUBMITTED`, and `needsReview: true` on `GET /api/v1/admin/students/{id}/profile`).
3. Open the submission, write feedback using color/highlight/strikethrough, save.
4. Confirm the submission now shows as reviewed and disappears from the review queue and per-student indicator (`PUT /api/v1/admin/homework/submissions/{submissionId}/feedback` → `status: REVIEWED`).
5. Confirm no email was sent (check Mailpit at `:8025` — inbox should be unchanged, per FR-012).

### 3. Student views feedback (User Story 3)

1. Log in as the student again, open the same homework in `/aprendizaje`.
2. Confirm both the student's original formatted answer and the teacher's formatted feedback render together with all colors/highlights/strikethroughs intact.
3. Confirm the answer and feedback are both read-only (no edit controls) — proves the `REVIEWED` terminal rule (FR-007).

### 4. Edge cases

- Attempt to save teacher feedback with an empty segment array → expect `400 VALIDATION_ERROR`.
- Attempt `PUT .../feedback` twice on the same submission → second call expects `409 ALREADY_REVIEWED`.
- Paste rich content copied from Word/Google Docs into the student answer field → confirm only plain text lands in the editor (no images/links/foreign styling carried over).
- Type an answer at exactly the 2000-character visible-text limit with heavy formatting applied → confirm the limit is enforced on visible text, not on the JSON-encoded size.

## Expected outcome

All four scenarios pass without errors, matching `spec.md`'s Acceptance Scenarios and `contracts/api.md`.
