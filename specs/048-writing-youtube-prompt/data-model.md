# Data Model: Writing Homework YouTube Prompt

**Feature**: `048-writing-youtube-prompt` | **Date**: 2026-09-20

No new tables or columns. This feature widens the valid use of three existing `homework_assignments` columns to one more `homework_type` value, and relies on the existing freeze snapshot to already cover them.

## Entities

### Homework assignment (live) — `homework_assignments`

Existing columns, unchanged shape, now also meaningful when `homework_type = 'WRITE'`:

| Field | Type | Notes (this feature) |
|-------|------|--------|
| `homework_type` | text | No change. `WRITE` may now have non-null media fields below (previously always null for WRITE). |
| `media_source_kind` | text, nullable | For `WRITE`, only `YOUTUBE` or `NULL` is valid (unlike `AUDIO`, which allows all four kinds). Enforced in `HomeworkAdminService.resolveAudio`, not at the DB level (same approach already used for AUDIO). |
| `audio_url` | text, nullable | For `WRITE` + `media_source_kind = YOUTUBE`, holds the YouTube URL. Must resolve a video id via the existing `ListeningMedia.extractYouTubeId` pattern. |
| `audio_file_id` | uuid, nullable | Always `NULL` for `WRITE` — the uploaded-file option is AUDIO-only and out of scope for writing prompts. |

No new completeness/required gate for WRITE: unlike `ListeningMedia.isComplete()` (AUDIO-only, required), a `WRITE` row with `media_source_kind = NULL` and `audio_url = NULL` is valid and unaffected — the field is optional per FR-002.

### Homework submission — `homework_submissions.assignment_snapshot` (JSONB)

No schema change. `AssignmentSnapshot.serialize()` already writes `audioUrl`, `audioFileId`, and `mediaSourceKind` into every submission's snapshot unconditionally (see spec 039's data-model). Once a `WRITE` assignment has these fields set, they are captured the same way for a `WRITE` submission as they already are for an `AUDIO` one — this is what makes FR-011 (freeze) free of extra backend work.

Logical shape (unchanged, `WRITE` example):

```text
{
  title, instructions, homeworkType: "WRITE", level, format, composition: "WRITE",
  audioUrl: "https://www.youtube.com/watch?v=…", audioFileId: null, mediaSourceKind: "YOUTUBE",
  questions: []
}
```

## Validation (new, inside `resolveAudio`)

| `homeworkType` | `mediaSourceKind` | Result |
|---|---|---|
| `WRITE` | `null` / blank | Valid — no video (`audioUrl=null, audioFileId=null, mediaSourceKind=null`). |
| `WRITE` | `YOUTUBE` | Valid only if `audioUrl` resolves a YouTube video id; otherwise rejected with a validation error (same message family as AUDIO's YouTube validation). |
| `WRITE` | `AUDIO_URL` / `UPLOADED_FILE` / `VIDEO_PAGE` | Rejected — these kinds remain AUDIO-only. |
| `AUDIO` | *(any)* | Unchanged existing behaviour (required, four kinds). |
| `READ` / `GRAMMAR` | *(any)* | Unchanged — always cleared to `null`. |

## Relationships

```text
homework_assignments 1 ─── * homework_submissions
homework_submissions 1 ─── 0..1 assignment_snapshot (JSONB, already includes media fields for every type)
```

No new relationships. No migration required.
