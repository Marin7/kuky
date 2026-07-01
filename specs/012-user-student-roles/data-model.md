# Phase 1 Data Model: Differentiate Users from Students

New migration: `back-end/src/main/resources/db/migration/V23__add_user_role.sql`. No new table — this widens the existing `users.role` column introduced in `V6__add_user_roles.sql`.

```sql
ALTER TABLE users
    DROP CONSTRAINT users_role_check;

ALTER TABLE users
    ALTER COLUMN role SET DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT users_role_check CHECK (role IN ('USER', 'STUDENT', 'ADMIN'));
```

No `UPDATE` statement: every row that exists today already has `role = 'STUDENT'` (the prior default), which is exactly the grandfathering FR-010 requires.

---

## Entity: `User` (existing, `users` table — no new columns)

| Column | Type | Notes |
|--------|------|-------|
| `role` | VARCHAR(20) NOT NULL DEFAULT `'USER'` | CHECK in (`USER`, `STUDENT`, `ADMIN`). `USER` = registered, not yet a student (new default). `STUDENT` = teacher-designated, unlocks booking/purchase/coursework. `ADMIN` = the teacher, unaffected by student status. |

**State transitions**:

```
USER  --(teacher grants student status)-->  STUDENT
STUDENT  --(teacher revokes student status)-->  USER
```

- `ADMIN` is out of band: never entered or exited via the grant/revoke actions in this feature (the teacher's own promotion to `ADMIN` remains `AdminBootstrap`'s job, unchanged).
- Transitions are idempotent at the service layer: granting to an already-`STUDENT` user, or revoking an already-`USER` user, is a no-op that still returns success (per spec Acceptance Scenario "already have student status").
- No new join/history table. A user's `bookings`, `purchases`, and `homework_targets` rows are keyed by `user_id` and are never touched by a role transition (FR-012) — they remain queryable regardless of current `role`.

**Java model** (`back-end/src/main/java/com/kuky/backend/auth/model/User.java`): change the field initializer only.

```java
private String role = "USER"; // was "STUDENT"
```

## New repository methods (`UserRepository`, alongside existing `findStudents()`)

| Method | Query | Purpose |
|--------|-------|---------|
| `findRegisteredUsers()` | `SELECT * FROM users WHERE role = 'USER' ORDER BY email` | Admin "Users" tab — candidates for promotion. |
| `promoteToStudentById(UUID id)` | `UPDATE users SET role = 'STUDENT', updated_at = NOW() WHERE id = :id AND role = 'USER'` | Returns rows-affected; `0` means already `STUDENT` (or missing/`ADMIN`) — caller treats as idempotent success/no-op, not error, when the user is already `STUDENT`. |
| `revokeStudentById(UUID id)` | `UPDATE users SET role = 'USER', updated_at = NOW() WHERE id = :id AND role = 'STUDENT'` | Returns rows-affected; `0` means already `USER` — same idempotent handling. |

Both follow the exact shape of the existing `promoteToAdminByEmail` method (back-end/src/main/java/com/kuky/backend/auth/repository/UserRepository.java:94-99).

## DTO: `RegisteredUserResponse` (new, `admin/dto`)

Mirrors the existing `StudentResponse` shape used by `GET /api/v1/admin/students`.

```java
public record RegisteredUserResponse(UUID id, String email, String firstName, String lastName, String username) {}
```

## No changes to

- `bookings`, `purchases`/resource-unlock tables, `homework_targets`, `unit_assignees`, `presentations` sharing tables — all already key off `user_id`, unaffected by the role value.
- `StudentProfileAdminService` / `StudentAdminController`'s existing `GET /admin/students` and `GET /admin/students/{id}/profile` — both already filter `role = 'STUDENT'`, which is exactly "current students" under the new model too.
