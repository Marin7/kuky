# Data Model: Spot the Wrong Words

No new tables. The new kind uses the existing question rows (`homework_questions`,
`activity_questions`, `quiz_questions`) and answer rows (`answer_json`, `score`).

## Schema change

`V27__spot_wrong_words_kind.sql`: drop and re-create the three `*_kind_check` constraints
with `'SPOT_WRONG_WORDS'` added to the existing eight values. No data migration.

## Word segmentation (shared rule)

```
word  := [\p{L}\p{M}\p{N}]+ ( ['’-] [\p{L}\p{M}\p{N}]+ )*
```

Words are numbered 0…n-1 in order. Text between words is a gap (rendered verbatim, never selectable).

**Shared test vectors** (asserted in `SpotWrongWordsTest`, checked in quickstart §1):

| Input | Words |
|---|---|
| `¿Dónde está el baño?` | `Dónde`, `está`, `el`, `baño` |
| `Yo sabo que l'hotel es bien-estar.` | `Yo`, `sabo`, `que`, `l'hotel`, `es`, `bien-estar` |
| `En 1990, mi madre —y yo— fuimos.` | `En`, `1990`, `mi`, `madre`, `y`, `yo`, `fuimos` |
| `rock - and` | `rock`, `and` |
| `d’Artagnan` | `d’Artagnan` |
| `la la la` | `la`, `la`, `la` (three independent words) |
| `¡Hola!\n\nAdiós.` | `Hola`, `Adiós` (paragraph gap preserved) |

## Question (`kind = SPOT_WRONG_WORDS`)

| Field | Type | Rules |
|---|---|---|
| `prompt` | text | The passage. Required, non-blank (existing rule), ≤ 3,000 chars after strip, ≥ 1 word. |
| `structure_json.errors` | array | 1–30 entries, sorted by `wordIndex`, unique `wordIndex`. |
| `errors[].wordIndex` | int | 0 ≤ i < word count of `prompt`. |
| `errors[].word` | string | Must equal the token at `wordIndex` (exact, case- and accent-sensitive). |
| `errors[].correction` | string \| null | Trimmed; blank → `null`; ≤ 100 chars. |
| `options` | — | Must be empty (existing structured-kind rule). |

Validation messages (`IllegalArgumentException` → `VALIDATION_ERROR` 422), Spanish:

- `Marca al menos una palabra incorrecta.` (0 errors)
- `Puedes marcar como máximo 30 palabras incorrectas.`
- `El texto puede tener como máximo 3000 caracteres.`
- `Una palabra marcada ya no coincide con el texto. Vuelve a marcarla.` (index out of range or `word` mismatch)
- `Una palabra está marcada dos veces.` (duplicate index)
- `La corrección puede tener como máximo 100 caracteres.`

## Student question structure (pre-submit)

```json
{ "errorCount": 3 }
```

No indices, words or corrections (FR-009). The client tokenizes `prompt` to render words.

## Student answer (`answer_json`)

```json
{ "selected": [1, 5] }
```

Server normalisation before grading: keep integers within `[0, wordCount)`, de-duplicate.
If the remaining count > `errorCount` → `VALIDATION_ERROR` 422 `Has marcado más palabras que errores.`
(whole submission refused). Missing/empty → nothing selected (score 0).

Stored as given (normalised) in `answer_json`; contributes to the existing draft object in
`localStorage` before submit.

## Grading result

`score = |selected ∩ errorIndices| / errorCount` (one contribution; `correct` iff 1.0).

`unitResults` (ordered by `index` = word index), reusing `UnitResultDto`:

| State | `score` | `correct` | `studentDisplay` | `expectedDisplay` | `label` |
|---|---|---|---|---|---|
| Found | 1 | true | word | `[correction]` or `[]` | null |
| Missed | 0 | false | null | `[correction]` or `[]` | null |
| Wrongly marked | 0 | false | word | `[]` | `"EXTRA"` |

Unit scores are display-only; the question `score` is never derived from them.

## State / lifecycle

Unchanged. The kind is auto-gradable: all-auto homework → `GRADED` on submit; mixed → `SUBMITTED`
until the teacher grades FREE_TEXT parts. Quizzes and activities follow their existing flows.

## Editor-only model (front-end)

```ts
type SpotWrongWordsStructure = {
  errors: { wordIndex: number; word: string; correction: string | null }[];
};
```

On prompt change: `errors = carryMarks(tokenize(oldPrompt), tokenize(newPrompt), errors)` —
LCS match on token text; matched errors move to their new index, unmatched are dropped (FR-002b).
