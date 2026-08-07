# API Contract: Unit Content Ordering

All admin endpoints are **ADMIN-only** under `/api/v1/admin/**`. Auth via
`auth-token` HttpOnly cookie; front-end uses `credentials: 'include'`. Errors:
`{"error":"CODE","message":"..."}`.

Extends [010-class-units contracts](../../010-class-units/contracts/api.md).

New error code: `INVALID_CONTENT_ORDER` (reorder body is not an exact permutation
of the unit’s current contents).

---

## Admin — Unit detail shape

### `GET /api/v1/admin/units/{id}` (and create/update/set* responses)

**200** → `UnitDetail`:

```json
{
  "id": "uuid",
  "level": "A1",
  "subject": "Family",
  "position": 1,
  "contents": [
    {
      "type": "PRESENTATION",
      "unitPosition": 0,
      "presentation": { /* PresentationSummary */ }
    },
    {
      "type": "HOMEWORK",
      "unitPosition": 1,
      "homework": { /* HomeworkAdminItem */ }
    }
  ],
  "assignedStudents": [ /* StudentResponse[] */ ]
}
```

- `contents` sorted by `unitPosition` ascending.
- Exactly one of `presentation` / `homework` is non-null, matching `type`.
- Top-level `presentations` / `homeworks` arrays are **removed** from this DTO.
- `UnitSummary` still exposes `presentationCount` / `homeworkCount`.

---

## Admin — Reorder unit contents

### `PUT /api/v1/admin/units/{id}/contents/reorder`

Body:

```json
{
  "items": [
    { "type": "PRESENTATION", "id": "uuid" },
    { "type": "HOMEWORK", "id": "uuid" },
    { "type": "PRESENTATION", "id": "uuid" }
  ]
}
```

- `type` ∈ {`PRESENTATION`, `HOMEWORK`}.
- `items` MUST list each current member of the unit exactly once → else
  **400 INVALID_CONTENT_ORDER**.
- Rewrites `unit_position` to `0..n-1` in request order.

**200** → updated `UnitDetail`.  
**404 UNIT_NOT_FOUND** if the unit is missing.

---

## Admin — Membership (behavioural change)

### `PUT /api/v1/admin/units/{id}/presentations`

Body unchanged: `{ "presentationIds": ["uuid", ...] }`.

Additional behaviour:
- Retained presentations keep their place in the mixed sequence relative to
  homeworks and other retained presentations.
- Newly listed presentations (including those moved from another unit) are
  **appended** to the end of this unit’s sequence.
- Omitted presentations are detached (`unit_id` NULL, `unit_position` 0); remaining
  items keep relative order (positions rewritten contiguous).

**200** → `UnitDetail` with ordered `contents`.

### `PUT /api/v1/admin/units/{id}/homeworks`

Same append / retain / detach rules for homeworks.

---

## Learning — Overview DTO fields

### `GET /api/v1/learning` (existing)

Each shared presentation gains optional `unitPosition`:

```json
{
  "id": "uuid",
  "title": "...",
  "files": [],
  "unit": { "id": "uuid", "level": "A1", "subject": "Family", "position": 1 },
  "unitPosition": 0
}
```

Each homework item gains optional `unitPosition`:

```json
{
  "id": "uuid",
  "title": "...",
  "unit": { "id": "uuid", "level": "A1", "subject": "Family", "position": 1 },
  "unitPosition": 2
}
```

- `unitPosition` is present when the item belongs to a unit; omit or `null` for
  legacy/unattached items.
- Client merges presentations + homeworks for a unit and sorts by `unitPosition`
  for the interleaved unit view.
- Access filtering unchanged (overview already returns only accessible items).

---

## Unchanged

- `PUT /api/v1/admin/units/reorder` (units within a level)
- Presentation access via unit assignment (dynamic)
- Presentation authoring endpoints
- Error codes `UNIT_NOT_FOUND`, `INVALID_LEVEL`, `STUDENT_NOT_FOUND`

## Admin — Unit assignees (behavioural change)

### `PUT /api/v1/admin/units/{id}/assignees`

Body unchanged: `{ "studentIds": ["uuid", ...] }`.

Additional behaviour:
- Newly assigned students are also added to `homework_targets` for **every
  homework currently in the unit** (so they see presentations and homeworks).
- Students removed from the unit lose those unit-homework targets (other
  homeworks they were assigned to outside this unit are untouched).
- Adding a homework to a unit later (`PUT .../homeworks`) grants it to all
  current unit assignees; removing a homework from the unit revokes it for
  those assignees.
