# Tasks: Multi-Language Support for Static UI Text

**Input**: Design documents from `specs/008-language-support/`

**Prerequisites**: plan.md âœ… | spec.md âœ… | research.md âœ… | data-model.md âœ… | contracts/ âœ… | quickstart.md âœ…

**Tests**: Not requested â€” validation via TypeScript strict mode (compile-time key checks) + quickstart.md manual scenarios.

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking dependencies)
- **[Story]**: User story label (US1, US2, US3) â€” only on user story phase tasks
- File paths relative to repo root

---

## Phase 1: Setup

**Purpose**: Install library and scaffold the i18n directory.

- [X] T001 Install `react-i18next` and `i18next` in `front-end/package.json` via `npm install react-i18next i18next` in `front-end/`
- [X] T002 Create empty placeholder files to establish the directory structure: `front-end/src/i18n/index.ts`, `front-end/src/i18n/i18next.d.ts`, `front-end/src/i18n/locales/es.ts`, `front-end/src/i18n/locales/ro.ts`, `front-end/src/i18n/locales/en.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core i18n infrastructure â€” must be complete before any component replacement begins.

**âš ï¸ CRITICAL**: No user story work can begin until this phase is complete.

- [X]  [P] Implement `front-end/src/i18n/index.ts` â€” call `i18next.use(initReactI18next).init(...)` with `fallbackLng: 'es'`, `lng: 'es'`, `initImmediate: false`, `interpolation: { escapeValue: false }`, and stub resource imports (real locales wired in T009); export `LANGUAGES` constant: `[{ code: 'es', flag: 'ðŸ‡ªðŸ‡¸', label: 'ES' }, { code: 'ro', flag: 'ðŸ‡·ðŸ‡´', label: 'RO' }, { code: 'en', flag: 'ðŸ‡¬ðŸ‡§', label: 'EN' }]`
- [X]  [P] Create `front-end/src/hooks/useLanguage.ts` â€” exports `useLanguage()` returning `{ language, setLanguage }` where `setLanguage` calls `i18next.changeLanguage(lang)` and writes to `localStorage.setItem('kuky-lang', lang)`; `language` reads from `i18next.language`
- [X]  Scan every route and component file and build `front-end/src/i18n/locales/es.ts` â€” the authoritative Spanish translation dictionary covering all key groups: `nav.*`, `common.*`, `home.*`, `about.*`, `account.*`, `schedule.*`, `resources.*`, `learning.*`, `admin.*`; see `contracts/translation-catalogue.md` for the expected key groups and interpolation convention; export as `export const esDict = { ... } as const`
- [X]  Update `front-end/src/i18n/i18next.d.ts` â€” augment `i18next` `CustomTypeOptions` to set `defaultNS: 'translation'` and `resources: { translation: typeof esDict }` imported from `./locales/es`; this makes all `t()` calls type-checked against `es.ts` (depends on T005)
- [X]  [P] Create `front-end/src/i18n/locales/ro.ts` â€” full Romanian translation of all keys from `es.ts`; export as `export const roDict = { ... }` following the same shape; missing keys fall back to Spanish at runtime (depends on T005)
- [X]  [P] Create `front-end/src/i18n/locales/en.ts` â€” full English translation of all keys from `es.ts`; export as `export const enDict = { ... }` (depends on T005)
- [X]  Update `front-end/src/i18n/index.ts` â€” replace stub resource imports with real locale imports (`esDict`, `roDict`, `enDict`) and wire into `i18next.init` resources object: `{ es: { translation: esDict }, ro: { translation: roDict }, en: { translation: enDict } }` (depends on T005, T007, T008)
- [X]  [P] Create `front-end/src/components/LanguageSwitcher.tsx` â€” Shadcn `DropdownMenu` component showing the active language as `ðŸ‡ªðŸ‡¸ ES` / `ðŸ‡·ðŸ‡´ RO` / `ðŸ‡¬ðŸ‡§ EN`; uses `useLanguage()` from `@/hooks/useLanguage`; iterates over `LANGUAGES` from `@/i18n`; active language shown as the trigger; on mobile Sheet, render as three inline buttons instead (depends on T003, T004)
- [X]  Update `front-end/src/routes/__root.tsx` â€” add `import '@/i18n'` at the top to initialise i18next before render; in `RootComponent` add a `useEffect` that reads `localStorage.getItem('kuky-lang')`, validates it against `['es','ro','en']`, and calls `i18next.changeLanguage(stored)` if valid; update `RootShell` to derive `lang` from `i18next.language` and set `<html lang={lang}>` dynamically (depends on T003, T004)
- [X]  Update `front-end/src/components/SiteHeader.tsx` â€” import `<LanguageSwitcher />` and add it to the desktop nav row (between nav links and the right edge) and inside the mobile `Sheet` content below the nav links (depends on T010)

**Checkpoint**: Build compiles, site runs in Spanish, language selector appears in header, switching languages works end-to-end â€” before any component text has been replaced.

---

## Phase 3: User Story 1 â€” Switch Interface Language (Priority: P1) ðŸŽ¯ MVP

**Goal**: All static text across every page updates immediately when the user selects a different language from the header selector.

**Independent Test**: Open any page, select ðŸ‡·ðŸ‡´ RO, confirm navigation labels, page headings, buttons, and descriptive text all render in Romanian without a page reload.

All tasks in this phase depend on Phase 2 completion (specifically T005 for the key definitions).

### Shell & Root Components

- [X]  [P] [US1] Update `front-end/src/components/SiteHeader.tsx` â€” replace all hardcoded nav label strings (`"Inicio"`, `"Sobre mÃ­"`, etc.) and `aria-label="Abrir menÃº"` with `t('nav.*')` calls; import `useTranslation` from `react-i18next`
- [X]  [P] [US1] Update `front-end/src/components/SiteHeader.tsx` `SiteFooter` â€” replace the hardcoded copyright text with `t('common.footer')` (or equivalent key)
- [X]  [P] [US1] Update `front-end/src/routes/__root.tsx` â€” replace hardcoded strings in `NotFoundComponent` ("PÃ¡gina no encontrada", "La pÃ¡gina que buscasâ€¦", "Volver al inicio") and `ErrorComponent` ("Algo saliÃ³ mal", "Intenta de nuevoâ€¦", "Reintentar", "Inicio") with `t('common.notFound.*')` and `t('common.error.*')` calls

### Landing Page

- [X]  [P] [US1] Update `front-end/src/routes/index.tsx` â€” replace all hardcoded strings (hero badge, h1, subtitle, CTA buttons, feature card titles and descriptions, CTA section) with `t('home.*')` calls

### About Page

- [X]  [P] [US1] Update `front-end/src/routes/sobre-mi.tsx` â€” replace all hardcoded static strings with `t('about.*')` calls; teacher-authored bio content (if loaded from API) must remain untranslated

### Account / Auth Pages

- [X]  [P] [US1] Update `front-end/src/components/auth/LoginForm.tsx` â€” replace form labels, button labels, error messages, and link text with `t('account.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/auth/RegistrationForm.tsx` â€” replace all hardcoded strings with `t('account.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/auth/PasswordResetForm.tsx` â€” replace all hardcoded strings with `t('account.*')` calls
- [X]  [P] [US1] Update `front-end/src/routes/cuenta.tsx` â€” replace page-level static strings and tab labels with `t('account.*')` calls

### Schedule / Reservas Pages

- [X]  [P] [US1] Update `front-end/src/components/scheduling/ScheduleView.tsx` â€” replace section headings and empty-state messages with `t('schedule.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/scheduling/SlotGrid.tsx` â€” replace column headers, labels, and empty-state text with `t('schedule.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/scheduling/TimeSlotList.tsx` â€” replace time slot UI labels with `t('schedule.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/scheduling/CalendarPicker.tsx` â€” replace any hardcoded UI labels with `t('schedule.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/scheduling/BookingDialog.tsx` â€” replace dialog title, field labels, button labels, and confirmation messages with `t('schedule.*')` calls; dynamic data (teacher name, slot time) passed as interpolation variables, not translated
- [X]  [P] [US1] Update `front-end/src/components/scheduling/MyBookings.tsx` â€” replace section heading, status labels, and action button labels with `t('schedule.*')` calls; booking data remains untranslated
- [X]  [P] [US1] Update `front-end/src/routes/reservas.tsx` â€” replace page-level static strings with `t('schedule.*')` calls

### Resources / Recursos Pages

- [X]  [P] [US1] Update `front-end/src/components/resources/ResourcesView.tsx` â€” replace section headings and empty-state messages with `t('resources.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/resources/FreeResourcesSection.tsx` â€” replace section heading and labels with `t('resources.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/resources/BundleCard.tsx` and `front-end/src/components/resources/ResourceCard.tsx` â€” replace UI chrome labels (price labels, badge text, button labels) with `t('resources.*')` calls; resource name and description remain untranslated (teacher-entered)
- [X]  [P] [US1] Update `front-end/src/components/resources/ResourceDetailDialog.tsx`, `front-end/src/components/resources/PurchaseDialog.tsx`, and `front-end/src/components/resources/MyPurchases.tsx` â€” replace dialog labels, button text, and section headings with `t('resources.*')` calls; resource content remains untranslated
- [X]  [P] [US1] Update `front-end/src/routes/recursos.tsx` â€” replace page-level static strings with `t('resources.*')` calls

### Learning / Aprendizaje Pages

- [X]  [P] [US1] Update `front-end/src/components/learning/LearningView.tsx` â€” replace tab labels and section headings with `t('learning.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/learning/HomeworkList.tsx` and `front-end/src/components/learning/HomeworkItemCard.tsx` â€” replace section heading, status labels, due-date labels, and button labels with `t('learning.*')` calls; homework titles remain untranslated
- [X]  [P] [US1] Update `front-end/src/components/learning/HomeworkSubmitDialog.tsx` â€” replace dialog labels and button text with `t('learning.*')` calls; submission text area placeholder only if generic (otherwise leave)
- [X]  [P] [US1] Update `front-end/src/components/learning/HomeworkExercisePage.tsx` and `front-end/src/components/learning/ExerciseResult.tsx` â€” replace UI chrome (progress labels, submit button, result headings, score label) with `t('learning.*')` calls; exercise questions and answer options remain untranslated
- [X]  [P] [US1] Update `front-end/src/components/learning/PastClassesList.tsx`, `front-end/src/components/learning/SharedPresentationsList.tsx`, and `front-end/src/components/learning/ClassPresentation.tsx` â€” replace section headings and empty-state messages with `t('learning.*')` calls; class/presentation titles remain untranslated
- [X]  [P] [US1] Update `front-end/src/routes/aprendizaje.tsx` â€” replace page-level static strings with `t('learning.*')` calls

### Admin / Panel Pages

- [X]  [P] [US1] Update `front-end/src/components/admin/AdminPanel.tsx` â€” replace tab labels (Disponibilidad, Reservas, Tareas, Presentaciones, Alumnos) and page heading with `t('admin.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/admin/availability/AvailabilityTab.tsx`, `WeeklyAvailabilityEditor.tsx`, `AvailabilityExceptionList.tsx`, and `BookingConflictNotice.tsx` â€” replace all UI labels, headings, and button text with `t('admin.*')` calls
- [X]  [P] [US1] Update `front-end/src/components/admin/bookings/BookingsTab.tsx` â€” replace headings, column labels, and action buttons with `t('admin.*')` calls; student names and booking data remain untranslated
- [X]  [P] [US1] Update `front-end/src/components/admin/homework/HomeworkTab.tsx`, `HomeworkAdminList.tsx`, and `HomeworkEditorPage.tsx` â€” replace UI chrome labels (headings, buttons, status badges, form field labels) with `t('admin.*')` calls; homework titles and question text authored by the teacher remain untranslated
- [X]  [P] [US1] Update `front-end/src/components/admin/homework/QuestionEditorCard.tsx` and `QuestionListEditor.tsx` â€” replace toolbar labels, type selector labels, and button labels with `t('admin.*')` calls; question text and answer text fields remain untranslated
- [X]  [P] [US1] Update `front-end/src/components/admin/presentations/PresentationsTab.tsx`, `PresentationAdminList.tsx`, and `SharePresentationDialog.tsx` â€” replace headings, button labels, and dialog labels with `t('admin.*')` calls; presentation titles remain untranslated
- [X]  [P] [US1] Update `front-end/src/components/admin/students/StudentsTab.tsx`, `StudentLink.tsx`, and `front-end/src/components/admin/homework/StudentMultiSelect.tsx` â€” replace section headings and UI labels with `t('admin.*')` calls; student names remain untranslated

**Checkpoint**: All pages render in all 3 languages. `npm run lint` passes with zero TypeScript errors (unknown keys caught at compile time).

---

## Phase 4: User Story 2 â€” Language Preference Persisted Across Sessions (Priority: P2)

**Goal**: Returning visitors see their last-selected language without re-selecting.

**Independent Test**: Select ðŸ‡¬ðŸ‡§ EN, close the browser, reopen â€” site loads in English. Check `localStorage.getItem('kuky-lang')` === `'en'` in DevTools.

- [ ] T047 [US2] Review and harden `front-end/src/hooks/useLanguage.ts` â€” confirm `setLanguage` writes to `localStorage` and that `__root.tsx` hydration reads on mount, validates the stored value against `['es','ro','en']`, falls back to `'es'` for any unknown or missing value; add a comment documenting the fallback contract

**Checkpoint**: Preference survives browser close/reopen. Invalid or missing `kuky-lang` value silently defaults to Spanish.

---

## Phase 5: User Story 3 â€” Dynamic Content Stays Unchanged (Priority: P3)

**Goal**: Teacher-authored content (homework text, exercise questions, resource names, class notes) is never passed through the translation system.

**Independent Test**: Switch to ðŸ‡¬ðŸ‡§ EN, open `/aprendizaje`, confirm homework titles remain in Spanish. Open DevTools Network tab and confirm no translation API calls are made.

- [ ] T048 [US3] Audit `front-end/src/components/learning/` and `front-end/src/components/admin/homework/` â€” verify zero instances of homework titles, exercise question text, answer option text, or resource names wrapped in `t()` calls; document any edge cases found and resolved

**Checkpoint**: All three user stories are independently functional.

---

## Final Phase: Polish & Cross-Cutting Concerns

- [ ] T049 [P] Run `npm run lint && npm run format` in `front-end/` â€” fix any TypeScript errors (unknown `t()` keys), lint warnings, or formatting issues
- [ ] T050 Run all 8 validation scenarios from `specs/008-language-support/quickstart.md` â€” fix any missing translation keys in `ro.ts` or `en.ts`, layout overflow issues, or persistence bugs found during testing
- [ ] T051 [P] Update `front-end/src/routes/__root.tsx` SEO meta â€” `seo()` call currently hardcodes `"EspaÃ±ol con Paula â€” Clases de espaÃ±ol para rumanos"`; add translated `title` and `description` keys to the locale files and pass the active language's values to `seo()` so the `<title>` element reflects the current language

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies â€” start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 â€” **BLOCKS all user story work**
  - T003, T004 can run in parallel after T001-T002
  - T005 (es.ts) can start in parallel with T003/T004 but must complete before T006-T009
  - T007, T008 (ro.ts, en.ts) run in parallel after T005
  - T009 depends on T005 + T007 + T008
  - T010 depends on T003 + T004
  - T011 depends on T003 + T004
  - T012 depends on T010
- **Phase 3 (US1)**: All tasks depend on Phase 2 completion; all T013-T046 are parallel with each other (different files)
- **Phase 4 (US2)**: T047 can start after T004 + T011 are done (T047 is a hardening/review task)
- **Phase 5 (US3)**: T048 can start after Phase 3 tasks for learning/admin components are done
- **Polish**: After all user story phases

### User Story Dependencies

- **US1 (P1)**: Depends on Phase 2 â€” no dependency on US2 or US3
- **US2 (P2)**: Depends on Phase 2 (useLanguage + root hydration already built there); T047 is a review/hardening step only
- **US3 (P3)**: Audit task; can run concurrently with Phase 3 implementation

### Within Phase 2

```
T001 â†’ T002 â†’ T003 [P]
                    T004 [P]
                    T005 â”€â”€â†’ T006
                         â””â”€â”€â†’ T007 [P] â”€â†’ T009 â†’ (unblocks Phase 3)
                         â””â”€â”€â†’ T008 [P] â”€â”˜
               T010 (needs T003+T004) â†’ T012
               T011 (needs T003+T004)
```

### Parallel Opportunities

All Phase 3 tasks (T013â€“T046) are parallelizable â€” each touches distinct files.

---

## Parallel Example: Phase 3 (US1)

```
# These can all run simultaneously â€” completely independent files:
T016  front-end/src/routes/index.tsx
T017  front-end/src/routes/sobre-mi.tsx
T018  front-end/src/components/auth/LoginForm.tsx
T022  front-end/src/components/scheduling/ScheduleView.tsx
T029  front-end/src/components/resources/ResourcesView.tsx
T034  front-end/src/components/learning/LearningView.tsx
T040  front-end/src/components/admin/AdminPanel.tsx
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001â€“T002)
2. Complete Phase 2: Foundational (T003â€“T012) â€” CRITICAL
3. **Checkpoint**: Selector visible, language switching works
4. Complete Phase 3: US1 component replacement (T013â€“T046) â€” parallelize aggressively
5. **STOP and VALIDATE**: Run quickstart.md scenarios 1â€“3
6. Deploy/demo â€” all static text is translated

### Incremental Delivery

1. Setup + Foundational â†’ working selector in header
2. Phase 3 â†’ all pages translated (US1 MVP)
3. Phase 4 â†’ persistence verified (US2)
4. Phase 5 â†’ dynamic content audit (US3)
5. Polish â†’ lint clean, full quickstart validation

---

## Notes

- `[P]` tasks = different files, no blocking dependencies â€” safe to run in parallel
- `[Story]` label maps each task to a user story for traceability
- **Critical scope rule** (US3): Never wrap teacher-entered content (homework titles, exercise questions, resource names, answer text, class notes) in `t()` â€” only UI chrome strings
- `es.ts` is the authoritative key source â€” adding a key there makes it TypeScript-enforced across the whole app immediately
- Missing keys in `ro.ts`/`en.ts` silently fall back to Spanish â€” no runtime error, no visible breakage
- Commit after each phase checkpoint to enable easy rollback
