---
description: "Task list for on-site PDF presentation viewing"
---

# Tasks: View Presentations On-Site

**Input**: Design documents from `specs/026-view-presentations/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/presentation-viewer.md](./contracts/presentation-viewer.md), [quickstart.md](./quickstart.md)

**Tests**: No new backend tests required for MVP (file GET unchanged). Frontend has no unit-test framework â€” verify via [quickstart.md](./quickstart.md) in a browser (constitution).

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths are relative to the repo root (`front-end/`, `back-end/`)

---

## Phase 1: Setup

**Purpose**: Confirm MVP stays frontend-only; no schema or API work.

- [x] T001 Confirm no Flyway migration and no backend endpoint changes for MVP per [plan.md](./plan.md) / [research.md](./research.md) (reuse `GET /api/v1/learning/presentations/{id}/files/{fileId}` as-is); note view eligibility rule (`contentType === "application/pdf"`) in a one-line comment on the helper added in Phase 2.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared fetch helper, PDF detection, and i18n keys that all stories need before UI wiring.

**âš ï¸ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 Add `isPresentationPdf(contentType: string): boolean` and `fetchPresentationFileBlob(presentationId, fileId): Promise<Blob>` in `front-end/src/lib/learning.ts` (authenticated fetch with `credentials: "include"`; throw on non-OK like `downloadPresentationFile`); keep existing `downloadPresentationFile` unchanged. Depends on T001.
- [x] T003 [P] Add learning presentation-viewer i18n keys (open/view, download if missing, back to learning, loading, load error, not viewable) in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts`.

**Checkpoint**: Blob helper + i18n ready; viewer route and list actions still TODO.

---

## Phase 3: User Story 1 - Student opens a PDF presentation in the browser (Priority: P1) ðŸŽ¯ MVP

**Goal**: Student opens a shared PDF from Aprendizaje on a dedicated full-page viewer (blob + iframe), with loading/error states, explicit Back always to `/aprendizaje`, and refresh/bookmark working while access lasts. No download control on the viewer.

**Independent Test**: Share a PDF with a student â†’ Open/View â†’ readable PDF on full page â†’ Back lands on `/aprendizaje` â†’ refresh same URL still works â†’ guest/denied cannot see content.

### Implementation for User Story 1

- [x] T004 [US1] Create `front-end/src/components/learning/PresentationPdfViewer.tsx`: props for `presentationId`, `fileId`, optional title/displayName; load blob via T002; show loading; embed PDF in `<iframe>` with object URL; revoke URL on unmount; show error + always render "Back to learning materials" that `navigate`s to `/aprendizaje`; **no** download button. Depends on T002, T003.
- [x] T005 [US1] Add route `front-end/src/routes/aprendizaje_.presentacion.$presentationId.archivo.$fileId.tsx` wiring auth like `aprendizaje.tsx` (guest → `/cuenta`), rendering `PresentationPdfViewer`, and SEO/head as needed. Depends on T004.
- [x] T006 [US1] Update `front-end/src/components/learning/LearningContent.tsx` so PDF files (`isPresentationPdf`) show primary Open/View that navigates to `/aprendizaje/presentacion/$presentationId/archivo/$fileId`. Depends on T002, T005.
- [x] T007 [US1] On the viewer route/component, treat failed fetch (network/404/denied) with clear error copy from i18n and keep Back available (FR-006). Depends on T004, T003.

**Checkpoint**: PDFs open full-page; Back and refresh work. MVP deliverable (download affordances refined in US2/US3).

---

## Phase 4: User Story 2 - Download remains available for every file (Priority: P1)

**Goal**: Every accessible file (PDF and PowerPoint) can still be downloaded from the learning file list by display name; opening a PDF does not remove or break download. Viewer still has no download control.

**Independent Test**: From the list, download PDF and PPTX successfully; after viewing a PDF and returning via Back, download that PDF again.

### Implementation for User Story 2

- [x] T008 [US2] In `front-end/src/components/learning/LearningContent.tsx`, keep secondary Download for PDF rows calling existing `downloadPresentationFile` with `displayName` (primary remains Open/View from T006). Depends on T006.
- [x] T009 [US2] Confirm PowerPoint (and other non-PDF) rows still expose Download via `downloadPresentationFile` in `front-end/src/components/learning/LearningContent.tsx` without requiring Open. Depends on T006.
- [x] T010 [US2] Browser-check per [quickstart.md](./quickstart.md) "download still works": PDF + PPTX download from list after a view round-trip; viewer chrome still has no download. Depends on T008, T009, T005.

**Checkpoint**: View and download coexist; list remains the only download surface.

---

## Phase 5: User Story 3 - Non-viewable files stay download-only (Priority: P2)

**Goal**: PowerPoint (and non-PDF types) never get an Open/View action; direct navigation to a non-PDF viewer URL shows a clear not-viewable state + Back, not a broken iframe.

**Independent Test**: PPTX list row is download-only; visiting viewer URL with a PPTX `fileId` shows not-viewable + Back.

### Implementation for User Story 3

- [x] T011 [US3] Harden `front-end/src/components/learning/LearningContent.tsx` so Open/View is rendered **only** when `isPresentationPdf(f.contentType)`; never for PPTX. Depends on T006, T002.
- [x] T012 [US3] In `front-end/src/components/learning/PresentationPdfViewer.tsx` (and/or the route), if the loaded blob's type / known `contentType` is not PDF (e.g. student bookmarked a PPTX file id), show not-viewable message from i18n + Back — do not embed. Depends on T004, T003.
- [x] T013 [US3] Browser-check per [quickstart.md](./quickstart.md) "PowerPoint download-only" and load-failure paths. Depends on T011, T012.

**Checkpoint**: Non-PDFs cannot be "opened" on-site; errors are clear.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end validation and cleanup across stories.

- [x] T014 Run full [quickstart.md](./quickstart.md) browser checklist (Open PDF, Back, refresh/bookmark, downloads, PPTX-only, access denied, guest redirect).
- [x] T015 [P] Run `npm run lint` in `front-end/` and fix any issues introduced by the viewer/list changes.
- [x] T016 Confirm `back-end/` untouched for this feature (or only incidental); if any backend file changed accidentally, revert unless intentionally documented.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Start immediately
- **Foundational (Phase 2)**: Depends on Setup â€” **BLOCKS** all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational â€” **MVP**
- **User Story 2 (Phase 4)**: Depends on US1 list/viewer existing (extends `LearningContent` download UX)
- **User Story 3 (Phase 5)**: Depends on US1 viewer + list Open gating
- **Polish (Phase 6)**: After desired stories complete

### User Story Dependencies

- **US1 (P1)**: After Phase 2 only â€” delivers MVP viewer
- **US2 (P1)**: After US1 Open navigation exists â€” primarily list download polish/verification
- **US3 (P2)**: After US1 viewer exists â€” non-PDF gating + not-viewable state

### Parallel Opportunities

- T003 can run in parallel with T002 once T001 is done
- T014/T015 sequencing: lint can overlap with manual quickstart prep; full checklist after US3 preferred
- US2 and US3 both touch `LearningContent.tsx` / viewer â€” prefer sequential (US2 then US3) to avoid merge conflicts

### Parallel Example: Foundational

```text
Task: "Add isPresentationPdf + fetchPresentationFileBlob in front-end/src/lib/learning.ts"
Task: "Add viewer i18n keys in front-end/src/i18n/locales/{es,en,ro}.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1â€“2
2. Complete Phase 3 (US1)
3. **STOP and VALIDATE**: Open PDF full-page + Back + refresh
4. Demo if ready (download can remain as todayâ€™s single button until US2)

### Incremental Delivery

1. Setup + Foundational â†’ helpers ready
2. US1 â†’ PDF viewer MVP
3. US2 â†’ Download secondary preserved
4. US3 â†’ PPTX download-only + not-viewable
5. Polish â†’ full quickstart

---

## Notes

- Backend intentionally unchanged for MVP â€” do not add PDF.js or Content-Disposition changes unless a later bug forces it
- `[P]` = different files, no incomplete dependencies
- Commit after each task or logical group
- Avoid embedding download on the viewer page (clarify decision)
