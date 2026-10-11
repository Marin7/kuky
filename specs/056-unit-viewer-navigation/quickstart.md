# Quickstart: Verify Unit Viewer Contents & Level Navigation

**Feature**: `056-unit-viewer-navigation`

Front-end only; no migration. See [ui-contract.md](contracts/ui-contract.md) and [data-model.md](data-model.md) for the interfaces being verified.

## Prerequisites

- PostgreSQL `kuky_dev` + Mailpit running (see `CLAUDE.md` → Local dev setup).
- Back-end: `cd back-end && ./gradlew bootRun --args='--spring.profiles.active=local'` (→ :8081).
- Front-end: `cd front-end && npm run dev` (→ :8080).

## Test data (as the teacher, in `/panel`)

1. In **Unidades**, create three **A2** units ("La familia", "La casa", "El trabajo"), in that order, and one **B1** unit.
2. Upload a PDF of **≥ 12 pages** to "La casa"; rename page 1 to "Vocabulario"; mark pages 5 and 9 as activities ("Completa el diálogo", "Ordena las frases") with at least one question each.
3. Upload a short PDF to "La familia". Leave "El trabajo" without a PDF but link **one quiz** to its end. Link **two quizzes** to the end of "La casa".
4. Assign all four units to a test student account that has `STUDENT` role.

## Scenarios

| # | Steps (as the student) | Expected |
|---|------------------------|----------|
| 1 | Open **Mi aprendizaje** → "La casa" (desktop width) | Left panel headed "Nivel A2" lists La familia, **La casa** (current, expanded), El trabajo — no B1 unit. Expanded list: all page titles in order ("Vocabulario" first), "Completa el diálogo" indented under page 5, "Ordena las frases" under page 9, then the two quizzes. |
| 2 | Click page 8's entry right after load | Page 8's title lands just below the site header, even if earlier pages were still rendering. |
| 3 | Start typing an answer in the page 5 activity, click page 2, then click "Completa el diálogo" | View returns to the activity block; the typed answer is still there. |
| 4 | Scroll slowly from page 2 to page 10 | The highlighted entry follows the page/activity being read; the panel scrolls itself to keep it visible; the panel stays in view (sticky). |
| 5 | Submit the page 9 activity | Its contents entry switches from pending to done (check icon); no score shown. |
| 6 | Click a quiz entry | The existing quiz page `/quizzes/{id}` opens. |
| 7 | Teacher marks new content on "La familia" so it shows an unseen dot on Mi aprendizaje; student reloads "La casa" | "La familia" row shows the dot; clicking it opens La familia at the top, its panel expanded, dot cleared (also on Mi aprendizaje). |
| 8 | Open "El trabajo" | "Esta unidad no tiene contenido todavía." in the main column; panel shows "Esta unidad aún no tiene páginas." plus its quiz; sibling links work. |
| 9 | Teacher renames page 3; student reloads | Panel and the title above page 3 both show the new name. |
| 10 | Resize to 375 px (or `resize_window` preset `mobile`) | No left column; PDF full width; no horizontal scroll; a "Ver contenido" button is visible. Tap it → sheet opens from the left with the same panel; tap an entry → sheet closes and the target scrolls into view. |
| 11 | Open **Mi aprendizaje** → "Otros materiales" | Unchanged: no contents panel. |
| 12 | Sign in as the teacher (ADMIN) and open `/aprendizaje/unidad/{La casa id}` | Same layout; lists the units visible to that account (none unless assigned — "not found" behaviour as today). |

## Build checks

```bash
cd front-end && npm run lint && npm run build
```

Both must pass with no new warnings.
