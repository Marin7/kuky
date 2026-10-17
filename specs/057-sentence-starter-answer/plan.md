# Implementation Plan: Sentence-Starter Answer (new manual question kind)

**Branch**: `057-sentence-starter-answer` | **Date**: 2026-10-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/057-sentence-starter-answer/spec.md`

## Summary

Add a tenth question kind, `SENTENCE_STARTER`, to the homework/activity question model. It is
**manually graded** exactly like `FREE_TEXT`: a new `QuestionKind.isManual()` predicate replaces
the `kind == FREE_TEXT` checks that mean "manual" across composition, submission, scoring and
review, so the lifecycle, teacher %, annotations, note, unseen dots and combined score are reused
unchanged. `structure_json` holds ordered `{starter, modelAnswer?}` items (starter normalised at
save: trailing `...`/`…` removed). The student sends only continuations; the server composes each
sentence (`SentenceStarters.compose`) so every stored sentence begins with its starter, stores
them one per line in `answer_text`, and snapshots `{starters, modelAnswers, continuations}` in
`answer_json`. `ManualAnswerViewDto` exposes `starters` and (post-finalisation) `modelAnswers`.
Front-end: one editor, one input component used by both student forms (MIXED and ALL_MANUAL),
one answer renderer for results and review dialogs. Not offered in quizzes.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3.5) · TypeScript 5 strict (React 19)

**Primary Dependencies**: Spring JDBC, Jackson, Flyway 11 · TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next

**Storage**: PostgreSQL 18 — existing `homework_questions`/`activity_questions` (`kind`, `prompt`, `structure_json`) and `homework_answers`/`activity_answers` (`answer_text`, `answer_json`, `prompt_snapshot`, teacher %). Migration `V33` only widens two CHECKs.

**Testing**: `./gradlew test` (new `SentenceStartersTest`; integration tests in CI), `npm run lint`, `npm run build`, browser checks in [quickstart.md](./quickstart.md)

**Target Platform**: Web (SSR), desktop and phone-width browsers

**Project Type**: Web application (back-end + front-end)

**Performance Goals**: No new requests or endpoints; composition is O(starters)

**Constraints**: Model answers never sent to students before finalisation; composed answer ≤ 2,000 chars (annotation limit); no horizontal scroll at 375 px; es/en/ro labels

**Scale/Scope**: ≤ 10 starters per question; 1 migration, 1 new back-end helper + ~10 files touched; 3 new front-end components + ~12 files touched; 3 locale files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment |
|---|---|
| I. Simplicity First | ✅ No new tables, endpoints or dependencies. Reuses the whole FREE_TEXT manual-correction pipeline; one predicate (`isManual()`) instead of parallel code paths. No draft feature added (parity with FREE_TEXT questions). |
| II. Component-Driven UI | ✅ New named components `SentenceStarterEditor`, `SentenceStarterInput`, `SentenceStarterAnswerView`; Tailwind + Shadcn only. |
| III. Evolution-Ready Architecture | ✅ Types/helpers in `lib/learning.ts`, `lib/admin.ts`, `lib/sentenceStarter.ts`; API calls stay in existing service modules. |
| Workflow: browser verification | ✅ Quickstart covers authoring, both student forms, correction, activity unlock, quiz exclusion, versioning, mobile, locales. |
| Workflow: no dead code | ✅ Additive branches; `== FREE_TEXT` "manual" checks replaced, not duplicated. |

**Post-design re-check**: ✅ unchanged. The one new back-end helper (`SentenceStarters`) is shared by the validator and three submit paths (homework unified, homework ALL_MANUAL, activities).

## Project Structure

### Documentation (this feature)

```text
specs/057-sentence-starter-answer/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/sentence-starter-api.md
└── tasks.md             # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V33__sentence_starter_kind.sql              # NEW: widen homework/activity kind CHECKs

back-end/src/main/java/com/kuky/backend/
├── learning/
│   ├── ExerciseStructureLimits.java            # + starter limits (10 items, 80, 300, 500, 2000)
│   ├── model/QuestionKind.java                 # + SENTENCE_STARTER, isManual()
│   ├── dto/ManualAnswerDto.java                # + continuations
│   ├── dto/ManualAnswerViewDto.java            # + starters, modelAnswers
│   └── service/
│       ├── SentenceStarters.java               # NEW: normalizeStructure, studentStructure, composeAnswer
│       ├── HomeworkCompositionSupport.java     # isAutoGradable / composition / contributions → isManual()
│       ├── HomeworkSubmissionService.java      # manual filters → isManual(); compose for SENTENCE_STARTER
│       ├── ActivityStudentService.java         # same for activities
│       ├── HomeworkItems.java                  # answer view starters/modelAnswers; student DTO
│       ├── ExerciseGradingService.java         # strip → {starters}; manual guard
│       ├── ActivityExerciseGradingService.java # strip + guard
│       └── QuestionScoring.java                # "not auto-graded" guard covers kind
├── admin/service/HomeworkAdminService.java     # validate kind; prompt optional in activities; review isManual()
├── admin/service/ActivityAdminService.java     # review isManual(); answer view fields
└── quiz/service/QuizAdminService.java          # reject kind

back-end/src/test/java/com/kuky/backend/learning/
└── SentenceStartersTest.java                   # NEW

front-end/src/
├── lib/
│   ├── sentenceStarter.ts                      # NEW: normalizeStarter, compose (preview), splitAnswerLines
│   ├── learning.ts                             # kind, isManualKind, continuations payload, ManualAnswerView fields
│   └── admin.ts                                # kind, SentenceStarterStructure
├── components/admin/homework/
│   ├── SentenceStarterEditor.tsx               # NEW: starter + model-answer rows, add/remove/reorder
│   ├── QuestionEditorCard.tsx                  # kind option + editor branch + excludedKinds prop
│   ├── questionDefaults.ts                     # default { items: [{ starter: "" }] }
│   ├── HomeworkPreview.tsx                     # render SentenceStarterInput
│   └── HomeworkReviewDialog.tsx                # isManual + SentenceStarterAnswerView
├── components/admin/units/ActivityReviewDialog.tsx  # same
├── components/quiz/admin/…                     # pass excludedKinds=["SENTENCE_STARTER"]
├── components/learning/
│   ├── SentenceStarterInput.tsx                # NEW: fixed starter + input lines, counter
│   ├── SentenceStarterAnswerView.tsx           # NEW: lines with semibold starter + model answers
│   ├── MixedHomeworkForm.tsx                   # manual kinds, continuations state/payload, render
│   ├── ManualMultiAnswerForm.tsx               # kind-aware questions, continuations payload, render
│   ├── HomeworkInlinePanel.tsx / ActivityPanel.tsx  # pass kind/structure to ManualMultiAnswerForm
└── i18n/locales/{es,en,ro}.ts                  # kind name, editor labels, "Respuesta modelo", limits
```

**Structure Decision**: Existing web-app layout. The kind plugs into the manual-question code
paths (not the structured ones); activities reuse the homework validator and student forms, so
both homework and activities get the type from the same changes.

## Complexity Tracking

No constitution violations.
