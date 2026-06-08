# Quickstart & Validation: Spanish Teacher Presentation Site

**Phase**: 1 — Design
**Date**: 2026-06-08

Use this guide to run the site locally and validate all acceptance scenarios
defined in [spec.md](spec.md) and [contracts/routes.md](contracts/routes.md).

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Bun | ≥ 1.x | https://bun.sh |
| Node.js | ≥ 20 (for Vite compat) | https://nodejs.org |

---

## Setup

```bash
cd front-end
bun install
```

---

## Run (development)

```bash
cd front-end
bun run dev
```

The site will be available at **http://localhost:3000** (or the port shown in
the terminal).

---

## Validation scenarios

Work through each scenario in order. Mark it complete once it passes.

### Scenario 1 — First impression (User Story 1, P1)

1. Open http://localhost:3000 in an **incognito** browser tab.
2. **Without scrolling**, verify:
   - Paula's photo is visible.
   - A headline referencing Spanish lessons for Romanian students is visible.
   - Two CTAs are visible: one for booking, one for learning about Paula.
3. Scroll to the features section and verify three cards are displayed, each
   with a title and description.
4. Scroll to the bottom CTA banner and verify a "book now" action is present.

**Pass criteria**: All four checks above pass on both desktop (≥1024 px window)
and mobile (browser DevTools device emulation at 375 × 812 px).

---

### Scenario 2 — About Me page (User Story 2, P2)

1. From the homepage, click "Conóceme" (or "Sobre mí" in the navigation).
2. Verify:
   - Paula's photo loads at a portrait aspect ratio.
   - A bio with at least three paragraphs is visible.
   - A stat grid shows four items: Niveles, Modalidad, Idiomas, Enfoque.
3. Confirm the page title in the browser tab reads
   "Sobre mí — Español con Paula".

**Pass criteria**: All checks pass.

---

### Scenario 3 — Placeholder pages (User Story 3, P3)

For each of the following links in the navigation: **Clases**, **Reservas**,
**Mi cuenta**:

1. Click the navigation link.
2. Verify:
   - The page renders without a blank screen or error.
   - An eyebrow label, a heading, and a descriptive sentence are visible.
   - The browser tab title matches the expected title from
     [contracts/routes.md](contracts/routes.md).

**Pass criteria**: All three placeholder pages pass.

---

### Scenario 4 — Navigation

1. From any page, verify:
   - The sticky header is visible at the top.
   - All five nav links (Inicio, Sobre mí, Clases, Reservas, Mi cuenta) are
     present.
   - The currently active route link is visually distinct from the others.
2. Click each link and confirm it navigates to the correct page without a
   full-page reload (client-side navigation).

**Pass criteria**: All five links work; active state updates correctly.

---

### Scenario 5 — Mobile layout

1. Open http://localhost:3000 in DevTools with device emulation set to
   **375 × 812 px (iPhone SE)**.
2. Verify the homepage hero is readable (text not clipped, photo visible).
3. Verify the navigation is accessible (hamburger menu or collapsed nav,
   depending on implementation).
4. Verify no horizontal scroll appears on any page.

**Pass criteria**: All checks pass at 375 px width.

---

### Scenario 6 — SEO metadata

1. Open DevTools → Elements and inspect `<head>` on each of the five pages.
2. Confirm `<title>` and `<meta name="description">` match the values in
   [contracts/routes.md](contracts/routes.md).

**Pass criteria**: Title and description correct on all five routes.

---

## Build verification

```bash
cd front-end
bun run build
bun run preview
```

Visit the preview URL and repeat Scenarios 1–4 to confirm the production build
behaves identically to the dev build.
