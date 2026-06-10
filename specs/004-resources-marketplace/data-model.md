# Phase 1 Data Model: Recursos — Paid & Free Teaching Resources

Persisted in PostgreSQL 18 via Flyway migration `V4__create_resources.sql`. Style mirrors the existing `auth` and `scheduling` schemas: `UUID` PKs via `gen_random_uuid()`, `TIMESTAMPTZ` timestamps, `CHECK` constraints for enums, plain JDBC access (no JPA). Reuses the existing `users` table.

Money is stored as integer **cents** in EUR (`*_cents INT`). No floating-point amounts.

---

## Tables

### `resources`

A teaching material in the catalogue.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | `DEFAULT gen_random_uuid()` |
| `slug` | `VARCHAR(120)` | **UNIQUE**, URL-safe identifier for deep-linking (FR-008) |
| `title` | `VARCHAR(200)` | NOT NULL |
| `description` | `TEXT` | NOT NULL — short catalogue description |
| `level` | `VARCHAR(8)` | nullable — CEFR target level (`A1`..`C2`) or null |
| `category` | `VARCHAR(80)` | nullable — topic/category label |
| `pricing` | `VARCHAR(8)` | NOT NULL, `CHECK (pricing IN ('FREE','PAID'))` |
| `price_cents` | `INT` | nullable; **required when `pricing = 'PAID'`** (`CHECK`), null/0 for FREE |
| `preview_text` | `TEXT` | nullable — public teaser shown to everyone (the "free preview" of a paid resource) |
| `related_resource_id` | `UUID` | nullable FK → `resources(id)` — for a FREE resource, the PAID resource its CTA points to (FR-012) |
| `published` | `BOOLEAN` | NOT NULL `DEFAULT true` — unpublished rows excluded from the catalogue |
| `sort_order` | `INT` | NOT NULL `DEFAULT 0` — display ordering |
| `created_at` | `TIMESTAMPTZ` | NOT NULL `DEFAULT NOW()` |

Constraints:
- `CHECK (pricing = 'FREE' OR price_cents IS NOT NULL)` — paid resources must have a price.
- `related_resource_id` `ON DELETE SET NULL`.

### `resource_assets`

The protected material payload for a resource (downloadable files and/or embedded media). Empty for placeholder resources until authored via the backoffice.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | `DEFAULT gen_random_uuid()` |
| `resource_id` | `UUID` | NOT NULL FK → `resources(id)` `ON DELETE CASCADE` |
| `asset_type` | `VARCHAR(8)` | NOT NULL, `CHECK (asset_type IN ('FILE','EMBED'))` |
| `label` | `VARCHAR(200)` | NOT NULL — display name |
| `locator` | `TEXT` | NOT NULL — file storage key/URL (FILE) or media/link URL (EMBED). Placeholder string until real materials exist; never served to non-entitled viewers |
| `sort_order` | `INT` | NOT NULL `DEFAULT 0` |

Index: `resource_assets_resource_id_idx` on `(resource_id)`.

### `bundles`

A named group of resources sold at a combined price.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | `DEFAULT gen_random_uuid()` |
| `slug` | `VARCHAR(120)` | **UNIQUE** |
| `title` | `VARCHAR(200)` | NOT NULL |
| `description` | `TEXT` | NOT NULL |
| `price_cents` | `INT` | NOT NULL — combined bundle price in cents |
| `published` | `BOOLEAN` | NOT NULL `DEFAULT true` |
| `sort_order` | `INT` | NOT NULL `DEFAULT 0` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL `DEFAULT NOW()` |

### `bundle_resources`

Join table: which resources belong to a bundle.

| Column | Type | Notes |
|--------|------|-------|
| `bundle_id` | `UUID` | NOT NULL FK → `bundles(id)` `ON DELETE CASCADE` |
| `resource_id` | `UUID` | NOT NULL FK → `resources(id)` `ON DELETE CASCADE` |

Primary key: `(bundle_id, resource_id)`.

### `purchases`

A completed purchase event (financial record + receipt source). One row per checkout.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | `DEFAULT gen_random_uuid()` |
| `user_id` | `UUID` | NOT NULL FK → `users(id)` `ON DELETE CASCADE` |
| `item_type` | `VARCHAR(8)` | NOT NULL, `CHECK (item_type IN ('RESOURCE','BUNDLE'))` |
| `resource_id` | `UUID` | nullable FK → `resources(id)` |
| `bundle_id` | `UUID` | nullable FK → `bundles(id)` |
| `amount_cents` | `INT` | NOT NULL — amount paid in cents (0 for the placeholder, but the price is recorded as the catalogue price at purchase time) |
| `currency` | `CHAR(3)` | NOT NULL `DEFAULT 'EUR'` |
| `receipt_reference` | `VARCHAR(40)` | NOT NULL **UNIQUE** — human-readable receipt id (e.g. `REC-2026-000007`) |
| `payment_provider` | `VARCHAR(40)` | NOT NULL — e.g. `placeholder` (future: `stripe`) |
| `payment_reference` | `VARCHAR(120)` | nullable — synthetic reference from the placeholder provider |
| `purchased_at` | `TIMESTAMPTZ` | NOT NULL `DEFAULT NOW()` |

Constraints:
- `CHECK ((item_type = 'RESOURCE' AND resource_id IS NOT NULL AND bundle_id IS NULL) OR (item_type = 'BUNDLE' AND bundle_id IS NOT NULL AND resource_id IS NULL))` — exactly one target.
- Index `purchases_user_id_idx` on `(user_id)`.

### `entitlements`

A per-resource access grant for a user. Drives lock/unlock and content gating.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `UUID` PK | `DEFAULT gen_random_uuid()` |
| `user_id` | `UUID` | NOT NULL FK → `users(id)` `ON DELETE CASCADE` |
| `resource_id` | `UUID` | NOT NULL FK → `resources(id)` `ON DELETE CASCADE` |
| `source_purchase_id` | `UUID` | NOT NULL FK → `purchases(id)` `ON DELETE CASCADE` — the purchase that granted it |
| `granted_at` | `TIMESTAMPTZ` | NOT NULL `DEFAULT NOW()` |

Constraints:
- **`UNIQUE (user_id, resource_id)`** — a user owns a resource at most once. Bundle purchases insert with `ON CONFLICT (user_id, resource_id) DO NOTHING` to dedup overlap (FR-019).

---

## Relationships

```text
users 1───∞ purchases ∞ ──── (RESOURCE → resources | BUNDLE → bundles)
users 1───∞ entitlements ∞───1 resources          (UNIQUE per user+resource)
purchases 1───∞ entitlements                       (source_purchase_id)
resources 1───∞ resource_assets
bundles ∞───∞ resources   (via bundle_resources)
resources 0..1 ── related_resource_id ──> resources (free → paid CTA target)
```

---

## Derived / virtual concepts (not stored)

- **Ownership / `owned` flag**: a resource is "owned" by the current user iff an `entitlements` row exists for `(user_id, resource_id)`. FREE resources are treated as always accessible (no entitlement needed).
- **`locked` flag**: `pricing = 'PAID'` AND no entitlement for the caller. Logged-out viewers see all paid resources as locked.
- **Receipt**: a read projection of a `purchases` row joined to the buyer (`users.email`) and the purchased item's title(s); not a separate table.
- **Catalogue split**: `freeResources` = `pricing='FREE'`; `paidResources` = `pricing='PAID'`; `bundles` = published bundles — all filtered to `published = true` and ordered by `sort_order`.

---

## Validation & rules (from requirements)

| Rule | Source | Enforcement |
|------|--------|-------------|
| Paid resource must have a price | FR-002 | `CHECK` on `resources` |
| Free resources/previews are public | FR-005 | Catalogue + content endpoints serve FREE assets without auth |
| Paid materials gated to owners (incl. direct links) | FR-008 | `CatalogService` checks `entitlements` before returning assets; `403 RESOURCE_LOCKED` otherwise |
| Purchase requires auth | FR-009 | `/api/v1/purchases/**` authenticated in `SecurityConfig` |
| No duplicate purchase of an owned resource | FR-010 | `AlreadyOwnedException` when an entitlement already covers the target |
| Placeholder grants ownership on confirm | FR-017 | `PurchaseService` → `PlaceholderPaymentProvider` auto-approves → insert purchase + entitlements |
| Bundle unlocks all members, dedups overlap | FR-018/019 | `bundle_resources` expansion + `entitlements` `UNIQUE` with `ON CONFLICT DO NOTHING` |
| Amounts in EUR | FR-015 | `amount_cents` + `currency='EUR'` |
| Empty/"coming soon" when nothing published | FR-013 | Catalogue filters `published=true`; frontend renders empty state |

---

## Seed data (placeholder, in `V4`)

Clearly-labelled placeholder content so every user story is demonstrable in dev (removable without breaking the empty state):

- 2 FREE resources (one standalone, one with `related_resource_id` pointing at a paid resource) with a public `preview_text` and at least one `EMBED` asset.
- 2 PAID resources (`price_cents` e.g. `1500`, `2500`) each with `preview_text` and 1–2 placeholder assets (FILE + EMBED).
- 1 bundle grouping the 2 paid resources at a discounted combined `price_cents` (e.g. `3500`).
