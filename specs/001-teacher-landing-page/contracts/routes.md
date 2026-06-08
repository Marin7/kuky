# Route Contracts: Spanish Teacher Presentation Site

**Phase**: 1 — Design
**Date**: 2026-06-08

These contracts define the public surface of each page: its URL, SEO metadata,
expected visual sections, and navigation behaviour. They are the acceptance
criteria for the UI.

---

## Route: `/` — Home (Landing Page)

**File**: `front-end/src/routes/index.tsx`
**Status**: Implemented

### SEO

| Key | Value |
|-----|-------|
| `<title>` | `Español con Paula — Clases de español para rumanos` |
| `<meta description>` | `Clases de español personalizadas, 100% online, para estudiantes rumanos de todos los niveles.` |

### Required sections (in order)

| Section | Content | CTA |
|---------|---------|-----|
| Hero | Paula's photo, headline, audience tag, description | "Reservar una clase" → `/reservas`; "Conóceme" → `/sobre-mi` |
| Features | Three `ServiceDifferentiator` cards | None |
| CTA banner | Invitation to book | "Reservar ahora" → `/reservas` |

### Validation

- Hero headline visible without scrolling on 375 px mobile.
- Both hero CTAs navigate to the correct routes.
- All three feature cards render with title and description.

---

## Route: `/sobre-mi` — About Me

**File**: `front-end/src/routes/sobre-mi.tsx`
**Status**: Implemented

### SEO

| Key | Value |
|-----|-------|
| `<title>` | `Sobre mí — Español con Paula` |
| `<meta description>` | `Conoce a Paula, profesora de español dedicada a ayudar a estudiantes rumanos a dominar el idioma.` |

### Required sections (in order)

| Section | Content |
|---------|---------|
| Teacher photo | `teacher.jpg`, aspect-ratio 4:5 |
| Identity | "Sobre mí" eyebrow, `h1` with Paula's name |
| Bio | 3 paragraphs |
| Stat grid | 4 `TeacherStat` items (Niveles, Modalidad, Idiomas, Enfoque) |

### Validation

- Photo loads and maintains 4:5 ratio on all screen sizes.
- All 4 stat items display label and value correctly.

---

## Route: `/clases` — Classes (Placeholder)

**File**: `front-end/src/routes/clases.tsx`
**Status**: Placeholder

### SEO

| Key | Value |
|-----|-------|
| `<title>` | `Clases y materiales — Español con Paula` |
| `<meta description>` | `Explora las clases disponibles y accede a materiales de estudio.` |

### Required content

- Eyebrow: "Clases"
- Heading: "Clases y materiales"
- Description: what will be available in this section

### Validation

- Page renders without errors.
- Eyebrow, heading, and description are all visible.

---

## Route: `/reservas` — Bookings (Placeholder)

**File**: `front-end/src/routes/reservas.tsx`
**Status**: Placeholder

### SEO

| Key | Value |
|-----|-------|
| `<title>` | `Reservas — Español con Paula` |
| `<meta description>` | `Reserva tu clase de español y consulta tus citas.` |

### Required content

- Eyebrow: "Reservas"
- Heading: "Resumen de reservas"
- Description: booking and calendar purpose

### Validation

- Page renders without errors.
- Eyebrow, heading, and description are all visible.

---

## Route: `/cuenta` — My Account (Placeholder)

**File**: `front-end/src/routes/cuenta.tsx`
**Status**: Placeholder

### SEO

| Key | Value |
|-----|-------|
| `<title>` | `Mi cuenta — Español con Paula` |
| `<meta description>` | `Gestiona tu cuenta y tu perfil de estudiante.` |

### Required content

- Eyebrow: "Mi cuenta"
- Heading: "Gestión de cuenta"
- Description: account management purpose

### Validation

- Page renders without errors.
- Eyebrow, heading, and description are all visible.

---

## Global contracts

### Navigation header (SiteHeader)

- Sticky, renders on every page via root layout.
- Logo text: "Español con Paula", links to `/`.
- Five nav links in order: Inicio, Sobre mí, Clases, Reservas, Mi cuenta.
- Active route link is visually distinguished.

### Footer (SiteFooter)

- Renders on every page via root layout.
- Displays copyright with current year and site identity.
