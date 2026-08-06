# Data Model: View Presentations On-Site

## No schema changes

This feature does **not** add tables, columns, or Flyway migrations. It reuses existing presentation file storage and sharing.

## Existing entities (relevant fields)

### Presentation

| Attribute | Notes |
|-----------|--------|
| id | UUID |
| title | Shown on learning list / can label viewer chrome |
| shares / unit assignment | Existing access gate for students (unchanged) |

### Presentation File

| Attribute | Notes |
|-----------|--------|
| id | UUID — path param on viewer route |
| presentation_id | Parent presentation |
| display_name | List label; download filename |
| original_name | Stored; not required for viewer |
| content_type | **View eligibility**: `application/pdf` → Open/View; otherwise download-only |
| byte_size | Optional UX (e.g. large-file expectation); not required for gating |
| created_at | List order oldest-first (unchanged) |
| bytes | On disk via existing `PresentationFileStore` |

### Presentation Share (and unit assignment)

Unchanged. Student may open or download a file only if they may access the parent presentation under today’s learning rules.

## Logical concepts (UI / API usage)

### Viewable file

A presentation file the student may **open on-site**:

- Caller has access to the presentation
- `contentType` is `application/pdf`

### Downloadable file

Any attached file the student may access — PDF and PowerPoint — downloaded from the learning list only (not from the viewer chrome).

### Viewer address

Logical identity of a full-page view session:

- `presentationId` + `fileId`
- Stable across refresh/bookmark while access lasts
- Does not store server-side session state

## Validation rules (unchanged upload)

- Allowed uploads remain PowerPoint and PDF; size/cap rules unchanged
- This feature does not alter teacher upload or multi-file limits

## State transitions

None. Opening a PDF does not mutate presentation, file, or share records.
