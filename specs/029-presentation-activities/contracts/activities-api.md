# Contract: Presentation Activities API

Admin authoring + student fulfillment for presentation-linked activities. Builds on existing presentation access and homework exercise shapes.

---

## Admin — `/api/v1/admin/activities`

Auth: `ADMIN`.

### `GET /api/v1/admin/activities`

Optional query: `presentationId`.

**200**: `ActivityAdminItem[]` — id, title, format, level, homeworkType, presentationId, presentationTitle, position, triggerFileId, triggerPage, hasInstructions, createdAt, updatedAt; optionally compact submission counts.

### `GET /api/v1/admin/activities/{id}`

**200**: `ActivityAdminDetail` — above plus questions (with answer key, same shape as `HomeworkQuestionDto`), instructions metadata `{id, originalName, contentType, byteSize}`, assignees N/A.

**404**: unknown id.

### `POST /api/v1/admin/activities`

`application/json` body (`SaveActivityRequest`):

- Fields: `title`, `presentationId`, `format`, optional `level`, `homeworkType`, **required** `triggerFileId` + `triggerPage` (presentation PDF page where instructions already appear), `questions` (when EXERCISE).

**201**: `ActivityAdminDetail`.

**400**: missing title/presentation/trigger; invalid format; trigger file not on presentation; page &lt; 1; EXERCISE without questions.

### `PUT /api/v1/admin/activities/{id}`

Same fields as create. Changing `presentationId` may invalidate a trigger that belongs to the old presentation — a valid trigger for the target presentation is still required.

**200**: detail. **400** / **404** as above.

### `DELETE /api/v1/admin/activities/{id}`

**204**. Cascades submissions; deletes any leftover instructions PDF from disk if present.

### `PUT /api/v1/admin/presentations/{presentationId}/activities/reorder`

Body: `{ activityIds: UUID[] }` — permutation of all activities for that presentation.

**204**. **400** if not a complete permutation.

### Review (mirror homework)

| Method | Path | Notes |
|--------|------|--------|
| GET | `/api/v1/admin/activities/submissions` | MANUAL queue |
| GET | `/api/v1/admin/activities/submissions/{id}` | |
| GET | `/api/v1/admin/activities/submissions/{id}/exercise-result` | |
| PUT | `/api/v1/admin/activities/submissions/{id}/feedback` | MANUAL → REVIEWED |
| PUT | `/api/v1/admin/activities/submissions/{id}/exercise-feedback` | optional on GRADED |

---

## Student — Learning

Auth: `STUDENT` or `ADMIN`; presentation access required.

### Overview embed

`GET /api/v1/learning` → each `sharedPresentations[]` item gains:

```json
"activities": [
  {
    "id": "...",
    "title": "...",
    "format": "MANUAL|EXERCISE",
    "position": 0,
    "status": "PENDING|SUBMITTED|REVIEWED|GRADED",
    "scorePercent": null,
    "triggerFileId": "...|null",
    "triggerPage": 3
  }
]
```

Ordered by `position`. Only activities for presentations the user can access. No top-level unit-sequence entry.

### `GET /api/v1/learning/activities/{id}`

**200**:
- MANUAL: title, format, status, `triggerFileId` / `triggerPage` (instructions page), response/feedback if any.
- EXERCISE: same + questions stripped of answer key (same as `ExerciseResponse`), result if graded.

**404**: missing or no presentation access.

### `PUT /api/v1/learning/activities/{id}`

MANUAL submit — body same as homework `SubmitHomeworkRequest` (`response: FormattedTextSegment[]`).

**200**: updated item. **400** if not MANUAL / already terminal inappropriately.

### `PUT /api/v1/learning/activities/{id}/answers`

EXERCISE submit — body same as `SubmitExerciseRequest`.

**200**: `ExerciseResultResponse`-shaped result. Single submission (no retake), same as homework.

---

## Student progress (admin)

`GET /api/v1/admin/students/{id}/profile` → extend `progress`:

```json
"activityBreakdown": {
  "pending": 0,
  "submitted": 0,
  "completed": 0
}
```

and/or per-unit `totalActivities` / `completedActivities` when unit-scoped progress is shown. Counts only activities on presentations the student can access.

---

## Errors

Use existing `{ "error": "ERROR_CODE", "message": "..." }` pattern. Suggested codes:

| Code | When |
|------|------|
| `ACTIVITY_NOT_FOUND` | Unknown or inaccessible |
| `ACTIVITY_VALIDATION` | Bad trigger, missing PDF, bad questions |
| `ACTIVITY_REORDER_INVALID` | Incomplete permutation |
| `ACTIVITY_ALREADY_SUBMITTED` | Retake blocked |

---

## Frontend routes (contractual UX)

| Route | Role |
|-------|------|
| `/panel/actividades` | Admin list |
| `/panel/actividades/nueva` | Create |
| `/panel/actividades/$activityId` | Edit |
| `/aprendizaje/actividad/$activityId` | Student full-page fulfill (from unit list) |
| Viewer overlay | No new route — Dialog on existing presentation viewer |

---

## Out of scope

- Due dates, assignees, rich-text instructions, non-PDF instruction uploads
- Unit-level mixed-sequence entries for activities
- Server-side PDF page counting library
- PPTX page triggers
