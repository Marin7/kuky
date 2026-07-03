# Phase 1 Data Model: Shared Bookings

Note on naming: the database column is `second_student_id` (named for when it was added, since it's attached after the booking student's own row already exists), but the application layer and this document call that student the **companion student** — both students have equal standing on the class; "second" is not a rank.

## Entity: `Booking` (`bookings` table)

Existing fields (unchanged):

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | PK |
| `user_id` | UUID | FK → `users.id`, `ON DELETE CASCADE`. The **booking student**. |
| `slot_start` | timestamptz | |
| `duration_minutes` | int | 60 or 90 |
| `slot_end` | timestamptz | stored, computed at insert (`slot_start + duration_minutes`) |
| `status` | varchar | `CONFIRMED` \| `CANCELLED` |
| `zoom_meeting_id` / `zoom_join_url` | varchar / text | shared by both students — one Zoom meeting per class regardless of student count |
| `created_at` / `cancelled_at` | timestamptz | |
| `reminder_sent_at` | timestamptz | |
| `no_show` | boolean | now understood as **the booking student's** no-show flag |

New fields (this feature):

| Field | Type | Notes |
|---|---|---|
| `second_student_id` | UUID, nullable | FK → `users.id`, `ON DELETE SET NULL`. Present only when the teacher has attached a companion student (FR-002). |
| `second_student_no_show` | boolean, nullable | The companion student's independent attendance flag (FR-010). Meaningless while `second_student_id IS NULL`. |

**New constraint**: `bookings_second_student_distinct CHECK (second_student_id IS NULL OR second_student_id <> user_id)` — enforces FR-004's "can't attach the booking student as their own companion student" at the database level.

**New index**: `bookings_second_student_id_idx ON bookings (second_student_id) WHERE second_student_id IS NOT NULL` — supports "find my bookings where I'm the companion student" (FR-008).

### Validation rules (service layer, beyond the DB constraint)

- **Attach** (`BookingService.attachCompanionStudent`):
  - Booking must be `CONFIRMED` and `slot_start` in the future (FR-005).
  - Booking must not already have a `second_student_id` set (FR-003 — cap of two).
  - Target user must hold the `STUDENT` role (Assumption: companion student must already be a registered student).
  - Target user must not equal `user_id` (enforced twice: service-layer check for a clean error message, DB constraint as the backstop).
  - If `duration_minutes` is the extended length, target user must have `extended_class_eligible = true` (FR-014).
- **Detach** (`BookingService.detachCompanionStudent`): booking must currently have a `second_student_id` set; clears both `second_student_id` and `second_student_no_show`. Does not touch `user_id`, `status`, or any other field (FR-012).
- **Cancel** (`BookingService.cancelBooking`): caller must match `user_id` **or** `second_student_id` (FR-013). Cancelling clears the whole booking (`status = CANCELLED`) exactly as today — no per-student partial cancellation exists.
- **No-show** (`BookingService.setNoShow`): now takes which student's flag to set (`BOOKING_STUDENT` default, or `COMPANION` — only valid when `second_student_id` is set) (FR-010).

### State transitions

```
[No companion student] --attach (teacher)--> [Companion student attached]
[Companion student attached] --detach (teacher)--> [No companion student]
[Companion student attached] --cancel (either student or teacher)--> [CANCELLED] (both students lose the class)
[No companion student] --cancel (booking student or teacher)--> [CANCELLED] (unchanged from today)
```

A booking can never go from `CANCELLED` back to having a companion student attached (attach requires `CONFIRMED`, FR-005).

## Entity: `User` (`users` table) — unchanged

No new columns. Referenced by `second_student_id`; the existing `role` and `extended_class_eligible` columns are read (not written) during attach validation.
