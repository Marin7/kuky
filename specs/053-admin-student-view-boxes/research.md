# Research: Admin Student View — Summary Boxes for Every Section

No `NEEDS CLARIFICATION` items remained in the Technical Context; the decisions below
record the design choices made while reading the existing code.

## R1 — One generic stat-box component

- **Decision**: Introduce `StudentStatBox` (`components/admin/students/StudentStatBox.tsx`):
  a toggle button showing `count` (`number | null` → `"…"` while loading), `title`,
  optional `breakdown: { label; value }[]` rendered as stacked lines, optional `unseen`
  (renders `NotificationDot` next to the title), and `expandLabel` / `collapseLabel`.
  All five boxes use it. `StudentMistakesBox` is deleted and its call site uses
  `StudentStatBox` directly.
- **Rationale**: The Tareas `<button>` markup in the route and `StudentMistakesBox`
  are already near-identical; three more copies would triple the duplication. One
  component guarantees FR-003's "consistent with Tareas/Errores" by construction.
- **Alternatives considered**: One component per box (5 near-copies — rejected,
  duplication); keeping `StudentMistakesBox` as a thin wrapper (rejected — a wrapper
  with no logic is dead indirection).

## R2 — Stacked breakdown replaces `StudentHomeworkBreakdown`

- **Decision**: Breakdown lines are rendered inside `StudentStatBox`, one line per
  entry: `<value>` in semibold followed by the label, centred, `text-xs`. Delete
  `StudentHomeworkBreakdown.tsx`.
- **Rationale**: Grep shows `StudentHomeworkBreakdown` is used only by the profile
  route (compact variant); the non-compact variant is unused. Once the box renders its
  own lines the component is dead code (constitution: dead code must not be committed).
  The Students tab list is unaffected (it does not use this component).
- **Alternatives considered**: Changing the compact variant to `grid-cols-1`
  (rejected — would keep a component whose other variant is unused and wouldn't
  serve Clases/Pruebas).

## R3 — Breakdown data

- **Decision**:
  - Tareas: existing `homeworkBreakdownFromList` (pending / submitted / completed).
  - Clases: the route's existing `upcoming` / `past` arrays (confirmed bookings split by
    `slotEnd` vs. now) — total = `upcoming.length + past.length`, identical to today's
    count of confirmed bookings.
  - Pruebas: new `quizBreakdownFromList(quizzes)` in `lib/admin.ts` next to
    `homeworkBreakdownFromList` → `{ toReview: status === "SUBMITTED", graded: status === "GRADED" }`.
- **Rationale**: `QuizAdminService.listForStudent` already filters out `IN_PROGRESS`
  attempts, so every listed attempt is `SUBMITTED` or `GRADED` and the two lines sum to
  the total. Keeping the helper in `lib/admin.ts` follows Principle III (logic out of
  components) and mirrors the homework helper.
- **Alternatives considered**: Deriving "por corregir" from per-skill `awaitingTeacher`
  (rejected — `status` is the canonical signal and is what the list badge shows).

## R4 — Unseen indicator

- **Decision**: `unseen` on Tareas = `profile.homeworks.some(h => h.unseen)`; on Pruebas
  = `quizzes.some(q => q.unseen)`. Both fields already exist on the API responses.
  The dot clears naturally because every review action already calls `reloadProfile()`.
- **Rationale**: No backend change; reuses `NotificationDot` for visual consistency with
  the rest of the panel.

## R5 — Expanded state

- **Decision**: Replace `tareasExpanded` / `erroresExpanded` with one
  `useState<Set<StudentBoxKey>>` (or equivalent record) where
  `StudentBoxKey = "classes" | "homework" | "presentations" | "mistakes" | "quizzes"`.
  A `BOX_ORDER` array defines both box order and panel order (FR-002, FR-010).
  State lives in the route component, so `reloadProfile()` (which only refetches
  data) preserves it (FR-011).
- **Rationale**: Five parallel booleans are error-prone; one ordered key list keeps
  box and panel order in sync.

## R6 — Quiz loading placeholder

- **Decision**: `quizzes` state becomes `StudentQuizSummary[] | null` (`null` until the
  first fetch settles); on failure it is set to `[]` as today. The Pruebas box shows
  `"…"` while `null`, matching the Errores box.

## R7 — Section panels

- **Decision**: Move the three standalone sections into named components:
  `StudentClassesPanel` (upcoming + past sub-lists, each with heading + count, empty
  states, no-show toggle via `onToggleNoShow` prop; takes `formatSlot` inputs /
  `teacherTimezone`), `StudentPresentationsPanel`, `StudentQuizzesPanel` (takes
  `onOpenAttempt`). The `Section` helper moves into `StudentClassesPanel` (its only
  remaining user). The homework list stays inline in the route (unchanged code, out of
  scope to move).
- **Rationale**: Principle II (named components) and keeps the route file shorter
  as markup moves anyway.

## R8 — Grid layout for five boxes

- **Decision**: `grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-5`, boxes
  `p-3` (was `p-4`) so five fit in the `max-w-3xl` container (~130 px each). Breakdown
  labels use `text-[11px] leading-tight` and may wrap onto a second line rather than
  truncate.
- **Rationale**: FR-013 / SC-004 — no horizontal scroll, no truncation, on phone
  (2 cols), tablet (3 + 2) and desktop (single row of 5).
- **Alternatives considered**: `lg:grid-cols-5` (rejected — the container width is the
  same at md and lg, so waiting for lg gains nothing); a scrolling carousel (rejected —
  violates FR-013).

## R9 — i18n

- **Decision**: New keys under `admin.studentProfile` in `es.ts`, `en.ts`, `ro.ts`:
  `stats.quizzes`, `classesUpcoming`, `classesPast`, `quizzesToReview`,
  `quizzesGraded`, `expandClasses` / `collapseClasses`,
  `expandPresentations` / `collapsePresentations`, `expandQuizzes` / `collapseQuizzes`.
  Existing keys reused for panel headings and empty states
  (`upcomingClasses`, `pastClasses`, `sharedPresentations`, `quiz.admin.studentQuizzes`,
  `quiz.admin.noStudentQuizzes`, …).
