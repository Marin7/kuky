# Quickstart: Per-Question Homework Grading

**Feature**: `033-per-question-grading` | **Date**: 2026-08-10

Manual browser validation after implementation. Details: [data-model.md](./data-model.md), [contracts/per-question-grading-api.md](./contracts/per-question-grading-api.md).

## Prerequisites

1. PostgreSQL `kuky_dev` + Flyway through `V17__per_question_grading.sql`.
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081`.
3. Frontend: `npm run dev` in `front-end/` → `:8080`.
4. Teacher (ADMIN) + at least one STUDENT account.

## 1. Author a mixed homework

1. Panel → Tareas → Nueva.
2. Confirm there is **no** Manual / Exercise radio for non-Writing types.
3. Set type AUDIO (or READ), add instructions (+ audio if available).
4. Add one structured question (e.g. single choice with answer key) and one FREE_TEXT prompt.
5. Save; reopen — both questions retain kind, order, and key.

**Expect**: Detail shows `composition: MIXED` (or equivalent UI). Legacy all-auto / all-manual still open/edit.

## 2. Student takes mixed homework

1. As student, open the assignment from Aprendizaje.
2. Answer every question on one form; submit once.
3. Confirm auto question shows correctness + answer key if wrong.
4. Confirm FREE_TEXT answer stored; status **awaiting teacher** (same as pure-manual submitted).
5. Confirm **no final** overall % (provisional auto-only indicator OK).

**Expect**: Cannot resubmit. Pure-auto homework still ends `GRADED` immediately. Pure-manual still `SUBMITTED` without score.

## 3. Teacher finalizes mixed

1. Admin submissions queue — mixed row appears with other awaiting (`SUBMITTED`) manuals.
2. Open review: see auto results + manual answer.
3. Try save without validate/invalidate → blocked.
4. Invalidate one manual answer (optionally annotate + ≤500 note); save.
5. As student, reopen: see validation mark, annotations/note, **combined** percentage (manual invalidated = incorrect).

**Expect**: Status terminal scored (`GRADED`). Re-open as teacher, flip to Validated, save — student sees updated mark and recalculated %.

## 4. Regression: WRITE + pure paths

1. WRITE homework: single rich answer; review annotate/note → `REVIEWED` (no validate required).
2. All-auto exercise: submit → immediate final %; teacher exercise-feedback still works.
3. All-manual multi FREE_TEXT: submit → review without validate/invalidate → `REVIEWED`.

## 5. Activity parity

Repeat steps 1–3 on a presentation **activity** (mixed FREE_TEXT + structured). Same submit / awaiting / finalize behavior.

## 6. Migration smoke

1. Open a pre-V17 MANUAL and EXERCISE homework without editing questions.
2. Student complete + teacher review / auto-grade as before.

**Expect**: No forced re-author; adding the other kind to either turns composition `MIXED` for **new** submissions.

## Done when

- [ ] Mixed authoring without format radio
- [ ] Mixed submit: auto keys + awaiting status + no final %
- [ ] Finalize requires validate/invalidate; combined % correct
- [ ] WRITE / all-manual / all-auto regressions pass
- [ ] Activity parity confirmed
