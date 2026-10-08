# API Contract: Unit as a PDF with Page Activities

Base path `/api/v1`. Every `/admin/**` endpoint requires `ADMIN` (existing matcher), and
every `/learning/**` endpoint requires `STUDENT` or `ADMIN` (existing). Errors use the
standard shape `{"error":"CODE","message":"..."}`.

## Admin — units

### `GET /admin/units` (changed response)

`UnitSummary[]`:

```json
{
  "id": "uuid", "level": "A1", "subject": "La familia", "position": 0,
  "hasPdf": true, "pageCount": 24,
  "activityCount": 5, "homeworkCount": 2,
  "assignedStudentIds": ["uuid"],
  "hasUnseenActivitySubmissions": false
}
```

`presentationCount` is removed; `hasPdf`, `pageCount`, `activityCount` and
`hasUnseenActivitySubmissions` are new.

### `GET /admin/units/{id}` · `POST /admin/units` · `PUT /admin/units/{id}` (changed response)

`UnitDetail`:

```json
{
  "id": "uuid", "level": "A1", "subject": "La familia", "position": 0,
  "pdf": {
    "presentationId": "uuid", "fileId": "uuid",
    "originalName": "familia.pdf", "byteSize": 1234567, "pageCount": 24
  },
  "contents": [
    { "type": "PDF", "unitPosition": 0 },
    { "type": "HOMEWORK", "unitPosition": 1, "homework": { "...HomeworkAdminItem (unchanged)": "" } }
  ],
  "activities": [
    {
      "id": "uuid", "page": 4, "title": null, "format": "EXERCISE", "composition": "ALL_AUTO",
      "submittedCount": 2, "awaitingCorrectionCount": 0, "gradedCount": 2,
      "hasUnseenSubmissions": true
    }
  ],
  "assignedStudents": [ { "...StudentResponse (unchanged)": "" } ]
}
```

- `pdf` is `null` when the unit has no PDF yet. The `PDF` content item is present only when it isn't.
- `activities` are sorted by `page`. When `title` is `null`, the client renders "Actividad – página {page}".
- `PUT /admin/units/{id}` also updates the owned presentation's `title` and `level`.

### `POST /admin/units/{id}/pdf` (NEW), multipart/form-data

| Part | Type | Required | Notes |
|---|---|---|---|
| `file` | file | yes | PDF only, max 50 MB |
| `pageCount` | int | yes | 1–500, counted client-side with pdf.js |
| `removeOutOfRangeActivities` | boolean | no (default `false`) | needed when activities exist on pages > `pageCount` |

Responses:
- `200` `UnitDetail`. Creates the owned presentation on the first upload; otherwise replaces its single file.
- `404 UNIT_NOT_FOUND`
- `422 VALIDATION_ERROR`: empty file, not a PDF (name, content type or `%PDF-` header), over 50 MB, or `pageCount` out of range
- `409 ACTIVITIES_OUT_OF_RANGE`: activities would be removed and the flag is not `true`. Message names how many; the client computes the count itself before sending.

The new bytes are written before the database change, and the old file is deleted from
disk after commit, so a failed request leaves the previous PDF intact.

### `GET /admin/units/{id}/pdf` (NEW)

`200` with the PDF bytes (`Content-Type: application/pdf`, inline). `404 UNIT_NOT_FOUND`,
or `404 PRESENTATION_NOT_FOUND` when the unit has no PDF.

### `PUT /admin/units/{id}/contents/reorder` (changed request)

`{"items":[{"type":"PDF","id":"<presentationId>"},{"type":"HOMEWORK","id":"uuid"}]}`. The
valid types are now `PDF` and `HOMEWORK`; the request must still be a full permutation of
the unit's contents (`INVALID_CONTENT_ORDER` otherwise).

### `PUT /admin/units/{id}/homeworks` · `PUT /admin/units/{id}/assignees`

Unchanged, including homework grants and the single email flush.

### `GET /admin/units/{id}/activity-progress` (NEW)

One row per (activity × assigned student):

```json
[
  {
    "activityId": "uuid", "page": 4, "studentId": "uuid",
    "status": "PENDING|SUBMITTED|REVIEWED|GRADED",
    "scorePercent": 80, "submissionId": "uuid|null",
    "submittedAt": "2026-10-07T10:00:00Z|null", "unseen": false
  }
]
```

A student without a submission row gets `status: "PENDING"` and `submissionId: null`.

### REMOVED

`PUT /admin/units/{id}/presentations`.

## Admin — activities

### `POST /admin/units/{unitId}/activities` (NEW: mark a page)

```json
{ "page": 4, "title": "optional", "questions": [ { "...HomeworkQuestionDto (unchanged)": "" } ] }
```

Responses:
- `201` `ActivityAdminDetail`
- `404 UNIT_NOT_FOUND`
- `400 ACTIVITY_VALIDATION`: no PDF, page out of `1..pageCount`, no questions, or invalid question structure
- `409 ACTIVITY_PAGE_TAKEN`: that page already has an activity

### `GET /admin/activities/{id}` · `PUT /admin/activities/{id}` (changed body)

`PUT` request: `{ "title": "optional", "questions": [...] }`. The page cannot be changed.
To move an activity, unmark the page and mark another one.

`ActivityAdminDetail`:

```json
{
  "id": "uuid", "unitId": "uuid", "presentationId": "uuid", "page": 4,
  "title": null, "format": "MIXED", "composition": "MIXED",
  "questions": [ "...HomeworkQuestionDto" ],
  "createdAt": "…", "updatedAt": "…"
}
```

These fields are removed: `presentationTitle`, `position`, `triggerFileId`, `triggerPage`,
`instructionsText`, `youtubeUrl`, `imageId`, `hasInstructions`, `instructions`, `level` and
`homeworkType`.

Editing questions after students have submitted keeps today's rules (submitted answers stay
tied to the question rows they answered; nothing new is introduced here).

### `DELETE /admin/activities/{id}[?deleteSubmissions=true]` (changed: unmark a page)

- `204`
- `404 ACTIVITY_NOT_FOUND`
- `409 ACTIVITY_HAS_SUBMISSIONS`: at least one submission exists and the flag is not `true`

### Review endpoints (unchanged paths, small additions)

- `GET /admin/activities/submissions[?unitId=uuid]`: the review queue, optionally filtered to one unit. Each item gains `activityId`, `unitId` and `page`. `assignmentTitle` holds the activity title, or the "página N" fallback built on the server for this queue only.
- `GET /admin/activities/submissions/{submissionId}` and `GET …/{submissionId}/exercise-result` now also mark the submission teacher-seen (`teacher_seen_at = NOW()`).
- `PUT …/{submissionId}/feedback` and `PUT …/{submissionId}/exercise-feedback`: unchanged.

### REMOVED

- `GET /admin/activities` (the list)
- `POST /admin/activities`
- `PUT /admin/presentations/{presentationId}/activities/reorder`

## Admin — presentations (standalone only)

- `GET /admin/presentations` lists only presentations with `unit_id IS NULL`.
- Every by-id endpoint (`GET`, `PUT`, `DELETE`, `files` upload/delete/download, `level`, `shares`) returns `404 PRESENTATION_NOT_FOUND` for a unit-owned presentation.
- `PresentationSummary` / `PresentationDetail` no longer carry unit or activity information, if they did.

## Admin — student profile

`GET /admin/students/{id}` (`StudentProfileResponse`) gains:

```json
"unitActivities": [
  {
    "unitId": "uuid", "unitLevel": "A1", "unitSubject": "La familia",
    "activityId": "uuid", "page": 4, "title": null, "format": "MANUAL",
    "status": "SUBMITTED", "scorePercent": null,
    "submissionId": "uuid|null", "unseen": true
  }
]
```

It covers the units assigned to the student, sorted by unit level, unit position, then page. `format` decides how the teacher opens a submission: `EXERCISE` shows the auto-graded result, anything else opens the review.

## Notifications

`GET /notifications/badges` → `BadgeSummary` gains `units: boolean`, which is true for an
admin when any activity submission from a student who still holds the unit is unseen
(an unassigned student's work can no longer be opened from the unit or profile, so it never
raises a dot). `panel` becomes `homework || quiz || units`.

## Student — learning

### `GET /learning` (changed `sharedPresentations[].activities[]`)

`ActivitySummary`:

```json
{ "id": "uuid", "title": null, "page": 4, "format": "EXERCISE",
  "status": "PENDING|SUBMITTED|REVIEWED|GRADED", "scorePercent": null }
```

These fields are removed: `position`, `triggerFileId`, `triggerPage`, `instructionsText`,
`youtubeUrl` and `imageId`. A unit's PDF is still delivered as a `sharedPresentations`
entry, with its `unit` reference and `contentUnitPosition`, so the mixed unit sequence keeps
working. Standalone shared presentations always have `activities: []`.

### `GET /learning/activities/{id}` · `PUT /learning/activities/{id}` · `PUT /learning/activities/{id}/answers`

The paths and the submit/grade behaviour are unchanged. Each submit sets
`teacher_seen_at = NULL`.

`ActivityItemResponse`:
- removed: `level`, `homeworkType`, `triggerFileId`, `triggerPage`, `instructionsText`, `youtubeUrl`, `imageId`
- added: `presentationId`, `fileId`, `page`, `unitId`, so the client can render the instructions page with `GET /learning/presentations/{presentationId}/files/{fileId}`

The access gate is unchanged: the activity's presentation must be accessible to the student,
which for a unit PDF means through a `unit_assignments` row.

### REMOVED

`GET /learning/activities/{id}/instructions`.

## Error codes (new)

| Code | HTTP | When |
|---|---|---|
| `ACTIVITIES_OUT_OF_RANGE` | 409 | PDF replacement would delete activities and was not confirmed |
| `ACTIVITY_PAGE_TAKEN` | 409 | marking a page that already has an activity |
| `ACTIVITY_HAS_SUBMISSIONS` | 409 | unmarking a page with submissions without confirmation |

Removed: `ACTIVITY_REORDER_INVALID`.
