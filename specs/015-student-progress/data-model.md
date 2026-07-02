# Phase 1 Data Model: Student Progress on Teacher Profile View

New migration: `back-end/src/main/resources/db/migration/V26__add_booking_no_show.sql`.

```sql
-- A past confirmed class the student didn't attend. Defaults to attended (FALSE)
-- per the clarified behavior; the teacher flags a specific class as a no-show.
ALTER TABLE bookings ADD COLUMN no_show BOOLEAN NOT NULL DEFAULT FALSE;
```

No other schema changes. Curriculum completion, homework breakdown, and CEFR level are all computed from existing tables (`units`, `unit_assignments`, `homework_assignments`, `homework_targets`, `homework_submissions`, `placement_*`) — see [research.md](research.md) for why each is derived rather than stored.

---

## Entity: `Booking` (extended)

| Column | Type | Notes |
|--------|------|-------|
| `no_show` | BOOLEAN, NOT NULL, DEFAULT FALSE | **New.** Set by the teacher via the no-show endpoint. Only meaningful for `status = 'CONFIRMED'` bookings in the past; irrelevant (and unreachable through the API) for future or cancelled bookings (FR-012). |

**Java model** (`back-end/src/main/java/com/kuky/backend/scheduling/model/Booking.java`): add `private boolean noShow;` + getter/setter, mapped from the `no_show` column in `BookingRepository.BOOKING_MAPPER`.

**Repository method** (`BookingRepository`, new):

| Method | Query shape | Purpose |
|--------|-------------|---------|
| `setNoShow(id, noShow)` | `UPDATE bookings SET no_show = :noShow WHERE id = :id` | Unconditional update; eligibility (`CONFIRMED` + past) is validated in `BookingService.setNoShow` before calling this, mirroring how `markCancelled`'s `STATE` check happens in `cancelBookingAsAdmin` rather than in SQL. |

**Service method** (`BookingService`, new): `setNoShow(UUID bookingId, boolean noShow)` — loads the booking, throws `BookingNotFoundException` if missing, throws `BookingNotAllowedException(Reason.NOT_ELIGIBLE_FOR_NO_SHOW)` unless `status == "CONFIRMED" && slotStart.isBefore(Instant.now())`, otherwise calls `bookingRepository.setNoShow`.

**Exception** (`BookingNotAllowedException`, extended): add `NOT_ELIGIBLE_FOR_NO_SHOW` to the `Reason` enum; `GlobalExceptionHandler` maps it to `422 UNPROCESSABLE_ENTITY` / `BOOKING_NOT_ELIGIBLE_FOR_NO_SHOW`.

---

## Derived: `UnitProgress` (per assigned unit, not persisted)

| Field | Type | Notes |
|-------|------|-------|
| `unitId` | UUID | |
| `subject` | String | |
| `level` | String (CEFR) | |
| `totalHomeworks` | int | Count of the unit's homeworks that are actually assigned to this student (via `homework_targets`, not just filed under the unit — see [research.md](research.md) §2). |
| `completedHomeworks` | int | Of those, how many have reached `REVIEWED` or `GRADED`. |
| `complete` | boolean | `totalHomeworks > 0 && completedHomeworks == totalHomeworks`. A unit with zero targeted homeworks is `false` (no completion criteria yet, per Edge Cases), not `true`. |

**Repository method** (`UnitRepository`, new): `findProgressForStudent(UUID studentId)` — returns `List<UnitProgressView>` (a `record(UUID unitId, String subject, String level, int totalHomeworks, int completedHomeworks)`), one row per unit assigned to the student, via the query in [research.md](research.md) §2. `complete` is computed in the DTO mapping step, not in SQL.

---

## Derived: `HomeworkBreakdown` (not persisted)

| Field | Type | Notes |
|-------|------|-------|
| `pending` | int | Count of the student's assigned homeworks with status `PENDING`. |
| `submitted` | int | Count with status `SUBMITTED`. |
| `completed` | int | Count with status `REVIEWED` or `GRADED`, merged. |

Computed in `StudentProfileAdminService` by grouping the existing `HomeworkTargetRepository.findAssignmentsForStudent(studentId)` results — no new query (see [research.md](research.md) §1).

---

## Derived: `StudentProgressDto` (assembled response, not persisted)

```java
public record StudentProgressDto(
    List<UnitProgressDto> units,
    HomeworkBreakdownDto homeworkBreakdown,
    int attendedClasses
) {}

public record UnitProgressDto(
    UUID unitId, String subject, String level,
    int totalHomeworks, int completedHomeworks, boolean complete
) {}

public record HomeworkBreakdownDto(int pending, int submitted, int completed) {}
```

`attendedClasses` = count of the student's bookings where `status == "CONFIRMED" && slotStart.isBefore(now) && !noShow` — computed in `StudentProfileAdminService` from the bookings list already fetched via `bookingRepository.findByUserId(studentId)`; no new query.

CEFR level is **not** part of `StudentProgressDto` — it continues to be served by the existing `GET .../placement-evaluation` endpoint the profile page already calls (`getStudentPlacementEvaluation`); the frontend renders it alongside the new progress section without a backend change (FR-005).

## `StudentProfileResponse` (extended)

Add one field: `progress: StudentProgressDto`, populated by `StudentProfileAdminService.getProfile` alongside the existing `bookings`/`homeworks`/`presentations` fields.

## `StudentProfileBookingDto` (extended)

Add one field: `noShow: boolean`, mapped straight from `Booking.isNoShow()`.

## No changes to

- `units`, `unit_assignments`, `homework_assignments`, `homework_targets`, `homework_submissions`, `placement_*` tables — all read-only for this feature.
- Homework status enum (`PENDING`/`SUBMITTED`/`REVIEWED`/`GRADED`) — unchanged; only the *display grouping* changes (client/service-side, not the stored value).
