# Research: Listening Media Sources

**Feature**: `035-listening-media-sources` | **Date**: 2026-08-11

## 1. Persist explicit `media_source_kind` vs infer from URL

**Decision**: Add `homework_assignments.media_source_kind` (`AUDIO_URL` | `UPLOADED_FILE` | `VIDEO_PAGE` | `YOUTUBE`). Keep storing the payload in existing `audio_url` / `audio_file_id`. Infer only for **backfill** of legacy rows; after that, trust the stored kind for editor selection and student rendering (with one compatibility rule: `AUDIO_URL` whose value is YouTube/Vimeo still embeds — today’s `AudioPlayer` behaviour).

**Rationale**: VIDEO_PAGE and AUDIO_URL both use a URL string — without a kind, students cannot tell “outbound link” from “try `<audio>` / auto-embed”. FR-011 requires legacy YouTube URLs to stay on Enlace in the editor while still embedding — that is impossible if the editor always auto-detects YouTube into the YouTube radio.

**Alternatives considered**:
- Infer kind only on the client — breaks API consumers and student SSR; fragile.
- Separate columns per kind — YAGNI; url+file already cover payloads.
- Store kind only in frontend localStorage — not durable.

## 2. Backfill rules

**Decision** (Flyway `UPDATE` then optional app-side defense):

| Existing row | Set kind |
|--------------|----------|
| `audio_file_id IS NOT NULL` | `UPLOADED_FILE` (clear conflicting url if any — prefer file as today “one source”) |
| `audio_url` present, no file | `AUDIO_URL` (even if URL is YouTube/Vimeo) |
| neither | `NULL` (incomplete) |

Non-`AUDIO` homework types: force `media_source_kind`, `audio_url`, `audio_file_id` all null on write (unchanged clearing behaviour).

**Rationale**: Matches clarifications: legacy YouTube stays Enlace (`AUDIO_URL`); incomplete rows exist and must be blocked for students (FR-001b).

**Alternatives considered**:
- Auto-promote YouTube URLs to `YOUTUBE` on backfill — rejected by clarification (editor stays Enlace).
- Delete incomplete AUDIO rows — destructive; out of scope.

## 3. Validation on admin create/update

**Decision**: For `type == AUDIO`, require a complete source:

| Kind | Required payload | Reject if |
|------|------------------|-----------|
| `AUDIO_URL` | non-blank `audioUrl` | file id set; blank url |
| `UPLOADED_FILE` | existing `audioFileId` | url set; missing file |
| `VIDEO_PAGE` | non-blank `audioUrl` (http/https) | file id; blank url |
| `YOUTUBE` | non-blank `audioUrl` that parses to a YouTube video id | non-YouTube; file id |

Mutual exclusion: exactly one of url-bearing kinds vs file. Server clears the unused field. Missing/blank kind or incomplete payload → `VALIDATION_ERROR`. Non-AUDIO → clear all three fields.

**Rationale**: FR-001 / FR-001a; keeps DB free of contradictory pairs.

**Alternatives considered**: Allow both url and file with precedence — conflicts with “one source” (FR-006).

## 4. YouTube auto-switch (authoring UX)

**Decision**: Frontend-only while editing: if mode is `AUDIO_URL` or `VIDEO_PAGE` and the pasted value matches existing YouTube id regex (`AudioPlayer.youTubeId`), switch mode to `YOUTUBE` and keep the URL. Do **not** auto-switch on load of an existing `AUDIO_URL` row (even if URL is YouTube) — FR-011.

Server still accepts `YOUTUBE` only when kind is `YOUTUBE`; if client mistakenly sends YouTube URL with `AUDIO_URL`, server may accept as `AUDIO_URL` (legacy path) — preferred: server does **not** rewrite kind on save so explicit client kind wins; auto-switch is client responsibility for new pastes.

**Rationale**: Clarifications Q3 + Q7; avoids surprising reopen behaviour.

**Alternatives considered**:
- Server upgrades AUDIO_URL→YOUTUBE when URL matches — would break FR-011 on next save of legacy rows.
- Reject YouTube under AUDIO_URL — breaks legacy save-without-touch.

## 5. Student rendering by kind

**Decision**:

| Kind | Student UI |
|------|------------|
| `UPLOADED_FILE` | Native `<audio>` via file URL (today) |
| `AUDIO_URL` | Today’s `AudioPlayer` URL path: YouTube/Vimeo embed if detected, else `<audio>` + external fallback link |
| `YOUTUBE` | YouTube iframe only (validate id; if missing, treat as incomplete / error) |
| `VIDEO_PAGE` | Outbound link only (`target=_blank`, `rel=noopener noreferrer`); no `<audio>`, no iframe |
| `NULL` / incomplete AUDIO | Do not serve take UI — API error (e.g. `AssignmentNotFoundException` or dedicated `VALIDATION_ERROR` / `CONFLICT`); list card not openable |

**Rationale**: Spec FR-002–005, FR-001b, SC-007. Reuses embed logic for Enlace compatibility (YouTube + Vimeo).

**Alternatives considered**:
- Hide incomplete from list only but allow deep link — weaker than “block open/take”.
- Soft empty player — rejected by clarification A.

## 6. Where to gate incomplete AUDIO

**Decision**: Gate in learning services that return take payloads (`ExerciseGradingService.getExercise` and any manual/mixed listening loaders that return `audioUrl`/`audioFileId` for AUDIO). Also mark list items so the UI disables open (optional `mediaIncomplete` or infer `homeworkType===AUDIO && !mediaSourceKind`). Admin GET still returns the row so the teacher can fix it.

**Rationale**: Students must not open; teachers must edit.

**Alternatives considered**: DB CHECK requiring kind for all AUDIO — would block migration of incomplete rows until cleaned; prefer app validation + student gate.

## 7. Spanish (and en/ro) labels

**Decision** (authoring radios):

| Kind | es (primary) | en |
|------|--------------|-----|
| `AUDIO_URL` | Enlace (URL) | Link (URL) |
| `UPLOADED_FILE` | Archivo subido | Uploaded file |
| `VIDEO_PAGE` | Enlace a vídeo | Video page link |
| `YOUTUBE` | YouTube | YouTube |

Hints: Enlace = direct audio (YouTube/Vimeo still work if pasted historically); vídeo = opens externally; YouTube = embedded player. Incomplete student message: short “Esta tarea de audio aún no está lista” / EN equivalent.

**Rationale**: FR-009; distinguishable without marketing fluff.

**Alternatives considered**: “Vídeo (sin reproductor)” — clearer but wordy; keep short + hint text.

## 8. Scope exclusions

**Decision**: Placement-test listening audio unchanged. No dedicated Vimeo option. No change to grading/questions. Activities do not gain listening media (homework AUDIO only).

**Rationale**: Spec assumptions; YAGNI.
