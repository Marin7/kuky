# Quickstart: Multiple Files per Presentation

Validate the multi-file presentation feature end-to-end against [spec.md](spec.md), [data-model.md](data-model.md), and [contracts/presentation-files-api.md](contracts/presentation-files-api.md).

## Prerequisites

1. PostgreSQL `kuky_dev` running; Flyway applies through `V5__presentation_multi_files.sql` on backend start.
2. Backend: `./gradlew bootRun --args='--spring.profiles.active=local'` → `:8081`.
3. Frontend: `npm run dev` in `front-end/` → `:8080`.
4. Admin account (teacher email promoted to `ADMIN`) and at least one `STUDENT` account.
5. Sample `.pptx` and/or `.pdf` files under 50 MB (include two files with the **same** basename for collision checks).

## Setup checks

```bash
# After backend start — table should allow multiple rows per presentation
psql kuky_dev -c "\d presentation_files"
# Expect: id PK, presentation_id FK, display_name, UNIQUE(presentation_id, display_name)

# Legacy single-file rows survived migration
psql kuky_dev -c "SELECT id, presentation_id, original_name, display_name FROM presentation_files LIMIT 5;"
```

Confirm disk remapper: under `./data/presentation-files` (or `PRESENTATION_FILES_STORAGE_DIR`), blobs are named `{fileId}.pptx`, not only `{presentationId}.pptx`.

## API smoke (optional)

Cookie jar from admin login, then:

```bash
# Add first file
curl -s -b cookies.txt -F "file=@deck.pptx" \
  http://localhost:8081/api/v1/admin/presentations/{PRESENTATION_ID}/files | jq '.files | length'

# Add second (same name for suffix check)
curl -s -b cookies.txt -F "file=@deck.pptx" \
  http://localhost:8081/api/v1/admin/presentations/{PRESENTATION_ID}/files | jq '.files[].displayName'

# Student download (after share/unit access)
curl -s -b student.txt -OJ \
  http://localhost:8081/api/v1/learning/presentations/{PRESENTATION_ID}/files/{FILE_ID}
```

## Browser validation

### Teacher — admin Presentations (US1)

1. Open `/panel` → Presentations (or via Units content picker).
2. On a presentation with no files, upload one allowed file → it appears in a file list.
3. Upload a second file → both remain (upload does **not** replace).
4. Upload another file with the same original name → see auto-suffixed display names.
5. Remove one file → the other stays with its **same** display name.
6. Attempt an 11th file on a presentation that already has 10 → clear error; list unchanged.
7. Reject empty/wrong-type/oversized upload → error; list unchanged.

### Student — learning downloads (US2)

1. Share the multi-file presentation with a student (or assign via unit).
2. As that student, open `/aprendizaje`.
3. Confirm each file has its own download control labeled with `displayName`.
4. Download each file; saved names match the display names (including suffixes).

### Legacy continuity (US3)

1. Open a presentation that had a single file before this feature.
2. Confirm one entry in the files list without re-upload.
3. Student can still download it; teacher can add a second file and both appear.

## Expected outcomes

| Check | Pass when |
|-------|-----------|
| Add | Repeated uploads grow `files` up to 10 |
| Collision | Distinct stable `displayName`s; no renumber after remove |
| Student | Per-file download works; empty presentation shows no-file state |
| Migration | Pre-existing single file still listed and downloadable |
| Cleanup | Deleting a presentation removes its file rows and disk blobs |

## Out of scope for this quickstart

Zip download, drag-and-drop reorder, in-place replace, multi-select file picker, homework/placement uploads.
