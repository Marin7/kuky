# Contract: Placement Test API

Base path `/api/v1`. Conventions match the existing API: JSON, HTTP-only JWT cookie auth (`credentials: 'include'`), current user via `@AuthenticationPrincipal String email`, error bodies `{"error":"CODE","message":"..."}`. Student endpoints are under `/placement/**` (authenticated; anonymous → `401 UNAUTHENTICATED`). Admin endpoints are under `/admin/placement/**` (`ADMIN` role; non-admin → `403 ACCESS_DENIED`).

The Speaking assessment and the evaluation appointment use the **existing** `POST /api/v1/bookings` flow unchanged — no new booking endpoint is defined here.

---

## Student endpoints

### GET `/placement/test`
Returns the active test definition for the current user: section configs (skill, time limit, instructions) and, per skill, the **student view** of questions (no answer key). Includes the user's current in-progress attempt state if any (which sections are started/submitted, remaining time).

`200 OK`
```json
{
  "attemptId": "uuid|null",
  "sections": [
    {
      "skill": "READING",
      "timeLimitSeconds": 600,
      "status": "NOT_STARTED|IN_PROGRESS|SUBMITTED",
      "deadlineAt": "2026-06-30T10:10:00Z|null",
      "questions": [
        { "id": "uuid", "kind": "SINGLE_CHOICE", "prompt": "…",
          "audioUrl": null, "audioFileId": null,
          "options": [ { "id": "uuid", "label": "…" } ] }
      ]
    }
  ]
}
```
Notes: `options` never include `isCorrect`; fill-blank questions return `options: []`. Listening questions may carry `audioUrl` or `audioFileId` (stream via existing `GET /api/v1/audio/{id}`).

### POST `/placement/attempts`
Start (or resume) an attempt for the current user. Idempotent-ish: returns the existing `IN_PROGRESS` attempt if one exists, else creates one.

`201 Created` / `200 OK` → `{ "attemptId": "uuid", "status": "IN_PROGRESS" }`

### POST `/placement/attempts/{attemptId}/sections/{skill}/start`
Begin a section: server stamps `started_at` and computes `deadline_at`. Creating the section row.

`200 OK` → `{ "skill": "READING", "deadlineAt": "2026-06-30T10:10:00Z", "serverNow": "2026-06-30T10:00:00Z" }`

Errors: `404 PLACEMENT_NOT_FOUND` (bad attempt/skill or attempt not owned), `409 SECTION_ALREADY_SUBMITTED`.

### POST `/placement/attempts/{attemptId}/sections/{skill}/submit`
Submit answers for a section; auto-grades and returns the section result. Single submission only. Unanswered questions score 0. A submit after `deadline_at` is still accepted and grades exactly what was sent (the client auto-submits at expiry).

Request:
```json
{ "answers": [
    { "questionId": "uuid", "selectedOptionIds": ["uuid"], "answerText": null }
] }
```
`200 OK`
```json
{
  "skill": "READING",
  "scorePercent": 75,
  "cefrLevel": "B1",
  "questionResults": [
    { "questionId": "uuid", "score": 1.0, "correct": true,
      "correctOptionIds": ["uuid"], "acceptedAnswers": [] }
  ]
}
```
Errors: `409 SECTION_ALREADY_SUBMITTED`, `400 SECTION_NOT_STARTED` (submitting a section that was never started), `404 PLACEMENT_NOT_FOUND`.

### GET `/placement/attempts/{attemptId}/result`
Final combined result once all three sections are submitted.

`200 OK`
```json
{
  "status": "COMPLETED|IN_PROGRESS",
  "overallCefr": "B1",
  "skills": [
    { "skill": "READING", "scorePercent": 75, "cefrLevel": "B1" },
    { "skill": "LISTENING", "scorePercent": 60, "cefrLevel": "A2" },
    { "skill": "GRAMMAR", "scorePercent": 80, "cefrLevel": "B1" }
  ]
}
```
If not all sections are done, `overallCefr` is `null` and `status` is `IN_PROGRESS`.

### GET `/placement/full-evaluation`
Static content for the full-evaluation panel: bank-transfer instructions + writing prompt + the current user's latest writing submission (if any). **No payment data.**

`200 OK`
```json
{
  "paymentInstructions": "Transfiere … IBAN …",
  "writingPrompt": "Escribe 150–200 palabras sobre …",
  "mySubmission": { "id": "uuid", "body": "…", "submittedAt": "…" }
}
```

### POST `/placement/writing`
Submit a Writing response. Trust-based — never gated by payment. Stores the body + a prompt snapshot.

Request: `{ "body": "…" }`
`201 Created` → `{ "id": "uuid", "submittedAt": "…" }`
Errors: `400 VALIDATION_ERROR` (empty body).

> Booking the evaluation appointment is done via the existing `POST /api/v1/bookings` (see `contracts/` of feature 003 / current scheduling API). The front-end links to `/reservas`.

---

## Admin endpoints (`ADMIN` role)

### GET `/admin/placement/config` · PUT `/admin/placement/config`
Read / update the singleton config: per-section time limits, pass threshold, writing prompt, payment instructions.

PUT request:
```json
{ "readingTimeSeconds": 600, "listeningTimeSeconds": 480, "grammarTimeSeconds": 420,
  "passThresholdPercent": 60, "writingPrompt": "…", "paymentInstructions": "…" }
```
`200 OK` → the updated config.

### GET `/admin/placement/questions?skill=READING`
List authored questions for a skill, **including** the answer key (admin view): options with `isCorrect`, `cefrLevel`, audio fields, `active`.

### POST `/admin/placement/questions` · PUT `/admin/placement/questions/{id}` · DELETE `/admin/placement/questions/{id}`
Create / update / delete a question with its options. Validation: choice → ≥2 options with correct count (single = exactly 1, multi = ≥1); fill-blank → ≥1 accepted answer; Listening → at most one audio source. `position` assigned/reordered.

`200/201` → the saved question (admin view). Errors: `400 VALIDATION_ERROR`, `404 PLACEMENT_NOT_FOUND`.

### PUT `/admin/placement/questions/reorder?skill=READING`
Reorder questions within a skill. Request `{ "orderedIds": ["uuid", …] }` → `200 OK`. (Mirrors the existing `ReorderRequest` admin pattern.)

### GET `/admin/placement/students/{studentId}/evaluation`
For the student-detail admin view: the student's latest completed attempt (per-skill CEFR + overall) and their Writing submission(s), together in one payload (FR-015).

`200 OK`
```json
{
  "result": { "overallCefr": "B1", "completedAt": "…",
              "skills": [ { "skill": "READING", "scorePercent": 75, "cefrLevel": "B1" } ] },
  "writing": [ { "id": "uuid", "body": "…", "submittedAt": "…" } ]
}
```
`result`/`writing` may be `null`/empty. Audio upload reuses the existing admin image/audio upload endpoints; no new audio endpoint here.

---

## Error codes (added to `contracts/api.md`)

| Code | HTTP | Meaning |
|------|------|---------|
| `PLACEMENT_NOT_FOUND` | 404 | Attempt/section/question missing or not owned by the caller |
| `SECTION_ALREADY_SUBMITTED` | 409 | Re-submitting an already-graded section |
| `SECTION_NOT_STARTED` | 400 | Submitting a section that was never started |
| `VALIDATION_ERROR` | 400 | Authoring/writing payload failed validation |

Reuses existing `UNAUTHENTICATED` (401) and `ACCESS_DENIED` (403) from `SecurityConfig`.
