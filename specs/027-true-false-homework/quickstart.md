# Quickstart: True/False Homework Exercises

**Feature**: `027-true-false-homework` | **Date**: 2026-08-06

Manual validation guide after implementation. Details: [data-model.md](./data-model.md), [contracts/true-false-api.md](./contracts/true-false-api.md).

## Prerequisites

1. PostgreSQL `kuky_dev` running; Mailpit optional.
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081` (Flyway applies `V9__true_false_homework.sql`).
3. Frontend: `npm run dev` in `front-end/` → `:8080`.
4. Admin (teacher) account and at least one STUDENT assignee.

## 1. Author and take true/false (P1)

1. Admin → Homework → new EXERCISE → add question kind **True / False** (or locale equivalent).
2. Enter a statement prompt; mark **True** (or **False**) as correct. Confirm there is no add/remove options UI and order is always True then False.
3. Save, assign student. As student, open exercise: see statement + two localized choices in that order; no answer key visible.
4. Select the correct choice and submit.
5. Expect score contribution, GRADED status, read-only reopen; wrong answers on a separate attempt/student show the correct true/false value (no explanation text).

**Fail checks**: Empty prompt → cannot save. Leaving unanswered → that question scores 0.

## 2. Mix with another kind (P2)

1. Same exercise: add one TRUE_FALSE and one SINGLE_CHOICE (or MULTI_BLANK).
2. Student completes both and submits → overall % and fully-correct count include both.
3. Results page shows both questions graded.

## 3. Regression — existing kinds & placement

1. Open an existing EXERCISE without TRUE_FALSE; take + submit still grades as before.
2. Placement-test admin/student flows unchanged (no TRUE_FALSE there).

## Backend smoke (optional)

```bash
cd back-end
./gradlew test --tests '*ExerciseGrading*' --tests '*HomeworkAdmin*'
```

Expect TRUE_FALSE validation/grading tests green once implemented.

## Done when

- TRUE_FALSE author → take → grade → review works in the browser (es/en/ro labels ok).
- Mixed exercise and legacy exercises unaffected; placement test unaffected.
- Spec acceptance scenarios and success criteria remain satisfied.
