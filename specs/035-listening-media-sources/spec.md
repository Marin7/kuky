# Feature Specification: Listening Media Sources

**Feature Branch**: `035-listening-media-sources`

**Created**: 2026-08-11

**Status**: Draft

**Input**: User description: "For listening homeworks, there are 2 options now, Enlace (URL) - which tries to start an audio and Archivo subido (upload an audio file). I want to have a URL which redirects to a video too. (no wireframe, just a link to a page which has a video). Also, it would be nice to have a Youtube option as well, which will wireframe a Youtube video"

## Clarifications

### Session 2026-08-11

- Q: When reopening an existing listening homework whose media is a YouTube URL under the old Enlace field, which editor source option is selected? → A: Keep it under Enlace (audio URL) in the editor; students still get the embed
- Q: What should happen for existing (and new Enlace-saved) Vimeo URLs now that there is no dedicated Vimeo option? → A: Keep in-page Vimeo embed when the URL is under Enlace (preserve today’s behaviour)
- Q: When the teacher pastes a YouTube URL into Enlace (audio URL) instead of the YouTube option, what happens? → A: Auto-switch the editor to the YouTube option with that URL
- Q: Can a listening homework be saved without any media source? → A: Required — at least one of the four source types must be set before save
- Q: When a student uses the external video-page link, how should it open? → A: Always open in a new browser tab/window
- Q: What happens for existing listening homeworks that have no media, now that media is required? → A: Treat as invalid immediately — hide/block student access until the teacher adds media
- Q: When the teacher pastes a YouTube URL into the external video-page link option, what happens? → A: Auto-switch to the YouTube option with that URL

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher picks an external video page link (Priority: P1)

When authoring a listening homework, Paula chooses a new source option for an external video page (not an embedded player). She pastes a normal web link to a page that hosts a video. The homework saves with that link as its media source. When a student opens the homework, they see a clear link that opens in a new browser tab/window so they can watch the video on that page while keeping the homework available — there is no in-page video player for this option.

**Why this priority**: Today a generic video-page URL under “Enlace” is treated like playable audio and fails for students. Giving teachers an explicit “link to a video page” path unblocks the most common gap without requiring hosting or embeds.

**Independent Test**: Create a listening homework with only an external video-page URL; open it as a student and confirm a working outbound link appears and no in-page audio/video player is shown for that source.

**Acceptance Scenarios**:

1. **Given** the teacher is editing a listening homework’s media source, **When** they choose the external video-page option and paste a valid URL, **Then** the homework can be saved with that URL as the sole media source.
2. **Given** a listening homework saved with an external video-page URL, **When** a student opens it, **Then** they see a clear control/link to open that page in a new browser tab/window and do not see an in-page audio player or embedded video player for that source.
3. **Given** the teacher switches from another source type to the external video-page option, **When** they confirm the change, **Then** the previous source (uploaded file, audio URL, or YouTube) is cleared so only one source remains.

---

### User Story 2 - Teacher embeds a YouTube video (Priority: P1)

When authoring a listening homework, Paula chooses an explicit YouTube option, pastes a YouTube watch/share/shorts URL, and saves. Students see the video embedded (wireframed) on the homework page so they can watch without leaving the assignment.

**Why this priority**: YouTube is a primary teaching source; an explicit embed option makes intent clear and avoids mixing YouTube with “try to play as audio” URL behaviour.

**Independent Test**: Create a listening homework with only a YouTube URL via the YouTube option; open it as a student and confirm an in-page YouTube player appears for that video.

**Acceptance Scenarios**:

1. **Given** the teacher is editing a listening homework’s media source, **When** they choose YouTube and paste a recognised YouTube URL, **Then** the homework can be saved and a preview of the embedded player is available in the editor.
2. **Given** a listening homework saved with a YouTube source, **When** a student opens it, **Then** they see an in-page embedded YouTube player for that video (not only a plain outbound link, and not a native audio player).
3. **Given** the teacher pastes a non-YouTube URL into the YouTube option, **When** they try to save or the field is validated, **Then** they receive a clear error and the invalid value is not treated as a successful YouTube source.
4. **Given** the teacher has Enlace (audio URL) selected and pastes a recognised YouTube URL, **When** the URL is recognised, **Then** the editor auto-switches to the YouTube option with that URL (it is not left classified as Enlace).
5. **Given** the teacher has the external video-page link option selected and pastes a recognised YouTube URL, **When** the URL is recognised, **Then** the editor auto-switches to the YouTube option with that URL (it is not left as an outbound-only video-page link).

---

### User Story 3 - Existing audio URL and uploaded file options stay available (Priority: P2)

Paula can still choose “Enlace (URL)” for a direct audio link that plays in-page, or “Archivo subido” to upload an audio file. Students continue to hear those sources with the existing in-page audio player behaviour.

**Why this priority**: Preserves current workflows; the new options extend the picker rather than replace it.

**Independent Test**: Author one listening homework with a direct audio URL and another with an uploaded file; confirm both still play for students as today.

**Acceptance Scenarios**:

1. **Given** the teacher chooses the audio-URL option and pastes a direct audio link, **When** a student opens the homework, **Then** an in-page audio player uses that link.
2. **Given** the teacher chooses uploaded file and uploads an audio file, **When** a student opens the homework, **Then** an in-page audio player plays the uploaded file.
3. **Given** the teacher is choosing a media source, **When** they view the source options, **Then** they see four clear choices: audio URL, uploaded audio file, external video-page link, and YouTube embed.

---

### User Story 4 - Existing listening homeworks keep working (Priority: P2)

Listening homeworks already saved with an audio URL or uploaded file continue to present correctly for students and teachers after the new options exist. Homeworks whose saved URL is a YouTube link continue to show as an embedded YouTube player for students.

**Why this priority**: No teacher should need to re-author existing listening materials for the feature to ship safely.

**Independent Test**: Open previously saved listening homeworks (audio URL, uploaded file, and a YouTube URL stored under the old single-URL field) and confirm student playback matches prior expectations.

**Acceptance Scenarios**:

1. **Given** an existing listening homework with an uploaded audio file, **When** a student opens it after this feature ships, **Then** the audio still plays in-page.
2. **Given** an existing listening homework whose URL is a direct audio link, **When** a student opens it, **Then** the audio still plays in-page.
3. **Given** an existing listening homework whose URL is a YouTube link, **When** a student opens it, **Then** the YouTube video still embeds in-page.
4. **Given** an existing listening homework whose URL is a YouTube link saved under the old Enlace field, **When** the teacher reopens the editor, **Then** the Enlace (audio URL) option remains selected (not auto-switched to YouTube); students still see the in-page embed.
5. **Given** an existing listening homework with no media source, **When** a student tries to open it after this feature ships, **Then** access is blocked (or the assignment is not available to take) until the teacher adds a valid media source.

---

### Edge Cases

- What happens when the external video-page URL is missing or blank on save? Save is blocked until a valid URL is provided for that source type (media is required for listening homework).
- What happens when no media source is set at all on a listening homework? Save is blocked; the teacher must choose one of the four source types and complete it. Existing homeworks already lacking media are incomplete: students cannot open/take them until the teacher adds media.
- What happens when a YouTube URL cannot be recognised (playlist-only link, channel page, malformed string)? The teacher sees a clear validation message; students never see a broken embed for that save.
- What happens when the student opens an external video-page link and the destination is unavailable? The product still shows the link correctly; failure at the destination site is outside product control.
- How does the system handle a YouTube URL typed into Enlace during editing? The editor auto-switches to the YouTube option with that URL. This does not change how already-saved legacy YouTube-under-Enlace homeworks open in the editor (they stay on Enlace until the teacher changes them).
- How does the system handle a YouTube URL typed into the external video-page link option? The editor auto-switches to the YouTube option with that URL (same as Enlace paste).
- How does the system handle switching source types mid-edit? Only one source is kept; changing type clears the previous value.
- What about Vimeo or other embeddable hosts? No dedicated Vimeo option. When a Vimeo URL is stored under Enlace, students MUST continue to see an in-page Vimeo embed (same as today). Other non-YouTube/non-Vimeo page URLs the teacher classifies as an external video-page link remain outbound-only. Dedicated options for additional hosts are out of scope.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: For listening homework media, the teacher MUST be able to choose exactly one of: direct audio URL, uploaded audio file, external video-page URL, or YouTube embed URL.
- **FR-001a**: A listening homework MUST NOT be saveable without an active media source; exactly one of the four source types MUST be fully set before save succeeds.
- **FR-001b**: Existing listening homeworks that currently have no media source MUST be treated as incomplete: students MUST NOT be able to open/take them until the teacher adds a valid media source; teacher save remains blocked until media is set.
- **FR-002**: The external video-page option MUST store a normal web URL and MUST present to students as an outbound link that always opens in a new browser tab/window, without an in-page audio player and without an embedded video player.
- **FR-003**: The YouTube option MUST accept common YouTube share/watch/shorts URL forms, MUST validate that the value refers to a YouTube video, and MUST present an in-page embedded YouTube player to students (and a matching preview for the teacher when authoring).
- **FR-004**: The existing direct audio URL option (Enlace) MUST continue to play direct audio links in an in-page audio player for students (and MUST NOT be used as the path for “just open a video page”). When the Enlace URL is a YouTube or Vimeo video URL, students MUST continue to see the corresponding in-page embed (backward-compatible with today’s behaviour).
- **FR-005**: The existing uploaded audio file option MUST continue to play in an in-page audio player for students.
- **FR-006**: Switching among source options MUST clear the previously selected source so a listening homework never has more than one active media source.
- **FR-007**: Existing listening homeworks with an uploaded file or a direct audio URL MUST keep working without re-authoring.
- **FR-008**: Existing listening homeworks whose stored URL is a YouTube video MUST continue to embed for students (backward compatible with today’s YouTube-in-URL behaviour).
- **FR-009**: Media source labelling in the teacher UI MUST make the four options distinguishable in the interface language(s) already used for homework authoring (Spanish primary labels such as Enlace / Archivo subido, plus clear labels for the video-page link and YouTube options).
- **FR-010**: Non-listening homework types MUST remain unaffected (no new media source requirement outside listening).
- **FR-011**: When the teacher reopens a listening homework whose media is a YouTube URL that was saved under the Enlace (audio URL) path (legacy or without an explicit YouTube source kind), the editor MUST keep Enlace selected and MUST NOT auto-switch to the YouTube option; student playback MUST still embed that YouTube video.
- **FR-012**: When the teacher is on the Enlace (audio URL) option or the external video-page link option and enters a recognised YouTube URL, the editor MUST auto-switch to the YouTube option populated with that URL (so new authoring does not leave YouTube classified as Enlace or as an outbound-only video-page link).

### Key Entities

- **Listening homework media source**: The single media attachment on a listening homework. Attributes conceptually include source kind (audio URL | uploaded audio file | external video-page URL | YouTube) and the corresponding URL or file reference.
- **External video-page URL**: A link to a third-party page that hosts a video; consumed only as navigation, not as embedded/playable media inside the product.
- **YouTube source**: A YouTube video URL intended for in-page embedding on the homework.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can attach an external video-page link to a listening homework and confirm the student sees only an outbound link (no failed audio player) in under 2 minutes.
- **SC-002**: A teacher can attach a YouTube video via the dedicated option and confirm the student sees an in-page embedded player in under 2 minutes.
- **SC-003**: In a manual review of at least 5 pre-existing listening homeworks covering uploaded audio, direct audio URL, and YouTube URL, 100% still present media correctly for students after the change.
- **SC-004**: 100% of attempted saves with a non-YouTube value in the YouTube option are blocked with a clear message before the homework is stored as a YouTube source.
- **SC-005**: Teachers can identify and select among all four source options without training beyond on-screen labels/hints (spot-check: another teacher or reviewer succeeds on first try).
- **SC-006**: 100% of attempted saves of a listening homework with no media source are blocked with a clear message.
- **SC-007**: 100% of existing listening homeworks with no media are unavailable for students to open/take until media is added.

## Assumptions

- Scope is listening homework media only (student take UI and teacher authoring). Placement-test audio and other surfaces are out of scope unless they already share the same listening media picker and inherit behaviour for free.
- “Wireframe a YouTube video” means an in-page embedded YouTube player, not a static thumbnail-only mock.
- “URL which redirects to a video” means an outbound link to a page that has a video — no in-product player and no attempt to stream that page as audio. That link always opens in a new browser tab/window.
- Only one media source per listening homework remains the rule; that source is required (listening homework cannot be saved with no media). Existing no-media listening homeworks are blocked for students until fixed.
- Dedicated options for Vimeo or other hosts are out of scope; Vimeo URLs under Enlace continue to embed in-page. Other hosts intended as “open the page” use the external video-page link option.
- Direct audio URL and uploaded file behaviour stay as today aside from clearer separation from the new options.
- Legacy YouTube URLs under Enlace stay classified as Enlace when reopened in the editor; only newly authored YouTube sources via the dedicated option (including after auto-switch from Enlace or video-page paste) are shown under YouTube. Student embed behaviour applies in both cases.
- Interface copy follows the site’s existing localisation approach (Spanish primary authoring labels).
- No change to homework grading, questions, or assignment flow — only how listening media is chosen and shown.
