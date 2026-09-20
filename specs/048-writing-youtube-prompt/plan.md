# Implementation Plan: Writing Homework YouTube Prompt

**Branch**: `048-writing-youtube-prompt` | **Date**: 2026-09-20 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/048-writing-youtube-prompt/spec.md`

## Summary

Let teachers attach a single, optional YouTube video to a `WRITE` (writing) homework as a prompt, embedded in-page for students the same way YouTube already embeds for `AUDIO` (listening) homework. Reuses the existing `homework_assignments.media_source_kind` / `audio_url` columns and the existing `AudioPlayer` embed component — no new database column and no new listening-style four-way picker. `HomeworkAdminService.resolveAudio` currently clears media for every non-`AUDIO` type; it is extended to also accept `WRITE` + `YOUTUBE` (optional, validated as a YouTube URL) while still clearing media for `READ`/`GRAMMAR`. Freeze-on-submit already serializes `audioUrl`/`audioFileId`/`mediaSourceKind` into `homework_submissions.assignment_snapshot` regardless of homework type, so the video is automatically covered by the existing submission-freeze behaviour (spec 039) with no snapshot changes. Two of the three student-facing renderers (`HomeworkInlinePanel.tsx`'s WRITE branch, `HomeworkPreview.tsx`) already render `AudioPlayer` off the same generic `audioUrl`/`mediaSourceKind` fields with no type check, so they pick this up for free; only the standalone `HomeworkWritePage.tsx` needs a new render call. Authoring gets a new small, YouTube-only field editor (not the full `AudioSourceEditor` four-mode picker).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, NamedParameterJdbcTemplate. **No new libraries.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, existing `AudioPlayer` (YouTube iframe embed already used for listening homework). **No new libraries.**

**Storage**: PostgreSQL 18 — **no migration**. Reuses `homework_assignments.media_source_kind` / `audio_url` (added by `V18__listening_media_source_kind.sql` for spec 035), now also settable when `homework_type = 'WRITE'` with `media_source_kind = 'YOUTUBE'`. `homework_submissions.assignment_snapshot` (spec 039) already serializes these three columns unconditionally, so no snapshot schema change either.

**Testing**: Backend JUnit — extend `HomeworkAdminServiceTest` (`resolveAudio` / create+update) for WRITE+YOUTUBE valid/invalid/optional/other-kind-rejected cases; extend `HomeworkFreezeSubmittedIntegrationTest` with a WRITE+video case proving a submitted student's snapshot keeps the original video after the teacher changes it. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Negligible — no new column, no new query; same payload shape, occasionally non-null for WRITE rows.

**Constraints**: Writing homework only; YouTube URL only (no audio-URL, uploaded-file, or video-page options for WRITE — that four-way picker stays AUDIO-only); at most one video per writing homework; video is optional (no "media required" gate, unlike AUDIO); freeze-on-submit via the existing `assignment_snapshot` mechanism, no version history UI; embed placed directly below instructions and above the answer box; i18n es/en/ro.

**Scale/Scope**: Single-teacher site. Touches the homework admin editor, the standalone writing-homework take page, and (already-generic, no-change) the inline homework panel and admin preview.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Zero new DB columns/migrations; reuses the existing `media_source_kind`/`audio_url` columns, the existing freeze snapshot, and the existing `AudioPlayer` embed. The only new code is one narrow validation branch on the backend and one small single-field editor + one render call on the frontend.
- **II. Component-Driven UI** — PASS. New `WriteVideoEditor` component (named, reusable) instead of inlining a raw `<input>`; reuses `AudioPlayer` for both the teacher preview and the student embed rather than a second embed implementation.
- **III. Evolution-Ready Architecture** — PASS. All validation stays server-side in `HomeworkAdminService.resolveAudio`; frontend components only render what the API returns, matching the existing pattern for listening media.
- **Technology Stack** — PASS. Unchanged stack, no new dependencies.
- **Development Workflow** — PASS. Quickstart browser checks required before done; branch `048-writing-youtube-prompt` already created.

**Post-design re-check**: PASS — contracts only widen an existing request/response field's valid values for one existing homework type; no new entities; Complexity Tracking is empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/048-writing-youtube-prompt/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md         # Phase 1 output
├── contracts/
│   └── writing-youtube-prompt-api.md
├── checklists/
│   └── requirements.md
└── tasks.md              # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/
├── admin/service/HomeworkAdminService.java        # EDIT: resolveAudio() accepts WRITE + YOUTUBE (optional); rejects WRITE + any other kind
└── (tests) admin/HomeworkAdminServiceTest.java     # EDIT: WRITE+YOUTUBE valid/invalid/optional/other-kind cases
    (tests) learning/HomeworkFreezeSubmittedIntegrationTest.java  # EDIT: submitted WRITE homework keeps its original video after teacher edits it

front-end/src/
├── components/admin/homework/
│   ├── WriteVideoEditor.tsx             # NEW: single YouTube-URL input + inline embed preview (reuses AudioPlayer + the existing youTubeId regex)
│   └── HomeworkEditorPage.tsx           # EDIT: render WriteVideoEditor when homeworkType === "WRITE"; stop force-clearing media on save for WRITE (still clears for READ/GRAMMAR)
├── components/learning/
│   └── HomeworkWritePage.tsx            # EDIT: render <AudioPlayer> directly below instructions, above <ManualAnswerForm>, when item.audioUrl is set
│   # HomeworkInlinePanel.tsx (WRITE branch) and admin HomeworkPreview.tsx already render AudioPlayer off the same generic fields — no change expected there
└── i18n/locales/{en,es,ro}.ts           # EDIT: writeVideo label/hint/placeholder/invalid-url copy
```

**Structure Decision**: Existing full-stack layout (`front-end/` + `back-end/`). No new top-level modules; this is a scoped extension of the existing listening-media-sources (035) and freeze-submitted-homework (039) work onto one more homework type.

## Complexity Tracking

> No constitution violations requiring justification.
