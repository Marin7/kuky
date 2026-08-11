# Tasks: Listening Media Sources

**Input**: Design documents from `/specs/035-listening-media-sources/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Not requested in the feature spec - no TDD task phase. Backend JUnit coverage appears in Polish where noted in the plan.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Backend: `back-end/src/main/java/com/kuky/backend/...`
- Frontend: `front-end/src/...`
- Migrations: `back-end/src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Align types and labels before schema/API work

- [x] T001 Confirm feature docs are current in `specs/035-listening-media-sources/` (plan, research, data-model, contracts, quickstart) before coding
- [x] T002 [P] Add `MediaSourceKind` TypeScript union and extend homework types with `mediaSourceKind` in `front-end/src/lib/admin.ts`
- [x] T003 [P] Add `mediaSourceKind` to student homework/exercise types in `front-end/src/lib/learning.ts`
- [x] T004 [P] Add es/en/ro authoring labels for four media modes plus incomplete-student copy in `front-end/src/i18n/locales/es.ts`, `front-end/src/i18n/locales/en.ts`, and `front-end/src/i18n/locales/ro.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Persist `media_source_kind`, plumb DTOs/repos, and enforce AUDIO media validation - required before any user-story UI can work end-to-end

**WARNING: CRITICAL**: No user story work that depends on save/load of kind can begin until this phase is complete

- [x] T005 Create Flyway migration `back-end/src/main/resources/db/migration/V18__listening_media_source_kind.sql` (column, CHECK, backfill `UPLOADED_FILE` / `AUDIO_URL`, clear conflicting url when file present)
- [x] T006 [P] Add enum `back-end/src/main/java/com/kuky/backend/learning/model/MediaSourceKind.java`
- [x] T007 [P] Add `mediaSourceKind` field + accessors on `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkAssignment.java`
- [x] T008 Update insert/update/row-mapping for `media_source_kind` in `back-end/src/main/java/com/kuky/backend/learning/repository/ContentRepository.java`
- [x] T009 [P] Add `mediaSourceKind` to `back-end/src/main/java/com/kuky/backend/admin/dto/CreateHomeworkRequest.java` and `back-end/src/main/java/com/kuky/backend/admin/dto/UpdateHomeworkRequest.java`
- [x] T010 [P] Add `mediaSourceKind` to `back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkAdminItem.java`
- [x] T011 [P] Add `mediaSourceKind` to `back-end/src/main/java/com/kuky/backend/learning/dto/ExerciseResponse.java` and `back-end/src/main/java/com/kuky/backend/learning/dto/HomeworkItemResponse.java`
- [x] T012 Map `mediaSourceKind` in admin `toItem` and create/update calls in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`
- [x] T013 Rewrite `resolveAudio` (or successor) in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` to require kind+payload for AUDIO, clear unused field, validate YouTube URL for `YOUTUBE`, clear all media for non-AUDIO
- [x] T014 Pass `mediaSourceKind` through student list mapping in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkItems.java`
- [x] T015 Pass `mediaSourceKind` through unit homework DTOs in `back-end/src/main/java/com/kuky/backend/units/repository/UnitRepository.java` and `back-end/src/main/java/com/kuky/backend/units/service/UnitService.java`
- [x] T016 Wire create/update payloads to send `mediaSourceKind` from `front-end/src/components/admin/homework/HomeworkEditorPage.tsx` (state + load from admin detail)

**Checkpoint**: Foundation ready - AUDIO save/load includes kind; migration backfills legacy rows

---

## Phase 3: User Story 1 - Teacher picks an external video page link (Priority: P1) - MVP

**Goal**: Teacher can choose VIDEO_PAGE, save a URL, and students see only an outbound new-tab link (no in-page player)

**Independent Test**: Create listening homework with only an external video-page URL; as student, confirm new-tab link and no audio/iframe player (quickstart Scenario A)

### Implementation for User Story 1

- [x] T017 [US1] Extend `front-end/src/components/admin/homework/AudioSourceEditor.tsx` with a `VIDEO_PAGE` radio mode (url input, clears other sources on switch)
- [x] T018 [US1] Teach `front-end/src/components/learning/AudioPlayer.tsx` (or thin wrapper) to render `VIDEO_PAGE` as outbound link only with `target="_blank"` and `rel="noopener noreferrer"`
- [x] T019 [US1] Pass `mediaSourceKind` into student media renderers in `front-end/src/components/learning/HomeworkListeningPage.tsx` and `front-end/src/components/learning/HomeworkInlinePanel.tsx`
- [x] T020 [US1] Ensure VIDEO_PAGE preview in admin editor uses the same outbound-link rendering in `front-end/src/components/admin/homework/AudioSourceEditor.tsx`

**Checkpoint**: VIDEO_PAGE authoring + student outbound link works independently

---

## Phase 4: User Story 2 - Teacher embeds a YouTube video (Priority: P1)

**Goal**: Dedicated YouTube option with embed preview, student iframe, validation, and auto-switch from Enlace / VIDEO_PAGE paste

**Independent Test**: Author via YouTube option; student sees embed; paste YouTube into Enlace or video-page auto-switches; bad YouTube URL blocked (quickstart Scenario B)

### Implementation for User Story 2

- [x] T021 [US2] Add `YOUTUBE` mode + embed preview to `front-end/src/components/admin/homework/AudioSourceEditor.tsx`
- [x] T022 [US2] Auto-switch editor to `YOUTUBE` when a recognised YouTube URL is pasted while mode is `AUDIO_URL` or `VIDEO_PAGE` in `front-end/src/components/admin/homework/AudioSourceEditor.tsx` (do not auto-switch on load of existing `AUDIO_URL`)
- [x] T023 [US2] Render `YOUTUBE` kind as iframe-only in `front-end/src/components/learning/AudioPlayer.tsx` (reuse existing YouTube id helper)
- [x] T024 [US2] Enforce YouTube URL parse validation on save for kind `YOUTUBE` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` (clear Spanish/`VALIDATION_ERROR` message)

**Checkpoint**: YouTube option + auto-switch + validation work independently

---

## Phase 5: User Story 3 - Existing audio URL and uploaded file options stay available (Priority: P2)

**Goal**: Four clear options in the picker; Enlace (direct audio) and Archivo subido keep today's student playback

**Independent Test**: Author one homework with direct audio URL and one with upload; both play for students; picker shows four choices (quickstart Scenario D + US3 acceptance)

### Implementation for User Story 3

- [x] T025 [US3] Keep `AUDIO_URL` and `UPLOADED_FILE` modes in `front-end/src/components/admin/homework/AudioSourceEditor.tsx` as two of four radios; update placeholders/hints so Enlace is audio-oriented (not "paste any video page")
- [x] T026 [US3] Confirm `AUDIO_URL` / `UPLOADED_FILE` student paths still use native `<audio>` (and Enlace YouTube/Vimeo heuristic embed) in `front-end/src/components/learning/AudioPlayer.tsx`
- [x] T027 [US3] Block admin save when AUDIO has no complete media source in `front-end/src/components/admin/homework/HomeworkEditorPage.tsx` (client-side guard aligned with server FR-001a)

**Checkpoint**: All four options visible; legacy audio modes unchanged for students

---

## Phase 6: User Story 4 - Existing listening homeworks keep working (Priority: P2)

**Goal**: Backfilled rows work; legacy YouTube-under-Enlace stays Enlace in editor but embeds for students; incomplete AUDIO blocked for students

**Independent Test**: Reopen legacy YouTube `AUDIO_URL` -> Enlace selected + student embed; incomplete AUDIO not openable by student until fixed (quickstart Scenarios C + E)

### Implementation for User Story 4

- [x] T028 [US4] Select editor mode from stored `mediaSourceKind` only (never infer YouTube->YOUTUBE on load) in `front-end/src/components/admin/homework/HomeworkEditorPage.tsx` / `AudioSourceEditor.tsx`
- [x] T029 [US4] Gate incomplete AUDIO (`mediaSourceKind == null` or missing payload) in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java` `getExercise` (AssignmentNotFound or equivalent)
- [x] T030 [US4] Apply the same incomplete gate on manual/mixed listening open paths that serve take UI (trace callers of `HomeworkItems` / listening routes; update the relevant service(s) under `back-end/src/main/java/com/kuky/backend/learning/service/`)
- [x] T031 [US4] Disable or block open for incomplete AUDIO list/cards in `front-end/src/components/learning/HomeworkItemCard.tsx` (and listening entry points) with i18n incomplete message
- [x] T032 [US4] Verify backfill behaviour against local DB after `V18` (spot-check file -> `UPLOADED_FILE`, url -> `AUDIO_URL`, neither -> null) using `specs/035-listening-media-sources/quickstart.md` Scenario C/E

**Checkpoint**: Legacy compatible; incomplete AUDIO blocked for students; teacher can still edit

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Coverage, mixed forms, and quickstart sign-off

- [x] T033 [P] Propagate `mediaSourceKind` through any remaining student surfaces that render listening media (e.g. `front-end/src/components/learning/MixedHomeworkForm.tsx` if it shows audio)
- [x] T034 [P] Extend admin unit homework mapping tests/fixtures if needed under `back-end/src/test/java/` when constructors gained `mediaSourceKind`
- [x] T035 Add/extend unit tests for media resolve + incomplete gate in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` and learning service tests
- [x] T036 Run browser validation for Scenarios A-F in `specs/035-listening-media-sources/quickstart.md` and fix gaps
- [x] T037 [P] Confirm placement-test audio UI untouched (no `mediaSourceKind` requirement) under `front-end/src/components/placement/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup - **BLOCKS** all user stories that save/load kind
- **US1 (Phase 3)**: After Foundational - MVP
- **US2 (Phase 4)**: After Foundational; builds on shared `AudioSourceEditor` / `AudioPlayer` from US1
- **US3 (Phase 5)**: After Foundational; polish of same picker (best after US1/US2 radios exist)
- **US4 (Phase 6)**: After Foundational; student gate independent of US1 UI but needs kind on DTOs; editor load rules interact with US2 auto-switch
- **Polish (Phase 7)**: After desired stories complete

### User Story Dependencies

- **US1 (P1)**: After Phase 2 - no dependency on US2-US4
- **US2 (P1)**: After Phase 2 - shares editor/player files with US1 (sequential on those files recommended)
- **US3 (P2)**: After Phase 2 - ideally after US1/US2 so all four radios exist
- **US4 (P2)**: After Phase 2 - incomplete gate can proceed in parallel with US1 on backend files; editor load rule should land after US2 auto-switch logic

### Parallel Opportunities

- T002-T004 in parallel (Setup)
- T006-T007, T009-T011 in parallel once T005 started/done (Foundational)
- T033, T034, T037 in parallel (Polish)
- Backend incomplete gate (T029-T030) can parallel frontend VIDEO_PAGE work (T017-T020) after Phase 2

---

## Parallel Example: After Foundational

```text
# Developer A - US1 student/admin video-page UI:
Task: T017 AudioSourceEditor VIDEO_PAGE mode
Task: T018-T019 AudioPlayer + listening pages

# Developer B - US4 incomplete gate (backend):
Task: T029 ExerciseGradingService gate
Task: T030 Other learning open-path gates
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 Setup
2. Complete Phase 2 Foundational
3. Complete Phase 3 US1 (VIDEO_PAGE)
4. **STOP and VALIDATE** quickstart Scenario A
5. Demo outbound video links

### Incremental Delivery

1. Setup + Foundational -> kind persisted and required on AUDIO save
2. US1 -> VIDEO_PAGE MVP
3. US2 -> YouTube + auto-switch
4. US3 -> four-option polish + client save guard
5. US4 -> legacy + incomplete block
6. Polish -> quickstart A-F

### Parallel Team Strategy

1. Team finishes Phase 1-2 together
2. Then split: UI story work (US1/US2) vs backend gates (US4) on different files where possible
3. US3/US4 editor behaviour merged carefully on `AudioSourceEditor.tsx`

---

## Notes

- [P] = different files, no dependencies on incomplete tasks
- Do not auto-promote legacy YouTube URLs to `YOUTUBE` on load (FR-011)
- Placement-test audio remains out of scope
- Commit after each task or logical group
- Suggested MVP = US1 after Foundational
