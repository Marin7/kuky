# Research: Presentation Activities

**Feature**: `029-presentation-activities` | **Date**: 2026-08-07

## 1. Separate activity tables vs reuse `homework_assignments`

**Decision**: New parallel tables (`activities`, `activity_questions`, `activity_question_options`, `activity_submissions`, `activity_answers`, …) mirroring homework shapes, plus `activity_instructions_files` (1:1 PDF). Do **not** overload `homework_assignments` with a nullable `presentation_id`.

**Rationale**: Spec requires no due dates, no assignees, not appearing in the unit mixed sequence (`unit_position` / homework list), and presentation ownership with cascade delete. Putting activities in `homework_assignments` would force filters everywhere (`ContentRepository`, unit reorder, homework tab, progress) and risk regressions (FR-012). Parallel tables keep homework untouched.

**Alternatives considered**:
- Discriminator on `homework_assignments` — rejected (pollutes unit sequencing, assignees, due dates, admin homework list).
- Single polymorphic “assignments” table — rejected (YAGNI; large migration for one feature).

## 2. Exercise engine reuse

**Decision**: Copy the homework question/answer schema for activities and reuse `ExerciseGradingService` / admin question validation by extracting shared helpers (or dual-entry methods keyed by activity vs homework IDs) so all seven `QuestionKind` values work identically. Frontend reuses existing question editors and `ExerciseForm` / `ManualAnswerForm` behind activity wrappers.

**Rationale**: Spec demands full homework parity for work types. Grading logic is already kind-driven, not homework-branded. Shared code paths avoid a second grading engine while keeping FKs clean.

**Alternatives considered**:
- Call homework tables with a synthetic “hidden” homework per activity — rejected (access/assignee semantics wrong; cascade and listing messy).
- Defer EXERCISE kinds — rejected by clarification.

## 3. Instructions PDF storage

**Decision**: Dedicated `ActivityInstructionsFileStore` + `app.activity-instructions.storage-dir` (default `./data/activity-instructions`), same pattern as `PresentationFileStore` (UUID key, metadata in DB, bytes on disk). Accept `application/pdf` only in v1. Multipart upload on create/update.

**Rationale**: Matches existing file-store convention; keeps presentation files and activity instructions separable for cleanup and quotas.

**Alternatives considered**:
- Store under `presentation_files` — rejected (wrong ownership; deleted when presentation file removed independently of activity lifecycle).
- Rich-text instructions — out of scope (clarification: PDF only).

## 4. Page-trigger model and page-count validation

**Decision**: Columns `trigger_file_id` (FK → `presentation_files`, nullable, `ON DELETE SET NULL`) and `trigger_page` (INT nullable). Both null = no prompt; both set = valid trigger. Changing presentation clears trigger if file no longer belongs to the new presentation. On presentation file delete, SET NULL clears the trigger (activity remains). Server validates: file belongs to activity’s presentation; `trigger_page >= 1`. Page-count upper bound: client sends `triggerPage` after pdfjs reports page count; server optionally rejects if client also sends `pageCount` and `triggerPage > pageCount`. Do not persist page counts on `presentation_files` in v1.

**Rationale**: Spec requires file+page; missing file must not fire prompts. Opening every PDF server-side for page count adds Apache PDFBox dependency — avoid (YAGNI); client already uses pdfjs.

**Alternatives considered**:
- Server-side PDF page count library — deferred.
- Trigger only when presentation has one PDF — rejected by clarification (file+page always).

## 5. Student discovery API shape

**Decision**: Embed `activities: ActivitySummary[]` (ordered by `position`) on each `SharedPresentationSummary` in `GET /api/v1/learning`. Dedicated student endpoints for get/submit (mirror homework): `GET/PUT /api/v1/learning/activities/{id}`, `PUT .../answers` for exercises, `GET .../instructions` for PDF bytes. Access = presentation access (`isSharedWith` / unit assignment).

**Rationale**: Activities nest under presentations in UI; embedding avoids a second client join. Detail/submit endpoints match homework patterns students/teachers already understand.

**Alternatives considered**:
- Flat top-level `activities[]` on learning overview — weaker nesting; more client work.
- Only return activities inside unit-detail API — no such student API exists today.

## 6. Admin surface

**Decision**: New **Activities** admin tab + dedicated authoring routes mirroring homework (`/panel/actividades`, `/panel/actividades/nueva`, `/panel/actividades/$id`). List filterable by presentation; create requires presentation + instructions PDF; reorder via DnD on the presentation’s activity list (or in editor context). Reuse homework question editor components.

**Rationale**: Spec allows placement near presentations/homeworks; a dedicated tab keeps Presentations tab focused on files/shares and mirrors Homework authoring complexity (full exercise parity).

**Alternatives considered**:
- Only nested under Presentations tab — cramped for full exercise authoring.
- Embed inside Units — wrong ownership (presentation-linked, not unit-sequence).

## 7. Viewer prompt + overlay UX

**Decision**: Full-page viewer route (`aprendizaje_.presentacion...`) and embedded viewer both expose `onPageVisible(pageNumber)` via IntersectionObserver on each rendered page. When `fileId`+`page` matches incomplete activities, show a non-blocking prompt (toast/banner). Opening an activity mounts `ActivityOverlay` (Shadcn Dialog/Sheet) with fulfillment UI; close restores viewer. Fulfilled activities never re-prompt. Unit-list open navigates to `/aprendizaje/actividad/$activityId` full page.

**Rationale**: Matches clarifications (overlay from prompt; full page from list; land-on page; no re-prompt when done).

**Alternatives considered**:
- Navigate away from viewer — rejected by clarification.
- Blocking modal that freezes page turn until complete — rejected (non-blocking).

## 8. Progress overview

**Decision**: Extend `StudentProgressDto` / homework breakdown UI with activity counts (e.g. `totalActivities`, `completedActivities`, and/or `activityBreakdown` pending/submitted/completed analogous to homework). Source: activities whose presentation the student can access (same share/unit rules), joined to `activity_submissions`.

**Rationale**: FR-018 / clarification require inclusion in existing student progress overview.

**Alternatives considered**:
- Per-presentation only — insufficient per clarification.

## 9. Cascade delete

**Decision**: `activities.presentation_id REFERENCES presentations(id) ON DELETE CASCADE`; child tables cascade from `activities`. Application code deletes instructions PDF from disk when activity rows are removed (presentation delete path + explicit activity delete). Mirror `PresentationService` disk cleanup pattern.

**Rationale**: Spec clarification cascade; DB FK handles rows; disk needs explicit cleanup.

## 10. i18n / localization

**Decision**: Add es/en/ro strings for Activities tab, nested list labels, viewer prompt, overlay, validation errors — same pattern as homework/presentations.

**Rationale**: Project already localizes admin/learning UI.
