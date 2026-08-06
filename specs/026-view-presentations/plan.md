# Implementation Plan: View Presentations On-Site

**Branch**: `026-view-presentations` | **Date**: 2026-08-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/026-view-presentations/spec.md`

## Summary

Let students **open PDF presentation files on a dedicated full-page viewer** from Aprendizaje, without downloading first. PowerPoint stays download-only. Download remains on the learning file list (secondary for PDFs). Reuse the existing authenticated file GET; the client loads bytes into a blob URL and embeds them in the viewer. No schema change; no new PDF library (browser-native display via `<iframe>`).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Spring Security — **no new dependencies**; existing `GET /api/v1/learning/presentations/{id}/files/{fileId}` reused as-is.
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next — **no new dependencies** (blob URL + `<iframe>`).

**Storage**: PostgreSQL 18 + on-disk presentation files — **no migration**. Eligibility for view = stored `contentType` is `application/pdf`.

**Testing**: Backend: existing learning presentation-file access tests remain green (no behavior change required unless disposition tweaks are added later). Frontend: browser verify Open → full-page PDF, Back to learning, download secondary, PPTX download-only, refresh/bookmark, revoked access.

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Student begins reading a typical PDF within ~10s under normal conditions (SC-001); large files show loading until the blob is ready.

**Constraints**: PDF-only in-browser view; viewer has no download control; explicit “Back to learning materials” → `/aprendizaje`; bookmark/refresh OK while shared; same share/unit access gate as download today.

**Scale/Scope**: Student learning UI + one new route; i18n es/en/ro; admin upload/share unchanged.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. No PDF.js, no PPTX conversion, no new API or tables. Reuse file download endpoint + native browser PDF display.
- **II. Component-Driven UI** — PASS. Named components for file-list actions and the full-page viewer (not ad-hoc DOM in the route only).
- **III. Evolution-Ready Architecture** — PASS. Fetch/view helpers live in `front-end/src/lib/learning.ts`; route is thin.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart requires browser verification; branch `026-view-presentations`.

**Post-design re-check**: PASS — contracts document route + reuse of existing GET; no schema; Complexity Tracking empty.

**Result: PASS — no violations.**

## Project Structure

### Documentation (this feature)

```text
specs/026-view-presentations/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── presentation-viewer.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/
└── (unchanged for MVP — existing LearningController.downloadPresentation)

front-end/src/
├── routes/
│   ├── aprendizaje.tsx                                      # (unchanged entry)
│   └── aprendizaje_.presentacion.$presentationId.archivo.$fileId.tsx  # NEW: full-page viewer
├── lib/learning.ts                                          # EDIT: fetchPresentationFileBlob (or view helper); keep downloadPresentationFile
├── components/learning/
│   ├── LearningContent.tsx                                  # EDIT: PDF primary Open → navigate; secondary Download; PPTX download-only
│   └── PresentationPdfViewer.tsx                            # NEW: chrome (title, Back, loading/error) + iframe blob
└── i18n/locales/{es,en,ro}.ts                               # EDIT: open/view, back, loading, errors
```

**Structure Decision**: Frontend-only feature on top of the existing learning file endpoint. Pathless route under `aprendizaje_` matches other learning subpages (`tarea`, `lectura`, …).

## Complexity Tracking

> No constitution violations requiring justification.
