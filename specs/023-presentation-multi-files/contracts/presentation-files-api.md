# Contract: Presentation multi-file API

Extends admin presentation authoring and student learning download. Replaces the singular `/file` endpoints and scalar `hasFile` / `originalFileName` fields documented in earlier specs (e.g. `specs/010-class-units/contracts/api.md`).

Auth unchanged: admin routes require `ADMIN`; learning routes require `STUDENT` or `ADMIN` with existing share-or-unit gate.

---

## Shared shapes

### `PresentationFileSummary`

```json
{
  "id": "uuid",
  "displayName": "deck (2).pptx",
  "originalName": "deck.pptx",
  "contentType": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  "byteSize": 1234567,
  "createdAt": "2026-08-03T12:00:00Z"
}
```

- List order is always **oldest-first** (`createdAt` ascending).
- `displayName` is what the UI shows and what download uses for `Content-Disposition`.

### Presentation list/detail (admin) — field change

`PresentationSummary` / `PresentationDetail` replace:

```json
{ "hasFile": true, "originalFileName": "deck.pptx" }
```

with:

```json
{
  "files": [ /* PresentationFileSummary[], oldest-first */ ]
}
```

Empty attachment set → `"files": []`.

### Learning overview — field change

Each entry in `sharedPresentations` on `GET /api/v1/learning` likewise uses `files[]` instead of `hasFile` / `originalFileName`. `unit` grouping behavior is unchanged.

---

## Admin — add file

### `POST /api/v1/admin/presentations/{presentationId}/files`

- Multipart form field: `file` (exactly one file per request).
- Validates: non-empty; ≤ 50 MiB; PPTX or PDF (extension and/or content type, same rules as before).
- Rejects with **400** if presentation already has **10** files (`error` + Spanish `message`).
- On success: persists metadata + disk bytes; assigns `displayName` (auto-suffix on collision); **does not** remove existing files.
- **200** → updated `PresentationDetail` (includes full `files` list).
- **404** if presentation missing.

> Former `POST …/file` (replace/upsert) is **removed**.

---

## Admin — remove one file

### `DELETE /api/v1/admin/presentations/{presentationId}/files/{fileId}`

- Deletes that row and its disk blob only.
- **204** on success.
- **404** if presentation or file missing, or file does not belong to that presentation.

> Former `DELETE …/file` (clear the only file) is **removed**.

---

## Admin — download one file (optional parity)

### `GET /api/v1/admin/presentations/{presentationId}/files/{fileId}`

- Streams bytes; `Content-Type` from stored type; `Content-Disposition: attachment; filename="<displayName>"`.
- **200** file body; **404** if not found / wrong presentation.

---

## Student — download one file

### `GET /api/v1/learning/presentations/{presentationId}/files/{fileId}`

- Allowed if caller may access the presentation (legacy share **or** assigned unit) **and** `fileId` belongs to that presentation.
- Same response headers as admin download (`filename` = `displayName`).
- **404** (presentation-not-found style) if access denied, presentation missing, or file not on that presentation.

> Former `GET …/presentations/{id}/file` is **removed**.

---

## Error expectations

| Situation | Status | Notes |
|-----------|--------|--------|
| Empty / oversized / wrong type | 400 | Existing file list unchanged |
| Already 10 files | 400 | Clear limit message |
| Unknown presentation or file | 404 | |
| Unauthenticated / wrong role | 401 / 403 | Existing security |

Payload shape remains `{"error":"…","message":"…"}` per global handler.

---

## Contract test checklist

- Upload two allowed files → detail `files.length === 2`, oldest first; second upload does not drop the first.
- Upload duplicate `originalName` → distinct `displayName`s (`deck.pptx`, `deck (2).pptx`); downloads use those names.
- Remove first of a colliding pair → remaining keeps `deck (2).pptx`.
- Eleventh upload → 400; list still 10.
- Delete one file → student overview/download no longer offers that `fileId`; other files intact.
- Presentation with migrated legacy single file → appears as one-element `files[]`; student can download via new path.
- Student without share/unit → 404 on file download.
- Delete presentation → no leftover DB rows; disk blobs for its file ids gone (or cleaned by delete path).
