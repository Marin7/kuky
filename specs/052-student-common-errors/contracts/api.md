# API Contract: Student Common Errors

**Feature**: `052-student-common-errors`

One new admin-only endpoint. No existing endpoint changes.

---

## `GET /api/v1/admin/students/{studentId}/mistakes`

Returns the student's mistakes on auto-graded homework questions for the selected filters: the summary counts, the label options and one page of entries. Read-only: it has no side effects on seen or notification state.

**Auth**: `ADMIN` only. The existing `/api/v1/admin/**` matcher in `SecurityConfig` enforces this.

### Query parameters

| Name | Type | Required | Default | Notes |
|---|---|---|---|---|
| `period` | `ALL` \| `DAYS_30` \| `DAYS_90` | no | `ALL` | Filters on `submittedAt` |
| `label` | string (a `labelOptions[].key`) | no | — (all labels) | Case-insensitive group key. An unknown key is ignored (`appliedLabel: null`). |
| `page` | int | no | `1` | 1-based, clamped to `[1, totalPages]` |

**Lenient parsing.** `GlobalExceptionHandler` has no type-mismatch handler, so the controller takes all three parameters as optional `String`s and normalises them itself. It never answers 400:
- an unrecognised `period` → `ALL`;
- a non-numeric or `< 1` `page` → `1`;
- a blank `label` → none.

The `applied*` / `page` fields in the response always report what was actually served.

### 200 OK

```json
{
  "allTimeMistakeCount": 17,
  "summary": {
    "mistakeCount": 4,
    "answeredCount": 11
  },
  "labelOptions": [
    { "key": "subjuntivo", "label": "Subjuntivo" },
    { "key": "vocabulario", "label": "Vocabulario" }
  ],
  "appliedPeriod": "ALL",
  "appliedLabel": null,
  "page": 1,
  "pageSize": 20,
  "totalPages": 1,
  "totalEntries": 3,
  "entries": [
    {
      "submissionId": "7f0c…",
      "assignmentId": "a12e…",
      "homeworkTitle": "Presente de subjuntivo — práctica",
      "homeworkFormat": "EXERCISE",
      "labels": ["Subjuntivo"],
      "submittedAt": "2026-09-20T17:04:11Z",
      "questionPosition": 2,
      "mistakeCount": 1,
      "question": {
        "id": "q-uuid",
        "kind": "MULTI_BLANK",
        "prompt": "Espero que tú ___ (venir) mañana.",
        "options": [],
        "structure": {}
      },
      "result": {
        "questionId": "q-uuid",
        "score": 0.0,
        "correct": false,
        "correctOptionIds": [],
        "acceptedAnswers": [],
        "unitResults": [
          { "index": 0, "score": 0.0, "correct": false,
            "studentDisplay": "vienes", "expectedDisplay": ["vengas"], "label": null }
        ],
        "selectedOptionIds": []
      }
    }
  ]
}
```

- `question` has the shape of the existing `ExerciseQuestionDto`; `result` has the shape of the existing `ExerciseResultResponse.QuestionResultDto`. The front end reuses its existing `StudentQuestion` / `QuestionResult` types (`front-end/src/lib/learning.ts`).
- `allTimeMistakeCount` ignores `period`, `label` and `page`. It feeds the collapsed box (FR-001).
- `summary` reflects `period` + `label` (FR-010).
- `labelOptions` ignores `period` (research R7).
- `totalEntries` counts list rows, and `summary.mistakeCount` counts wrong items. The two differ only for numbered single-choice questions (research R4).
- Empty states (FR-013), told apart on the client:
  - `summary.answeredCount == 0` → "no auto-graded homework submitted" for these filters. When `allTimeMistakeCount == 0` and there are no label options either, the student has none at all.
  - `answeredCount > 0 && mistakeCount == 0` → "no mistakes for the selected filters", which still shows `answeredCount`.

### Errors

| Status | `error` | When |
|---|---|---|
| 401 | — | Not authenticated |
| 403 | — | Authenticated but not `ADMIN` (`USER` / `STUDENT`) |
| 404 | `STUDENT_NOT_FOUND` | No user with `{studentId}` (existing `StudentNotFoundException`) |

A user who exists but is not currently a `STUDENT` still gets 200 with their history (spec edge case "student status revoked").

---

## Front-end client (`front-end/src/lib/admin.ts`)

```ts
export type MistakePeriod = "ALL" | "DAYS_30" | "DAYS_90";

export interface StudentMistakeEntry {
  submissionId: string;
  assignmentId: string;
  homeworkTitle: string;
  homeworkFormat: LearningHomeworkFormat; // "EXERCISE" | "MIXED"
  labels: string[];
  submittedAt: string;
  questionPosition: number;
  mistakeCount: number;
  question: StudentQuestion;
  result: QuestionResult;
}

export interface StudentMistakes {
  allTimeMistakeCount: number;
  summary: { mistakeCount: number; answeredCount: number };
  labelOptions: { key: string; label: string }[];
  appliedPeriod: MistakePeriod;
  appliedLabel: string | null;
  page: number;
  pageSize: number;
  totalPages: number;
  totalEntries: number;
  entries: StudentMistakeEntry[];
}

export const getStudentMistakes = (
  studentId: string,
  params: { period?: MistakePeriod; label?: string | null; page?: number },
) => apiCall<StudentMistakes>(`/students/${studentId}/mistakes?${/* query string */""}`);
```
