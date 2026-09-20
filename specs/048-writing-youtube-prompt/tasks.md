# Tasks: Writing Homework YouTube Prompt

**Input**: Design documents from `/specs/048-writing-youtube-prompt/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Not requested in the feature spec — no dedicated TDD task phase. Backend JUnit coverage for the new validation branch and the freeze case appears in Polish / US5, matching how spec 035 handled this.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Backend: `back-end/src/main/java/com/kuky/backend/...`
- Frontend: `front-end/src/...`
- No new migrations for this feature (see data-model.md)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm docs and add shared copy before touching code

- [X] T001 Confirm feature docs are current in `specs/048-writing-youtube-prompt/` (plan, research, data-model, contracts, quickstart) before coding
- [X] T002 [P] Add es/en/ro copy for the new writing-video field (label, hint, placeholder, invalid-URL message) in `front-end/src/i18n/locales/es.ts`, `front-end/src/i18n/locales/en.ts`, and `front-end/src/i18n/locales/ro.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Let the API accept and validate a video on a `WRITE` homework — required before any user-story UI can persist or render one

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Extend `resolveAudio(HomeworkType type, ...)` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`: for `HomeworkType.WRITE`, accept an optional `mediaSourceKind = YOUTUBE` validated via the existing `ListeningMedia.extractYouTubeId` (same error message family as AUDIO's YouTube validation), treat a blank/null kind as "no video" (no required-media error, unlike AUDIO), and reject `AUDIO_URL` / `UPLOADED_FILE` / `VIDEO_PAGE` for `WRITE`. Leave `AUDIO`, `READ`, and `GRAMMAR` behaviour unchanged.

**Checkpoint**: Foundation ready — a `WRITE` homework can now be saved with an optional, validated YouTube video via the API (no frontend surface yet)

---

## Phase 3: User Story 1 - Teacher attaches a YouTube prompt video to a writing homework (Priority: P1)

**Goal**: Teacher can add, change, or remove a YouTube video URL on a writing homework, with save-time validation

**Independent Test**: Create a writing homework, add only a YouTube URL, save, reopen and confirm it persists; paste an invalid URL and confirm save is blocked (quickstart Scenarios 1 steps 1–2, and 2)

### Implementation for User Story 1

- [X] T004 [P] [US1] Create `front-end/src/components/admin/homework/WriteVideoEditor.tsx`: a single YouTube-URL input (no radio group, no upload, no video-page option) using the i18n copy from T002, reusing the existing `youTubeId` extraction pattern for client-side format feedback
- [X] T005 [US1] In `front-end/src/components/admin/homework/HomeworkEditorPage.tsx`, render `<WriteVideoEditor>` (wired to the existing `audio` state) when `homeworkType === "WRITE"`, replacing the current plain `writeHint`-only text
- [X] T006 [US1] In `save()` in `front-end/src/components/admin/homework/HomeworkEditorPage.tsx`, build the audio payload for `type === "WRITE"` from state (`mediaSourceKind`/`audioUrl`, `audioFileId` always `null`, no required-media guard), keep the existing AUDIO required-media guard unchanged, and keep READ/GRAMMAR clearing media to `null`
- [X] T007 [US1] Surface the backend's invalid-YouTube-URL validation error in the editor's existing error banner in `front-end/src/components/admin/homework/HomeworkEditorPage.tsx` (reuse the pattern already used for the AUDIO required-media error)

**Checkpoint**: Teacher can add, edit, remove, and get validation feedback on a writing homework's video end-to-end (persistence only — embed rendering is US2)

---

## Phase 4: User Story 2 - Student sees the prompt video embedded in the writing homework (Priority: P1)

**Goal**: A writing homework's video renders as an in-page embedded player for students, positioned below the instructions and above the answer box, and fits mobile widths

**Independent Test**: Open, as a student, a writing homework with a saved YouTube URL and confirm the in-page embed appears in the right place; open one without a video and confirm no change from today (quickstart Scenario 1 steps 3–6)

### Implementation for User Story 2

- [X] T008 [US2] In `front-end/src/components/learning/HomeworkWritePage.tsx`, render `<AudioPlayer mediaSourceKind={item.mediaSourceKind} audioUrl={item.audioUrl} audioFileId={item.audioFileId} />` directly below the instructions paragraph and above `<ManualAnswerForm>`, guarded by `item.audioUrl` being set
- [X] T009 [P] [US2] Spot-check that `front-end/src/components/learning/HomeworkInlinePanel.tsx` (the "WRITE / ALL_MANUAL" branch) and `front-end/src/components/admin/homework/HomeworkPreview.tsx` already embed a `WRITE` homework's video correctly with no code change (both already render `AudioPlayer` off the same generic fields); fix either file only if the spot-check finds a gap

**Checkpoint**: Students see the embedded, responsive video on every surface that renders a writing homework (standalone page, inline panel, admin preview) — US1 + US2 together are the smallest deployable increment for this feature

---

## Phase 5: User Story 3 - Teacher previews the embed while authoring (Priority: P2)

**Goal**: Teacher sees a live embedded preview in the editor as soon as a valid YouTube URL is entered, without saving first

**Independent Test**: Paste a valid YouTube URL while editing and confirm a working preview appears; clear it and confirm the preview disappears (quickstart Scenario 3)

### Implementation for User Story 3

- [X] T010 [US3] Extend `front-end/src/components/admin/homework/WriteVideoEditor.tsx` to render an embedded `<AudioPlayer mediaSourceKind="YOUTUBE" audioUrl={value} audioFileId={null} />` preview whenever the current field value resolves a valid YouTube id, and show no preview area when the field is empty or unresolved

**Checkpoint**: Teacher gets live authoring feedback — US3 independently testable via quickstart Scenario 3

---

## Phase 6: User Story 4 - Existing writing homeworks are unaffected (Priority: P2)

**Goal**: Writing homeworks created before this feature keep behaving exactly as before, with no video field, embed, or layout change unless a teacher explicitly adds one

**Independent Test**: Open several pre-existing writing homeworks (as teacher and as student) after T003–T010 land and confirm no video area appears and editing/saving still works (quickstart Scenario 4)

### Implementation for User Story 4

- [ ] T011 [US4] Manually verify at least 3 pre-existing writing homeworks (no video ever set) render and save unchanged in the editor (`HomeworkEditorPage.tsx` + `WriteVideoEditor.tsx`), the standalone write page (`HomeworkWritePage.tsx`), and the inline panel (`HomeworkInlinePanel.tsx`) after this feature's changes; fix any regression found in those files

**Checkpoint**: No regression for legacy writing homeworks — US4 verified via quickstart Scenario 4

---

## Phase 7: User Story 5 - Submitted students keep the video they answered against (Priority: P2)

**Goal**: A student who already submitted a writing homework keeps seeing the video as it was at their submit time, even after the teacher changes or removes it, per the existing freeze-submitted-homework rule (spec 039)

**Independent Test**: Assign a writing homework with a video, one student submits, teacher changes the video, confirm the submitted student's view/review still shows the original video while a not-yet-submitted student sees the update (quickstart Scenario 5)

### Implementation for User Story 5

- [X] T012 [P] [US5] Add a `WRITE` + video case to `back-end/src/test/java/com/kuky/backend/learning/HomeworkFreezeSubmittedIntegrationTest.java`: student submits against a writing homework with a YouTube video, teacher changes the video, assert the submission's snapshot/review still returns the original video while a not-yet-submitted assignee sees the updated one
- [ ] T013 [US5] Manually run quickstart Scenario 5 end-to-end in the browser (two students, one submits, teacher edits the video, verify the frozen vs. live split) and fix any gap found — this should already pass with no new backend code, since `AssignmentSnapshot.serialize()` already captures `audioUrl`/`audioFileId`/`mediaSourceKind` for every homework type

**Checkpoint**: Freeze-on-submit correctly covers the writing-homework video — US5 independently testable via quickstart Scenario 5 and the new integration test

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Backend unit coverage and final sign-off across all stories

- [X] T014 [P] Extend `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` with `resolveAudio` cases for `WRITE`: valid YouTube URL accepted, invalid/malformed URL rejected, no video (null/blank kind) saves fine, and `AUDIO_URL`/`UPLOADED_FILE`/`VIDEO_PAGE` rejected for `WRITE`
- [X] T015 [P] Confirm the placement test and other non-homework surfaces (`front-end/src/components/placement/`) remain untouched by this feature
- [ ] T016 Run full quickstart validation (`specs/048-writing-youtube-prompt/quickstart.md` Scenarios 1–5) end-to-end and fix any gaps found

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories (T003 must land before any story's save path can be exercised)
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - US1 and US2 (both P1) should ship together as the real MVP — US1 alone persists a video nobody can see; US2 alone has nothing to render
  - US3, US4, US5 (P2) can each proceed independently once US1/US2 exist, in any order
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Starts after Foundational. No dependency on other stories.
- **User Story 2 (P1)**: Starts after Foundational. Reads data that US1's editor writes, but is independently testable by seeding a video directly via the API/DB per its own Independent Test.
- **User Story 3 (P2)**: Extends the `WriteVideoEditor.tsx` component created in US1 (T004) — start after US1's T004 lands.
- **User Story 4 (P2)**: A verification pass over the files touched by Foundational + US1 + US2 — start after those land.
- **User Story 5 (P2)**: Depends on Foundational (T003) only for data to exist; does not depend on US1/US2/US3/US4 code, since the freeze snapshot already serializes the relevant fields unconditionally.

### Within Each User Story

- Story-specific setup before implementation (none needed beyond Phase 1/2 here)
- New components before wiring them into pages (T004 before T005/T006/T007; T004 before T010)
- Backend validation before backend test coverage (T003 before T012/T014)
- Story complete before moving to the next priority, per the checkpoints above

### Parallel Opportunities

- T002 (i18n) can run in parallel with T001
- T004 (new `WriteVideoEditor.tsx` file) can be built in parallel with other Foundational/Setup work, though it can't be *wired in* until T003 lands (US1 phase starts after Foundational)
- T009 (spot-check InlinePanel/Preview) can run in parallel with T008 (both touch different files)
- T012 (new test) and T014 (test extension) can run in parallel with each other and with T015
- Different user stories (US3, US4, US5) can be worked on in parallel by different people once US1 + US2 are done

---

## Parallel Example: Foundational → User Story 1 handoff

```bash
# After T003 lands, launch these together:
Task: "Create front-end/src/components/admin/homework/WriteVideoEditor.tsx"
Task: "Add es/en/ro copy for the writing-video field (if not already done in Setup)"
```

## Parallel Example: Polish

```bash
Task: "Extend HomeworkAdminServiceTest.java with resolveAudio WRITE cases"
Task: "Add WRITE+video case to HomeworkFreezeSubmittedIntegrationTest.java"
Task: "Confirm placement test surfaces are untouched"
```

---

## Implementation Strategy

### MVP First (User Story 1 + User Story 2)

Both are P1 for a reason: attaching a video that never renders (US1 alone) and rendering a field nobody can set (US2 alone) are each incomplete on their own from a user's perspective, even though they're independently *testable* as written.

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks everything)
3. Complete Phase 3: User Story 1
4. Complete Phase 4: User Story 2
5. **STOP and VALIDATE**: run quickstart Scenario 1 end-to-end
6. Deploy/demo if ready — this is the real MVP

### Incremental Delivery

1. Setup + Foundational → API accepts a WRITE video
2. US1 + US2 → MVP: teacher attaches, student sees it embedded
3. US3 → authoring preview (nice-to-have, no user-facing dependency on US4/US5)
4. US4 → regression sign-off for legacy writing homeworks
5. US5 → freeze-on-submit coverage (should already work; this phase mainly adds the regression test and browser confirmation)
6. Polish → backend test coverage rounding out T003, full quickstart sign-off

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- No database migration in this feature — see `data-model.md`
- Two of the three student-facing renderers (`HomeworkInlinePanel.tsx`, `HomeworkPreview.tsx`) and the freeze snapshot (`AssignmentSnapshot`) already handle this generically; T009, T011, and T013 are verification tasks confirming that rather than new implementation
- Verify each story against its quickstart scenario before moving to the next
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence
