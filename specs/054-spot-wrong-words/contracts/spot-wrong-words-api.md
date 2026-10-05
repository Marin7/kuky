# Contract: SPOT_WRONG_WORDS question kind

No new endpoints. The new kind travels through the existing question/answer payloads.
Error envelope as in `contracts/api.md`: `{"error":"VALIDATION_ERROR","message":"…"}`.

## Authoring (admin) — homework, activities, quizzes

Existing create/update endpoints for homework (`/api/v1/admin/homework…`), presentation
activities and quizzes accept a question with:

```json
{
  "id": "…optional uuid…",
  "kind": "SPOT_WRONG_WORDS",
  "prompt": "Ayer yo sabo que mi hermana estás en Madrid.",
  "options": [],
  "structure": {
    "errors": [
      { "wordIndex": 2, "word": "sabo", "correction": "supe" },
      { "wordIndex": 7, "word": "estás", "correction": "estaba" }
    ]
  }
}
```

- Response echoes the normalised structure (sorted by `wordIndex`, trimmed corrections, blank → `null`).
- Invalid → **422** `VALIDATION_ERROR` with one of the messages in [data-model.md](../data-model.md).
- Admin reads return the full structure (answer key included), like other kinds.

## Taking (student) — homework exercise, activity, quiz

Question as delivered before submit:

```json
{
  "id": "…",
  "kind": "SPOT_WRONG_WORDS",
  "prompt": "Ayer yo sabo que mi hermana estás en Madrid.",
  "options": [],
  "structure": { "errorCount": 2 }
}
```

Answer item in the existing submit body:

```json
{ "questionId": "…", "selectedOptionIds": [], "answerJson": { "selected": [2, 5] } }
```

- More valid indices than `errorCount` → **422** `VALIDATION_ERROR` "Has marcado más palabras que errores." (nothing saved).
- Out-of-range / duplicate / non-integer indices are silently dropped.

## Result (student and teacher)

Inside the existing `questions[]` of the exercise / activity / quiz result:

```json
{
  "questionId": "…",
  "score": 0.5,
  "correct": false,
  "correctOptionIds": [],
  "acceptedAnswers": [],
  "selectedOptionIds": [],
  "unitResults": [
    { "index": 2, "score": 1, "correct": true,  "studentDisplay": "sabo", "expectedDisplay": ["supe"],   "label": null },
    { "index": 5, "score": 0, "correct": false, "studentDisplay": "mi",   "expectedDisplay": [],         "label": "EXTRA" },
    { "index": 7, "score": 0, "correct": false, "studentDisplay": null,   "expectedDisplay": ["estaba"], "label": null }
  ]
}
```

- Overall `%` / `fullyCorrectCount`: the question contributes `score` once.
- Errores frecuentes (`GET /api/v1/admin/students/{id}/mistakes`): an entry appears when `correct=false`, with `wrongCount = 1`, carrying the same `question` (snapshot) and `result`.
