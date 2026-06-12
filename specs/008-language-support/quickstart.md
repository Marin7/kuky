# Quickstart: Validating Multi-Language Support

## Prerequisites

- Node.js 22+ installed
- Front-end dependencies installed: `cd front-end && npm install`
- Back-end running (only needed for authenticated pages): `./gradlew bootRun --args='--spring.profiles.active=local'`

## Running the Front-End

```bash
cd front-end
npm run dev
# → http://localhost:8080
```

## Validation Scenarios

### 1. Language selector visible on every page

- Open http://localhost:8080
- Confirm the language selector (flag + code, e.g. `🇪🇸 ES`) appears in the top navigation bar on all pages: `/`, `/sobre-mi`, `/reservas`, `/recursos`, `/cuenta`.
- On a screen narrower than 768px, confirm the selector also appears inside the mobile Sheet nav.

### 2. Language switching — all static text updates

- Start on http://localhost:8080 (Spanish default).
- Click the language selector and choose `🇷🇴 RO`.
- Confirm: navigation links, hero heading, feature card text, CTA button all render in Romanian.
- Click `🇬🇧 EN` — confirm the same elements render in English.
- Navigate to `/sobre-mi` — confirm static page text is in the selected language.
- No full page reload should occur during any switch.

### 3. Language persists across navigation

- Select `🇬🇧 EN` on the landing page.
- Click the "Reservas" nav link.
- Confirm the schedule page renders in English.
- Confirm the active language in the selector still shows `🇬🇧 EN`.

### 4. Language persists across sessions

- Select `🇷🇴 RO`.
- Close the browser tab.
- Open http://localhost:8080 in a new tab.
- Confirm the interface loads in Romanian without any manual selection.
- Confirm `localStorage.getItem('kuky-lang')` returns `'ro'` (check via browser DevTools → Application → Local Storage).

### 5. Default to Spanish for new visitors

- Open browser DevTools → Application → Local Storage → delete the `kuky-lang` key.
- Reload http://localhost:8080.
- Confirm the interface loads in Spanish.

### 6. Dynamic content is not translated

- Log in as a student (requires back-end running).
- Switch to `🇬🇧 EN`.
- Navigate to `/aprendizaje`.
- Confirm: page headings ("My Learning", "Homework", etc.) are in English.
- Confirm: any homework titles or exercise questions the teacher created remain in the language the teacher wrote them (Spanish).

### 7. Missing translation key fallback

- Temporarily remove a key from `ro.ts` (e.g., delete `nav.home`).
- Switch to `🇷🇴 RO`.
- Confirm that the corresponding label falls back to the Spanish text, not an empty string or raw key.
- Restore the key after testing.

### 8. No layout breakage

- Switch to each language in turn and visually inspect:
  - Navigation bar: no text overflow, items remain on one line on desktop.
  - Hero section: heading does not break the two-column layout.
  - Feature cards: text wraps cleanly within cards.
  - CTA section: button labels fit within buttons.
- Resize to mobile (375px width) and repeat for each language.

## Type-Safety Check

```bash
cd front-end
npm run lint    # tsc strict + eslint — should pass with zero errors
```

A typo in a `t('key')` call will produce a TypeScript compile error, not a silent runtime fallback.
