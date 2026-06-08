# Research: Spanish Teacher Presentation Site

**Phase**: 0 — Research & unknowns resolution
**Date**: 2026-06-08

## Summary

The technology stack was fully established before this feature was specified.
No external unknowns required external research. All decisions below are
confirmed from the existing codebase (`front-end/package.json`, `vite.config.ts`).

---

## Decision 1: Framework and routing

**Decision**: TanStack Start 1.x with TanStack Router (file-based routing)

**Rationale**: Already bootstrapped via Lovable. Provides SSR out of the box,
file-based routing under `src/routes/`, and clean `head()` API for per-page
meta tags. No migration cost.

**Alternatives considered**: Next.js (not used — different paradigm), Remix
(not used — same SSR space, not already installed), plain Vite SPA (rejected —
SSR gives better SEO for a teacher presentation site).

---

## Decision 2: Styling

**Decision**: TailwindCSS 4.x utility classes + Shadcn UI component library

**Rationale**: TailwindCSS 4 is already configured via
`@lovable.dev/vite-tanstack-config`. Shadcn UI provides accessible, composable
Radix UI primitives already copied into `src/components/ui/`. No additional
setup needed.

**Alternatives considered**: CSS Modules (not needed — TailwindCSS already in
place), plain CSS (less ergonomic for responsive layouts).

---

## Decision 3: Static asset handling

**Decision**: `teacher.jpg` served via Vite's asset pipeline; imported as
`.asset.json` (Lovable convention) which resolves to a CDN URL at build time.

**Rationale**: The import `@/assets/teacher.jpg.asset.json` pattern is already
used in `index.tsx` and `sobre-mi.tsx`. It produces an optimised, cache-busted
URL without manual configuration.

**Alternatives considered**: `<img src="/teacher.jpg">` (no cache busting,
no CDN), `import teacherUrl from "@/assets/teacher.jpg"` (standard Vite but
inconsistent with existing pattern).

---

## Decision 4: Testing approach

**Decision**: Manual browser verification using quickstart.md scenarios; no
automated test suite for this phase.

**Rationale**: The feature is a static presentation page with no business logic
to unit-test. Visual correctness and navigation are best validated by a human
in a browser. Automated tests can be added when the backend is introduced.

**Alternatives considered**: Playwright E2E (overkill for a static site with
no user interactions beyond navigation), Vitest unit tests (nothing to test at
the component level — all content is static).

---

## Decision 5: SEO metadata

**Decision**: Use TanStack Router's `head()` export per route for page-level
`<title>` and `<meta description>`.

**Rationale**: Already established pattern in `index.tsx` and `sobre-mi.tsx`.
Consistent across all routes.

**Alternatives considered**: React Helmet (not installed), manual `document.title`
mutation (SSR-incompatible).

---

## Open items / future phases

| Item | When to address |
|------|----------------|
| Calendly embed for `/reservas` | When scheduling feature is added |
| Authentication for `/cuenta` | When account management is added |
| Classes catalogue backend | When classes/materials feature is added |
| Automated E2E tests | When backend is introduced |
