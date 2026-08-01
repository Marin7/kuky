# Phase 1 Data Model: Student Interests on Profile

New migration: `back-end/src/main/resources/db/migration/V3__student_interests.sql`.

```sql
ALTER TABLE users
    ADD COLUMN interests_note VARCHAR(280);

CREATE TABLE user_interests (
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interest_code VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_id, interest_code),
    CONSTRAINT user_interests_code_check CHECK (interest_code IN (
        'TRAVEL', 'MUSIC', 'SPORTS', 'FOOD', 'CINEMA', 'READING',
        'TECHNOLOGY', 'NATURE', 'ART', 'WORK', 'FAMILY', 'CULTURE'
    ))
);

CREATE INDEX user_interests_user_id_idx ON user_interests (user_id);
```

---

## Entity: Interest Option (catalogue — not a DB table)

| Attribute | Type | Notes |
|-----------|------|-------|
| `code` | stable string | One of the 12 CHECK-constrained codes above. |
| `label` | localized string | Resolved in the front-end via `t("interests." + code)` (es/en/ro). |

Defined in:

- Java: `com.kuky.backend.auth.InterestCatalogue` — `Set<String> CODES`, `int MAX_SELECTIONS = 10`, `int MAX_NOTE_LENGTH = 280`, helpers `isAllowed(code)`, `filterKnown(codes)`.
- TypeScript: `front-end/src/lib/interests.ts` — `INTEREST_CODES` const array + `InterestCode` union type; labels via i18n, not hardcoded English.

---

## Entity: `User` (extended)

| Column / field | Type | Notes |
|----------------|------|-------|
| `interests_note` | `VARCHAR(280)`, nullable | **New.** Optional free-text note (FR-013/FR-014). Empty string from clients is stored as `NULL`. |
| *(selections)* | via `user_interests` | Not a column on `users`; loaded/replaced as a set of codes. |

**Java model** (`User.java`): add `private String interestsNote;` + getter/setter. Catalogue selections are not necessarily hydrated onto every `User` load — prefer dedicated repository methods for interests to keep existing `USER_MAPPER` lean (see below). Alternatively hydrate `List<String> interests` on User when needed by profile/me responses.

**Repository** (`UserRepository`, extended):

| Method | Shape | Purpose |
|--------|-------|---------|
| `findInterestCodesByUserId(userId)` | `SELECT interest_code FROM user_interests WHERE user_id = :id ORDER BY interest_code` | Read selection. |
| `replaceInterests(userId, codes)` | delete all for user + batch insert | Last-write-wins replace of the full set (FR edge case). |
| `updateInterestsNote(userId, note)` | `UPDATE users SET interests_note = :note, updated_at = NOW() WHERE id = :id` | Persist note (nullable). |
| `findInterestsNoteByUserId(userId)` | select `interests_note` | Or include `interests_note` in `USER_MAPPER` once the column exists — preferred for `/me` and admin profile so note comes with the user row. |

Prefer adding `interests_note` to `USER_MAPPER` and `User`, and keeping only the multi-select codes in `user_interests` helpers.

---

## Entity: Student Interest Selection (aggregate)

Logical aggregate returned to clients (not a separate table):

| Field | Type | Notes |
|-------|------|-------|
| `interests` | `string[]` | Known catalogue codes only (unknown/retired filtered out on read). Ordered stably (e.g. alphabetical by code, or catalogue display order). |
| `interestsNote` | `string \| null` | ≤280 chars; null when empty. |

**Validation (service layer)** on write:

1. Caller must have role `STUDENT` or `ADMIN`; otherwise `403`.
2. Every code must be in `InterestCatalogue.CODES`; otherwise `400` / `INVALID_INTEREST`.
3. Distinct codes only; duplicates collapsed or rejected — prefer collapse to unique set.
4. `codes.size() <= 10`; otherwise `400` / `TOO_MANY_INTERESTS`.
5. Note length ≤ 280 after trim; blank → null; otherwise `400` validation (Bean Validation `@Size(max = 280)`).

**Replace semantics**: one transaction deletes existing `user_interests` for the user and inserts the new set, then updates `interests_note`. Empty list + null note is valid (US3).

---

## DTO extensions

| DTO | Change |
|-----|--------|
| `UserResponse` | Add `List<String> interests`, `String interestsNote`. Populated for all roles (empty list / null for users who never set them). |
| `UpdateInterestsRequest` *(new)* | `List<@NotNull String> interests`, `@Size(max = 280) String interestsNote`. |
| `StudentProfileResponse` | Add `List<String> interests`, `String interestsNote` for teacher prep (FR-005/FR-006). |

No changes to booking, schedule, or public testimonial DTOs (FR-008, clarification Q2).

---

## Relationships

```text
users 1 ──< user_interests >── interest_code (catalogue constant)
users.interests_note (optional scalar)
```

Teacher reads via `StudentProfileAdminService.getProfile` → joins/loads interests for that user id. Student writes via `AuthService.updateInterests`.
