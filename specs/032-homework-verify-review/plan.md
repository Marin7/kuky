# Implementation Plan: Admin Homework Verify Review

**Branch**: `032-homework-verify-review` | **Date**: 2026-08-10 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/032-homework-verify-review/spec.md`

## Summary

Enhance MANUAL homework (and MANUAL activity) admin verify: (1) wrap long unbroken student answers so review never needs horizontal scroll; (2) let the teacher annotate the student’s answer in place (color / highlight / strikethrough) with full mark control but locked wording; (3) replace whole-submission rich feedback with an optional plain ≤500-character note. New-model reviews stay editable after `REVIEWED`; pre-existing rich-feedback reviews are frozen as `LEGACY_RICH`. EXERCISE feedback unchanged.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Backend: Spring Boot 3.5, Spring Security, NamedParameterJdbcTemplate, Flyway, PostgreSQL. **No new libraries.**
- Frontend: React 19, TanStack Start/Router, TailwindCSS 4, Shadcn UI, existing `RichTextEditor` / `RichTextViewer`. **No new libraries** — add a format-only mode to the existing editor.

**Storage**: PostgreSQL 18 — Flyway `V16__homework_verify_review.sql`: add `review_model` on `homework_submissions` / `activity_submissions`; backfill `LEGACY_RICH` for already-`REVIEWED` MANUAL rows with feedback; FREE_TEXT `answer_text` may store FormattedText JSON after annotation (plain until then).

**Testing**: Backend JUnit — wrap not unit-tested; plain-text equality on annotate, 500-char feedback, `LEGACY_RICH` frozen vs `ANNOTATED` re-edit, multi-answer annotate, WRITE annotate, activity parity, EXERCISE regression. Frontend browser checks per [quickstart.md](./quickstart.md).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Single-request review save; small FormattedText payloads.

**Constraints**: No word rewrite on annotate; legacy rich reviews frozen; EXERCISE path untouched; feedback optional empty; activities full parity; reuse FormattedText segment schema and color palette.

**Scale/Scope**: Single-teacher site; one migration; homework + activity review dialogs; student learning views for annotated answers + plain note; i18n es/en/ro.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. Reuse FormattedText + existing feedback column; one `review_model` discriminator; FREE_TEXT answers stay on `answer_text` (plain → optional FormattedText JSON); extend `RichTextEditor` with format-only rather than a second editor.
- **II. Component-Driven UI** — PASS. Shared wrap class + format-only annotation on existing rich-text components; plain `<textarea>` for the 500-char note; review dialogs stay named components.
- **III. Evolution-Ready Architecture** — PASS. Types/API in `admin.ts` / `learning.ts`; backend under existing `admin` + `learning` packages.
- **Technology Stack** — PASS. Unchanged stack.
- **Development Workflow** — PASS. Quickstart browser checks; branch `032-homework-verify-review`.

**Post-design re-check**: PASS — contracts extend existing review PUT; data model is one migration + discriminator; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/032-homework-verify-review/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── homework-verify-review-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
back-end/src/main/resources/db/migration/
└── V16__homework_verify_review.sql

back-end/src/main/java/com/kuky/backend/
├── learning/
│   ├── model/FormattedTextSegment.java     # EDIT: plain feedback max 500 helper; plainText(); optional empty
│   ├── model/ReviewModel.java              # NEW: LEGACY_RICH | ANNOTATED
│   ├── dto/ManualAnswerViewDto.java        # EDIT: optional formatted segments
│   ├── dto/HomeworkItemResponse.java       # EDIT: feedbackText + reviewModel; answers formatted
│   ├── dto/ActivityItemResponse.java       # EDIT: same
│   ├── service/HomeworkItems.java          # EDIT: map new fields
│   ├── service/ActivityStudentService.java # EDIT: student view of annotated answers
│   └── repository/*SubmissionRepository.java  # EDIT: review_model + annotate+feedback update
├── admin/
│   ├── dto/SaveHomeworkFeedbackRequest.java   # EDIT: plain feedback + annotated response/answers
│   ├── dto/HomeworkSubmissionAdminDto.java    # EDIT: reviewModel, feedbackText, formatted answers
│   ├── service/HomeworkAdminService.java      # EDIT: new-model save; legacy freeze; allow re-edit
│   └── service/ActivityAdminService.java      # EDIT: parity

front-end/src/
├── lib/admin.ts / learning.ts
├── components/learning/richtext/
│   ├── RichTextEditor.tsx                  # EDIT: formatOnly (no text mutate)
│   ├── RichTextViewer.tsx                  # EDIT: overflow-wrap anywhere
│   └── types.ts                            # EDIT: helpers if needed
├── components/admin/homework/HomeworkReviewDialog.tsx
├── components/admin/activities/ActivityReviewDialog.tsx
├── components/learning/                        # student reviewed views
└── i18n/locales/{es,en,ro}.ts
```

**Structure Decision**: Stay inside existing homework/activity + richtext packages. No new top-level module.

## Complexity Tracking

> No constitution violations requiring justification.
