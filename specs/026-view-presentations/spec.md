# Feature Specification: View Presentations On-Site

**Feature Branch**: `026-view-presentations`

**Created**: 2026-08-06

**Status**: Draft

**Input**: User description: "Enhance the presentations on the website by allowing the student to actually open a presentation and view it on the website directly."

## Clarifications

### Session 2026-08-06

- Q: Where should the PDF open? → A: Dedicated full-page view on the site (back/close returns to learning materials)
- Q: Should the full-page viewer also offer Download? → A: Download only from the learning materials file list (viewer is view + leave)
- Q: For PDF files in the list, which action is primary? → A: Open/View is primary; Download is secondary for PDFs
- Q: Should the viewer page stay usable after refresh or a bookmark? → A: Yes — same PDF reloads if the student still has access (bookmark/refresh OK)
- Q: How should “leave the viewer” return to learning materials? → A: Explicit “Back to learning materials” always goes to the learning page; browser Back uses normal history

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Student opens a PDF presentation in the browser (Priority: P1)

A student who has been shared a presentation goes to their learning materials, sees a PDF file attached to that presentation, and opens it. The site navigates to a dedicated full-page viewer so they can read the PDF without downloading it first. They can leave the viewer via an explicit “Back to learning materials” control that always returns them to the learning page (browser Back still follows normal history).

**Why this priority**: This is the core value — students currently must download every file to see it. In-browser viewing for PDFs removes that friction for the most common “just look at the slides” use case.

**Independent Test**: Share a presentation that has at least one PDF with a student; as that student, open the PDF from learning materials and confirm the content is readable on the site without a download completing first.

**Acceptance Scenarios**:

1. **Given** a shared presentation with a PDF file, **When** the student chooses to open/view that file, **Then** the site opens a dedicated full-page viewer showing that PDF (not only a forced download).
2. **Given** the student is on the full-page PDF viewer, **When** they use the explicit “Back to learning materials” control, **Then** they land on the learning materials page with the presentation list available (even if they originally opened the viewer from a bookmark).
3. **Given** the student is on the full-page PDF viewer after navigating from learning materials, **When** they use the browser Back button, **Then** normal browser history applies (typically returning to learning materials).
4. **Given** a shared presentation with several files including at least one PDF, **When** the student opens one PDF, **Then** only that file is shown in the viewer and they can later open another PDF from the same presentation.
5. **Given** a student who is not shared a presentation, **When** they attempt to open one of its files, **Then** they cannot view the content (same access rules as download today).
6. **Given** a student who previously opened a PDF viewer page and still has access, **When** they refresh the page or revisit the same viewer address, **Then** the same PDF is shown again.
7. **Given** a student who bookmarked a PDF viewer page but no longer has access to that presentation, **When** they open that address, **Then** they cannot view the content.

---

### User Story 2 - Download remains available for every file (Priority: P1)

Students can still download any attached file (PDF or PowerPoint) by its display name, whether or not that file can also be opened in the browser. Opening a PDF on the site does not remove the option to save a copy.

**Why this priority**: Offline study, annotation in desktop apps, and PowerPoint files still need download. Viewing and downloading are complementary, not replacements.

**Independent Test**: As a student with a multi-file presentation (PDF + PowerPoint), confirm each file still has a working download, and that opening a PDF does not block downloading that same PDF afterward.

**Acceptance Scenarios**:

1. **Given** a shared presentation with a PDF, **When** the student downloads that file, **Then** they receive the file under its display name as today.
2. **Given** a shared presentation with a PowerPoint file, **When** the student downloads it, **Then** the download succeeds (unchanged from today).
3. **Given** a student has opened a PDF in the on-site viewer, **When** they return to learning materials and choose download for that same file, **Then** the download still works.

---

### User Story 3 - Non-viewable files stay download-only with a clear affordance (Priority: P2)

When a shared presentation includes a PowerPoint (or any attached type that cannot be shown in the browser), the student sees that they can download it but not open it on the site. The UI does not offer a broken “view” action for those files.

**Why this priority**: Prevents confusion and failed opens; secondary to PDF viewing because PowerPoint remains useful via download.

**Independent Test**: Share a presentation with only a PowerPoint file; as the student, confirm download works and there is no view/open action that fails or pretends the file can be shown on the site.

**Acceptance Scenarios**:

1. **Given** a shared presentation whose only file is PowerPoint, **When** the student views that presentation in learning materials, **Then** they can download the file and do not get an on-site view action for it.
2. **Given** a shared presentation with both a PDF and a PowerPoint, **When** the student looks at the file list, **Then** the PDF shows Open/View as the primary action with Download as a secondary control, while the PowerPoint offers download only (no open/view).
3. **Given** a PDF fails to load in the viewer (network error or file unavailable), **When** the error is shown, **Then** the student sees a clear message and can return to learning materials to try download if the file remains available.

---

### Edge Cases

- A presentation with no attached files continues to show the existing empty/no-file state (no view or download controls).
- Very large PDFs may take longer to appear on the full-page viewer; the student sees a loading state and can navigate back without breaking the learning page.
- Closing the viewer mid-load via “Back to learning materials” (or browser Back) does not leave the learning page in a broken state.
- Access follows existing presentation sharing: only students who are shared the presentation can open or download its files; teachers/admins managing content are out of scope for this student-facing viewer. Refreshing or revisiting a viewer address after access is revoked must not show the file.
- Multi-file presentations: each PDF is opened individually; there is no requirement to “play” all files as one slideshow.
- File type rules for upload are unchanged (PowerPoint and PDF remain the allowed types); this feature does not add new upload formats.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Students who can access a shared presentation MUST be able to open each attached PDF on the website and read its content without first completing a download-to-disk flow.
- **FR-002**: Opening a PDF MUST take the student to a dedicated full-page viewer on the site that shows the selected PDF clearly enough to read typical slide/handout content (page navigation or continuous scroll as appropriate). The viewer MUST include an explicit “Back to learning materials” control that always navigates to the learning materials page (not merely browser history). The viewer MUST NOT include a download control; downloading remains available only from the learning materials file list. Browser Back continues to follow normal history.
- **FR-003**: Students MUST still be able to download every attached file individually by display name from the learning materials file list, including PDFs that are also viewable on the site and PowerPoint files that are not.
- **FR-004**: The system MUST NOT offer an on-site open/view action for PowerPoint files (or other non-PDF attached types); those files remain download-only.
- **FR-005**: Opening or downloading a presentation file MUST enforce the same sharing/access rules already used for student presentation downloads — students without access MUST NOT be able to view file content.
- **FR-006**: When a PDF cannot be shown (load failure, missing file, or access denied), the student MUST see a clear error on the viewer page and MUST still have the explicit “Back to learning materials” control so they can return and use download as a fallback when download is still permitted.
- **FR-007**: Presentations with multiple files MUST allow the student to open each viewable (PDF) file independently from the file list.
- **FR-008**: Existing presentation upload, multi-file management, sharing, and display-name behavior MUST remain unchanged; this feature only adds on-site viewing for eligible files.
- **FR-009**: The student MUST be able to tell from the learning materials UI which files can be opened on the site versus which are download-only. For PDF files, Open/View MUST be the primary action and Download MUST be available as a secondary control; for PowerPoint files, only Download is offered.
- **FR-010**: The full-page PDF viewer MUST remain usable after a browser refresh or a later revisit of the same viewer address, as long as the student still has access to that presentation file; if access has been revoked, the content MUST NOT be shown.

### Key Entities

- **Presentation**: Existing shared teaching material unit the student already sees in learning materials.
- **Presentation File**: An attached teaching file (display name, type, size); PDFs are eligible for on-site viewing; PowerPoint files remain download-only.
- **Presentation Share**: Existing grant that controls which students may view or download a presentation’s files.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A student with access to a shared PDF can open the full-page viewer and begin reading that PDF within 10 seconds under normal conditions (excluding unusually large files), without saving the file to disk first.
- **SC-002**: 100% of shared PowerPoint files remain downloadable after the change, and none incorrectly offer a working on-site viewer.
- **SC-003**: In a single visit, a student can open a PDF on the full-page viewer, use “Back to learning materials”, and successfully download that same PDF or another file from the same presentation.
- **SC-004**: Students without a share for a presentation cannot open any of its files on the site, including via a previously bookmarked viewer address (same confidentiality expectation as download today).
- **SC-005**: On first try, students can identify which files are openable on the site versus download-only without teacher instruction (PDF: primary Open/View plus secondary Download; PowerPoint: Download only).
- **SC-006**: A student with ongoing access can refresh or reopen the same PDF viewer address and still see that PDF without returning to the learning list first.

## Assumptions

- Eligible on-site viewing applies to PDF attachments only; PowerPoint remains download-only because browsers cannot reliably display PowerPoint without heavy conversion or third-party viewers.
- Download is retained for all file types from the learning materials file list only; viewing does not replace download, and the full-page viewer does not offer download.
- For PDF files, Open/View is the primary student action and Download is secondary on the learning materials file list; PowerPoint remains download-only.
- The viewer is a student-facing dedicated full page reached from learning materials (not an overlay/dialog and not a new browser tab); the same address remains usable after refresh or bookmark while access lasts. An explicit “Back to learning materials” control always returns to the learning page; browser Back uses normal history. A separate teacher “preview” admin flow is out of scope unless later requested.
- Students use a modern browser with built-in or standard PDF viewing capability on desktop and mobile; highly exotic PDF features (forms, embedded video) are not a goal.
- No new file formats are introduced; upload validation stays PowerPoint + PDF as today.
- Existing auth/session and presentation-share checks continue to gate file access; no public unauthenticated viewing.
- Multi-file presentations continue to list files individually; there is no requirement to merge PDFs into a single viewer session.
