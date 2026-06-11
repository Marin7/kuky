# Quickstart — Validate the Teacher Backoffice

Feature: `006-teacher-backoffice` | Spec: [spec.md](spec.md) · Plan: [plan.md](plan.md) · API: [contracts/api.md](contracts/api.md)

This is a run/validation guide, not an implementation guide. It proves the three user
stories end-to-end against a locally running stack. Implementation details live in `tasks.md`.

## Prerequisites

1. **PostgreSQL 18** running with the `kuky_dev` database (see root `CLAUDE.md` → Local dev setup).
2. **Mailpit** running (`mailpit`) — optional; only the booking/auth emails use it.
3. Backend: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'`
   - Flyway applies `V6`–`V9` on first start; check the log for `Migrating schema "public" to version "9"`.
   - On boot, `AdminBootstrap` promotes the account whose email is `app.scheduling.teacher-email`
     (`paula@kuky.es` locally) to `ADMIN`. Look for the `AdminBootstrap` log line.
4. Frontend: `cd front-end && npm run dev` → http://localhost:8080

## One-time data setup

1. Register the teacher account from `/cuenta` using **`paula@kuky.es`** (the configured admin
   email). Register at least two **student** accounts (any other emails), e.g.
   `ana@example.com` and `luis@example.com`.
2. Re-login as `paula@kuky.es` after the first backend start (the `ADMIN` role lands in the JWT
   on login). Confirm `GET /api/v1/auth/me` returns `"role": "ADMIN"`.

## Scenario A — Availability (User Story 1, P1)

1. Signed in as `paula@kuky.es`, open **Panel → Disponibilidad**.
   - Expected: the weekly grid shows the seeded Mon–Fri `09:00–12:00` and `14:00–18:00` windows.
2. As a guest/incognito (or as a student) open `/reservas`.
   - Expected (baseline): the public calendar matches today's behavior (Mon–Fri, lunch gap).
3. Back in the panel, add a Wednesday evening window `19:00–20:00`, save.
   - Reload `/reservas` → Wednesday now offers a 19:00 slot (subject to the 24 h lead rule).
4. Add a **BLOCK** exception for an upcoming Monday (full day), save.
   - Reload `/reservas` → that Monday shows no open slots; other Mondays unchanged.
5. Make a confirmed booking as a student inside a window, then in the panel remove that window
   and save.
   - Expected: the save succeeds and returns a `bookingConflicts` entry; the panel shows a
     non-blocking warning; the student's booking still appears in **Mis clases** and the slot
     still shows `BOOKED` on the public calendar (FR-010 / SC-008).

**Pass criteria**: SC-001 (Paula changes published availability with no redeploy), SC-002
(student sees it on next load), SC-008 (confirmed booking survives).

## Scenario B — Homework (User Story 2, P2)

1. Panel → **Tareas**. Create a homework item (title, instructions, due date) and assign it to
   `ana@example.com` only. Save.
2. Sign in as `ana@example.com`, open `/aprendizaje`.
   - Expected: the new homework appears with correct details; mark it submitted with a short
     response.
3. Sign in as `luis@example.com`, open `/aprendizaje`.
   - Expected: the homework does **not** appear (not assigned).
4. Back in the panel, open the item.
   - Expected: Ana shows `SUBMITTED` with her response text; Luis is not listed.
5. Edit the instructions; confirm Ana sees the update. Unassign Ana; confirm it disappears from
   her space. Delete the item (confirm dialog).

**Pass criteria**: SC-003 (create + assign < 3 min, assigned sees / unassigned doesn't),
SC-004 (visibility isolation).

## Scenario C — Presentations (User Story 3, P3)

1. Panel → **Presentaciones**. Create a deck "Clase 1 — Saludos". Add 5 slides; on at least one
   slide upload an image (jpg/png/webp ≤ 2 MB).
2. Reorder two slides; edit a heading. Open **Vista previa** → page through full-screen with
   keyboard/next-prev.
3. Try uploading a non-image or a >2 MB file → expect an `INVALID_IMAGE` rejection message.
4. Share the deck with `ana@example.com`. Sign in as Ana → `/aprendizaje` shows the deck; open
   and page through it; images load.
5. Sign in as `luis@example.com` → the deck is not listed; hitting its URL/endpoint returns
   `PRESENTATION_NOT_FOUND`.

**Pass criteria**: SC-005 (build ≥ 5-slide deck, reorder, preview, share in one session),
SC-004 (only shared student sees it).

## Scenario D — Access control (cross-cutting)

1. As a signed-in student, request `GET /api/v1/admin/homework` → **403 ACCESS_DENIED**.
2. As a guest (no cookie), request any `/api/v1/admin/**` → **401**.
3. As a student, navigate directly to `/panel` → redirected to `/`; the **Panel** nav entry is
   absent for students and guests, present only for `paula@kuky.es`.

**Pass criteria**: SC-006 (no non-teacher reaches the panel), FR-001/002/003.

## Backend test commands

```bash
cd back-end && ./gradlew test          # unit + slice tests for availability/homework/presentations/admin authz
cd front-end && npm run lint           # frontend lint; UI verified visually per constitution
```

## Expected automated coverage (see tasks.md)

- `AvailabilityService` derivation: weekly windows, BLOCK/OPEN exceptions, lunch-gap absence,
  weekend = no rules, lead/past/booked overlay, booking-conflict detection.
- Admin authorization: 401 guest, 403 student, 200 admin on `/api/v1/admin/**`.
- Homework targeting: assigned student sees it, unassigned does not; teacher sees submissions.
- Presentations: share gate (shared opens, non-shared 404), image type/size validation, reorder permutation.
