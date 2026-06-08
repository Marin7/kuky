<!--
SYNC IMPACT REPORT
==================
Version change: (template) → 1.0.0
Modified principles: all placeholders replaced; reduced from 5 slots to 3
Added sections: Technology Stack, Development Workflow
Removed sections: PRINCIPLE_4, PRINCIPLE_5 (not needed at current scope)
Templates requiring updates:
  ✅ plan-template.md — Constitution Check section already generic; no changes needed
  ✅ spec-template.md — no constitution-specific references; no changes needed
  ✅ tasks-template.md — no constitution-specific references; no changes needed
Deferred TODOs: none
-->

# Kuky Constitution

## Core Principles

### I. Simplicity First

Every feature MUST start as the simplest possible implementation. No abstraction,
pattern, or dependency may be introduced unless a concrete, present need exists.
YAGNI (You Aren't Gonna Need It) is non-negotiable: the project begins as a
static presentation page and complexity is added incrementally only when a real
requirement arrives.

### II. Component-Driven UI

All UI MUST be built from React components using the established stack: React 19,
TanStack Router (file-based routing), TailwindCSS v4, and Shadcn UI (Radix UI
primitives). Raw DOM manipulation is not permitted. Every visual element MUST be
a named, reusable component — even if currently used only once.

### III. Evolution-Ready Architecture

The codebase MUST be structured so that introducing a backend does not require
rewriting the front-end. Data-fetching logic MUST be isolated in dedicated hooks
or service modules (never inlined in components). Static data used today MUST
live in the same locations where API calls will live tomorrow.

## Technology Stack

- **Language**: TypeScript 5.x (strict mode)
- **Framework**: React 19 + TanStack Start (SSR) + TanStack Router (file-based)
- **Styling**: TailwindCSS v4 + Shadcn UI (Radix UI primitives)
- **Build**: Vite 7 via `@lovable.dev/vite-tanstack-config`
- **Package manager**: Bun
- **Future backend**: to be determined — backend code MUST live under `back-end/`

## Development Workflow

- All UI changes MUST be verified in a running browser before the task is marked
  complete — type-checking and builds are not a substitute for visual verification.
- Feature branches MUST follow the naming convention enforced by the git extension
  (`speckit-git-feature`).
- Dead code MUST NOT be committed; remove rather than comment out.

## Governance

This constitution supersedes all other informal practices. Any amendment requires:
1. A version bump following semantic versioning (MAJOR for breaking principle
   changes, MINOR for new sections, PATCH for clarifications).
2. An updated Sync Impact Report embedded as an HTML comment at the top of this
   file.
3. A commit with message format: `docs: amend constitution to vX.Y.Z (<summary>)`.

All implementation plans MUST include a Constitution Check gate before Phase 0.

**Version**: 1.0.0 | **Ratified**: 2026-06-08 | **Last Amended**: 2026-06-08
