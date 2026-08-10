# Implementation Plan: Multiple Correct Blank Answers

**Branch**: `034-multi-correct-blanks` | **Date**: 2026-08-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/034-multi-correct-blanks/spec.md`

## Summary

Extend auto-gradable gap exercises so blanks can have multiple correct responses: typed **MULTI_BLANK** already stores `acceptedAnswers[]` — enforce max 10 and always reveal the full set after grading when there is more than one. **DRAG_DROP** moves from positional `bank[i] → blank i` to an explicit per-blank `correctBankIds[]` (any-of match), with bank size 2–30 allowing alternates and pure distractors. Legacy word-bank JSON keeps grading via dual-read (no Flyway rewrite). Table-fill, choice, matching, true/false unchanged. Homework + presentation activities share validation/grading paths. Frontend: cap MultiBlankEditor; redesign DragDropEditor (free bank + per-blank multi-select); show all accepted alternatives on graded MULTI_BLANK/DRAG_DROP units when `expectedDisplay.length > 1` (and when populated for correct blanks).

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, NamedParameterJdbcTemplate, Jackson JSONB `structure_json`. **No new libraries.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, existing `MultiBlankEditor` / `DragDropEditor` / `MultiBlankResult`. **No new libraries.**

**Storage**: PostgreSQL 18 — **no schema migration**. `structure_json` shape for `DRAG_DROP` gains optional `blanks[].correctBankIds`; legacy `{ bank: [...] }` remains readable. `MULTI_BLANK` shape unchanged aside from enforced caps.

**Testing**: Backend JUnit — `validateDragDrop` / `validateMultiBlank` caps and mapping rules; `gradeDragDrop` any-of + legacy positional; `gradeMultiBlank` always-show multi accepted; activity grading parity. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Negligible — grading over ≤20 blanks / ≤30 bank items per question.

**Constraints**: Max 10 accepted/correct per blank; max 30 bank items; bank item correct for at most one blank; no table-fill behaviour change; answer keys still stripped pre-submit; i18n es/en/ro; existing DRAG_DROP submissions/keys must not silently change scores (FR-008).

**Scale/Scope**: Single-teacher site; authoring + student take + result/review for homework and activities; dual-read compatibility for stored word banks.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Extend existing JSON structures and graders; no new question kind; no Flyway unless dual-read proves insufficient (it is sufficient). Table-fill left alone per clarification.
- **II. Component-Driven UI** — PASS. Evolve named editors/result components (`DragDropEditor`, `MultiBlankEditor`, `MultiBlankResult`); no raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Types in `admin.ts` / `learning.ts`; validation in `HomeworkAdminService` (already shared by activities); grading in `ExerciseGradingService` + `ActivityExerciseGradingService`.
- **Technology Stack** — PASS. Unchanged stack.
- **Development Workflow** — PASS. Quickstart browser checks; branch `034-multi-correct-blanks`.

**Post-design re-check**: PASS — contracts document structure/grading deltas only; no new endpoints; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/034-multi-correct-blanks/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── multi-correct-blanks-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/
├── admin/service/HomeworkAdminService.java          # EDIT: validateDragDrop + max-10 on MULTI_BLANK acceptedAnswers
├── learning/service/ExerciseGradingService.java     # EDIT: gradeDragDrop any-of + multi accepted reveal
├── learning/service/ActivityExerciseGradingService.java  # EDIT: same grading/reveal rules
└── (tests under src/test/java/…)

front-end/src/
├── lib/admin.ts                                     # EDIT: DragDropStructure + blanks.correctBankIds
├── lib/learning.ts                                  # EDIT: types if student structure exposes blanks map
├── components/admin/homework/
│   ├── DragDropEditor.tsx                           # EDIT: free bank + per-blank correct multi-select
│   ├── MultiBlankEditor.tsx                         # EDIT: max 10 accepted answers
│   └── (activity editor reuses same question editors)
├── components/learning/
│   ├── MultiBlankResult.tsx                         # EDIT: show expected when multi even if correct
│   ├── DragDropQuestion.tsx                         # EDIT: bank may exceed blanks; unchanged placement UX
│   └── ExerciseResult.tsx                           # EDIT: only if shared expected display needs tweak
└── i18n/locales/{en,es,ro}.ts                       # EDIT: authoring/feedback strings
```

**Structure Decision**: Existing full-stack layout. No new packages or migrations. Activities inherit via shared `validateAndMapQuestions` and mirrored grading service edits.

## Complexity Tracking

> No constitution violations requiring justification.
