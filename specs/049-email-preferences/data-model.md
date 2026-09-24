# Phase 1 Data Model: Email Preferences

**Feature**: `049-email-preferences` | **Date**: 2026-09-23 | **Plan**: [plan.md](./plan.md)

The feature adds **one column** and **no tables**. Most of the design work sits in the *semantics* of an existing table (`homework_targets`) rather than in new structure.

---

## 1. `users.email_on_homework_assigned` — the stored preference

Realises the spec's **Email preference set** entity. Per [R1](./research.md), it is a column on `users` rather than a preferences table.

### Migration — `V26__email_preferences.sql`

```sql
ALTER TABLE users
    ADD COLUMN email_on_homework_assigned BOOLEAN NOT NULL DEFAULT false;
```

That is the whole migration. Deliberately **no** backfill, no `UPDATE`, and no data-dependent default — FR-002 ("no migration, backfill, or bulk action may switch an option on") is satisfied by the shape of the DDL, not by a step someone has to remember to leave out.

### Field

| Field | Type | Null | Default | Notes |
|---|---|---|---|---|
| `email_on_homework_assigned` | `BOOLEAN` | `NOT NULL` | `false` | True ⇔ this account has opted in to the new-homework email |

### Rules

- **R1.1** — Default `false` for every row, existing and future. Applies to all roles (`USER`, `STUDENT`, `ADMIN`).
- **R1.2** — Writable only by the owning account, through the principal on the request. No admin path reaches it (FR-015).
- **R1.3** — Untouched by role changes. Granting or revoking `STUDENT` must not read or write it, so a revoked-then-restored student keeps their setting (spec Assumptions).
- **R1.4** — Untouched by account deletion beyond the row disappearing with the user; no separate cleanup.
- **R1.5** — No index. The column is only ever read for an explicit, already-bounded set of user IDs (§4), never scanned on its own.

### No new state machine

The value is a plain boolean with no lifecycle: `false ⇄ true`, both directions freely and repeatedly (FR-004). "Never touched" and "explicitly switched off" are deliberately indistinguishable — nothing in the spec needs to tell them apart.

---

## 2. `EmailPreferenceType` — the catalogue of options

Realises the spec's **Email event type** entity. A Java enum, not a table: the set of options is code, deployed with the code that honours them, so a value can never exist in the database that no code sends.

```java
public enum EmailPreferenceType {
    NEW_HOMEWORK_ASSIGNED
}
```

### Rules

- **R2.1** — Exactly one constant in this release (FR-003).
- **R2.2** — The enum is the single source of truth for what `GET` lists and what `PUT` accepts. Adding an option is: one constant, one column, one mapping entry, three translation keys — and no change to the page's structure ([R6](./research.md)).
- **R2.3** — An unrecognised type on `PUT` is a client error, never a silent no-op. It surfaces as the existing `VALIDATION_ERROR` (400) via `GlobalExceptionHandler`'s `IllegalArgumentException` handler — no new exception type or error code is introduced.
- **R2.4** — The constant name is the wire value. It is stable API surface: renaming a constant is a breaking change.

### Mapping to storage

One entry today, in `EmailPreferencesRepository`:

| Enum constant | Column |
|---|---|
| `NEW_HOMEWORK_ASSIGNED` | `users.email_on_homework_assigned` |

At three entries, [R1](./research.md)'s revisit trigger fires and this mapping becomes a key/value table.

---

## 3. `homework_targets` — unchanged structure, load-bearing semantics

Realises the spec's **Homework assignment event**. **No schema change.** The table already carries the fact the feature depends on, via its existing uniqueness constraint on `(assignment_id, user_id)`.

### The rule that makes FR-013 hold

A row in `homework_targets` *is* "this student has this homework". So:

> A homework is **newly assigned to a student** exactly when an `INSERT` into `homework_targets` for that `(assignment_id, user_id)` actually inserts a row.

`HomeworkTargetRepository.addTargets` already issues `INSERT ... ON CONFLICT (assignment_id, user_id) DO NOTHING`, which evaluates precisely that predicate and then discards the answer. The change is to stop discarding it:

```sql
INSERT INTO homework_targets (id, assignment_id, user_id, student_seen_at, due_on)
VALUES (:id, :aid, :uid, :seenAt, :dueOn)
ON CONFLICT (assignment_id, user_id) DO NOTHING
RETURNING user_id
```

An insert returns one row; a conflict returns none.

### Rules

- **R3.1** — `addTargets(...)` returns the user IDs it actually inserted. Existing callers that ignore the return value keep their current behaviour exactly (FR-014).
- **R3.2** — `replaceTargets(...)` delegates to `addTargets(...)` and inherits this; it needs no separate change.
- **R3.3** — Re-saving an assignment with an unchanged assignee list inserts nothing, so it reports nothing, so it emails nothing (FR-013).
- **R3.4** — The delta is computed by the database inside the statement, not by a read-then-compare in Java. Two concurrent grants of the same homework to the same student cannot both report an insert ([R2](./research.md)).
- **R3.5** — `student_seen_at` is **not** consulted. Unit-sourced rows are created already-seen and direct rows unseen, but FR-007 requires all three routes to email alike, so the in-site seen flag is irrelevant here. This is the one place email and the in-site dots deliberately diverge (spec Assumptions).
- **R3.6** — Removal is silent. `removeTargets` sends nothing; the spec has no un-assignment email.

### The three grant routes, all covered by R3.1

| Route | Call site | Spec clause |
|---|---|---|
| Student assigned to a unit | `UnitService.setAssignees` | FR-007a |
| Homework added to a unit students already hold | `UnitService.setHomeworks` | FR-007b |
| Direct assign, on homework create | `HomeworkAdminService.create` | FR-007c |
| Direct assign, editing the assignee list | `HomeworkAdminService.setAssignees` | FR-007c |

---

## 4. `NewHomeworkGrants` — the per-action accumulator

Transient, in-memory, request-lifetime. **Not persisted.** It exists only to turn per-homework inserts into one email per student (FR-006).

```java
/** Newly granted homework for one teacher action, grouped by student. */
public final class NewHomeworkGrants {
    private final Map<UUID, List<UUID>> byStudent = new LinkedHashMap<>();

    public void add(UUID assignmentId, Collection<UUID> newlyGrantedUserIds) { ... }
    public boolean isEmpty() { ... }
    public Map<UUID, List<UUID>> byStudent() { ... }
}
```

### Rules

- **R4.1** — One instance per service method invocation — that is, per teacher action, which is the spec's grouping unit ([R3](./research.md)).
- **R4.2** — Keyed by student. Each key produces exactly one email, so no email can ever name two recipients (FR-011).
- **R4.3** — Insertion-ordered (`LinkedHashMap` / `List`) so the homework is listed in the order the teacher's action granted it, making the email deterministic and snapshot-testable.
- **R4.4** — Empty ⇒ no recipient lookup, no send, no log noise. The common case (re-saving an unchanged assignment) costs nothing.
- **R4.5** — A student appearing under several homework in one action gets one email listing all of them (FR-006) — the whole reason this type exists.
- **R4.6** — Different students in the same action may hold different lists. Assigning a unit where one student already had two of its homework produces per-recipient emails that differ (spec edge case).
- **R4.7** — Flushed **once**, as the last step of the service method, after all repository writes ([R4](./research.md)).

---

## 5. Recipient resolution

One query, run once per flush, over the accumulator's key set.

```sql
SELECT id, email, first_name
FROM users
WHERE id IN (:userIds)
  AND email_on_homework_assigned = true
  AND status = 'ACTIVE'
```

→ `List<Recipient(UUID userId, String email, String firstName)>`

### Rules

- **R5.1** — Opted-out students are dropped here, so the "on at the moment of assignment" reading of FR-008 is the value this query sees — read once, after the writes, per action.
- **R5.2** — `status = 'ACTIVE'` implements "an account still awaiting activation is not emailed" (spec edge case). A `PENDING` account is silently skipped, not an error.
- **R5.3** — A student in the accumulator but absent from this result is simply not emailed. Normal, not exceptional.
- **R5.4** — One query, not `findById` in a loop — assigning a unit to fifteen students stays at one lookup ([R8](./research.md)).
- **R5.5** — Role is **not** filtered. Only students can hold `homework_targets` rows, so the accumulator cannot contain a non-student; adding a role predicate would be dead weight.
- **R5.6** — Homework titles come from `homework_assignments.title`, the same column `findAssignmentsForStudent` already reads.

---

## Entity summary

| Spec entity | Realised as | Persisted |
|---|---|---|
| Email preference set | `users.email_on_homework_assigned` | Yes — 1 new column |
| Email event type | `EmailPreferenceType` enum | No — code |
| Homework assignment event | An actual insert into `homework_targets` | Yes — existing table, unchanged |
| *(grouping mechanism)* | `NewHomeworkGrants` | No — request-lifetime |

**Total schema change: one `ALTER TABLE ... ADD COLUMN`.**
