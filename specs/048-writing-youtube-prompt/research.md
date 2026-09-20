# Research: Writing Homework YouTube Prompt

**Feature**: `048-writing-youtube-prompt` | **Date**: 2026-09-20

No `[NEEDS CLARIFICATION]` markers remained in the spec after `/speckit-clarify` (both open questions — freeze behaviour and video placement — were resolved there). This document instead records the implementation-approach decisions made by reading the existing codebase, since they materially shape the plan.

## Decision 1: Reuse the existing media columns, don't add a new one

**Decision**: Store the writing-homework video in the same `homework_assignments.media_source_kind` / `audio_url` columns already used by listening homework, with `media_source_kind = YOUTUBE`, rather than adding a dedicated `prompt_video_url` column.

**Rationale**: These columns already exist, are already nullable, and are already read generically (not gated by homework type) by `HomeworkItemResponse`, `ExerciseResponse`, `HomeworkAdminItem`, `AudioPlayer`, `HomeworkPreview.tsx`, and — critically — by `AssignmentSnapshot.serialize()`, which already writes `audioUrl` / `audioFileId` / `mediaSourceKind` into every submission's frozen snapshot regardless of homework type. Reusing the column means the freeze behaviour required by FR-011 needs zero additional backend work: it already happens today for any row that has these fields set. This is also the smallest change consistent with the Simplicity First principle.

**Alternatives considered**:
- *New dedicated column* (e.g. `write_prompt_video_url`): rejected — would require a migration, a second freeze-serialization path in `AssignmentSnapshot`, and duplicate rendering logic, for no behavioural benefit over the existing column.
- *Reuse the full four-way `AudioSourceEditor` picker for WRITE*: rejected — the feature request and the spec (Assumptions) scope this to YouTube only; showing audio-URL/uploaded-file/video-page options for a writing prompt would be misleading and outside what was asked for.

## Decision 2: Gate validation in `HomeworkAdminService.resolveAudio`, not in the DTOs or the frontend

**Decision**: The only backend logic change is inside the existing private `resolveAudio(HomeworkType type, ...)` method: today it does `if (type != HomeworkType.AUDIO) return empty;`. It becomes: AUDIO keeps its existing required four-way validation; WRITE accepts an optional `YOUTUBE` kind (validated with the existing `ListeningMedia.extractYouTubeId`) and rejects any other kind for WRITE; every other type still clears media as before.

**Rationale**: This mirrors exactly how the AUDIO-only gate already works, keeps all validation server-side (Evolution-Ready Architecture principle), and requires no DTO shape changes — `CreateHomeworkRequest`/`UpdateHomeworkRequest` already carry `audioUrl` / `mediaSourceKind` / `audioFileId` as type-agnostic fields.

**Alternatives considered**:
- *Validate only in the frontend*: rejected — the constitution requires business rules to live where they can't be bypassed; other homework-editing paths (future API consumers, tests) would not get the guarantee.

## Decision 3: A new small `WriteVideoEditor` component, not a mode of `AudioSourceEditor`

**Decision**: Add a small new component with a single URL input (no radio group, no upload button, no video-page option) rather than passing a "restrict to YouTube only" flag into the existing `AudioSourceEditor`.

**Rationale**: `AudioSourceEditor` renders a `RadioGroup` over all four `MediaSourceKind` values plus an upload flow; bending it with a prop to hide three of its four modes would make an already fairly dense component harder to read for a feature that doesn't need any of that branching. A dedicated component stays small, single-purpose, and still reuses the shared `AudioPlayer` for its preview and the existing `youTubeId` extraction pattern — consistent with Component-Driven UI (named, reusable, no raw DOM) without inheriting unrelated complexity.

**Alternatives considered**:
- *Prop-driven restricted mode on `AudioSourceEditor`*: rejected for the readability reason above.
- *Inline the input directly in `HomeworkEditorPage.tsx`*: rejected — violates Component-Driven UI (every visual element must be a named component).

## Decision 4: No change needed in `HomeworkInlinePanel.tsx` or `HomeworkPreview.tsx`

**Decision**: Do not modify these two files as part of this feature.

**Rationale**: Both already render `<AudioPlayer mediaSourceKind={...} audioUrl={...} audioFileId={...} />` guarded only by `(audioUrl || audioFileId)`, with no `homeworkType` check, in the exact code path already used for `WRITE`/`ALL_MANUAL` composition (`HomeworkInlinePanel.tsx`, "WRITE / ALL_MANUAL" branch) and for every homework type (`HomeworkPreview.tsx`). Once the backend allows these fields to be non-null for WRITE, both surfaces embed the video automatically, positioned directly below instructions and above the answer form/questions — already matching the placement decided in clarification.

**Alternatives considered**: N/A — confirmed by reading both files; no alternative needed since the existing code already does the right thing.

## Decision 5: `HomeworkWritePage.tsx` needs one new render call

**Decision**: This is the one student-facing surface that does not already render `AudioPlayer`. Add it directly below the instructions paragraph and above `<ManualAnswerForm>`, guarded by `item.audioUrl`.

**Rationale**: `HomeworkWritePage.tsx` is the full-page (non-inline) writing homework view reached from `/aprendizaje`; today it renders title, due date, instructions, and the answer form only. This is the placement decided during clarification (below instructions, above the answer box), matching the pattern already used elsewhere.
