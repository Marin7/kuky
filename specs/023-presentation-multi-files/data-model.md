# Data Model: Multiple Files per Presentation

## Entity: Presentation File (1:N under Presentation)

One uploaded teaching file attached to a presentation. Replaces the previous 1:1 row keyed only by `presentation_id`.

### Table: `presentation_files` (reshaped)

| Column | Type | Notes |
|--------|------|--------|
| `id` | `UUID` PK | Surrogate identity; also disk key |
| `presentation_id` | `UUID` NOT NULL | FK → `presentations(id)` `ON DELETE CASCADE` |
| `original_name` | `VARCHAR(255)` NOT NULL | Filename as uploaded (may collide) |
| `display_name` | `VARCHAR(255)` NOT NULL | Distinct label for UI + Content-Disposition; set once at insert |
| `content_type` | `VARCHAR(100)` NOT NULL | `application/pdf` or PPTX MIME |
| `byte_size` | `INT` NOT NULL | Bytes on disk |
| `created_at` | `TIMESTAMPTZ` NOT NULL DEFAULT NOW() | Oldest-first sort key |

### Constraints & indexes

- `PRIMARY KEY (id)`
- `FOREIGN KEY (presentation_id) REFERENCES presentations(id) ON DELETE CASCADE`
- `UNIQUE (presentation_id, display_name)` — supports stable collision allocation under concurrency
- Index `presentation_files_presentation_created_idx ON (presentation_id, created_at)` for list order
- App-enforced: ≤ 10 rows per `presentation_id`; `byte_size` > 0 and ≤ 50 MiB; allowed content types only

### Validation rules (service)

- Non-empty multipart; extension/MIME must resolve to PDF or PPTX (same sniff as today)
- Reject upload when `COUNT(*) >= 10` for that presentation with a clear Spanish error message
- `display_name` allocation (case-insensitive compare of existing `display_name` values on the same presentation):
  - If `original_name` unused as a display name → `display_name = original_name`
  - Else find smallest `n ≥ 2` such that `"{base} ({n}){ext}"` is unused → assign that
  - Never rewrite existing rows’ `display_name` on later upload/remove

### Relationships

```text
presentations 1 ──< presentation_files
presentations 1 ──< presentation_shares     (unchanged; access is presentation-scoped)
```

Deleting a presentation cascades DB rows; service/store must delete each file’s disk blob (by `id`), or delete-by-presentation helper that lists ids then deletes.

## Migration notes (`V5__presentation_multi_files.sql`)

1. Add `id UUID` (nullable temporarily), `display_name VARCHAR(255)`.
2. Backfill: `id = gen_random_uuid()`, `display_name = original_name` for existing rows.
3. Set `id` / `display_name` NOT NULL; drop old PK on `presentation_id`; add PK on `id`; add UNIQUE `(presentation_id, display_name)`; add index.
4. Disk remapper (app startup, idempotent): for each row, if `{presentationId}.pptx` exists and `{fileId}.pptx` does not, rename; if both exist, prefer file-id path and optionally delete obsolete presentation-id path when sizes match.

## DTO projection (API)

```text
PresentationFileSummary:
  id: UUID (string)
  displayName: string
  originalName: string   # optional for admin debugging; required in contract as available
  contentType: string    # optional on list if unused by UI
  byteSize: number       # optional on list if unused by UI
  createdAt: Instant     # optional; order is array order

PresentationSummary / PresentationDetail / SharedPresentationSummary:
  …existing non-file fields…
  files: PresentationFileSummary[]   # oldest-first; [] when none
  # REMOVED: hasFile, originalFileName
```

Clients treat “has files” as `files.length > 0`.

## Unchanged entities

- `presentations`, `presentation_shares`, `presentation_slides`, unit assignment / `unit_id` — no structural change for this feature.
