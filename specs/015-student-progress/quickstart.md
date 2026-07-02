# Quickstart: Student Progress on Teacher Profile View

Validation guide proving the feature works end-to-end. Assumes the standard local setup from `CLAUDE.md` (PostgreSQL `kuky_dev`, back-end `local` profile, front-end `npm run dev`).

## Prerequisites

- Back-end running: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'` (→ `:8081`). Flyway applies `V26__add_booking_no_show.sql` on boot.
- Front-end running: `cd front-end && npm run dev` (→ `http://localhost:8080`).
- One admin account (`TEACHER_EMAIL`, promoted to `ADMIN` by `AdminBootstrap`), one `STUDENT` account with:
  - At least one unit assigned (`/panel` → Units tab → assign student), with 2+ homeworks attached to that unit and targeted at the student (Homework tab).
  - A mix of homework statuses (leave one `PENDING`, submit one as the student, grade/review at least one as admin).
  - At least 2 past confirmed bookings (book via `/reservas` with a slot in the near future, or seed directly).

## Backend checks

```bash
cd back-end
./gradlew test
./gradlew build
```

Expected: new/extended tests pass — `BookingServiceTest` (`setNoShow` eligibility rules), `BookingAdminControllerTest` (no-show endpoint auth + happy path), `StudentProfileAdminServiceTest` / `UnitRepositoryTest` (progress aggregation). See [contracts/progress-api.md](contracts/progress-api.md) for endpoint shapes and [data-model.md](data-model.md) for the schema/query.

## Scenario A — Progress summary renders with real data (US1, FR-001–FR-006)

1. Log in as admin, open `/panel` → **Students** tab → the seeded student's profile.
2. **Expected**: a "Progreso" section shows the assigned unit(s) with a completion indicator, a homework breakdown (pending/submitted/completed counts matching what was seeded), an attended-classes count matching the number of past confirmed bookings, and the student's CEFR level (or "no result yet" if no placement test was taken).

## Scenario B — Empty state for a fresh student (US1, FR-008)

1. Promote a brand-new account to `STUDENT` with no units, homeworks, or bookings.
2. Open their profile in `/panel`.
3. **Expected**: the Progreso section renders a clear "no activity yet" state — no blank section, no error.

## Scenario C — Homework breakdown updates (US2, FR-003)

1. On the seeded student, submit the still-`PENDING` homework as the student (`/aprendizaje`), then reload the admin profile.
   - **Expected**: the "submitted" count increases by one, "pending" decreases by one.
2. As admin, review/grade that submission.
   - **Expected**: reloading the profile moves it into "completed".

## Scenario D — Unit completion flips to complete (US3, FR-002)

1. Ensure a unit has exactly the homeworks seeded in Prerequisites, and grade/review all of them for the student.
2. Reload the student's profile.
   - **Expected**: that unit shows as complete; a second unit with ungraded homework still shows as in progress.

## Scenario E — Mark and unmark a no-show (US3, FR-010/FR-011/FR-012)

1. On the student's profile, find a past confirmed booking in the attended-classes count.
2. Mark it as a no-show.
   - **Expected**: `PUT /api/v1/admin/bookings/{id}/no-show` → `204`; after reload, `attendedClasses` decreases by one and that booking shows a no-show indicator.
3. Revert the marking.
   - **Expected**: `attendedClasses` increases by one again.
4. Attempt to mark a **future** confirmed booking (or a cancelled one) as a no-show, e.g. via a direct API call.
   - **Expected**: `422 BOOKING_NOT_ELIGIBLE_FOR_NO_SHOW`.

## Scenario F — Access control (FR-007)

1. As a non-admin (or logged-out) client, call `GET /api/v1/admin/students/{id}/profile` and `PUT /api/v1/admin/bookings/{id}/no-show`.
   - **Expected**: `403 ACCESS_DENIED` (or `401` if logged out) for both — the existing `/api/v1/admin/**` matcher, unchanged.

## Scenario G — Revoked student retains history (FR-009)

1. Revoke the seeded student's `STUDENT` role from the admin panel.
2. Reload their profile.
   - **Expected**: the progress summary (units, homework breakdown, attended classes) is unchanged — revocation doesn't alter past activity, consistent with existing booking/homework history behavior.

## Cleanup / re-run

All new state lives in the single `bookings.no_show` column. Re-running any scenario is just re-toggling no-show flags or re-grading homeworks on the same seeded student/unit — no additional tables to reset.
