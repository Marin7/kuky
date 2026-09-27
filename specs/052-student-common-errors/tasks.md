---

description: "Task list for Student Common Errors"
---

# Tasks: Student Common Errors

**Input**: Design documents from `/specs/052-student-common-errors/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md, quickstart.md

**Tests**: Back-end tests are included because `plan.md` (Technical Context, research R11) calls for them:
- a service unit test (`StudentMistakesAdminServiceTest`), which runs locally;
- controller integration coverage in `StudentAdminControllerIntegrationTest`, which runs in CI against Postgres and is skipped locally.

The front end has no test framework, so the UI is verified manually in a browser via `quickstart.md`, as the constitution requires.

**Organization**: Grouped by user story from `spec.md`:
- **US1** (P1): the box and the newest-first list, with its entries, paging and "open homework".
- **US2** (P2): the label filter.
- **US3** (P3): the period filter.

The endpoint, DTOs and client are shared, so they sit in Foundational. Each story then adds its own server behaviour and UI.

## Path Conventions

This is the existing web-app split. Back-end code lives in `back-end/src/main/java/com/kuky/backend/`, back-end tests in `back-end/src/test/java/com/kuky/backend/`, and front-end code in `front-end/src/`.

---

## Phase 1: Setup

**Purpose**: Confirm the reused building blocks exist. There is nothing to install and no migration (research R1).

- [X] T001 Verify that the reused APIs exist with the expected visibility:
  - `ExerciseGradingService.studentQuestionsFor(List<HomeworkQuestion>)` and `storedProvisionalResultFor(List<HomeworkQuestion>, HomeworkSubmission)` in `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java`;
  - the public `AssignmentSnapshot.present/questionsOf/applyContent` in `learning/service/AssignmentSnapshot.java`;
  - `SingleChoiceItems.isNumbered` and `itemCount` in `learning/service/SingleChoiceItems.java`;
  - `HomeworkCompositionSupport.isAutoGradable` in `learning/service/HomeworkCompositionSupport.java`;
  - `QuestionResultBlock` and `QuestionCard` exported from `front-end/src/components/learning/ExerciseResult.tsx` and `front-end/src/components/learning/QuestionHeading.tsx`;
  - the Shadcn `select.tsx` and `pagination.tsx` in `front-end/src/components/ui/`.

  If `itemCount` is not public, make it `public static`. That is its only change.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The endpoint returns correctly shaped (unfiltered) data, and the front end can call it. All stories depend on this.

**⚠️ CRITICAL**: No user-story work can begin until this phase is complete.

- [X] T002 [P] Add `findAutoGradedSubmittedByUser(UUID userId)` to `back-end/src/main/java/com/kuky/backend/learning/repository/HomeworkSubmissionRepository.java`.
  - It returns a new nested record, `AutoGradedSubmissionRow(HomeworkSubmission submission, HomeworkFormat format, String currentTitle, List<String> labels)`.
  - Query: join `homework_submissions s` with `homework_assignments a`, filtered by `s.user_id = :userId AND s.status <> 'PENDING' AND s.submitted_at IS NOT NULL AND a.format IN ('EXERCISE','MIXED')`, `ORDER BY s.submitted_at DESC, s.id`.
  - Read `a.labels` as a `TEXT[]` → `List<String>`, and map the submission with the repository's existing row mapper, including `assignment_snapshot` (research R2, data-model "Source data").
- [X] T003 [P] Create `back-end/src/main/java/com/kuky/backend/admin/dto/StudentMistakesResponse.java`, a record with every field in `contracts/api.md`:
  - `allTimeMistakeCount`, `Summary summary(mistakeCount, answeredCount)`, `List<LabelOption> labelOptions(key, label)`, `String appliedPeriod`, `String appliedLabel`, `int page`, `int pageSize`, `int totalPages`, `int totalEntries`;
  - `List<Entry> entries`, where each `Entry` has `submissionId`, `assignmentId`, `homeworkTitle`, `homeworkFormat`, `labels`, `submittedAt`, `questionPosition`, `mistakeCount`, an `ExerciseQuestionDto question` and an `ExerciseResultResponse.QuestionResultDto result`.
- [X] T004 Create `back-end/src/main/java/com/kuky/backend/admin/service/StudentMistakesAdminService.java` with `getMistakes(UUID studentId, String period, String labelKey, String page)`.

  **Setup and parsing**
  - Throw `StudentNotFoundException` if `userRepository.findById` is empty.
  - Normalise the parameters leniently: an unknown `period` → `ALL`, a bad or `< 1` `page` → 1, a blank label → null (contract "Lenient parsing").

  **Per submission row**
  - Get questions from `AssignmentSnapshot.questionsOf` when a snapshot is `present`, otherwise from `HomeworkQuestionRepository.findByAssignment`.
  - The title comes from the snapshot when present (research R3), otherwise `currentTitle`.
  - Call `storedProvisionalResultFor` and `studentQuestionsFor`, pair them by question id, and keep only auto-gradable kinds.

  **Counting**
  - Numbered question: answered = `unitResults.size()` and wrong = the units that are not correct. When `unitResults` is empty, answered = wrong = `itemCount(q)`.
  - Any other kind: answered = 1, wrong = 0 or 1.
  - Keep entries where wrong > 0, with a 1-based `questionPosition`.

  **Sorting and paging**
  - Sort by `submittedAt` descending, then `questionPosition` ascending.
  - Compute `allTimeMistakeCount` and a summary over all rows. Page size is 20; clamp `page` to `[1, max(1, totalPages)]` and return the page actually served.
  - For now, return `labelOptions` empty and `appliedLabel` null, with `appliedPeriod = "ALL"`. The filters come in US2 and US3.

  **Read-only**: must NOT inject or call `NotificationService` (FR-014, research R8).
- [X] T005 Add `@GetMapping("/students/{id}/mistakes")` to `back-end/src/main/java/com/kuky/backend/admin/controller/StudentAdminController.java`.
  - Parameters: `@PathVariable UUID id`, plus `@RequestParam(required = false) String period`, `label` and `page`, all passed through as strings.
  - Inject `StudentMistakesAdminService` through the constructor, and extend the Javadoc to mention the mistakes view.
- [X] T006 [P] In `front-end/src/lib/admin.ts`, add:
  - `MistakePeriod`, `StudentMistakeEntry` (with `question: StudentQuestion` and `result: QuestionResult` imported from `@/lib/learning`) and `StudentMistakes`, exactly as in `contracts/api.md`;
  - `getStudentMistakes(studentId, { period, label, page })`, which builds the query string with `URLSearchParams`, leaves out null or undefined values, and calls `apiCall<StudentMistakes>(\`/students/${studentId}/mistakes?...\`)`.

**Checkpoint**: `GET /api/v1/admin/students/{id}/mistakes` returns every mistake newest-first, 20 per page. The front end can call it.

---

## Phase 3: User Story 1 - Teacher reviews a student's recent mistakes (Priority: P1) 🎯 MVP

**Goal**: The Errores frecuentes box in the profile's top row shows the all-time count. It expands (independently of Tareas) into a newest-first list with the summary line, per-entry details including labels and the student's vs the correct answer, numbered pages of 20, and "Abrir tarea".

**Independent Test**: Run `quickstart.md` scenarios 1–6, 10, 11, 14–18. For example, an older "Vocabulario" homework with 1 of 6 wrong and a newer "Subjuntivo" homework with 3 of 5 wrong → 4 mistakes, Subjuntivo's first, each with answers and labels and a working open link.

### Tests for User Story 1

- [X] T007 [P] [US1] Create `back-end/src/test/java/com/kuky/backend/admin/StudentMistakesAdminServiceTest.java`, in the style of `StudentProfileAdminServiceTest.java`, with mocked repositories and a real `AssignmentSnapshot` or `ExerciseGradingService` where practical. Cover:
  - (a) Only questions with a wrong answer become entries, and FREE_TEXT questions in a MIXED homework are ignored and not counted.
  - (b) A numbered single choice with 2 of 3 items wrong gives 1 entry, `mistakeCount` 2 and answered 3. Assert that this equals the result of `HomeworkCompositionSupport.contributions`.
  - (c) A numbered question with no stored answer counts N answered and N wrong.
  - (d) A missing answer on a normal question counts as wrong.
  - (e) Ordering is by `submittedAt` descending, then position.
  - (f) Paging: 45 entries → `totalPages` 3; `page=3` has 5 entries; `page=9` is clamped to 3; `page=abc` becomes 1.
  - (g) The snapshot title and questions are used when present.
  - (h) An unknown student throws `StudentNotFoundException`.
  - (i) No interaction with `NotificationService` (it is not even a dependency).
- [X] T008 [P] [US1] Extend `back-end/src/test/java/com/kuky/backend/admin/StudentAdminControllerIntegrationTest.java`:
  - an admin calling `GET /api/v1/admin/students/{id}/mistakes` gets 200 and the contract fields;
  - a `STUDENT` or `USER` session gets 403;
  - an unauthenticated request gets 401;
  - an unknown UUID gets 404 with `error = STUDENT_NOT_FOUND`;
  - a user whose student role was revoked still gets 200.

### Implementation for User Story 1

- [X] T009 [P] [US1] Add `admin.studentProfile.mistakes.*` keys to `front-end/src/i18n/locales/es.ts`, `front-end/src/i18n/locales/en.ts` and `front-end/src/i18n/locales/ro.ts`:
  - `title` ("Errores frecuentes"), `expand`, `collapse`;
  - `summary` ("{{mistakes}} errores en {{answered}} preguntas");
  - `emptyNone` (no auto-graded homework submitted yet) and `emptyNoMistakes` ("Sin errores en este periodo" plus the answered count);
  - `openHomework` ("Abrir tarea"), `submittedOn`;
  - `pageOf` ("Página {{page}} de {{total}}"), `previous`, `next`;
  - `loadError`.

  Put placeholders now for keys that US2 and US3 will use: `allLabels` ("Todas las etiquetas"), `labelFilter`, `periodFilter`, `periodAll`, `period30`, `period90`.
- [X] T010 [P] [US1] Create `front-end/src/components/admin/students/StudentMistakesBox.tsx`, a `<button>` stat card that mirrors the Tareas card in `front-end/src/routes/panel_.alumnos.$studentId.tsx` (same classes, `aria-expanded`, `ring-2 ring-primary/30` when expanded).
  - It shows `allTimeMistakeCount` (or "…" while loading), the title, and the expand or collapse hint.
  - Props: `count: number | null`, `expanded: boolean`, `onToggle: () => void`.
- [X] T011 [US1] Create `front-end/src/components/admin/students/StudentMistakesList.tsx`.
  - **Props**: `data: StudentMistakes | null`, `loading`, `error`, `onPageChange(page)`, `onOpen(entry)`. Filter props are added in US2 and US3.
  - **Summary**: the `summary` line.
  - **Empty states** (FR-013): `answeredCount === 0` → `emptyNone`; `mistakeCount === 0` → `emptyNoMistakes`, still showing the answered count.
  - **Entries**: each entry is a `QuestionCard` with a header row showing the homework title, a label chip per entry label (none when empty), the formatted `submittedAt` date (Intl `es`, as `formatDate` does in the profile route) and an "Abrir tarea" button calling `onOpen(entry)`.
  - **Answer display**: below the header, `<QuestionResultBlock question={entry.question} result={entry.result} number={entry.questionPosition} questionCount={entry.questionPosition} showAllAnswers />`. Existing components already provide the student's vs the correct answer, per-part marks and the "sin respuesta" marker.
  - **Pagination**: Shadcn `Pagination` with Anterior and Siguiente plus "Página X de Y". Disable Anterior on page 1 and Siguiente on the last page, and render no controls when `totalPages <= 1` (FR-011).
- [X] T012 [US1] Wire the section into `front-end/src/routes/panel_.alumnos.$studentId.tsx`:
  - (a) Add state for `mistakes`, `mistakesLoading`, `mistakesError`, `erroresExpanded` (independent of `tareasExpanded`) and `mistakesPage`.
  - (b) Add a `loadMistakes()` that calls `getStudentMistakes(studentId, { page: mistakesPage })`. Call it in the `[studentId]` effect, whenever the page changes, and inside `reloadProfile()` (FR-015).
  - (c) Change the stats grid to `grid grid-cols-2 gap-4 sm:grid-cols-4` and add `<StudentMistakesBox>` as the 4th box.
  - (d) Below the grid, render the existing Tareas list block (unchanged) and then, if `erroresExpanded`, `<StudentMistakesList>` in a `mt-4` wrapper. Both can be open at once, Tareas first (FR-001a).
  - (e) `onOpen(entry)`: `isExerciseResultFormat(entry.homeworkFormat)` → `setOpenResultId(entry.submissionId)`, otherwise `setOpenSubmissionId(entry.submissionId)`. This reuses the existing dialogs (research R9).

**Checkpoint**: US1 is fully usable on its own (the MVP). Run the quickstart scenarios listed above.

---

## Phase 4: User Story 2 - Teacher filters the mistakes by label (Priority: P2)

**Goal**: A label selector ("Todas las etiquetas" plus each case-insensitive label group on the student's auto-graded submissions). It narrows the summary and list, resets to page 1, and falls back to "Todas" when the selected label disappears.

**Independent Test**: Run `quickstart.md` scenarios 7, 8, 12 and 13. Selecting "Subjuntivo" shows only the entries from homework labelled `Subjuntivo` or `subjuntivo`, with the summary recomputed, and there is no "sin etiqueta" option.

### Tests for User Story 2

- [X] T013 [P] [US2] Extend `back-end/src/test/java/com/kuky/backend/admin/StudentMistakesAdminServiceTest.java`:
  - label options are case-insensitive: `Subjuntivo` and `subjuntivo` give one option whose label is the first spelling seen newest-first, sorted by key, with no unlabelled option;
  - the options come from all periods;
  - a label filter matches any of a homework's several labels;
  - "all labels" includes unlabelled homework;
  - the summary is recomputed under the filter;
  - an unknown key gives `appliedLabel` null and an unfiltered result.

### Implementation for User Story 2

- [X] T014 [US2] In `back-end/src/main/java/com/kuky/backend/admin/service/StudentMistakesAdminService.java`:
  - Build `labelOptions` from all qualifying rows. Walk them newest-first and key each label as `trim().toLowerCase(Locale.ROOT)`. The first spelling wins; skip blank labels; sort by key (research R7).
  - If `labelKey` matches an option, keep only rows whose current labels contain that key and set `appliedLabel = key`. Otherwise treat the request as "all labels" with `appliedLabel = null`.
  - Compute the summary and the entries from the filtered rows. `allTimeMistakeCount` stays unfiltered.
- [X] T015 [US2] Add label filter props to `front-end/src/components/admin/students/StudentMistakesList.tsx`: `labelKey: string | null` and `onLabelChange(key: string | null)`. Render a Shadcn `Select` with "Todas las etiquetas" (sentinel value `"ALL"` → null) plus `data.labelOptions`, shown above the summary.
- [X] T016 [US2] In `front-end/src/routes/panel_.alumnos.$studentId.tsx`:
  - add `mistakesLabel` state and pass it to `getStudentMistakes`;
  - when the label changes, set the page to 1 and refetch;
  - after each response, if `mistakesLabel !== null && data.appliedLabel === null`, set `mistakesLabel` to null (FR-012a fallback, research R6).

**Checkpoint**: US1 and US2 both work independently.

---

## Phase 5: User Story 3 - Teacher narrows the period (Priority: P3)

**Goal**: A period selector (Todo / Últimos 30 días / Últimos 90 días) that combines with the label filter and resets to page 1. The box count stays all-time.

**Independent Test**: Run `quickstart.md` scenario 9. A submission from 10 days ago appears under 30 days, 90 days and all time; one from 120 days ago appears only under all time.

### Tests for User Story 3

- [X] T017 [P] [US3] Extend `back-end/src/test/java/com/kuky/backend/admin/StudentMistakesAdminServiceTest.java`. Inject a fixed `java.time.Clock` and check:
  - `DAYS_30` and `DAYS_90` apply at the boundary: `submittedAt` exactly at `now − n days` is included;
  - an unknown period value becomes `ALL`, reported back in `appliedPeriod`;
  - the period and label filters combine;
  - `allTimeMistakeCount` and `labelOptions` do not change with the period;
  - an empty period still reports the answered count as 0.

### Implementation for User Story 3

- [X] T018 [US3] In `back-end/src/main/java/com/kuky/backend/admin/service/StudentMistakesAdminService.java`:
  - Accept a `Clock`: inject a `Clock` bean if one exists; otherwise add a constructor overload so tests can pass one, while production uses `Clock.systemUTC()`.
  - Parse `period` into `ALL`, `DAYS_30` or `DAYS_90`. Before the label filter, keep only rows with `submittedAt >= now − n days`, and return `appliedPeriod`.
- [X] T019 [US3] Add `period: MistakePeriod` and `onPeriodChange(p)` props to `front-end/src/components/admin/students/StudentMistakesList.tsx`, rendered as a second Shadcn `Select` next to the label filter.
- [X] T020 [US3] In `front-end/src/routes/panel_.alumnos.$studentId.tsx`, add `mistakesPeriod` state (default `"ALL"`) and pass it to `getStudentMistakes`. When the period changes, set the page to 1 and refetch. Keep `StudentMistakesBox` bound to `allTimeMistakeCount` (FR-001).

**Checkpoint**: All three stories are functional and combine.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T021 [P] Add a **Common errors** bullet under "Key implementation notes › Back-end" in `CLAUDE.md`. It should cover:
  - the admin-only `GET /api/v1/admin/students/{id}/mistakes`, derived on read from submission snapshots, stored scores and current labels, with no table;
  - auto-graded homework only;
  - numbered items counted per item but shown as one entry;
  - read-only, no notification side effects.

  Also update the `/panel` row in "Current pages" to mention the Errores frecuentes box.
- [X] T022 [P] Run `cd front-end && npm run lint && npm run format` and fix any findings in the touched files: `front-end/src/lib/admin.ts`, `front-end/src/components/admin/students/StudentMistakesBox.tsx`, `front-end/src/components/admin/students/StudentMistakesList.tsx`, `front-end/src/routes/panel_.alumnos.$studentId.tsx` and the three locale files.
- [X] T023 Run `cd back-end && ./gradlew build`. It compiles and runs `StudentMistakesAdminServiceTest` locally; integration tests are skipped locally and run in CI.
- [X] T024 Verify in a running browser, as the constitution requires. Run every scenario in `specs/052-student-common-errors/quickstart.md`, including the 2×2 layout at phone width (375 px), dark mode, and the `curl` security check. Record any deviations and fix them before marking this complete.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (T001)**: no dependencies.
- **Foundational (T002–T006)**: depends on T001 and blocks every story.
  - T002, T003 and T006 are `[P]` (separate files).
  - T004 needs T002 and T003.
  - T005 needs T004.
- **US1 (T007–T012)**: depends on Foundational.
- **US2 (T013–T016)**: depends on Foundational and edits the files US1 created (`StudentMistakesList.tsx`, the profile route). Do it after US1.
- **US3 (T017–T020)**: same situation as US2. Do it after US2, because it touches the same service and component and filters are applied period-first, then label.
- **Polish (T021–T024)**: after the stories you intend to ship.

### Within each story

- The back-end test task is written first (it is `[P]`, in a separate file).
- Then the service change, then the component, then the route wiring.
- In US1, T009 and T010 are independent `[P]` files. T011 uses the i18n keys from T009. T012 needs T010 and T011.

## Parallel Examples

**Foundational**:
```text
T002 HomeworkSubmissionRepository.findAutoGradedSubmittedByUser
T003 StudentMistakesResponse DTO
T006 front-end/src/lib/admin.ts client + types
```

**User Story 1**:
```text
T007 StudentMistakesAdminServiceTest (derivation, numbered, paging)
T008 StudentAdminControllerIntegrationTest (/mistakes auth + 404)
T009 i18n keys es/en/ro
T010 StudentMistakesBox.tsx
```

**User Story 2 / 3**: the test tasks (T013, T017) can be written in parallel with the front-end component edits of the same story. The service edits come in order.

## Implementation Strategy

### MVP first (US1 only)

1. T001, then T002–T006.
2. T007–T012.
3. **Stop and validate**: quickstart scenarios 1–6, 10, 11 and 14–18. Paula can already see every mistake newest-first and open any homework. That is shippable.

### Incremental delivery

4. US2 (label filter) → quickstart 7, 8, 12, 13 → ship.
5. US3 (period filter) → quickstart 9 → ship.
6. Polish (T021–T024).
