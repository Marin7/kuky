# Data Model: Listening Media Sources

**Feature**: `035-listening-media-sources` | **Date**: 2026-08-11

## Entity: HomeworkAssignment (listening media)

Existing table `homework_assignments` gains one column. Payload fields unchanged.

| Field | Type | Notes |
|-------|------|--------|
| `audio_url` | `TEXT` nullable | Used for `AUDIO_URL`, `VIDEO_PAGE`, `YOUTUBE` |
| `audio_file_id` | `UUID` nullable FK → `audio_files` | Used for `UPLOADED_FILE` |
| `media_source_kind` | `VARCHAR(20)` nullable | New — see enum below |

### `media_source_kind` values

| Value | Meaning | Payload |
|-------|---------|---------|
| `AUDIO_URL` | Enlace — play/embed per URL heuristics | `audio_url` set; `audio_file_id` null |
| `UPLOADED_FILE` | Uploaded audio file | `audio_file_id` set; `audio_url` null |
| `VIDEO_PAGE` | Outbound link to a video page | `audio_url` set; `audio_file_id` null |
| `YOUTUBE` | Explicit YouTube embed | `audio_url` YouTube video URL; `audio_file_id` null |
| `NULL` | No media / non-AUDIO / incomplete | both payload fields null for incomplete AUDIO |

### Migration backfill

1. Add column nullable + `CHECK (media_source_kind IS NULL OR media_source_kind IN (...))`.
2. `UPLOADED_FILE` where `audio_file_id IS NOT NULL` (and null out `audio_url` if both were set — prefer file).
3. `AUDIO_URL` where `audio_url IS NOT NULL` and file null.
4. Leave others `NULL`.

### Validation rules (AUDIO type only)

- Kind required and must be one of the four values.
- Payload must match kind (see research §3).
- Non-AUDIO: kind and both payload fields null.

### Student access rule

- AUDIO with `media_source_kind IS NULL` **or** missing required payload → incomplete: students cannot open/take; admin can still load for edit.

### Relationships

- Unchanged: optional FK `audio_file_id` → `audio_files`.
- Kind is assignment-scoped (not per-question).

## API-facing DTO fields

Admin + student homework DTOs that already expose `audioUrl` / `audioFileId` also expose:

- `mediaSourceKind: "AUDIO_URL" | "UPLOADED_FILE" | "VIDEO_PAGE" | "YOUTUBE" | null`

Create/Update requests for homework include the same field (required when `type` is `AUDIO`).

## State / lifecycle

No new submission states. Incomplete is a content completeness condition on the assignment, not a submission status.
