# Data Model: Spanish Teacher Presentation Site

**Phase**: 1 — Design
**Date**: 2026-06-08

> All data in this phase is **static** (hardcoded in source). No database or
> API is involved. This model documents the shape of the data so it can be
> migrated to a real data store in a future phase without restructuring the
> components.

---

## Entity: TeacherProfile

Represents the teacher's public-facing identity and offering.

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `name` | string | Full display name | `"Paula"` |
| `photoUrl` | string | Resolved URL of the teacher's photo | CDN URL from `teacher.jpg` |
| `tagline` | string | Short hero headline suffix | `"con confianza"` |
| `targetAudience` | string | Who the service is for | `"estudiantes rumanos"` |
| `bio` | string[] | Paragraphs of biographical text | Array of 3 strings |
| `stats` | TeacherStat[] | Key fact panels (levels, modality, etc.) | See below |

### TeacherStat

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `label` | string | Stat category label | `"Niveles"` |
| `value` | string | Stat value | `"A2 – C2"` |

**Current stats**: Niveles (A2–C2), Modalidad (100% online), Idiomas (Español
y rumano), Enfoque (Conversación + gramática)

---

## Entity: ServiceDifferentiator

Represents a value proposition card displayed on the homepage.

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `title` | string | Card heading | `"Método personalizado"` |
| `description` | string | Card body text | `"Cada clase se adapta…"` |

**Current differentiators** (ordered):
1. Método personalizado
2. Pensado para rumanos
3. Materiales incluidos

---

## Entity: NavigationLink

Represents a top-level navigation entry.

| Field | Type | Description | Status |
|-------|------|-------------|--------|
| `path` | string | Route path | — |
| `label` | string | Display label | — |
| `isPlaceholder` | boolean | True if the page is not yet functional | — |

**Current links**:

| path | label | isPlaceholder |
|------|-------|---------------|
| `/` | Inicio | false |
| `/sobre-mi` | Sobre mí | false |
| `/clases` | Clases | true |
| `/reservas` | Reservas | true |
| `/cuenta` | Mi cuenta | true |

---

## Future evolution notes

- `TeacherProfile` will be fetched from a backend API when account management
  is added. The component interface stays the same; the data source changes.
- `ServiceDifferentiator` list may become CMS-managed in a future phase.
- `NavigationLink.isPlaceholder` will be removed once all pages are functional.
