# UI Contract: Admin Student Profile Boxes

No REST contract changes. This documents the component interfaces the route composes.
All live in `front-end/src/components/admin/students/`.

## `StudentStatBox`

```ts
interface StudentStatBoxProps {
  title: string;
  /** null → renders "…" (loading / unavailable). */
  count: number | null;
  /** Optional stacked lines, one per entry: "<value> <label>". */
  breakdown?: { label: string; value: number }[];
  /** Shows NotificationDot beside the title. */
  unseen?: boolean;
  expanded: boolean;
  onToggle: () => void;
  expandLabel: string;
  collapseLabel: string;
}
```

Behaviour:
- Renders a `<button type="button" aria-expanded={expanded}>`; clicking calls `onToggle`.
- Expanded → `ring-2 ring-primary/30`; hint text switches `expandLabel` ↔ `collapseLabel`.
- Breakdown lines stack vertically (one per line); labels wrap, never truncate.
- Replaces `StudentMistakesBox` and the inline Tareas button; `StudentHomeworkBreakdown` is removed.

## `StudentClassesPanel`

```ts
interface StudentClassesPanelProps {
  upcoming: StudentProfileBooking[];
  past: StudentProfileBooking[];
  teacherTimezone: string;
  onToggleNoShow: (bookingId: string, noShow: boolean, isCompanionStudent: boolean) => void;
}
```

Renders two sub-sections ("Próximas clases" / "Clases pasadas", each with count badge and
existing empty state), identical row content to today's sections (slot, Zoom link on
upcoming, no-show toggle on past).

## `StudentPresentationsPanel`

```ts
interface StudentPresentationsPanelProps {
  presentations: StudentProfilePresentation[];
}
```

Title + level rows; `emptyPresentations` when empty.

## `StudentQuizzesPanel`

```ts
interface StudentQuizzesPanelProps {
  quizzes: StudentQuizSummary[];
  onOpenAttempt: (quizId: string, attemptId: string) => void;
}
```

Identical content to today's quiz section (title + unseen dot, status · score,
per-skill grid, view/review action); `quiz.admin.noStudentQuizzes` when empty.

_Type names for booking/presentation rows follow whatever `StudentProfile` exposes in
`lib/admin.ts` (e.g. `StudentProfile["bookings"][number]`)._
