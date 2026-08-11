# Contract: Admin Student Profile API

**Feature**: `036-admin-student-view` | **Date**: 2026-08-11  
**Endpoint**: `GET /api/v1/admin/students/{id}/profile` (existing admin matcher; path as currently wired under admin students)

Auth: admin session (existing). Unchanged error shape on missing student.

## Response change

**Remove** the `progress` object from the JSON body.

### Before (relevant fragment)

```json
{
  "id": "...",
  "homeworks": [ /* ... */ ],
  "presentations": [ /* ... */ ],
  "progress": {
    "units": [],
    "homeworkBreakdown": { "pending": 0, "submitted": 0, "completed": 0 },
    "activityBreakdown": { "pending": 0, "submitted": 0, "completed": 0 },
    "attendedClasses": 0
  }
}
```

### After (relevant fragment)

```json
{
  "id": "...",
  "bookings": [ /* unchanged */ ],
  "homeworks": [
    {
      "id": "...",
      "title": "...",
      "status": "PENDING",
      "submittedAt": null,
      "needsReview": false,
      "submissionId": null,
      "scorePercent": null,
      "hasTeacherFeedback": false
    }
  ],
  "presentations": [ /* unchanged */ ]
}
```

No `progress` key. Clients MUST NOT rely on it.

## Unchanged

- Homework row fields used for list actions (`needsReview`, `submissionId`, `scorePercent`, `hasTeacherFeedback`, `status`, `submittedAt`, `title`).
- Bookings (including `noShow`, `isCompanionStudent`) and presentations.
- Interests fields.
- Separate placement evaluation endpoint used by the profile page (unchanged by this feature).

## Client contract (UI)

| Surface | Behavior |
|---------|----------|
| Collapsed Tareas | `homeworks.length` + counts from status buckets (see [data-model.md](../data-model.md)) |
| Expanded Tareas | Full `homeworks` list, full width under stats row |
| Removed | Progreso section; duplicate lower Tareas section |

Breaking change: only the admin student profile consumer (`front-end` profile page / `getStudentProfile`). No public student API impact.
