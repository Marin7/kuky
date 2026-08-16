# Data Model: Emojis in Homework Writing and Feedback

**Feature**: `046-homework-text-emojis` | **Date**: 2026-08-16

No migration. Existing tables and JSON shapes stay.

## Entity: Written homework answer

Unchanged structure: JSON array of `FormattedTextSegment` in `homework_submissions.response_text` (WRITE) or per-question answer text (FREE_TEXT).

| Field | Type | Emoji rule |
|-------|------|------------|
| `text` | string | May include Unicode emoji. Counted with existing UTF-16 `length` toward 2000. |
| `color` / `highlight` / `strike` | unchanged | May apply to a run that also contains emojis. |

Validation (`FormattedTextSegment.validate`) does not strip emoji. Empty-after-trim still rejected for required answers; emoji-only (e.g. `👍`) is non-empty.

## Entity: Teacher feedback comment

Plain string the teacher types; stored via `FormattedTextSegment.encodePlainFeedback` as a single unformatted segment in `homework_submissions.feedback`.

| Field | Type | Emoji rule |
|-------|------|------------|
| `feedbackText` (API) | string \| null | May include Unicode emoji. Manual review comment ≤ 500 UTF-16 units (`MAX_MANUAL_FEEDBACK_LENGTH`). Exercise-only save uses existing encode max (2000). |

Not a formatted writing area: no color/highlight/strike on this comment.

## Classroom emoji set (client-only)

Not persisted as its own entity. A fixed list of 26 default-appearance characters (see [research.md](./research.md)). The picker offers this list; storage accepts any Unicode the field already accepts.

## State

Existing homework statuses and edit locks apply. Emoji insertion is allowed only while the writing area or feedback comment is already editable. After submit / read-only review, display only.

## Validation

| Input | Result |
|-------|--------|
| Classroom-set emoji within limit | stored |
| Other emoji via paste/keyboard within limit | stored (not stripped) |
| Insert that would exceed max | rejected; previous text kept |
| Skin-tone variant via paste | stored |
| Scripts, links, images in paste | still stripped (plain-text paste only) |
