# Research: Rich Text Formatting for Writing Homework Feedback

## Decision 1: Rich text representation — structured segments, not HTML

**Decision**: Both the student's Writing answer and the teacher's feedback are stored and transmitted as a JSON array of plain-text segments, each segment carrying only three optional style attributes:

```json
[
  { "text": "This part is fine. " },
  { "text": "This part has a mistake", "strike": true, "color": "red" },
  { "text": " — try instead: ", "color": "neutral" },
  { "text": "una mesa", "highlight": "yellow" }
]
```

- `color`: one of a fixed enum of preset text colors, or absent.
- `highlight`: one of a fixed enum of preset background colors, or absent.
- `strike`: boolean, or absent.
- All three attributes are independent and may be combined on the same segment (per clarification: combinable formatting).

**Rationale**: The feature needs exactly three formatting primitives over plain text — no bold/italic/links/images/lists/headings. Storing free-form HTML would require a server-side and client-side HTML sanitizer (e.g. OWASP Java HTML Sanitizer, DOMPurify) to satisfy FR-008 ("no scripts, links, images, or other markup"), neither of which exists in this codebase today. A constrained JSON schema makes FR-008 true by construction: the data model has no field capable of holding a script, link, or image in the first place, so there is nothing to sanitize away. This is the simplest option that satisfies every functional requirement, matching the constitution's Simplicity First principle.

**Alternatives considered**:
- *Sanitized HTML string* (rejected): requires adding a new sanitization dependency on both front-end and back-end, and still leaves a residual risk surface (sanitizer bugs, allow-list drift) for a feature that only needs 3 style primitives.
- *Markdown-like syntax* (e.g. `~~text~~` for strikethrough, custom syntax for color) (rejected): would need a bespoke parser for color/highlight syntax that doesn't exist in Markdown, and pushes parsing complexity onto both ends for no benefit over structured JSON.

## Decision 2: Editor implementation — small custom formatting component, not a rich-text library

**Decision**: Build a minimal custom React component (`RichTextEditor`) that lets the user select a range of the text they're typing and apply/remove color, highlight, or strikethrough to that range, backed directly by the segment array above (no `contentEditable`, no third-party rich-text library).

**Rationale**: The codebase has zero rich-text editor dependencies today (confirmed: no Tiptap, Slate, Quill, Lexical, or Draft.js in `front-end/package.json`). Pulling in one of these libraries to support exactly 3 formatting primitives is disproportionate — they bring their own document models, serialization formats, and upgrade surface for capabilities (tables, embeds, collaborative editing) this feature will never use. A small selection-driven component that operates on plain text plus a segment array is well within reach using native `textarea` selection APIs (`selectionStart`/`selectionEnd`) for editing, paired with a read-only `RichTextViewer` component that simply maps segments to styled `<span>` elements for display. This keeps the feature aligned with Simplicity First and Component-Driven UI (both editor and viewer are new, named, reusable components built from existing Tailwind/Shadcn primitives for the toolbar buttons).

**Alternatives considered**:
- *Tiptap/Slate/Lexical* (rejected): large new dependency, steep integration cost, most capabilities unused.
- *`contentEditable` with `document.execCommand`* (rejected): `execCommand` is deprecated, produces inconsistent/uncontrolled HTML across browsers, and would reintroduce the HTML-sanitization problem Decision 1 avoids.

## Decision 3: Data storage — extend the existing `homework_submissions` table

**Decision**: Reuse the existing `homework_submissions` table (no new table/entity). Add two nullable columns via a new Flyway migration (`V28`):
- `feedback TEXT` — JSON-encoded segment array for the teacher's feedback (null until reviewed).
- `reviewed_at TIMESTAMPTZ` — set when the teacher saves feedback and the submission transitions to `REVIEWED`.

`response_text` is kept (same column, same name) but its *contents* change going forward: newly submitted answers store a JSON-encoded segment array instead of a raw plain string. A migration step converts any existing plain-text `response_text` values into the equivalent single-segment JSON form (`[{"text": "<existing value>"}]`) so old submissions continue to render correctly under the new format with zero formatting applied — no data loss, no visible behavior change for existing (unformatted) answers.

**Rationale**: `homework_submissions` already has the right shape (one row per student/assignment, existing status lifecycle including `REVIEWED`). No reviewer-identity column is added since this app has exactly one teacher account (`ADMIN` role) — adding one would be speculative (YAGNI).

**Alternatives considered**:
- *New `homework_feedback` table* (rejected): a 1:1 relationship with `homework_submissions` that adds a join for no benefit; the existing table already models "one submission, one lifecycle."
- *Renaming `response_text`* (rejected): unnecessary churn; the column's purpose (holding the student's answer) is unchanged, only its serialization is.

## Decision 4: Backend review endpoints — extend existing admin homework surface

**Decision**: Add:
- `GET /api/v1/admin/homework/submissions?status=SUBMITTED` — the cross-student review queue (FR-010), returning submission id, student, assignment title, submitted-at, for all Writing/MANUAL submissions in `SUBMITTED` status.
- `GET /api/v1/admin/homework/submissions/{submissionId}` — full detail (student's formatted answer) for the review screen.
- `PUT /api/v1/admin/homework/submissions/{submissionId}/feedback` — teacher saves formatted feedback; validates non-empty content (edge case: no blank reviews), sets `status = REVIEWED`, `reviewed_at = now()`, rejects if already `REVIEWED` (terminal, FR-007).

`StudentProfileHomeworkDto` (used by the existing per-student profile endpoint, FR-010's per-student indicator) gains a `needsReview: boolean` field (true when status is `SUBMITTED` and format is MANUAL) so the existing student-profile screen can show the indicator without a separate call.

**Rationale**: `HomeworkAdminController`/`HomeworkAdminService` already own the admin-side homework surface (assignment authoring); the review endpoints are new methods in the same package, following the existing convention rather than introducing a new controller. No email notification logic is added (FR-012: in-app only).

## Decision 5: Preset color palette

**Decision**: A fixed set of 4 text colors (red, green, blue, neutral/black) and 3 highlight colors (yellow, green, pink), specified as concrete values in `data-model.md`. Exact hex values are a presentation-layer (Tailwind token) detail, not a spec-level concern; the constraint that matters is that both ends of the API only ever accept one of these named tokens, never an arbitrary color string.

**Rationale**: Small, fixed, mutually distinguishable — enough to mark "correct/error/suggestion/neutral" style feedback without becoming a full color-picker UI (per spec Assumption on preset palette).

## Testing approach

- **Backend**: JUnit 5 + Spring Boot Test, following existing conventions in `back-end/src/test/java/com/kuky/backend/learning/` (e.g. `HomeworkSubmissionServiceTest.java`) and `.../admin/` (e.g. `HomeworkAdminServiceTest.java`). New tests cover: segment validation/sanitization-by-construction, the SUBMITTED→REVIEWED transition, rejecting edits after REVIEWED, and the new admin endpoints (unit + `*ControllerIntegrationTest` style).
- **Frontend**: No test framework exists in `front-end/` today (confirmed: no Vitest/Jest/Playwright/Testing Library in `package.json`). Consistent with the constitution's Development Workflow rule ("All UI changes MUST be verified in a running browser before the task is marked complete"), this feature is verified manually in a running browser rather than introducing a new frontend test framework — adding one would be a scope increase unrelated to this feature's actual requirement (YAGNI).

## Resolved Technical Context unknowns

All Technical Context fields are resolved from the existing codebase (see CLAUDE.md and the research above); none require further clarification.
