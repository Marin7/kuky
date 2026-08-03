# Implementation Plan: Multiple Files per Presentation

**Branch**: `023-presentation-multi-files` | **Date**: 2026-08-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/023-presentation-multi-files/spec.md`

## Summary

Change presentation attachments from a hard 1:1 (`presentation_files.presentation_id` as PK, singular `/file` endpoints, `hasFile`/`originalFileName` scalars) to a 1:N collection: up to 10 PPTX/PDF files per presentation, one upload action at a time, oldest-first listing, per-file remove/download, and auto-suffixed **persisted** `display_name` on original-name collisions (stable after removals). Existing single-file rows migrate in place (new UUID PK + disk rename from `{presentationId}.pptx` → `{fileId}.pptx`). Admin Presentations tab, Units content picker, and student learning download UI switch to a files list.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate, Flyway, PostgreSQL. **No new dependencies.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next. **No new dependencies.**

**Storage**:
- PostgreSQL 18 — Flyway `V5__presentation_multi_files.sql`: reshape `presentation_files` to multi-row (surrogate `id`, FK `presentation_id`, add `display_name`); existing rows kept with `display_name = original_name`.
- Disk: `PresentationFileStore` keyed by **file id** (`{fileId}.pptx` opaque suffix, same as today’s convention). One-time rename of existing `{presentationId}.pptx` → `{fileId}.pptx` during migration (SQL alone cannot rename files — app bootstrap helper or migration-time Java `CommandLineRunner` / Flyway Java migration; prefer a small `PresentationFileDiskMigrator` `CommandLineRunner` that remaps once using DB rows — see research).

**Testing**:
- Backend: JUnit 5 + Mockito — expand `PresentationServiceTest` for upload-add (not replace), 10-cap, display-name suffix, remove-one, get-by-fileId; repository/integration coverage for list oldest-first and cascade delete.
- Frontend: browser verification per constitution (admin multi-file list, student per-file download, legacy single-file still works).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (existing `front-end/` + `back-end/`).

**Performance Goals**: N/A — ≤10 small metadata rows per presentation; downloads stream existing byte arrays.

**Constraints**: Max 10 files/presentation; ≤50 MB and PPTX/PDF per file (unchanged); one file per upload action; display names fixed at insert; no zip/reorder/rename/in-place-replace; share/unit access remains per-presentation.

**Scale/Scope**: Single-teacher site; one migration + disk remap; DTO/API reshape; admin Presentations + Units picker + learning download UI; i18n for multi-file copy.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Extend existing `presentation_files` + `PresentationFileStore` / `PresentationService`; no new package, no object storage, no zip, no reorder UI. Cap and suffix logic stay in the service layer.
- **II. Component-Driven UI** — PASS. Named React updates in existing `PresentationAdminList`, `UnitContentPicker`, `LearningContent` (file list / per-file download), no raw DOM beyond existing download-anchor pattern.
- **III. Evolution-Ready Architecture** — PASS. API clients remain in `front-end/src/lib/admin.ts` and `learning.ts`; components call those modules.
- **Technology Stack** — PASS. No new dependencies.
- **Development Workflow** — PASS. Quickstart includes browser checks; branch `023-presentation-multi-files`.

**Post-design re-check**: PASS — contracts stay REST-over-existing auth; data model is one table reshape + `display_name`; Complexity Tracking empty.

**Result: PASS — no violations.**

## Project Structure

### Documentation (this feature)

```text
specs/023-presentation-multi-files/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── presentation-files-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V5__presentation_multi_files.sql

back-end/src/main/java/com/kuky/backend/
├── presentations/
│   ├── model/PresentationFile.java              # EDIT: id, displayName; presentationId is FK
│   ├── repository/PresentationRepository.java   # EDIT: listFiles, insertFile, deleteFile(fileId), count…
│   ├── service/PresentationFileStore.java       # EDIT: path by fileId; deleteQuietly(fileId); migrate helpers
│   ├── service/PresentationService.java         # EDIT: add/upload (not upsert), suffix, cap 10, per-file get/remove
│   └── …                                        # optional small DisplayNameAllocator helper if keeps service lean
├── admin/
│   ├── controller/PresentationAdminController.java  # EDIT: /files collection endpoints
│   └── dto/PresentationSummary.java, PresentationDetail.java  # EDIT: files[] replaces hasFile/originalFileName
├── learning/
│   ├── controller/LearningController.java       # EDIT: GET …/files/{fileId}
│   ├── service/LearningService.java             # EDIT: getPresentationFile(email, presentationId, fileId)
│   └── dto/SharedPresentationSummary.java       # EDIT: files[]
└── config/ or presentations/                    # NEW: one-shot disk remapper CommandLineRunner (idempotent)

front-end/src/
├── lib/admin.ts                                 # EDIT: files types; upload/delete/download by fileId
├── lib/learning.ts                              # EDIT: files[]; downloadPresentationFile(id, fileId, name)
├── components/admin/presentations/PresentationAdminList.tsx  # EDIT: multi-file list, upload-add, per-file remove
├── components/admin/units/UnitContentPicker.tsx # EDIT: same files list pattern
├── components/learning/LearningContent.tsx      # EDIT: per-file download actions
└── i18n/locales/{es,en,ro}.ts                   # EDIT: multi-file / limit / remove labels
```

**Structure Decision**: Stay inside existing `presentations`, `admin`, and `learning` packages and the three UI surfaces that already manage presentation files. No new top-level domain package.

## Complexity Tracking

> No constitution violations to justify.
