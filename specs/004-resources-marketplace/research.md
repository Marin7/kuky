# Phase 0 Research: Recursos — Paid & Free Teaching Resources

All Technical Context unknowns were resolvable from the spec, the existing codebase conventions, and the four clarifications recorded in the spec. No open `NEEDS CLARIFICATION` items remain. Decisions below.

---

## 1. How is "payment" modelled when no real processor exists yet?

**Decision**: Introduce a `PaymentProvider` interface with a single `PlaceholderPaymentProvider` implementation that auto-approves and returns a synthetic payment reference. A `PaymentProviderConfig` `@Bean` factory selects it. `PurchaseService` calls `paymentProvider.authorize(...)`, and on approval records the purchase + entitlements.

**Rationale**: This is the exact pattern already proven in the scheduling feature (`MeetingProvider` → `StubMeetingProvider` selected by `MeetingProviderConfig`, degrading to a no-op locally). It satisfies FR-017 ("a real payment processor can be wired in later without reworking the catalogue or ownership model") by isolating the only money-touching seam behind an interface. The placeholder logs a WARN ("no real payment taken") so it is obvious in dev which provider is active.

**Alternatives considered**:
- *Inline the "always succeed" logic in `PurchaseService`*: rejected — bakes the placeholder into business logic, forcing a rewrite when a real processor arrives (violates Evolution-Ready).
- *Integrate Stripe now*: rejected — out of scope per the clarification (placeholder this release); the materials being sold don't exist yet, and pulling in a payment SDK violates Simplicity First.
- *Use Spring `@ConditionalOnProperty` to pick the provider*: rejected for the same reason documented in CLAUDE.md for `MeetingProvider` — Spring Boot 4 treats an empty-string property as "present", registering both beans. Use a `@Configuration` factory that inspects config at runtime instead.

---

## 2. Where does catalogue data live before the backoffice exists?

**Decision**: Create real PostgreSQL tables (`resources`, `resource_assets`, `bundles`, `bundle_resources`) and seed a small set of clearly-labelled placeholder rows in the same `V4__create_resources.sql` migration. The catalogue is served from these tables via `CatalogService`.

**Rationale**: The constitution's Evolution-Ready principle requires that "static data used today MUST live in the same locations where API calls will live tomorrow." Building the tables + REST API now means the future backoffice simply writes to the same schema — no migration of a hardcoded list into a database later. Seeding a couple of free and paid resources plus a bundle lets every user story (catalogue, free preview, purchase/unlock, access control, history) be demonstrated end-to-end immediately.

**Alternatives considered**:
- *Hardcode resources in the frontend or a Java list*: rejected — would have to be thrown away and re-modelled once the backoffice lands; cannot represent per-user ownership.
- *Wait for the backoffice*: rejected — the customer-facing catalogue, purchase, and access-control behaviour are the whole point of this feature and are independent of how content is authored.

The empty/"coming soon" state (FR-013) is still implemented and triggers whenever no rows are `published`, so removing the seed data degrades gracefully.

---

## 3. What shape do the protected materials take, and how is access enforced?

**Decision**: Model resource content as a `resource_assets` table where each row is either a `FILE` (a locator: storage key / URL) or an `EMBED` (a video or external link URL). The public catalogue and a resource's `preview_text` are served to everyone; a dedicated `GET /api/v1/resources/{slug}/content` endpoint returns the assets only when the resource is FREE or the caller holds an entitlement, otherwise `403 RESOURCE_LOCKED`. Actual file bytes are **not** served this release (no files exist yet); the locator is a placeholder string the backoffice will populate, and the same ownership check will guard file serving when it is added.

**Rationale**: The "Mixed files + embedded" clarification means the model must accommodate both downloadable files and embedded media, so a typed asset list is the natural shape. Enforcing ownership in a single content endpoint (rather than relying on Spring URL rules) keeps the catalogue/preview public while gating only the payload, and gives one clearly testable enforcement point for FR-008. Deferring real upload/storage/streaming to the backoffice phase keeps this release within Simplicity First while leaving the access seam in place.

**Alternatives considered**:
- *Store materials as a single blob/markdown column*: rejected — cannot represent the mix of downloadable files and embedded media, and provides no per-asset structure for future file serving.
- *Gate content purely via Spring Security URL matchers*: rejected — ownership is per-row and per-user (not a static URL pattern), so it must be checked in the service against entitlements.

---

## 4. How are bundles and overlapping ownership handled?

**Decision**: A `bundle_resources` join table lists each bundle's member resources. Ownership is recorded in an `entitlements` table with a `UNIQUE(user_id, resource_id)` constraint. Buying a bundle creates one `purchases` row (`item_type = BUNDLE`) and inserts an entitlement per member resource using `INSERT ... ON CONFLICT (user_id, resource_id) DO NOTHING`, so resources the customer already owns are silently skipped and all bundle resources end up unlocked.

**Rationale**: Directly satisfies FR-019 ("no duplicate ownership records, and access to all bundle resources is granted") with a database-level guarantee rather than application bookkeeping. Separating `purchases` (the financial event + receipt) from `entitlements` (the access grants) cleanly supports both one-time resource purchases and bundle purchases, and a future subscription model could grant entitlements through the same table.

**Alternatives considered**:
- *Expand a bundle into N separate resource purchases*: rejected — loses the "bought as a bundle for a combined price" record needed for an accurate receipt and history.
- *Store ownership as an array column on `users`*: rejected — not relational, hard to dedup/constrain, breaks the established plain-JDBC/normalized style.

---

## 5. What is a "receipt", and how is it produced?

**Decision**: A receipt is a read view of a `purchases` row: a human-readable `receipt_reference` generated at purchase time, the purchase date, the item(s) and amount in EUR, and the buyer's email. `GET /api/v1/purchases/{id}/receipt` returns it as JSON (owner only); the frontend renders a printable receipt component that the user can save via the browser's print-to-PDF.

**Rationale**: Satisfies FR-016 (customer-visible history + a receipt per purchase) without adding a server-side PDF library (Simplicity First). The browser print dialog produces a PDF on every platform with zero new dependencies.

**Alternatives considered**:
- *Generate PDF server-side (e.g. a PDF library)*: rejected — new dependency and rendering complexity for no functional gain over print-to-PDF at this scale.
- *Email the receipt*: deferred — the email infrastructure (JavaMailSender/Mailpit) exists and a confirmation email could be added later, but it is not required by the spec and is out of scope this release.

---

## 6. Renaming `/clases` → `/recursos` without breaking the app

**Decision**: Rename the route file `clases.tsx` to `recursos.tsx`, update the single `nav` array in `SiteHeader.tsx` (label `Clases` → `Recursos`, `to` `/clases` → `/recursos`), and let TanStack Router regenerate `routeTree.gen.ts`. No manual edit of the generated route tree.

**Rationale**: Routing is file-based (per CLAUDE.md), and the nav is defined once in `SiteHeader.tsx` for both desktop and mobile, so a single nav edit covers FR-001's "both desktop and mobile" requirement and preserves the existing `activeProps`/`activeOptions` styling. `routeTree.gen.ts` is auto-generated and must not be hand-edited.

**Alternatives considered**:
- *Keep `/clases` and add `/recursos` as a second route*: rejected — leaves a dead placeholder route and dead code (violates the no-dead-code workflow rule).
- *Add a redirect from `/clases` to `/recursos`*: not needed — the site is not yet publicly indexed and there are no external inbound links to preserve; can be revisited if needed.

---

## 7. Currency representation

**Decision**: Store prices and purchase amounts as integer **cents** (`price_cents INT`, `amount_cents INT`) with an implicit/recorded currency of `EUR`. APIs expose `priceCents` (+ `currency: "EUR"`); the frontend formats with `Intl.NumberFormat("es-ES", { style: "currency", currency: "EUR" })`.

**Rationale**: Integer cents avoid floating-point rounding errors in money math. EUR-only matches the clarification; `es-ES` formatting keeps the page Spanish-consistent (FR-014).

**Alternatives considered**:
- *Decimal/float euros*: rejected — floating-point money is error-prone.
- *Multi-currency now*: rejected — out of scope per spec assumptions.
