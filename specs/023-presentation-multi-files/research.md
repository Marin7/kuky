# Research: Multiple Files per Presentation

## Decision 1 — Reshape `presentation_files` in place (1:N)

- **Decision**: Keep the table name. Drop PK on `presentation_id`, add surrogate `id UUID PRIMARY KEY`, keep `presentation_id` as FK `ON DELETE CASCADE`, add `display_name VARCHAR(255) NOT NULL`, index `(presentation_id, created_at)` for oldest-first lists. Migrate existing rows: generate `id`, set `display_name = original_name`. Shipped as idempotent Flyway `V5__presentation_multi_files.sql`.
- **Rationale**: Minimal schema churn; CASCADE already deletes files with the presentation (FR-009); no parallel table or dual-write period.
- **Alternatives considered**: New `presentation_attachments` table + copy — more migration risk for the same end state. Soft-delete columns — out of scope.

## Decision 2 — Persist `display_name` at insert; never renumber

- **Decision**: On upload, compute `display_name` from `original_name` and **existing display names** on that presentation (case-insensitive basename+extension collision). First file keeps the original name; later collisions get `name (2).ext`, `name (3).ext`, … choosing the smallest unused `n ≥ 2`. Store the result; removals leave remaining names unchanged (FR-014).
- **Rationale**: Spec requires stable labels for downloads; computing only at read time would renumber after deletes.
- **Alternatives considered**: Reject duplicate originals — rejected in clarify. Renumber on delete — rejected in clarify. Display-only suffix without persistence — fails FR-014 and download filename stability.

## Decision 3 — API: collection endpoints; remove singular `/file`

- **Decision** (same release, FE+BE together):
  - Admin: `POST /api/v1/admin/presentations/{id}/files` (multipart `file`) — **adds** a row (reject at 10).
  - Admin: `DELETE /api/v1/admin/presentations/{id}/files/{fileId}` — remove one.
  - Admin download optional: `GET …/files/{fileId}` if useful for parity; not required for MVP admin UX.
  - Student: `GET /api/v1/learning/presentations/{id}/files/{fileId}` — replace singular `/file`.
  - List/detail/learning overview: `files: [{ id, displayName, originalName?, contentType?, byteSize? }]` oldest-first; drop scalar `hasFile` / `originalFileName` (clients use `files.length` / list).
- **Rationale**: Singular replace semantics conflict with FR-002; fileId is required for per-file remove/download. Coordinated FE update avoids a compat shim.
- **Alternatives considered**: Keep `/file` as “replace primary” — out of scope (clarify). Soft-deprecate both shapes — YAGNI for a single-teacher app.

## Decision 4 — Disk keys by file id; one-shot remap of legacy files

- **Decision**: `PresentationFileStore` paths become `{storageDir}/{fileId}.pptx` (keep opaque `.pptx` suffix). After SQL migration assigns new `id`s, an idempotent `CommandLineRunner` (or equivalent startup step) renames `{presentationId}.pptx` → `{fileId}.pptx` when the old path exists and the new path does not. Classpath seed fallback updates to `presentation-files/{fileId}.pptx` (re-seed / docs if any classpath seeds remain keyed by presentation id).
- **Rationale**: File id is unique per row; presentation id no longer identifies a single blob. Startup remapper avoids putting filesystem IO inside Flyway SQL.
- **Alternatives considered**: Store under `{presentationId}/{fileId}.pptx` — nicer nesting but more migration/code for no product gain. Flyway Java migration for renames — couples deploy to local disk paths awkwardly vs a quiet remapper that no-ops when already migrated.

## Decision 5 — Cap and validation stay in the service layer

- **Decision**: Enforce max 10 with `COUNT(*)` (or list size) before insert in `PresentationService`; keep 50 MB + PPTX/PDF checks as today. Optional DB `CHECK` not required; unique constraint on `(presentation_id, display_name)` recommended to prevent race duplicates of the same display name.
- **Rationale**: Matches existing size/type validation location; UNIQUE on display_name hardens concurrent uploads.
- **Alternatives considered**: DB trigger for cap — heavier than needed. No UNIQUE — risk two identical suffixes under concurrent admin tabs.

## Decision 6 — Frontend surfaces to update

- **Decision**: Update `PresentationAdminList`, `UnitContentPicker` (same file UX), and `LearningContent` to render a files list: upload adds; remove per row; student download per row using `displayName`. Replace/remove singular controls.
- **Rationale**: Units picker already duplicates upload/replace/remove against the same DTOs; leaving it on scalars would break compile/runtime after DTO change.
- **Alternatives considered**: Shared extracted `PresentationFilesEditor` component — nice but optional; can extract if duplication hurts during implement (YAGNI until both edited).
