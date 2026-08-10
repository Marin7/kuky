# Contract: Multi-Question Manual Homework API

**Feature**: `031-manual-multi-questions` | **Date**: 2026-08-10

Extends existing learning + admin homework/activity endpoints. Auth: `STUDENT`/`ADMIN` for learning; `ADMIN` for authoring/review. Errors use `{"error":"ERROR_CODE","message":"..."}`.

## Authoring (admin)

### Create / update homework

`POST /api/v1/admin/homework` · `PUT /api/v1/admin/homework/{id}`

Request (relevant fields):

```json
{
  "title": "Escucha — noticia",
  "instructions": "Escucha el audio y responde.",
  "homeworkType": "AUDIO",
  "format": "MANUAL",
  "audioFileId": "…",
  "questions": [
    { "kind": "FREE_TEXT", "prompt": "¿Qué palabra se repite?", "options": [], "structure": {} },
    { "kind": "FREE_TEXT", "prompt": "¿Cuál es el tema?", "options": [], "structure": {} }
  ],
  "assigneeIds": ["…"]
}
```

| Case | `questions` |
|------|-------------|
| MANUAL + WRITE | `[]` required |
| MANUAL + non-WRITE | ≥1, all `FREE_TEXT` |
| EXERCISE | existing kinds only; **no** `FREE_TEXT` |

`VALIDATION_ERROR` if rules fail (Spanish messages as today).

### Create / update activity

`POST /api/v1/admin/activities` · `PUT /api/v1/admin/activities/{id}` — same MANUAL question rules as non-WRITE MANUAL (activities have no WRITE type).

### Admin homework detail

`GET /api/v1/admin/homework/{id}` — for MANUAL non-WRITE, `questions` includes FREE_TEXT prompts (no answer key). WRITE: `questions: []`.

## Student learning

### List / item payload

`GET /api/v1/learning` (and activity item embeds): MANUAL non-WRITE items include:

```json
{
  "id": "…",
  "format": "MANUAL",
  "homeworkType": "AUDIO",
  "instructions": "…",
  "audioUrl": "…",
  "status": "PENDING",
  "questions": [
    { "id": "q1", "kind": "FREE_TEXT", "prompt": "¿Qué palabra se repite?", "position": 0 }
  ],
  "answers": null,
  "response": null,
  "feedback": null
}
```

After submit (and on reload):

```json
{
  "answers": [
    { "questionId": "q1", "promptSnapshot": "¿Qué palabra se repite?", "text": "democracia" }
  ],
  "response": null,
  "status": "SUBMITTED"
}
```

WRITE items keep `response: FormattedText` and omit multi `questions`/`answers` (or empty arrays).

### Submit MANUAL homework

`PUT /api/v1/learning/homework/{assignmentId}`

**WRITE:**

```json
{ "response": [ { "text": "…", "color": null, "highlight": null, "strike": false } ] }
```

**Multi MANUAL:**

```json
{
  "answers": [
    { "questionId": "q1", "text": "democracia" },
    { "questionId": "q2", "text": "la política" }
  ]
}
```

| Condition | Error |
|-----------|--------|
| Any blank / missing question id vs current list | `VALIDATION_ERROR` |
| Already `REVIEWED` | `SUBMISSION_NOT_ALLOWED` |
| Format EXERCISE | `SUBMISSION_NOT_ALLOWED` / bad request (use `/answers`) |
| WRITE with `answers` or multi with `response` | `VALIDATION_ERROR` |

### Submit MANUAL activity

`PUT /api/v1/learning/activities/{id}` — same body rules as multi MANUAL homework.

## Teacher review

### Queue

`GET /api/v1/admin/homework/submissions` — unchanged (MANUAL SUBMITTED queue). Activity review queue if already present follows the same enrichment.

### Submission detail

`GET /api/v1/admin/homework/submissions/{submissionId}`

```json
{
  "submissionId": "…",
  "assignmentTitle": "…",
  "status": "SUBMITTED",
  "response": null,
  "answers": [
    { "questionId": "q1", "promptSnapshot": "¿Qué palabra se repite?", "text": "democracia" },
    { "questionId": null, "promptSnapshot": "Pregunta eliminada…", "text": "…" }
  ],
  "feedback": null,
  "submittedAt": "…"
}
```

WRITE: `response` populated; `answers` empty/null.

### Save feedback

`PUT /api/v1/admin/homework/submissions/{submissionId}/feedback` — unchanged body (`feedback` FormattedText); marks `REVIEWED`.

## Unchanged

- All EXERCISE get/submit/grade endpoints and DTOs.
- WRITE rich-text submit/review behavior (aside from coexistence with new fields).
