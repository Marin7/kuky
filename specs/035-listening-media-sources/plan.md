# Implementation Plan: Listening Media Sources

**Branch**: `035-listening-media-sources` | **Date**: 2026-08-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/035-listening-media-sources/spec.md`

## Summary

Extend listening (`AUDIO`) homework media from two teacher options (Enlace URL + uploaded file) to four: **audio URL**, **uploaded file**, **external video-page link** (outbound, new tab, no player), and **YouTube** (in-page embed). Persist an explicit `media_source_kind` on `homework_assignments` so VIDEO_PAGE vs AUDIO_URL and YOUTUBE vs legacy Enlace-with-YouTube-URL stay distinguishable. Backfill existing rows; require media on save; block students from opening incomplete listening homeworks. Frontend: expand `AudioSourceEditor` + student media renderer; auto-switch to YouTube when a YouTube URL is pasted into Enlace or video-page modes.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, NamedParameterJdbcTemplate, Flyway 11. **No new libraries.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, existing `AudioSourceEditor` / `AudioPlayer`. **No new libraries** (YouTube iframe embed already used).

**Storage**: PostgreSQL 18 — Flyway migration adds `homework_assignments.media_source_kind` (`AUDIO_URL` | `UPLOADED_FILE` | `VIDEO_PAGE` | `YOUTUBE`, nullable). Backfill from `audio_file_id` / `audio_url`. Non-AUDIO types keep kind + url/file cleared.

**Testing**: Backend JUnit — `resolveAudio`/media validation (required, mutual exclusion, YouTube URL shape); student get paths reject incomplete AUDIO; list/detail DTOs expose kind. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Negligible — one extra column read/write on homework admin/student payloads.

**Constraints**: Listening homework only (placement-test audio out of scope); one source at a time; media required for AUDIO save; legacy YouTube-under-Enlace stays `AUDIO_URL` in editor but still embeds for students; Vimeo under Enlace keeps embed; YouTube paste into Enlace/VIDEO_PAGE auto-switches to YOUTUBE; video-page opens in new tab; i18n es/en/ro.

**Scale/Scope**: Single-teacher site; admin homework editor + student listening/exercise/mixed take UIs that already render audio.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. One nullable kind column + extend existing URL/file fields; reuse `AudioPlayer` embed paths; no new media host options beyond YouTube + outbound link.
- **II. Component-Driven UI** — PASS. Evolve `AudioSourceEditor` and student media presentation (`AudioPlayer` / thin wrapper); no raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Types in `admin.ts` / `learning.ts`; validation in `HomeworkAdminService.resolveAudio` (or successor); student gate in learning fetch paths.
- **Technology Stack** — PASS. Unchanged stack + Flyway (already in project).
- **Development Workflow** — PASS. Quickstart browser checks; branch `035-listening-media-sources`.

**Post-design re-check**: PASS — contracts add `mediaSourceKind` to existing homework DTOs; one migration; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/035-listening-media-sources/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── listening-media-sources-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V18__listening_media_source_kind.sql          # NEW: column + backfill + check

back-end/src/main/java/com/kuky/backend/
├── learning/model/HomeworkAssignment.java        # EDIT: mediaSourceKind
├── learning/model/MediaSourceKind.java           # NEW: enum
├── learning/repository/ContentRepository.java    # EDIT: insert/update/map kind
├── learning/dto/ExerciseResponse.java            # EDIT: mediaSourceKind
├── learning/dto/HomeworkItemResponse.java        # EDIT: mediaSourceKind
├── learning/service/ExerciseGradingService.java  # EDIT: reject incomplete AUDIO
├── learning/service/… (manual/mixed fetch paths) # EDIT: same incomplete gate if applicable
├── admin/dto/CreateHomeworkRequest.java          # EDIT: mediaSourceKind
├── admin/dto/UpdateHomeworkRequest.java          # EDIT: mediaSourceKind
├── admin/dto/HomeworkAdminItem.java              # EDIT: mediaSourceKind
├── admin/service/HomeworkAdminService.java       # EDIT: resolve + require media
├── units/…                                       # EDIT: pass-through kind if unit homework DTOs expose audio
└── (tests under src/test/java/…)

front-end/src/
├── lib/admin.ts                                  # EDIT: MediaSourceKind + request/response fields
├── lib/learning.ts                               # EDIT: mediaSourceKind on exercise/item types
├── components/admin/homework/
│   ├── AudioSourceEditor.tsx                     # EDIT: four modes + YouTube auto-switch + preview
│   └── HomeworkEditorPage.tsx                    # EDIT: wire kind; block save without media
├── components/learning/
│   ├── AudioPlayer.tsx                           # EDIT: respect kind (VIDEO_PAGE = link only)
│   ├── HomeworkListeningPage.tsx                 # EDIT: kind-aware render / incomplete UX
│   ├── HomeworkInlinePanel.tsx                   # EDIT: kind-aware render
│   └── (MixedHomeworkForm / related)             # EDIT: if they surface audio
└── i18n/locales/{en,es,ro}.ts                    # EDIT: four option labels + incomplete messages
```

**Structure Decision**: Existing full-stack layout. Kind column is required to distinguish VIDEO_PAGE from AUDIO_URL and to honour legacy Enlace-with-YouTube editor behaviour. Placement test unchanged.

## Complexity Tracking

> No constitution violations requiring justification.
