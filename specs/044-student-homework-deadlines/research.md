# Research: Student-Based Homework Deadlines

**Feature**: `044-student-homework-deadlines` | **Date**: 2026-08-16

## 1. Where the date lives

**Decision**: Nullable `homework_targets.due_on DATE`. Drop `homework_assignments.due_on` in the same migration. No copy of old values (spec: unused; no compatibility).

**Rationale**: The target row already is the student–homework assignment (FR-002, unique `(assignment_id, user_id)`). Unassign already deletes the row (FR-006). A second table would duplicate that link.

**Alternatives considered**:
- Keep assignment `due_on` as a default plus per-student overrides — rejected; spec removes the overall concept (FR-001).
- Store on `homework_submissions` — no row until the student writes; pending work would have nowhere to put a date.
- JSON on the assignment — not per student.

## 2. Assign-time date without overwriting existing rows

**Decision**: Extend `PUT /api/v1/admin/homework/{id}/assignees` with optional `dueOn`. `replaceTargets` still deletes removed students and `INSERT ... ON CONFLICT DO NOTHING` for the rest. **Only the INSERT path writes `due_on`.** Existing targets keep their stored date. Same rule on `POST /homework`: `dueOn` applies to initial `assigneeIds` only; if there are no assignees, `dueOn` is ignored (nowhere to store it).

**Rationale**: Clarification B / FR-004. Current `addTargets` already skips existing rows; adding `due_on` to INSERT is the smallest change that implements “new only.”

**Alternatives considered**:
- Replace-set of `{ userId, dueOn }[]` that writes every date on save — would clobber extensions; rejected.
- No assign-time field (per-row only) — rejected by clarification B.

## 3. Per-row change / clear

**Decision**: `PUT /api/v1/admin/homework/{id}/assignees/{userId}/due-on` with body `{ "dueOn": "<ISO date>" | null }`. Updates that target only. `404` if the homework does not exist or that student is not assigned. Does not bump `content_revised_at`.

**Rationale**: FR-003 / FR-014. The assignee replace-set is the wrong write for an extension (it is about who is assigned). A dedicated update avoids sending the full id list to change one date.

**Alternatives considered**:
- PATCH the whole `HomeworkAdminItem` — content PUT must not carry due dates (FR-016).
- Inline date only in `PUT .../assignees` for everyone — conflicts with “do not overwrite existing.”

## 4. Overdue

**Decision**: Keep today’s rule, per student: `dueOn != null && dueOn.isBefore(todayTeacherZone) && status == PENDING`. Compute in `HomeworkItems` for student payloads and in admin mapping for `AssigneeDto` / `StudentProfileHomeworkDto`. Extract a tiny shared helper (same three predicates) so teacher and student cannot drift. Never persist overdue.

**Rationale**: FR-008, FR-009; `HomeworkItems` already implements this against assignment `dueOn`.

**Alternatives considered**:
- Treat the due day as overdue (`!isAfter(today)`) — would change current product meaning; out of scope.
- Block submit after due — forbidden by FR-009.

## 5. Freeze / `content_revised_at`

**Decision**: Remove `dueOn` from `UpdateHomeworkRequest` / content PUT. `contentChanged` already ignores due date (it never compared `dueOn`). New due-date writes (`setAssignees`, per-row due-on, create assignees) must not bump the token. Rewrite the freeze test that today PUTs homework with only `dueOn` changed: assert assignee due-on update does not bump and does not yield `HOMEWORK_UPDATED`.

**Rationale**: FR-011. Due date is operational, like assignees.

**Alternatives considered**:
- Keep a dummy `dueOn` on content PUT for back-compat — spec says no compatibility.

## 6. Student read path

**Decision**: Stop mapping `homework_assignments.due_on` on `HomeworkAssignment`. When loading work for a user, join `homework_targets.due_on` and pass that `LocalDate` into `HomeworkItems.toResponse`. `HomeworkItemResponse.dueOn` / `overdue` stay in the student JSON so `HomeworkItemCard` and `UnitDetailContent` do not change shape.

**Rationale**: FR-007. One homework loaded for two students must not share a date on the assignment entity.

**Alternatives considered**:
- Alias `t.due_on AS due_on` onto `HomeworkAssignment` — hides the ownership; easy to leak the wrong date on admin loads.

## 7. Unit assignment

**Decision**: `UnitService.setAssignees` / homework sync keep calling `addTargets` with no date → `NULL`. Do not add a date field to unit assign. Admin unit `HomeworkAdminItem` mapping drops assignment `dueOn`.

**Rationale**: Clarification A / FR-005a.

## 8. Admin UI split

**Decision**:
- Remove the due-date control from homework content (title/type/level/questions).
- Optional date next to `StudentMultiSelect`: sent as `dueOn` on create and on `setAssignees` (new rows only).
- Each `HomeworkAssigneeList` row: date control + overdue; save via per-row endpoint (not the big content Save).
- `HomeworkAdminCard`: no overall date.
- Student profile Tareas rows: show date + overdue; no editor (link to homework still as today).

**Rationale**: FR-014–016; one editor surface.

**Alternatives considered**:
- Edit dates on the student profile — rejected in clarification.
- Save existing row dates only when clicking the homework Save — slower extensions and mixes content freeze with operational edits.
