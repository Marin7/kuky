# Data Model: Admin Student View Overhaul

**Feature**: `036-admin-student-view` | **Date**: 2026-08-11

No database schema changes. This document describes the **admin student profile** view model after the overhaul.

## Entities (API / UI)

### Student profile (admin)

Teacher-facing aggregate for one user with student activity.

| Field | Notes |
|-------|--------|
| Identity | `id`, `email`, `firstName`, `lastName`, `username`, `avatarImageId`, `createdAt` |
| Interests | `interests[]`, `interestsNote` |
| `bookings[]` | Unchanged — used for upcoming/past + no-show |
| `homeworks[]` | Unchanged row shape — **single source** for Tareas breakdown + expanded list |
| `presentations[]` | Unchanged |
| ~~`progress`~~ | **Removed** |

### Homework assignment (on profile)

| Field | Notes |
|-------|--------|
| `id` | Assignment id |
| `title` | Display title |
| `status` | Includes at least `PENDING`, `SUBMITTED`, `REVIEWED`, `GRADED` (and any others already returned) |
| `submittedAt` | Optional |
| `needsReview` | Drives review CTA |
| `submissionId` | Required for open review/result |
| `scorePercent` | Shown when graded and non-null |
| `hasTeacherFeedback` | Badge when present |

### Homework status breakdown (client-derived, not a persisted entity)

| Bucket | Rule |
|--------|------|
| `pending` | Status not in `{SUBMITTED}` and not in completed set |
| `submitted` | Status `SUBMITTED` |
| `completed` | Status `REVIEWED` or `GRADED` |

`total` = `homeworks.length` (= pending + submitted + completed).

## Removed concepts (UI + API)

| Concept | Disposition |
|---------|-------------|
| Unit progress rows | Removed from profile response and UI |
| Activity submission breakdown | Removed from profile response and UI |
| Attended-classes count | Removed from profile response and UI (no-show remains on past bookings) |
| Placement level chip inside Progreso | Removed (placement evaluation section unchanged) |

## Validation / UI rules

- Tareas control always visible; zeros allowed.
- Expanded empty list → empty-state copy (no error).
- Score only when status is graded and `scorePercent` is present.
- Open-response actions only when `submissionId` (and existing needsReview / GRADED gates) allow it — same as former lower list.

## Relationships

```text
StudentProfile
├── bookings[]          → upcoming / past sections
├── homeworks[]         → collapsed breakdown + expanded Tareas list
└── presentations[]     → presentations section
```
