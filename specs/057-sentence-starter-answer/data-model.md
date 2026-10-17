# Data Model: Sentence-Starter Answer

No new tables. One migration widens two `kind` CHECK constraints.

## Migration `V33__sentence_starter_kind.sql`

- `homework_questions.kind` and `activity_questions.kind` CHECKs gain `'SENTENCE_STARTER'`.
- `quiz_questions` is **not** changed (kind not available in quizzes, FR-016).

## Question (`homework_questions` / `activity_questions`)

| Field | Value for `SENTENCE_STARTER` |
|---|---|
| `kind` | `SENTENCE_STARTER` |
| `prompt` | Instruction / stimulus text. Required in homework; optional in activities (page is the stimulus). |
| `structure_json` | `{"items":[{"starter":"porque","modelAnswer":"Porque quiere vivir en una ciudad grande."}, …]}` |
| options | none (rejected if sent) |

**Validation (save)** — `HomeworkAdminService.validateAndMapQuestions` → `SentenceStarters.normalizeStructure`:
- `items`: 1–10 entries, order = display order.
- `starter`: strip, drop trailing `...`/`…` and whitespace; then 1–80 chars. Empty → "Cada inicio de frase necesita texto."
- `modelAnswer`: optional; blank → omitted; ≤ 300 chars.
- Unknown keys dropped. Quiz save with this kind → 422.

**Student view** (`stripStructureForStudent`): `{"starters":["porque","para","por"]}`.

## Answer (`homework_answers` / `activity_answers`) — existing columns

| Column | Value |
|---|---|
| `answer_text` | Composed sentences, one per line, joined by `\n` (later replaced by the teacher's annotated formatted JSON, as for FREE_TEXT). |
| `prompt_snapshot` | Question prompt at submit. |
| `answer_json` | `{"starters":[…],"modelAnswers":[…|null],"continuations":[…]}` — snapshot at submit. |
| `teacher_score_percent`, `score` | As FREE_TEXT (null/0 until teacher scores). |

**Validation (submit)** — `SentenceStarters.composeAnswer(question, continuations)`:
- `continuations.size() == items.size()` else "Las respuestas no coinciden con las preguntas actuales."
- each continuation stripped, internal newlines → space; empty → "Debes responder a todas las preguntas."; > 500 chars → "Una de las respuestas es demasiado larga."
- composed total > 2,000 chars → "Una de las respuestas es demasiado larga."
- `compose(starter, c) = starter + (c starts with , . ; : ! ? … ? "" : " ") + c`.

## Lifecycle

Identical to FREE_TEXT: makes composition `ALL_MANUAL` (only manual kinds) or `MIXED` (with any
auto kind); `PENDING → SUBMITTED → GRADED` when the teacher has a % on every manual question;
contributes one teacher-% to the combined score.

## Kind predicate

`QuestionKind.isManual()` = `FREE_TEXT || SENTENCE_STARTER`; `isStructured()` unchanged (false).
Front-end mirror: `isManualKind(kind)` in `lib/learning.ts` (and admin types in `lib/admin.ts`).
