# Contract: Translation Catalogue

This document defines the shape of the translation catalogue that all three language files must conform to. It is the authoritative interface between translation content and the application.

## Catalogue Shape

Each locale file (`es.ts`, `ro.ts`, `en.ts`) exports a `const` object of type `TranslationDict`. The Spanish file is the authoritative source — all keys defined in `es.ts` represent the full key set. Romanian and English files may omit keys; missing keys fall back to Spanish at runtime.

## Key Groups

### `nav` — Navigation

| Key | Spanish value (example) | Notes |
|-----|------------------------|-------|
| `nav.home` | Inicio | |
| `nav.about` | Sobre mí | |
| `nav.schedule` | Reservas | |
| `nav.learning` | Mi aprendizaje | Authenticated users only |
| `nav.panel` | Panel | Admin only |
| `nav.account` | Mi cuenta | |

### `home` — Landing Page

| Key | Notes |
|-----|-------|
| `home.hero.badge` | "Clases para estudiantes rumanos" |
| `home.hero.title` | Main h1 (may contain `<em>` via HTML interpolation) |
| `home.hero.subtitle` | Paragraph below title |
| `home.hero.cta_book` | Primary CTA button |
| `home.hero.cta_about` | Secondary CTA button |
| `home.features.personalized.title` | Feature card title |
| `home.features.personalized.desc` | Feature card description |
| `home.features.romanian.title` | |
| `home.features.romanian.desc` | |
| `home.features.materials.title` | |
| `home.features.materials.desc` | |
| `home.cta.title` | CTA section heading |
| `home.cta.subtitle` | CTA section subtext |
| `home.cta.button` | CTA button label |

### `common` — Shared UI

| Key | Notes |
|-----|-------|
| `common.save` | |
| `common.cancel` | |
| `common.close` | |
| `common.confirm` | |
| `common.loading` | |
| `common.error` | Generic error label |
| `common.success` | |
| `common.back` | |
| `common.next` | |
| `common.submit` | |
| `common.edit` | |
| `common.delete` | |
| `common.notFound.title` | 404 page heading |
| `common.notFound.body` | |
| `common.notFound.back` | |
| `common.error.title` | Error boundary heading |
| `common.error.retry` | |
| `common.error.home` | |

### `account` — Authentication

Keys covering registration form, login form, forgot/reset password forms, field labels, validation messages, and button labels.

### `schedule` — Reservas Page

Keys covering the time slot grid labels, booking dialog fields, confirmation messages, "My bookings" section headings and status labels.

### `resources` — Recursos Page

Keys covering section headings, resource card labels, purchase dialog text, purchase history headings.

### `learning` — Aprendizaje Page

Keys covering homework list headings, submission dialog, exercise page UI (question labels, submit button, result screen). Does **not** include homework titles, exercise questions, or teacher-entered content — those are dynamic data.

### `admin` — Panel Page

Keys covering tab labels (Availability, Bookings, Homework, Presentations, Students), form labels, and action buttons in the admin panel. Does **not** include homework content being authored.

## Interpolation Convention

Keys that require runtime values use `{{variable}}` syntax:

```
"schedule.bookingConfirmed": "Clase reservada para el {{date}}"
```

The `t()` call passes the variable: `t('schedule.bookingConfirmed', { date: formattedDate })`.

## Adding a New Key

1. Add the key + Spanish value to `es.ts` (the authoritative file).
2. Add translations to `ro.ts` and `en.ts`.
3. TypeScript will error on any `t('unknown.key')` call because of the `i18next.d.ts` augmentation.

## Adding a New Language

1. Create `front-end/src/i18n/locales/xx.ts` following the `TranslationDict` shape.
2. Add the language to the `LANGUAGES` constant in `front-end/src/i18n/index.ts`.
3. Add the flag + code entry to `LanguageSwitcher.tsx`.
