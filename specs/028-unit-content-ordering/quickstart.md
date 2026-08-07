# Quickstart: Unit Content Ordering

**Branch**: `028-unit-content-ordering` | **Date**: 2026-08-07

Validate end-to-end against [spec.md](./spec.md) and
[contracts/unit-content-ordering-api.md](./contracts/unit-content-ordering-api.md).

## Prerequisites

1. PostgreSQL `kuky_dev` + Mailpit (see root `CLAUDE.md`).
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081`.
3. Frontend: `npm run dev` in `front-end/` → `:8080`.
4. Flyway applies `V10__unit_content_position.sql` on startup.
5. Teacher (ADMIN) and at least one STUDENT account; a unit with ≥2 presentations
   and ≥2 homeworks (or create them in the Units tab).

## 1. Seed after migrate

1. Open Panel → Units → expand a unit that already had mixed content before V10.
2. **Expect**: One combined list; presentations first (prior presentation order),
   then homeworks — not two separate headed sections.
3. As the assigned student, open `/aprendizaje/unidad/{unitId}`.
4. **Expect**: Same relative order for accessible items in **one** interleaved
   list (no separate presentation/homework sections). Unassigned homeworks hidden.

## 2. Teacher drag-and-drop reorder (P1)

1. As admin, drag a homework between two presentations; drop.
2. Reload the unit (collapse/expand or refresh).
3. **Expect**: Order persisted (`GET /api/v1/admin/units/{id}` → `contents` matches).
4. Optionally confirm keyboard reorder still works (dnd-kit keyboard or ▲/▼
   fallback if implemented).

## 3. Student sees updated sequence (P2)

1. Assign the unit to a student; assign only some homeworks.
2. Reorder to: presentation A → homework 1 → presentation B → homework 2.
3. As student, open the unit page.
4. **Expect**: A, homework 1, B in that order; homework 2 absent.

## 4. Attach / detach / move (P3)

1. Add a new presentation or homework to the unit.
2. **Expect**: New item at the **end** of `contents`.
3. Remove a middle item.
4. **Expect**: Neighbours keep relative order; positions contiguous on next load.
5. Move an item into another unit (attach via destination membership).
6. **Expect**: Appended on destination; gone from source sequence.

## 5. Access unchanged

1. Reorder freely as teacher.
2. **Expect**: Student still only sees unit presentations + explicitly assigned
   homeworks; reorder alone never reveals a homework.

## 6. Regression

1. Reorder **units** within a level (existing ▲/▼ on unit cards) still works.
2. Homework take/submit/grade and presentation download/view still work from the
   interleaved list entries.
3. “Other” / unattached learning content (if any) still appears outside unit
   sequences.

## API smoke (optional)

```http
PUT /api/v1/admin/units/{id}/contents/reorder
Content-Type: application/json

{ "items": [ { "type": "HOMEWORK", "id": "…" }, { "type": "PRESENTATION", "id": "…" } ] }
```

- Wrong/missing id → **400 INVALID_CONTENT_ORDER**
- Happy path → **200** with ordered `contents`
