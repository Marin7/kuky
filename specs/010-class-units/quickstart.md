# Quickstart: Class Units (Packages)

Validation guide proving the feature end-to-end. See [data-model.md](data-model.md)
and [contracts/api.md](contracts/api.md) for details; this file is the run/verify
script, not the implementation.

## Prerequisites

- PostgreSQL 18 running with `kuky_dev` (see CLAUDE.md "Local dev setup").
- Mailpit on :1025 (not required for this feature but part of normal dev).
- An ADMIN account (the `app.scheduling.teacher-email` account, promoted by
  `AdminBootstrap`) and at least one STUDENT account.

## Setup / run

```bash
# Back-end (applies V18 migration on startup)
cd back-end
./gradlew bootRun --args='--spring.profiles.active=local'   # → http://localhost:8081

# Front-end
cd front-end
npm run dev                                                 # → http://localhost:8080
```

Confirm migration applied:
```sql
\d units
\d unit_assignments
SELECT column_name FROM information_schema.columns
 WHERE table_name='presentations' AND column_name='unit_id';
```

## Scenario A — Teacher organizes content (US1, P1)

1. Log in as ADMIN, open `/panel`. **Expect**: a single **Units** tab; no separate
   "Homework"/"Presentations" tabs (FR-001, SC-005).
2. Create a unit: level **A1**, subject **Family**. **Expect**: it appears under the
   A1 group at position 1 (FR-002/FR-002a).
3. Create a second A1 unit "Food"; reorder so Food is first. **Expect**: order
   persists after reload (FR-007).
4. Open the A1·Family unit; upload a `.pptx` presentation and create one homework
   inside it. **Expect**: both listed as the unit's contents (FR-004/FR-005/FR-006).
5. Move the presentation to the A1·Food unit. **Expect**: it disappears from Family
   (single membership, FR-005a).

## Scenario B — Assign a unit, presentations flow to the student (US2, P2)

1. As ADMIN, in A1·Family assign the unit to **Student X** (FR-008).
2. Log in as Student X, open `/aprendizaje`. **Expect**: every presentation in
   A1·Family is visible and downloadable, grouped under "A1 · Family"; **no**
   homeworks from the unit appear (FR-009/FR-012/FR-015, SC-002).
3. As ADMIN, add another presentation to A1·Family.
4. As Student X, reload `/aprendizaje`. **Expect**: the new presentation appears with
   no re-assignment (FR-010, SC-003).
5. As ADMIN, unassign A1·Family from Student X. As Student X, reload. **Expect**:
   those presentations are gone (FR-011).

## Scenario C — Homework stays teacher-gated (US3, P3)

1. With A1·Family assigned to Student X (presentations visible) but no homework
   assigned, as Student X open `/aprendizaje`. **Expect**: zero homeworks from the
   unit (FR-013/FR-016, SC-004).
2. As ADMIN, assign exactly one homework from the unit to Student X (existing
   homework assignee flow).
3. As Student X, reload. **Expect**: only that one homework appears; the unit's other
   homeworks remain unavailable (FR-014).

## Scenario D — Continuity & deletion edge cases

1. **Legacy share**: pick a presentation that was directly shared to a student before
   this feature (a row in `presentation_shares`). Confirm that student still sees it
   even though it has no unit (FR-017, SC-006).
2. **Delete unit**: delete A1·Food. **Expect**: its `unit_assignments` are removed;
   its presentations/homeworks still exist but are now unattached (`unit_id` NULL);
   the underlying files are not deleted.

## Automated checks

```bash
cd back-end && ./gradlew test     # unit/integration tests incl. access UNION
cd front-end && npm run lint      # TS strict + lint
```

## Success criteria mapping

| Criterion | Verified by |
|-----------|-------------|
| SC-001 (create + assign < 2 min, one tab) | Scenario A + B |
| SC-002 (100% presentations, 0% homeworks) | Scenario B step 2 |
| SC-003 (dynamic add) | Scenario B steps 3–4 |
| SC-004 (only explicit homeworks) | Scenario C |
| SC-005 (single tab) | Scenario A step 1 |
| SC-006 (no access regressions) | Scenario D step 1 |
