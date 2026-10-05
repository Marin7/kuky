# Implementation Plan: Spot the Wrong Words (new question kind)

**Branch**: `054-spot-wrong-words` | **Date**: 2026-10-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/054-spot-wrong-words/spec.md`

## Summary

Add a ninth question kind, `SPOT_WRONG_WORDS`, to the shared question model used by
homework, presentation activities and quizzes. The question's `prompt` is the passage
(as for `MULTI_BLANK`); `structure_json` holds the answer key as a list of wrong words
identified by **word index** (with the word text as a drift guard and an optional
correction). A single word-segmentation rule (letters/digits, internal `-`/`'` joins)
is implemented once in TypeScript (editor, preview, student form, results) and once in
Java (save-time validation), pinned to each other by shared test vectors.

Students click words to toggle them (capped at the error count); the answer is
`{ "selected": [wordIndex…] }`. Scoring lives in `QuestionScoring` (shared by homework
and quizzes) and is reused by `ActivityExerciseGradingService`: score = found ÷ errors,
weighted as one question. `unitResults` carry one entry per error **and** per wrongly
marked word, so the existing result DTOs need no new fields. Front-end adds an editor,
a clickable-text question, an inline result renderer, and branches in the existing
switch points. One Flyway migration widens the three `kind` CHECK constraints.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3.5) · TypeScript 5 strict (React 19)

**Primary Dependencies**: Spring JDBC (`NamedParameterJdbcTemplate`), Jackson, Flyway 11 · TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next

**Storage**: PostgreSQL 18 — existing `homework_questions` / `activity_questions` / `quiz_questions` (`kind`, `prompt`, `structure_json` JSONB); answers in existing `answer_json`. New migration `V27` only widens CHECK constraints.

**Testing**: `./gradlew test` (JUnit — scoring, validation, tokenizer vectors; integration tests run in CI), `npm run lint`, `npm run build`, browser verification per [quickstart.md](./quickstart.md)

**Target Platform**: Web (SSR) — desktop and phone-width browsers

**Project Type**: Web application (back-end + front-end)

**Performance Goals**: Tokenising ≤3,000 characters is sub-millisecond; no new requests or endpoints

**Constraints**: Identical segmentation on client and server; answer key never sent before submit; no horizontal scroll at 375 px; tap targets usable on phone; es/en/ro labels

**Scale/Scope**: ≤30 errors and ≤3,000 characters per question; 1 migration, ~6 back-end files touched + 1 new helper, ~12 front-end files touched + 3–4 new components, 3 locale files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment |
|---|---|
| I. Simplicity First | ✅ Reuses the existing question/answer tables, DTOs, grading pipeline, snapshots and draft storage. No new endpoints, tables or dependencies. One small tokenizer per side instead of shipping server-side segment arrays. |
| II. Component-Driven UI | ✅ New named components: `SpotWrongWordsEditor`, `SpotWrongWordsQuestion`, `SpotWrongWordsResult`; tokenizer in `lib/spotWrongWords.ts`. Tailwind + Shadcn only. |
| III. Evolution-Ready Architecture | ✅ Types and segmentation live in `lib/` modules beside the other kinds; API calls unchanged. |
| Workflow: browser verification | ✅ Quickstart scenarios cover editor, student form, cap, results, mixed homework, quiz, activity, Errores frecuentes, mobile. |
| Workflow: no dead code | ✅ Nothing replaced; only additive branches. |

**Post-design re-check**: ✅ unchanged. The one new back-end abstraction (`SpotWrongWords` helper) exists because two graders (`QuestionScoring` and the duplicated `ActivityExerciseGradingService`) and the validator need the same logic.

## Project Structure

### Documentation (this feature)

```text
specs/054-spot-wrong-words/
├── plan.md              # This file
├── research.md          # Phase 0 decisions
├── data-model.md        # structure / answer / result shapes, validation
├── quickstart.md        # Browser + test validation scenarios
├── contracts/
│   └── spot-wrong-words-api.md
└── tasks.md             # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V27__spot_wrong_words_kind.sql            # NEW: widen 3 kind CHECKs

back-end/src/main/java/com/kuky/backend/
├── learning/
│   ├── ExerciseStructureLimits.java          # + MAX_SPOT_ERRORS=30, MAX_SPOT_TEXT_CHARS=3000, MAX_SPOT_CORRECTION_CHARS=100
│   ├── model/QuestionKind.java               # + SPOT_WRONG_WORDS; isStructured() includes it
│   └── service/
│       ├── SpotWrongWords.java               # NEW: tokenize(), grade() (score + unitResults, cap check)
│       ├── QuestionScoring.java              # + case → SpotWrongWords.grade
│       ├── ExerciseGradingService.java       # stripStructureForStudent → { errorCount }
│       └── ActivityExerciseGradingService.java # grade + strip cases (delegate to SpotWrongWords)
└── admin/service/HomeworkAdminService.java   # validateStructure → validateSpotWrongWords (shared by activity/quiz admin)

back-end/src/test/java/com/kuky/backend/learning/
├── SpotWrongWordsTest.java                   # NEW: tokenizer vectors, scoring, cap, units
└── ExerciseGradingServiceTest.java           # + kind round-trip / strip case

front-end/src/
├── lib/
│   ├── spotWrongWords.ts                     # NEW: tokenize(), carryMarks() (LCS remap on edit)
│   ├── learning.ts                           # QuestionKind, StudentStructure.errorCount, SpotWrongWordsAnswer
│   └── admin.ts                              # QuestionKind, SpotWrongWordsStructure
├── components/admin/homework/
│   ├── SpotWrongWordsEditor.tsx              # NEW: textarea + clickable view + correction fields
│   ├── QuestionEditorCard.tsx                # kind option + editor branch
│   ├── questionDefaults.ts                   # isStructuredKind, default { errors: [] }
│   └── HomeworkPreview.tsx                   # toStudentStructure + hidesPromptLabel + render branch
├── components/learning/
│   ├── SpotWrongWordsQuestion.tsx            # NEW: clickable text, counter, cap hint
│   ├── SpotWrongWordsResult.tsx              # NEW: inline found/missed/wrongly-marked rendering
│   ├── ExerciseForm.tsx                      # answer state, payload, render branch
│   ├── MixedHomeworkForm.tsx                 # same (used by quizzes too)
│   └── ExerciseResult.tsx                    # result branch
├── components/admin/students/MistakeOnlyDetail.tsx  # Errores frecuentes branch
└── i18n/locales/{es,en,ro}.ts                # kind name, counter, cap hint, result legend, editor labels
```

**Structure Decision**: Existing web-app layout (`back-end/`, `front-end/`). The new kind
plugs into the same switch points every structured kind already uses; activity and quiz
authoring and taking reuse the homework components and validator, so FR-017 comes from
the shared code paths plus the one activity-grader branch.

## Complexity Tracking

No constitution violations.
