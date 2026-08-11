# Implementation Plan: Percentage Grades for Manual Answers

**Branch**: `037-manual-percent-grade` | **Date**: 2026-08-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/037-manual-percent-grade/spec.md`

## Summary

Replace binary teacher **validate/invalidate** on manually graded answers (FREE_TEXT and Writing) with a **0–100%** score per answer. Overall homework/activity `%` remains the equal average of question scores (auto 0/1, manual teacher%/100), half-up to a whole percent. Teachers may **save partial** percentages while status stays `SUBMITTED`; **finalize** requires every manual answer scored → `GRADED`. Students **never see** teacher percentages until finalized. Applies to pure-manual, Writing, mixed, and presentation activities. Migrate existing `VALIDATED`→100 / `INVALIDATED`→0.

Technical approach: add nullable `teacher_score_percent` on answer rows (and WRITE draft on submission), replace `teacherValidation` in admin feedback API with `teacherScorePercent` + `finalize` flag; reuse `answer.score` as `percent/100` once set; strip teacher scores from student payloads while `SUBMITTED`.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend)

**Primary Dependencies**:
- Backend: Spring Boot 3.5, NamedParameterJdbcTemplate, Flyway — **no new libraries**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI — **no new libraries**; evolve `HomeworkReviewDialog` / `ActivityReviewDialog` and student result views

**Storage**: PostgreSQL 18 — Flyway `V19__manual_percent_grade.sql`: add `teacher_score_percent`, backfill from `teacher_validation`, drop `teacher_validation` (+ WRITE draft column if needed)

**Testing**: Backend JUnit (`HomeworkAdminService` / `ActivityAdminService` / composition scoring / student DTO stripping). Frontend browser checks per [quickstart.md](./quickstart.md)

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`)

**Project Type**: Full-stack web (`front-end/` + `back-end/`)

**Performance Goals**: Negligible — same review/submit paths; O(questions) average unchanged

**Constraints**: Whole percentages 0–100 only; hide teacher % from students until finalize; keep annotations + ≤500-char note; all-auto unchanged; i18n es/en/ro; parity homework ↔ activities

**Scale/Scope**: Admin review dialogs + student result views + shared composition scoring; one migration; API field rename on feedback/result DTOs

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Reuse existing `score` / `scorePercentFromScores`; replace binary enum with integer percent; one `finalize` flag on existing feedback PUT; no new review product surface.
- **II. Component-Driven UI** — PASS. Evolve existing review dialogs and learning result components; percent input as named control; no raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Types in `lib/learning.ts` / `lib/admin.ts`; scoring stays in `HomeworkCompositionSupport`.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `037-manual-percent-grade`.

**Post-design re-check**: PASS — data model migrates `teacher_validation` → `teacher_score_percent`; contracts define `finalize` + student stripping; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/037-manual-percent-grade/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── manual-percent-grade-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V19__manual_percent_grade.sql                 # NEW

back-end/src/main/java/com/kuky/backend/
├── learning/
│   ├── model/TeacherValidation.java              # DELETE (or leave unused → delete)
│   ├── model/HomeworkAnswer.java                 # EDIT: teacherScorePercent; drop teacherValidation
│   ├── model/HomeworkSubmission.java             # EDIT: optional WRITE draft percent if stored on submission
│   ├── dto/ManualAnswerViewDto.java              # EDIT: teacherScorePercent; hide when awaiting (student)
│   └── service/HomeworkCompositionSupport.java   # EDIT: teacherPercentScore; fullyCorrect from 1.0 / 100
├── admin/
│   ├── dto/SaveHomeworkFeedbackRequest.java      # EDIT: teacherScorePercent + finalize; drop teacherValidation
│   ├── dto/AnnotatedAnswerRequest.java           # (nested) EDIT same
│   ├── dto/HomeworkSubmissionAdminDto.java       # EDIT: expose teacherScorePercent to teacher
│   ├── service/HomeworkAdminService.java         # EDIT: partial save vs finalize; WRITE %
│   └── service/ActivityAdminService.java         # EDIT: mirror homework
└── (tests) …AdminService* / composition / student stripping

front-end/src/
├── lib/learning.ts                               # EDIT: TeacherValidation → teacherScorePercent types
├── lib/admin.ts                                  # EDIT: SaveHomeworkReviewPayload finalize + percent
├── components/admin/homework/HomeworkReviewDialog.tsx
├── components/admin/activities/ActivityReviewDialog.tsx
├── components/learning/MixedHomeworkForm.tsx
├── components/learning/ManualMultiAnswerForm.tsx
├── components/learning/HomeworkWritePage.tsx     # (or Write result view) show % when graded
└── i18n/locales/{es,en,ro}.ts                    # EDIT: percent labels; remove validate/invalidate copy
```

**Structure Decision**: Existing full-stack layout; change review/scoring path in place (no new packages).

## Complexity Tracking

> Empty — no constitution violations.
