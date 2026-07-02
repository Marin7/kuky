# Contract: Timezone Preference & Display

## `GET /api/v1/auth/me` (existing endpoint, extended response)

Response body gains two fields on `UserResponse`:

```jsonc
{
  "id": "...",
  "email": "...",
  "role": "STUDENT",
  "firstName": "...",
  "lastName": "...",
  "username": "...",
  "avatarImageId": "...",
  "status": "ACTIVE",
  "timezone": "Europe/Bucharest",   // new, nullable
  "timezoneIsManual": false         // new
}
```

`timezone: null` means no preference has ever been synced — front-end callers fall back to `Intl.DateTimeFormat().resolvedOptions().timeZone` (browser-detected) for display, and the sync call below should fire.

## `PUT /api/v1/auth/timezone` (new)

Purpose: two callers use this endpoint —
1. **Silent session sync** (fires once per app load if `timezoneIsManual` is `false`): reports the browser-detected zone.
2. **Explicit override** (fires from the account-settings "Time zone" control): sets `manual: true` with the student's chosen zone, or `manual: false` with no zone to clear an override and revert to auto-detection on next sync.

**Request**:

```jsonc
{
  "zone": "Europe/Bucharest",  // required unless manual=false is clearing an override
  "manual": false
}
```

**Behavior**:
- Requires authentication (`@AuthenticationPrincipal`), same pattern as `PUT /api/v1/auth/profile`. Available to any authenticated role (`USER`, `STUDENT`, `ADMIN`) — the preference is account-level, not role-gated.
- If the caller's current `timezone_is_manual = true` and this request has `manual: false` with no explicit clear intent... **not applicable** — the only two request shapes are "auto-sync" (`manual: false`, always allowed to overwrite `timezone`, since auto-sync is meant to run every session) and "set override" (`manual: true`). A dedicated "clear override" is just `manual: false` with the newly-detected zone — the front-end always sends the current detection alongside `manual: false` when clearing, so there is no ambiguous partial state.
- `zone` MUST be a valid IANA zone id (`ZoneId.of(zone)`); invalid values return `400` with the standard `{"error":"INVALID_TIMEZONE","message":"..."}` shape (see `contracts/api.md` conventions).

**Response**: `200 OK` with the updated `UserResponse` (same shape as `GET /me`).

## Booking/reminder email formatting (internal contract, not HTTP)

`BookingEmailService` currently formats every email's time with:

```java
DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC)
```

New behavior — one formatter per recipient category, both zone-labeled:

- **Student-facing** (confirmation, cancellation, reminder-to-student): zone = `student.timezone` if non-null, else `app.scheduling.teacher-timezone` (fallback per FR-008-equivalent for the email context). Rendered as e.g. `12/08/2026 18:00 (Europe/Bucharest)`.
- **Teacher-facing** (reminder-to-teacher, any teacher notification): zone = `app.scheduling.teacher-timezone`, always. Rendered as e.g. `12/08/2026 15:00 (Europe/Madrid)`.

`.ics` attachment generation (`IcsEventFactory`) is unchanged — UTC `Z`-suffixed, no `TZID` — per the Phase 0 research decision.

## Admin front-end: teacher timezone source

No new endpoint. Admin components (`WeeklyAvailabilityEditor.tsx`, `BookingsTab.tsx`, `panel_.alumnos.$studentId.tsx`) read `teacherTimezone` from the existing public `GET /api/v1/schedule` response (`ScheduleResponse.teacherTimezone`, already sourced from `app.scheduling.teacher-timezone`) instead of a hardcoded `"Europe/Madrid"` literal or an unqualified (browser-local) `Intl.DateTimeFormat` call.
