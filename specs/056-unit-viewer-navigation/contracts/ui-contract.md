# UI Contract: Unit Viewer Contents & Level Navigation

**Feature**: `056-unit-viewer-navigation`

This feature exposes **no new HTTP endpoint** and changes no existing one. It consumes `GET /api/v1/learning` (`LearningResponse`) unchanged. The contracts below are the front-end interfaces other components rely on.

## 1. DOM anchors in the unit PDF viewer

Rendered by `PresentationPdfViewerClient` → `PdfPageStack` / `ActivityPageSlot`. Stable, unique per page:

| Element | `id` | Extra |
|---------|------|-------|
| Wrapper of page `n` (title + canvas + activity slot) | `unit-page-{n}` (1-based) | `scroll-mt-24` |
| Activity block `<section>` | `unit-activity-{activityId}` | `scroll-mt-24`; existing `data-activity-after-page` kept |

Anchors exist only while the PDF is loaded; consumers must tolerate their temporary absence (`scrollToUnitAnchor` waits for them).

## 2. `lib/unitAnchors.ts`

```ts
export const pageAnchorId: (page: number) => string;          // "unit-page-{page}"
export const activityAnchorId: (activityId: string) => string; // "unit-activity-{id}"

/** Scroll the window to the anchor; waits up to ~5 s for it to mount, re-aligns once
 *  while earlier pages finish sizing (≤ ~1.5 s), aborts on user wheel/touch/keydown. */
export function scrollToUnitAnchor(anchorId: string): void;
```

## 3. `components/learning/unitOutline.ts`

```ts
export type OutlineEntry = /* see data-model.md */;
export function buildUnitOutline(group: UnitGroup): { entries: OutlineEntry[]; anchorIds: string[] };
export function levelSiblings(groups: UnitGroup[], current: UnitGroup): UnitGroup[];
```

Pure functions, no React — the only logic with branching worth isolating.

## 4. `components/learning/useActiveUnitAnchor.ts`

```ts
/** The anchor id currently being read (reading line = 6rem header + 30 % viewport). */
export function useActiveUnitAnchor(anchorIds: string[]): string | null;
```

## 5. `components/learning/UnitContentsPanel.tsx`

```ts
interface UnitContentsPanelProps {
  groups: UnitGroup[];          // from buildGroups(...)
  current: UnitGroup;           // the open unit (unitId !== null)
  activeAnchorId: string | null;
  onSelectAnchor?: (anchorId: string) => void; // page/activity chosen; default scrollToUnitAnchor
  onNavigate?: () => void;      // a link (sibling unit or quiz) was followed (closes the phone sheet)
}
```

The phone sheet passes `onSelectAnchor` that stores the anchor and closes the sheet; the scroll runs in `SheetContent.onCloseAutoFocus`, once Radix has released the page scroll lock.

Behaviour:

- Heading: `learning.units.levelHeading` → "Nivel {{level}}".
- One row per level sibling, teacher order. Current row: marked (`aria-current="page"`), expanded with its outline as a nested list. Other rows: `<Link to="/aprendizaje/unidad/$unitId">` + `NotificationDot` when unseen.
- Page/activity entries are `<button>`s calling `scrollToUnitAnchor`; the active one gets `aria-current="location"` and highlighted styling; the panel scrolls itself (not the window) to keep it visible.
- Quiz entries are `<Link to="/quizzes/$quizId">` with status text (`quiz.status.*`) and `NotificationDot` when unseen.
- Empty PDF: a muted `learning.units.noPages` line in place of page entries; quizzes still listed.

## 6. i18n keys (added to `es`, `en`, `ro`)

| Key | es |
|-----|----|
| `learning.units.contents` | "Contenido" |
| `learning.units.openContents` | "Ver contenido" |
| `learning.units.levelHeading` | "Nivel {{level}}" |
| `learning.units.noPages` | "Esta unidad aún no tiene páginas." |
| `learning.units.activityLabel` | "Actividad" |
| `learning.units.quizLabel` | "Prueba" |

Existing keys reused: `learning.homework.status.*`, `quiz.status.*`, `notification.item`, `learning.units.back`.
