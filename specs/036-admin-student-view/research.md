# Research: Admin Student View Overhaul

**Feature**: `036-admin-student-view` | **Date**: 2026-08-11

## 1. Keep vs remove `progress` on the profile API

**Decision**: Remove `progress` from `StudentProfileResponse` / `StudentProfile`. Derive pending / submitted / completed on the client from `homeworks[].status` using the same three buckets as today (`PENDING` → pending; `SUBMITTED` → submitted; `REVIEWED`|`GRADED` → completed).

**Rationale**: Spec removes Progreso and does not relocate attended count, unit progress, activity breakdown, or level chips. Shipping those aggregates unused violates YAGNI and keeps misleading tests alive. Breakdown for the collapsed Tareas box needs only homework statuses already present on each row.

**Alternatives considered**:
- Keep full `progress` object, hide in UI — dead payload and dead backend queries (units, activities, attended).
- Replace `progress` with top-level `homeworkBreakdown` only — redundant with `homeworks[]`; extra field to keep in sync.
- Soft-deprecate (optional field) — unnecessary complexity for a single admin consumer.

## 2. Where to compute the three-bucket breakdown

**Decision**: Small pure helper in the frontend (e.g. next to the profile page or in `admin.ts`) that counts from `StudentProfileHomework[]`. Mirror existing backend rules from `StudentProfileAdminService.computeHomeworkBreakdown`.

**Rationale**: Single source of list data; no second API round-trip; matches FR-002a without backend breakdown DTO.

**Alternatives considered**:
- Keep server `homeworkBreakdown` only — still a partial `progress` concept.
- Duplicate breakdown in both FE and BE — drift risk.

## 3. Expandable Tareas layout

**Decision**:
- Stats row stays three cards (classes / tareas / presentations).
- Only the **Tareas** card is interactive (button/disclosure): shows total + compact pending/submitted/completed when collapsed.
- When expanded, render the **full homework list as a full-width block immediately below the stats grid**, then the rest of the profile sections (upcoming, past, presentations, etc.).
- Collapse restores stats-only; list unmounts or hides; no URL state required for v1.

**Rationale**: Clarifications Q1–Q2; matches former lower Tareas readability without cramming into the card column.

**Alternatives considered**:
- Inline list inside the Tareas card — rejected (clarification A preferred full-width).
- Accordion replacing the whole stats row — rejected.
- Persist expand in query string — YAGNI.

## 4. Homework row parity with former lower Tareas

**Decision**: Move the existing list markup/behavior (sort pending first, then by `submittedAt` desc; `StatusBadge`; score when `GRADED` + `scorePercent`; needs-review / view-result actions opening `HomeworkReviewDialog` / `ExerciseResultDialog`) into the expanded panel. Do not change grading/review business rules.

**Rationale**: FR-004–FR-006; SC-005; “relocate and consolidate.”

**Alternatives considered**: Simplified rows without open-response actions — would fail SC-005.

## 5. What to delete from Progreso UI / i18n

**Decision**: Remove the Progreso `Section` and its use of units, attended classes, placement level chip (from progress), and activity breakdown. Keep past-classes no-show toggles and the separate placement evaluation section. Retarget breakdown labels to `admin.studentProfile` keys that are not under a “progress product” narrative (or keep `progress.homeworkPending` keys temporarily if cheaper — prefer renaming for clarity). Remove unused Progreso title/empty/units/attended/level strings when nothing references them.

**Rationale**: Clarification Q3 + FR-001 / FR-009.

**Alternatives considered**: Leave dead i18n keys — avoid; clean when unused.

## 6. Backend dependency cleanup

**Decision**: After removing progress computation, drop `UnitRepository` and `ActivitySubmissionRepository` from `StudentProfileAdminService` if no longer referenced. Delete DTOs only used by progress (`StudentProgressDto`, `UnitProgressDto`, `HomeworkBreakdownDto`, `ActivityBreakdownDto`) if unused elsewhere. Update `StudentProfileAdminServiceTest` to assert profile identity + homeworks/bookings behavior still needed; remove progress-only tests (or rewrite to assert `progress` is absent / response has no such field).

**Rationale**: Dead code must not remain (constitution Development Workflow).

**Alternatives considered**: Leave unused injection — fails dead-code rule.
