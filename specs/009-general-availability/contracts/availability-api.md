# Contract: Admin Availability API (updated)

Base path `/api/v1/admin/availability`. Admin-only (gated by `/api/v1/admin/**`). Times `"HH:mm"` (teacher local), dates `"YYYY-MM-DD"`. Error shape `{"error":"CODE","message":"..."}`.

## GET `/api/v1/admin/availability`

Returns the general template and the materialized per-day availability for the current horizon. Calls `ensureWeeksMaterialized` first.

**200 Response** (shape changed — `exceptions` removed, `days` added):
```json
{
  "weekly": [
    { "id": "uuid", "dayOfWeek": 1, "startTime": "09:00", "endTime": "12:00" }
  ],
  "days": [
    {
      "date": "2026-06-29",
      "windows": [ { "startTime": "09:00", "endTime": "12:00" } ]
    }
  ]
}
```
- `weekly` = general template windows (FR-001/FR-002).
- `days` = one entry per date in the horizon (materialized snapshot, the source of truth — FR-003/FR-008). A date with an empty `windows` array is fully unavailable.

## PUT `/api/v1/admin/availability/weekly`

Replaces the **general template** (unchanged endpoint, re-roled). Does **not** modify any already-materialized week (FR-007).

**Request**:
```json
{ "windows": [ { "dayOfWeek": 1, "startTime": "09:00", "endTime": "12:00" } ] }
```
**200 Response**:
```json
{
  "weekly": [ { "id": "uuid", "dayOfWeek": 1, "startTime": "09:00", "endTime": "12:00" } ],
  "bookingConflicts": [ { "bookingId": "uuid", "studentEmail": "s@x.com", "slotStart": "2026-06-29T07:00:00Z" } ]
}
```
- `422 VALIDATION_ERROR` if any `endTime <= startTime` or unparseable time (reuses the existing `IllegalArgumentException` mapping).

## PUT `/api/v1/admin/availability/days/{date}`  *(NEW — replaces POST/DELETE `/exceptions`)*

Sets the absolute available windows for one date in a materialized week (FR-005). Replaces all existing windows for that date atomically.

**Request**:
```json
{ "windows": [ { "startTime": "10:00", "endTime": "13:00" }, { "startTime": "15:00", "endTime": "18:00" } ] }
```
**200 Response**:
```json
{
  "date": "2026-06-29",
  "windows": [ { "startTime": "10:00", "endTime": "13:00" }, { "startTime": "15:00", "endTime": "18:00" } ],
  "bookingConflicts": [ { "bookingId": "uuid", "studentEmail": "s@x.com", "slotStart": "2026-06-29T13:00:00Z" } ]
}
```
- An empty `windows` array clears the date (fully off, that week only — FR-006).
- `422 VALIDATION_ERROR` — `endTime <= startTime` or unparseable time; also returned when the date is in the past or outside the current horizon. (All reuse the existing `IllegalArgumentException → VALIDATION_ERROR` mapping; no new error codes are introduced.)

## Removed endpoints

- `POST /api/v1/admin/availability/exceptions` — removed (replaced by per-day PUT).
- `DELETE /api/v1/admin/availability/exceptions/{id}` — removed.

## Unchanged

- `GET /api/v1/schedule` (public) — same response contract; now computed from `week_availability`. Students see each week's source-of-truth calendar (FR-008).
- Booking create/cancel contracts unchanged.
