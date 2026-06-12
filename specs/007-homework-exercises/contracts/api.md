# Phase 1 API Contracts: Homework Exercises & Self-Correction

Extends the existing teacher-only `/api/v1/admin/homework` endpoints and the student
`/api/v1/learning` endpoints. Auth, cookie handling, and the `{error,message}` error envelope are
unchanged from the established conventions. Admin endpoints require `ROLE_ADMIN`
(`401` guest / `403` student). Learning endpoints require an authenticated user; an exercise the
caller is not assigned returns `404`.

Shared JSON shapes (TypeScript-style; backend uses `record` DTOs):

```ts
type HomeworkFormat = "MANUAL" | "EXERCISE";
type QuestionKind   = "SINGLE_CHOICE" | "MULTI_CHOICE" | "FILL_BLANK";
type HomeworkType   = "AUDIO" | "READ" | "WRITE" | "GRAMMAR";   // existing skill tag
type HomeworkLevel  = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";  // existing
type HomeworkStatus = "PENDING" | "SUBMITTED" | "REVIEWED" | "GRADED";
```

---

## Teacher (admin) — authoring

### Question shape (teacher view — includes the answer key)

```ts
// For SINGLE_CHOICE / MULTI_CHOICE: options are the selectable choices, `correct` marks the key.
// For FILL_BLANK: options carry the accepted answers (each `correct: true`); the student never
// sees them. `label` = accepted text.
interface AdminQuestion {
  id?: string;             // present on read/edit; omitted on create
  kind: QuestionKind;
  prompt: string;
  options: { id?: string; label: string; correct: boolean }[];
}

interface AdminAssignee {
  userId: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  username: string | null;
  status: HomeworkStatus;
  responseText: string | null;   // manual homework
  submittedAt: string | null;
  scorePercent: number | null;   // NEW — set for GRADED exercise submissions
}

interface HomeworkAdminItem {
  id: string;
  title: string;
  instructions: string;
  dueOn: string | null;          // YYYY-MM-DD
  homeworkType: HomeworkType | null;
  level: HomeworkLevel | null;
  format: HomeworkFormat;        // NEW
  questions: AdminQuestion[];    // NEW — empty for MANUAL
  assignees: AdminAssignee[];
}
```

### `GET /api/v1/admin/homework`
Unchanged path. Each item now includes `format` and `questions` (with the answer key) and each
assignee includes `scorePercent`.

### `GET /api/v1/admin/homework/{id}`  *(NEW)*
Returns one `HomeworkAdminItem` (the edit page loads questions via this).
- `404 ASSIGNMENT_NOT_FOUND` if missing.

### `POST /api/v1/admin/homework`  *(extended)*
Request:
```ts
{
  title: string;
  instructions: string;
  dueOn: string | null;
  homeworkType: HomeworkType | null;
  level: HomeworkLevel | null;
  format: HomeworkFormat;            // NEW
  questions: AdminQuestion[];        // NEW — required non-empty when format==="EXERCISE", else []
  assigneeIds: string[];
}
```
- `201` → created `HomeworkAdminItem`.
- `400 VALIDATION_ERROR` when exercise validation fails (see data-model validation rules), e.g.
  `EXERCISE` with no questions, single-choice without exactly one correct option, fill-blank with no
  accepted answer, or `MANUAL` carrying questions.

### `PUT /api/v1/admin/homework/{id}`  *(extended)*
Same body as create minus `assigneeIds`. Replaces the assignment's fields **and** its questions
(full replace, transactional). Existing `GRADED` submissions are preserved and not re-graded
(research §8).
- `200` → updated `HomeworkAdminItem`; `400 VALIDATION_ERROR`; `404 ASSIGNMENT_NOT_FOUND`.

### `PUT /api/v1/admin/homework/{id}/assignees`  *(unchanged)*
### `DELETE /api/v1/admin/homework/{id}`  *(unchanged)*

---

## Student (learning) — taking an exercise

### `GET /api/v1/learning` *(extended)*
Each `homework[]` item gains:
```ts
format: HomeworkFormat;       // NEW
scorePercent: number | null;  // NEW — present when status === "GRADED"
```
The existing fields (`id, title, instructions, dueOn, homeworkType, level, status, response,
submittedAt, overdue`) are unchanged. The list does **not** include questions.

### `GET /api/v1/learning/homework/{id}`  *(NEW)* — fetch an exercise to take or review
Returns the exercise **without the answer key**:
```ts
interface StudentQuestion {
  id: string;
  kind: QuestionKind;
  prompt: string;
  // present only for SINGLE_CHOICE / MULTI_CHOICE; omitted/empty for FILL_BLANK
  options: { id: string; label: string }[];   // NO `correct`
}

interface ExerciseResponse {
  id: string;
  title: string;
  instructions: string;
  format: "EXERCISE";
  status: HomeworkStatus;          // PENDING (not yet taken) or GRADED (locked)
  questions: StudentQuestion[];
  result: ExerciseResult | null;   // present iff status === "GRADED" (see below)
}
```
- `404` if the homework is not assigned to the caller, not found, or `format !== EXERCISE`.
- When `status === "GRADED"`, `result` is populated so the locked exercise re-renders with feedback
  (FR-014/020) and the client renders inputs read-only.

### `PUT /api/v1/learning/homework/{id}/answers`  *(NEW)* — submit & auto-grade
Request:
```ts
interface SubmitExerciseRequest {
  answers: {
    questionId: string;
    selectedOptionIds: string[];   // choice questions; [] otherwise
    answerText: string | null;     // fill-blank; null otherwise
  }[];
}
```
Response `200`:
```ts
interface ExerciseResult {
  scorePercent: number;            // 0–100, rounded
  fullyCorrectCount: number;
  totalQuestions: number;
  questions: {
    questionId: string;
    score: number;                 // 0..1 (per-question; fractional for MULTI_CHOICE)
    correct: boolean;              // score === 1
    correctOptionIds: string[];    // for choice — revealed post-submit (FR-014)
    acceptedAnswers: string[];     // for fill-blank — revealed post-submit
  }[];
}
```
Errors:
- `400 SUBMISSION_NOT_ALLOWED` if the homework `format !== EXERCISE` (manual homework uses the
  existing `PUT /learning/homework/{id}` free-text endpoint).
- `409 SUBMISSION_NOT_ALLOWED` if a `GRADED` submission already exists (single-submission lock,
  FR-020).
- `404` if not assigned to the caller / not found.
- `400 VALIDATION_ERROR` for malformed answers (unknown question id, oversized text).

### `PUT /api/v1/learning/homework/{id}` *(unchanged)* — manual free-text submit
The existing endpoint stays for `MANUAL` homework only. If called on an `EXERCISE` it returns
`400 SUBMISSION_NOT_ALLOWED`.

---

## Error codes (additions / reuse)

| Code | HTTP | When |
|------|------|------|
| `VALIDATION_ERROR` | 400 | Exercise authoring validation; malformed answers (existing code) |
| `SUBMISSION_NOT_ALLOWED` | 400 / 409 | Wrong format for the endpoint; already-graded lock (existing exception, reused) |
| `ASSIGNMENT_NOT_FOUND` | 404 | Unknown/ unassigned homework (existing) |
| `ACCESS_DENIED` | 403 | Non-admin hitting `/api/v1/admin/**` (existing) |

No new exception types are required beyond reusing `SubmissionNotAllowedException` and
`AssignmentNotFoundException`.
