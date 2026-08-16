# Quickstart: Emojis in Homework Writing and Feedback

**Feature**: `046-homework-text-emojis` | **Date**: 2026-08-16

Manual validation guide. Contract: [homework-text-emojis-api.md](./contracts/homework-text-emojis-api.md). Data model: [data-model.md](./data-model.md).

## Prerequisites

- PostgreSQL `kuky_dev`; backend `local` profile (`:8081`); frontend `npm run dev` (`:8080`)
- Teacher (ADMIN) on `/panel` → Tareas
- One `STUDENT` account assigned a Writing homework and an auto-graded exercise (and optionally a mixed homework with a FREE_TEXT question)

## Setup

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

## Scenarios

### 1. Student Writing homework (P1)

1. As the student, open a Writing homework that is still editable.
2. Confirm the formatting bar has an emoji control next to color / highlight / strike.
3. Place the caret, insert 👍 from the grid (no categories, no search, no skin-tone step). It appears at the caret.
4. Select a word, insert 😊: the selection is replaced.
5. Apply a text color to a phrase that includes an emoji; both remain.
6. Submit. As the teacher, open the submission: emojis and formatting match.

### 2. Teacher feedback comment (P1)

1. As the teacher, open that Writing (or mixed/manual) review. The student answer `formatOnly` editor has **no** emoji control.
2. Next to the feedback comment, open the same 26-emoji grid. Insert 👏. No color/highlight/strike on the comment.
3. Save. As the student, reopen the homework: the comment shows 👏.

### 3. Exercise feedback and mixed FREE_TEXT (P1)

1. As the teacher, open a graded exercise result. Insert an emoji into the feedback note; save; student sees it on the result.
2. As the student, on a mixed homework FREE_TEXT question (plain textarea, not quiz): insert an emoji from the control next to the field; submit; teacher sees it.

### 4. Limits and paste (P2)

1. Fill a Writing answer to 2000 units; inserting another classroom emoji does nothing extra / stays at the limit; previous text remains.
2. Paste `hola 👍🏽` (skin-tone thumbs-up, not in the classroom set). It is kept after submit.
3. After submit, the writing area is read-only: no emoji insertion. Saved emojis still display.

### 5. Out of scope (sanity)

1. Quiz take/review and presentation activity write/review: no new classroom emoji control.
2. Homework authoring (create/edit questions): no emoji picker added.
