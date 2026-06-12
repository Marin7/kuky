# Feature Specification: Multi-Language Support for Static UI Text

**Feature Branch**: `008-language-support`

**Created**: 2026-06-12

**Status**: Draft

**Input**: User description: "We need to add language support. The website is in Spanish only now, but I want to add Romanian and English as options. This is only for the static text, not for homeworks and stuff like that"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Switch Interface Language (Priority: P1)

A visitor or student arrives at the Kuky website and wants to read the interface in their preferred language. They see a language selector (e.g., a flag or language name in the navigation bar) and switch from Spanish to Romanian or English. All static labels, headings, navigation items, buttons, and descriptive text on the current page immediately update to the selected language — without a full page reload.

**Why this priority**: This is the core deliverable. All other stories depend on language switching being in place.

**Independent Test**: Can be fully tested by opening any page, selecting a different language, and confirming all visible static text updates to the chosen language.

**Acceptance Scenarios**:

1. **Given** the site is displayed in Spanish, **When** the user selects "Română" from the language selector, **Then** all static text on the current page is displayed in Romanian.
2. **Given** the site is displayed in Spanish, **When** the user selects "English" from the language selector, **Then** all static text on the current page is displayed in English.
3. **Given** the user has selected Romanian, **When** they navigate to a different page, **Then** the language remains Romanian.

---

### User Story 2 - Language Preference Persisted Across Sessions (Priority: P2)

A returning student reopens the website in a new browser session and finds the interface in the same language they selected on their previous visit, without needing to switch again.

**Why this priority**: Eliminates friction for repeat visitors; the site should remember their preference.

**Independent Test**: Select a non-default language, close the browser, reopen the site, and confirm the previously selected language is still active.

**Acceptance Scenarios**:

1. **Given** a user previously selected "English", **When** they return to the site in a new session, **Then** the interface loads in English.
2. **Given** no prior preference is stored, **When** a new visitor opens the site, **Then** the interface defaults to Spanish.

---

### User Story 3 - Dynamic/User-Generated Content Stays Unchanged (Priority: P3)

A student uses the website after language support is added and notices that homework titles, exercise text, class notes, resource names, and any other content entered by the teacher remain in their original language — only the surrounding UI chrome (labels, navigation, buttons, headings, and static descriptions) changes.

**Why this priority**: Guards against scope creep and ensures the translation effort is bounded and manageable.

**Independent Test**: Switch language and confirm that a homework title, exercise question, or resource name is not translated — only surrounding UI elements change.

**Acceptance Scenarios**:

1. **Given** the site is in English, **When** a student views their homework list, **Then** the homework title and exercise questions remain in Spanish (as entered by the teacher), while the surrounding page labels ("Homework", "Submit", "Due date") appear in English.
2. **Given** the site is in Romanian, **When** a student views the schedule page, **Then** booking button labels appear in Romanian but the teacher's availability notes (if any) remain unchanged.

---

### Edge Cases

- What happens when a translation key is missing for a given language? The Spanish (default) text should be shown as a fallback.
- How does the language selector behave on mobile viewports (small screens)?
- What happens if a user's stored language preference refers to a language that no longer exists in the system?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST support three languages for static UI text: Spanish (default), Romanian, and English.
- **FR-002**: The system MUST provide a language selector in the navigation bar (header), visible and accessible from every page. Each option MUST display a flag icon paired with a short language code (e.g., 🇪🇸 ES / 🇷🇴 RO / 🇬🇧 EN).
- **FR-003**: Switching language MUST update all static UI text on the current page without requiring a full page reload.
- **FR-004**: The selected language MUST persist across page navigations within the same session.
- **FR-005**: The selected language preference MUST be saved in browser localStorage (and optionally mirrored to a cookie for server-side reads) so that returning visitors see their preferred language on their next visit. No URL path prefix is used; routing remains unchanged.
- **FR-006**: If no preference is stored, the system MUST default to Spanish.
- **FR-007**: Dynamic/user-generated content (homework text, exercise questions, resource names, class notes) MUST NOT be translated — only static UI text is in scope.
- **FR-008**: If a translation key is missing for the selected language, the system MUST fall back to the Spanish text for that key.
- **FR-009**: All pages and routes MUST have their static text covered by the translation system: landing page, bio page, account page, schedule page, resources page, learning page, and admin panel.

### Key Entities

- **Translation Key**: A string identifier that maps to a human-readable phrase in each supported language.
- **Language Catalogue**: The full set of translation keys and their corresponding text for a given language.
- **Language Preference**: The visitor's chosen language, persisted between sessions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can switch between all three languages in under 2 seconds, with all static text updating without a page reload.
- **SC-002**: 100% of static UI text strings across all pages are covered by the translation system — no untranslated hardcoded strings remain in any language.
- **SC-003**: A returning user's language preference is restored on their next visit with no manual action required.
- **SC-004**: Zero instances of user-generated or teacher-entered content are altered or translated by the language system.
- **SC-005**: Switching language does not cause any visible layout breakage or text overflow across all pages.

## Clarifications

### Session 2026-06-12

- Q: Should language be reflected in the URL path (e.g., `/ro/reservas`) or kept as client-side state with no URL change? → A: Client-side state only (localStorage + optional cookie); no URL prefix; server always renders default language; client switches on hydration.
- Q: For new visitors with no stored preference, should the site auto-detect the browser language or always default to Spanish? → A: Always default to Spanish unconditionally; no browser language detection.
- Q: How should each language option be visually presented in the selector? → A: Flag icon + short code combined (e.g., 🇪🇸 ES / 🇷🇴 RO / 🇬🇧 EN).
- Q: Where in the page layout should the language selector live? → A: Navigation bar / header (top of every page).

## Assumptions

- Mobile layout support for the language selector is in scope and follows the same responsive patterns already used in navigation.
- Browser-based local storage is an acceptable persistence mechanism for language preference (no backend storage needed for this preference).
- The three initial languages — Spanish, Romanian, English — are the complete set for this feature; adding more languages later is a separate concern.
- Translation strings will be maintained manually by the development team; no external translation service or CMS is required.
- The admin panel's static labels are also in scope; translated homework authoring or homework content is explicitly out of scope.
- Right-to-left (RTL) language support is not required.
