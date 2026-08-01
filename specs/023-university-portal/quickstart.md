# Quickstart: University Student Portal

Validate the feature end-to-end against [spec.md](spec.md), [data-model.md](data-model.md), and [contracts/university-api.md](contracts/university-api.md).

University portal entry (local): `http://localhost:8080/universidad` (path-based shell). Optional host: `http://uni.localhost:8080` with `VITE_UNIVERSITY_HOST` / CORS including that origin.

## Prerequisites

- PostgreSQL `kuky_dev` running; backend `local` profile; Mailpit optional (university grant/revoke sends **no** email).
- Front-end and back-end from repo root conventions in `CLAUDE.md`.
- University host reachable at the configured local hostname (e.g. `http://uni.localhost:8080`) **or** documented equivalent; private site at `http://localhost:8080`.
- API at `http://localhost:8081` with CORS allowing **both** front origins and cookie settings that keep auth working on the university host after login.

## Setup

```bash
# back-end/
./gradlew bootRun --args='--spring.profiles.active=local'

# front-end/
npm run dev
```

Confirm Flyway applied `V4__university_portal.sql` (role CHECK includes `UNIVERSITY_STUDENT`, new university tables present).

## Validation scenarios

### 1. Public informative (no login)

1. Open the **university** entry (not the private site).
2. Open schedule, exams, news without signing in.
3. **Expect**: schedule shows **all** sessions labeled beginner/intermediate; exams/news list published items (or empty states). No 1-1 booking nav.

### 2. Registration on university entry

1. Register a new account from the university entry.
2. **Expect**: account is `USER`, no university materials/homework access; informative pages still readable.
3. Log in on the private site with the same credentials (shared account) — **Expect**: same identity; no private STUDENT privileges.

### 3. Teacher grants university status

1. As ADMIN on `/panel`, grant university-student with level `BEGINNER` to the new user (no email expected in Mailpit).
2. **Expect**: user `role=UNIVERSITY_STUDENT`, `universityLevel=BEGINNER`.
3. Attempt to grant private STUDENT without revoking — **Expect**: rejected (`ROLE_CONFLICT`).
4. On university entry, user opens materials/homework — **Expect**: level-appropriate empty or assigned content; schedule is level-filtered.

### 4. Schedule template + exception

1. Admin creates beginner/intermediate template sessions (aim for 5+2).
2. Add a `CANCEL` or `EXTRA` exception for one date.
3. **Expect**: anonymous schedule shows labeled full week + exception effect; beginner university student sees only beginner sessions including exception.

### 5. Shared catalog availability

1. Pick an existing 1-1 homework and presentation; make both available for `BEGINNER` via admin availability APIs/UI.
2. As beginner university student, open university learning — **Expect**: both appear; complete one MANUAL or EXERCISE homework successfully.
3. As intermediate university student (or same user after level change) — **Expect**: beginner-only items not shown as theirs.

### 6. Mutual exclusion with private student

1. Create another `USER`; promote to private `STUDENT`.
2. Try grant university — **Expect**: rejected until private student revoked.
3. University-only user cannot book 1-1 / open private `/aprendizaje` coursework — **Expect**: existing STUDENT gates still apply.

### 7. Revoke

1. Revoke university status — **Expect**: materials/homework blocked on next request; public informative still readable; no revoke email.

## Backend tests (smoke)

```bash
# back-end/
./gradlew test
```

Focus on new/extended tests for university security matchers, grant/revoke mutual exclusion, schedule merge, and learning access via availability.

## Done when

- All scenarios above pass in the browser on the university host.
- `./gradlew test` green for university-related tests.
- Private-lesson flows for `STUDENT` unchanged in a quick smoke (schedule book + aprendizaje).
