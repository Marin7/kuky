# Tasks: Sentence-Starter Answer (new manual question kind)

**Input**: Design documents from `specs/057-sentence-starter-answer/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/sentence-starter-api.md](./contracts/sentence-starter-api.md), [quickstart.md](./quickstart.md)

**Tests**: Not TDD. One unit test class for the pure helper (`SentenceStartersTest`), mirroring `SpotWrongWordsTest` from 054; everything else is verified in the browser per quickstart.md.

**Organization**: All three stories are P1 and together form the MVP. US2 (authoring) is ordered first because US1 (answering) needs a question to exist; US3 (correction/results) builds on submitted answers.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: US1 = student answers, US2 = teacher authors, US3 = correction & results

Paths: back-end Java under `back-end/src/main/java/com/kuky/backend/`, front-end under `front-end/src/`.

---

## Phase 1: Setup

- [X] T001 Create migration `back-end/src/main/resources/db/migration/V33__sentence_starter_kind.sql`: drop and re-add `homework_questions_kind_check` and `activity_questions_kind_check` with the full kind list from `V27__spot_wrong_words_kind.sql` plus `'SENTENCE_STARTER'`; do **not** touch `quiz_questions_kind_check`. Header comment: "Sentence-starter question kind (feature 057-sentence-starter-answer). Manual grading; starters in structure_json."

---

## Phase 2: Foundational (blocks all stories)

- [X] T002 Add `SENTENCE_STARTER` to `learning/model/QuestionKind.java` (javadoc bullet: "ordered `{starter, modelAnswer?}` items in `structure_json`; student writes each continuation; manual grading like FREE_TEXT") and add `public boolean isManual() { return this == FREE_TEXT || this == SENTENCE_STARTER; }`. `isStructured()` stays unchanged (false for the new kind).
- [X] T003 [P] Add constants to `learning/ExerciseStructureLimits.java`: `MAX_STARTERS = 10`, `MAX_STARTER_CHARS = 80`, `MAX_MODEL_ANSWER_CHARS = 300`, `MAX_CONTINUATION_CHARS = 500`, `MAX_SENTENCE_STARTER_ANSWER_CHARS = 2000`.
- [X] T004 Create `learning/service/SentenceStarters.java` (final utility class, Jackson `ObjectMapper`) with:
  - `static String normalizeStarter(String raw)` — strip; repeatedly remove a trailing `...` or `…` and trailing whitespace.
  - `static String normalizeStructure(JsonNode structure)` — reads `items[]` (`starter`, optional `modelAnswer`), normalises starters, drops blank model answers and unknown keys, enforces 1–`MAX_STARTERS` items, starter 1–80 chars ("Cada inicio de frase necesita texto." / "Cada inicio de frase admite como máximo 80 caracteres."), model answer ≤ 300 ("La respuesta modelo admite como máximo 300 caracteres."), "Añade al menos un inicio de frase." / "Una pregunta admite como máximo 10 inicios de frase."; returns the normalised JSON string `{"items":[…]}`; throws `IllegalArgumentException`.
  - `static List<String> starters(String structureJson)` and `static List<String> modelAnswers(String structureJson)` (nulls for absent).
  - `static ObjectNode studentStructure(String structureJson)` → `{"starters":[…]}`.
  - `static String compose(String starter, String continuation)` — continuation stripped, `\r?\n` runs collapsed to one space; no space when it starts with one of `, . ; : ! ? …`.
  - `record Composed(String answerText, String answerJson)` and `static Composed composeAnswer(String structureJson, List<String> continuations)` — size mismatch → "Las respuestas no coinciden con las preguntas actuales."; blank continuation → "Debes responder a todas las preguntas."; continuation > 500 or total > 2000 → "Una de las respuestas es demasiado larga."; `answerText` = composed lines joined by `\n`; `answerJson` = `{"starters":[…],"modelAnswers":[…],"continuations":[…]}`.
  - `static List<String> continuationsFrom(JsonNode answerJson)` — reads `continuations` array (null/absent → empty list).
- [X] T005 [P] Create `back-end/src/test/java/com/kuky/backend/learning/SentenceStartersTest.java` covering: `normalizeStarter("porque...")`/`("porque …")` → `"porque"`; `compose("porque", " quiere ir.")` → `"porque quiere ir."`; `compose("Si yo fuera tú", ", iría.")` → `"Si yo fuera tú, iría."`; newline collapse; structure limits (0 items, 11 items, 81-char starter, blank starter, 301-char model answer); `composeAnswer` mismatch / blank / 501 chars / total > 2000; `studentStructure` contains no `modelAnswer`.
- [X] T006 Replace "manual" checks with `isManual()` in `learning/service/HomeworkCompositionSupport.java`: `isAutoGradable` → `kind != null && !kind.isManual()`; `composition` → `kind.isManual()` sets `hasManual`; `contributions` → teacher-% branch for `question.getKind().isManual()`.
- [X] T007 [P] In `learning/service/QuestionScoring.java` and `learning/service/ActivityExerciseGradingService.java`, add `SENTENCE_STARTER` to the "no se califica automáticamente" guard case next to `FREE_TEXT` (switch exhaustiveness), and in `ActivityExerciseGradingService`'s student-structure strip add a `SENTENCE_STARTER` branch returning `SentenceStarters.studentStructure(...)`.
- [X] T008 [P] In `learning/service/ExerciseGradingService.java`: `stripStructureForStudent` returns `SentenceStarters.studentStructure(q.getStructureJson())` for `SENTENCE_STARTER`; every filter that skips FREE_TEXT when grading structured subsets (`kind == FREE_TEXT` / `!= FREE_TEXT`) uses `isManual()`.
- [X] T009 [P] Front-end types in `front-end/src/lib/learning.ts`: add `"SENTENCE_STARTER"` to the student `QuestionKind` union; `StudentStructure.starters?: string[]`; export `isManualKind(kind) => kind === "FREE_TEXT" || kind === "SENTENCE_STARTER"` and use it in the composition helper (lines ~126-127 `hasFree`/`hasAuto`); add `continuations?: string[]` to the heterogeneous answer payload and to the ALL_MANUAL answer payload type; add `starters?: string[] | null` and `modelAnswers?: (string | null)[] | null` to the manual answer view type (both near line 101 and 521 comments "Submitted answers").
- [X] T010 [P] Front-end admin types in `front-end/src/lib/admin.ts`: add `"SENTENCE_STARTER"` to `QuestionKind` (~line 433); `SentenceStarterStructure = { items: { starter: string; modelAnswer?: string }[] }` in the question structure union; `starters`/`modelAnswers` on the admin manual-answer view type (~line 738).
- [X] T011 [P] Create `front-end/src/lib/sentenceStarter.ts`: `normalizeStarter` and `compose` mirroring T004 (preview only; server authoritative), constants `MAX_STARTERS=10`, `MAX_STARTER_CHARS=80`, `MAX_MODEL_ANSWER_CHARS=300`, `MAX_CONTINUATION_CHARS=500`, and `splitAnswerLines(text: string): string[]` (split on `\n`).

**Checkpoint**: Back-end compiles with the new kind treated as manual everywhere composition/scoring is decided; `SentenceStartersTest` passes.

---

## Phase 3: User Story 2 — Teacher authors a sentence-starter question (P1) 🎯 MVP part 1

**Goal**: Teacher adds/edits/reorders starters (+ optional model answers) in homework and page activities, previews, saves; quizzes cannot use the kind.

**Independent Test**: quickstart.md scenario 1 (and activity part of scenario 5, scenario 6).

- [X] T012 [US2] In `admin/service/HomeworkAdminService.java#validateAndMapQuestions` (~line 713-760): add a `SENTENCE_STARTER` branch before the structured branch — reject options ("Este tipo de pregunta no admite opciones."), `model.setStructureJson(SentenceStarters.normalizeStructure(q.structure()))`, `model.setOptions(List.of())`; extend `promptOptional` so `freeTextPromptOptional && kind.isManual()` (prompt optional for activity sentence-starter questions).
- [X] T013 [P] [US2] In `quiz/service/QuizAdminService.java#mapQuestions` (~line 326 loop) reject `"SENTENCE_STARTER"` (case-insensitive) with `IllegalArgumentException("Este tipo de pregunta no está disponible en las pruebas.")` before delegating to `validateAndMapQuestions`.
- [X] T014 [P] [US2] In `front-end/src/components/admin/homework/questionDefaults.ts`: default structure for `SENTENCE_STARTER` = `{ items: [{ starter: "" }] }`; default options `[]` (extend the `isStructuredKind(kind) || kind === "FREE_TEXT"` check at line 21 to manual kinds).
- [X] T015 [US2] Create `front-end/src/components/admin/homework/SentenceStarterEditor.tsx`: props `{ structure: SentenceStarterStructure; onChange(next) }`; one row per item with a starter `Input` (maxLength 80) and an optional "Respuesta modelo" `Input` (maxLength 300), up/down reorder buttons and remove (disabled when one item left), "Añadir inicio de frase" button disabled at 10; inline hint when a starter is empty. Shadcn `Input`/`Button`, lucide icons as in sibling editors (e.g. `MatchingEditor`).
- [X] T016 [US2] In `front-end/src/components/admin/homework/QuestionEditorCard.tsx`: add `"SENTENCE_STARTER"` to the kind list (~line 50) with label key `admin.homework.kinds.SENTENCE_STARTER`; render `SentenceStarterEditor` for the kind; hide the options block for it (like FREE_TEXT, ~line 405); prompt placeholder/optional handling as FREE_TEXT (~lines 295-297, using `freeTextPromptOptional`); add prop `excludedKinds?: QuestionKind[]` that filters the kind picker.
- [X] T017 [P] [US2] Pass `excludedKinds={["SENTENCE_STARTER"]}` to every `QuestionEditorCard`/`QuestionListEditor` used in `front-end/src/components/quiz/admin/QuizEditorPage.tsx` (thread the prop through `QuestionListEditor.tsx` if the quiz page uses it).
- [X] T018 [US2] Create `front-end/src/components/learning/SentenceStarterInput.tsx`: props `{ starters: string[]; values: string[]; onChange(index, value); disabled? }`; for each starter a row: starter text (`font-medium`, not editable) followed by a `Textarea` (rows 1, auto-grow, maxLength 500, placeholder "…") — on narrow screens the starter sits above a full-width input (`flex flex-col sm:flex-row sm:items-baseline gap-2`); per-line counter when ≥ 450 chars; no horizontal overflow.
- [X] T019 [US2] In `front-end/src/components/admin/homework/HomeworkPreview.tsx`: map the admin structure to the student shape (`starters` = `items.map(i => normalizeStarter(i.starter))`, model answers dropped) in `toStudentStructure`, and render `SentenceStarterInput` (local state) for the kind, below the prompt like FREE_TEXT (~lines 284-291).
- [X] T020 [P] [US2] Add i18n keys in `front-end/src/i18n/locales/es.ts`, `en.ts`, `ro.ts`: kind name (es "Completa la frase", en "Complete the sentence", ro "Completează propoziția"), editor labels (starter, model answer, add, remove, move up/down, limit hints), student placeholder/counter, "Respuesta modelo" result label.

**Checkpoint**: Teacher can create, preview, save and reopen the question in homework and activities; quiz editor does not offer it; API rejects it for quizzes.

---

## Phase 4: User Story 1 — Student completes sentences from given starters (P1) 🎯 MVP part 2

**Goal**: Student answers each starter (fixed text + input) in ALL_MANUAL and MIXED homework and in page activities; server composes and stores sentences.

**Independent Test**: quickstart.md scenarios 2, 4 and the student part of 5.

- [X] T021 [US1] Add `List<String> continuations` to `learning/dto/ManualAnswerDto.java` (keep a 2-arg constructor `(questionId, text)` delegating with `null` for existing callers).
- [X] T022 [US1] In `learning/service/HomeworkSubmissionService.java`:
  - `requireEveryQuestionAnswered` (~line 275): for `SENTENCE_STARTER`, validate via `SentenceStarters.composeAnswer(q.getStructureJson(), SentenceStarters.continuationsFrom(given.answerJson()))` (throws the spec'd messages).
  - `mapFreeTextFromUnified` (~line 298) and `freeTextQuestions` (~line 262): use `isManual()`; for `SENTENCE_STARTER` set `answerText`/`answerJson` from `composeAnswer`, `promptSnapshot` = prompt.
  - `toManualDtos` (~line 316): pass `continuationsFrom(answerJson)` as `continuations`.
  - `validateAndMapFreeTextAnswers` and the ALL_MANUAL filter (~line 198): accept manual kinds; for `SENTENCE_STARTER` require `continuations` and compose (ignore `text`); FREE_TEXT unchanged.
  - Make sure `HomeworkAnswerRepository.saveAll` persists `answer_json` for manual rows (it already does for structured rows — verify the insert writes `answerJson` regardless of kind).
- [X] T023 [US1] Same changes in `learning/service/ActivityStudentService.java` (`submitManual` ~line 106, `submitAnswers` ~line 146-190, manual filters ~lines 188/291/309/329, `toManualDtos` ~line 343); verify `ActivityAnswerRepository` insert persists `answer_json` for manual rows.
- [X] T024 [US1] In `learning/service/HomeworkItems.java#toQuestionDto` (~line 156): for `SENTENCE_STARTER` return `SentenceStarters.studentStructure(...)` as structure (not `{}`), so ALL_MANUAL items carry starters.
- [X] T025 [US1] In `front-end/src/components/learning/MixedHomeworkForm.tsx`: replace local `isFreeText` with `isManualKind`; add `continuations: string[]` to `AnswerState` (`initialAnswerState`: `Array(q.structure?.starters?.length ?? 0).fill("")`); render `SentenceStarterInput` for the kind in the answer branch (~line 476); completeness check (~line 230) requires every continuation non-blank; payload (~line 255) sends `answerJson: { continuations }` for the kind.
- [X] T026 [US1] In `front-end/src/components/learning/ManualMultiAnswerForm.tsx`: extend `QuestionPrompt` with `kind?` and `starters?: string[]`; keep `string[]` continuations per sentence-starter question in state; render `SentenceStarterInput` instead of the textarea for that kind; non-empty check covers every continuation; submit payload sends `{ questionId, continuations }` for the kind (both default `submitHomework` and the `submitAnswer` override).
- [X] T027 [P] [US1] Pass `kind: q.kind` and `starters: q.structure?.starters` into `ManualMultiAnswerForm` question props in `front-end/src/components/learning/HomeworkInlinePanel.tsx` (~line 223) and `front-end/src/components/learning/ActivityPanel.tsx` (~line 137); update the `submitHomework` / activity manual submit helpers' answer type in `front-end/src/lib/learning.ts` to carry `continuations`.

**Checkpoint**: Student can submit sentence-starter answers in ALL_MANUAL, MIXED and activities; DB rows hold composed lines + snapshot JSON; empty line or too-long input is refused.

---

## Phase 5: User Story 3 — Teacher corrects and the student sees the result (P1) 🎯 MVP part 3

**Goal**: Correction via the existing manual flow, starter visually distinguished, model answers shown to the student after finalisation; activity unlock/review works.

**Independent Test**: quickstart.md scenarios 3, 5 (unlock + review), 7.

- [X] T028 [US3] Add nullable `List<String> starters` and `List<String> modelAnswers` to `learning/dto/ManualAnswerViewDto.java`; extend `fromStored` with an `answerJson` + `boolean includeModelAnswers` overload that fills them from the `{"starters","modelAnswers"}` snapshot (null for non-sentence-starter answers, `modelAnswers` null unless `includeModelAnswers`); keep the existing overloads delegating with `null`.
- [X] T029 [US3] Update every `ManualAnswerViewDto.fromStored` call site to pass `answerJson` and `includeModelAnswers`: `learning/service/HomeworkItems.java#toAnswerView` (`!stripTeacherScores`), `learning/service/ActivityStudentService.java`, `learning/service/ExerciseGradingService.java`, `admin/service/HomeworkAdminService.java` and `admin/service/ActivityAdminService.java` (teacher views: `true`); check `admin/dto/HomeworkSubmissionAdminDto.java`, `learning/dto/ActivityItemResponse.java`, `HomeworkItemResponse.java`, `ExerciseResponse.java` compile unchanged.
- [X] T030 [US3] In `admin/service/HomeworkAdminService.java` (~line 328) and `admin/service/ActivityAdminService.java` (~line 269): annotation/percent eligibility uses `kindByQuestion.get(...).isManual()`; also the "every FREE_TEXT has a %" finalize check and any count of manual questions in these services.
- [X] T031 [US3] Create `front-end/src/components/learning/SentenceStarterAnswerView.tsx`: props `{ text?: string; formatted?: FormattedText | null; starters: string[]; modelAnswers?: (string | null)[] | null }`; split plain text with `splitAnswerLines` (or annotated segments at `\n`), render each line with its leading `starters[i]` in `font-semibold` when the line starts with it (split the first segment at that offset when annotated, keeping the teacher's colours via `RichTextViewer` segment rendering), and below each line "Respuesta modelo: …" (muted) when `modelAnswers?.[i]`.
- [X] T032 [US3] Use `SentenceStarterAnswerView` when `ans.starters` is present in the manual-answer result sections of `front-end/src/components/learning/MixedHomeworkForm.tsx` (~lines 379-420) and `front-end/src/components/learning/ManualMultiAnswerForm.tsx` (read-only/submitted rendering).
- [X] T033 [P] [US3] In `front-end/src/components/admin/homework/HomeworkReviewDialog.tsx` and `front-end/src/components/admin/units/ActivityReviewDialog.tsx`: `isManual` helper (lines ~65 / ~61) and the auto-question filter (~lines 340 / 333) use `isManualKind`; read-only answer display uses `SentenceStarterAnswerView` when `starters` is present (annotation editor unchanged, editing the full text).
- [X] T034 [US3] Verify (no code expected) the activity unlock and Unidades dot: a SUBMITTED sentence-starter activity yields status `SUBMITTED` in `LearningResponse`, so `lastUnlockedPage`/`quizzesLocked` advance and `activity_submissions.teacher_seen_at` drives the dot; fix only if a FREE_TEXT-specific check blocks it.

**Checkpoint**: Full loop works: author → answer → correct → result with model answers; editing the question later doesn't change past results.

---

## Phase 6: Polish & Cross-Cutting

- [X] T035 [P] Update `CLAUDE.md` "Homework grading" note: add `SENTENCE_STARTER` as a manual kind (`isManual()`), structure `{items:[{starter, modelAnswer?}]}`, server-composed `answer_text` lines + `answer_json` snapshot, not in quizzes; add the kind to the `QuestionKind` list wording.
- [X] T036 Run `./gradlew build` in `back-end/` and `npm run lint && npm run format && npm run build` in `front-end/`; fix any failure.
- [X] T037 Run quickstart.md scenarios 1–9 in the browser (incl. 375 px width and es/en/ro); capture a screenshot of the student form and the corrected result.

---

## Dependencies & Execution Order

- **Setup (T001)** → **Foundational (T002–T011)** → **US2 (T012–T020)** → **US1 (T021–T027)** → **US3 (T028–T034)** → **Polish (T035–T037)**.
- Within Foundational: T002 before T004/T006–T008; T003 before T004; T004 before T005/T007/T008. T009–T011 (front-end) independent of back-end tasks.
- US2: T012 needs T004; T015 before T016; T018 before T019 (also reused by US1).
- US1: T021 before T022/T023; T025/T026 need T018 and T009; T027 after T026.
- US3: T028 before T029; T031 before T032/T033.

### Parallel opportunities

- Foundational: T003, T005, T007, T008 (different back-end files, after T002/T004) alongside T009, T010, T011 (front-end).
- US2: T013, T014, T017, T020 alongside T012/T015.
- US1: back-end T022 + T023 (different services) alongside front-end T025 + T026.
- US3: T030 and T033 alongside T031.

```text
# Example: Foundational fan-out after T002 + T004
T005 SentenceStartersTest.java
T007 QuestionScoring.java / ActivityExerciseGradingService.java
T008 ExerciseGradingService.java
T009 lib/learning.ts   T010 lib/admin.ts   T011 lib/sentenceStarter.ts
```

## Implementation Strategy

- **MVP** = US2 + US1 + US3 (all P1): the type is only useful once it can be authored, answered and corrected. Deliver in that order, checking each phase's checkpoint.
- Incremental: after US2 the teacher can already author and preview; after US1 students can submit (correction falls back to the existing plain free-text view); US3 adds starter emphasis and model answers.
