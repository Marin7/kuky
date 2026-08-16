# Implementation Plan: Emojis in Homework Writing and Feedback

**Branch**: `046-homework-text-emojis` | **Date**: 2026-08-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/046-homework-text-emojis/spec.md`

## Summary

Let students insert a fixed classroom set of 26 device emojis while composing written homework, and let Paula insert the same set into the homework feedback comment. Emojis are Unicode in existing answer/feedback fields (no migration). Student Writing uses a popover on the existing formatting bar; mixed FREE_TEXT and teacher comments use the same popover next to the textarea. Teacher markup of the student’s answer stays format-only. Quizzes, activities, and authoring stay out of scope.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x strict (frontend).

**Primary Dependencies**:
- Frontend: React 19, existing `RichTextEditor` / `FormattingToolbar` / Shadcn `Popover` + `Textarea`. **No new libraries.**
- Backend: existing `FormattedTextSegment` validate/encode. **No new libraries. No Flyway.**

**Storage**: PostgreSQL 18 — existing UTF-8 JSON/text on `homework_submissions.response_text` and `feedback`. No schema change.

**Testing**: JUnit (`FormattedTextSegment` / homework admin+submit tests: emoji in WRITE `response` and `feedbackText` round-trips; over-limit still rejected). Frontend: browser checks in [quickstart.md](./quickstart.md) (no frontend unit runner).

**Target Platform**: Browser via TanStack Start SSR (`:8080`) + JVM (`:8081`).

**Project Type**: Full-stack web (`front-end/` + `back-end/`).

**Performance Goals**: Opening the 26-emoji grid is instant (in-memory const). Submit/save payload size unchanged aside from a few extra characters.

**Constraints**: UTF-16 `.length()` limits unchanged (2000 answers; 500 manual feedback comments). Insert whole emoji or skip if it would exceed. `formatOnly` must not insert characters. `allowEmojiInsert` default false so shared editors do not leak to quizzes/activities. Paste/keyboard emojis outside the set are kept.

**Scale/Scope**: Homework Writing compose, mixed homework FREE_TEXT, teacher homework review comment, teacher exercise feedback note. One shared picker component.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Simplicity First (YAGNI)** — PASS. No emoji pack, no search, no skin-tone UI, no API catalog, no grapheme library, no new table. Reuse textarea insert + existing Popover.
- **II. Component-Driven UI** — PASS. Named pieces: `classroomEmojis.ts`, `ClassroomEmojiPicker`, toolbar hook, comment-side picker. No raw DOM.
- **III. Evolution-Ready Architecture** — PASS. Persistence stays in existing learning/admin DTOs; picker is presentation-only.
- **Technology Stack** — PASS. Unchanged.
- **Development Workflow** — PASS. Quickstart browser checks; branch `046-homework-text-emojis`.

**Post-design re-check**: PASS — contract documents Unicode on existing fields only; Complexity Tracking empty.

**Result: PASS.**

## Project Structure

### Documentation (this feature)

```text
specs/046-homework-text-emojis/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── homework-text-emojis-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                        # /speckit-tasks (not created here)
```

### Source Code (repository root)

```text
front-end/src/components/learning/richtext/
├── classroomEmojis.ts              # NEW: 26-character list + insertAtCaret helper
├── ClassroomEmojiPicker.tsx        # NEW: Popover grid
├── FormattingToolbar.tsx           # EDIT: optional emoji button when allowEmojiInsert
├── RichTextEditor.tsx              # EDIT: allowEmojiInsert; insert via reconcileEdit
└── types.ts                        # unchanged model; insert uses existing length

front-end/src/components/learning/
├── ManualAnswerForm.tsx            # EDIT: allowEmojiInsert on editor
├── MixedHomeworkForm.tsx           # EDIT: picker next to homework FREE_TEXT textarea
├── HomeworkWritePage.tsx           # (picker via ManualAnswerForm)
└── HomeworkInlinePanel.tsx         # (picker via ManualAnswerForm / mixed)

front-end/src/components/admin/homework/
├── HomeworkReviewDialog.tsx        # EDIT: picker next to feedback comment; none on formatOnly
└── ExerciseResultDialog.tsx        # EDIT: picker next to exercise feedback

front-end/src/i18n/locales/
├── en.ts / es.ts / ro.ts           # EDIT: richText.emoji (open/insert labels)

back-end/src/test/java/com/kuky/backend/
├── learning/HomeworkSubmissionServiceTest.java   # EDIT: emoji in WRITE response
└── admin/HomeworkAdminServiceTest.java           # EDIT: emoji in feedbackText
```

**Structure Decision**: Front-end-first feature in the existing rich-text module plus two admin comment fields. Backend tests only prove persistence; production Java/SQL unchanged except what those tests already exercise.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

None.
