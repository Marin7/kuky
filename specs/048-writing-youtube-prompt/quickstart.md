# Quickstart: Writing Homework YouTube Prompt

**Feature**: `048-writing-youtube-prompt` | **Date**: 2026-09-20

Manual, runnable validation for this feature. Run the app as usual (`docker-compose up` or your normal dev setup: backend on `:8081`, frontend on `:8080`) and sign in as a teacher (ADMIN) and, separately, as a student.

## Prerequisites

- A teacher (ADMIN) account and at least one student account already assigned to each other (as used by other homework specs in this repo).
- A valid public YouTube URL for testing (any short, embeddable video), e.g. `https://www.youtube.com/watch?v=dQw4w9WgXcQ`.

## Scenario 1 — Teacher attaches a video, student sees it embedded (US1, US2)

1. As the teacher, create a new homework: type **Writing**, fill title + instructions, paste the test YouTube URL into the new video field, save.
2. **Expect**: save succeeds; reopening the homework for edit shows the same URL still set.
3. As the assigned student, open the homework from `/aprendizaje`.
4. **Expect**: an in-page embedded, playable YouTube player appears directly below the instructions text and above the answer box — not a bare link, and the answer box is still fully usable below it.
5. Resize the browser to a phone width.
6. **Expect**: the embed shrinks to fit the width; no horizontal scroll or broken layout.

## Scenario 2 — Video stays optional; invalid URL is rejected (US1)

1. As the teacher, create another Writing homework with no video. Save.
2. **Expect**: saves successfully, exactly as before this feature.
3. Edit it, paste a non-YouTube URL (e.g. a plain webpage link) into the video field, try to save.
4. **Expect**: a clear validation error; save is blocked until the field is fixed or cleared.

## Scenario 3 — Teacher preview while authoring (US3)

1. While editing a Writing homework, paste a valid YouTube URL into the video field without saving yet.
2. **Expect**: an embedded preview of that video appears in the editor immediately.
3. Clear the field.
4. **Expect**: the preview disappears.

## Scenario 4 — Existing writing homeworks are unaffected (US4)

1. Open a Writing homework created before this feature shipped (no video ever set), as both teacher and student.
2. **Expect**: no video field/area appears anywhere; layout and editing behave exactly as before.

## Scenario 5 — Freeze on submit (US5, FR-011, SC-006)

1. As the teacher, create a Writing homework with the test video, assign it to two students (A and B).
2. As student A, submit an answer.
3. As the teacher, edit the homework: change the video to a different YouTube URL (or remove it), save.
4. Reopen student A's submission as the teacher (review) and as student A (their own view).
5. **Expect**: both still show the *original* video from before the edit, not the new one.
6. As student B (who has not submitted), open the homework.
7. **Expect**: student B sees the *updated* video (or no video, if it was removed).

## Done criteria

All six scenarios above pass. Combined with `back-end` unit/integration tests (`HomeworkAdminServiceTest`, `HomeworkFreezeSubmittedIntegrationTest`) covering the same cases at the API layer, this satisfies SC-001 through SC-006 in the spec.
