---
description: "Task list for percentage grades on manual answers"
---

# Tasks: Percentage Grades for Manual Answers

**Input**: Design documents from `specs/037-manual-percent-grade/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/manual-percent-grade-api.md](./contracts/manual-percent-grade-api.md), [quickstart.md](./quickstart.md)

**Tests**: Backend JUnit for percent scoring, partial save, finalize, student stripping, migration backfill, activity parity (per [plan.md](./plan.md)). Frontend — browser verification via [quickstart.md](./quickstart.md). Spec did not request TDD-first; tests follow implementation where noted.

**Organization**: Tasks are grouped by user story (from [spec.md](./spec.md)) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Paths are relative to the repo root (`back-end/`, `front-end/`)

---

## Phase 1: Setup

**Purpose**: Schema migration every story depends on (`teacher_score_percent`, drop `teacher_validation`).

- [x] T001 Write `back-end/src/main/resources/db/migration/V19__manual_percent_grade.sql` per [data-model.md](./data-model.md): add nullable `teacher_score_percent INT` CHECK (NULL or 0–100) on `homework_answers`, `activity_answers`, and `homework_submissions`; backfill answers `VALIDATED`→100 / `INVALIDATED`→0 and set `score = percent/100.0`; backfill WRITE graded submissions `teacher_score_percent = score_percent` where applicable; drop `teacher_validation` columns and CHECKs

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared models, composition scoring helpers, DTO/repo field rename so review/student stories share one percent model. No user story work until this phase completes.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T002 [P] Replace `teacherValidation` with `teacherScorePercent` (Integer nullable) on answer models/repos under `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkAnswer.java` and activity answer model + JDBC repository update methods (`updateManualReview` etc.)
- [x] T003 [P] Add `teacherScorePercent` on `back-end/src/main/java/com/kuky/backend/learning/model/HomeworkSubmission.java` (+ repository read/write) for WRITE draft/grade
- [x] T004 Update `HomeworkCompositionSupport` in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkCompositionSupport.java`: replace `teacherValidationScore` with `teacherPercentAsScore(int percent)` → `percent/100.0`; keep `scorePercentFromScores` half-up; add helper for fully-correct count (contribution == 1.0)
- [x] T005 [P] Update admin/learning DTOs: `SaveHomeworkFeedbackRequest` / nested annotated answer (`teacherScorePercent`, `finalize`), `ManualAnswerViewDto`, `HomeworkSubmissionAdminDto` (and activity mirrors) under `back-end/src/main/java/com/kuky/backend/admin/dto/` and `back-end/src/main/java/com/kuky/backend/learning/dto/` — remove `teacherValidation` fields
- [x] T006 Delete unused `back-end/src/main/java/com/kuky/backend/learning/model/TeacherValidation.java` and fix compile breakages that still reference it
- [x] T007 [P] Update TypeScript types in `front-end/src/lib/learning.ts` and `front-end/src/lib/admin.ts`: remove `TeacherValidation`; add `teacherScorePercent?: number | null` and `finalize?: boolean` on review payloads per [contracts/manual-percent-grade-api.md](./contracts/manual-percent-grade-api.md)

**Checkpoint**: V19 applies; APIs compile with `teacherScorePercent` + `finalize`; composition helpers score from percents.

---

## Phase 3: User Story 1 - Teacher grades a manual answer with a percentage (Priority: P1) — MVP

**Goal**: Teacher enters 0–100% per FREE_TEXT/WRITE instead of validate/invalidate; can save partial progress (`finalize: false`); finalize requires all percents → `GRADED` with overall average; optional annotations/note unchanged.

**Independent Test**: Submit WRITE or ALL_MANUAL as student; as teacher enter e.g. 70%, finalize; submission graded with matching overall % ([quickstart.md](./quickstart.md) §1–2).

### Implementation for User Story 1

- [x] T008 [US1] Rework `HomeworkAdminService.saveFeedback` / `finalizeWithValidations` in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`: accept `teacherScorePercent` + `finalize`; progress save persists subset and stays `SUBMITTED`; finalize requires all FREE_TEXT/WRITE percents ∈ 0–100, computes overall via `scorePercentFromScores`, sets `GRADED`; reject out-of-range / missing-on-finalize with `VALIDATION_ERROR`
- [x] T009 [US1] Persist per-answer `teacher_score_percent` + derived `score` on progress/finalize; WRITE uses submission-level `teacher_score_percent` in the same service/repository paths under `back-end/src/main/java/com/kuky/backend/`
- [x] T010 [US1] Replace Validate/Invalidate UI with percent inputs (0–100) and distinct Save progress / Finalize actions in `front-end/src/components/admin/homework/HomeworkReviewDialog.tsx`; wire payload `teacherScorePercent` + `finalize`
- [x] T011 [P] [US1] i18n for percent labels, validation errors, save-progress vs finalize in `front-end/src/i18n/locales/es.ts`, `en.ts`, `ro.ts` (remove/repurpose validate/invalidate copy)
- [x] T012 [US1] Backend tests in `back-end/src/test/java/com/kuky/backend/admin/` (extend `HomeworkAdminServiceTest` or add focused test): partial save stays `SUBMITTED`; finalize blocked if any percent missing; 70% WRITE → `scorePercent` 70; out-of-range rejected

**Checkpoint**: Teacher can percent-grade homework manuals with partial save; SC-001 / FR-001–003, FR-006, FR-010 ready for homework.

---

## Phase 4: User Story 2 - Student sees percentage-based manual results (Priority: P1)

**Goal**: After finalize, student sees per-answer `%`, overall `%`, fully-correct only at 100%; while `SUBMITTED` (incl. teacher drafts), student never sees teacher percents; MIXED auto provisional unchanged.

**Independent Test**: After teacher finalizes, student sees numeric percents (no validated/invalidated); while awaiting with partial teacher saves, student sees no teacher % ([quickstart.md](./quickstart.md) §1–3).

### Implementation for User Story 2

- [x] T013 [US2] Strip `teacherScorePercent` (and hide teacher manual `score` credit) from student learning item builders while status is `SUBMITTED` in `back-end/src/main/java/com/kuky/backend/learning/service/HomeworkItems.java` (and activity student item builder); after `GRADED` expose percents, overall `scorePercent`, `fullyCorrectCount` per contract
- [x] T014 [US2] Replace validated/invalidated badges with percent display on graded views in `front-end/src/components/learning/MixedHomeworkForm.tsx` and `front-end/src/components/learning/ManualMultiAnswerForm.tsx`
- [x] T015 [P] [US2] Show graded percent on Writing result UI in `front-end/src/components/learning/HomeworkWritePage.tsx` (or the component that renders WRITE results)
- [x] T016 [US2] Backend/unit coverage for student stripping + fully-correct count (100 only) under `back-end/src/test/java/com/kuky/backend/learning/`

**Checkpoint**: Students see percents only when graded; FR-002b, FR-005, FR-005a, SC-002–003 satisfied for homework.

---

## Phase 5: User Story 3 - Teacher can revise percentages after grading (Priority: P2)

**Goal**: On `GRADED` submissions, teacher can change percents and re-finalize; overall recalculates; student sees latest.

**Independent Test**: Grade at 60%; change to 80% and save; student sees 80% ([quickstart.md](./quickstart.md) §4).

### Implementation for User Story 3

- [x] T017 [US3] Confirm/extend `HomeworkAdminService` re-edit path for `GRADED` + `ANNOTATED` to accept updated `teacherScorePercent` with `finalize: true`, recalc `scorePercent` / fully-correct in `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java` (block clear/missing percents on graded save)
- [x] T018 [US3] Ensure `HomeworkReviewDialog.tsx` loads existing percents for graded editable reviews and allows edit + finalize in `front-end/src/components/admin/homework/HomeworkReviewDialog.tsx`
- [x] T019 [US3] Test re-edit updates overall % in `back-end/src/test/java/com/kuky/backend/admin/` 

**Checkpoint**: FR-007 / SC-004 work for homework.

---

## Phase 6: User Story 4 - Mixed and presentation activities use the same percentage model (Priority: P2)

**Goal**: MIXED homework and presentation activities use the same percent + partial/finalize rules; no remaining validate/invalidate path.

**Independent Test**: Mixed homework + activity with FREE_TEXT: partial save then finalize; combined overall correct ([quickstart.md](./quickstart.md) §3, §5).

### Implementation for User Story 4

- [x] T020 [US4] Mirror percent progress/finalize + WRITE-N/A path in `back-end/src/main/java/com/kuky/backend/admin/service/ActivityAdminService.java` (same rules as homework FREE_TEXT)
- [x] T021 [US4] Replace Validate/Invalidate with percent + Save progress / Finalize in `front-end/src/components/admin/activities/ActivityReviewDialog.tsx`
- [x] T022 [US4] Ensure MIXED overall average (auto 0/1 + manual percent/100) and activity student stripping parity in activity student services under `back-end/src/main/java/com/kuky/backend/learning/`
- [x] T023 [US4] Activity (+ mixed) backend tests under `back-end/src/test/java/com/kuky/backend/admin/` and/or learning tests: activity finalize; mixed 1 auto correct + manual 50 → overall 75

**Checkpoint**: FR-008, FR-004, US4 complete; no validate/invalidate left in activity UI.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Migration smoke, cleanup, docs, end-to-end quickstart.

- [x] T024 [P] Grep/remove remaining `teacherValidation` / Validate/Invalidate UI strings across `front-end/` and `back-end/`; update any admin student-profile review entry points that still assume binary validation
- [x] T025 Verify migration smoke: pre-V19 validated/invalidated rows appear as 100/0 with unchanged overall % (manual SQL or test fixture per [quickstart.md](./quickstart.md) §6) — FR-011 / SC-005
- [ ] T026 Run [quickstart.md](./quickstart.md) browser scenarios (§1–5) against local `:8080`/`:8081`
- [x] T027 [P] Confirm `CLAUDE.md` homework-grading note matches shipped behavior (already drafted in plan); adjust only if implementation diverged

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on T001 — **BLOCKS** all user stories
- **US1 (Phase 3)**: After Phase 2 — MVP
- **US2 (Phase 4)**: After Phase 2; practically after T008–T009 so graded payloads exist (can stub, but prefer after US1 backend)
- **US3 (Phase 5)**: After US1 review path exists
- **US4 (Phase 6)**: After Phase 2; can parallel with US1 frontend once T008 patterns are clear; ideally after T008 for copy-paste parity
- **Polish (Phase 7)**: After desired stories complete

### User Story Dependencies

- **US1 (P1)**: Foundation only — MVP
- **US2 (P1)**: Needs graded + awaiting payloads from US1 backend (T008–T009)
- **US3 (P2)**: Extends US1 re-edit
- **US4 (P2)**: Mirrors US1/US2 for activities + mixed emphasis

### Parallel Opportunities

- T002, T003, T005, T007 in Phase 2 can proceed in parallel after T001
- T011 i18n parallel with T010 dialog work
- T014 / T015 student UI parallel after T013
- T020 activity backend can start once T008 homework pattern is stable; T021 parallel with T020 after types exist

---

## Parallel Example: Phase 2

```bash
# After T001 migration:
Task: "Replace teacherValidation on answer models/repos (T002)"
Task: "Add teacherScorePercent on HomeworkSubmission (T003)"
Task: "Update admin/learning DTOs (T005)"
Task: "Update front-end types (T007)"
# Then T004 composition helpers, T006 delete enum
```

---

## Parallel Example: User Story 2

```bash
Task: "Percent badges in MixedHomeworkForm (T014)"
Task: "Percent on WRITE result (T015)"
# After T013 student stripping
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (V19)
2. Complete Phase 2: Foundational
3. Complete Phase 3: US1 (teacher percent grade + partial/finalize on homework)
4. **STOP and VALIDATE** via quickstart §1–2
5. Demo if ready

### Incremental Delivery

1. Setup + Foundational → schema/API fields ready
2. US1 → teacher can percent-grade homework (MVP)
3. US2 → students see correct visibility/results
4. US3 → re-edit after grade
5. US4 → activities + mixed parity
6. Polish → migration smoke + quickstart full pass

### Parallel Team Strategy

1. Together: Phase 1–2
2. Then: Dev A US1 → US3; Dev B US2 frontend + T013; Dev C US4 activity mirror after T008 lands

---

## Notes

- [P] = different files, no incomplete-task dependencies
- Do not keep a dual validate/invalidate path (FR-012)
- Students must never see teacher percents while `SUBMITTED` (FR-002b)
- Commit after each task or logical group
- Stop at checkpoints to validate independently
