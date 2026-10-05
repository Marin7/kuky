---

description: "Task list for Spot the Wrong Words (SPOT_WRONG_WORDS question kind)"
---

# Tasks: Spot the Wrong Words (new question kind)

**Input**: Design documents from `specs/054-spot-wrong-words/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/spot-wrong-words-api.md, quickstart.md

**Tests**: The spec doesn't ask for TDD. The plan names a back-end unit test (`SpotWrongWordsTest`) as the place where the shared tokenizer vectors are pinned, so this list includes back-end test tasks only. The front end has no test runner; it is verified in the browser with quickstart.md, as the constitution requires.

**Organization**: Tasks are grouped by user story. US1 and US2 are both P1. US2 (authoring) comes first because a student can only take a question that has been written; US1 can still be tested on its own by creating the question through the admin API (contract §Authoring).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: US1 = student finds wrong words, US2 = teacher authors, US3 = teacher reviews / Errores frecuentes

## Path Conventions

Web app: `back-end/src/main/java/com/kuky/backend/…`, `back-end/src/test/java/com/kuky/backend/…`, `front-end/src/…`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Let the database accept the new kind.

- [X] T001 Create `back-end/src/main/resources/db/migration/V27__spot_wrong_words_kind.sql`. It must drop and re-add `homework_questions_kind_check`, `activity_questions_kind_check` and `quiz_questions_kind_check`, each with `CHECK (kind IN ('SINGLE_CHOICE','MULTI_CHOICE','MULTI_BLANK','DRAG_DROP','TABLE_FILL','MATCHING','TRUE_FALSE','FREE_TEXT','SPOT_WRONG_WORDS'))`. Follow the style of `V15__manual_free_text_questions.sql` and start with a header comment citing feature 054.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Add the kind constant, the shared word-segmentation rule and the type definitions every story needs.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Add `SPOT_WRONG_WORDS` to `back-end/src/main/java/com/kuky/backend/learning/model/QuestionKind.java`: append the constant, document it in the class Javadoc list ("passage in prompt; wrong words by word index in structure_json; student toggles words, capped at error count") and include it in `isStructured()`.
- [X] T003 [P] Add `MAX_SPOT_ERRORS = 30`, `MAX_SPOT_TEXT_CHARS = 3000` and `MAX_SPOT_CORRECTION_CHARS = 100`, with Javadoc citing `specs/054-spot-wrong-words`, to `back-end/src/main/java/com/kuky/backend/learning/ExerciseStructureLimits.java`.
- [X] T004 Create `back-end/src/main/java/com/kuky/backend/learning/service/SpotWrongWords.java`, a final utility class with:
  - `public static List<String> tokenize(String text)` using `Pattern.compile("[\\p{L}\\p{M}\\p{N}]+(?:['’\\-][\\p{L}\\p{M}\\p{N}]+)*")`, returning the matched words in order.
  - `public static int errorCount(JsonNode structure)`, which reads the size of the `errors` array.
  - A Javadoc comment stating that the regex must stay identical to `front-end/src/lib/spotWrongWords.ts` (research R4).
- [X] T005 [P] Create `front-end/src/lib/spotWrongWords.ts` with:
  - The word regex `/[\p{L}\p{M}\p{N}]+(?:['’-][\p{L}\p{M}\p{N}]+)*/gu`.
  - `tokenize(text): string[]`.
  - `segment(text): { text: string; wordIndex: number | null }[]`, which returns words and the gaps between them verbatim, in order, so the text can be rendered with newlines and punctuation preserved.
  - A header comment stating that the regex must stay identical to `SpotWrongWords.java`.
- [X] T006 [P] Add `"SPOT_WRONG_WORDS"` to the `QuestionKind` union in `front-end/src/lib/learning.ts`. In the same file:
  - Add `errorCount?: number; // SPOT_WRONG_WORDS` to `StudentStructure`.
  - Add `export interface SpotWrongWordsAnswer { selected: number[] }` next to the other answer shapes.
  - Document on `UnitResult` that for SPOT_WRONG_WORDS `index` is the word index and `label === "EXTRA"` marks a wrongly marked word.
- [X] T007 [P] Add `"SPOT_WRONG_WORDS"` to the `QuestionKind` union in `front-end/src/lib/admin.ts`. Add `export interface SpotWrongWordsStructure { errors: { wordIndex: number; word: string; correction: string | null }[] }` and include it in the `QuestionStructure` union.
- [X] T008 [P] Add the new i18n keys to `front-end/src/i18n/locales/es.ts`, `front-end/src/i18n/locales/en.ts` and `front-end/src/i18n/locales/ro.ts`:
  - Kind name under the existing `kind` map: `SPOT_WRONG_WORDS` = "Encontrar errores" / "Spot the mistakes" / "Găsește greșelile".
  - Editor labels: hint, clickable-view heading, correction placeholder, "no marks yet".
  - Student labels: instruction "Encuentra los {{count}} errores", counter "{{marked}} / {{count}} marcadas", cap hint "Desmarca una palabra para marcar otra".
  - Result legend: found, missed, "marcada por error".

**Checkpoint**: The kind exists end to end in types and the database, and both tokenizers exist.

---

## Phase 3: User Story 2 — Teacher authors a spot-the-wrong-words question (Priority: P1)

**Goal**: The teacher can create, preview, save and reopen the question in homework, activities and quizzes, with marks kept correctly when the text is edited.

**Independent Test**: Quickstart scenarios 1–4. Create a question, mark 4 words (2 with corrections), save, reopen, and check that everything is preserved. Saving with 0 marks is refused. Editing the text keeps the marks on unchanged words.

### Tests for User Story 2

- [X] T009 [P] [US2] Create `back-end/src/test/java/com/kuky/backend/learning/SpotWrongWordsTest.java` with a `tokenize` test for every vector in the `specs/054-spot-wrong-words/data-model.md` table: `¿Dónde está el baño?`, `l'hotel`, `bien-estar`, `1990`, the em-dash case, `rock - and`, `d’Artagnan`, `la la la`, and the paragraph case.

### Implementation for User Story 2

- [X] T010 [US2] In `back-end/src/main/java/com/kuky/backend/admin/service/HomeworkAdminService.java`, add `case SPOT_WRONG_WORDS -> validateSpotWrongWords(prompt, structure)` to `validateStructure`. The new private method must:
  - Reject a prompt longer than `MAX_SPOT_TEXT_CHARS`, or with 0 words.
  - Require an `errors` array of 1–`MAX_SPOT_ERRORS` entries.
  - For each entry, require an integer `wordIndex` within range, require `word` to equal `SpotWrongWords.tokenize(prompt).get(wordIndex)`, and reject duplicate indices.
  - Trim each correction, turning blank into `null`, and cap it at `MAX_SPOT_CORRECTION_CHARS`.
  - Return `{ errors: [...] }` sorted by `wordIndex`.
  - Throw `IllegalArgumentException` with the exact Spanish messages from `data-model.md`.

  Activities and quizzes reach this validator through `ActivityAdminService` and `QuizAdminService`, so they need no change.
- [X] T011 [P] [US2] Add `carryMarks(oldText, newText, errors)` to `front-end/src/lib/spotWrongWords.ts`. It runs an LCS over `tokenize(oldText)` and `tokenize(newText)` by token text. Each error whose old token is matched moves to its new index, keeping its correction and the new word. Unmatched errors are dropped. The result is sorted by `wordIndex` (research R10).
- [X] T012 [P] [US2] Update `front-end/src/components/admin/homework/questionDefaults.ts`: include `SPOT_WRONG_WORDS` in `isStructuredKind`, and return `{ errors: [] }` from `defaultStructureForKind` for it.
- [X] T013 [US2] Create `front-end/src/components/admin/homework/SpotWrongWordsEditor.tsx` (props: `prompt`, `structure: SpotWrongWordsStructure`, `onChange(prompt, structure)`) with:
  - A textarea for the text that calls `carryMarks` on every change.
  - Below it, a clickable view rendered from `segment(prompt)`. Each word is a `<button type="button">`; marked words are highlighted with a non-colour cue such as a strikethrough or icon. Clicking a word toggles it, and unmarking discards its correction.
  - A list of the marked words in text order, each with an optional correction `Input` (maxLength 100).
  - A "N marcadas" count and a hint when there are no marks.
  - Mobile-friendly wrapping with no horizontal scroll at 375 px.

  Use the Shadcn `Textarea`/`Input` and the i18n keys from T008.
- [X] T014 [US2] Update `front-end/src/components/admin/homework/QuestionEditorCard.tsx`: add `"SPOT_WRONG_WORDS"` to the kind list (line ~45). When the kind is selected, render `SpotWrongWordsEditor` in place of the plain prompt field, feeding `question.prompt`/`question.structure` and writing both back through the existing question `onChange`. Follow how the `MULTI_BLANK` branch uses the prompt as its passage.
- [X] T015 [US2] Create `front-end/src/components/learning/SpotWrongWordsQuestion.tsx` (props: `prompt`, `errorCount`, `selected: number[]`, `onToggle(index)`, `disabled?`). It renders:
  - The instruction "Encuentra los N errores" and the counter "x / N marcadas".
  - The text from `segment(prompt)`, with each word as a `<button type="button" aria-pressed>`, a min tap height of about 32 px and inline wrapping. Gaps render with `whitespace-pre-wrap` so paragraphs are kept.
  - At the cap, a click on an unmarked word does nothing and shows the cap hint.

  This component also serves the read-only admin preview when `disabled` is set.
- [X] T016 [US2] Update `front-end/src/components/admin/homework/HomeworkPreview.tsx`:
  - In `hidesPromptLabel`, return true for `SPOT_WRONG_WORDS`.
  - In `toStudentStructure`, map the admin structure to `{ errorCount: errors.length }` and nothing else.
  - Add a render branch next to the `MATCHING` one that shows `SpotWrongWordsQuestion` with `selected=[]`, `disabled` and a no-op toggle.

**Checkpoint**: Quickstart scenarios 1–4 pass in homework, quiz editor (`QuizEditorPage`) and activity editor, all of which share `QuestionEditorCard`. Saving returns the normalised structure.

---

## Phase 4: User Story 1 — Student finds the wrong words in a text (Priority: P1) 🎯 MVP

**Goal**: The student takes the question with the cap in place, and their draft is kept. The answer is auto-graded on submit (found ÷ errors, one question) and the result is shown inline in the text.

**Independent Test**: Quickstart scenarios 5–9. Mark words, hit the cap, reload to restore the draft, submit, check the 50% score and the inline found/missed/extra states, check the mixed-homework lifecycle and the server-side cap rejection, and repeat in a quiz and an activity.

### Tests for User Story 1

- [X] T017 [P] [US1] Extend `back-end/src/test/java/com/kuky/backend/learning/SpotWrongWordsTest.java` with `grade` tests:
  - Scores: all found = 1.0; half found plus one extra = 0.5; nothing selected = 0.0.
  - Out-of-range and duplicate indices are ignored.
  - More valid indices than errors throws `IllegalArgumentException`.
  - Unit states, ordering by word index, `label = "EXTRA"`, and `expectedDisplay` holding `[correction]` or `[]`.
- [X] T018 [P] [US1] Add a `SPOT_WRONG_WORDS` case to `back-end/src/test/java/com/kuky/backend/learning/ExerciseGradingServiceTest.java`, following the existing `structured(QuestionKind.MATCHING, …)` test. It must show that the student question structure is exactly `{ "errorCount": n }` and that grading through the service gives the expected score.

### Implementation for User Story 1

- [X] T019 [US1] Add `public static QuestionScoring.GradedAnswer grade(HomeworkQuestion q, JsonNode answerJson, ObjectMapper om)` to `back-end/src/main/java/com/kuky/backend/learning/service/SpotWrongWords.java`. It must:
  - Read `errors` from the structure, and read `selected` from the answer.
  - Keep only integers in `[0, tokenize(prompt).size())` and de-duplicate them.
  - If more remain than there are errors, throw `IllegalArgumentException("Has marcado más palabras que errores.")`.
  - Score = found ÷ errorCount.
  - Build `unitResults` per data-model.md (found, missed and `EXTRA` units, ordered by word index).
  - Store the normalised `{ "selected": [...] }` as the answer JSON.
- [X] T020 [US1] In `back-end/src/main/java/com/kuky/backend/learning/service/QuestionScoring.java`, add `case SPOT_WRONG_WORDS -> SpotWrongWords.grade(q, answerJsonOf(given), objectMapper)` to the `grade` switch. This covers homework exercises and quizzes.
- [X] T021 [US1] In `back-end/src/main/java/com/kuky/backend/learning/service/ExerciseGradingService.java`, add `case SPOT_WRONG_WORDS -> result.put("errorCount", SpotWrongWords.errorCount(structure))` to `stripStructureForStudent`. Quizzes reuse this through `studentQuestionsFor`.
- [X] T022 [US1] Update `back-end/src/main/java/com/kuky/backend/learning/service/ActivityExerciseGradingService.java`:
  - In `gradeQuestion` (line ~263), add a `SPOT_WRONG_WORDS` case that delegates to `SpotWrongWords.grade` and maps the result onto the local `GradedAnswer` record.
  - In its `stripStructureForStudent` (line ~541), add the same `{ errorCount }` case.
- [X] T023 [US1] Update `front-end/src/components/learning/ExerciseForm.tsx`:
  - Add `selected: number[]` to the per-question answer state, initialised to `[]`; it is saved and restored through the existing draft mechanism.
  - Add a toggle handler that enforces the cap with `structure.errorCount`.
  - In submit, add `else if (q.kind === "SPOT_WRONG_WORDS") answerJson = { selected: a?.selected ?? [] }`.
  - Add a render branch with `SpotWrongWordsQuestion` next to the `MATCHING` branch.
  - Hide the prompt label for this kind, as for `MULTI_BLANK`.
- [X] T024 [US1] Make the same changes in `front-end/src/components/learning/MixedHomeworkForm.tsx`: answer state init (~line 105), payload (~line 265), render branch (~line 619) and prompt-label hiding. This form is used by mixed homework and by quizzes (`front-end/src/routes/quizzes_.$quizId.tsx`).
- [X] T025 [US1] Create `front-end/src/components/learning/SpotWrongWordsResult.tsx` (props: `prompt`, `unitResults`, `compact?`). It renders `segment(prompt)` inline:
  - Units with `correct` get the found style (✓ icon plus success tone).
  - Units with `!correct && studentDisplay == null` get the missed style (✗ icon plus underline).
  - Units with `label === "EXTRA"` get the "marcada por error" style (dotted outline plus "!" icon).
  - Found and missed words are followed by `→ correction` when `expectedDisplay[0]` exists.
  - A small legend.

  The three states must not rely on colour alone (FR-014).
- [X] T026 [US1] Update `front-end/src/components/learning/ExerciseResult.tsx`: add a branch for `question?.kind === "SPOT_WRONG_WORDS"` that renders `SpotWrongWordsResult` with `question.prompt` and `result.unitResults`, placed beside the `TABLE_FILL` branch (~line 193), and hide the generic prompt for this kind. This covers homework, quiz and activity results and the admin review dialogs (`HomeworkReviewDialog`, `QuizReviewDialog`, `ActivityReviewDialog`) that reuse `ExerciseResult`. Confirm each of those dialogs renders it.

**Checkpoint**: Quickstart scenarios 5–9 pass. US1 and US2 together are the MVP.

---

## Phase 5: User Story 3 — Teacher reviews results and common errors (Priority: P2)

**Goal**: The teacher sees the per-word breakdown in submission review and in "Errores frecuentes", where the question counts as one mistake.

**Independent Test**: Quickstart scenarios 10–11. A submission with a missed error appears once in Errores frecuentes with the inline result; a fully correct one does not; editing the question afterwards leaves past results unchanged.

### Implementation for User Story 3

- [X] T027 [US3] Update `front-end/src/components/admin/students/MistakeOnlyDetail.tsx`: add a `SPOT_WRONG_WORDS` branch next to the `MULTI_BLANK`/`DRAG_DROP` branch (~line 122) that renders `<SpotWrongWordsResult compact prompt={question.prompt} unitResults={result.unitResults} />` from the snapshot question. No back-end change is needed: `StudentMistakesAdminService.answeredAndWrong` already counts a non-numbered kind once (research R11).
- [X] T028 [US3] Check that the submission review shows found, missed and extra words for this kind in `front-end/src/components/admin/homework/HomeworkReviewDialog.tsx`, `front-end/src/components/quiz/admin/QuizReviewDialog.tsx` and `front-end/src/components/admin/activities/ActivityReviewDialog.tsx`. If a dialog lists auto-graded questions through a per-kind path that skips `ExerciseResult`, add the `SpotWrongWordsResult` branch there.

**Checkpoint**: All three user stories work independently.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T029 [P] In the "Key implementation notes → Back-end" section of `CLAUDE.md`, add one line to the homework grading note listing `SPOT_WRONG_WORDS` among the auto-graded kinds: passage in the prompt, wrong words by word index, cap equal to the error count, score found ÷ errors as one question, and the shared tokenizer rule in `SpotWrongWords.java` / `lib/spotWrongWords.ts`.
- [X] T030 Run `./gradlew build` in `back-end/` and `npm run lint && npm run format && npm run build` in `front-end/`, and fix any issues.
- [ ] T031 Run every scenario in `specs/054-spot-wrong-words/quickstart.md` (1–13) in the browser, including 375 px width and the en/ro locales, and record the results. The constitution requires browser verification before UI tasks are complete.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (T001)**: none.
- **Foundational (T002–T008)**: depends on T001 only for running the app; the code tasks themselves are independent. Blocks all stories.
- **US2 (Phase 3)**: after Foundational.
- **US1 (Phase 4)**: after Foundational. Its back-end tasks (T017–T022) don't depend on US2. Its front-end tasks reuse `SpotWrongWordsQuestion` from T015, so finish T015 first or move it here when working on US1 alone. End-to-end testing needs either US2 or a question created through the admin API.
- **US3 (Phase 5)**: needs `SpotWrongWordsResult` (T025) and submissions from US1.
- **Polish**: after the stories you intend to ship.

### Within-story order

- T010 depends on T002–T004. T013 depends on T005, T007, T011 and T012. T014 depends on T013. T016 depends on T015.
- T019 depends on T004. T020–T022 depend on T019. T023–T024 depend on T006 and T015. T026 depends on T025.

### Parallel Opportunities

- Foundational: T002, T003, T005, T006, T007 and T008 all touch different files. T004 can run with all of the front-end ones.
- US2: T009, T011 and T012 run in parallel. The back-end T010 runs alongside the front-end T013–T016.
- US1: T017 and T018 run in parallel. The back-end T019–T022 run alongside the front-end T023–T026; within the front end, T025 can start alongside T023 and T024.

## Parallel Example: User Story 1

```text
# Back end and front end side by side:
T019 SpotWrongWords.grade  →  T020 QuestionScoring  →  T021 strip  →  T022 Activity grader
T025 SpotWrongWordsResult.tsx  ‖  T023 ExerciseForm.tsx  ‖  T024 MixedHomeworkForm.tsx  →  T026 ExerciseResult.tsx
# Tests alongside:
T017 SpotWrongWordsTest (grade)  ‖  T018 ExerciseGradingServiceTest
```

## Implementation Strategy

### MVP (US2 + US1, both P1)

1. Phase 1 and Phase 2.
2. Phase 3 (US2): authoring works in all three editors.
3. Phase 4 (US1): students can take the question and see results. **Stop and validate** with quickstart scenarios 1–9. This can ship.

### Incremental delivery

4. Phase 5 (US3): Errores frecuentes and checking the review dialogs.
5. Phase 6: docs note, build, full quickstart pass.
