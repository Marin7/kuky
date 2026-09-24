# Quickstart & Validation: Email Preferences

**Feature**: `049-email-preferences` | **Date**: 2026-09-23 | **Plan**: [plan.md](./plan.md)

How to run the feature locally and prove it satisfies the spec. Scenarios map to `spec.md` acceptance criteria and success criteria; details live in [data-model.md](./data-model.md) and [contracts/email-preferences-api.md](./contracts/email-preferences-api.md).

---

## Prerequisites

Standard local setup from `CLAUDE.md` — PostgreSQL 18 (`kuky_dev`), Mailpit on `:1025`, back-end `local` profile, front-end dev server.

**Mailpit is the whole test instrument here.** Its inbox at <http://localhost:8025> is where every assertion below is made. Confirm `app.mail.enabled=true` in the `local` profile before starting, or every send is a silent no-op and all scenarios will appear to "pass" by sending nothing.

```bash
cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'
```

```bash
cd front-end && npm run dev
```

**Accounts needed**: the teacher (`app.scheduling.teacher-email`, auto-promoted to `ADMIN`), and **two** student accounts — call them *Ana* and *Bea* — both activated and both granted `STUDENT` from the admin panel. Two are required: several scenarios turn on the difference between them.

**Before each run**: clear the Mailpit inbox, so a message left over from a previous scenario cannot be mistaken for a new one.

---

## S0 — Migration applied, everyone starts off

**Covers**: FR-002, SC-001 · **Run first** — every later scenario assumes this baseline.

```bash
psql -U kuky -d kuky_dev -c "SELECT email_on_homework_assigned, count(*) FROM users GROUP BY 1;"
```

**Expected**: one row, `f`, counting every account. Any `t` row means the migration did something it must not.

Then, with **nobody** opted in, assign Ana a unit containing homework.

**Expected**: Ana sees the homework on **Mi aprendizaje**; Mailpit is **empty**. The site works, the inbox stays silent.

---

## S1 — Find and switch on the preference

**Covers**: FR-001, FR-003, FR-004, FR-005, US1 #1–2, SC-002

1. Sign in as Ana, go to `/cuenta`.
2. Find the email preferences section in the account panel.

   **Expected**: exactly one option, clearly labelled in Spanish, switched **off**.
3. Switch it on.
4. Hard-reload, then sign out and back in.

   **Expected**: still on — it survives both.

```bash
psql -U kuky -d kuky_dev -c "SELECT email, email_on_homework_assigned FROM users WHERE email IN ('ana@example.com','bea@example.com');"
```

**Expected**: Ana `t`, Bea `f`.

Also check the three locales (es / ro / en) render a real label, not a raw key.

---

## S2 — Unit assignment sends one email listing everything

**Covers**: FR-006, FR-007a, FR-009, FR-010, US1 #3 and #7, SC-006 · **The primary scenario.**

1. As the teacher, create a unit with **three** homework items.
2. Assign the unit to Ana (opted in, from S1).

**Expected in Mailpit**:

- **Exactly one** message to Ana. Not three. If you see three, the accumulator is being flushed inside the loop instead of after it ([R3](./research.md)).
- It lists **all three** homework titles, in the order the unit granted them.
- It links to the work (`/aprendizaje`) and to preferences (`/cuenta`), and says why it was received.
- To: Ana alone — no CC, no BCC.

---

## S3 — Homework added to a unit the student already holds

**Covers**: FR-007b, US1 #8 · **The case that diverges from the in-site dots.**

With Ana still assigned that unit, add a **fourth** homework to it.

**Expected**:

- One email to Ana naming **only** the new homework.
- On the site, **no in-site unseen dot** appears for it.

That mismatch is correct and deliberate — the in-site dots do not mark this case unseen (`specs/041-notification-system/`), while FR-007b requires the email. If you "fix" the missing dot, you have changed a different feature.

---

## S4 — Direct per-homework assignment

**Covers**: FR-007c, US1 #9

As the teacher, create a homework in **Tareas** outside any unit and assign it to Ana.

**Expected**: one email to Ana naming that homework. Same email whichever route was used (E9).

---

## S5 — Opted-out students get nothing

**Covers**: FR-008, US1 #5, SC-004

Assign the same unit to **both** Ana (on) and Bea (off).

**Expected**: one email to Ana; **nothing** to Bea. Bea still sees the work on the site.

---

## S6 — Per-recipient content differs

**Covers**: FR-011, US1 #10, spec edge case "one action, several students, different amounts"

1. Opt Bea in as well.
2. Arrange for Bea to already hold two of the unit's homework (assign her the unit, then remove and re-add her — or assign her those two directly first).
3. Assign the unit to both.

**Expected**: two separate messages; Ana's lists what is new to her, Bea's lists **only** the homework new to *her*. Neither names the other student. If a student has nothing new, they get no message at all.

---

## S7 — Re-saving sends nothing

**Covers**: FR-013, SC-005 · **The regression most likely to slip through.**

With everything from S2–S6 already assigned:

1. Open the unit and re-save the assignees unchanged.
2. Re-save a homework's assignee list unchanged.
3. Remove Ana from the unit, then add her back.

**Expected**: Mailpit stays **empty** for steps 1 and 2. Step 3 *does* email — removing genuinely discards the targets, so adding her back is a real new grant. That is correct behaviour, not a bug.

---

## S8 — Switching off stops the emails

**Covers**: FR-004, US3 #1–2, SC-004

1. As Ana, switch the option off at `/cuenta`.
2. As the teacher, assign her new homework.

**Expected**: no email. Reload `/cuenta` — still off.

---

## S9 — A broken mail server does not break assignment

**Covers**: FR-012, E8 · **Do this one; it is the requirement most likely to be assumed rather than verified.**

1. Stop Mailpit (leave `app.mail.enabled=true`, so sending is attempted against a dead port).
2. As the teacher, assign Ana new homework.

**Expected**:

- The assignment **succeeds**. No error surfaces to the teacher.
- Ana sees the homework on the site.
- The back-end log shows a WARN, not a stack trace propagating out of the request.
- The `homework_targets` row exists — nothing rolled back.

Restart Mailpit afterwards.

---

## S10 — Nothing else changed

**Covers**: FR-014, SC-007, SC-008

1. Click both links in a received email — each lands on the right page in one click.
2. Exercise the other emails and confirm each still arrives unchanged: registration/activation, password reset, booking confirmation, booking cancellation, student-access grant.
3. Confirm in-site unseen dots on **Mi aprendizaje** / **Panel** behave exactly as before (S3's case excepted, which was always dot-free).

---

## Automated checks

```bash
cd back-end && ./gradlew test
```

Unit tests (run locally): `HomeworkAssignmentEmailServiceTest` — one message per student, correct recipient, body lists all granted titles, nothing sent when the flag is off. `EmailPreferencesServiceTest` — default off, toggle round-trip, unknown type rejected.

Integration tests (`AbstractIntegrationTest`, **GitHub Actions only** — skipped locally by design): `NewHomeworkGrantIntegrationTest` — `RETURNING` reports an insert exactly once, and reports none on re-insert. This is the DB-level proof behind S7, so **S7 must be run manually before merging**; CI is the only place the automated version executes.

```bash
cd front-end && npm run lint && npm run build
```

---

## Validation checklist

| # | Scenario | Requirements |
|---|---|---|
| S0 | Migration + default off | FR-002, SC-001 |
| S1 | Find, toggle, persist | FR-001, FR-003, FR-004, FR-005, SC-002 |
| S2 | Unit assignment → one email | FR-006, FR-007a, FR-009, FR-010, SC-006 |
| S3 | Homework added to held unit | FR-007b |
| S4 | Direct assignment | FR-007c |
| S5 | Opted-out gets nothing | FR-008, SC-004 |
| S6 | Per-recipient content | FR-011 |
| S7 | Re-save sends nothing | FR-013, SC-005 |
| S8 | Switching off | FR-004, US3 |
| S9 | Mail failure isolated | FR-012 |
| S10 | Nothing else changed | FR-014, SC-007, SC-008 |

SC-003 (arrival within 5 minutes) is satisfied by construction — sending is synchronous and completes within the request ([R4](./research.md)).
