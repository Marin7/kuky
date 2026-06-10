# Implementation Plan: Recursos — Paid & Free Teaching Resources

**Branch**: `004-resources-marketplace` | **Date**: 2026-06-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-resources-marketplace/spec.md`

## Summary

Rename the `/clases` placeholder tab to **Recursos** and turn it into a catalogue of Spanish-teaching resources (aimed at fellow teachers) authored by Paula. The page shows a fully-public **free / preview** section and a **paid catalogue** of resources and bundles, each card showing title, description, level/topic, price in EUR, and a lock/owned indicator. Logged-in customers can "buy" a paid resource or bundle through a **placeholder checkout** ("no real payment taken") that immediately and permanently grants ownership and unlocks the protected materials; ownership persists per account and dedups across bundles. Customers see a purchase history with a printable receipt per purchase. Free resources/previews carry calls-to-action to unlock the related paid resource or to book a 1-on-1 class on `/reservas`.

The backend extends the existing Java 26 / Spring Boot 4 service (plain JDBC + Flyway, no JPA) with a new `resources` package mirroring the established `auth`/`scheduling` layout. Five new tables (`resources`, `resource_assets`, `bundles`, `bundle_resources`, `purchases`, `entitlements`) are created and seeded with clearly-labelled placeholder content via a single Flyway migration `V4__create_resources.sql`; the future backoffice will write to the same tables. Money is abstracted behind a `PaymentProvider` interface whose only implementation this release is `PlaceholderPaymentProvider` (auto-approves, returns a synthetic reference) — exactly mirroring the `MeetingProvider`/`StubMeetingProvider` pattern from scheduling, so a real processor (e.g. Stripe) drops in later with no model rework. Ownership is enforced server-side: the catalogue and previews are public, but protected materials are served only to entitled (or free) viewers. Prices are stored as integer cents in EUR. The frontend renames the route to `/recursos`, updates the nav, adds a `components/resources/` folder of named Shadcn components, and an isolated `lib/resources.ts` API client following the existing `lib/auth.ts` / `lib/scheduling.ts` pattern.

## Technical Context

**Language/Version**: TypeScript 5.x strict (frontend) / Java 26 (backend, per `back-end/build.gradle` toolchain)

**Primary Dependencies**:
- Frontend: React 19, TanStack Start 1.x (SSR), TanStack Router (file-based), TanStack Query (already wired in `__root.tsx`), TailwindCSS 4, Shadcn UI, Vite 7. **No new dependencies** — money formatting via the platform `Intl.NumberFormat`; printable receipts via the browser print dialog.
- Backend: Spring Boot 4.0.x (`spring-boot-starter-web`, `-security`, `-jdbc`, `-validation`), Flyway, PostgreSQL driver, jjwt. **No new dependencies** — the placeholder payment provider is plain Java; no payment SDK, no PDF library.

**Storage**: PostgreSQL 18 — five/six new tables created by Flyway `V4__create_resources.sql` (`resources`, `resource_assets`, `bundles`, `bundle_resources`, `purchases`, `entitlements`), plus seeded placeholder rows. Reuses the existing `users` table for ownership FK.

**Testing**:
- Backend: JUnit 5 + Spring Boot Test (already present). Unit tests for `PurchaseService` (placeholder grant, already-owned dedup, free-not-purchasable, bundle overlap) and `CatalogService` (ownership flagging, free/paid split). Slice/integration test for the controllers (public catalogue, gated content 403, authenticated purchase flow).
- Frontend: visual verification in a running browser per the constitution (no test framework configured in the repo).

**Target Platform**: Browser via TanStack Start SSR (frontend) + JVM server on port 8081 (backend REST API).

**Project Type**: Full-stack web application — SSR React frontend (`front-end/`) + REST API backend (`back-end/`).

**Performance Goals**: Recursos page loads and a free resource is reachable in < 30 s (SC-002); a placeholder purchase from selection to unlocked access completes in < 3 min (SC-003). Catalogue is a handful of rows; all paths are well within standard request latencies.

**Constraints**:
- Free resources/previews are fully public — no authentication required (FR-005).
- Purchasing requires authentication (reuse existing JWT-cookie auth); unauthenticated buyers are routed to sign-in and returned to the purchase (FR-009).
- This release's checkout is a placeholder: confirming immediately records ownership and unlocks, with no real money moved and no payment-instrument data stored (FR-017). Money is behind a `PaymentProvider` so a real processor can replace the placeholder without reworking the catalogue/ownership model.
- Paid resources sell as one-time purchases (permanent access); bundles unlock all member resources; overlap with already-owned resources dedups gracefully (FR-018, FR-019) via a `UNIQUE(user_id, resource_id)` entitlement constraint with insert-on-conflict-do-nothing.
- Protected materials (downloadable files and/or embedded media) are served only to entitled or free viewers, including via direct content requests (FR-008).
- Prices and recorded amounts are in euros, stored as integer cents; multi-currency is out of scope.
- Actual material payloads and catalogue management are authored later via a future backoffice; this release seeds clearly-labelled placeholder content and handles the empty/"coming soon" state (FR-013).

**Scale/Scope**: Small — one author (Paula), a low number of resources/bundles, dozens to low hundreds of customers; single-instance backend.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I — Simplicity First | ✅ PASS | No payment SDK (placeholder provider only), no file upload/storage or PDF generation this phase, no subscriptions, no discounts/tax/refunds. Catalogue data lives in real DB tables seeded by one migration (the same tables the backoffice will use). Printable receipt via browser print. New tables are the minimal normalized set the bundle + dedup + history requirements actually need. |
| II — Component-Driven UI | ✅ PASS | Catalogue, resource card, bundle card, free section, purchase dialog, resource detail, and purchase history are named Shadcn-based React components; no raw DOM manipulation. |
| III — Evolution-Ready Architecture | ✅ PASS | API calls isolated in `front-end/src/lib/resources.ts`; money behind a backend `PaymentProvider` abstraction (placeholder now, real processor later) mirroring the existing `MeetingProvider`; catalogue source isolated behind `CatalogService`; ownership model designed so the future backoffice writes to the same tables. |
| Technology Stack | ✅ PASS | Frontend stack unchanged; backend continues Java 26 + Spring Boot 4 + plain JDBC + Flyway. No stack additions. |
| Development Workflow | ✅ PASS | UI changes browser-verified before completion; feature branch `004-resources-marketplace` follows the naming convention; no dead code. |
| Backend location | ✅ PASS | New code under `back-end/` and `front-end/` only. |

**Gate result**: All principles satisfied. No complexity violations requiring justification — Complexity Tracking section omitted.

## Project Structure

### Documentation (this feature)

```text
specs/004-resources-marketplace/
├── plan.md              # This file
├── research.md          # Phase 0 — payment abstraction, data source, materials/access, receipts, route rename
├── data-model.md        # Phase 1 — resources/bundles/purchases/entitlements schema + virtual catalogue model
├── quickstart.md        # Phase 1 — local validation guide
├── contracts/
│   └── api.md           # Phase 1 — REST API contract for catalogue, content, purchases, receipts
├── checklists/
│   └── requirements.md  # Spec quality checklist (from /speckit-specify)
└── tasks.md             # Phase 2 — task list (/speckit-tasks output, not created here)
```

### Source Code (repository root)

```text
kuky/
├── front-end/                                  # Existing React/TanStack SSR app
│   └── src/
│       ├── routes/
│       │   ├── recursos.tsx                     # NEW: catalogue + free section + my-purchases (replaces clases.tsx)
│       │   └── clases.tsx                        # REMOVED (renamed to recursos.tsx)
│       ├── components/
│       │   ├── SiteHeader.tsx                    # UPDATED: nav entry "Clases" → "Recursos", /clases → /recursos
│       │   └── resources/                        # NEW component folder
│       │       ├── ResourcesView.tsx             # Orchestrates fetch + free/paid/bundle sections + empty state
│       │       ├── FreeResourcesSection.tsx      # Public free/preview section with conversion CTAs
│       │       ├── ResourceCard.tsx              # Title, description, level badge, price/Gratis, lock/owned indicator
│       │       ├── BundleCard.tsx                # Bundle: member list + combined price + buy/owned
│       │       ├── ResourceDetailDialog.tsx      # Preview + (materials if unlocked / lock + buy CTA if not)
│       │       ├── PurchaseDialog.tsx            # Placeholder checkout: "no real payment" notice → confirm → unlock
│       │       └── MyPurchases.tsx               # Logged-in: owned resources + purchase history + receipt view
│       └── lib/
│           └── resources.ts                      # NEW: API client (getCatalog, getResource, getResourceContent, purchase, listPurchases, getReceipt)
└── back-end/                                     # Existing Spring Boot service
    └── src/
        ├── main/
        │   ├── java/com/kuky/backend/
        │   │   ├── resources/                    # NEW package (mirrors auth/ and scheduling/ layout)
        │   │   │   ├── controller/CatalogController.java     # GET /api/v1/resources, /{slug}, /{slug}/content (public, ownership-aware)
        │   │   │   ├── controller/PurchaseController.java    # POST/GET /api/v1/purchases, GET /{id}/receipt (auth)
        │   │   │   ├── dto/                                  # CatalogResponse, ResourceCard, ResourceDetail, ContentResponse, PurchaseRequest, PurchaseResponse, ReceiptResponse
        │   │   │   ├── model/Resource.java                   # POJO
        │   │   │   ├── model/ResourceAsset.java              # POJO (FILE | EMBED)
        │   │   │   ├── model/Bundle.java                     # POJO
        │   │   │   ├── model/Purchase.java                   # POJO
        │   │   │   ├── model/Entitlement.java                # POJO
        │   │   │   ├── repository/ResourceRepository.java    # NamedParameterJdbcTemplate + RowMappers
        │   │   │   ├── repository/PurchaseRepository.java    # purchases + entitlements (insert-on-conflict dedup)
        │   │   │   ├── service/CatalogService.java           # builds catalogue, flags owned, gates content
        │   │   │   ├── service/PurchaseService.java          # validate → PaymentProvider → record purchase + entitlements
        │   │   │   ├── service/ReceiptService.java           # builds receipt view from a purchase
        │   │   │   ├── payment/PaymentProvider.java          # interface: authorize(amount, ref) → PaymentResult
        │   │   │   ├── payment/PaymentProviderConfig.java    # @Bean factory (placeholder now; real processor later)
        │   │   │   ├── payment/PlaceholderPaymentProvider.java # auto-approves, returns synthetic reference, logs WARN
        │   │   │   └── exception/                            # ResourceNotFoundException, AlreadyOwnedException, NotPurchasableException, ResourceLockedException
        │   │   └── config/
        │   │       ├── SecurityConfig.java                   # UPDATED: permit GET /api/v1/resources/**; purchases stay authenticated
        │   │       └── GlobalExceptionHandler.java           # UPDATED: map new resource/purchase error codes
        │   └── resources/
        │       └── db/migration/
        │           └── V4__create_resources.sql             # NEW migration + seeded placeholder catalogue
        └── test/java/com/kuky/backend/
            └── resources/
                ├── PurchaseServiceTest.java
                ├── CatalogServiceTest.java
                └── ResourcesControllerIntegrationTest.java
```

**Structure Decision**: Extend the existing two-service layout. The backend gains a self-contained `resources` package that mirrors the established `auth` / `scheduling` conventions (controller / dto / model / repository / service, plain JDBC repositories, POJO models, records for DTOs) and reuses the `PaymentProvider`-as-`MeetingProvider` abstraction pattern. The frontend renames the `/clases` placeholder route to `/recursos`, updates the single nav definition in `SiteHeader.tsx`, and adds a `components/resources/` folder plus an isolated `lib/resources.ts` API client following the existing `lib/scheduling.ts` pattern. No new top-level projects, infrastructure, or third-party dependencies are introduced.
