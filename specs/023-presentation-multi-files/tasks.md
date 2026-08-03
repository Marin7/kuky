---

description: "Task list for multiple files per presentation"
---

# Tasks: Multiple Files per Presentation

**Input**: Design documents from `specs/023-presentation-multi-files/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/presentation-files-api.md](contracts/presentation-files-api.md), [quickstart.md](quickstart.md)

**Tests**: Included for backend â€” [plan.md](plan.md) calls for expanding `PresentationServiceTest` (upload-add, 10-cap, display-name suffix, remove-one, get-by-fileId). Frontend has no test framework â€” verify via [quickstart.md](quickstart.md) in a browser.

**Organization**: Tasks are grouped by user story (from [spec.md](spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Schema reshape every other task depends on.

- [X] T001 Write `back-end/src/main/resources/db/migration/V5__presentation_multi_files.sql` per [data-model.md](data-model.md): add `id UUID` + `display_name VARCHAR(255)`; backfill `id = gen_random_uuid()`, `display_name = original_name`; set NOT NULL; drop PK on `presentation_id`; add `PRIMARY KEY (id)`; add `UNIQUE (presentation_id, display_name)`; add index `(presentation_id, created_at)`; keep FK `presentation_id â†’ presentations(id) ON DELETE CASCADE`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Multi-file persistence, disk keys by file id, DTO `files[]` shape, and service primitives (add / list / remove / display-name / cap) that US1â€“US3 all need.

**âš ï¸ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Reshape `back-end/src/main/java/com/kuky/backend/presentations/model/PresentationFile.java` for multi-file: `id`, `presentationId` (FK), `originalName`, `displayName`, `contentType`, `byteSize`, `createdAt`, optional `byte[] data` for download payload.
- [X] T003 Update `back-end/src/main/java/com/kuky/backend/presentations/service/PresentationFileStore.java` to key paths by **file id** (`{fileId}.pptx`); add `write(UUID fileId, â€¦)`, `read(UUID fileId)`, `deleteQuietly(UUID fileId)`, and `renameIfNeeded(UUID presentationId, UUID fileId)` (or equivalent) for legacy `{presentationId}.pptx` â†’ `{fileId}.pptx`. Update classpath fallback to `presentation-files/{fileId}.pptx`.
- [X] T004 In `back-end/src/main/java/com/kuky/backend/presentations/repository/PresentationRepository.java`: replace singular `findFile` / `upsertFile` / `deleteFile(presentationId)` / `findOriginalFileName` with multi-file ops â€” `listFiles(UUID presentationId)` oldest-first, `countFiles(UUID presentationId)`, `insertFile(...)`, `findFile(UUID presentationId, UUID fileId)`, `deleteFile(UUID presentationId, UUID fileId)`, `listDisplayNames(UUID presentationId)`; update `listSummaries` / shared-summary queries to support loading file collections (or load files in a second query). Depends on T001, T002.
- [X] T005 [P] Create `back-end/src/main/java/com/kuky/backend/admin/dto/PresentationFileSummary.java` (record: `id`, `displayName`, `originalName`, `contentType`, `byteSize`, `createdAt`) per [contracts/presentation-files-api.md](contracts/presentation-files-api.md).
- [X] T006 Replace `hasFile` / `originalFileName` with `List<PresentationFileSummary> files` on `back-end/src/main/java/com/kuky/backend/admin/dto/PresentationSummary.java` and `PresentationDetail.java`. Depends on T005.
- [X] T007 [P] Replace `hasFile` / `originalFileName` with `List<PresentationFileSummary> files` (or learning-local equivalent) on `back-end/src/main/java/com/kuky/backend/learning/dto/SharedPresentationSummary.java`. Depends on T005.
- [X] T008 In `back-end/src/main/java/com/kuky/backend/presentations/service/PresentationService.java`: implement display-name allocation (case-insensitive; first keeps `originalName`, collisions get `base (n).ext` with smallest unused `nâ‰¥2`); `uploadFile` becomes **add** (insert + write by file id, reject at countâ‰¥10, no upsert/replace); `removeFile(presentationId, fileId)`; `getFileData(presentationId, fileId)`; `delete(presentationId)` deletes all file blobs by id; map `files` oldest-first into detail/list (T006). Depends on T003, T004, T006.
- [X] T009 Create idempotent `PresentationFileDiskMigrator` `CommandLineRunner` under `back-end/src/main/java/com/kuky/backend/presentations/` (or `config/`): for each `presentation_files` row, rename `{presentationId}.pptx` â†’ `{fileId}.pptx` when old exists and new does not (no-op when already migrated). Depends on T003, T004.
- [X] T010 [P] Update `front-end/src/lib/admin.ts` types: `PresentationFileSummary`, `PresentationSummary` / `PresentationDetail` with `files[]` (remove `hasFile` / `originalFileName`).
- [X] T011 [P] Update `front-end/src/lib/learning.ts` types: `SharedPresentationSummary.files[]` (remove `hasFile` / `originalFileName`).

**Checkpoint**: Schema, store, repository, service add/list/remove, DTOs, and disk remapper exist. User stories can begin.

---

## Phase 3: User Story 1 - Teacher attaches several files (Priority: P1) ðŸŽ¯ MVP

**Goal**: Paula can upload multiple PPTX/PDF files one at a time onto a presentation (add, not replace), see oldest-first display names (with auto-suffix on collisions), remove individual files, and hit a clear error at 10 files â€” in admin Presentations and the Units content picker.

**Independent Test**: As admin, open a presentation with no files, upload two allowed files, confirm both listed; upload a same-named third and see a distinct suffix; remove one and confirm the other keeps its display name; attempt an 11th file on a full presentation and get a rejection.

### Tests for User Story 1

- [X] T012 [P] [US1] Extend `back-end/src/test/java/com/kuky/backend/presentations/PresentationServiceTest.java`: upload adds (second upload keeps first); display-name suffix on colliding `originalName`; remove one leaves others with unchanged display names; reject at 10 files; reject empty/oversized/wrong-type without mutating list; `getFileData` by file id; presentation delete cleans all store deletes.

### Implementation for User Story 1

- [X] T013 [US1] In `back-end/src/main/java/com/kuky/backend/admin/controller/PresentationAdminController.java`: replace singular `/file` with `POST /{id}/files` (multipart `file` â†’ add), `DELETE /{id}/files/{fileId}`, and optional `GET /{id}/files/{fileId}` download with `Content-Disposition` filename = `displayName` per [contracts/presentation-files-api.md](contracts/presentation-files-api.md). Depends on T008.
- [X] T014 [P] [US1] In `front-end/src/lib/admin.ts`, replace `uploadPresentationFile` / `deletePresentationFile` with collection clients: `uploadPresentationFile(id, file)` â†’ `POST â€¦/files`, `deletePresentationFile(id, fileId)` â†’ `DELETE â€¦/files/{fileId}` (optional admin download helper). Depends on T010.
- [X] T015 [P] [US1] Add i18n keys in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts` for multi-file admin copy (file list, upload/add, remove one, limit reached, uploading) under `admin.presentations.*`.
- [X] T016 [US1] Update `front-end/src/components/admin/presentations/PresentationAdminList.tsx`: render `item.files` oldest-first; single-file picker upload **adds**; per-file remove; drop Replace; show limit/error messages; merge returned `files` into local state. Depends on T014, T015.
- [X] T017 [US1] Update `front-end/src/components/admin/units/UnitContentPicker.tsx` with the same multi-file list / upload-add / per-file remove behavior against the new admin client APIs. Depends on T014, T015.

**Checkpoint**: Teacher can manage multiple files per presentation in admin (and units picker). Student download may still be incomplete until US2.

---

## Phase 4: User Story 2 - Student downloads each file (Priority: P1)

**Goal**: A student with share/unit access sees every attached file on `/aprendizaje` and can download each individually under its `displayName`.

**Independent Test**: As teacher, attach two named files and share (or unit-assign) with a student; as that student, open learning and download each file with the correct saved names.

### Tests for User Story 2

- [X] T018 [P] [US2] Extend or add coverage in `back-end/src/test/java/com/kuky/backend/learning/LearningServiceTest.java` (and/or controller integration test): `getPresentationFile(email, presentationId, fileId)` succeeds when shared/unit-assigned; 404 when not allowed or fileId not on presentation; overview `sharedPresentations[].files` oldest-first.

### Implementation for User Story 2

- [X] T019 [US2] In `back-end/src/main/java/com/kuky/backend/learning/service/LearningService.java`: map `files` into shared presentation summaries (T007); change download to `getPresentationFile(email, presentationId, fileId)` using share/unit gate + `PresentationService.getFileData`. Depends on T007, T008.
- [X] T020 [US2] In `back-end/src/main/java/com/kuky/backend/learning/controller/LearningController.java`: replace `GET /presentations/{id}/file` with `GET /presentations/{id}/files/{fileId}`; stream with `Content-Disposition` filename = `displayName`. Depends on T019.
- [X] T021 [P] [US2] In `front-end/src/lib/learning.ts`, replace `downloadPresentation` with `downloadPresentationFile(presentationId, fileId, displayName)` calling `GET â€¦/files/{fileId}`. Depends on T011.
- [X] T022 [P] [US2] Add i18n keys in `front-end/src/i18n/locales/es.ts`, `en.ts`, and `ro.ts` for per-file download / empty-files copy under `learning.presentations.*` as needed.
- [X] T023 [US2] Update `front-end/src/components/learning/LearningContent.tsx`: for each presentation, render a download control per `files[]` entry (label = `displayName`); empty `files` shows no-file state (no broken button). Depends on T021, T022.

**Checkpoint**: Students can download every attached file. Admin multi-file (US1) + student download (US2) form the full happy path.

---

## Phase 5: User Story 3 - Existing single-file presentations keep working (Priority: P2)

**Goal**: Pre-feature single-file rows and disk blobs remain usable after migration â€” listed as one-element `files[]`, downloadable by students, extendable with more uploads.

**Independent Test**: Use a presentation that had one file before V4; confirm admin shows it without re-upload, student can download via the new path, then add a second file and see both.

### Implementation for User Story 3

- [X] T024 [US3] Verify/adjust `PresentationFileDiskMigrator` (T009) against a local row that still has `{presentationId}.pptx`: after boot, blob exists at `{fileId}.pptx` and download by file id succeeds; remapper is idempotent on second boot.
- [X] T025 [US3] Smoke-check repository/service mapping for legacy rows: `display_name = original_name`, single-element `files[]` on admin list/detail and learning overview without requiring re-upload (fix any mapper gaps found in T004/T008).
- [X] T026 [US3] Browser-verify per [quickstart.md](quickstart.md) US3: legacy one-file presentation visible in admin + student download; upload a second file â†’ both available.

**Checkpoint**: Migration path is proven; no re-upload required for existing materials.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup, docs alignment, full quickstart pass.

- [X] T027 [P] Grep and remove remaining `hasFile` / `originalFileName` / singular `/file` references in `front-end/` and `back-end/` (including any stale comments or DTO builders).
- [X] T028 [P] Note in `specs/010-class-units/contracts/api.md` (short pointer) that presentation file endpoints are now the multi-file contract in `specs/023-presentation-multi-files/contracts/presentation-files-api.md` (avoid leaving contradictory singular `/file` as the source of truth).
- [X] T029 Run full [quickstart.md](quickstart.md) validation (admin multi-file, collisions, cap, student downloads, legacy continuity) and fix any gaps found.
- [X] T030 [P] Ensure Spanish error messages for 10-file cap and validation failures match existing `PresentationService` style / `GlobalExceptionHandler` mapping.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 â€” start immediately
- **Foundational (Phase 2)**: Depends on T001 â€” **BLOCKS** all user stories
- **US1 (Phase 3)**: Depends on Phase 2 â€” MVP
- **US2 (Phase 4)**: Depends on Phase 2 (uses same service/DTOs); ideally after US1 admin can create multi-file fixtures, but API-level US2 can proceed once T008/T007 exist
- **US3 (Phase 5)**: Depends on Phase 2 remapper + US1/US2 download paths for full browser proof
- **Polish (Phase 6)**: After desired stories complete

### User Story Dependencies

- **US1**: After Foundational â€” no dependency on US2/US3
- **US2**: After Foundational â€” needs learning `files[]` + per-file download; can use admin API from US1 for fixtures
- **US3**: After Foundational remapper; browser proof needs US1 UI + US2 download

### Parallel Opportunities

- T002 / T003 / T005 after T001 (T004 needs T001+T002)
- T006 / T007 after T005
- T010 / T011 anytime after contract shape is known (parallel with backend foundational)
- T012 tests can be written alongside T008
- T014 / T015 parallel before T016/T017
- T021 / T022 parallel before T023
- T027 / T028 / T030 in polish

---

## Parallel Example: User Story 1

```bash
# After Foundational:
Task: "Extend PresentationServiceTest for add/suffix/cap/remove"
Task: "Wire PresentationAdminController /files endpoints"
Task: "Update admin.ts collection clients + i18n keys"
# Then sequentially:
Task: "PresentationAdminList multi-file UI"
Task: "UnitContentPicker multi-file UI"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1â€“2 (migration, store, service, DTOs, remapper)
2. Complete Phase 3 (admin API + Presentations + Units UI + service tests)
3. **STOP and VALIDATE**: multi-file add/remove/suffix/cap in admin
4. Proceed to US2 for student value

### Incremental Delivery

1. Setup + Foundational â†’ multi-file domain ready
2. US1 â†’ teacher MVP
3. US2 â†’ student downloads
4. US3 â†’ legacy continuity proof
5. Polish â†’ quickstart + doc/stale-ref cleanup

### Suggested MVP scope

**US1 only** (admin can attach/remove multiple files). Ship US2 next so students receive the materials.

---

## Notes

- [P] = different files, no incomplete-task dependencies
- Do not keep singular replace semantics â€” upload always **adds**
- Persist `display_name` at insert; never renumber on delete
- Commit after each task or logical group
- Frontend verification is browser-only per constitution
