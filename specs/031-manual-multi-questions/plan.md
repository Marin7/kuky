# Implementation Plan: Multi-Question Manual Homework

**Branch**: `031-manual-multi-questions` | **Date**: 2026-08-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/031-manual-multi-questions/spec.md`

## Summary

Extend **non-WRITE MANUAL** homeworks (and MANUAL presentation activities) so teachers author an ordered list of free-text questions and students answer each with a compact plain-text field — page order **instructions → media (e.g. audio) → questions**. Reuse `homework_questions` / `activity_questions` with a new `FREE_TEXT` kind; store per-question plain answers (and a submit-time **prompt snapshot**) on answer rows; keep **WRITE** on the existing single rich-text `response_text` path. Migrate legacy non-WRITE MANUAL single answers into one FREE_TEXT question + answer row.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate, Flyway, PostgreSQL. **No new libraries.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, react-i18next. **No new libraries.**

**Storage**: PostgreSQL 18 — Flyway `V15__manual_free_text_questions.sql`: widen question `kind` CHECKs with `FREE_TEXT`; add `answer_text` + `prompt_snapshot` on `homework_answers` / `activity_answers`; data migration for legacy non-WRITE MANUAL submissions.

**Testing**: Backend JUnit — MANUAL authoring validation (WRITE vs non-WRITE), submit requires all answers non-empty, snapshot retained after question delete, WRITE/`EXERCISE` regression. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Small question lists per homework/activity; submit/review stay single-request.

**Constraints**: WRITE unchanged (single rich-text); EXERCISE unchanged; compact answers plain text only; whole-submission feedback only; every FREE_TEXT answer required non-empty on submit; activities in scope; no new video media type.

**Scale/Scope**: Single-teacher site; one migration; shared validation in `HomeworkAdminService` reused by activities; student/admin UI for homework + activity MANUAL; i18n es/en/ro.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Reuse existing question/answer tables + `FREE_TEXT` kind instead of parallel manual-question tables. Snapshot as columns on answer rows (not a second history store). WRITE stays on `response_text`.
- **II. Component-Driven UI** — PASS. Compact multi-answer form component; reuse existing listening/reading page shells; authoring reuses a slim free-text question list (not full EXERCISE `QuestionEditorCard` kinds).
- **III. Evolution-Ready Architecture** — PASS. Types/API helpers in `front-end/src/lib/admin.ts` and `learning.ts`; backend under existing `learning` + `admin` packages.
- **Technology Stack** — PASS. Unchanged stack.
- **Development Workflow** — PASS. Quickstart lists browser checks; branch `031-manual-multi-questions`.

**Post-design re-check**: PASS — contracts extend existing MANUAL submit/review DTOs; data model is one migration + kind widen; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/031-manual-multi-questions/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── manual-multi-questions-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V15__manual_free_text_questions.sql

back-end/src/main/java/com/kuky/backend/
├── learning/
│   ├── model/QuestionKind.java              # EDIT: + FREE_TEXT
│   ├── model/HomeworkAnswer.java            # EDIT: answerText, promptSnapshot
│   ├── model/ActivityAnswer.java            # EDIT: same
│   ├── dto/SubmitHomeworkRequest.java       # EDIT: answers[] for multi MANUAL
│   ├── dto/HomeworkItemResponse.java        # EDIT: expose FREE_TEXT questions + answers
│   ├── service/HomeworkSubmissionService.java
│   ├── service/ActivityStudentService.java
│   ├── service/HomeworkItems.java
│   └── repository/*AnswerRepository.java
├── admin/
│   ├── service/HomeworkAdminService.java    # EDIT: validateAndMapQuestions for MANUAL
│   ├── service/ActivityAdminService.java    # inherits via validateAndMapQuestions
│   └── dto/HomeworkSubmissionAdminDto.java  # EDIT: question+answer pairs for review

front-end/src/
├── lib/admin.ts / learning.ts               # EDIT: FREE_TEXT, submit/review shapes
├── components/admin/homework/
│   ├── HomeworkEditorPage.tsx               # EDIT: question list when MANUAL && !WRITE
│   └── ManualQuestionListEditor.tsx         # NEW: prompt-only ordered list
├── components/admin/activities/
│   └── ActivityEditorPage.tsx               # EDIT: same MANUAL question authoring
├── components/learning/
│   ├── ManualMultiAnswerForm.tsx            # NEW: compact plain-text per question
│   ├── ManualAnswerForm.tsx                 # KEEP: WRITE only
│   ├── HomeworkListeningPage.tsx            # EDIT: layout order + multi form
│   ├── HomeworkReadingPage.tsx
│   ├── HomeworkInlinePanel.tsx
│   └── (activity MANUAL overlay/panel)      # EDIT: multi form
├── components/admin/homework/HomeworkReviewDialog.tsx  # EDIT: per-question review
└── i18n/locales/{es,en,ro}.ts
```

**Structure Decision**: Stay inside existing homework/activity packages. No new top-level module. Placement test untouched.

## Complexity Tracking

> No constitution violations requiring justification.
