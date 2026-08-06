# Contract: Presentation on-site viewer

Student-facing viewing of PDF presentation files. Builds on [presentation multi-file API](../../023-presentation-multi-files/contracts/presentation-files-api.md). Admin upload/share contracts unchanged.

---

## Backend — reuse existing file GET

### `GET /api/v1/learning/presentations/{presentationId}/files/{fileId}`

**Unchanged** for this feature:

- Auth: `STUDENT` or `ADMIN` with existing presentation access (share or unit).
- **200**: body = file bytes; `Content-Type` = stored type; `Content-Disposition: attachment; filename="<displayName>"`.
- **404**: missing presentation/file or access denied (same opacity as today).

**Client usage**:

| Intent | Client behavior |
|--------|-----------------|
| Download | Fetch → blob → temporary `<a download={displayName}>` (existing) |
| View (PDF) | Fetch → blob → `URL.createObjectURL` → `<iframe src={blobUrl}>` on the viewer page |

No new query params or endpoints in MVP.

### Learning overview (unchanged shape)

`GET /api/v1/learning` → `sharedPresentations[].files[]` includes `id`, `displayName`, `contentType`, etc. UI uses `contentType === "application/pdf"` to offer Open/View.

---

## Frontend — viewer route

### Path

`/aprendizaje/presentacion/$presentationId/archivo/$fileId`

File route: `aprendizaje_.presentacion.$presentationId.archivo.$fileId.tsx`

### Behavior

1. Requires authenticated student/admin session (same gate as `/aprendizaje`; guests → `/cuenta`).
2. Loads file via learning GET above with `credentials: "include"`.
3. If load succeeds and content is PDF → show full-page viewer with embedded PDF (blob URL).
4. If load fails → error message + **Back to learning materials**.
5. If file is not a PDF (e.g. direct navigation to a PPTX id) → not-viewable message + Back (do not pretend to render).
6. Chrome includes:
   - Explicit **Back to learning materials** → always `navigate` to `/aprendizaje`
   - Optional title/label (presentation title and/or `displayName`)
   - **No** Download control on this page
7. Refresh / revisit same URL reloads the file while access lasts.

### Learning list actions

On `/aprendizaje` presentation file rows:

| `contentType` | Primary action | Secondary |
|---------------|----------------|-----------|
| `application/pdf` | Open/View → navigate to viewer route | Download (existing helper) |
| other (e.g. PPTX) | Download only | — |

---

## Errors (UI)

| Situation | Student sees |
|-----------|----------------|
| Network / 404 / denied | Clear load error + Back |
| Non-PDF on viewer route | Not viewable on site + Back |
| Loading | Loading state; Back still available |

---

## Out of scope

- Teacher admin preview route
- PPTX in-browser rendering or server-side conversion
- Changing `Content-Disposition` or adding a dedicated view API
- Download control inside the viewer
