# Phase 0 Research: Student Interests on Profile

All items resolved from the clarified spec and existing auth/admin profile patterns. No external research required.

## Decision: Store catalogue selections in a `user_interests` join table; note as a column on `users`

**Rationale**: A student may select zero-to-many catalogue codes (soft cap 10). A join table `(user_id, interest_code)` with a `CHECK (interest_code IN (...))` matches how the project already models multi-valued associations (`homework_targets`, unit assignments) and gives DB-level rejection of unknown codes. The optional free-text note is a single scalar (FR-013/FR-014), so it belongs on `users` as `interests_note VARCHAR(280)` — same pattern as `timezone` / `first_name` profile columns. Cascade-delete on `user_id` keeps cleanup automatic when an account is removed; revoking STUDENT leaves rows intact (FR edge case).

**Alternatives considered**:
- **`users.interests TEXT[]` / JSON column**: rejected — this repo has no array/JSON column precedent; NamedParameterJdbcTemplate array binding is awkward; a CHECK on array elements is harder than a row-level CHECK. A join table stays consistent with existing JDBC mappers.
- **Comma-separated `VARCHAR` on `users`**: rejected — loses per-code integrity, harder to query/filter later, and invites parsing bugs for no gain at this scale.
- **Separate `interest_options` table seeded by migration**: rejected — catalogue is product-fixed and not teacher-editable (clarification Q1). A Java/TS constant list plus a CHECK constraint is enough; a runtime table would invite accidental admin CRUD.

## Decision: Dedicated `PUT /api/v1/auth/interests` instead of extending `PUT /profile`

**Rationale**: `PUT /profile` is used by every authenticated user (including `USER` role) for name/username. Interests are student-access-only (clarification Q4 / FR-010). A dedicated endpoint can enforce `STUDENT`/`ADMIN` in the service (matching `/learning/**` and testimonials write rules) without branching inside `updateProfile` or risking USER clients sending/ignoring interest fields. FR-011’s “save independently” maps cleanly to a separate save control on the account page, following the existing `TimezoneSetting` pattern (own section + own save) rather than folding into the name form.

**Alternatives considered**:
- **Extend `UpdateProfileRequest`**: rejected — forces every profile save path to reason about student-only fields, and makes “USER must not see/edit interests” a soft ignore rather than a hard 403.
- **Nested under `/api/v1/learning/interests`**: workable but weaker — interests are profile data, not learning content, and the teacher reads them via the admin student profile; keeping write under `/auth` next to `/profile` keeps the self-service mental model.

## Decision: Product catalogue as shared string codes + i18n labels (no Java enum class)

**Rationale**: The codebase stores closed sets as `VARCHAR` + `CHECK` and uses plain strings in Java / string-literal unions in TypeScript (roles, homework format, etc.) — no Java `enum` types. Interest codes follow the same pattern: a small `InterestCatalogue` constants class (allowed codes + max selection count), a matching TS `INTEREST_CODES` const, and localized labels under `interests.<CODE>` in `es`/`en`/`ro`. Expanding the catalogue later is a migration (widen CHECK) + constants + i18n update — a product release, not admin UI (FR-012).

**Catalogue for v1** (12 codes; soft select cap 10 per Assumptions):

`TRAVEL`, `MUSIC`, `SPORTS`, `FOOD`, `CINEMA`, `READING`, `TECHNOLOGY`, `NATURE`, `ART`, `WORK`, `FAMILY`, `CULTURE`

**Unknown / retired codes on read**: filter out any stored code not in the current catalogue before returning to clients (edge case), so removed options disappear from both student and teacher views without a data scrub migration.

## Decision: Expose interests on `UserResponse` and `StudentProfileResponse`; no booking/schedule surfaces

**Rationale**: Clarification Q2 locked teacher visibility to the admin student profile only. Self-service needs the current selection on `/auth/me` (and the interests PUT response) so `/cuenta` can hydrate without a second fetch. Extending those two existing DTOs avoids a new read endpoint. Booking/schedule DTOs stay untouched (explicit out of scope).

## Decision: Gate editor UI and write API to student access (`STUDENT` or `ADMIN`)

**Rationale**: Spec FR-010 / clarification Q4 — non-student (`USER`) must not see the section. In this app, “student access” for learning features is `hasAnyRole("STUDENT", "ADMIN")`. The write endpoint rejects other roles with `403`. The account page shows the interests section when `user.role === "STUDENT" || user.role === "ADMIN"`. Teacher does not edit another student’s interests (FR-009) — admin profile is read-only for this data.

## Decision: Soft max of 10 catalogue selections; note max 280 characters

**Rationale**: Spec Assumptions (~10) and clarification Q5 (280). Enforce both in the service (and Bean Validation `@Size(max = 280)` on the note / `@Size(max = 10)` on the list). UI communicates the catalogue cap and shows a character counter or `maxLength={280}` on the note field.
