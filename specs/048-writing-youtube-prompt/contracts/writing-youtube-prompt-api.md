# API Contract: Writing Homework YouTube Prompt

**Feature**: `048-writing-youtube-prompt` | **Date**: 2026-09-20

This feature does not add endpoints. It widens the accepted values of existing fields on the existing homework admin endpoints, and the existing student/read endpoints already pass those fields through unchanged.

## `POST /api/homework` and `PUT /api/homework/{id}` (admin, create/update)

Request body: `CreateHomeworkRequest` / `UpdateHomeworkRequest` — **no field added or removed**. Behaviour change only:

| Field | Before this feature | After this feature |
|---|---|---|
| `homeworkType = "WRITE"` + `mediaSourceKind` | Any value silently ignored; `audioUrl`/`audioFileId`/`mediaSourceKind` always stored as `null`. | `mediaSourceKind: "YOUTUBE"` + a valid `audioUrl` is stored. `mediaSourceKind` omitted/blank stores `null` for all three fields (video stays optional). Any other `mediaSourceKind` (`AUDIO_URL`, `UPLOADED_FILE`, `VIDEO_PAGE`) with `homeworkType = "WRITE"` is now a validation error. |

**New error case**: `homeworkType = "WRITE"` with `mediaSourceKind = "YOUTUBE"` and an `audioUrl` that does not resolve a YouTube video id → `400` with a validation message (same family as the existing "Indica un enlace de YouTube válido." used for AUDIO).

**New error case**: `homeworkType = "WRITE"` with `mediaSourceKind` set to `AUDIO_URL`, `UPLOADED_FILE`, or `VIDEO_PAGE` → `400` (these kinds remain AUDIO-only).

No change to the AUDIO, READ, or GRAMMAR validation paths.

## `GET /api/homework`, `GET /api/homework/{id}` (admin) and the student learning endpoints

Response DTOs (`HomeworkAdminItem`, `HomeworkItemResponse`, `ExerciseResponse`) — **no field added or removed**. `audioUrl` / `mediaSourceKind` may now be non-null on a `WRITE` item where they were previously always `null`. Consumers that already render these fields generically (student inline panel, admin preview) need no client-side contract change.

## Submission read paths (result / review)

No contract change. `homework_submissions.assignment_snapshot` already includes `audioUrl` / `audioFileId` / `mediaSourceKind` for every homework type (spec 039); a `WRITE` submission's snapshot now meaningfully carries a video when one was set at submit time, and review/result reads already surface whatever the snapshot contains.
