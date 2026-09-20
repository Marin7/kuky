# Feature Specification: Writing Homework YouTube Prompt

**Feature Branch**: `048-writing-youtube-prompt`

**Created**: 2026-09-20

**Status**: Draft

**Input**: User description: "Enhance writing homeworks by allowing the teacher to provide a youtube URL based on which the homework will be done. Make sure the video can be wireframed, similar to how it works for listening homeworks"

## Clarifications

### Session 2026-09-20

- Q: Should the writing homework's video follow the existing "freeze submitted homework" rule, which already explicitly covers Writing homeworks? → A: Yes — freeze it like other media; a student who already submitted keeps seeing the video as it was at their submit time, even if the teacher later changes or removes it.
- Q: Where should the embedded video appear relative to the instructions and the student's answer box? → A: Directly below the instructions, above the answer box.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher attaches a YouTube prompt video to a writing homework (Priority: P1)

When authoring a writing homework, Paula pastes a YouTube video URL to serve as the writing prompt: students will watch the video and then write their response about it. She saves the homework with the video attached.

**Why this priority**: This is the core ask — without the ability to attach and persist a video, nothing else in this feature has a purpose.

**Independent Test**: Create a writing homework, add only a YouTube URL (no other change), save it, and confirm it persists and reopens with that URL still set.

**Acceptance Scenarios**:

1. **Given** the teacher is editing a writing homework, **When** they paste a recognised YouTube URL into the new video field and save, **Then** the homework saves successfully with that video attached.
2. **Given** a writing homework with no video set, **When** the teacher saves it with just title and instructions filled in (as before this feature), **Then** it saves successfully exactly as today — the video field is fully optional.
3. **Given** the teacher pastes a non-YouTube or malformed URL into the video field, **When** they try to save, **Then** they see a clear validation error and the save is blocked until the value is fixed or cleared.
4. **Given** a writing homework already has a video set, **When** the teacher clears the field and saves, **Then** the homework saves without a video (the video can be removed).

---

### User Story 2 - Student sees the prompt video embedded in the writing homework (Priority: P1)

When a student opens a writing homework that has a YouTube prompt, they see the video embedded in-page (wireframed) directly below the instructions, so they can read the instructions, watch the video, and then write their response — all without leaving the page or following an outbound link.

**Why this priority**: This matches the explicit ask ("wireframed … similar to listening") — an in-page embed is what makes the video usable as a writing prompt in the student's flow.

**Independent Test**: Open, as a student, a writing homework that has a saved YouTube URL and confirm an in-page playable YouTube embed appears; open one without a video and confirm no broken or empty player area appears.

**Acceptance Scenarios**:

1. **Given** a writing homework with a saved YouTube video, **When** a student opens it, **Then** they see an in-page embedded YouTube player positioned directly below the instructions and above their answer/response area.
2. **Given** a writing homework with no video, **When** a student opens it, **Then** the page looks exactly as it does today — no embed and no empty placeholder gap.
3. **Given** the student is on a small/mobile screen, **When** the embedded video renders, **Then** it fits the screen width without breaking the page layout.

---

### User Story 3 - Teacher previews the embed while authoring (Priority: P2)

While editing a writing homework, Paula can see a live preview of how the embedded video will look to students, the same way the listening-homework YouTube option already previews for teachers.

**Why this priority**: Reduces authoring mistakes (wrong link, wrong video) before assigning the homework. Not required for a minimal version, since save-time validation already blocks invalid URLs, but it meaningfully improves teacher confidence.

**Independent Test**: Paste a valid YouTube URL into the field while editing and confirm a working embedded preview appears in the editor without needing to save first.

**Acceptance Scenarios**:

1. **Given** the teacher pastes a valid YouTube URL, **When** the field updates, **Then** an embedded preview of that video appears in the editor.
2. **Given** the teacher clears the field, **When** a preview would otherwise show, **Then** no preview area (or an empty state) is shown instead.

---

### User Story 4 - Existing writing homeworks are unaffected (Priority: P2)

All writing homeworks created before this feature continue to behave exactly as before: no video field, no embed, same layout for both students and teachers.

**Why this priority**: Safety net so shipping this feature doesn't disturb existing content, layout, or grading.

**Independent Test**: Open several pre-existing writing homeworks after the change ships and confirm no video section renders, and that editing and saving other fields still works.

**Acceptance Scenarios**:

1. **Given** an existing writing homework authored before this feature, **When** a student opens it, **Then** the page renders as before, with no video area.
2. **Given** an existing writing homework authored before this feature, **When** the teacher opens it for editing, **Then** the video field is empty/unset, and editing other fields and saving still works normally.

---

### User Story 5 - Submitted students keep the video they answered against (Priority: P2)

A student who has already submitted a writing homework continues to see the exact video that was attached at the moment they submitted, even if the teacher later changes or removes it — consistent with how instructions and media are already frozen for submitted homework across the site.

**Why this priority**: Matches an existing, already-shipped rule (homework content is frozen at submit time) that already explicitly names Writing homework as in scope; skipping it for this one new field would silently break that guarantee.

**Independent Test**: Assign a writing homework with a video; one student submits. Teacher then changes the video to a different one (or removes it) and saves. Reopen the submitted student's work: it still shows the original video, not the new one. A second, not-yet-submitted student sees the updated video.

**Acceptance Scenarios**:

1. **Given** a student has submitted a writing homework that had a video attached, **When** the teacher later changes or removes that video and saves, **Then** the submitted student's view (and the teacher's review of that submission) still shows the original video from submit time.
2. **Given** a student has not yet submitted a writing homework, **When** the teacher changes or adds a video and saves, **Then** that student sees the current (updated) video when they open or take the homework.

---

### Edge Cases

- Teacher pastes a YouTube playlist link, a channel link, or a link that doesn't resolve to a single video: rejected the same way YouTube links are already validated for listening homeworks — a clear error, never saved as a valid video.
- Teacher pastes a shortened form (`youtu.be`, `shorts`) or a watch URL with extra query parameters: all equivalent recognised forms are accepted, matching what's already supported for listening homeworks.
- Teacher removes or changes the video after some students have already submitted: allowed; per the existing freeze rule, already-submitted students (and the teacher reviewing their submission) keep seeing the video as it was at that student's submit time, while students who have not yet submitted see the updated video (see User Story 5).
- The referenced YouTube video is later deleted, made private, or removed by its owner: the embed area still renders as usual; playback failing at YouTube's end is outside the product's control.
- A slow connection or a region where YouTube itself is unreachable: the embed area still renders; playback failure from an external network/YouTube condition is not a product defect.
- A homework's type is changed away from Writing (or into Writing) while a video is set: the video field is only meaningful for writing homeworks; changing the type away from Writing clears or ignores it, matching the existing pattern where listening-only audio fields don't apply to other homework types.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Writing homework MUST support an optional YouTube video URL as a writing prompt, in addition to its existing title, instructions, and rich-text answer area.
- **FR-002**: The YouTube video field on a writing homework MUST remain fully optional; a writing homework MUST continue to be saveable with no video, exactly as before this feature.
- **FR-003**: When a video URL is set, it MUST be validated as a recognised YouTube video URL (standard watch, shortened, and short-form URL styles); an unrecognised value MUST block save with a clear message.
- **FR-004**: When a writing homework has a YouTube video set, students MUST see it as an in-page embedded ("wireframed") player, not merely an outbound link — matching how YouTube videos already embed in-page for listening homeworks.
- **FR-005**: The embedded video MUST appear directly below the homework's instructions and above the student's answer/response area.
- **FR-006**: Teachers MUST be able to add, change, or remove a writing homework's video at any time by editing the homework, the same as any other homework field.
- **FR-007**: While authoring, teachers MUST see a preview of the embedded video once a valid YouTube URL is entered, before saving.
- **FR-008**: This video field and its embed apply only to writing-type homework; other homework types (listening/audio, grammar, reading) are unaffected and keep their own existing media behaviour.
- **FR-009**: Existing writing homeworks created before this feature MUST continue to work unchanged (no video, no embed, no layout change) unless a teacher explicitly adds a video afterward.
- **FR-010**: The student-facing embed MUST render usably at both desktop and mobile widths, matching the responsive behaviour already used for listening-homework video embeds.
- **FR-011**: Freeze MUST apply to a writing homework's video the same way it already applies to instructions and media for other homework types: a submission's frozen copy MUST include the video that was set at that student's submit time, and the teacher's review of that submission MUST show that same frozen video, never a later edit. Students who have not yet submitted MUST see the current (live) video.

### Key Entities

- **Writing homework prompt video**: The optional YouTube video attached to a writing-type homework, intended for students to watch before writing their response. Attributes conceptually include the YouTube URL and whether a valid video is currently set.
- **Frozen writing prompt video**: The copy of the video reference captured in a submission's frozen homework copy (per the existing submission-freeze behaviour), shown whenever that submission is displayed or reviewed, regardless of later edits to the live homework.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can attach a YouTube video to a writing homework and confirm it appears embedded for students in under 2 minutes.
- **SC-002**: 100% of attempted saves with an unrecognised or invalid YouTube value in the video field are blocked with a clear message before being stored.
- **SC-003**: In a manual review of at least 5 pre-existing writing homeworks, 100% continue to display and function exactly as before (no video, no layout change).
- **SC-004**: 100% of writing homeworks with a saved YouTube video show a working in-page embedded player to students (not merely a link), verified by opening each in a review pass.
- **SC-005**: Teachers can find and use the new video field without additional training beyond on-screen labels (spot-check: another teacher or reviewer succeeds on first try).
- **SC-006**: In a review of at least 3 writing homeworks edited after a student submission, 100% of already-submitted students' views (and the teacher's review of those submissions) still show the video captured at that student's submit time, while every not-yet-submitted student sees the updated video.

## Assumptions

- "Wireframe" / "wireframed" is read the same way it already applies to listening homeworks: an in-page embedded YouTube player, not a static thumbnail or a plain outbound link.
- The request asks specifically for a YouTube URL, not the full four-way listening media picker (audio URL / uploaded file / external video-page link / YouTube). Writing homework gains a single optional YouTube video field with embedded playback; it does not gain audio-URL, uploaded-file, or generic video-page-link options.
- Exactly one video per writing homework (no multiple videos), mirroring the "one media source" rule already used for listening homework media.
- The video is a prompt for the writing task (context to watch before answering), not itself gradable content; grading of the written response is unchanged by this feature.
- No data migration is needed: this is a brand-new optional field, so no pre-existing writing homework has data to backfill.
- Interface copy and labels follow the site's existing localisation approach (Spanish primary authoring labels), consistent with the listening homework editor.
- Validation for "what counts as a YouTube URL" mirrors the forms already accepted for listening homeworks (standard watch, youtu.be, shorts, and embed link forms).
- The video's freeze behaviour (FR-011) follows the same mechanism already used to freeze instructions/media/questions for other homework types; the exact data-storage approach for the frozen copy is a planning-phase decision, not a specification-level one.
