# Contract: Homework Labels API

**Feature**: `043-homework-labels` | **Date**: 2026-08-16

No new routes. Extends existing admin homework payloads. Auth unchanged (`ADMIN` on `/api/v1/admin/homework/**`). Student `/api/v1/learning/**` **does not** include `label`.

Errors stay `{"error":"ERROR_CODE","message":"..."}`.

## Field

| JSON | Type | Meaning |
|------|------|---------|
| `label` | `string` or `null` | Optional teacher label. Omitted on create/update ≡ unlabeled. |

Normalized on write: trim; blank → `null`; max 40 characters after trim.

## Endpoints

### `GET /api/v1/admin/homework`

Each `HomeworkAdminItem` gains `label` (`string` \| `null`). Client derives filter options from non-null values (case-insensitive groups). No `?label=` query param.

### `GET /api/v1/admin/homework/{id}`

Same `label` on the item used by the editor.

### `POST /api/v1/admin/homework`

Body adds optional `label`. Response item includes stored `label`.

### `PUT /api/v1/admin/homework/{id}`

Body adds optional `label`. Sending `null` or `""` (or whitespace) clears the label. Label-only changes **must not** bump `content_revised_at`.

### Unchanged

`PUT .../assignees`, `DELETE .../{id}`, review-queue and submission routes. Delete still removes the assignment; leftover filter state is a client concern (reset to all labels when the selected group is gone).

## Errors

| Code | HTTP | When |
|------|------|------|
| `VALIDATION_ERROR` | 400 | Label longer than 40 characters **after trim**. Message tells the teacher it is too long (Spanish, consistent with other homework validation). |

Do not add a new error code.

## Student and other admin JSON

Do **not** add `label` to:

- `GET /api/v1/learning` homework items
- `GET /api/v1/learning/homework/{id}` (take/result)
- Admin review queue / submission / exercise-result DTOs
- Quiz or presentation admin/student payloads

Unit admin may reuse `HomeworkAdminItem`; it may ignore `label`.

## Example

Create:

```json
{
  "title": "El subjuntivo — deseo",
  "instructions": "...",
  "homeworkType": "GRAMMAR",
  "level": "B1",
  "label": "Subjuntivo"
}
```

List item excerpt:

```json
{
  "id": "…",
  "title": "El subjuntivo — deseo",
  "homeworkType": "GRAMMAR",
  "level": "B1",
  "label": "Subjuntivo"
}
```

Unlabeled item: `"label": null`.
