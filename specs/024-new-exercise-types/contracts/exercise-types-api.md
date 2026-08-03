# API Contract: New Exercise Types

Extends `specs/007-homework-exercises/contracts/api.md`. Same auth, cookies, and `{error,message}` envelope. **Homework only** — placement endpoints unchanged.

```ts
type QuestionKind =
  | "SINGLE_CHOICE"
  | "MULTI_CHOICE"
  | "FILL_BLANK"
  | "MULTI_BLANK"
  | "DRAG_DROP"
  | "TABLE_FILL"
  | "MATCHING";
```

Endpoint paths are unchanged (`/api/v1/admin/homework/**`, `/api/v1/learning/homework/{id}`, `…/answers`). Behaviours below are additive.

---

## Shared structure types (authoring / stored)

```ts
interface MultiBlankStructure {
  blanks: { acceptedAnswers: string[] }[];
}

interface DragDropStructure {
  bank: { id: string; label: string }[]; // index i = correct for blank i
}

interface TableFillStructure {
  rowHeaders: string[];
  colHeaders: string[];
  cells: {
    r: number;
    c: number;
    type: "fixed" | "blank";
    text?: string;                 // fixed
    acceptedAnswers?: string[];    // blank
  }[];
}

interface MatchingStructure {
  left: { id: string; label: string }[];
  right: { id: string; label: string }[];
  pairs: { leftId: string; rightId: string }[];
}

type QuestionStructure =
  | MultiBlankStructure
  | DragDropStructure
  | TableFillStructure
  | MatchingStructure
  | Record<string, never>; // original kinds: {}
```

---

## Teacher — `AdminQuestion` (extended)

```ts
interface AdminQuestion {
  id?: string;
  kind: QuestionKind;
  prompt: string;
  /** Legacy kinds only; MUST be [] for the four new kinds */
  options: { id?: string; label: string; correct: boolean }[];
  /** Required shape for new kinds; {} or omitted for legacy kinds */
  structure?: QuestionStructure;
}
```

### Validation (additive `400 VALIDATION_ERROR`)

- `MULTI_BLANK`: `prompt` has 2–20 exact `___` tokens; `structure.blanks.length` matches; each blank ≥1 accepted answer.
- `DRAG_DROP`: same blank count rules; `structure.bank.length ===` blank count; non-empty labels; unique ids.
- `TABLE_FILL`: valid rectangular cells; 1–50 blanks; each blank ≥1 accepted answer; limits in data-model.
- `MATCHING`: ≥1 pair; ids resolve; no duplicate left/right in pairs; 1–20 per side.
- New kinds with non-empty `options` → validation error.
- Legacy kinds with non-empty meaningful `structure` → ignore or reject; prefer reject if `structure` has unexpected keys (keep simple: allow `{}` only for legacy).

`GET/POST/PUT /api/v1/admin/homework` (and `GET /{id}`) return/accept the extended `AdminQuestion` unchanged otherwise (full question replace on PUT).

---

## Student — take / review

### `StudentQuestion` (extended)

```ts
interface StudentQuestion {
  id: string;
  kind: QuestionKind;
  prompt: string;
  options: { id: string; label: string }[]; // legacy choice only; else []
  /** Answer key stripped — see data-model student projection */
  structure?: {
    // MULTI_BLANK: omit or {}
    bank?: { id: string; label: string }[];           // DRAG_DROP
    rowHeaders?: string[];
    colHeaders?: string[];
    cells?: { r: number; c: number; type: "fixed" | "blank"; text?: string }[]; // TABLE_FILL
    left?: { id: string; label: string }[];           // MATCHING
    right?: { id: string; label: string }[];
    // never includes acceptedAnswers or pairs
  };
}
```

Client shuffles `bank` / `left` / `right` for display; server may return authored order.

### `SubmitExerciseRequest` (extended)

```ts
interface SubmitExerciseRequest {
  answers: {
    questionId: string;
    selectedOptionIds: string[];   // choice; [] otherwise
    answerText: string | null;     // FILL_BLANK only; else null
    answerJson?: unknown | null;   // new kinds — shapes below; null for legacy
  }[];
}

// answerJson by kind:
// MULTI_BLANK: { blanks: string[] }
// DRAG_DROP:   { placements: (string | null)[] }
// TABLE_FILL:  { cells: Record<string, string> }  // "r,c" -> text
// MATCHING:    { pairs: { leftId: string; rightId: string }[] }
```

### `ExerciseResult` (extended)

```ts
interface UnitResult {
  index: number;              // blank index, or flat blank-cell order, or expected-pair index
  score: number;              // 0 or 1
  correct: boolean;
  studentDisplay?: string | null;
  expectedDisplay?: string | string[] | null; // revealed when !correct (and for consistency may always send expected on wrong)
}

interface QuestionResult {
  questionId: string;
  score: number;
  correct: boolean;
  correctOptionIds: string[];     // choice only; else []
  acceptedAnswers: string[];      // FILL_BLANK only; else []
  unitResults?: UnitResult[];     // new multi-unit kinds; omit/empty for legacy
}
```

For `TABLE_FILL`, `index` is enumeration order of blank cells sorted by `(r,c)`. For `MATCHING`, `index` follows authored `pairs` order.

Grading, `409` lock, and `404` rules unchanged.

---

## Error codes

No new codes. Reuse `VALIDATION_ERROR`, `SUBMISSION_NOT_ALLOWED`, `ASSIGNMENT_NOT_FOUND`, `ACCESS_DENIED`.
