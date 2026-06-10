# API Contract: Recursos — Paid & Free Teaching Resources

Base URL (local): `http://localhost:8081/api/v1`
All responses are JSON. Auth uses the existing `auth-token` HTTP-only cookie (sent with `credentials: "include"`). Errors follow the established envelope: `{"error":"ERROR_CODE","message":"..."}` (see `GlobalExceptionHandler`).

Money is expressed as integer cents in EUR (`priceCents`, `amountCents`) with `currency: "EUR"`; the frontend formats with `Intl.NumberFormat`. Timestamps are UTC ISO-8601 instants.

The catalogue and resource detail/preview are **public** but **ownership-aware**: when the request carries a valid auth cookie, `owned`/`locked` reflect the caller's entitlements; when anonymous, all paid items report `owned: false`, `locked: true`.

---

## Security (SecurityConfig change)

- `GET /api/v1/resources`, `GET /api/v1/resources/{slug}`, `GET /api/v1/resources/{slug}/content` → **permitAll** (add alongside `/api/v1/auth/**` and `GET /api/v1/schedule`). Ownership and content gating are enforced **in the service**, not by URL rules.
- `POST /api/v1/purchases`, `GET /api/v1/purchases`, `GET /api/v1/purchases/{id}/receipt` → **authenticated** (covered by `anyRequest().authenticated()`); unauthenticated calls receive `401`.

---

## 1. Get catalogue (public, ownership-aware) — FR-002, FR-003, FR-004, FR-013

`GET /api/v1/resources`

Returns published free resources, paid resources, and bundles. `owned`/`locked` reflect the caller when authenticated.

**200 OK**
```json
{
  "currency": "EUR",
  "freeResources": [
    {
      "slug": "saludos-basicos-preview",
      "title": "Saludos básicos — muestra",
      "description": "Una ficha de ejemplo para empezar.",
      "level": "A1",
      "category": "Vocabulario",
      "pricing": "FREE",
      "priceCents": null,
      "owned": true,
      "locked": false,
      "relatedResourceSlug": "pack-vocabulario-a1"
    }
  ],
  "paidResources": [
    {
      "slug": "pack-vocabulario-a1",
      "title": "Pack de vocabulario A1",
      "description": "50 fichas imprimibles para nivel A1.",
      "level": "A1",
      "category": "Vocabulario",
      "pricing": "PAID",
      "priceCents": 1500,
      "owned": false,
      "locked": true,
      "relatedResourceSlug": null
    }
  ],
  "bundles": [
    {
      "slug": "pack-completo-a1",
      "title": "Pack completo A1",
      "description": "Todos los recursos de nivel A1.",
      "priceCents": 3500,
      "resourceSlugs": ["pack-vocabulario-a1", "gramatica-a1"],
      "owned": false
    }
  ]
}
```
- A bundle's `owned` is `true` only when the caller already holds entitlements to **all** its member resources.
- Empty arrays drive the frontend "coming soon" state (FR-013).

---

## 2. Get resource detail (public, ownership-aware) — FR-002, FR-004, FR-012

`GET /api/v1/resources/{slug}`

**200 OK**
```json
{
  "slug": "pack-vocabulario-a1",
  "title": "Pack de vocabulario A1",
  "description": "50 fichas imprimibles para nivel A1.",
  "level": "A1",
  "category": "Vocabulario",
  "pricing": "PAID",
  "priceCents": 1500,
  "currency": "EUR",
  "previewText": "Incluye una ficha de muestra y el índice completo…",
  "owned": false,
  "locked": true,
  "relatedResourceSlug": null
}
```
- `previewText` is always returned (public teaser). Protected `assets` are **not** in this response; fetch them via the content endpoint.

**Errors**: `404 RESOURCE_NOT_FOUND` when no published resource has that slug.

---

## 3. Get resource content (gated) — FR-005, FR-008

`GET /api/v1/resources/{slug}/content`

Returns the protected material payload (downloadable files + embedded media). Allowed when the resource is FREE, or when the caller holds an entitlement.

**200 OK**
```json
{
  "slug": "pack-vocabulario-a1",
  "assets": [
    { "assetType": "FILE",  "label": "Fichas A1 (PDF)", "locator": "https://…/pack-a1.pdf" },
    { "assetType": "EMBED", "label": "Vídeo de uso",      "locator": "https://…/embed/abc" }
  ]
}
```

**Errors**
| HTTP | error | When |
|------|-------|------|
| 403 | `RESOURCE_LOCKED` | Resource is PAID and the caller (anonymous or logged-in) does not own it |
| 404 | `RESOURCE_NOT_FOUND` | No published resource with that slug |

> Note: for placeholder resources the `assets` array may be empty (materials authored later via the backoffice). When real file serving is added, it will reuse the same entitlement check.

---

## 4. Purchase a resource or bundle (auth, placeholder) — FR-006, FR-009, FR-010, FR-017, FR-018, FR-019

`POST /api/v1/purchases`

**Request**
```json
{ "itemType": "RESOURCE", "slug": "pack-vocabulario-a1" }
```
`itemType` ∈ `RESOURCE` | `BUNDLE`; `slug` identifies the resource or bundle.

**201 Created**
```json
{
  "id": "5f1c…",
  "itemType": "RESOURCE",
  "slug": "pack-vocabulario-a1",
  "title": "Pack de vocabulario A1",
  "amountCents": 1500,
  "currency": "EUR",
  "receiptReference": "REC-2026-000007",
  "paymentProvider": "placeholder",
  "grantedResourceSlugs": ["pack-vocabulario-a1"],
  "purchasedAt": "2026-06-09T10:12:00Z"
}
```
- For a `BUNDLE`, `grantedResourceSlugs` lists every member resource newly entitled (resources already owned are omitted but still accessible — FR-019).
- This release: the placeholder provider auto-approves; the response is the proof of the granted unlock. No real charge occurs.

**Error responses**
| HTTP | error | When |
|------|-------|------|
| 401 | (filter) | Not signed in (FR-009 — frontend routes to sign-in and retries) |
| 404 | `RESOURCE_NOT_FOUND` | No such published resource/bundle for the slug+type |
| 422 | `NOT_PURCHASABLE` | Target resource is FREE (nothing to buy) |
| 409 | `ALREADY_OWNED` | Caller already owns the resource, or already owns every resource in the bundle |

Side effects on success: a `purchases` row is recorded; one `entitlements` row per granted resource (insert-on-conflict-do-nothing for overlap); the resource(s) become permanently accessible (FR-007).

---

## 5. List my purchases (auth) — FR-016

`GET /api/v1/purchases`

Returns the authenticated customer's purchase history (most recent first).

**200 OK**
```json
{
  "currency": "EUR",
  "purchases": [
    {
      "id": "5f1c…",
      "itemType": "BUNDLE",
      "slug": "pack-completo-a1",
      "title": "Pack completo A1",
      "amountCents": 3500,
      "receiptReference": "REC-2026-000007",
      "purchasedAt": "2026-06-09T10:12:00Z",
      "grantedResourceSlugs": ["pack-vocabulario-a1", "gramatica-a1"]
    }
  ]
}
```

---

## 6. Get a receipt (auth, owner only) — FR-016

`GET /api/v1/purchases/{id}/receipt`

**200 OK**
```json
{
  "receiptReference": "REC-2026-000007",
  "purchasedAt": "2026-06-09T10:12:00Z",
  "buyerEmail": "teacher@example.com",
  "itemType": "BUNDLE",
  "itemTitle": "Pack completo A1",
  "lineItems": [
    { "title": "Pack de vocabulario A1" },
    { "title": "Gramática A1" }
  ],
  "amountCents": 3500,
  "currency": "EUR",
  "seller": "Español con Paula"
}
```
The frontend renders this as a printable receipt (browser print → PDF).

**Errors**: `404 BOOKING_NOT_FOUND`-style `RESOURCE_NOT_FOUND`/`PURCHASE_NOT_FOUND` when the purchase does not exist or does not belong to the caller (do not leak others' purchases).

---

## Error code summary (added to GlobalExceptionHandler)

| Code | HTTP | Exception |
|------|------|-----------|
| `RESOURCE_NOT_FOUND` | 404 | `ResourceNotFoundException` (resource, bundle, or purchase not found / not caller's) |
| `RESOURCE_LOCKED` | 403 | `ResourceLockedException` (paid content requested without entitlement) |
| `NOT_PURCHASABLE` | 422 | `NotPurchasableException` (attempt to buy a FREE resource) |
| `ALREADY_OWNED` | 409 | `AlreadyOwnedException` (resource/all-bundle-members already owned) |

Validation errors on the request body reuse the existing `VALIDATION_ERROR` (400) handler.
