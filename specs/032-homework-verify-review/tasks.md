---
description: "Task list for admin homework verify review"
---

# Tasks: Admin Homework Verify Review

**Input**: Design documents from `specs/032-homework-verify-review/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/homework-verify-review-api.md](./contracts/homework-verify-review-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit for plainText equality, 500-char feedback, `LEGACY_RICH` freeze vs `ANNOTATED` re-edit, WRITE/multi annotate, activity parity, EXERCISE regression (per [plan.md](./plan.md)). Frontend — browser verification via [quickstart.md](./quickstart.md) (no frontend unit-test framework). Spec did not request TDD-first; tests follow implementation in each phase where noted.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Schema + discriminator every annotate/feedback story depends on.

- [x] T001 Write `back-end/src/main/resources/db/migration/V16__homework_verify_review.sql` per [data-model.md](./data-model.md): add nullable `review_model` TEXT with CHECK (`NULL` | `LEGACY_RICH` | `ANNOTATED`) on `homework_submissions` and `activity_submissions`; backfill `LEGACY_RICH` for MANUAL homework/`REVIEWED`+non-null feedback and MANUAL activity `REVIEWED`+non-null feedback (leave EXERCISE/`GRADED` untouched)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared enum, FormattedText helpers, DTO/repo plumbing for `review_model` so US2–US3 can save/load without inventing parallel types. US1 wrap can start after T001 conceptually but wait for this phase to keep one foundation checkpoint.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Add `ReviewModel` enum (`LEGACY_RICH`, `ANNOTATED`) in `back-end/src/main/java/com/kuky/backend/learning/model/ReviewModel.java`
- [x] T003 [P] Extend `FormattedTextSegment` in `back-end/src/main/java/com/kuky/backend/learning/model/FormattedTextSegment.java`: `plainText(...)`, `encodePlainFeedback(plain, maxLen)` (or MANUAL max **500** helper), allow empty/null plain feedback encoding; keep exercise 2000 path intact
- [x] T004 [P] Map `review_model` on `HomeworkSubmissionRepository` / `ActivitySubmissionRepository` (detail row + reads) under `back-end/src/main/java/com/kuky/backend/learning/repository/`
- [x] T005 [P] Extend `ManualAnswerViewDto` with optional `formatted` (`List<FormattedTextSegment>`) and helpers to parse `answer_text` as plain vs FormattedText JSON in `back-end/src/main/java/com/kuky/backend/learning/dto/ManualAnswerViewDto.java` (and answer repos if mapping lives there)
- [x] T006 Extend `HomeworkSubmissionAdminDto` with `reviewModel`, `feedbackText` in `back-end/src/main/java/com/kuky/backend/admin/dto/HomeworkSubmissionAdminDto.java`; populate in `HomeworkAdminService.toSubmissionAdminDto` (legacy → `feedback` FormattedText + null `feedbackText`; annotated → `feedbackText` + null rich `feedback`)
- [x] T007 [P] Mirror admin DTO mapping for activity review detail in `back-end/src/main/java/com/kuky/backend/admin/service/ActivityAdminService.java`
- [x] T008 [P] Extend student `HomeworkItemResponse` / `ActivityItemResponse` + `HomeworkItems` under `back-end/src/main/java/com/kuky/backend/learning/` with `reviewModel`, `feedbackText`, and answer `formatted` when present
- [x] T009 [P] Update TypeScript types in `front-end/src/lib/admin.ts` and `front-end/src/lib/learning.ts` for `reviewModel`, `feedbackText`, `answers[].formatted` per [contracts/homework-verify-review-api.md](./contracts/homework-verify-review-api.md)

**Checkpoint**: V16 applies; GET review/learning payloads expose `reviewModel` / `feedbackText` / optional formatted answers; FormattedText helpers ready for annotate + plain note saves.

---

## Phase 3: User Story 1 - Wrap long student answers (Priority: P1) — MVP

**Goal**: Admin review (and student post-review) views wrap unbroken long lines; no horizontal scroll on answer areas.

**Independent Test**: Submit a long unbroken string; open admin review and student view — text wraps; no horizontal scrollbar on the answer block ([quickstart.md](./quickstart.md) §1).

### Implementation for User Story 1

- [x] T010 [P] [US1] Strengthen wrap on `front-end/src/components/learning/richtext/RichTextViewer.tsx` (`overflow-wrap: anywhere` / Tailwind `break-all` as needed alongside existing `break-words`)
- [x] T011 [P] [US1] Apply the same no-horizontal-scroll wrap classes to plain answer blocks in `front-end/src/components/admin/homework/HomeworkReviewDialog.tsx` and `front-end/src/components/admin/activities/ActivityReviewDialog.tsx`
- [x] T012 [US1] Ensure student reviewed MANUAL views under `front-end/src/components/learning/` (WRITE viewer + multi FREE_TEXT answer display) use the same wrap behavior

**Checkpoint**: SC-001 / FR-001–002 satisfied without needing annotate/feedback yet.

---

## Phase 4: User Story 2 - Annotate student answer in place (Priority: P1)

**Goal**: Teacher annotates WRITE / FREE_TEXT answers with color/highlight/strikethrough; wording locked; full mark control; `ANNOTATED` reviews re-editable; `LEGACY_RICH` frozen; activity parity.

**Independent Test**: Annotate WRITE and multi answers, save → REVIEWED/`ANNOTATED`; student sees marks; re-open as teacher and edit marks again; legacy rich review stays view-only ([quickstart.md](./quickstart.md) §2–3, §5–6).

### Implementation for User Story 2

- [x] T013 [US2] Change `SaveHomeworkFeedbackRequest` in `back-end/src/main/java/com/kuky/backend/admin/dto/SaveHomeworkFeedbackRequest.java` to new-model body: optional `feedbackText`, optional `response` FormattedText, optional `answers: [{ questionId, formatted }]` per contract (activity reuse same DTO or twin)
- [x] T014 [US2] Implement annotate save in `HomeworkAdminService.saveFeedback`: plainText equality vs stored WRITE/`answer_text`; write annotated `response_text` / FormattedText JSON into `answer_text`; first save `SUBMITTED`→`REVIEWED` + `review_model=ANNOTATED`; allow re-save when `ANNOTATED`; reject `LEGACY_RICH` with `ALREADY_REVIEWED`; repository update sets `review_model` + annotations (feedback plain can be stubbed null until US3)
- [x] T015 [US2] Mirror annotate save + legacy freeze in `ActivityAdminService.saveFeedback` and `ActivitySubmissionRepository` under `back-end/src/main/java/com/kuky/backend/`
- [x] T016 [P] [US2] Add `formatOnly` mode to `front-end/src/components/learning/richtext/RichTextEditor.tsx` — toolbar/selection formatting works; block typing/paste/delete that changes characters
- [x] T017 [US2] Rework `HomeworkReviewDialog.tsx` to annotate WRITE via format-only editor on `response`, and each FREE_TEXT answer via format-only editor seeded from plain/`formatted`; disable save UI when `reviewModel === "LEGACY_RICH"`; allow edit when `SUBMITTED` or `ANNOTATED`
- [x] T018 [US2] Same review UX in `ActivityReviewDialog.tsx`
- [x] T019 [US2] Update `saveHomeworkFeedback` / activity save helpers in `front-end/src/lib/admin.ts` to send the new request body
- [x] T020 [US2] Student learning UI: render annotated `response` / `answers[].formatted` with `RichTextViewer` after review under `front-end/src/components/learning/` (homework + activity)
- [x] T021 [P] [US2] Add i18n for annotate/legacy-frozen/save errors in `front-end/src/i18n/locales/es.ts`, `en.ts`, `ro.ts`
- [x] T022 [US2] Backend tests in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` (and activity tests): plainText mismatch rejected; mark-only save OK; `LEGACY_RICH` frozen; `ANNOTATED` re-edit OK; EXERCISE `exercise-feedback` still works

**Checkpoint**: In-place annotation works for homework + activity; wording locked; legacy frozen; student sees marks.

---

## Phase 5: User Story 3 - Short plain feedback note (Priority: P2)

**Goal**: Optional plain ≤500-char feedback box replaces rich whole-submission feedback editor for new-model reviews; student sees the note; empty allowed; over-limit rejected on first save and later edits.

**Independent Test**: Save note ≤500 with annotations; student sees it; >500 rejected; clear/empty note OK; edit note after review ([quickstart.md](./quickstart.md) §2, §4).

### Implementation for User Story 3

- [x] T023 [US3] Complete plain `feedbackText` persistence in `HomeworkAdminService.saveFeedback` / `ActivityAdminService.saveFeedback` (encode ≤500, allow null/empty, keep exercise 2000 path separate); ensure GET maps `feedbackText` for `ANNOTATED` and rich `feedback` only for `LEGACY_RICH`
- [x] T024 [US3] Replace rich feedback `RichTextEditor` with a small plain `<textarea>` (maxLength/validation 500) in `HomeworkReviewDialog.tsx` and `ActivityReviewDialog.tsx` for editable new-model reviews; legacy continues to show `RichTextViewer` of `feedback`
- [x] T025 [US3] Show `feedbackText` on student reviewed MANUAL homework/activity views under `front-end/src/components/learning/` (omit empty); keep legacy `feedback` FormattedText display for `LEGACY_RICH`
- [x] T026 [P] [US3] i18n for feedback placeholder, over-limit message, optional-note labels in `front-end/src/i18n/locales/{es,en,ro}.ts`
- [x] T027 [US3] Backend tests: feedback >500 rejected; empty OK; re-edit updates note; legacy still cannot save — extend `HomeworkAdminServiceTest` / activity tests under `back-end/src/test/java/com/kuky/backend/admin/`

**Checkpoint**: New-model reviews use annotated answers + plain ≤500 note; legacy rich feedback unchanged.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end validation and cleanup across stories.

- [x] T028 Run [quickstart.md](./quickstart.md) browser checks (wrap, WRITE + multi annotate, feedback limit + re-edit, legacy freeze, activity parity, EXERCISE regression)
- [x] T029 [P] Run `./gradlew test` in `back-end/` and fix regressions from review DTO/save changes
- [x] T030 [P] Spot-check `front-end` lint on touched files (`HomeworkReviewDialog`, `ActivityReviewDialog`, richtext, `admin.ts`, `learning.ts`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on T001 — **BLOCKS** US1–US3
- **US1 (Phase 3)**: After Phase 2 — frontend-only wrap; no dependency on US2/US3
- **US2 (Phase 4)**: After Phase 2 — delivers annotate + legacy/re-edit; can stub empty `feedbackText`
- **US3 (Phase 5)**: After US2 save path (T014/T015) — completes plain note UX on the same PUT
- **Polish (Phase 6)**: After desired stories complete

### User Story Dependencies

- **US1 (P1)**: Independent after foundation — MVP for readability
- **US2 (P1)**: Independent of US1 visually (but wrap helps annotate UX); core of verify
- **US3 (P2)**: Builds on US2’s new save contract/UI shell

### Parallel Opportunities

- T002–T005, T007–T009 after T001/T006 sequencing as marked [P]
- T010–T011 in parallel within US1
- T016 can proceed in parallel with backend T013–T015
- T021 / T026 i18n parallel within their stories
- T029–T030 parallel in polish

---

## Parallel Example: User Story 2

```text
# After T013–T015 backend save lands (or in parallel with T014):
Task: "Add formatOnly mode to front-end/src/components/learning/richtext/RichTextEditor.tsx"
Task: "Add i18n for annotate/legacy-frozen/save errors in front-end/src/i18n/locales/{es,en,ro}.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1–2 (migration + DTO plumbing)
2. Complete Phase 3: US1 wrap
3. **STOP and VALIDATE** quickstart §1
4. Demo readable long answers immediately

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 wrap → demo
3. US2 annotate (+ legacy freeze / re-edit) → demo core verify
4. US3 plain ≤500 note → full spec
5. Polish / quickstart full pass

### Suggested MVP scope

**US1 only** (wrap) is the smallest valuable ship. Practical “verify” MVP for Paula is **US1 + US2**; US3 completes the clarified feedback model.

---

## Notes

- [P] = different files, no incomplete-task dependencies
- Do not change EXERCISE `…/exercise-feedback` behavior
- FREE_TEXT `answer_text`: plain until annotated, then FormattedText JSON (see research)
- Commit after each task or logical group
- Stop at checkpoints to validate independently
