# Quickstart: Multi-Question Manual Homework

**Feature**: `031-manual-multi-questions` | **Date**: 2026-08-10

Validate authoring, student layout (instructions → audio → questions), required answers, review with snapshots, WRITE/EXERCISE regression, and MANUAL activity parity. Details: [contracts/manual-multi-questions-api.md](./contracts/manual-multi-questions-api.md), [data-model.md](./data-model.md).

## Prerequisites

1. PostgreSQL `kuky_dev` + Mailpit (see repo root `CLAUDE.md`).
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081` (applies Flyway `V15`).
3. Frontend: `npm run dev` in `front-end/` → `:8080`.
4. Admin (teacher) and student accounts; student has `STUDENT` role.

## 1. Author MANUAL AUDIO with two free-text questions

1. Admin → Homework → create MANUAL, type AUDIO, instructions + audio, add two FREE_TEXT prompts, assign student, save.
2. Reopen editor: both prompts present in order.
3. Try save with zero questions → validation error.
4. Create WRITE MANUAL → no question list; save with empty questions OK.

**Expect**: Non-WRITE MANUAL requires ≥1 FREE_TEXT; WRITE has none.

## 2. Student listening layout + submit

1. As student, open the AUDIO homework.
2. Confirm order: instructions, then audio player, then two compact plain-text fields (no rich-text toolbar).
3. Submit with one blank → blocked with validation message.
4. Fill both → submit succeeds; status SUBMITTED; answers reload under the same prompts.

**Expect**: FR-003 / FR-004a satisfied.

## 3. Teacher review

1. Admin review queue → open submission.
2. See each prompt + plain answer; leave rich-text feedback; mark reviewed.
3. Student reopens → answers + feedback read-only.

## 4. Snapshot after question removal

1. While a SUBMITTED (or REVIEWED) multi MANUAL exists, admin removes one question and saves.
2. Teacher (and student, if reviewed/submitted view) still see the removed prompt via snapshot on that submission.
3. A student who has not submitted yet only sees the remaining current questions.

## 5. Legacy migration smoke

1. If a pre-V15 non-WRITE MANUAL with `response_text` exists in DB, after migrate: one “Tu respuesta” question; plain answer visible in student/teacher views; WRITE rows still use rich-text `response`.

## 6. MANUAL activity parity

1. Admin → activity on a presentation, format MANUAL, ≥1 FREE_TEXT question, instructions PDF.
2. Student opens activity: instructions then compact questions; all answers required; submit.
3. Teacher review (activity path) shows prompt/answer pairs.

## 7. EXERCISE regression

1. Open an existing EXERCISE homework: authoring kinds unchanged; take + auto-grade still works; no FREE_TEXT in EXERCISE editor.

## Backend tests (optional local)

```bash
cd back-end
./gradlew test --tests "*HomeworkAdmin*" --tests "*HomeworkSubmission*" --tests "*Activity*"
```

## Done when

- Scenarios 1–4 and 6 pass in the browser.
- WRITE and EXERCISE behave as before (scenarios 1 / 7).
- Migration smoke OK if legacy data present (scenario 5).
