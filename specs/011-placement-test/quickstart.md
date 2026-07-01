# Quickstart: Placement Test

Validation guide proving the feature works end-to-end. Assumes the standard local setup from `CLAUDE.md` (PostgreSQL `kuky_dev`, Mailpit, back-end `local` profile, front-end `npm run dev`).

## Prerequisites

- Back-end running: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'` (→ `:8081`). Flyway applies `V19__create_placement_test.sql` on boot.
- Front-end running: `cd front-end && npm run dev` (→ `http://localhost:8080`).
- One admin account (the `TEACHER_EMAIL` account, promoted to `ADMIN` by `AdminBootstrap`) and one ordinary student account.

## Backend checks

```bash
cd back-end
./gradlew test                      # unit + integration (grading, CEFR banding, timer authority, auth gate)
./gradlew build
```

Expected: `PlacementGradingServiceTest`, `PlacementScoringServiceTest`, and `PlacementFlowIntegrationTest` pass. See [contracts/placement-api.md](contracts/placement-api.md) for endpoint shapes and [data-model.md](data-model.md) for the schema.

## Scenario A — Author the test (admin, US3 / FR-014)

1. Log in as the admin, open `/panel`, select the **Prueba de nivel** tab.
2. Set per-section time limits, the pass threshold, the writing prompt, and the bank-transfer instructions; save (`PUT /admin/placement/config`).
3. Add a few questions per skill (Reading, Listening, Grammar), each tagged with a CEFR level, mixing `SINGLE_CHOICE`, `MULTI_CHOICE`, `FILL_BLANK`. For a Listening question, attach an audio URL or upload a clip.
   - **Expected**: validation blocks a single-choice with ≠1 correct option, a fill-blank with no accepted answer, etc.

## Scenario B — Free auto-graded test (student, US1)

1. Log out, open `/prueba-de-nivel` while anonymous → **redirected to `/cuenta`** (FR-001 / SC: 401 `UNAUTHENTICATED` under the hood).
2. Log in as the student, open `/prueba-de-nivel`, start the **Reading** section.
   - **Expected**: a visible countdown to the server `deadlineAt` appears.
3. Answer some questions, leave one blank, click submit → **warning lists the blank question**; confirm → section grades (blank scored incorrect) and shows the section's CEFR level (FR-007).
4. Start a section, let the timer run out without submitting → **section auto-submits silently**, answered questions graded, blanks incorrect (FR-006). Refresh mid-section → countdown resumes from `deadlineAt`, not reset.
5. Finish all three sections → result shows **per-skill CEFR (Reading/Listening/Grammar) + overall estimate + per-section scores**, within ~3 s (FR-004 / SC-002).
6. Re-running with identical answers yields **identical levels** (SC-003).

## Scenario C — Full evaluation (student, US2)

1. On the result page, follow the CTA to the full-evaluation panel.
   - **Expected**: static bank-transfer instructions are shown; **no payment form, no amount field** (FR-009 / SC-005).
2. Write and submit the Writing response (`POST /placement/writing`) → stored, confirmation shown. **No payment check blocks submission** (FR-010 / SC-006).
3. Follow the "book appointment" CTA to `/reservas`, book a normal slot exactly like a regular class (FR-011) → booking confirmed via the existing flow (Zoom/stub + email).

## Scenario D — Teacher review (admin, US3 / FR-015)

1. As admin, open the student's detail page under `/panel` (`panel_.alumnos.$studentId`).
2. **Expected**: the student's per-skill CEFR results and their Writing submission are shown **together** (`GET /admin/placement/students/{id}/evaluation`, SC-007), giving the teacher everything needed to run the live audition + results discussion in the booked appointment.

## Cleanup / re-run

Placement attempts are append-only and unlimited per user; re-running Scenario B simply creates a new attempt. To reset authored content, delete questions in the admin tab. No production data migration is involved.
