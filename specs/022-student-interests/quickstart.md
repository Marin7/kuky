# Quickstart: Student Interests on Profile

Validation guide proving the feature works end-to-end. Assumes the standard local setup from `CLAUDE.md` (PostgreSQL `kuky_dev`, back-end `local` profile, front-end `npm run dev`).

## Prerequisites

- Back-end running: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'` (→ `:8081`). Flyway applies `V3__student_interests.sql` on boot.
- Front-end running: `cd front-end && npm run dev` (→ `http://localhost:8080`).
- One `ADMIN` account (`TEACHER_EMAIL`), one `STUDENT` account, and one `USER` (registered, not promoted) account.

## Backend checks

```bash
cd back-end
./gradlew test
./gradlew build
```

Expected: new/extended tests pass for interests validation (unknown code, >10 codes, note >280, USER forbidden), replace/clear semantics, and admin profile including interests. See [contracts/interests-api.md](contracts/interests-api.md) and [data-model.md](data-model.md).

## Scenario A — Student selects interests and note (US1, FR-001–FR-004, FR-013)

1. Log in as the student → `/cuenta`.
2. **Expected**: an interests section is visible (catalogue checkboxes/chips + optional note field with 280 max).
3. Select 3 topics (e.g. Travel, Music, Food), enter a short note, save the interests section.
4. Reload `/cuenta`.
5. **Expected**: the same three topics and note are still shown; `GET /api/v1/auth/me` returns matching `interests` / `interestsNote`.

## Scenario B — Non-student cannot edit (US1, FR-010)

1. Log in as the `USER` account → `/cuenta`.
2. **Expected**: no interests section in the UI.
3. Call `PUT /api/v1/auth/interests` with a valid body while authenticated as `USER`.
4. **Expected**: `403 FORBIDDEN`.

## Scenario C — Teacher reviews on student profile (US2, FR-005–FR-007)

1. With the student’s interests saved from Scenario A, log in as admin → `/panel` → Students → that student’s profile.
2. **Expected**: a dedicated interests section lists the localized labels (not raw codes) and shows the note; scannable without leaving the profile page.

## Scenario D — Empty state (US3, FR-006)

1. As the student, clear all selections and the note, save.
2. Reload admin student profile.
3. **Expected**: explicit empty state (e.g. “No interests selected yet”), not a missing section or error.

## Scenario E — Validation (FR-014, catalogue rules)

1. As the student, attempt to save a note of 281+ characters (or call the API directly).
   - **Expected**: validation error; previous saved values unchanged.
2. Call the API with an unknown code (e.g. `"GAMING"`) or 11 valid codes.
   - **Expected**: `400 INVALID_INTEREST` or `400 TOO_MANY_INTERESTS`.

## Scenario F — Isolation from name profile save (FR-011)

1. As the student, change only first/last name via the existing profile form and save.
2. **Expected**: interests and note remain unchanged.
3. Change only interests and save via the interests section.
4. **Expected**: name fields remain unchanged.
