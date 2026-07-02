# Quickstart: Student Testimonials on Homepage

Validation guide proving the feature works end-to-end. Assumes the standard local setup from `CLAUDE.md` (PostgreSQL `kuky_dev`, Mailpit, back-end `local` profile, front-end `npm run dev`).

## Prerequisites

- Back-end running: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'` (→ `:8081`). Flyway applies `V25__create_testimonials.sql` on boot.
- Front-end running: `cd front-end && npm run dev` (→ `http://localhost:8080`).
- Mailpit running (`http://localhost:8025`) to inspect the teacher-notification email.
- One admin account (`TEACHER_EMAIL`, promoted to `ADMIN` by `AdminBootstrap`) and at least one account promoted to `STUDENT` from `/panel`.

## Backend checks

```bash
cd back-end
./gradlew test
./gradlew build
```

Expected: `TestimonialServiceTest`, `TestimonialControllerTest`, `TestimonialAdminControllerTest`, and the extended `SecurityConfig` gating test pass. See [contracts/testimonials-api.md](contracts/testimonials-api.md) for endpoint shapes and [data-model.md](data-model.md) for the schema/state machine.

## Scenario A — Homepage hides the section when empty (US1, FR-002)

1. On a fresh database (no testimonials yet), load `/` as a logged-out visitor.
2. **Expected**: no testimonials section is rendered anywhere on the page (SC-005).

## Scenario B — Student submits a testimonial (US2, FR-005/FR-006/FR-008)

1. Log in as the `STUDENT` account, go to `/aprendizaje`.
2. Submit a testimonial via the new "My Testimonial" section.
   - **Expected**: `POST /api/v1/testimonials` → `200 OK`, status shown as pending review.
   - Check Mailpit: the teacher received a "new testimonial submitted" email (FR-008).
3. Load `/` in a different (logged-out) browser session → **the testimonial does NOT appear** (still `PENDING`, FR-009).
4. As the same student, submit again with different text.
   - **Expected**: the student's own status still shows exactly one testimonial (the new text), not two (FR-006).

## Scenario C — Teacher reviews and publishes (US3, FR-003/FR-009)

1. Log in as admin, open `/panel` → **Testimonios** tab. The pending submission from Scenario B appears.
2. Approve it (`POST /admin/testimonials/{id}/approve`).
   - **Expected**: `200 OK`, status flips to `APPROVED`.
3. Reload `/` as a logged-out visitor → **the testimonial is now visible**, showing the student's full name and text (SC-001, SC-003).
4. As the student, check their own status at `/aprendizaje` → shows `APPROVED` (FR-007).

## Scenario D — Reorder, edit, unpublish (US3, FR-003/FR-012)

1. Submit and approve a second testimonial from another student account.
2. In the **Testimonios** admin tab, reorder the two published testimonials.
   - **Expected**: `/` reflects the new order on next load.
3. Edit one testimonial's text as admin.
   - **Expected**: the change appears on `/` on next load, without touching its status.
4. Unpublish one of the two.
   - **Expected**: it disappears from `/` immediately (next load) but remains visible in the admin tab as `UNPUBLISHED` (FR-012), and can be re-approved later.

## Scenario E — Rejection doesn't mislead the student (US2, FR-005)

1. Submit a new testimonial from a third student account.
2. As admin, reject it.
3. As that student, check `/aprendizaje` → status shows `REJECTED`, clearly distinct from `PENDING`/`APPROVED` (not silently hidden as if nothing happened).

## Scenario F — Validation and access control (FR-010, contracts)

1. As a `STUDENT`, attempt to submit an empty or one-character testimonial.
   - **Expected**: `400 VALIDATION_ERROR`, clear message, no row created/updated.
2. As a plain `USER` (not promoted to `STUDENT`) account, attempt `POST /api/v1/testimonials`.
   - **Expected**: `403 ACCESS_DENIED`, surfaced via the shared `StudentOnlyNotice` message.
3. As a logged-out visitor, attempt `GET /api/v1/testimonials/me`.
   - **Expected**: `401 UNAUTHENTICATED`.

## Cleanup / re-run

All state lives in the single `testimonials` table, one row per student (`UNIQUE(user_id)`). Deleting or resubmitting a testimonial via the admin tab or the student resubmission flow is enough to reset a test account between runs — no other tables are touched.
