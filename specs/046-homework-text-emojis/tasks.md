---
description: "Task list for emojis in homework writing and feedback"
---

# Tasks: Emojis in Homework Writing and Feedback

**Input**: Design documents from `specs/046-homework-text-emojis/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/homework-text-emojis-api.md](./contracts/homework-text-emojis-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit that WRITE `response` and `feedbackText` round-trip emoji and still reject over-limit (per [plan.md](./plan.md)). Frontend — browser verification via [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US3)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: i18n strings for the picker. No new packages.

- [x] T001 [P] Add `richText.emoji` open/insert labels (and per-emoji aria if needed) in `front-end/src/i18n/locales/en.ts`, `es.ts`, and `ro.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared classroom set, insert helper, and picker. Blocks all user stories.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 Add the 26 default-appearance classroom emojis and `insertAtCaret(text, start, end, emoji, maxLength)` (whole emoji or skip if it would exceed; UTF-16 `.length`) in `front-end/src/components/learning/richtext/classroomEmojis.ts` per [research.md](./research.md) §2–3
- [x] T003 Create `ClassroomEmojiPicker` (Shadcn `Popover`, wrap grid of the 26, no categories/search/skin tones) in `front-end/src/components/learning/richtext/ClassroomEmojiPicker.tsx` (depends on T001, T002)

**Checkpoint**: Picker can render in isolation and insert into a string via `insertAtCaret`. No homework screen wired yet.

---

## Phase 3: User Story 1 - Student inserts emojis while writing homework (Priority: P1) — MVP

**Goal**: Students insert classroom emojis from the Writing formatting bar and from mixed homework FREE_TEXT; submit keeps them.

**Independent Test**: Open a Writing homework, insert an emoji from the bar, submit; teacher sees it. Same for mixed FREE_TEXT ([quickstart.md](./quickstart.md) Scenarios 1 and 3 step 2).

### Implementation for User Story 1

- [x] T004 [US1] Add optional `allowEmojiInsert` (default false) to `front-end/src/components/learning/richtext/FormattingToolbar.tsx`: when true, show `ClassroomEmojiPicker`; omit it when the toolbar is used for `formatOnly` (FR-009, FR-010)
- [x] T005 [US1] Add `allowEmojiInsert` (default false) to `front-end/src/components/learning/richtext/RichTextEditor.tsx`; when true and not `formatOnly`, pass it to the toolbar and insert via existing `reconcileEdit` / pending style so color/highlight/strike still apply (FR-001, FR-003, FR-007)
- [x] T006 [US1] Pass `allowEmojiInsert` on the editor in `front-end/src/components/learning/ManualAnswerForm.tsx` (Writing homework compose)
- [x] T007 [US1] Add `allowEmojiInsert` to `front-end/src/components/learning/MixedHomeworkForm.tsx`: when true, put `ClassroomEmojiPicker` next to the FREE_TEXT `Textarea` (do **not** turn on `richFreeText`); pass `allowEmojiInsert` from homework parents only — `HomeworkExercisePage.tsx`, `HomeworkReadingPage.tsx`, `HomeworkListeningPage.tsx`, `HomeworkInlinePanel.tsx` — **not** from `ActivityPanel.tsx` or `front-end/src/routes/quizzes_.$quizId.tsx` (FR-012)
- [x] T008 [US1] Test: submit a WRITE answer whose `response[].text` contains 👍; reload still has 👍 in `back-end/src/test/java/com/kuky/backend/learning/HomeworkSubmissionServiceTest.java` ([contracts/homework-text-emojis-api.md](./contracts/homework-text-emojis-api.md))

**Checkpoint**: Student Writing and mixed homework FREE_TEXT can insert the classroom set. Quizzes/activities still have no picker. FR-001, FR-003, FR-004 (paste already works), FR-012.

---

## Phase 4: User Story 2 - Teacher inserts emojis when leaving homework feedback (Priority: P1)

**Goal**: Paula inserts the same classroom set into the feedback comment (Writing/mixed review and graded exercise). Comment stays unformatted. Student answer `formatOnly` editors stay without emoji insert.

**Independent Test**: Insert an emoji in review feedback, save; student sees it. Repeat on a graded exercise note ([quickstart.md](./quickstart.md) Scenarios 2 and 3 step 1).

### Implementation for User Story 2

- [x] T009 [P] [US2] Put `ClassroomEmojiPicker` next to the feedback `Textarea` in `front-end/src/components/admin/homework/HomeworkReviewDialog.tsx` (max 500); do **not** pass `allowEmojiInsert` on `formatOnly` student-answer editors (FR-002, FR-009)
- [x] T010 [P] [US2] Put `ClassroomEmojiPicker` next to the feedback `Textarea` in `front-end/src/components/admin/homework/ExerciseResultDialog.tsx` (existing max 2000) (FR-002)
- [x] T011 [US2] Test: save `feedbackText` containing 👏; re-read still has 👏 in `back-end/src/test/java/com/kuky/backend/admin/HomeworkAdminServiceTest.java` ([contracts/homework-text-emojis-api.md](./contracts/homework-text-emojis-api.md))

**Checkpoint**: Teacher comments accept classroom emojis without gaining color/highlight/strike. FR-002, FR-005.

---

## Phase 5: User Story 3 - Emojis display correctly after submit and review (Priority: P2)

**Goal**: Saved emojis reappear with formatting for both parties; limits and out-of-set paste still behave.

**Independent Test**: Submit a colored phrase that includes an emoji plus feedback with an emoji; both views match. Over-limit insert is refused. Pasted skin-tone emoji is kept ([quickstart.md](./quickstart.md) Scenarios 1.5, 4).

### Implementation for User Story 3

- [x] T012 [US3] Tests: `FormattedTextSegment.validate` / `encodePlainFeedback` accept emoji-only and mixed emoji+text; reject when UTF-16 length exceeds the existing max in `back-end/src/test/java/com/kuky/backend/learning/HomeworkSubmissionServiceTest.java` and/or a focused test next to `FormattedTextSegment` usage in `HomeworkAdminServiceTest.java` (FR-005, FR-008)
- [x] T013 [US3] Confirm student/teacher read views already render Unicode in formatted answers and `feedbackText` (no strip) in `front-end/src/components/learning/richtext/RichTextViewer.tsx` and existing feedback display in `HomeworkWritePage.tsx` / `HomeworkInlinePanel.tsx` / `HomeworkReviewDialog.tsx` — change only if a view would drop non-BMP characters (FR-006, FR-007)

**Checkpoint**: Persistence and display match the contract. FR-005–FR-008.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Scope sanity and browser check.

- [x] T014 Confirm `allowEmojiInsert` is absent on `front-end/src/components/admin/activities/ActivityReviewDialog.tsx` and `front-end/src/components/quiz/admin/QuizReviewDialog.tsx` (FR-012)
- [x] T015 Run `./gradlew test` in `back-end/` for the homework submit/admin tests touched above
- [x] T016 ESLint/Prettier on new emoji UI files; live [quickstart.md](./quickstart.md) Scenarios 1–5 still need a running local app

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on T001 (picker labels). Blocks US1–US3
- **US1 (Phase 3)**: Depends on Phase 2 — MVP
- **US2 (Phase 4)**: Depends on Phase 2; can proceed in parallel with US1 (different files except shared picker)
- **US3 (Phase 5)**: Depends on US1 submit + US2 save for a full loop; JUnit can start after T008/T011
- **Polish (Phase 6)**: After desired stories

### User Story Dependencies

- **User Story 1 (P1)**: After Phase 2 — no dependency on US2
- **User Story 2 (P1)**: After Phase 2 — no dependency on US1
- **User Story 3 (P2)**: After US1+US2 persistence paths exist

### Parallel Opportunities

- T001 with early T002 draft (T003 needs both)
- T009 and T010 after T003
- US1 and US2 in parallel once the picker exists

---

## Parallel Example: User Story 2

```text
Task: "Picker next to feedback in HomeworkReviewDialog.tsx"
Task: "Picker next to feedback in ExerciseResultDialog.tsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1–2 (set + picker)
2. Phase 3 (Writing bar + mixed FREE_TEXT + submit test)
3. **STOP and VALIDATE** with [quickstart.md](./quickstart.md) Scenario 1

### Incremental Delivery

1. Setup + Foundational
2. US1 → student writing MVP
3. US2 → teacher comments
4. US3 → round-trip tests + display check
5. Polish / quickstart

---

## Notes

- [P] tasks = different files, no dependencies
- Do not enable `richFreeText` on mixed homework just to reuse the toolbar
- `allowEmojiInsert` defaults false so shared editors do not leak to quizzes/activities
- No Flyway, no new npm packages
- Suggested next command: `/speckit-implement`
