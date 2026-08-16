# Research: Emojis in Homework Writing and Feedback

**Feature**: `046-homework-text-emojis` | **Date**: 2026-08-16

## 1. Storage — no schema change

**Decision**: Store emojis as ordinary Unicode in the existing `FormattedTextSegment.text` JSON and in plain `feedbackText` strings. No Flyway migration, no new columns, no new endpoints.

**Rationale**: Answers and feedback are already UTF-8 JSON/text (`homework_submissions.response_text` / `feedback`). Jackson and PostgreSQL already round-trip non-ASCII. Spec FR-005: device characters, not pictures.

**Alternatives considered**:
- Custom image sprites or an emoji font pack — rejected (clarification: device glyphs).
- A parallel “emoji tokens” format — extra mapping; YAGNI.

## 2. Length counting

**Decision**: Keep today’s UTF-16 `.length()` on both sides (`visibleLength` / `String.length()`). Insert a classroom emoji only if the whole emoji fits; do not slice mid-character. HTML `maxLength` stays as-is.

**Rationale**: Spec reuses existing limits (2000 visible for written answers; 500 for manual review comments; exercise feedback UI already uses 2000). Introducing grapheme-cluster counting would need a new library and would change how every existing answer is measured.

**Alternatives considered**:
- Count Unicode grapheme clusters — more “user visible” but changes the limit meaning and needs extra code.
- Count code points (`codePointCount`) — still splits ZWJ sequences; little gain.

## 3. In-app set

**Decision**: One frozen list of 26 default-appearance classroom reactions in `front-end/src/components/learning/richtext/classroomEmojis.ts`, reused by the formatting bar and the comment picker. No categories, search, or skin-tone picker. Paste/keyboard may add other emojis; the server does not whitelist the set.

**Rationale**: Clarifications: ~20–30, default only, same set everywhere. A TypeScript const is the simplest shared source; no backend catalog.

**Set (26)**: 😀 😊 😂 😍 🤔 😅 😎 😢 😮 👍 👎 👏 🙏 👋 ❤️ 💕 ⭐ ✅ ❌ 💡 📚 ✏️ 🎉 💪 🔥 💯

**Alternatives considered**:
- Full catalog / search — rejected in spec.
- Load set from the API — no need; it does not change per user.

## 4. Insertion UI

**Decision**:
- **Writing `RichTextEditor` (not `formatOnly`)**: emoji button on `FormattingToolbar` opens the existing Shadcn `Popover` with a wrap grid of the 26. Insert goes through the same `reconcileEdit` path as paste so sticky color/highlight/strike apply.
- **Plain comment / FREE_TEXT textarea**: same popover via `ClassroomEmojiPicker` next to the field (no formatting bar).
- **`formatOnly` editors** (teacher markup of the student answer): omit the emoji control (FR-009).

**Rationale**: Clarification on two placements; Popover is already in the UI kit; no new dependency.

**Alternatives considered**:
- Native `<input type="text">` + OS picker only — fails SC-005 (no emoji keyboard).
- `contentEditable` emoji panel — would replace the textarea editor; out of scope.

## 5. Where the control is wired (scope gate)

**Decision**: Do **not** turn emoji on for every `RichTextEditor`. Pass `allowEmojiInsert` (default `false`). Enable only on homework student-compose surfaces and homework teacher-comment surfaces.

| Surface | Emoji |
|---------|--------|
| `ManualAnswerForm` / Writing homework | yes (toolbar) |
| Mixed homework FREE_TEXT (`Textarea`, `richFreeText` false) | yes (picker next to field) |
| `HomeworkReviewDialog` feedback comment | yes (picker next to comment) |
| `ExerciseResultDialog` teacher feedback | yes (picker next to comment) |
| Teacher `formatOnly` markup of student text | no |
| Activities, quizzes, homework authoring | no (FR-012) |

**Rationale**: `MixedHomeworkForm` and `RichTextEditor` are shared with quizzes and activities. A default-on toolbar would leak out of scope.

**Alternatives considered**:
- Enable on all non-`formatOnly` editors — simpler, but violates FR-012.
- Turn on `richFreeText` for mixed homework just to reuse the toolbar — would add color/highlight to mixed FREE_TEXT, which those questions do not have today.

## 6. Backend tests vs frontend tests

**Decision**: Add JUnit coverage that a thumbs-up in a WRITE response and in `feedbackText` survives validate/encode/save. Frontend has no unit runner; prove the picker in the browser per [quickstart.md](./quickstart.md).

**Rationale**: Constitution YAGNI — do not add Vitest for one helper. Persistence is the only server risk.

**Alternatives considered**:
- Add Vitest — extra toolchain for a 26-emoji const and an insert helper.
