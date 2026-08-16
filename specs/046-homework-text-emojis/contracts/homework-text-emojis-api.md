# Contract: Homework text emojis

**Feature**: `046-homework-text-emojis` | **Date**: 2026-08-16

No new endpoints. Auth and error envelope unchanged.

Emoji characters are valid content in existing string fields. Clients MUST send them as UTF-8 JSON strings (normal `fetch` / Jackson). Servers MUST persist and return the same code points (appearance may differ by device).

## Student submit (unchanged paths)

`POST /api/v1/learning/homework/{id}/submit` (WRITE body `response` segments) and mixed/FREE_TEXT answer payloads:

| JSON | Emoji |
|------|--------|
| `response[].text` | May contain emoji; visible length ≤ 2000 (existing) |
| mixed FREE_TEXT `text` / formatted `text` | Same |

Example WRITE segment:

```json
{ "text": "Gracias 👍", "color": "green" }
```

## Teacher feedback (unchanged paths)

`PUT /api/v1/admin/homework/submissions/{id}/feedback` (manual / WRITE / mixed) body field `feedbackText`:

| JSON | Emoji |
|------|--------|
| `feedbackText` | May contain emoji; ≤ 500 UTF-16 units |

`PUT` (or existing save) exercise teacher feedback on a graded exercise: `feedback` / `teacherFeedback` string may contain emoji; existing 2000 max on that path.

Responses that already echo `feedbackText` or formatted `response` MUST include those emojis when re-read.

## Out of contract

Quizzes, presentation activities, homework authoring bodies, and teacher `formatOnly` markup of the student answer are unchanged and are not required to grow an emoji picker.
