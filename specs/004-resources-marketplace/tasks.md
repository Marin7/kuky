# Tasks: Recursos â€” Paid & Free Teaching Resources

**Input**: Design documents from `specs/004-resources-marketplace/`

**Prerequisites**: plan.md âœ… spec.md âœ… research.md âœ… data-model.md âœ… contracts/api.md âœ… quickstart.md âœ…

**Tests**: Limited to the three backend tests named in plan.md (`CatalogServiceTest`, `PurchaseServiceTest`, `ResourcesControllerIntegrationTest`). No frontend test framework is configured; UI is browser-verified per the constitution. No full TDD ordering is imposed.

**Organization**: Tasks grouped by user story (US1 = Browse catalogue, US2 = Purchase/unlock + history, US3 = Free-preview conversion) to enable independent implementation and delivery. Backend package `com.kuky.backend.resources` mirrors the existing `auth`/`scheduling` packages (plain JDBC, POJO models, record DTOs); money is abstracted behind a `PaymentProvider` exactly as scheduling abstracts `MeetingProvider`.

## Format: `[ID] [P?] [Story?] Description â€” file path`

- **[P]**: Can run in parallel (different files, no incomplete-task dependencies)
- **[Story]**: Which user story (US1, US2, US3); Setup/Foundational/Polish carry no story label
- All backend paths are under `back-end/src/main/java/com/kuky/backend/`; tests under `back-end/src/test/java/com/kuky/backend/`; frontend under `front-end/src/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the package layout mirroring the existing `scheduling` package. No user story work begins here.

- [X] T001 [P] Scaffold the backend package `back-end/src/main/java/com/kuky/backend/resources/` with subpackages `controller/`, `dto/`, `model/`, `repository/`, `service/`, `payment/`, `exception/` (created as files land below), and the frontend folder `front-end/src/components/resources/` â€” match the `scheduling` package conventions

**Checkpoint**: Project layout ready; no behaviour change yet.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The resource/bundle/purchase/entitlement schema and read-side persistence that every user story depends on (US1 reads the catalogue and entitlement flags; US2 writes purchases/entitlements; US3 reuses US1 reads).

**âš ï¸ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 Create `back-end/src/main/resources/db/migration/V4__create_resources.sql` per `data-model.md` â€” tables `resources`, `resource_assets`, `bundles`, `bundle_resources`, `purchases`, `entitlements` (UUID PKs `gen_random_uuid()`, `TIMESTAMPTZ`, `*_cents INT`, `CHECK` constraints for `pricing`/`asset_type`/`item_type`, `resources` price-required check, `purchases` exactly-one-target check, `entitlements` `UNIQUE(user_id, resource_id)`, FKs to `users(id)` ON DELETE CASCADE, indexes `resource_assets_resource_id_idx` + `purchases_user_id_idx`); then seed clearly-labelled placeholder content (2 FREE incl. one with `related_resource_id`, 2 PAID with `price_cents`, 1 bundle grouping the paid pair, sample `resource_assets`)
- [X] T003 [P] Create `Resource` POJO in `resources/model/Resource.java` (fields per `data-model.md`; getters/setters in `auth/model/User.java` style)
- [X] T004 [P] Create `ResourceAsset` POJO + `AssetType` enum (`FILE`, `EMBED`) in `resources/model/ResourceAsset.java`
- [X] T005 [P] Create `Bundle` POJO in `resources/model/Bundle.java` (incl. member resource slugs/ids)
- [X] T006 [P] Create `Purchase` POJO + `ItemType` enum (`RESOURCE`, `BUNDLE`) in `resources/model/Purchase.java`
- [X] T007 [P] Create `Entitlement` POJO in `resources/model/Entitlement.java`
- [X] T008 Create `ResourceRepository` (`NamedParameterJdbcTemplate`) in `resources/repository/ResourceRepository.java` â€” `RowMapper`s; read methods `findPublishedResources()`, `findResourceBySlug(String)`, `findAssetsByResourceId(UUID)`, `findPublishedBundles()` + `findBundleMembers(UUID)`, `findBundleBySlug(String)`, and entitlement reads `findOwnedResourceIds(UUID userId)` / `hasEntitlement(UUID userId, UUID resourceId)` (depends on T003â€“T007)

**Checkpoint**: Schema migrates on boot with seed data; catalogue read layer ready.

---

## Phase 3: User Story 1 â€” Browse the Recursos catalogue (Priority: P1) ðŸŽ¯ MVP

**Goal**: The nav tab reads **Recursos** (desktop + mobile) and `/recursos` shows a public free/preview section and a paid catalogue (+ bundles) with title/description/level/price and lock/owned indicators; free materials are viewable without login; empty state degrades gracefully.

**Independent Test**: Open `/recursos` logged out â†’ free section + paid catalogue render with correct prices and lock indicators; a free resource's materials open without any sign-in; `GET /api/v1/resources` returns the catalogue with no cookie (quickstart Scenarios A & B.1).

### Backend

- [X] T009 [P] [US1] Create `ResourceNotFoundException` and `ResourceLockedException` in `resources/exception/`
- [X] T010 [P] [US1] Create record DTOs in `resources/dto/` â€” `CatalogResponse` (currency, freeResources[], paidResources[], bundles[]), `ResourceCardDto`, `BundleCardDto`, `ResourceDetailDto`, `ContentResponse` + `AssetDto` â€” per `contracts/api.md` Â§1â€“Â§3
- [X] T011 [US1] Implement `CatalogService` in `resources/service/CatalogService.java` â€” `getCatalog(Optional<UUID> userId)` (split free/paid, build bundle cards, flag `owned`/`locked` from entitlements; bundle `owned` only when all members owned), `getResource(slug, Optional<UUID>)`, `getContent(slug, Optional<UUID>)` (return assets when FREE or entitled, else throw `ResourceLockedException`; `ResourceNotFoundException` when missing) (depends on T008, T009, T010)
- [X] T012 [US1] Implement `CatalogController` in `resources/controller/CatalogController.java` â€” `GET /api/v1/resources`, `GET /api/v1/resources/{slug}`, `GET /api/v1/resources/{slug}/content`; resolve the optional authenticated user id from the security context for ownership flags (depends on T011)
- [X] T013 [US1] Update `config/SecurityConfig.java` â€” add `.requestMatchers(HttpMethod.GET, "/api/v1/resources/**").permitAll()` before `anyRequest().authenticated()` (gating handled in `CatalogService`)
- [X] T014 [US1] Update `config/GlobalExceptionHandler.java` â€” map `ResourceNotFoundException` â†’ 404 `RESOURCE_NOT_FOUND`, `ResourceLockedException` â†’ 403 `RESOURCE_LOCKED` per `contracts/api.md`
- [X] T015 [P] [US1] Backend unit test `CatalogServiceTest` in `resources/CatalogServiceTest.java` â€” free/paid split, `owned`/`locked` flags for entitled vs anonymous, bundle `owned` only when all members owned, content gating returns FREE assets but throws for unowned PAID

### Frontend

- [X] T016 [P] [US1] Create API client `front-end/src/lib/resources.ts` â€” `getCatalog()`, `getResource(slug)`, `getResourceContent(slug)` with types (`Pricing`, `ResourceCard`, `BundleCard`, `ResourceDetail`, `CatalogResponse`, `ContentResponse`, `ApiError`), `fetch` with `credentials: "include"` mirroring `lib/scheduling.ts`, plus a `formatEur(cents)` helper using `Intl.NumberFormat("es-ES", { style: "currency", currency: "EUR" })`
- [X] T017 [US1] Rename route `front-end/src/routes/clases.tsx` â†’ `front-end/src/routes/recursos.tsx` (update `createFileRoute("/recursos")`, Spanish head/meta) and update the single `nav` array in `front-end/src/components/SiteHeader.tsx` (label `Clases`â†’`Recursos`, `to` `/clases`â†’`/recursos`) â€” covers desktop + mobile (FR-001); let TanStack regenerate `routeTree.gen.ts` (do not hand-edit)
- [X] T018 [P] [US1] Create `ResourceCard` in `front-end/src/components/resources/ResourceCard.tsx` â€” title, description, level badge, price via `formatEur` or "Gratis", lock/owned indicator, action button; emits `onOpen`/`onBuy`
- [X] T019 [P] [US1] Create `BundleCard` in `front-end/src/components/resources/BundleCard.tsx` â€” title, member list, combined price, owned/buy action
- [X] T020 [P] [US1] Create `ResourceDetailDialog` in `front-end/src/components/resources/ResourceDetailDialog.tsx` (Shadcn Dialog) â€” show `previewText`; if FREE or owned, fetch `getResourceContent` and render FILE links + EMBED media; else show lock state + buy CTA
- [X] T021 [US1] Create `FreeResourcesSection` in `front-end/src/components/resources/FreeResourcesSection.tsx` â€” render free `ResourceCard`s in a visually distinct section (depends on T018)
- [X] T022 [US1] Create `ResourcesView` in `front-end/src/components/resources/ResourcesView.tsx` â€” fetch `getCatalog()`, render `FreeResourcesSection` + paid `ResourceCard` grid + `BundleCard`s, loading state, and the empty/"prÃ³ximamente" state when all arrays empty (FR-013); open `ResourceDetailDialog` on card open (depends on T016, T018, T019, T020, T021)
- [X] T023 [US1] Render `ResourcesView` in `front-end/src/routes/recursos.tsx` (publicly viewable; keep head/meta) (depends on T022)

**Checkpoint**: Recursos tab live; catalogue + free materials fully browsable logged-out; paid items show locked. US1 independently demoable.

---

## Phase 4: User Story 2 â€” Unlock a paid resource by purchasing it (Priority: P2)

**Goal**: A logged-in customer buys a paid resource or bundle through a placeholder checkout that immediately and permanently grants ownership; ownership persists per account and dedups across bundles; the customer sees purchase history with a printable receipt. Unauthenticated buyers are routed to sign-in.

**Independent Test**: Signed in, buy a locked paid resource â†’ it flips to owned/unlocked, persists across reload/sessions, content endpoint now 200; second purchase â†’ 409; buy a bundle overlapping an owned resource â†’ all members unlocked with no duplicate; receipt + history visible (quickstart Scenarios C, D, E).

### Backend â€” payment abstraction & persistence

- [X] T024 [P] [US2] Create `PaymentProvider` interface + `PaymentResult` record in `resources/payment/PaymentProvider.java` â€” `authorize(long amountCents, String currency, String reference) â†’ PaymentResult(approved, providerReference)`
- [X] T025 [P] [US2] Create `PlaceholderPaymentProvider` in `resources/payment/PlaceholderPaymentProvider.java` â€” always approves, returns a synthetic reference, logs a WARN that no real payment is taken
- [X] T026 [US2] Create `PaymentProviderConfig` `@Configuration` factory in `resources/payment/PaymentProviderConfig.java` â€” returns `PlaceholderPaymentProvider` (runtime-selectable later; do NOT use `@ConditionalOnProperty`, per the `MeetingProvider` note in CLAUDE.md) (depends on T024, T025)
- [X] T027 [P] [US2] Create `AlreadyOwnedException` and `NotPurchasableException` in `resources/exception/`
- [X] T028 [US2] Create `PurchaseRepository` in `resources/repository/PurchaseRepository.java` â€” `insertPurchase(Purchase)` returning generated id + `receipt_reference` (sequence-style human reference e.g. `REC-2026-NNNNNN`), `insertEntitlements(userId, resourceIds, purchaseId)` via `INSERT ... ON CONFLICT (user_id, resource_id) DO NOTHING`, `findByUserId(UUID)`, `findByIdAndUserId(UUID id, UUID userId)`, and a receipt projection query (depends on T006, T007)

### Backend â€” service & endpoints

- [X] T029 [P] [US2] Create record DTOs in `resources/dto/` â€” `PurchaseRequest` (itemType, slug; bean-validated), `PurchaseResponse`, `MyPurchasesResponse` + `PurchaseSummary`, `ReceiptResponse` â€” per `contracts/api.md` Â§4â€“Â§6
- [X] T030 [US2] Implement `PurchaseService` in `resources/service/PurchaseService.java` â€” resolve resource/bundle by slug (`ResourceNotFoundException`), reject FREE (`NotPurchasableException`), reject fully-owned (`AlreadyOwnedException`), expand bundle members, call `PaymentProvider.authorize`, then record one `purchases` row + entitlements (on-conflict dedup) in a transaction; return granted resource slugs; plus `listPurchases(userId)` (depends on T008, T024, T026, T027, T028)
- [X] T031 [US2] Implement `ReceiptService` in `resources/service/ReceiptService.java` â€” build `ReceiptResponse` from a purchase owned by the caller (line items from resource/bundle members, buyer email, EUR amount) (depends on T028)
- [X] T032 [US2] Implement `PurchaseController` in `resources/controller/PurchaseController.java` â€” `POST /api/v1/purchases` (201), `GET /api/v1/purchases`, `GET /api/v1/purchases/{id}/receipt`; read authenticated user id from security context (depends on T029, T030, T031)
- [X] T033 [US2] Update `config/GlobalExceptionHandler.java` â€” map `AlreadyOwnedException` â†’ 409 `ALREADY_OWNED`, `NotPurchasableException` â†’ 422 `NOT_PURCHASABLE`
- [X] T034 [P] [US2] Backend unit test `PurchaseServiceTest` in `resources/PurchaseServiceTest.java` â€” placeholder grant creates purchase + entitlement, second purchase â†’ AlreadyOwned, FREE â†’ NotPurchasable, bundle with one pre-owned member unlocks all with no duplicate entitlement
- [X] T035 [P] [US2] Backend integration test `ResourcesControllerIntegrationTest` in `resources/ResourcesControllerIntegrationTest.java` â€” public catalogue without cookie, gated content `403`, authenticated purchase â†’ content `200` afterwards, receipt fetch scoped to owner

### Frontend

- [X] T036 [US2] Extend `front-end/src/lib/resources.ts` â€” add `purchase(itemType, slug)`, `listPurchases()`, `getReceipt(id)` with `PurchaseResponse`/`MyPurchasesResponse`/`ReceiptResponse` types
- [X] T037 [US2] Create `PurchaseDialog` in `front-end/src/components/resources/PurchaseDialog.tsx` (Shadcn Dialog) â€” show the "no se realiza ningÃºn pago real" placeholder notice + price; on confirm call `purchase()`; if not authenticated (401), route to `/cuenta` and return to the purchase afterward (FR-009) (depends on T036)
- [X] T038 [US2] Wire buy actions: `ResourceCard`, `BundleCard`, and `ResourceDetailDialog` open `PurchaseDialog`; on success refresh the catalogue and "Mis recursos" via shared refresh refs in `recursos.tsx` (the two-ref pattern from `reservas.tsx`) so lockedâ†’owned flips without reload (depends on T037)
- [X] T039 [P] [US2] Create `MyPurchases` in `front-end/src/components/resources/MyPurchases.tsx` â€” list owned resources + purchase history (date, amount via `formatEur`), and a printable receipt view (render `getReceipt` data; `window.print()` to save as PDF)
- [X] T040 [US2] In `front-end/src/routes/recursos.tsx`, render `MyPurchases` when authenticated and wire the refresh refs shared with `ResourcesView` (depends on T038, T039)

**Checkpoint**: Full purchaseâ†’unlockâ†’historyâ†’receipt flow works on top of US1; bundle dedup verified. US2 independently demoable.

---

## Phase 5: User Story 3 â€” Use free previews to drive conversion (Priority: P3)

**Goal**: Free resources and paid-resource previews present clear CTAs to unlock the related paid resource and to book a 1-on-1 class, turning the free section into a conversion funnel.

**Independent Test**: Open a free resource/preview â†’ see a "Desbloquear el recurso completo" CTA (opens the purchase flow for the related paid resource) and a "Reservar una clase 1-on-1" CTA linking to `/reservas` (quickstart Scenario B.2â€“3).

- [X] T041 [US3] Add conversion CTAs in `front-end/src/components/resources/FreeResourcesSection.tsx` and `ResourceDetailDialog.tsx` â€” a "Reservar una clase 1-on-1" link to `/reservas` and, when `relatedResourceSlug` is present, a "Desbloquear el recurso completo" button that opens `PurchaseDialog` for that paid resource (FR-012; depends on T021, T037)
- [X] T042 [US3] Surface the unlock target end-to-end: confirm `relatedResourceSlug` is included in `ResourceCardDto`/`ResourceDetailDto` (T010) and drive the CTA target from it in the free section / detail dialog; verify a paid resource's own preview also offers the unlock CTA

**Checkpoint**: Free previews convert to paid purchases and bookings. All three stories independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, verification, and cleanup spanning the stories.

- [X] T043 [P] Update `CLAUDE.md` â€” "Current pages" table (`/clases` placeholder â†’ `/recursos` âœ… Live with description), the front-end project-structure notes (routes/components/lib for resources), and the "Planned future phases" entry that referenced `/clases`
- [X] T044 [P] Confirm `front-end/src/routeTree.gen.ts` regenerated by the dev server (no `/clases` route remains) and remove any leftover references to the old route
- [X] T045 Run the `quickstart.md` validation scenarios Aâ€“E end-to-end (browser + curl), including the empty-state check and bundle overlap dedup
- [X] T046 [P] Browser-verify per the constitution: nav + catalogue render correctly on desktop and in the mobile `Sheet`; Spanish copy and `es-ES` EUR formatting throughout (FR-014)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies â€” start immediately.
- **Foundational (Phase 2)**: Depends on Setup â€” **BLOCKS all user stories** (shared schema + read repository).
- **User Stories (Phase 3â€“5)**: All depend on Foundational. US2 builds on US1's catalogue UI/DTOs; US3 builds on US1's free section and US2's `PurchaseDialog`. Recommended order P1 â†’ P2 â†’ P3.
- **Polish (Phase 6)**: After the desired stories are complete.

### User Story Dependencies

- **US1 (P1)**: Only Foundational. Fully independent and demoable (catalogue + free materials).
- **US2 (P2)**: Foundational + reuses US1 cards/DTOs and the `recursos.tsx` route. The purchase backend (payment provider, purchase repo/service/controller) is otherwise self-contained.
- **US3 (P3)**: Reuses US1's `FreeResourcesSection`/`ResourceDetailDialog` and US2's `PurchaseDialog` (the unlock CTA opens the purchase flow). Thin UI layer.

### Within Each User Story

- Models â†’ repository â†’ service â†’ controller â†’ security/handler wiring (backend).
- API client â†’ presentational components â†’ orchestrator view â†’ route (frontend).
- Backend tests can be written alongside their service/controller.

### Parallel Opportunities

- Foundational model POJOs T003â€“T007 are all `[P]` (different files).
- US1: backend `[P]` T009, T010, T015 and frontend `[P]` T016, T018, T019, T020 can run in parallel; T017 (route/nav rename) is independent of the catalogue components.
- US2: T024, T025, T027, T029, T034, T035, T039 are `[P]`.
- Polish: T043, T044, T046 are `[P]`.
- With multiple developers, once Foundational is done one can take the US1 backend while another takes the US1 frontend.

---

## Parallel Example: User Story 1

```bash
# Backend, in parallel (different files):
Task: "Create ResourceNotFoundException + ResourceLockedException in resources/exception/"   # T009
Task: "Create catalogue DTOs in resources/dto/"                                                # T010
Task: "CatalogServiceTest in resources/CatalogServiceTest.java"                                # T015

# Frontend, in parallel (different files):
Task: "Create lib/resources.ts API client + formatEur"                                         # T016
Task: "Create ResourceCard component"                                                          # T018
Task: "Create BundleCard component"                                                            # T019
Task: "Create ResourceDetailDialog component"                                                  # T020
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup â†’ Phase 2: Foundational (migration + seed + read repo).
2. Phase 3: US1 â€” rename tab, public catalogue, free materials, lock indicators, empty state.
3. **STOP and VALIDATE**: quickstart Scenarios A & B.1. Deploy/demo the renamed Recursos catalogue as the MVP.

### Incremental Delivery

1. Setup + Foundational â†’ foundation ready.
2. US1 â†’ browsable catalogue (MVP) â†’ demo.
3. US2 â†’ purchase/unlock + history/receipts â†’ demo.
4. US3 â†’ free-preview conversion CTAs â†’ demo.
5. Polish â†’ docs, verification, cleanup.

---

## Notes

- `[P]` = different files, no incomplete-task dependencies.
- This release's checkout is a **placeholder** (no real money); the `PaymentProvider` seam lets a real processor replace `PlaceholderPaymentProvider` later without touching the catalogue/ownership model.
- Protected materials are placeholder strings until the future backoffice authors them; the entitlement gate (`/content` 403) is the durable enforcement point.
- `routeTree.gen.ts` is auto-generated â€” never hand-edit (CLAUDE.md).
- Commit after each task or logical group; stop at any checkpoint to validate a story independently.

