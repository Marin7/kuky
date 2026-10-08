# Research: Unit as a PDF with Page Activities

**Feature**: `055-unit-pdf-activities` · **Date**: 2026-10-07

There were no open `NEEDS CLARIFICATION` items in Technical Context after reading the
code. These are the design decisions, made against how units, presentations and
activities work today.

## Current state (as read from the code)

- `units` (level, subject, position) holds a **mixed ordered sequence** of
  `presentations` (via `presentations.unit_id` + `unit_position`, FK `ON DELETE SET NULL`)
  and `homework_assignments` (via `homework_assignments.unit_id` + `unit_position`).
  `UnitService.setPresentations` / `setHomeworks` / `reorderContents` / `setAssignees`.
- Student access to a presentation = `presentation_shares` **or** `unit_assignments` on
  `presentations.unit_id` (`PresentationRepository.isSharedWith`, `findSharedSummariesForUser`).
- `activities` belong to a presentation (`presentation_id ON DELETE CASCADE`), have
  title/format/level/homework_type/position, an optional `(trigger_file_id, trigger_page)`
  meaning **"insert after this PDF page"**, plus instruction media (`instructions_text`,
  `youtube_url`, `image_id`) and a legacy `activity_instructions_files` table.
  Questions/answers/submissions mirror homework (`activity_questions`, `activity_answers`,
  `activity_submissions`), graded by `ActivityExerciseGradingService`, reviewed through
  `ActivityAdminService` (review queue, feedback, per-question teacher %).
- Student viewer (`PresentationPdfViewerClient`) renders every page in a scrolling stack and
  inserts a collapsible `ActivityPageSlot` **after** the trigger page. The slot shows the
  activity's media and the `ActivityPanel` form. This is today's "prompt": it sits in the
  page flow and does not block anything.
- Admin: an **Actividades** tab (`ActivitiesTab` = `ActivityAdminList` + `ActivityReviewQueue`)
  and `/panel/actividades/{nueva,$activityId}` editor routes.
- Activity submissions have **no** teacher-unseen flag today (only homework and quiz do).
- `pdfjs-dist` already exists on the front-end. The back-end has no PDF library.

## Decisions

### D1 — The unit's PDF is a unit-owned `presentations` row

- **Decision**: Keep the `presentations` / `presentation_files` tables. A unit owns **at most
  one** presentation: the row with `unit_id = <unit>`. That row is not shown in the
  Presentations tab and has exactly one PDF file. `presentations.unit_id` changes its meaning
  from "linked into" to "owned by": the FK becomes `ON DELETE CASCADE`, and a partial unique
  index enforces one row per unit. The owned row is created on the first PDF upload, with
  `title = unit.subject` and `level = unit.level`, and stays in sync when the unit is renamed.
- **Rationale**: Many things already key on presentation/file ids:
  - the student file endpoint `GET /learning/presentations/{id}/files/{fileId}` and its access gate
  - the student viewer route
  - `SharedPresentationSummary` with its `activities`
  - `activities.presentation_id`, the cascade on delete
  - the unit's mixed sequence (`unit_position`)
  - `PresentationFileStore`

  Reusing them leaves the student experience, grading and review almost untouched
  (Simplicity First).
- **Alternatives rejected**:
  - *New `unit_pdfs` table + `activities.unit_id`*: duplicates the file store, the access gate,
    the viewer route and the sequence handling, for no user-visible gain.
  - *Columns on `units`*: same duplication, and it breaks the mixed-sequence code that expects
    a presentation item.

### D2 — An activity is identified by `(presentation_id, page)`

- **Decision**: Rework `activities`:
  - add `page INT NOT NULL CHECK (page >= 1)` and `UNIQUE (presentation_id, page)`
  - drop `trigger_file_id`, `trigger_page`, `instructions_text`, `youtube_url`, `image_id`,
    `position`, `level` and `homework_type`
  - make `title` nullable (blank means "Actividad – página N", rendered client-side from i18n)
  - drop table `activity_instructions_files`

  Activities are ordered by `page`. Questions, answers, submissions, grading and review
  stay exactly as they are.
- **Rationale**: The page is the instructions (FR-004), and there is at most one activity per
  page (FR-003). A unit-owned presentation has only one file, so a file reference would
  only go stale when the PDF is replaced.
- **Alternatives rejected**:
  - *Keep `trigger_file_id`*: it has to be rewritten on every PDF replacement, and it allows
    states that cannot happen.
  - *Keep the media fields as optional extras*: the request says the page is the activity.
    YAGNI.

### D3 — Existing data is discarded

- **Decision**: Migration `V28` does the following:
  - `DELETE FROM activities` (cascades questions, answers and submissions)
  - detaches every presentation currently linked to a unit (`UPDATE presentations SET unit_id = NULL, unit_position = 0`)
  - drops `activity_instructions_files`

  The detached presentations stay as standalone presentations in the Presentations tab.
  Homework stays in its units (FR-015), so `homework_assignments.unit_id` and its
  `unit_position` are kept. Orphaned instruction files on disk are removed by hand; there is
  no runtime cleanup code.
- **Rationale**: The spec allows it (FR-014: no students hold units). Detaching instead of
  deleting keeps the teacher's existing decks (FR-016). Homework links into units remain valid.
- **Alternative rejected**: converting linked presentations into unit-owned ones. They can
  have several or non-PDF files, and the spec says not to bother.

### D4 — Page count comes from the client and is checked on the server

- **Decision**: The admin UI opens the chosen file with pdf.js before uploading and sends
  `pageCount` as a multipart field. The server checks that it is between 1 and 500 and that
  the file is a PDF (name/content-type check plus the `%PDF-` magic bytes), then stores it in
  `presentation_files.page_count`. That column is nullable and only required for unit-owned
  files. Activity pages are validated against it.
- **Rationale**: Only an admin can upload, and the browser already loads the document to
  render it. Adding PDFBox to the back-end just to count pages is a heavy dependency for one
  integer.
- **Alternative rejected**: server-side parsing with PDFBox, kept as a fallback if page counts
  ever turn out to be untrustworthy.

### D5 — Replacing the PDF keeps activities whose page still exists

- **Decision**: `POST /admin/units/{id}/pdf` (multipart, like the other upload endpoints) replaces the file (the new file is written first,
  then the old one is deleted after commit). Activities with `page > newPageCount` are
  deleted together with their submissions, but only when the request carries
  `removeOutOfRangeActivities=true`. Without that flag the server answers
  `409 ACTIVITIES_OUT_OF_RANGE`. The client already knows the activity pages and the new
  page count, so it shows the count and asks the teacher to confirm before sending.
- **Rationale**: FR-012 requires an explicit confirmation that states the count. The server
  guard makes sure no client can skip it.

### D6 — Unmarking a page with submissions needs confirmation

- **Decision**: `DELETE /admin/activities/{id}` answers `409 ACTIVITY_HAS_SUBMISSIONS` when
  submissions exist, unless `?deleteSubmissions=true` is passed.
- **Rationale**: FR-013 and US2-6. It mirrors D5.

### D7 — The student "prompt" stays as today's inline slot after the page

- **Decision**: Keep `PresentationPdfViewerClient`'s "insert after page N" slot, with
  `N = activity.page`. Students see the marked page, and the slot with the questions comes
  right below it. The slot loses its YouTube/image/text block, because the page above it is
  the instructions. On the stand-alone route `/aprendizaje/actividad/$activityId`, the page
  is rendered as the instructions with a single-page viewer (`PdfSinglePage`) above the
  `ActivityPanel`.
- **Rationale**: The spec says to keep today's activity behaviour, and today's behaviour is
  an inline, non-blocking slot after the page. The overlay wording in the spec came from
  spec 029 but was never built; we document the actual behaviour instead of adding a modal.
  This satisfies FR-009: the prompt appears at the page, does not block, and once the
  activity is completed it shows only its status.
- **Spec alignment**: in US3, "prompt / overlay" is read as today's in-flow slot.
  Completed activities still show their slot, collapsed with the status badge. That matches
  today, and the student can reopen the result from there.

### D8 — The student activity list (FR-010)

- **Decision**: In `UnitDetailContent`, the unit PDF's expanded body lists the activities
  (page order: "p. N · title · status") above the viewer. Each one links to
  `/aprendizaje/actividad/$id`.
- **Rationale**: This reuses the existing `ActivitySummary` data. No new endpoint.

### D9 — The teacher's unit view is a dedicated route

- **Decision**: New route `/panel/unidades/$unitId`, rendered by `UnitEditorPage`, with
  these sections:
  1. Header: level, subject, delete, assign students (reuses `UnitAssignDialog`).
  2. PDF: upload or replace, with a thumbnail strip of all pages. Activity pages carry a badge,
     and pages with work awaiting correction carry a dot.
  3. Selected page: large render, plus a "Marcar como actividad" switch. When the page is
     marked, a `PageActivityEditor` shows the optional title, `QuestionListEditor` (the same
     component homework and activities use today), Save, and Unmark.
  4. Per-activity progress: one row per assigned student with status and score. Opening a
     submission uses the existing `ActivityReviewDialog`, moved to `components/admin/units/`.
  5. Unit content order: `UnitContentSortableList`, with the PDF as one item plus homeworks.
     Homeworks are added through the existing `AddContentCombobox`, restricted to homework.

  The Units tab cards become compact summaries that link to this route.
- **Rationale**: Paging through a PDF and editing questions does not fit the inline
  expanding card. This follows the pattern of `/panel/tareas/$homeworkId`. The spec requires
  marking pages "from the same view" (FR-003), and both happen on this one route.
- **Alternative rejected**: a dialog inside `UnitCard`. It is too cramped for the
  page preview plus the question editor, and it is poor on phones.

### D10 — Unseen indicator for activity submissions

- **Decision**:
  - Add `activity_submissions.teacher_seen_at`, set to NULL whenever a student submits.
  - Opening a submission in the review dialog marks it seen, through
    `NotificationService.markActivitySeen`.
  - `BadgeSummary` gains a `units` flag, which also feeds `panel`. The **Unidades** tab
    trigger, the unit card, the unit-editor page thumbnails and the progress rows show the dot.
- **Rationale**: US4-3 asks for the in-site indicator. It mirrors
  `homework_submissions.teacher_seen_at` and `markHomeworkSeen`.

### D11 — Student profile entry point

- **Decision**: `StudentProfileResponse` gains `unitActivities`: each activity of the
  student's assigned units with unit subject, page, title, status, score, submission id and
  unseen flag. The **Presentaciones** box (spec 053) lists them grouped by unit when
  expanded, and a row opens the same `ActivityReviewDialog`. The box shows a dot when any
  of them is unseen.
- **Rationale**: FR-011 asks for a second path from the student's profile. It reuses the
  box that already concerns presentations instead of adding a sixth box.

### D12 — Removals

- **Back-end**:
  - delete `ActivityInstructionsFileStore`, `ActivityInstructionsProperties` (and its
    `@EnableConfigurationProperties` entry and config key), `ActivityInstructionsFile`,
    `YoutubeUrls` (only `ActivityAdminService` uses it) and `ActivityReorderInvalidException`
  - delete the endpoints `GET /admin/activities` (list), `POST /admin/activities`,
    `PUT /admin/presentations/{id}/activities/reorder`,
    `GET /learning/activities/{id}/instructions` and `PUT /admin/units/{id}/presentations`
  - remove the activity-trigger code from `PresentationService.removeFile` and `delete`
- **Front-end**:
  - delete `components/admin/activities/*`, except `ActivityReviewDialog` (moved)
  - delete routes `panel_.actividades.nueva.tsx` and `panel_.actividades.$activityId.tsx`,
    and the `activities` tab in `AdminPanel`
  - delete `activityImageUrl`, and the presentation-picking code in `UnitContentPicker` (that file is replaced)
- **Rationale**: FR-007, and the constitution's "no dead code" rule.

### D13 — Guarding the Presentations tab against unit-owned rows

- **Decision**: `PresentationRepository.list*` for admin filters on `unit_id IS NULL`.
  `PresentationService` operations (`get`, `rename`, `delete`, `uploadFile`, `removeFile`,
  `setShares`, `setLevel`) treat a unit-owned id as not found (404). Unit-owned files are
  managed only through `/admin/units/{id}/pdf`. Shares can only be set on standalone
  presentations, so a unit PDF's access is always the unit assignment.
- **Rationale**: FR-016. It also stops the teacher from attaching a second file or a PPTX
  to a unit PDF through the old API.
