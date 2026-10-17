# API Contract: Sentence-Starter Answer

No new endpoints. Existing payloads gain one kind and two optional fields.

## Authoring — homework & activity question DTO (`HomeworkQuestionDto`)

```json
{
  "id": "…", "kind": "SENTENCE_STARTER",
  "prompt": "Lee el texto: ¿por qué se va Ana a Madrid?",
  "options": [],
  "structure": { "items": [
    { "starter": "porque...", "modelAnswer": "Porque ha encontrado trabajo allí." },
    { "starter": "para" }
  ] }
}
```
Response/stored structure has normalised starters (`"porque"`). Errors: 422 `VALIDATION_ERROR`
(0 or >10 items, empty/long starter, long model answer, options present).
Quiz create/update with this kind → 422 `VALIDATION_ERROR` "Este tipo de pregunta no está disponible en las pruebas."

## Student question DTO (`ExerciseQuestionDto`)

`{"id":"…","kind":"SENTENCE_STARTER","prompt":"…","options":[],"structure":{"starters":["porque","para"]}}`
— never contains `modelAnswer`.

## Submit

**Unified `/answers` (MIXED, from `MixedHomeworkForm`)** — `SubmitExerciseRequest.AnswerDto`:
```json
{ "questionId": "…", "selectedOptionIds": [], "answerJson": { "continuations": ["quiere cambiar de vida.", "estar cerca de su familia."] } }
```
**ALL_MANUAL (`ManualMultiAnswerForm`)** — `ManualAnswerDto` gains optional `continuations`:
```json
{ "questionId": "…", "text": null, "continuations": ["…", "…"] }
```
Same rules for homework and activity submit endpoints. Errors: 422 `VALIDATION_ERROR` with the
messages in [data-model.md](../data-model.md).

## Answer view (`ManualAnswerViewDto`) — two new nullable fields

```json
{
  "questionId": "…", "promptSnapshot": "…",
  "text": "porque quiere cambiar de vida.\npara estar cerca de su familia.",
  "formatted": null, "teacherScorePercent": 80, "score": 0.8,
  "starters": ["porque", "para"],
  "modelAnswers": ["Porque ha encontrado trabajo allí.", null]
}
```
- `starters`: present for `SENTENCE_STARTER` answers, null otherwise.
- `modelAnswers`: present only when teacher scores are visible (teacher always; student after
  finalisation); null otherwise. Same shape on activity answer views and admin review detail.

## Review / annotation

Unchanged endpoints and bodies; the manual-answer annotation/percent checks accept any
`isManual()` kind instead of only `FREE_TEXT`.
