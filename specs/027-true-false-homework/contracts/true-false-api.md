# API Contract: True/False Homework Exercises

Extends homework exercise APIs from `specs/007-homework-exercises` and `specs/024-new-exercise-types`. Same auth, cookies, and `{error,message}` envelope. **Homework only** — placement endpoints unchanged.

```ts
type QuestionKind =
  | "SINGLE_CHOICE"
  | "MULTI_CHOICE"
  | "MULTI_BLANK"
  | "DRAG_DROP"
  | "TABLE_FILL"
  | "MATCHING"
  | "TRUE_FALSE";
```

Endpoint paths are unchanged (`/api/v1/admin/homework/**`, `/api/v1/learning/homework/{id}`, `…/answers`). Behaviours below are additive.

---

## Teacher — `AdminQuestion` (TRUE_FALSE)

```ts
interface AdminQuestion {
  id?: string;
  kind: QuestionKind;
  prompt: string; // non-empty plain text
  options: {
    id?: string;
    label: string;   // MUST be "true" | "false"
    correct: boolean;
  }[];
  structure?: Record<string, never>; // MUST be {} / omitted
}
```

### Authoring shape for TRUE_FALSE

```json
{
  "kind": "TRUE_FALSE",
  "prompt": "El verbo 'ser' se usa para nacionalidad.",
  "options": [
    { "label": "true", "correct": true },
    { "label": "false", "correct": false }
  ],
  "structure": {}
}
```

Options MUST appear in order: `true` then `false`. Exactly one `correct: true`.

### Validation (additive `400 VALIDATION_ERROR`)

- `TRUE_FALSE`: non-empty `prompt`; exactly 2 options; labels `true` then `false`; exactly one correct; `structure` empty.
- Non-empty `structure` with keys → validation error.
- Wrong option count / editable free-text labels → validation error.

---

## Student — take exercise

`ExerciseQuestionDto` for TRUE_FALSE:

```ts
{
  id: string;
  kind: "TRUE_FALSE";
  prompt: string;
  options: { id: string; label: string }[]; // two options, order true then false;
                                            // labels may be canonical "true"/"false";
                                            // UI maps to localized display strings
  // no structure / stripped
}
```

Answer key (`correct` / which option is right) MUST NOT appear before submit.

### Submit

```ts
{
  questionId: string;
  selectedOptionIds: string[]; // [] or [oneOptionId]
  answerJson: null
}
```

---

## Grading / result

Reuse single-choice semantics:

| Case | Score | Feedback |
|------|-------|----------|
| Selected correct option | 1 | Correct |
| Selected wrong option | 0 | Incorrect; `correctOptionIds` includes the right option |
| No selection | 0 | Incorrect; reveal correct option |

`QuestionResultDto` fields used: `score`, `correct`, `correctOptionIds`, `selectedOptionIds`. No `unitResults` / `acceptedAnswers` for TRUE_FALSE.

---

## UI contracts (non-HTTP)

- **Admin**: Kind selectable as True/False; prompt editor same as other kinds; radio to mark correct true vs false; no add/remove/reorder options.
- **Student**: Radio (or equivalent) for the two localized labels in fixed order; may change selection before submit; may leave unanswered.
- **i18n**: Kind label + True/False option labels in `es` / `en` / `ro` under homework exercise namespaces (not placement).
