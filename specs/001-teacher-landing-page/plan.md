# Implementation Plan: Spanish Teacher Presentation Site

**Branch**: `001-teacher-landing-page` | **Date**: 2026-06-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-teacher-landing-page/spec.md`

## Summary

Build a fully functional static presentation website for Paula, a Spanish teacher
targeting Romanian students. The homepage delivers a first-impression hero with
photo + key differentiators + CTAs, the About Me page presents Paula's background
and teaching stats, and three placeholder pages (Classes, Bookings, Account)
communicate the planned roadmap. No backend is required for this phase.

## Technical Context

**Language/Version**: TypeScript 5.x (strict)

**Primary Dependencies**: React 19, TanStack Start 1.x + TanStack Router 1.x
(file-based routing, SSR), TailwindCSS 4.x, Shadcn UI (Radix UI primitives),
Lucide React icons, Vite 7 via `@lovable.dev/vite-tanstack-config`

**Storage**: N/A — static data only; no database for this phase

**Testing**: No automated test suite for this phase; manual browser verification
against quickstart.md scenarios is the acceptance gate

**Target Platform**: Web browser — desktop (≥1024 px) and mobile (≥375 px)

**Project Type**: Web application (static presentation site, SSR-ready)

**Performance Goals**: Largest Contentful Paint ≤ 2.5 s on a standard connection;
layout stable (no CLS) on load

**Constraints**: No backend, no authentication, no dynamic data fetching; teacher
photo (`teacher.jpg`) is the only media asset

**Scale/Scope**: Single teacher profile, handful of pages, small audience (tens
to hundreds of monthly visitors)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Notes |
|-----------|-------|-------|
| I. Simplicity First | ✅ PASS | Static site — minimum complexity; no premature abstraction |
| II. Component-Driven UI | ✅ PASS | React + Shadcn/Radix + TailwindCSS already in place |
| III. Evolution-Ready Architecture | ✅ PASS | Static content in isolated data objects; placeholder routes ready for future backend wiring |

**Post-design re-check**: All three principles still satisfied after Phase 1 design.
No complexity violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/001-teacher-landing-page/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── routes.md
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

```text
front-end/
├── src/
│   ├── assets/
│   │   └── teacher.jpg            # Teacher photo asset
│   ├── components/
│   │   ├── ui/                    # Shadcn UI primitives (accordion, button, card …)
│   │   ├── Placeholder.tsx        # Reusable coming-soon placeholder
│   │   └── SiteHeader.tsx         # Sticky nav header + footer
│   ├── routes/
│   │   ├── __root.tsx             # Root layout (header + footer wrapper)
│   │   ├── index.tsx              # / — landing page (hero, features, CTA)
│   │   ├── sobre-mi.tsx           # /sobre-mi — About Me
│   │   ├── clases.tsx             # /clases — Classes placeholder
│   │   ├── reservas.tsx           # /reservas — Bookings placeholder
│   │   └── cuenta.tsx             # /cuenta — Account placeholder
│   ├── hooks/
│   │   └── use-mobile.tsx
│   ├── lib/
│   │   └── utils.ts
│   ├── styles.css
│   ├── router.tsx
│   ├── routeTree.gen.ts
│   ├── server.ts
│   └── start.ts
└── package.json
```

**Structure Decision**: Web application (Option 2 variant). No `backend/` exists
for this phase. All source lives under `front-end/src/`. This aligns with the
constitution's Evolution-Ready principle — `back-end/` will be added separately
in a future phase without restructuring the front-end.
