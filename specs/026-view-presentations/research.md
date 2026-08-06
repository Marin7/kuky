# Research: View Presentations On-Site

## 1. How to display PDFs without a new library

**Decision**: Authenticated `fetch` of the existing file endpoint → `blob()` → `URL.createObjectURL` → embed in an `<iframe>` (or `<embed>`) on a dedicated page. Revoke the object URL on unmount.

**Rationale**: Cookies/`credentials: 'include'` already work for download. Blob URLs avoid navigating the browser to an `attachment` response (which would force download). Native PDF rendering meets the spec’s “modern browser” assumption and avoids PDF.js weight (YAGNI).

**Alternatives considered**:
- Direct iframe `src` to the API URL — `Content-Disposition: attachment` often forces download instead of inline display.
- PDF.js / react-pdf — better cross-browser consistency and custom chrome, but new dependency and complexity not required for v1.
- Convert PPTX → PDF on upload — out of scope; PowerPoint stays download-only.

## 2. Backend: change Content-Disposition vs leave as-is

**Decision**: **No backend change** for MVP. Keep `Content-Disposition: attachment` on `GET /api/v1/learning/presentations/{id}/files/{fileId}`. Viewing uses the blob path; downloading keeps using `a.download` + display name.

**Rationale**: One code path for auth/share checks; no dual disposition query params; download UX unchanged.

**Alternatives considered**:
- `inline` for `application/pdf`, `attachment` for PPTX — nice if we later deep-link the raw URL; unnecessary while the client uses blobs.
- Separate `GET …/view` endpoint — duplicates access logic for no gain.

## 3. Routing: full-page, bookmarkable viewer

**Decision**: New TanStack file route:

`aprendizaje_.presentacion.$presentationId.archivo.$fileId.tsx`

→ `/aprendizaje/presentacion/$presentationId/archivo/$fileId`

Explicit **Back to learning materials** uses `navigate({ to: '/aprendizaje' })` (always), not `history.back()`. Browser Back remains default history behavior.

**Rationale**: Matches clarification (full page, refresh/bookmark OK, explicit back always to learning). Same pathless-layout pattern as `aprendizaje_.tarea.$homeworkId`.

**Alternatives considered**:
- Overlay/dialog — rejected in clarify.
- New browser tab — rejected in clarify.
- Query-param on `/aprendizaje` — weaker bookmark story and messier SSR/auth.

## 4. Detecting viewable files

**Decision**: Treat a file as viewable when `contentType === 'application/pdf'` (already on `PresentationFileSummary` from learning overview). List shows primary Open for PDFs; PPTX and other types get Download only. If a non-PDF URL is opened directly, viewer shows an error + Back (no broken iframe).

**Rationale**: Spec FR-004/FR-009; overview already exposes `contentType`; no extra metadata API.

**Alternatives considered**:
- Infer from `displayName` extension only — fragile vs stored type.
- Server-side “viewable” flag — redundant with content type.

## 5. Loading file bytes on the viewer page

**Decision**: On mount, call the same learning file GET (shared helper with download). Show loading until blob URL is ready; on failure show clear error + Back. Do not offer Download on the viewer chrome.

**Rationale**: FR-002/FR-006; download stays on the list (clarify). Refresh re-runs the fetch (FR-010).

**Alternatives considered**:
- Pass blob via router state — breaks refresh/bookmark.
- Prefetch all PDFs from the list — wasteful for multi-file presentations.

## 6. Auth and access errors

**Decision**: Reuse existing learning gate (share or unit assignment). Unauthenticated → existing learning pattern (redirect to `/cuenta`). Access denied / missing file → treat like today’s download failure (typically opaque 404); viewer shows friendly error + Back.

**Rationale**: FR-005/SC-004; no new security model.

## 7. i18n

**Decision**: Add es/en/ro strings for Open/View, Download (existing), Back to learning materials, loading, load error, and non-PDF not viewable.

**Rationale**: Site already localizes learning copy; FR-009 needs distinct actions.
