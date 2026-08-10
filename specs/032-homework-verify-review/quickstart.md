# Quickstart: Admin Homework Verify Review

**Feature**: `032-homework-verify-review` | **Date**: 2026-08-10

Validate wrap, in-place annotation, plain ≤500 feedback, post-review edits, legacy freeze, activity parity, EXERCISE regression. Details: [contracts/homework-verify-review-api.md](./contracts/homework-verify-review-api.md), [data-model.md](./data-model.md).

## Prerequisites

1. PostgreSQL `kuky_dev` + Mailpit (see repo root `CLAUDE.md`).
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081` (applies Flyway `V16`).
3. Frontend: `npm run dev` in `front-end/` → `:8080`.
4. Admin (teacher) and student with `STUDENT` role; at least one MANUAL WRITE and one multi FREE_TEXT homework (and one MANUAL activity if testing parity).

## 1. Long-line wrap (admin + student)

1. As student, submit a FREE_TEXT (or WRITE) answer containing a long unbroken string (no spaces), e.g. 200+ `x` characters.
2. Admin → open review dialog.
3. Confirm the answer wraps inside the panel; **no horizontal scrollbar** on the answer block.
4. After review (step 2–3 below), student opens the homework — same wrap behavior.

**Expect**: SC-001 / FR-001–002.

## 2. Annotate + plain feedback (WRITE)

1. Student submits a WRITE answer (optionally with their own colors).
2. Admin opens review: annotate with color, highlight, and strikethrough; try typing in the answer — wording must not change.
3. Enter a short plain note ≤500 chars (no rich toolbar on the note).
4. Save → status REVIEWED, `reviewModel` ANNOTATED.
5. Student sees annotated answer + plain note.

**Expect**: FR-003b, FR-005, FR-007, FR-009.

## 3. Multi FREE_TEXT annotate

1. Student submits two FREE_TEXT answers.
2. Admin annotates one or both; leave feedback empty; save.
3. Student sees marks on annotated answers; no empty feedback section required.

**Expect**: FR-003a; optional empty note.

## 4. Feedback length + post-review edit

1. Try save with note >500 chars → validation error; status unchanged if first save.
2. Save valid review, then reopen as admin: change marks and note; save again.
3. Student refreshes → sees latest marks/note.

**Expect**: FR-008, FR-012, SC-004–006.

## 5. Legacy frozen

1. Use a row migrated as `LEGACY_RICH` (or create before deploy: REVIEWED with rich feedback, then migrate).
2. Admin opens it → rich feedback + answers viewable; no annotate/plain-note save.
3. Attempt PUT → `ALREADY_REVIEWED` (or equivalent frozen handling).

**Expect**: FR-010a.

## 6. MANUAL activity parity

Repeat annotate + plain note + wrap on a MANUAL activity submission; confirm student activity view shows the same.

**Expect**: FR-015.

## 7. Regression

1. EXERCISE graded + exercise-feedback still works (plain ≤2000).
2. Student cannot edit answers after REVIEWED.
3. `./gradlew test` green for updated admin/learning tests.

## Done when

- [ ] Wrap verified on admin + student for unbroken lines
- [ ] WRITE + multi annotate + ≤500 plain note work; re-edit after review works
- [ ] Legacy rich reviews frozen
- [ ] Activity parity checked
- [ ] EXERCISE path unchanged; backend tests pass
