---
description: "Task list for 055-unit-pdf-activities"
---

# Tasks: Unit as a PDF with Page Activities

**Input**: Design documents from `specs/055-unit-pdf-activities/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md) (decisions D1–D13), [data-model.md](./data-model.md), [contracts/api.md](./contracts/api.md), [quickstart.md](./quickstart.md)

**Tests**: The plan and quickstart require targeted JUnit service tests (Mockito style, like `back-end/src/test/java/com/kuky/backend/presentations/PresentationServiceTest.java`); those are included. The browser verification follows quickstart.md.

**Organization**: The phases follow the spec's user stories. US1–US3 are P1 and US4 is P2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US4 from spec.md
- Paths are relative to the repo root (`back-end/`, `front-end/`). Java package root is `back-end/src/main/java/com/kuky/backend/`, written below as `BE/`. Front-end source root is `front-end/src/`, written as `FE/`.

---

## Phase 1: Setup (Schema)

**Purpose**: Ship the one migration and keep the dev seed loadable.

- [X] T001 Create `back-end/src/main/resources/db/migration/V28__unit_owned_pdf_activities.sql` exactly per the "Migration outline" in data-model.md:
  1. `DELETE FROM activities`
  2. `DROP TABLE activity_instructions_files`
  3. Detach presentations from units (`unit_id = NULL, unit_position = 0`).
  4. Drop and re-add the `presentations.unit_id` FK to `units(id)` `ON DELETE CASCADE`. Look up the existing constraint name in `V1__baseline.sql` or use `presentations_unit_id_fkey`.
  5. Add `CREATE UNIQUE INDEX presentations_unit_owned_unique ON presentations (unit_id) WHERE unit_id IS NOT NULL`.
  6. Add `presentation_files.page_count INT` with `CHECK (page_count IS NULL OR page_count BETWEEN 1 AND 500)`.
  7. On `activities`, drop:
     - the indexes `activities_presentation_position_idx`, `activities_trigger_file_idx` and `activities_image_idx`
     - the constraints `activities_trigger_pair_check` and `activities_trigger_page_check`
     - the columns `trigger_file_id`, `trigger_page`, `position`, `level`, `homework_type`, `instructions_text`, `youtube_url` and `image_id`
  8. Then, on `activities`:
     - add `page INT NOT NULL` with `CONSTRAINT activities_page_check CHECK (page >= 1)`
     - `ALTER COLUMN title DROP NOT NULL`
     - add `CONSTRAINT activities_presentation_page_unique UNIQUE (presentation_id, page)`
  9. Add `activity_submissions.teacher_seen_at TIMESTAMPTZ` and the partial index `activity_submissions_teacher_unseen_idx ON activity_submissions (activity_id) WHERE status IN ('SUBMITTED','REVIEWED','GRADED') AND teacher_seen_at IS NULL`.
- [X] T002 [P] Update `back-end/src/main/resources/db/dev/full_seed.sql`: insert the two seeded `presentations` rows (around lines 52–54) with `unit_id` NULL, and remove any seeded `activities`, `activity_*` or `activity_instructions_files` inserts.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Reshape the activity model, remove the dead code paths (research D12), guard the Presentations API (D13) and extract the shared PDF rendering, so that both projects compile and every story can build on them.

**⚠️ CRITICAL**: No user-story work starts until this phase builds (`./gradlew build -x test`, `npm run build`).

### Back-end

- [X] T003 Reshape `BE/learning/model/Activity.java` to fields `id, presentationId, page (int), title (nullable), format, createdAt, updatedAt`. Remove `level`, `homeworkType`, `position`, `triggerFileId`, `triggerPage`, `instructionsText`, `youtubeUrl` and `imageId`.
- [X] T004 Rewrite `BE/learning/repository/ActivityRepository.java` for the new columns:
  - `ACTIVITY_MAPPER` reads `page`.
  - `insert` / `update` cover only `title`, `format` and `page`, plus `updated_at` on update.
  - Add these methods:
    - `findByPresentationAndPage(UUID presentationId, int page)`
    - `listByPresentationId` (ordered by `page`)
    - `countSubmissions(UUID activityId)`
    - `findIdsBeyondPage(UUID presentationId, int maxPage)`
    - `deleteBeyondPage(UUID presentationId, int maxPage)`
  - Remove `ActivityListRow`, `listAll`, `maxPosition`, `reorderPositions`, `findInstructionFileIdsByPresentationId`, `clearTriggerForFile`, `clearTriggerOnActivity`, the instructions insert/find/delete methods, `INSTRUCTIONS_MAPPER` and `fileBelongsToPresentation`.
- [X] T005 [P] Delete `BE/learning/model/ActivityInstructionsFile.java`, `BE/learning/service/ActivityInstructionsFileStore.java`, `BE/config/ActivityInstructionsProperties.java`, `BE/learning/util/YoutubeUrls.java` and `BE/learning/exception/ActivityReorderInvalidException.java`. Remove `ActivityInstructionsProperties` from `@EnableConfigurationProperties` in `BE/BackEndApplication.java`, and remove its `app.activity-instructions.*` keys from `back-end/src/main/resources/application*.properties` (wherever present).
- [X] T006 [P] Add the new exceptions, each a `RuntimeException` with a message constructor:
  - `BE/learning/exception/ActivityPageTakenException.java`
  - `BE/learning/exception/ActivityHasSubmissionsException.java`
  - `BE/units/exception/ActivitiesOutOfRangeException.java`

  In `BE/config/GlobalExceptionHandler.java`, map each to 409 with codes `ACTIVITY_PAGE_TAKEN`, `ACTIVITY_HAS_SUBMISSIONS` and `ACTIVITIES_OUT_OF_RANGE`, and delete the `ActivityReorderInvalidException` handler.
- [X] T007 Reshape the learning DTOs per contracts/api.md:
  - `BE/learning/dto/ActivitySummary.java` becomes `(id, title, page, format, status, scorePercent)`.
  - `BE/learning/dto/ActivityItemResponse.java`: remove `level`, `homeworkType`, `triggerFileId`, `triggerPage`, `instructionsText`, `youtubeUrl` and `imageId`; add `presentationId`, `fileId`, `page` and `unitId`.
- [X] T008 Update `BE/learning/service/ActivityStudentService.java`:
  - `summariesForPresentations` builds the new `ActivitySummary`, ordered by page.
  - `toItemResponse` fills `presentationId`, `page`, `fileId` (the presentation's single file, via `PresentationRepository.listFileIds`) and `unitId` (the presentation's `unit_id`).
  - Delete the `InstructionsPdf` record and `getInstructions`. Remove the `ActivityInstructionsFileStore` dependency.
  - Keep submit and grade logic unchanged.
- [X] T009 Delete the `GET /activities/{id}/instructions` handler from `BE/learning/controller/LearningController.java`.
- [X] T010 Strip the media, trigger, reorder and list code from `BE/admin/service/ActivityAdminService.java`:
  - remove `list`, `reorder`, `requirePageTrigger`, `requireInstructionsText`, `normalizeYoutubeUrl`, `requireMedia` and `toItem`, plus every `ActivityInstructionsFileStore`, `YoutubeUrls` and image reference
  - make `toDetail` produce the new `ActivityAdminDetail` (T011)
  - keep the review, feedback and exercise-result methods unchanged

  Leave `create` / `update` / `delete` compiling against the new `Activity` for now; they are rewritten in US2 (T033).
- [X] T011 [P] Reshape `BE/admin/dto/ActivityAdminDetail.java` to `(id, unitId, presentationId, page, title, format, composition, questions, createdAt, updatedAt)`, and `BE/admin/dto/SaveActivityRequest.java` to `(String title, List<HomeworkQuestionDto> questions)`. Delete `BE/admin/dto/ActivityAdminItem.java` and `BE/admin/dto/ReorderActivitiesRequest.java`.
- [X] T012 Update `BE/admin/controller/ActivityAdminController.java`: remove `GET /activities` (list), `POST /activities` and `PUT /presentations/{presentationId}/activities/reorder`. Keep `GET` / `PUT` / `DELETE /activities/{id}` and every `/activities/submissions/**` route.
- [X] T013 Guard standalone presentations (research D13) in `BE/presentations/repository/PresentationRepository.java`:
  - Add `AND p.unit_id IS NULL` to the admin list and summary queries.
  - Add these methods:
    - `findOwnedByUnit(UUID unitId)`, returning the id, file id, original name, byte size and page count
    - `insertOwned(UUID unitId, String title, String level, int unitPosition)`
    - `updateOwnedTitleLevel(UUID unitId, String title, String level)`
    - `isUnitOwned(UUID presentationId)`
  - Add `page_count` support to `insertFile`, via an overload with an `Integer pageCount`.

  Keep `isSharedWith` / `findSharedSummariesForUser` as they are: they already cover unit-assigned access.
- [X] T014 Update `BE/presentations/service/PresentationService.java`:
  - make `requirePresentation` throw `PresentationNotFoundException` when `isUnitOwned(id)`, so every by-id operation (`get`, `rename`, `delete`, `uploadFile`, `removeFile`, `getFileData`, `setLevel`, `setShares`) returns 404 for unit-owned ids
  - delete the `activityRepository.clearTriggerForFile` call and the instruction-file cleanup in `delete`, and drop the now-unused dependencies
- [X] T015 [P] Remove `PresentationSummary`/`PresentationDetail` fields or helpers that exist only for unit linking or activities, if any, in `BE/admin/dto/PresentationSummary.java` and `BE/admin/dto/PresentationDetail.java`. Fix the compile fallout in `BE/units/service/UnitService.java` (temporarily keep `detail()` compiling; it is rewritten in T024).

### Front-end

- [X] T016 [P] Create `FE/lib/pdf.ts` (client-only; import pdfjs dynamically or only from client modules):
  - `usePdfDocument(load: () => Promise<Blob | ArrayBuffer> | null, deps)`, returning `{status: "loading"|"ready"|"error", pdf}` and destroying the loading task on cleanup
  - `countPdfPages(file: File): Promise<number>`
  - `isPdfFile(file: File): boolean`

  Set `GlobalWorkerOptions.workerSrc` once here, as `FE/components/learning/PresentationPdfViewerClient.tsx` does today.
- [X] T017 [P] Extract the `PdfPage` component from `FE/components/learning/PresentationPdfViewerClient.tsx` into `FE/components/learning/PdfSinglePage.tsx` (export `PdfSinglePage` with props `pdf`, `pageNumber`, `width`, `onPageVisible?`, `textLayer?: boolean` (default true)), and import it back into the viewer. Behavior must stay identical.
- [X] T018 Update the front-end API types and functions per contracts/api.md:
  - `FE/lib/learning.ts`:
    - `ActivitySummary` becomes `{id, title: string|null, page, format, status, scorePercent}`
    - `ActivityItem`: remove the trigger/media/level/type fields; add `presentationId`, `fileId`, `page` and `unitId`
    - delete the instructions-URL helpers
  - `FE/lib/admin.ts`:
    - delete `ActivityAdminItem`, `ActivityInstructionsMeta`, `listActivities`, `createActivity` and `reorderActivities`
    - `ActivityWriteFields` becomes `{title, questions}`
    - reshape `ActivityAdminDetail`
    - delete `setUnitPresentations`
  - `FE/lib/youtube.ts`: delete `activityImageUrl`.
- [X] T019 Remove the Actividades tab (FR-007):
  - Delete `FE/components/admin/activities/ActivitiesTab.tsx`, `ActivityAdminList.tsx`, `ActivityEditorPage.tsx` and `ActivityReviewQueue.tsx`.
  - `git mv` `FE/components/admin/activities/ActivityReviewDialog.tsx` to `FE/components/admin/units/ActivityReviewDialog.tsx` and fix its imports.
  - Delete `FE/routes/panel_.actividades.nueva.tsx` and `FE/routes/panel_.actividades.$activityId.tsx`.
  - Remove the `activities` `TabsTrigger` / `TabsContent` and its import from `FE/components/admin/AdminPanel.tsx`.
  - Fix the compile fallout in `FE/components/learning/ActivityPageSlot.tsx`: remove the YouTube, image and instructions-text block and the `youtube` imports.
  - Fix `FE/components/learning/ActivityViewerPrompts.tsx`: drop the `triggerFileId` filter and use `a.page`.
  - Fix `FE/components/learning/PresentationPdfViewerClient.tsx`: key `activitiesByPage` on `a.page` and sort by page.
  - Run `npm run build` to regenerate `routeTree.gen.ts`, and never edit that file by hand.

**Checkpoint**: Both projects build. The Actividades tab is gone. The Presentations tab still works for standalone decks.

---

## Phase 3: User Story 1 — Teacher builds a unit from a single PDF (Priority: P1) 🎯 MVP

**Goal**: Create a unit, then upload, view and replace its single PDF from a dedicated unit view, and order the PDF together with the unit's homeworks.

**Independent Test**: quickstart scenarios 1, 2, 3, 7, 8 and 20 (activities are not needed yet). Create a unit, upload a 10-page PDF, reload, add and reorder a homework, assign a student, and check the student can open the PDF.

### Tests for User Story 1

- [X] T020 [P] [US1] Create `back-end/src/test/java/com/kuky/backend/units/UnitPdfServiceTest.java` (Mockito). It covers:
  - rejecting empty, non-PDF (bad name, bad content type, missing `%PDF-` header), over-50 MB, and `pageCount` 0 or 501
  - the first upload creating the owned presentation via `insertOwned` at `max(unit_position)+1`
  - a replacement with `pageCount` ≥ max activity page keeping all activities
  - a replacement below max page without the flag throwing `ActivitiesOutOfRangeException` and writing nothing
  - a replacement with the flag calling `deleteBeyondPage`
  - the old file id being deleted only after commit

### Implementation for User Story 1

- [X] T021 [US1] Reshape the unit DTOs per contracts/api.md:
  - `BE/units/dto/UnitSummary.java` becomes `(id, level, subject, position, hasPdf, pageCount, activityCount, homeworkCount, assignedStudentIds, hasUnseenActivitySubmissions)`.
  - `BE/units/dto/UnitDetail.java` becomes `(id, level, subject, position, UnitPdfInfo pdf, List<UnitContentItem> contents, List<UnitActivitySummary> activities, List<StudentResponse> assignedStudents)`.
  - `BE/units/dto/UnitContentItem.java` becomes `(type, unitPosition, HomeworkAdminItem homework)` with constants `PDF` and `HOMEWORK`.
  - New `BE/units/dto/UnitPdfInfo.java` `(presentationId, fileId, originalName, byteSize, pageCount)`.
  - New `BE/units/dto/UnitActivitySummary.java` `(id, page, title, format, composition, questionCount, submittedCount, awaitingCorrectionCount, gradedCount, hasUnseenSubmissions)`.
  - Delete `BE/units/dto/SetUnitPresentationsRequest.java`.
- [X] T022 [US1] Update `BE/units/repository/UnitRepository.java`:
  - `listSummaries` computes `hasPdf`, `pageCount`, `activityCount`, `homeworkCount` and `hasUnseenActivitySubmissions` (join the owned presentation, its file, activities and unseen activity submissions).
  - `findContentMembers` returns the owned presentation as type `PDF` plus homeworks as `HOMEWORK`, ordered by `unit_position`.
  - `reorderContents` writes `presentations.unit_position` for `PDF` and `homework_assignments.unit_position` for `HOMEWORK`.
  - Add `findActivitySummaries(UUID unitId)`, which counts submissions by status and unseen across the activities of the owned presentation.
  - Remove `setPresentations` and `findPresentations`.
- [X] T023 [US1] Create `BE/units/service/UnitPdfService.java` (`@Service @Transactional`) with:
  - `upload(UUID unitId, MultipartFile file, Integer pageCount, boolean removeOutOfRangeActivities)`:
    1. `requireUnit`
    2. Validate per contracts/api.md, throwing `IllegalArgumentException` → 422. Validation covers the 50 MB limit, `.pdf`/`application/pdf`, the first 5 bytes `%PDF-`, and `pageCount` 1..500.
    3. Look up `findOwnedByUnit`. If activities exist with `page > pageCount` and the flag is false, throw `ActivitiesOutOfRangeException`, naming the count in Spanish.
    4. Write the new bytes via `PresentationFileStore.write(newFileId)`.
    5. If there is no owned presentation, `insertOwned(unit subject, level, nextPosition)`.
    6. Insert the file row with `page_count`, delete the old file row, and `deleteBeyondPage` when the flag is set.
    7. Register `TransactionSynchronization.afterCommit` to `deleteQuietly(oldFileId)`, and `afterCompletion(ROLLED_BACK)` to delete `newFileId`.
    8. Return `UnitService.get(unitId)`.
  - `PresentationFile download(UUID unitId)`: the bytes plus metadata of the owned file, or `PresentationNotFoundException`.
- [X] T024 [US1] Update `BE/units/service/UnitService.java`:
  - `detail()` builds `UnitPdfInfo` from `findOwnedByUnit`, contents from the `PDF`/`HOMEWORK` members, `activities` from `findActivitySummaries` (composition via `HomeworkCompositionSupport` from the activity questions) and the assigned students.
  - `normalizeType` accepts `PDF` and `HOMEWORK`.
  - `update` also calls `presentationRepository.updateOwnedTitleLevel`.
  - `delete` collects the owned file id before deleting and removes it from disk after commit.
  - Delete `setPresentations`.
  - Keep `setHomeworks` / `setAssignees` unchanged.
- [X] T025 [US1] Update `BE/units/controller/UnitAdminController.java`:
  - add `PUT /{id}/pdf` (multipart: `file`, `pageCount`, `removeOutOfRangeActivities` default false) → `UnitPdfService.upload`
  - add `GET /{id}/pdf` → inline `application/pdf` bytes, with the same headers pattern as `PresentationAdminController.downloadFile`
  - remove `PUT /{id}/presentations`
- [X] T026 [P] [US1] Update the unit API in `FE/lib/admin.ts` per contracts/api.md:
  - `UnitSummary`, `UnitDetail`, `UnitPdfInfo`, `UnitActivitySummary`, and `UnitContentItem` (`type: "PDF"|"HOMEWORK"`)
  - `uploadUnitPdf(unitId, file, pageCount, removeOutOfRangeActivities)` (FormData, `credentials: 'include'`, maps 409 to `ApiError.error`)
  - `fetchUnitPdfBlob(unitId)`
  - `reorderUnitContents` items `{type: "PDF"|"HOMEWORK", id}`
- [X] T027 [P] [US1] Create `FE/components/admin/units/PdfThumbnailStrip.tsx`:
  - a horizontally scrollable strip (`overflow-x-auto`, never page-level scroll) of page thumbnails about 120 px wide
  - each thumbnail renders lazily with `IntersectionObserver` + `PdfSinglePage` (`textLayer={false}`)
  - props: `pdf`, `pageCount`, `selectedPage`, `onSelect(page)`, `activityPages: Set<number>` (badge) and `unseenPages: Set<number>` (`NotificationDot`)
- [X] T028 [P] [US1] Create `FE/components/admin/units/UnitPdfUploader.tsx`, a file input (accept `application/pdf`) that does this on pick:
  1. Reject non-PDF files with an i18n error.
  2. Run `countPdfPages`.
  3. Compute how many existing activities have `page > count`. If any, `window.confirm` a message stating the count, and abort on cancel.
  4. Call `uploadUnitPdf`, then `onUploaded(detail)`.

  It shows "Subir PDF" or "Reemplazar PDF" and a busy state.
- [X] T029 [US1] Create `FE/components/admin/units/UnitEditorPage.tsx` and the route `FE/routes/panel_.unidades.$unitId.tsx`. The route is admin-gated with the same pattern as `FE/routes/panel_.tareas.$homeworkId.tsx` (getMe in `useEffect`, redirect non-admins).

  The page loads `getUnit` and renders these sections:
  - **Header**: back to the Panel's Units tab; level `Select` + subject `Input` + save (`updateUnit`); delete (`deleteUnit`, confirm, navigate back); assign students (`UnitAssignDialog`).
  - **PDF section**: `UnitPdfUploader`, PDF metadata, and once a PDF exists `usePdfDocument(() => fetchUnitPdfBlob(id))` → `PdfThumbnailStrip` + a large `PdfSinglePage` for `selectedPage`, measured to container width.
  - **Contents**: `UnitContentSortableList` + `AddContentCombobox`, saving with `setUnitHomeworks` / `reorderUnitContents`.

  Leave a placeholder region beside or below the large page for the US2 page editor. Stack the layout vertically under `md`.
- [X] T030 [US1] Rework the Units tab and content list:
  - `FE/components/admin/units/UnitContentSortableList.tsx`: render a single non-removable "PDF de la unidad" item for `type === "PDF"` instead of presentation rows.
  - `FE/components/admin/units/AddContentCombobox.tsx`: offer homeworks only.
  - `FE/components/admin/units/UnitCard.tsx`: compact summary (level, subject, `hasPdf` ? `${pageCount} págs.` : "Sin PDF", activity and homework counts, assigned students) with a link to `/panel/unidades/$unitId` (Link from `@tanstack/react-router`); remove the inline expand that used `UnitContentPicker`.
  - `FE/components/admin/units/UnitsTab.tsx`: after `createUnit`, navigate to the new unit's editor.
  - Delete `FE/components/admin/units/UnitContentPicker.tsx`.
- [X] T031 [P] [US1] Add the US1 i18n keys (unit editor header, PDF upload/replace, errors, the "Sin PDF" / page-count labels, the PDF content item, the out-of-range confirm with `{{count}}`) to `FE/i18n/locales/es.ts`, `en.ts` and `ro.ts`, under `admin.units.*`. Remove the keys used only by `UnitContentPicker` and presentation linking.

**Checkpoint**: A unit can be created, given a PDF, reordered with homework and assigned. The student sees the PDF in the unit with no activities.

---

## Phase 4: User Story 2 — Teacher marks PDF pages as activities (Priority: P1)

**Goal**: In the unit view, mark or unmark the selected page as an activity and author its questions with the existing editor.

**Independent Test**: quickstart scenarios 4, 5, 6, 9, 18 and 19.

### Tests for User Story 2

- [X] T032 [P] [US2] Create `back-end/src/test/java/com/kuky/backend/admin/ActivityAdminServiceTest.java` (Mockito). It covers:
  - `createOnPage` rejecting a unit without a PDF, page 0, a page greater than `pageCount`, and empty questions (`ActivityValidationException`)
  - a page that is already taken throwing `ActivityPageTakenException`
  - a blank title being stored as null
  - `update` replacing the title and questions, with the page unchanged
  - `delete` with submissions and `deleteSubmissions=false` throwing `ActivityHasSubmissionsException`; with `true`, deleting

### Implementation for User Story 2

- [X] T033 [US2] Rewrite the create, update and delete methods in `BE/admin/service/ActivityAdminService.java`:
  - `createOnPage(UUID unitId, int page, String title, List<HomeworkQuestionDto> questions)`:
    - resolve the owned presentation via `PresentationRepository.findOwnedByUnit` (`UnitNotFoundException` if the unit is missing; `ActivityValidationException` "La unidad no tiene PDF." if there is no PDF)
    - validate `1 ≤ page ≤ pageCount` and non-empty questions, using the existing structure validation and `mapQuestions`
    - throw `ActivityPageTakenException` if `findByPresentationAndPage` finds one
    - derive `format` as today
    - insert the activity and its questions
  - `update(UUID id, SaveActivityRequest)`: replace the title and questions as today's update does.
  - `delete(UUID id, boolean deleteSubmissions)`: guard with `countSubmissions`.

  Remove the old `create(SaveActivityRequest)` and the long-parameter overloads.
- [X] T034 [US2] Wire the endpoints:
  - `POST /api/v1/admin/units/{unitId}/activities` in `BE/units/controller/UnitAdminController.java`, with body `BE/units/dto/CreatePageActivityRequest.java` `(@NotNull @Min(1) Integer page, String title, @NotNull List<HomeworkQuestionDto> questions)`, delegating to `ActivityAdminService.createOnPage` → 201 `ActivityAdminDetail`
  - in `BE/admin/controller/ActivityAdminController.java`, `DELETE /activities/{id}` gains `@RequestParam(defaultValue = "false") boolean deleteSubmissions`
- [X] T035 [P] [US2] Add the page-activity API to `FE/lib/admin.ts`: `createPageActivity(unitId, {page, title, questions})`, `updateActivity(id, {title, questions})`, `deleteActivity(id, deleteSubmissions = false)` (surface 409 codes as `ApiError.error`), and `getActivityAdmin(id)` (kept, new shape).
- [X] T036 [US2] Create `FE/components/admin/units/PageActivityEditor.tsx` with props `unitId`, `page`, `activity?: UnitActivitySummary`, `onChanged()`:
  - **Unmarked**: a "Marcar como actividad" button opens an empty form.
  - **Marked**: load `getActivityAdmin(activity.id)`. Show an optional title `Input` (placeholder `Actividad – página {{page}}`), the questions via `FE/components/admin/homework/QuestionListEditor.tsx` (same props usage as the deleted `ActivityEditorPage`; check git history `git show HEAD:front-end/src/components/admin/activities/ActivityEditorPage.tsx`), and Save (`createPageActivity` or `updateActivity`).
  - **Unmark button**: confirm the plain removal. On a 409 `ACTIVITY_HAS_SUBMISSIONS`, confirm "Se borrarán las respuestas de los alumnos" and retry with `deleteSubmissions=true`.
  - Show validation errors from the API message.
- [X] T037 [US2] Integrate into `FE/components/admin/units/UnitEditorPage.tsx`:
  - render `PageActivityEditor` for `selectedPage` beside or below the large page
  - pass `activityPages` from `detail.activities` to `PdfThumbnailStrip`
  - add an "Actividades" list (page · title-or-fallback · composition), where clicking selects that page
  - refetch `getUnit` on `onChanged`
- [X] T038 [P] [US2] Add the US2 i18n keys (mark/unmark, title placeholder `pageTitle` with `{{page}}`, save, the delete-with-submissions confirm, error mapping for `ACTIVITY_PAGE_TAKEN`, the activity list heading) to `FE/i18n/locales/es.ts`, `en.ts` and `ro.ts`. Delete the now-unused `admin.activities.*` keys of the removed tab, editor and list.

**Checkpoint**: The teacher can mark pages, author activities and unmark them; replacing the PDF prunes the out-of-range activities after confirmation.

---

## Phase 5: User Story 3 — Student works through a unit and completes its activities (Priority: P1)

**Goal**: Students see the unit PDF and homework in order, see the activity list by page, answer each activity inline after its page, or open the stand-alone activity page with that page as the instructions.

**Independent Test**: quickstart scenarios 11–14.

- [X] T039 [US3] Verify, in `BE/learning/service/LearningService.java`, that `sharedPresentations` contains the unit's owned presentation with its unit ref and `contentUnitPosition`, and only the new `ActivitySummary` shape. Adjust the mapping if T007/T008 changed the constructor arity. No query change is expected (research, plan "Student overview").
- [X] T040 [P] [US3] Use the title fallback in the student activity UI:
  - In `FE/components/learning/ActivityPageSlot.tsx` and `FE/components/learning/ActivityPanel.tsx`, display `activity.title ?? t("learning.activities.pageTitle", { page })`.
  - Keep the slot's status badge and collapsible `ActivityPanel` exactly as today.
  - Set `data-activity-after-page={activity.page}`.
- [X] T041 [US3] In `FE/components/learning/UnitDetailContent.tsx` `PresentationExpandBody`:
  - When `presentation.unit` is set, i.e. the unit PDF (single file), skip the multi-file list and render the activity list above the viewer: one row per activity in page order, "p. {page} · title-or-fallback · status badge (with % when GRADED)", linking to `/aprendizaje/actividad/$activityId`.
  - Show the viewer directly for that single PDF.
  - Show an empty state (`learning.units.noPdf`) when the unit has no PDF entry.
  - Keep the multi-file UI for standalone shared presentations.
- [X] T042 [US3] Update `FE/routes/aprendizaje_.actividad.$activityId.tsx`: render the page above the `ActivityPanel` as the instructions, via `usePdfDocument(() => fetchPresentationFileBlob(item.presentationId, item.fileId))` + `PdfSinglePage` at container width. On a load error, show `learning.presentations.loadError` while keeping the answer form usable. Remove any instructions-PDF/YouTube/image rendering.
- [X] T043 [P] [US3] Add the US3 i18n keys (`learning.activities.pageTitle`, the activity list heading, `learning.units.noPdf`, the page label) to `FE/i18n/locales/es.ts`, `en.ts` and `ro.ts`. Delete the student keys only used by the removed instructions or media.

**Checkpoint**: The end-to-end student flow works; self-correcting activities grade on submit and FREE_TEXT activities wait for the teacher.

---

## Phase 6: User Story 4 — Teacher grades and tracks activity work (Priority: P2)

**Goal**: Reach activity reviews from the unit view and the student profile, with unseen dots on the Panel, the Unidades tab, the unit card, the unit page and the profile box.

**Independent Test**: quickstart scenarios 15–17.

### Tests for User Story 4

- [X] T044 [P] [US4] Extend `back-end/src/test/java/com/kuky/backend/notification/NotificationServiceTest.java`: for an admin, `badges` returns `units=true` and `panel=true` when `hasUnseenActivitySubmissions()` is true, and `markActivitySeen` delegates to the repository.

### Implementation for User Story 4

- [X] T045 [US4] Make activity submissions start unseen and become seen when opened:
  - In `BE/learning/repository/ActivitySubmissionRepository.java`, `upsertManual` and `upsertGraded` set `teacher_seen_at = NULL` on every student submit.
  - In `BE/notification/repository/NotificationRepository.java`:
    - add `hasUnseenActivitySubmissions()`: `EXISTS` on `activity_submissions` with status in `SUBMITTED`/`REVIEWED`/`GRADED` and `teacher_seen_at IS NULL`
    - add `markActivityTeacherSeen(UUID submissionId)`
  - In `BE/notification/service/NotificationService.java`, add `markActivitySeen`, and have `badges` compute `units` and set `panel = homework || quiz || units`.
  - In `BE/notification/dto/BadgeSummary.java`, add `boolean units` and update `none()`.
- [X] T046 [US4] In `BE/admin/service/ActivityAdminService.java`:
  - `getSubmissionDetail` and `getExerciseResult` call `notificationService.markActivitySeen(submissionId)`.
  - `getReviewQueue(UUID unitIdOrNull)` filters by the owned presentation's unit and returns a new `BE/admin/dto/ActivityReviewQueueItemDto.java` (the `HomeworkReviewQueueItemDto` fields plus `activityId`, `unitId`, `page`; `assignmentTitle` = title or `"Actividad – página " + page`), using `ActivitySubmissionRepository.findSubmittedManualQueue` extended with an optional unit filter and the page and unit columns.
  - In `BE/admin/controller/ActivityAdminController.java`, the queue endpoint accepts `@RequestParam(required = false) UUID unitId`.
- [X] T047 [US4] Add `GET /api/v1/admin/units/{id}/activity-progress`:
  - New DTO `BE/units/dto/UnitActivityProgressRow.java` `(activityId, page, studentId, status, scorePercent, submissionId, submittedAt, unseen)`.
  - New query `findActivityProgress(UUID unitId)` in `BE/units/repository/UnitRepository.java`: activities × `unit_assignments`, LEFT JOIN `activity_submissions`. A missing submission gives `PENDING`, and `unseen` = status submitted/reviewed/graded AND `teacher_seen_at IS NULL`.
  - New method `UnitService.activityProgress(id)` and the controller method in `BE/units/controller/UnitAdminController.java`.
- [X] T048 [US4] Extend the student profile:
  - In `BE/admin/dto/StudentProfileResponse.java`, add `List<StudentProfileUnitActivityDto> unitActivities`.
  - Create `BE/admin/dto/StudentProfileUnitActivityDto.java` `(unitId, unitLevel, unitSubject, activityId, page, title, status, scorePercent, submissionId, unseen)`.
  - Create a query in `BE/learning/repository/ActivitySubmissionRepository.java`, `findUnitActivitiesForStudent(UUID userId)`: the student's `unit_assignments` → owned presentation → activities, LEFT JOIN the student's submission, ordered by unit level, unit position, page.
  - Populate the list in `BE/admin/service/StudentProfileAdminService.java`.
- [X] T049 [P] [US4] Update the front-end API:
  - `FE/lib/notifications.ts`: `BadgeSummary.units` and the `EMPTY_BADGES` default.
  - `FE/lib/admin.ts`:
    - add `getUnitActivityProgress(unitId)`
    - `getActivityReviewQueue(unitId?)` with the new item type
    - add `unitActivities` to the `StudentProfile` type
- [X] T050 [US4] Create `FE/components/admin/units/UnitActivityProgress.tsx`: for each activity (page order), list the assigned students with a status badge, score and `NotificationDot` when `unseen`. Clicking a row that has a `submissionId` opens `FE/components/admin/units/ActivityReviewDialog.tsx`; on close, refresh the progress, `getUnit` and the badges, using the same refresh mechanism `HomeworkReviewDialog` callers use. Mount it in `UnitEditorPage.tsx` below the page editor, and pass `unseenPages` (from `detail.activities[].hasUnseenSubmissions`) to `PdfThumbnailStrip`.
- [X] T051 [P] [US4] Show the dots in the admin UI:
  - In `FE/components/admin/AdminPanel.tsx`, add a `NotificationDot` on the Unidades `TabsTrigger` when `badges.units`, following the pattern used on the homework and quizzes triggers.
  - In `FE/components/admin/units/UnitCard.tsx`, add a dot when `unit.hasUnseenActivitySubmissions`.
- [X] T052 [US4] In `FE/components/admin/students/StudentPresentationsPanel.tsx`, add a "Actividades de las unidades" section grouped by unit (level · subject), with rows "p. {page} · title-or-fallback · status (%)" and a dot when unseen. A row with a `submissionId` opens `ActivityReviewDialog`. In the student profile route `FE/routes/panel_.alumnos.$studentId.tsx` (or wherever `StudentStatBox` for Presentaciones is built), show the box's unseen dot when any `unitActivities[].unseen`.
- [X] T053 [P] [US4] Add the US4 i18n keys (progress section, status labels if new, the profile unit-activities heading, "Por corregir") to `FE/i18n/locales/es.ts`, `en.ts` and `ro.ts`.

**Checkpoint**: Submitting raises the dots; opening the review clears them; grading reaches the student.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T054 [P] Extend `back-end/src/test/java/com/kuky/backend/presentations/PresentationServiceTest.java`: `get`, `uploadFile`, `setShares` and `delete` on a unit-owned id throw `PresentationNotFoundException`. Fix any existing tests that referenced removed activity, trigger or instructions code (`grep -rn "Activity\|trigger" back-end/src/test`).
- [X] T055 [P] Update `CLAUDE.md` Key implementation notes. Add a "Units" bullet: units own one PDF (a unit-owned `presentations` row, `presentations.unit_id` = ownership with CASCADE, one per unit, `presentation_files.page_count` reported by pdf.js), activities = `(presentation_id, page)` with today's grading, no Actividades tab, and the Presentations tab = standalone decks only. Update the `/panel` and `/aprendizaje` rows of Current pages to match.
- [X] T056 Run `cd back-end && ./gradlew build` and `cd front-end && npm run lint && npm run format && npm run build`; fix every warning or error introduced by this feature, and make sure no reference to `triggerFileId`, `instructionsText`, `youtubeUrl` (in an activity context), `ActivitiesTab` or `setUnitPresentations` remains (`grep -rn`).
- [X] T057 Run all browser scenarios 1–21 of [quickstart.md](./quickstart.md) against the local stack (with the Mailpit check for scenario 8), including the 375 px pass. Fix any defects found, and capture a screenshot of the unit editor with marked pages as proof.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none. T001 must exist before any back-end run.
- **Foundational (Phase 2)**: depends on Phase 1 and blocks every story. T003 → T004 → T008/T010; T007 → T008; T011 → T010 → T012; T013 → T014; T016 and T017 come before any PDF UI; T018 → T019.
- **US1 (Phase 3)**: depends on Phase 2. T021 → T022 → T023/T024 → T025. Front-end: T026 → T027/T028 → T029 → T030.
- **US2 (Phase 4)**: depends on US1. It needs the owned presentation (T023), `UnitDetail.activities` (T022/T024) and `UnitEditorPage` (T029).
- **US3 (Phase 5)**: depends on Phase 2. To test it end to end, US1 and US2 data must exist, but the code tasks can run in parallel with US2.
- **US4 (Phase 6)**: depends on US2 (activities exist) and US1 (`UnitEditorPage`).
- **Polish (Phase 7)**: after all stories.

### Story independence

- US1 alone delivers a working "unit = PDF + homework" with student access (MVP).
- US2 builds on US1's view; US3 consumes US2's data but touches only student files; US4 adds only review and indicator surfaces.

### Parallel opportunities

- Phase 1: T002 ∥ T001.
- Phase 2: T005, T006, T011, T015, T016 and T017 ∥ each other. Back-end (T003–T015) ∥ front-end (T016–T019).
- US1: T020 ∥ T026 ∥ T027 ∥ T028 ∥ T031 once T021 is done.
- US2: T032 ∥ T035 ∥ T038; back-end T033/T034 ∥ front-end T036.
- US3: T040 ∥ T043 ∥ T039; T041 and T042 are separate files ∥.
- US4: T044 ∥ T049 ∥ T051 ∥ T053; back-end T045–T048 ∥ front-end T050/T052 after T049.

## Parallel Example: User Story 1

```text
Task: "T020 UnitPdfServiceTest in back-end/src/test/java/com/kuky/backend/units/UnitPdfServiceTest.java"
Task: "T026 Unit API types/functions in front-end/src/lib/admin.ts"
Task: "T027 PdfThumbnailStrip in front-end/src/components/admin/units/PdfThumbnailStrip.tsx"
Task: "T028 UnitPdfUploader in front-end/src/components/admin/units/UnitPdfUploader.tsx"
```

## Parallel Example: User Story 4

```text
Task: "T044 NotificationServiceTest units badge"
Task: "T049 BadgeSummary.units + progress/queue API in front-end/src/lib"
Task: "T051 Dots on Unidades tab + UnitCard"
Task: "T053 i18n for progress/profile"
```

## Implementation Strategy

### MVP (US1)

1. Phase 1 → Phase 2 (both builds green, Actividades tab removed).
2. Phase 3 (US1): stop and validate quickstart 1–3, 7, 8 and 20.

### Incremental delivery

1. Add US2: the teacher can author page activities (quickstart 4–6, 9, 18, 19).
2. Add US3: students complete them (quickstart 11–14).
3. Add US4: review entry points and dots (quickstart 15–17).
4. Polish: docs, full build, the full quickstart including mobile.

## Notes

- `routeTree.gen.ts` is regenerated by the router plugin; never edit it by hand.
- Keep client-only PDF code out of SSR paths (`useEffect` / client components, as `PresentationPdfViewer` does).
- Use `credentials: 'include'` for every new fetch, including multipart.
- Commit after each phase checkpoint.
