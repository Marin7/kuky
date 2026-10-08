# Implementation Plan: Unit as a PDF with Page Activities

**Branch**: `055-unit-pdf-activities` | **Date**: 2026-10-07 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/055-unit-pdf-activities/spec.md`

## Summary

A unit now owns its PDF, and activities become pages of that PDF.

**Storage.** A unit's PDF is stored as a **unit-owned `presentations` row**
(`presentations.unit_id`, reinterpreted as ownership, at most one per unit, cascade on unit
delete) with exactly one PDF file. The page count is reported by pdf.js in the browser and
stored as `presentation_files.page_count`. This keeps everything that already keys on
presentation and file ids:
- student file access through `unit_assignments`
- the viewer route
- `SharedPresentationSummary.activities`
- the mixed unit sequence (`unit_position`)
- cascade deletes

**Activities.** An activity is now identified by `(presentation_id, page)`. It keeps its
title (optional), questions, submissions, grading and review exactly as today. Trigger file
and page, instruction text/YouTube/image, position, level and homework type all go away,
and the instructions-file table is dropped.

**Teacher UI.** A new route `/panel/unidades/$unitId` does everything in one view: upload
or replace the PDF, a page thumbnail strip, mark or unmark the selected page, edit the
page's questions with the existing `QuestionListEditor`, order the PDF and homeworks,
assign students, and see per-activity progress and reviews. The **Actividades** tab and its
routes are removed. The Presentations tab only lists standalone decks.

**Unseen indicators.** Activity submissions gain a teacher-unseen flag, shown as a dot on
the **Unidades** tab, the unit card, the unit page, and the student profile's
**Presentaciones** box.

**Student UI.** Almost unchanged. The activity block sits after its page in the viewer. The
unit lists its activities in page order. The stand-alone activity route renders the page as
the instructions.

**Data.** One migration, `V28`, discards existing activities and unit links, keeping the
detached decks as standalone presentations.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3.5) · TypeScript 5 strict (React 19)

**Primary Dependencies**: Spring Web/Security/JDBC (`NamedParameterJdbcTemplate`), Flyway 11, Jackson · TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next, `pdfjs-dist` (already installed), `@dnd-kit` (already used by `UnitContentSortableList`)

**Storage**: PostgreSQL 18 — reshape `activities`, drop `activity_instructions_files`, add `presentation_files.page_count`, `activity_submissions.teacher_seen_at`, change the `presentations.unit_id` FK to CASCADE plus a partial unique index. PDF bytes are stored on disk through the existing `PresentationFileStore`.

**Testing**: `./gradlew test` (JUnit + Mockito service tests; integration tests run in CI against Postgres), `npm run lint`, `npm run build`, and browser verification per [quickstart.md](./quickstart.md)

**Target Platform**: Web (SSR); desktop and phone-width browsers

**Project Type**: Web application (`back-end/` + `front-end/`)

**Performance Goals**: Thumbnail strip renders lazily (only visible thumbnails rendered at low scale) so a 100-page PDF stays responsive; unit detail is one request + one PDF fetch

**Constraints**:
- PDF only, at most 50 MB (existing limit), at most 500 pages
- at most one activity per page
- answer keys hidden before submit (unchanged)
- no horizontal page scroll at 375 px
- labels in es/en/ro
- client-side PDF code stays out of SSR (`useEffect` / dynamic import, as in `PresentationPdfViewer`)

**Scale/Scope**:
- tens of units, under 100 pages per PDF, a handful of activities per unit, under 20 students
- 1 migration
- back-end: about 12 files changed, 4 deleted, 2–3 new
- front-end: about 15 files changed, 6 deleted, 5 new
- 3 locale files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment |
|---|---|
| I. Simplicity First | ✅ This reuses `presentations`, `presentation_files`, `PresentationFileStore`, the access gate, the viewer, the question editor, grading and review instead of adding a parallel "unit PDF" stack. There is no PDF library on the back-end (the page count comes from pdf.js, which the browser already loads). No new abstractions beyond one small `UnitPdfService`. |
| II. Component-Driven UI | ✅ New named components: `UnitEditorPage`, `UnitPdfUploader`, `PdfThumbnailStrip`, `PageActivityEditor`, `UnitActivityProgress`, and `PdfSinglePage` (extracted from the viewer for reuse). Tailwind + Shadcn only. |
| III. Evolution-Ready Architecture | ✅ API calls stay in `lib/admin.ts` / `lib/learning.ts`. PDF loading lives in a shared `lib/pdf.ts` hook, not inside components. |
| Workflow: browser verification | ✅ The quickstart has 21 browser scenarios, including mobile width. |
| Workflow: no dead code | ✅ The Actividades tab, routes, instructions-file store, YouTube/image activity media, reorder endpoint and presentation-to-unit linking are all deleted, not hidden (research D12). |

**Post-design re-check**: ✅ No violations. Reinterpreting `presentations.unit_id` as
ownership is the simplest model that keeps the student pipeline intact (research D1); the
rejected alternatives are documented there.

## Project Structure

### Documentation (this feature)

```text
specs/055-unit-pdf-activities/
├── plan.md              # This file
├── research.md          # Phase 0 decisions D1–D13
├── data-model.md        # Schema changes, validation, state transitions
├── quickstart.md        # Automated + browser validation scenarios
├── contracts/
│   └── api.md           # Changed / new / removed endpoints and DTOs
└── tasks.md             # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/
├── migration/V28__unit_owned_pdf_activities.sql      # NEW (data-model.md outline)
└── dev/full_seed.sql                                 # presentations standalone, no seeded activities

back-end/src/main/java/com/kuky/backend/
├── units/
│   ├── controller/UnitAdminController.java           # + PUT/GET /{id}/pdf, POST /{id}/activities, GET /{id}/activity-progress; − PUT /{id}/presentations
│   ├── service/UnitService.java                      # detail(): pdf + activities summary; PDF/HOMEWORK content types; sync owned presentation title/level on update; − setPresentations
│   ├── service/UnitPdfService.java                   # NEW: upload/replace (validation, out-of-range guard, write-then-swap, after-commit disk delete)
│   ├── repository/UnitRepository.java                # owned-presentation queries; content members PDF/HOMEWORK; activity summaries & progress; − setPresentations
│   ├── dto/{UnitDetail,UnitSummary,UnitContentItem}.java  # reshaped per contracts/api.md
│   ├── dto/{UnitPdfInfo,UnitActivitySummary,UnitActivityProgressRow,CreatePageActivityRequest}.java  # NEW
│   ├── dto/SetUnitPresentationsRequest.java          # DELETE
│   └── exception/ActivitiesOutOfRangeException.java  # NEW → 409 ACTIVITIES_OUT_OF_RANGE
├── admin/
│   ├── controller/ActivityAdminController.java       # − list, − POST, − reorder; DELETE ?deleteSubmissions; queue ?unitId
│   ├── service/ActivityAdminService.java             # createOnPage(unitId,page,…), update(title,questions), delete guard; mark seen on open; − media/trigger/instructions code
│   ├── service/StudentProfileAdminService.java       # + unitActivities
│   ├── dto/{ActivityAdminDetail,SaveActivityRequest,HomeworkReviewQueueItemDto*,StudentProfileResponse}.java  # reshaped (*queue item for activities: + activityId/unitId/page — new ActivityReviewQueueItemDto)
│   ├── dto/ActivityAdminItem.java, ReorderActivitiesRequest.java   # DELETE
├── learning/
│   ├── model/Activity.java                           # page; − trigger/media/position/level/type
│   ├── model/ActivityInstructionsFile.java           # DELETE
│   ├── repository/ActivityRepository.java            # page-based CRUD, findByPresentationAndPage, deleteBeyondPage, countSubmissions; − instructions/trigger/reorder
│   ├── repository/ActivitySubmissionRepository.java  # teacher_seen_at on upserts; queue by unit; progress rows
│   ├── service/ActivityStudentService.java           # summaries by page; item response + presentationId/fileId/page/unitId; − instructions
│   ├── service/ActivityInstructionsFileStore.java    # DELETE
│   ├── util/YoutubeUrls.java                         # DELETE (only used by activities)
│   ├── controller/LearningController.java            # − GET /activities/{id}/instructions
│   ├── dto/{ActivitySummary,ActivityItemResponse}.java  # reshaped
│   └── exception/ActivityReorderInvalidException.java   # DELETE; + ActivityPageTakenException, ActivityHasSubmissionsException
├── presentations/
│   ├── repository/PresentationRepository.java        # admin lists filter unit_id IS NULL; findOwnedByUnit; insert owned; page_count
│   └── service/PresentationService.java              # 404 on unit-owned ids; − activity trigger/instructions cleanup
├── notification/{service/NotificationService,repository/NotificationRepository,dto/BadgeSummary}.java  # + units flag, markActivitySeen
├── config/GlobalExceptionHandler.java                # + 3 codes, − ACTIVITY_REORDER_INVALID
├── config/ActivityInstructionsProperties.java        # DELETE (+ BackEndApplication registration, config key)

back-end/src/test/java/com/kuky/backend/
├── units/UnitPdfServiceTest.java                     # NEW
├── admin/ActivityAdminServiceTest.java               # NEW (page marking, guards, seen)
├── presentations/PresentationServiceTest.java        # unit-owned → 404
└── notification/NotificationServiceTest.java         # units badge

front-end/src/
├── lib/
│   ├── pdf.ts                                        # NEW: usePdfDocument(blobLoader) + page-count helper (client-only)
│   ├── admin.ts                                      # unit PDF upload/get, page activities, progress, queue ?unitId; − activity list/create/reorder, − setUnitPresentations
│   ├── learning.ts                                   # ActivitySummary/ActivityItem reshaped; − instructions helpers
│   ├── notifications.ts                              # BadgeSummary.units
│   └── youtube.ts                                    # − activityImageUrl
├── routes/
│   ├── panel_.unidades.$unitId.tsx                   # NEW (admin-gated like panel_.tareas.$homeworkId)
│   ├── panel_.actividades.nueva.tsx                  # DELETE
│   └── panel_.actividades.$activityId.tsx            # DELETE
├── components/admin/
│   ├── AdminPanel.tsx                                # − activities tab; units tab dot
│   ├── activities/                                   # DELETE folder (ActivityReviewDialog moves to units/)
│   ├── units/UnitEditorPage.tsx                      # NEW: header, PDF, strip, page editor, contents order, progress
│   ├── units/UnitPdfUploader.tsx                     # NEW: pick → count pages → confirm out-of-range → upload
│   ├── units/PdfThumbnailStrip.tsx                   # NEW: lazy thumbnails with activity badge / unseen dot
│   ├── units/PageActivityEditor.tsx                  # NEW: mark/unmark, title, QuestionListEditor, save
│   ├── units/UnitActivityProgress.tsx                # NEW: activity × student status, opens review
│   ├── units/ActivityReviewDialog.tsx                # MOVED from activities/
│   ├── units/UnitCard.tsx                            # compact summary + link to editor, dot
│   ├── units/UnitContentSortableList.tsx             # PDF item instead of presentation items
│   ├── units/AddContentCombobox.tsx                  # homeworks only
│   ├── units/UnitContentPicker.tsx                   # DELETE (folded into UnitEditorPage)
│   ├── presentations/PresentationAdminList.tsx       # drop any unit/activity references
│   └── students/StudentPresentationsPanel.tsx        # + unit activities with status, opens ActivityReviewDialog; box dot
├── components/learning/
│   ├── PdfSinglePage.tsx                             # NEW: extracted PdfPage renderer (reused by admin + activity route)
│   ├── PresentationPdfViewerClient.tsx               # activitiesByPage keyed by activity.page
│   ├── ActivityViewerPrompts.tsx                     # no file filter
│   ├── ActivityPageSlot.tsx                          # − YouTube/image/text block; title fallback
│   ├── ActivityPanel.tsx                             # title fallback
│   ├── UnitDetailContent.tsx                         # activity list (page order, status, link) above viewer
│   └── routes/aprendizaje_.actividad.$activityId.tsx # renders the page as instructions via PdfSinglePage
└── i18n/locales/{es,en,ro}.ts                        # unit editor, page activity, confirmations, fallbacks; − activities tab strings

CLAUDE.md                                             # update Units/activities notes, /panel row, remove Actividades tab mention
```

**Structure Decision**: The existing web-app layout (`back-end/`, `front-end/`). Unit PDF
and page-activity endpoints live in the `units` package, because the unit is the aggregate
the teacher edits. Grading and review stay in `admin/ActivityAdminService` and
`learning/ActivityStudentService`, unchanged in behaviour. Shared PDF rendering is
extracted once (`PdfSinglePage`, `lib/pdf.ts`) and reused by the admin editor, the student
viewer and the activity route.

## Implementation Notes

- **Transactions and files.** `UnitPdfService.upload` is `@Transactional`. It writes the new
  bytes under a fresh file id before the database change. If the database step fails, it
  deletes the new file. It removes the old file id from disk only after commit
  (`TransactionSynchronization.afterCommit`), the same pattern the homework email uses.
  `UnitService.delete` collects the owned file id before deleting and removes it from disk
  after commit.
- **Mixed sequence.** `ContentMember.type` becomes `PDF` or `HOMEWORK`. The PDF member is
  the owned presentation's `unit_position`. On first upload, append it at
  `max(unit_position) + 1`.
- **Student overview.** `findSharedSummariesForUser` already returns unit-linked
  presentations with their unit ref and position. It now returns only owned ones (unit
  presentations) plus shared standalone ones. No query change is needed beyond what the
  migration does.
- **Title fallback.** Store NULL for a blank title. The client renders
  `t("learning.activities.pageTitle", { page })`. The server builds "Actividad – página N"
  only for the cross-unit review queue's `assignmentTitle`, because the admin is in Spanish.
- **Thumbnails.** Render each thumbnail on demand with `IntersectionObserver` at about
  120 px width, reusing one `PDFDocumentProxy` per PDF. The large page preview reuses
  `PdfSinglePage`.
- **CLAUDE.md.** Replace the Units/Presentations/activities description: units own a PDF,
  activities are pages, there is no Actividades tab, and the Presentations tab holds
  standalone decks only. Update the `/panel` and `/aprendizaje` rows in Current pages.

## Complexity Tracking

No constitution violations to justify.
