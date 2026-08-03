# Feature Specification: Multiple Files per Presentation

**Feature Branch**: `023-presentation-multi-files`

**Created**: 2026-08-03

**Status**: Draft

**Input**: User description: "Allow multiple files to be uploaded per presentation"

## Clarifications

### Session 2026-08-03

- Q: Maximum files per presentation? → A: Cap at 10 files per presentation
- Q: Upload selection style? → A: One file per upload action (repeat to add more)
- Q: Duplicate original filenames? → A: Allow duplicates, but auto-suffix the displayed name (e.g. deck.pptx, deck (2).pptx)
- Q: File list order? → A: Oldest first (earliest upload at the top)
- Q: Auto-suffix after a file is removed? → A: Display names stay fixed after upload; removing a file does not renumber the others

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Teacher attaches several files to one presentation (Priority: P1)

Paula creates or opens a presentation in the admin Presentations area and uploads more than one teaching file (for example a PowerPoint deck and a PDF handout, or several related decks). Each successful upload is kept alongside the others — uploading a new file does not replace an existing one. She can see the list of attached files with their display names and remove any individual file she no longer wants.

**Why this priority**: This is the core capability. Today a presentation can hold only one file; without multi-file upload and per-file removal, the feature does not exist.

**Independent Test**: As the teacher, open a presentation with no files, upload two allowed files one after another, confirm both appear in the list, then remove one and confirm the other remains.

**Acceptance Scenarios**:

1. **Given** a presentation with no attached files, **When** the teacher uploads an allowed file, **Then** that file appears in the presentation's file list with its display name (normally the original filename).
2. **Given** a presentation that already has one or more files, **When** the teacher uploads another allowed file, **Then** the new file is added to the list and all previously attached files remain.
3. **Given** a presentation that already has a file named `deck.pptx`, **When** the teacher uploads another file also named `deck.pptx`, **Then** both remain attached and their display names are distinct via auto-suffix (for example `deck.pptx` and `deck (2).pptx`).
4. **Given** a presentation with multiple attached files, **When** the teacher removes one specific file, **Then** only that file is removed and the others stay attached with their existing display names unchanged.
5. **Given** a presentation that has `deck.pptx` and `deck (2).pptx`, **When** the teacher removes `deck.pptx`, **Then** `deck (2).pptx` remains listed under that same display name (not renumbered to `deck.pptx`).
6. **Given** the teacher attempts to upload a file that is empty, too large, or of a disallowed type, **When** the upload is rejected, **Then** a clear error is shown and the existing file list is unchanged.

---

### User Story 2 - Student downloads each file from a shared presentation (Priority: P1)

A student who has been shared a presentation opens their learning area and sees every file attached to that presentation. They can download each file individually by its display name (original filename, or an auto-suffixed name when duplicates exist), rather than being limited to a single download action for the whole presentation.

**Why this priority**: Multi-file upload only delivers value if students can obtain every attached material. Equal priority with teacher upload because both halves are required for the outcome.

**Independent Test**: As the teacher, attach two named files to a presentation and share it with a student; as that student, open learning materials and download each file, confirming both downloads succeed and match the expected names.

**Acceptance Scenarios**:

1. **Given** a presentation shared with a student that has two or more attached files, **When** the student views that presentation in their learning area, **Then** they see a download action for each file (identified by its distinct display name).
2. **Given** a shared presentation with multiple files, **When** the student downloads one file, **Then** they receive that file saved under its display name and can still download the others afterward.
3. **Given** a shared presentation with no attached files, **When** the student views it, **Then** they see a clear empty/no-file state (not a broken download control).
4. **Given** a shared presentation that previously had one file and now has several, **When** the student reloads their learning area, **Then** all current files are listed for download.

---

### User Story 3 - Existing single-file presentations keep working (Priority: P2)

Presentations that already have a single attached file continue to behave correctly for both teacher and student after the change. The teacher still sees that file in the new multi-file list and can add more or remove it; the student still sees and can download it.

**Why this priority**: Continuity for content already in use; secondary to the new multi-file happy path but required so launch does not break existing materials.

**Independent Test**: Use a presentation that already had one file before the feature; confirm the teacher sees it in the file list and the shared student can still download it, then add a second file and confirm both are available.

**Acceptance Scenarios**:

1. **Given** a presentation that had exactly one file before this feature, **When** the teacher opens it in admin, **Then** that file appears in the multi-file list without requiring re-upload.
2. **Given** such a presentation shared with a student, **When** the student opens learning materials, **Then** they can still download that file as before.
3. **Given** such a presentation, **When** the teacher uploads an additional file, **Then** both the original and the new file are available to shared students.

---

### Edge Cases

- Deleting a presentation removes all of its attached files so nothing is left orphaned for students or the teacher.
- Removing the last remaining file returns the presentation to the no-file empty state for both admin and student views.
- Concurrent uploads or removals by the same teacher resolve without corrupting the remaining file set (last successful action wins per file; other files stay intact).
- A presentation that already has 10 files rejects further uploads with a clear message until a file is removed.
- File type and size rules match the existing presentation-file rules (PowerPoint and PDF, same size cap per file); disallowed uploads never partially attach a broken entry.
- Uploading a file whose original name matches another file already on the same presentation is allowed; the new file receives an auto-suffixed display name (and download filename) so both remain distinguishable. Display names are assigned at upload time and do not change when other files are removed.
- Sharing and unsharing a presentation continue to control student visibility of the whole presentation (including all its files); there is no per-file sharing.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow a presentation to have zero or more attached teaching files (not limited to a single file).
- **FR-002**: The teacher MUST be able to upload an additional allowed file to a presentation without replacing existing attached files. Each upload action accepts exactly one file; attaching several files requires repeating the upload.
- **FR-003**: The teacher MUST be able to view the full list of files attached to a presentation, including each file's display name. When two or more files on the same presentation would share the same original filename, the system MUST assign distinct auto-suffixed display names (for example `deck.pptx`, `deck (2).pptx`) so every listed file is distinguishable.
- **FR-004**: The teacher MUST be able to remove any individual attached file without affecting the other files on that presentation.
- **FR-005**: Students who can access a shared presentation MUST be able to see and download each attached file individually, using the same distinct display names shown to the teacher (including any auto-suffix for name collisions).
- **FR-006**: Presentations with no attached files MUST show a clear empty state to the teacher and to students (no broken download controls).
- **FR-007**: Upload validation MUST reject empty files, files over the existing per-file size limit, and files that are not an allowed presentation type (PowerPoint or PDF), without changing the existing file list.
- **FR-008**: Existing presentations that already have a single attached file MUST retain that file and expose it through the multi-file list and student download experience without requiring re-upload.
- **FR-009**: Deleting a presentation MUST remove all of its attached files from availability.
- **FR-010**: The system MUST enforce a maximum of 10 files per presentation and reject uploads beyond that limit with a clear message.
- **FR-011**: Attached files for a presentation MUST be shown oldest-first (earliest upload at the top) for both teacher and student views; newly uploaded files appear at the end of the list.
- **FR-012**: Sharing a presentation with students MUST grant access to all of that presentation's attached files; there is no separate per-file share control.
- **FR-013**: Downloaded files MUST use the distinct display name (including any auto-suffix) as the saved filename so students can tell colliding originals apart after download.
- **FR-014**: Once assigned, a file's display name MUST remain unchanged for the life of that attachment; removing another file on the same presentation MUST NOT renumber or rewrite remaining display names.

### Key Entities

- **Presentation**: An existing teaching material unit (title, level, shares) that students may be granted access to; now associated with a collection of files rather than at most one.
- **Presentation File**: One uploaded teaching file (original name, distinct display name when needed, type, size) belonging to exactly one presentation; independently addable and removable.
- **Presentation Share**: Existing grant of a presentation to a student; visibility of all attached files follows the share, unchanged in concept.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A teacher can attach at least two allowed files to one presentation and confirm both remain listed after a page reload, in under 2 minutes.
- **SC-002**: A student with access to a multi-file presentation can successfully download every attached file on the first attempt in a single visit to the learning area.
- **SC-003**: 100% of presentations that had a single file before the change still expose that file for teacher management and student download after the change, without re-upload.
- **SC-004**: Rejected uploads (wrong type, oversized, or over the per-presentation file limit) never leave a partial or orphaned file visible in the list.
- **SC-005**: Removing one file from a presentation with N files leaves exactly N−1 files available to shared students on their next view.

## Assumptions

- Scope is the existing admin Presentations materials (PowerPoint/PDF files attached to presentations and downloaded from the student learning area), not homework attachments, placement-test audio, or other upload types.
- Allowed file types and the per-file size limit remain the same as today's single-file presentation uploads (PowerPoint and PDF, existing size cap).
- Maximum files per presentation is 10; the limit is communicated when reached.
- Students download files one at a time; a zip/bundle download is out of scope.
- Each teacher upload action accepts exactly one file (no multi-select in the file picker); additional files are added by repeating the upload.
- Teacher reordering of files (drag-and-drop) is out of scope; files are always listed oldest-first by upload time.
- Per-file rename in the admin UI is out of scope; display names come from the original uploaded filename, with automatic suffixes only when names collide on the same presentation.
- Duplicate original filenames on the same presentation are allowed; colliding names get auto-suffixed display/download names at upload time (for example `deck.pptx`, `deck (2).pptx`), and those names stay fixed even if other files are later removed.
- Replacing a specific file in place (upload-over-one) is out of scope; the teacher removes the old file and uploads a new one.
- Existing share, level, title, and unit-assignment behavior for presentations is unchanged aside from multi-file support.
