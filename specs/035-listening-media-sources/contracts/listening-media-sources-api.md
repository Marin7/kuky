# Contract: Listening Media Sources

**Feature**: `035-listening-media-sources` | **Date**: 2026-08-11

Extends existing homework admin and learning payloads. Auth and error envelope unchanged (`{"error","message"}`; validation failures → `VALIDATION_ERROR`). No new URL paths required.

## Enum

`mediaSourceKind`: `AUDIO_URL` | `UPLOADED_FILE` | `VIDEO_PAGE` | `YOUTUBE` | `null`

## Admin authoring

### `POST /api/v1/admin/homework` / `PUT /api/v1/admin/homework/{id}`

Request body gains:

```json
{
  "type": "AUDIO",
  "mediaSourceKind": "YOUTUBE",
  "audioUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "audioFileId": null
}
```

| Rule | HTTP / error |
|------|----------------|
| `type != AUDIO` but any of kind/url/file set | Cleared server-side (or `VALIDATION_ERROR` if preferred — **implement: clear**) |
| `type == AUDIO` and kind missing/invalid | `VALIDATION_ERROR` |
| Kind `UPLOADED_FILE` without existing file id | `VALIDATION_ERROR` |
| Kind `AUDIO_URL` / `VIDEO_PAGE` / `YOUTUBE` without url | `VALIDATION_ERROR` |
| Kind `YOUTUBE` with non-parseable YouTube URL | `VALIDATION_ERROR` |
| Kind url-based with `audioFileId` set (or file kind with url) | `VALIDATION_ERROR` or server clears unused field — **implement: clear unused + validate required** |

### Admin GET detail / list item

Response includes `mediaSourceKind`, `audioUrl`, `audioFileId` (and existing `audioFileName` when applicable). Incomplete AUDIO (`mediaSourceKind == null`) is still returned to admin.

## Student learning

### Exercise / homework item payloads

`ExerciseResponse`, `HomeworkItemResponse` (and any unit-embedded homework DTO that already returns audio fields) include:

```json
{
  "homeworkType": "AUDIO",
  "mediaSourceKind": "VIDEO_PAGE",
  "audioUrl": "https://example.com/watch/lesson-1",
  "audioFileId": null
}
```

### Incomplete AUDIO

| Endpoint behaviour | Contract |
|--------------------|----------|
| Open/take (exercise or manual listening load) | Fail with not-found or conflict — **implement: `AssignmentNotFoundException` → existing not-found mapping** so deep links do not expose an empty player |
| List cards | Include item with `mediaSourceKind: null` (or omit open action client-side when AUDIO && !kind) so UI can show unavailable |

Teacher-facing admin endpoints never use this gate.

## Rendering semantics (client)

Documented for implementers; not separate endpoints:

| `mediaSourceKind` | UI |
|-------------------|-----|
| `UPLOADED_FILE` | `<audio>` from file download URL |
| `AUDIO_URL` | Existing URL player: YT/Vimeo embed if detected, else `<audio>` + optional open link |
| `YOUTUBE` | YouTube iframe embed only |
| `VIDEO_PAGE` | Single outbound control; `target="_blank"` `rel="noopener noreferrer"` |
| `null` (AUDIO) | Do not render take UI |

## Unchanged

- Audio file upload endpoint(s) for homework.
- Placement-test question audio fields.
- Grading / question payloads.
