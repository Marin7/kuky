# API Contract: Class Units (Packages)

All endpoints are **ADMIN-only** unless noted, gated by the existing
`/api/v1/admin/**` security matcher. Auth via the `auth-token` HttpOnly cookie;
front-end calls use `credentials: 'include'`. Errors use the shared shape
`{"error":"CODE","message":"..."}` mapped in `GlobalExceptionHandler`.

New error codes: `UNIT_NOT_FOUND`, `INVALID_LEVEL`, `STUDENT_NOT_FOUND` (reuse).

---

## Admin — Units

### `GET /api/v1/admin/units`
List all units grouped/ordered for the Units tab.

**200** →
```json
[
  {
    "id": "uuid",
    "level": "A1",
    "subject": "Family",
    "position": 1,
    "presentationCount": 3,
    "homeworkCount": 2,
    "assignedStudentIds": ["uuid", "uuid"]
  }
]
```
Sorted by `(level, position)`.

### `POST /api/v1/admin/units`
Create a unit. Body:
```json
{ "level": "A1", "subject": "Family" }
```
- `level` required, ∈ {A1,A2,B1,B2,C1,C2} → else **400 INVALID_LEVEL**.
- `subject` required, 1–200 chars.
- `position` defaults to `max(position)+1` within the level.

**201** → `UnitDetail` (see below).

### `GET /api/v1/admin/units/{id}`
**200** → `UnitDetail`:
```json
{
  "id": "uuid",
  "level": "A1",
  "subject": "Family",
  "position": 1,
  "presentations": [ /* PresentationSummary[] */ ],
  "homeworks": [ /* HomeworkAdminItem[] */ ],
  "assignedStudents": [ /* StudentResponse[] */ ]
}
```
**404 UNIT_NOT_FOUND** if missing.

### `PUT /api/v1/admin/units/{id}`
Edit level and/or subject. Body `{ "level": "A2", "subject": "Food" }`.
**200** → `UnitDetail`. (FR-003)

### `DELETE /api/v1/admin/units/{id}`
Delete a unit. Cascades `unit_assignments`; sets contained presentations'/homeworks'
`unit_id` to NULL (content preserved). **204**. (FR-003, "Deleting a unit" edge case)

### `PUT /api/v1/admin/units/reorder`
Reorder units within a level. Body:
```json
{ "level": "A1", "orderedIds": ["uuid", "uuid", "uuid"] }
```
- `orderedIds` MUST be exactly the set of unit ids for that level → else **400**.
Rewrites `position` 0..n-1. **200** → updated `UnitSummary[]` for the level. (FR-002a/FR-007)

---

## Admin — Unit contents

A presentation/homework belongs to at most one unit; attaching moves it (FR-005a).

### `PUT /api/v1/admin/units/{id}/presentations`
Set the unit's presentation membership. Body:
```json
{ "presentationIds": ["uuid", "uuid"] }
```
- Sets `unit_id = {id}` for the listed presentations (detaching them from any prior
  unit) and clears `unit_id` for presentations previously in this unit but omitted.
**200** → `UnitDetail`. (FR-004)

### `PUT /api/v1/admin/units/{id}/homeworks`
Set the unit's homework membership. Body `{ "homeworkIds": ["uuid"] }`.
Same move/detach semantics on `homework_assignments.unit_id`. **200** → `UnitDetail`. (FR-005)

> Authoring presentations (file upload/replace/level) and homeworks (create/edit)
> continues to use the existing `/api/v1/admin/presentations/*` and
> `/api/v1/admin/homework/*` endpoints, now reached from inside a unit. New items
> created via the Units UI pass the owning `unit_id` (FR-005b).

---

## Admin — Unit assignment

### `PUT /api/v1/admin/units/{id}/assignees`
Set which students are assigned this unit (grants presentation access). Body:
```json
{ "studentIds": ["uuid", "uuid"] }
```
- Replaces the `unit_assignments` set for the unit (add/remove diff).
- Each id must be a STUDENT → else **404 STUDENT_NOT_FOUND**.
**200** → `UnitDetail`. (FR-008 → FR-009/FR-011)

> Homework assignment is **unchanged**: continue using
> `PUT /api/v1/admin/homework/{id}/assignees`. Assigning a unit never assigns
> homework (FR-012).

---

## Student — Learning overview (existing endpoint, response extended)

### `GET /api/v1/learning`
The `sharedPresentations` array gains an optional `unit` field so the client can group
by unit (FR-015). Access is the UNION of legacy shares and unit assignments.

```json
{
  "sharedPresentations": [
    {
      "id": "uuid",
      "title": "Mi familia",
      "hasFile": true,
      "unit": { "level": "A1", "subject": "Family", "position": 1 }
    },
    {
      "id": "uuid",
      "title": "Legacy deck",
      "hasFile": true,
      "unit": null
    }
  ],
  "homework": [ /* unchanged — only explicitly-assigned items (FR-016) */ ]
}
```
- `unit: null` → presentation reached via a legacy direct share; client renders it
  under an "Other" group.
- No homework is ever added to this response by unit assignment (FR-012/FR-016).

### `GET /api/v1/learning/presentations/{id}/file` (unchanged signature)
Download gate updated: allowed if the presentation is shared (legacy) **or** reachable
via an assigned unit. Otherwise **404** (PresentationNotFound). (FR-009/FR-010)

---

## Contract test checklist

- Create unit → appears in `GET /units` sorted by (level, position).
- Attach presentation already in unit B to unit A → it leaves B (single membership).
- Assign unit to student → `GET /api/v1/learning` lists all its presentations, no homeworks.
- Add presentation to assigned unit → student sees it without re-assignment.
- Unassign unit → student loses those presentations unless a legacy share exists.
- Assign homework via `/homework/{id}/assignees` → only that homework appears for the student.
- Delete unit → assignments gone, contained presentations/homeworks now `unit_id=null` and still exist.
