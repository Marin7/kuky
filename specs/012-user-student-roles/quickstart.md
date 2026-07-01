# Quickstart: Differentiate Users from Students

Validation guide proving the feature works end-to-end. Assumes the standard local setup from `CLAUDE.md` (PostgreSQL `kuky_dev`, Mailpit, back-end `local` profile, front-end `npm run dev`).

## Prerequisites

- Back-end running: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'` (→ `:8081`). Flyway applies `V23__add_user_role.sql` on boot.
- Front-end running: `cd front-end && npm run dev` (→ `http://localhost:8080`).
- Mailpit running (`http://localhost:8025`) to inspect grant/revoke emails.
- One admin account (`TEACHER_EMAIL`, promoted to `ADMIN` by `AdminBootstrap`).

## Backend checks

```bash
cd back-end
./gradlew test
./gradlew build
```

Expected: `SecurityConfigStudentGatingTest`, extended `StudentAdminControllerTest`, and extended `EmailServiceTest` pass. See [contracts/user-roles-api.md](contracts/user-roles-api.md) for endpoint shapes and [data-model.md](data-model.md) for the schema change.

## Scenario A — New account starts as a plain user (US2, FR-002/FR-011)

1. Register a brand-new account at `/cuenta`, activate it, log in.
2. Open `/reservas` and `/recursos` → **schedule and resource listings are fully browsable** (FR-006).
3. Attempt to actually book a class → **blocked**, shown the "ask the teacher" message, not a generic error (FR-009 / SC-003).
4. Attempt to purchase/unlock a resource → **same blocked message**.
5. Open `/aprendizaje` → **blocked/redirected** with the same message (coursework is not public browsing).
6. Open `/prueba-de-nivel` → **fully accessible and completable** (FR-008).

## Scenario B — Teacher grants student status (US1, FR-004/FR-014)

1. Log in as admin, open `/panel` → **Users** tab. The account from Scenario A appears in the list.
2. Click "make student" (`POST /admin/users/{id}/student`).
   - **Expected**: `200 OK`, the account moves off the Users list and onto the existing **Students** tab.
   - Check Mailpit: a "you're now a student" email arrived at that account's address (FR-014 / SC-006).
3. Log back in as that account (or refresh if already logged in) → booking, purchasing, and `/aprendizaje` are now unlocked (FR-004).
4. Repeat step 2 on the same user → still `200 OK`, no duplicate/error (idempotent grant).

## Scenario C — Teacher revokes student status (US3, FR-005/FR-012)

1. Before revoking, have the student book a class and/or purchase a resource so there's history to check.
2. As admin, on the **Students** tab, click "revoke" for that user (`DELETE /admin/users/{id}/student`).
   - **Expected**: `200 OK`, the account moves back to the **Users** tab. Mailpit shows the "student access removed" email (SC-006).
3. Log back in as that account → booking/purchasing/`aprendizaje` are blocked again, **but** their past booking and purchase remain visible in their own history views (FR-012 / SC-005).
4. Re-grant via Scenario B step 2 → access returns, history unchanged (no data loss).

## Scenario D — Existing accounts are unaffected (FR-010)

1. Inspect the `users` table before/after applying `V23__add_user_role.sql`:
   ```sql
   SELECT role, COUNT(*) FROM users GROUP BY role;
   ```
   - **Expected**: every pre-existing row is still `STUDENT` or `ADMIN` — none flipped to `USER` by the migration.
2. Log in as a pre-existing student account created before this feature shipped → booking, purchasing, and `/aprendizaje` work exactly as before, no interruption.

## Scenario E — Admin is unaffected either way (FR-013)

1. As the admin account, attempt to book a class / purchase a resource / open `/aprendizaje`.
   - **Expected**: works regardless of the admin's `role` value being `ADMIN` (never `STUDENT`) — `hasAnyRole("STUDENT","ADMIN")` passes.

## Cleanup / re-run

Granting/revoking only flips `users.role`; no other tables are touched, so scenarios can be repeated freely on the same test account. No production data migration is involved.
