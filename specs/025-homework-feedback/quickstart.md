# Quickstart: Teacher Feedback on Homework Submissions

Validate end-to-end after implementation. See [contracts/exercise-feedback-api.md](./contracts/exercise-feedback-api.md) and [data-model.md](./data-model.md).

## Prerequisites

1. PostgreSQL `kuky_dev` running; backend with `local` profile.
2. Seed or create at least one **EXERCISE** homework assigned to a student, with a **GRADED** submission.
3. Admin (teacher) account and that student account available.

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

## Automated checks

```bash
# back-end/
./gradlew test --tests '*HomeworkAdmin*' --tests '*Exercise*'
```

Expect coverage for: save/update/clear exercise feedback on GRADED; reject over-length; reject non-GRADED / MANUAL; Writing feedback path still works.

## Browser validation

### Teacher (P1)

1. Open **Panel → Homework** (or student profile homework list).
2. Find a **GRADED** exercise assignee → confirm **no** feedback indicator yet.
3. **View result** → enter plain text → save → reopen → text still there.
4. List now shows feedback-present indicator for that assignee.
5. Edit text → save → updated text shown.
6. Clear text → save → feedback gone; indicator gone.
7. Paste > 2000 characters → save rejected; previous feedback unchanged.
8. Open a Writing submission review → confirm existing rich feedback flow unchanged.

### Student (P2)

1. As the student, open **Aprendizaje** homework list → graded item shows feedback indicator.
2. Open the exercise → score/results as before **plus** teacher plain-text comment.
3. Graded exercise with no teacher comment → no indicator; result view has no empty feedback block.

## Done when

- [ ] Admin can save/update/clear plain feedback on GRADED exercises only
- [ ] Indicators appear/disappear on student and teacher lists
- [ ] Student sees comment on result view
- [ ] Writing feedback unchanged
- [ ] Backend tests green; UI verified in browser
