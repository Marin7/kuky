# Quickstart: Admin Student View Overhaul

**Feature**: `036-admin-student-view` | **Date**: 2026-08-11

Manual browser validation after implementation. See [data-model.md](./data-model.md) and [contracts/admin-student-profile-api.md](./contracts/admin-student-profile-api.md).

## Prerequisites

1. PostgreSQL `kuky_dev` + Mailpit (usual local setup).
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081`
3. Frontend: `npm run dev` in `front-end/` → `:8080`
4. Admin (teacher) account; at least one student with a mix of homework statuses (pending, submitted, graded) and ideally past/upcoming classes + placement data.

## Scenario A — Progreso gone

1. Admin → Students → open a student profile.
2. **Expect**: no **Progreso** section; no unit progress list; no attended-classes or level chips from the old progress block.
3. **Expect**: upcoming/past classes, presentations, interests, and placement evaluation (if any) still present as before.

## Scenario B — Collapsed Tareas breakdown

1. On the same profile, look at the top stats row without expanding.
2. **Expect**: Tareas card shows total homework count **and** pending / submitted / completed counts.
3. Open a student with zero homeworks → zeros / empty-friendly counts, not a broken card.

## Scenario C — Expand / collapse list

1. Click the Tareas card.
2. **Expect**: a full-width homework list appears **directly under** the stats row (not only inside the card column).
3. **Expect**: no second **Tareas** section further down the page.
4. Click Tareas again → list collapses; breakdown counts remain on the card.

## Scenario D — Row actions parity

1. With the list expanded, find a pending row → status visible; no score; no open-result if no submission.
2. Find a submitted / needs-review row → open review action works (same dialog as before).
3. Find a graded row with score → score % visible; view-result opens responses.

## Scenario E — Past classes no-show still works

1. Toggle no-show on a past confirmed class.
2. **Expect**: badge/toggle still works; profile reloads without errors (no dependency on removed attended-count field).

## Backend smoke (optional)

```bash
cd back-end
./gradlew test --tests '*StudentProfileAdmin*'
```
