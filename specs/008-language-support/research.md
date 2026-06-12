# Research: Multi-Language Support

## Decision 1: i18n Library

**Decision**: Use `react-i18next` + `i18next`

**Rationale**: The site has ~8 pages and ~30+ components with hardcoded Spanish strings. A custom context solution would work but would require hand-rolling interpolation, pluralization, and namespace splitting — all solved problems in `react-i18next`. It is the industry standard for React i18n, has excellent TypeScript support, and integrates cleanly with a client-side-only language switch (no SSR namespace loading required given our architecture choice).

**Alternatives considered**:
- **Custom React context + TS dictionaries**: Zero deps, fully typed, simpler setup. Rejected because interpolation and maintainability at ~300+ keys would require re-implementing `t('key', {name})` patterns that i18next handles well.
- **lingui**: Excellent type safety via code extraction. Rejected because compile-time extraction adds build complexity that conflicts with Constitution Principle I (simplicity first).
- **next-intl / @formatjs/intl**: Framework-specific or heavier. Not applicable.

---

## Decision 2: Translation File Format

**Decision**: TypeScript module per language exporting a plain object (`es.ts`, `ro.ts`, `en.ts`)

**Rationale**: JSON files are the i18next default but TypeScript objects give compile-time key checking for free when combined with a typed `t()` helper. A single flat namespace is sufficient at this scale; nested keys can be introduced later if needed.

**Alternatives considered**:
- **JSON files**: Standard, tooling-friendly. Rejected in favour of TS for type safety without extra codegen.
- **Multiple namespaces (common, auth, schedule, …)**: Better code splitting for large apps. Overkill for this site — a single `translation` namespace is fine.

---

## Decision 3: SSR Hydration Strategy

**Decision**: Server always renders in Spanish (the default). On the client, after hydration, read `localStorage.getItem('kuky-lang')` and call `i18next.changeLanguage()` in a top-level `useEffect`. No language in URL. No SSR language detection.

**Rationale**: Confirmed by user in clarification Q1 (Option B). This avoids routing restructure and keeps the server output stable. The flash of Spanish on first load for non-Spanish users is acceptable given the target audience (Romanian students learning Spanish — Spanish is the natural home language of the content, confirmed in clarification Q2 Option A).

**Alternatives considered**:
- **URL-prefix routing (`/ro/`, `/en/`)**: Cleanest for SEO. Rejected by user — would require rewriting all routes.
- **Cookie read on server**: Would allow SSR to render in user's language. Deferred — can be layered in later without changing the client contract; out of scope for this feature.

---

## Decision 4: Type-Safe Translation Keys

**Decision**: Use `i18next`'s TypeScript augmentation (`i18next.d.ts`) to declare the `resources` type from the `es.ts` dictionary. The `t()` function then autocompletes and catches unknown keys at compile time.

**Rationale**: Zero runtime cost, catches typos at build time, and aligns with "TypeScript strict mode" in the constitution.

**Alternatives considered**:
- **Untyped `t('any-string')`**: Simple but fragile — missing keys silently fall through to key strings in production.
- **Codegen from JSON**: Adds build step complexity. Unnecessary when TS modules are used directly.

---

## Decision 5: Language Selector Component

**Decision**: A `DropdownMenu` (Radix UI, already in the project) in `SiteHeader` showing the active language as `🇪🇸 ES` / `🇷🇴 RO` / `🇬🇧 EN`. On mobile (Sheet nav), the selector renders as three inline buttons.

**Rationale**: `DropdownMenu` from Shadcn UI is already available. No new UI dependency needed. Flag emojis are Unicode — no extra assets required.

**Alternatives considered**:
- **Select component**: Functional but less polished for a 3-item language picker.
- **Three always-visible buttons**: Takes too much nav space on desktop.
