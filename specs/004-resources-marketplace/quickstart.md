# Quickstart & Validation: Recursos — Paid & Free Teaching Resources

This guide validates the feature end-to-end against the user stories and acceptance scenarios in [spec.md](spec.md). It is a run/validation guide — implementation details live in `data-model.md`, `contracts/api.md`, and the upcoming `tasks.md`.

## Prerequisites

- PostgreSQL 18 running with the `kuky_dev` database (see CLAUDE.md "Local dev setup"). Flyway applies `V4__create_resources.sql` automatically on backend start, creating the tables and seeding placeholder content.
- Mailpit optional (not used by this feature).
- Node.js 22+/24+ and the frontend deps installed.

## Start the stack

```bash
# Backend (from back-end/)
./gradlew bootRun --args='--spring.profiles.active=local'   # http://localhost:8081

# Frontend (from front-end/)
npm run dev                                                  # http://localhost:8080
```

On first start, confirm the backend log shows Flyway applying `V4` and the placeholder provider is active (a WARN noting no real payment provider is configured).

---

## Scenario A — Rename & browse the catalogue (User Story 1, P1)

1. Load `http://localhost:8080`. In the header nav (desktop) the former **Clases** entry now reads **Recursos** and links to `/recursos`. Resize to mobile width / open the hamburger sheet → the same **Recursos** entry appears. *(FR-001, SC-001)*
2. Open **Recursos**. Confirm a visually distinct **free / preview** section and a **paid catalogue**; each card shows title, description, level badge, and either a price (e.g. `15,00 €`) or **Gratis**. *(FR-002, FR-003)*
3. A paid resource you do not own shows a **lock** indicator and a buy action, not the materials. *(FR-004)*
4. Bundles render with their member list and a combined price.

**Empty state**: with the backend running against an empty catalogue (e.g. all rows `published=false`), the page shows a graceful "próximamente" message instead of a broken layout. *(FR-013, SC-006)*

---

## Scenario B — Free resource is public & converts (User Story 3, P3)

1. While **logged out**, open a free resource from the free section. Its preview/content is viewable without any sign-in prompt. *(FR-005, SC-002)*
2. The free resource presents CTAs to **unlock the related paid resource** and to **book a 1-on-1 class** (links to `/reservas`). *(FR-012, SC-005)*
3. Selecting "unlock the full resource" routes to the purchase flow for the related paid resource. *(US3 scenario 2)*

API check:
```bash
curl -s http://localhost:8081/api/v1/resources | jq '.freeResources[].slug'
curl -s http://localhost:8081/api/v1/resources/<free-slug>/content | jq   # 200, assets returned without auth
```

---

## Scenario C — Purchase unlocks a paid resource (User Story 2, P2)

1. Logged **out**, attempt to buy a paid resource → you are prompted to sign in / register, then returned to the purchase. *(FR-009, US2 scenario 3)*
2. Sign in (existing `/cuenta` account), open a locked paid resource, choose **Comprar**. The placeholder checkout dialog shows a clear "no se realiza ningún pago real" notice; confirm. *(FR-017)*
3. The resource flips to **owned/unlocked** immediately and its materials become accessible. *(FR-006, US2 scenario 1)*
4. Revisit the catalogue (and reload) → the resource shows as owned with access, not a buy button. Open it on a second browser/session signed in as the same user → still owned. *(FR-007, SC-004, US2 scenario 2)*
5. Attempt to buy the same resource again → blocked. *(FR-010)*

API checks:
```bash
# Gated content denied before purchase
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8081/api/v1/resources/<paid-slug>/content   # 403

# Purchase (authenticated; reuse the auth-token cookie from /cuenta login)
curl -s -X POST http://localhost:8081/api/v1/purchases \
  -H 'Content-Type: application/json' --cookie 'auth-token=<jwt>' \
  -d '{"itemType":"RESOURCE","slug":"<paid-slug>"}' | jq

# Content now accessible
curl -s --cookie 'auth-token=<jwt>' http://localhost:8081/api/v1/resources/<paid-slug>/content | jq

# Second purchase rejected
# → 409 ALREADY_OWNED
```

---

## Scenario D — Bundle purchase & overlap dedup (FR-018, FR-019)

1. As a signed-in user who already owns **one** resource that is part of a bundle, buy the **bundle**.
2. Confirm the purchase succeeds and **every** member resource is now unlocked, with **no duplicate** ownership for the already-owned one.

API check:
```bash
curl -s -X POST http://localhost:8081/api/v1/purchases \
  -H 'Content-Type: application/json' --cookie 'auth-token=<jwt>' \
  -d '{"itemType":"BUNDLE","slug":"<bundle-slug>"}' | jq '.grantedResourceSlugs'
# grantedResourceSlugs omits the already-owned resource; all members are accessible
```

---

## Scenario E — Purchase history & receipt (FR-016, SC-007)

1. Signed in, open **Mis recursos** on `/recursos` → see owned resources plus a purchase history listing each purchase with date and amount.
2. Open a purchase's **receipt** → a printable receipt shows reference, date, item(s), amount in EUR, and buyer email; use the browser print dialog to save as PDF.

API checks:
```bash
curl -s --cookie 'auth-token=<jwt>' http://localhost:8081/api/v1/purchases | jq
curl -s --cookie 'auth-token=<jwt>' http://localhost:8081/api/v1/purchases/<id>/receipt | jq
# Requesting another user's purchase id → 404 (no leak)
```

---

## Success criteria coverage

| Criterion | Validated by |
|-----------|--------------|
| SC-001 nav shows Recursos, opens page (desktop+mobile) | Scenario A.1 |
| SC-002 free resource reachable < 30 s, no login | Scenario B.1 |
| SC-003 purchase → unlocked < 3 min | Scenario C.2–3 |
| SC-004 ownership persists; non-owners blocked | Scenario C.4 + content 403 |
| SC-005 free items present buy + book CTAs | Scenario B.2 |
| SC-006 usable/empty "coming soon" state | Scenario A empty state |
| SC-007 history + receipts; bundle unlocks all | Scenarios D & E |

## Backend tests

```bash
# from back-end/
./gradlew test --tests '*resources*'
```
Covers placeholder grant, already-owned dedup, free-not-purchasable, bundle overlap (`PurchaseServiceTest`); catalogue ownership flagging + free/paid split (`CatalogServiceTest`); public catalogue, gated content 403, authenticated purchase (`ResourcesControllerIntegrationTest`).
