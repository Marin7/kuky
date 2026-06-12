# Data Model: Multi-Language Support

This feature introduces no new database entities — all data lives client-side.

## Language Preference (client-side state)

| Attribute | Type | Description |
|-----------|------|-------------|
| `kuky-lang` | `'es' \| 'ro' \| 'en'` | Key in `localStorage`. Persists the user's selected language across sessions. Defaults to `'es'` when absent. |

**Lifecycle**:
1. On app load: read `localStorage.getItem('kuky-lang')`. If present and valid, call `i18next.changeLanguage(value)`. Otherwise keep default `'es'`.
2. On user switch: call `i18next.changeLanguage(lang)`, then `localStorage.setItem('kuky-lang', lang)`.
3. On invalid/missing stored value: treat as absent; fall back to `'es'`.

---

## Language Catalogue (static, front-end code)

Three TypeScript modules, each exporting a single object conforming to the `TranslationDict` shape:

```
front-end/src/i18n/locales/
  es.ts   ← Spanish (default, authoritative key set)
  ro.ts   ← Romanian
  en.ts   ← English
```

**Translation key structure** (flat namespace `translation`):

```
nav.*          Navigation labels (Inicio, Sobre mí, Reservas, …)
home.*         Landing page: hero, features, CTA
about.*        Sobre-mí page
account.*      Cuenta page: login, register, password reset forms
schedule.*     Reservas page: schedule view, booking dialog, my bookings
resources.*    Recursos page: resource cards, purchase dialog
learning.*     Aprendizaje page: homework list, exercise page, classes
admin.*        Panel page: availability editor, homework authoring, presentations
common.*       Shared: buttons (Guardar, Cancelar, …), status labels, error messages
```

**Fallback rule**: if a key is missing in `ro.ts` or `en.ts`, `i18next` automatically falls back to `es.ts`. No runtime error.

---

## i18next Configuration Entity

```
front-end/src/i18n/index.ts
```

| Setting | Value |
|---------|-------|
| `defaultNS` | `'translation'` |
| `fallbackLng` | `'es'` |
| `lng` | `'es'` (overridden client-side after hydration) |
| `interpolation.escapeValue` | `false` (React already escapes) |
| `resources` | `{ es: { translation: esDict }, ro: { translation: roDict }, en: { translation: enDict } }` |

---

## TypeScript Augmentation

File `front-end/src/i18n/i18next.d.ts` augments the `i18next` module so the `t()` function is constrained to keys that exist in `es.ts`:

```typescript
// i18next.d.ts (shape only — not implementation)
import type { esDict } from './locales/es'

declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'translation'
    resources: { translation: typeof esDict }
  }
}
```

This catches unknown translation keys at compile time with zero runtime cost.
