# Data Model: Admin Student View — Summary Boxes for Every Section

**No persisted data changes.** No migrations, no new API fields. The feature derives
front-end view models from responses the page already loads:

- `GET /api/v1/admin/students/{id}` → `StudentProfile` (`bookings`, `homeworks`, `presentations`)
- `GET /api/v1/admin/students/{id}/quizzes` → `StudentQuizSummary[]` (only `SUBMITTED` / `GRADED` attempts)
- `GET /api/v1/admin/students/{id}/mistakes` → `StudentMistakes` (`allTimeMistakeCount`)

## Front-end view models

### `StudentBoxKey`

`"classes" | "homework" | "presentations" | "mistakes" | "quizzes"` — ordered by
`BOX_ORDER`, which drives both box order (FR-002) and expanded-panel order (FR-010).

### Expanded state

`Set<StudentBoxKey>` held by the route component. Initially empty (FR-004). Toggling a
key adds/removes it; data reloads never touch it (FR-011).

### Box summaries (per box)

| Box | `count` | `breakdown` lines | `unseen` |
|---|---|---|---|
| Clases | `upcoming.length + past.length` (confirmed bookings) | próximas = `upcoming.length`; pasadas = `past.length` | — |
| Tareas | `homeworks.length` | pendientes / entregadas / completadas via `homeworkBreakdownFromList` | `homeworks.some(h => h.unseen)` |
| Presentaciones | `presentations.length` | — | — |
| Errores frecuentes | `mistakes?.allTimeMistakeCount ?? null` | — | — |
| Pruebas de evaluación | `quizzes?.length ?? null` | por corregir = `status === "SUBMITTED"`; corregidas = `status === "GRADED"` via `quizBreakdownFromList` | `quizzes.some(q => q.unseen)` |

`upcoming` = confirmed bookings with `slotEnd > now`; `past` = confirmed with `slotEnd <= now` (unchanged logic).

### `QuizBreakdown` (new, `lib/admin.ts`)

```ts
interface QuizBreakdown { toReview: number; graded: number }
function quizBreakdownFromList(quizzes: Pick<StudentQuizSummary, "status">[]): QuizBreakdown
```

Invariant: `toReview + graded === quizzes.length` (the endpoint never returns `IN_PROGRESS`).
