# Implementation Plan: Rich Text Formatting for Writing Homework Feedback

**Branch**: `018-homework-rich-text-annotations` | **Date**: 2026-07-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/018-homework-rich-text-annotations/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Writing (MANUAL-format) homework has no teacher-review capability today — no feedback field, no way to transition a submission to `REVIEWED`, and no rich text anywhere in the app (plain textarea only). This feature adds that review capability, using a constrained rich-text model (text color, background highlight, strikethrough — independently combinable) as the format for both the teacher's feedback and the student's own answer. Technical approach: represent formatted text as a JSON array of styled segments (not HTML) stored in the existing `homework_submissions` table (two new nullable columns: `feedback`, `reviewed_at`), rendered/edited by two small new React components (`RichTextEditor`, `RichTextViewer`) built on the existing Shadcn/Tailwind toolkit — no new rich-text-editor or HTML-sanitizer dependency required, since the segment schema makes disallowed markup structurally impossible. New admin endpoints expose a cross-student review queue and a per-submission feedback-save action; the existing per-student profile endpoint gains a `needsReview` flag.

## Technical Context

**Language/Version**: TypeScript 5 (strict) — front-end; Java 21 — back-end.

**Primary Dependencies**: React 19, TanStack Start (SSR) + TanStack Router (file-based), TailwindCSS 4, Shadcn UI (Radix primitives) — front-end. Spring Boot 3.5, Spring Security, plain JDBC (`NamedParameterJdbcTemplate`), Flyway 11 — back-end. No new dependencies added by this feature (see [research.md](research.md) Decisions 1–2).

**Storage**: PostgreSQL 18 — extends existing `homework_submissions` table (see [data-model.md](data-model.md)); new migration `V28`.

**Testing**: JUnit 5 + Spring Boot Test (back-end, existing convention under `back-end/src/test/java/com/kuky/backend/learning/` and `.../admin/`). No front-end test framework exists in this repo; front-end changes are verified manually in a running browser per the constitution's Development Workflow rule (see [research.md](research.md) Testing approach).

**Target Platform**: Web — browser front-end, Spring Boot server back-end (existing deployment).

**Project Type**: Web application (existing `front-end/` + `back-end/` monorepo).

**Performance Goals**: No feature-specific targets beyond standard interactive web responsiveness (matches SC-001: review completed in under 3 minutes end-to-end, a workflow-speed goal, not a latency target).

**Constraints**: Formatted content must be structurally incapable of carrying scripts/links/images/arbitrary markup (FR-008) — addressed by the segment schema itself, not runtime sanitization. Visible-text length capped at 2000 chars (existing limit), independent of formatting markup size (FR-009).

**Scale/Scope**: Single-teacher app with a small student roster (tens, not thousands); no scale concerns beyond what the existing schedule/booking features already handle.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment |
|---|---|
| I. Simplicity First | **PASS** — chose a constrained JSON segment model over HTML+sanitizer, and a small custom editor over a rich-text library, specifically to avoid adding dependencies/abstractions not needed for 3 formatting primitives (research.md Decisions 1–2). Reuses the existing `homework_submissions` table rather than a new entity (Decision 3). |
| II. Component-Driven UI | **PASS** — `RichTextEditor` and `RichTextViewer` are new, named, reusable React components built from existing Tailwind/Shadcn primitives; no raw DOM manipulation outside these components. |
| III. Evolution-Ready Architecture | **PASS** — all new data-fetching (review queue, submission detail, save-feedback) is added to the existing `front-end/src/lib/learning.ts` / `front-end/src/lib/admin.ts` service modules, following the established pattern, not inlined in components. |

Note: the constitution's "Technology Stack" section predates the back-end's existence ("Future backend: to be determined") and is stale relative to `CLAUDE.md`, which documents the current Spring Boot back-end as established fact. This is a documentation gap in the constitution, not a violation triggered by this feature — no gate failure.

No violations. Complexity Tracking table not needed.

## Project Structure

### Documentation (this feature)

```text
specs/018-homework-rich-text-annotations/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── api.md           # Phase 1 output
└── tasks.md              # Phase 2 output (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
back-end/src/main/java/com/kuky/backend/
├── learning/
│   ├── model/HomeworkSubmission.java          # +feedback, +reviewedAt fields
│   ├── model/FormattedTextSegment.java        # (new) shared value type + validation
│   ├── exception/ (new) AlreadyReviewedException.java, NotSubmittedException.java
│   ├── repository/HomeworkSubmissionRepository.java  # +updateFeedback, +findSubmittedManual queries
│   ├── service/HomeworkSubmissionService.java  # submit() now (de)serializes FormattedText
│   ├── dto/SubmitHomeworkRequest.java          # response: FormattedText, not String
│   └── dto/HomeworkItemResponse.java           # +feedback field
├── admin/
│   ├── controller/HomeworkAdminController.java     # +review endpoints
│   ├── service/HomeworkAdminService.java            # +review queue / save-feedback logic
│   ├── dto/StudentProfileHomeworkDto.java           # +needsReview
│   └── dto/ (new) HomeworkSubmissionAdminDto.java, HomeworkReviewQueueItemDto.java, SaveHomeworkFeedbackRequest.java

back-end/src/main/resources/db/migration/
└── V28__add_homework_feedback.sql              # new

front-end/src/
├── components/learning/
│   ├── ManualAnswerForm.tsx                    # uses RichTextEditor instead of Textarea
│   ├── HomeworkWritePage.tsx                   # renders teacher feedback via RichTextViewer
│   └── richtext/ (new) RichTextEditor.tsx, RichTextViewer.tsx, FormattingToolbar.tsx, types.ts
├── components/admin/homework/ (existing folder, new files)
│   ├── HomeworkReviewQueue.tsx
│   └── HomeworkReviewDialog.tsx
└── lib/
    ├── learning.ts                              # submit/get homework: FormattedText types
    └── admin.ts                                 # +review queue / save-feedback calls
```

**Structure Decision**: Existing `front-end/` + `back-end/` monorepo layout is unchanged. This feature adds a small `richtext/` component group under `front-end/src/components/learning/` (reused by both the student answer form and the teacher's feedback viewer), new review UI files under the existing `front-end/src/components/admin/homework/` folder (alongside `HomeworkTab.tsx`), and extends the existing `learning` and `admin` packages on the back-end (`FormattedTextSegment` lives in `learning/model/`, not a new top-level package) — no new top-level modules.
