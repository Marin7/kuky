# Quickstart & Validation: Mi aprendizaje — Classes & Homework Tab

A run/validation guide proving the feature end-to-end. Implementation details live in `tasks.md` and the code; this file is how you confirm it works.

## Prerequisites

- PostgreSQL 18 running with the `kuky_dev` database (see project `CLAUDE.md` → Local dev setup).
- Mailpit optional (not used by this feature).
- Backend deps and frontend deps installed.

## Start the stack

```bash
# Backend (from back-end/) — Flyway runs V5__create_learning.sql on startup via FlywayConfig
./gradlew bootRun --args='--spring.profiles.active=local'      # http://localhost:8081

# Frontend (from front-end/)
npm run dev                                                     # http://localhost:8080
```

On first start, confirm the migration applied:

```bash
# Tables exist and are seeded
psql kuky_dev -c "SELECT count(*) FROM learning_presentation;"   # >= 2
psql kuky_dev -c "SELECT count(*) FROM past_classes;"            # >= 3
psql kuky_dev -c "SELECT count(*) FROM homework_assignments;"    # 3 (one past-due, one future, one no due date)
psql kuky_dev -c "SELECT count(*) FROM homework_submissions;"    # 0 (none seeded — every student starts PENDING)
```

You need a registered user; if you don't have one, register via `/cuenta` in the browser, or `POST /api/v1/auth/register`.

---

## Story 1 — Tab is logged-in-only (P1)

**Guest:**
1. Open `http://localhost:8080` while logged out → the header shows **no** "Mi aprendizaje" link (FR-002).
2. Navigate directly to `http://localhost:8080/aprendizaje` → you are redirected to `/cuenta` (FR-003).
3. API check — guest is rejected server-side:
   ```bash
   curl -i http://localhost:8081/api/v1/learning            # HTTP/1.1 403 (default security entry point; empty body)
   ```

**Logged in:**
4. Log in via `/cuenta`. The header now shows **"Mi aprendizaje"**; click it → the section opens (SC-002).
5. Log out → the link disappears and you land on a public page.

✅ Pass when guests can neither see nor reach the section and a logged-in user can.

---

## Story 2 — Past classes review (P2)

1. As a logged-in student, open **Mi aprendizaje**.
2. The "Clases anteriores" area lists the seeded past classes, each with **title**, **date** (Spanish/`es` locale), and the **teacher note** (FR-005).
3. Confirm ordering is **most recent first** (FR-006).
4. (Empty state) Temporarily mark the seed unpublished and reload to see the friendly empty state, then restore:
   ```bash
   psql kuky_dev -c "UPDATE past_classes SET published = false;"   # reload → empty state (FR-010)
   psql kuky_dev -c "UPDATE past_classes SET published = true;"
   ```

✅ Pass when past classes render correctly and the empty state shows when there are none.

---

## Story 3 — Homework view & submit (P3)

1. As a logged-in student, open **Mi aprendizaje** → the "Tareas" area lists the 3 assignments with **title, instructions, due date (if any), and status** (all initially **Pendiente**) (FR-007).
2. The past-due assignment is visibly flagged **overdue/atrasada** while pending (FR-009).
3. Mark one done / submit a short response via the dialog. Its status flips to **Enviada/Submitted** (FR-014).
4. **Persistence:** reload the page → the status is still **Submitted** with your response (FR-008, SC-004).
5. **Cross-session/device:** log in from another browser/profile as the **same** user → the submitted state is present (server-side per-student persistence, FR-015).
6. **Isolation:** log in as a **different** user → that user still sees the assignment as **Pending** (FR-015).
7. (Empty state) similar to Story 2 using `homework_assignments.published`.

API spot-checks (cookie jar `cookies.txt` from a prior login):

```bash
# Overview includes presentation, pastClasses, homework
curl -s -b cookies.txt http://localhost:8081/api/v1/learning | jq '{p:(.presentation|length), c:(.pastClasses|length), h:(.homework|length)}'

# Submit a response → status SUBMITTED
ASSIGNMENT_ID=$(curl -s -b cookies.txt http://localhost:8081/api/v1/learning | jq -r '.homework[0].id')
curl -s -b cookies.txt -X PUT \
  -H 'Content-Type: application/json' \
  -d '{"response":"Mi respuesta de prueba."}' \
  http://localhost:8081/api/v1/learning/homework/$ASSIGNMENT_ID | jq '{status, response, overdue}'

# Unknown assignment → 404 ASSIGNMENT_NOT_FOUND
curl -s -o /dev/null -w "%{http_code}\n" -b cookies.txt -X PUT \
  -H 'Content-Type: application/json' -d '{}' \
  http://localhost:8081/api/v1/learning/homework/00000000-0000-0000-0000-000000000000   # 404

# Over-long response → 400 VALIDATION_ERROR (response > 2000 chars)
```

✅ Pass when submit flips status, persists across reloads/sessions, is isolated per student, and overdue is flagged.

---

## Responsiveness & language (cross-cutting)

- Resize to a mobile width: the section remains readable, and the "Mi aprendizaje" link appears in the mobile menu (when logged in) (FR-013, SC-006).
- All labels/copy are Spanish, consistent with the rest of the site (FR-012).

## Automated tests

```bash
# Backend (from back-end/)
./gradlew test --tests '*HomeworkSubmissionServiceTest' \
               --tests '*LearningServiceTest' \
               --tests '*LearningControllerIntegrationTest'
```

Covers: pending→submitted upsert, idempotent re-submit, reject when reviewed, response length cap, unknown assignment, overdue derivation, PENDING default for assignments with no submission, and the `401`/authenticated controller paths.
