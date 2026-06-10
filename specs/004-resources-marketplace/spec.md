# Feature Specification: Recursos — Paid & Free Teaching Resources

**Feature Branch**: `004-resources-marketplace`

**Created**: 2026-06-09

**Status**: Draft

**Input**: User description: "I want to rename the tab called \"Clases\" to \"Resources\" (in Spanish) which will contain some Spanish-teaching resources for other teachers made by the owner. The actual materials will be made once the backoffice is created. In order to access each resource, the customers will have to pay and unlock them. I would like to also have a section for \"free\" materials which should be like a preview or something very basic that will encourage customers to either pay for a resource or for 1-1 classes."

## Clarifications

### Session 2026-06-09

- Q: In this release's placeholder purchase flow, what happens when a customer confirms a purchase? → A: Auto-grant on confirm — confirming shows a "coming soon / no real payment" notice, then immediately records ownership and unlocks.
- Q: Should free resources / previews be viewable without an account? → A: Fully public, no login required.
- Q: What form will the protected resource materials take? → A: Mixed — downloadable files and/or embedded media (video/links).
- Q: Which currency should resource prices be shown and recorded in? → A: Euro (EUR).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse the Recursos catalogue (Priority: P1)

A visitor opens the renamed **Recursos** tab and sees a catalogue of Spanish-teaching resources created by Paula, aimed at fellow Spanish teachers. Each resource shows a title, short description, level/topic, a cover or icon, and a clear indication of whether it is free or paid (with its price). Free resources are visually separated into their own section so visitors immediately have something of value to engage with.

**Why this priority**: Renaming the tab and presenting the catalogue is the foundation of the whole feature — without a browsable list, nothing else (unlocking, paying, previewing) has a place to live. It also delivers immediate value as a marketing surface even before any purchase flow works.

**Independent Test**: Navigate to the Recursos tab from the site navigation (desktop and mobile) and confirm the page renders a separated "free" section and a "paid" catalogue with the correct metadata and locked/unlocked indicators, using placeholder resources.

**Acceptance Scenarios**:

1. **Given** the site navigation, **When** a visitor looks at the menu, **Then** the former "Clases" entry is labelled "Recursos" and routes to the resources page on both desktop and mobile.
2. **Given** the Recursos page, **When** it loads, **Then** free resources appear in a distinct "free / preview" section and paid resources appear in the main catalogue, each showing title, description, level/topic, and price (or "Gratis" for free items).
3. **Given** a paid resource the visitor has not purchased, **When** they view its card, **Then** it is clearly marked as locked and its full materials are not accessible.

---

### User Story 2 - Unlock a paid resource by purchasing it (Priority: P2)

A logged-in customer (a fellow teacher) decides to buy a paid resource or a bundle. They select it, are taken through a purchase/checkout flow (a placeholder "coming soon" payment step in this release), confirm, and the resource(s) become permanently unlocked in their account. Returning later, the item shows as owned and its materials are accessible without paying again, and the purchase appears in their history with a receipt.

**Why this priority**: Monetisation is the core business goal, but it depends on the catalogue (P1) existing first. It is the second slice because a viable MVP can ship with the catalogue and free previews while the purchase flow is finalised.

**Independent Test**: As a logged-in customer, purchase a paid resource end-to-end and confirm it transitions from locked to permanently owned, persists across sessions, and cannot be purchased twice.

**Acceptance Scenarios**:

1. **Given** a logged-in customer viewing a locked paid resource, **When** they choose to buy it and complete payment successfully, **Then** the resource becomes unlocked and its materials become accessible to them.
2. **Given** a customer who already owns a resource, **When** they revisit the catalogue, **Then** the resource shows as "owned/unlocked" and offers access rather than a purchase action.
3. **Given** a visitor who is not logged in, **When** they attempt to purchase a paid resource, **Then** they are prompted to sign in or create an account before the purchase can proceed.
4. **Given** a payment that fails or is abandoned, **When** the customer returns to the resource, **Then** it remains locked and no charge is recorded.

---

### User Story 3 - Use free previews to drive conversion (Priority: P3)

A visitor explores a free resource (or the free preview portion of a paid resource) and, encouraged by its quality, is presented with clear calls to action to either buy the related paid resource or book a 1-on-1 class.

**Why this priority**: The free section is the conversion engine, but it only matters once a catalogue and a way to buy/book exist. It refines the funnel rather than enabling it.

**Independent Test**: Open a free resource/preview and confirm it presents working calls to action that lead to the relevant paid resource and to the booking flow on `/reservas`.

**Acceptance Scenarios**:

1. **Given** a free resource or preview, **When** a visitor finishes viewing it, **Then** they are shown calls to action to unlock a related paid resource and to book a 1-on-1 class.
2. **Given** a free preview attached to a paid resource, **When** the visitor selects "unlock the full resource", **Then** they are taken to the purchase flow for that paid resource.

---

### Edge Cases

- What happens when the catalogue has no resources yet (initial state before the backoffice exists)? The page must show a graceful empty/"coming soon" state rather than a broken layout.
- What happens when a customer is logged in on one device, buys a resource, then opens the catalogue on another device? Ownership must reflect on all their sessions.
- What happens if a visitor tries to deep-link directly to a paid resource's materials without owning it? Access must be denied and they are routed to the purchase/sign-in flow.
- What happens to ownership if a resource is later edited or its price changes? Existing owners retain access to what they purchased.
- How does the page behave for a logged-out visitor versus a logged-in one (free content visible to all; ownership state only meaningful when authenticated)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The site navigation MUST rename the former "Clases" entry to "Recursos" (Spanish for "Resources") on both desktop and mobile, and route it to the resources page, preserving active-link styling behaviour.
- **FR-002**: The Recursos page MUST present a catalogue of teaching resources, each displaying at minimum a title, short description, level/topic indicator, and pricing state (free vs paid with price). Paid prices MUST be shown and recorded in euros (EUR).
- **FR-003**: The page MUST present free resources (and/or previews) in a section visually distinct from the paid catalogue.
- **FR-004**: Each paid resource MUST clearly indicate whether the current viewer has it locked (not purchased) or unlocked (owned).
- **FR-005**: Free resources and previews MUST be fully public — viewable by any visitor without payment and without signing in or creating an account.
- **FR-006**: Customers MUST be able to initiate a purchase of a paid resource and, on successful payment, have that resource permanently unlocked for their account.
- **FR-007**: The system MUST persist resource ownership per customer account so that unlocked resources remain accessible across sessions and devices.
- **FR-008**: The system MUST prevent access to a paid resource's full materials — both downloadable files and embedded media (e.g. video/links) — for any viewer who does not own it, including via direct links to files or embedded content.
- **FR-009**: The system MUST require authentication before a purchase can be completed, prompting unauthenticated visitors to sign in or register and returning them to the purchase afterward.
- **FR-010**: The system MUST prevent a customer from purchasing the same resource more than once.
- **FR-011**: When a payment fails or is abandoned, the system MUST leave the resource locked and record no ownership.
- **FR-012**: Free resources and previews MUST present calls to action that direct visitors to unlock a related paid resource and to book a 1-on-1 class (linking to the existing booking flow on `/reservas`).
- **FR-013**: The Recursos page MUST display a graceful empty/"coming soon" state when no resources are available, since real materials will be authored later via a future backoffice.
- **FR-014**: All customer-facing copy on the Recursos page MUST be in Spanish, consistent with the rest of the site.
- **FR-015**: The system MUST record each completed purchase (which customer, which resource or bundle, when, amount in EUR) so purchases are auditable.
- **FR-016**: Customers MUST be able to view their own purchase history (each purchase with date and amount) and obtain a receipt for each completed purchase.
- **FR-017**: For this release, the purchase flow MUST be a placeholder ("coming soon"): when a logged-in customer confirms a purchase, the system MUST show a notice that no real payment is taken, then immediately record ownership and unlock the resource(s). No real money moves and no payment-instrument data is stored. The unlock, ownership, and access-control behaviour MUST be fully functional so that a real payment processor can be wired in later without reworking the catalogue or ownership model.
- **FR-018**: Paid resources MUST be sellable as one-time individual purchases granting permanent access, and the system MUST also support selling bundles (a named group of resources offered at a combined price) that unlock all included resources on purchase.
- **FR-019**: Purchasing a bundle MUST unlock every resource it contains; if a customer already owns some resources in a bundle, the system MUST handle the overlap gracefully (no duplicate ownership records, and access to all bundle resources is granted).

### Key Entities *(include if data involved)*

- **Resource**: A teaching material created by the owner. Attributes: title, description, level/topic, pricing state (free or paid), price in EUR (if paid), an optional free preview, and the protected material payload (added later via the backoffice). The material payload may be downloadable files (PDFs, printables, worksheets) and/or embedded media (e.g. video, links). Free resources may stand alone or act as previews tied to a paid resource.
- **Customer (User)**: An existing account holder (extends the current authentication system). Relevant here as the owner of purchased resources. Target audience for paid resources is fellow Spanish teachers.
- **Bundle**: A named group of resources offered together at a combined price. Attributes: title, description, member resources, bundle price. Purchasing a bundle unlocks all of its member resources.
- **Purchase / Entitlement**: The record that links a Customer to the Resource(s) they have unlocked, whether bought individually or via a bundle. Attributes: customer, purchased item (resource or bundle), resulting resource entitlements, purchase timestamp, amount (EUR), receipt reference. Grants permanent access; entitlement is unique per customer-resource pair.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of visitors reaching the site see "Recursos" (not "Clases") in the navigation, and the link opens the resources page on both desktop and mobile.
- **SC-002**: A visitor can locate and open a free resource within 30 seconds of landing on the Recursos page, without signing in.
- **SC-003**: A logged-in customer can complete a paid-resource purchase from selection to unlocked access in under 3 minutes.
- **SC-004**: 100% of purchased resources remain accessible to their owner across new sessions and devices, and 0% of paid materials are accessible to non-owners.
- **SC-005**: Every free resource/preview presents at least one working call to action to buy a related resource and one to book a 1-on-1 class.
- **SC-006**: The Recursos page renders a usable state (including a clear empty/"coming soon" message) even when zero resources have been authored.
- **SC-007**: A customer can view a complete history of their purchases, with a receipt available for each, and buying a bundle unlocks 100% of its member resources in a single transaction.

## Assumptions

- The Spanish translation of "Resources" is "Recursos"; this is the navigation label and page name.
- The resources page replaces the current `/clases` placeholder; the route may be renamed to reflect "recursos" while preserving navigation behaviour.
- The target customers for paid resources are other Spanish teachers, distinct from the Romanian-student audience served by the 1-on-1 classes; both audiences use the same site and account system.
- This feature delivers the customer-facing catalogue, free-preview section, and purchase/unlock journey. Authoring real resource content and managing the catalogue are handled by a future backoffice (out of scope here); the catalogue therefore ships with placeholder/empty content.
- Purchases grant permanent access (no rental/expiry).
- This release uses a placeholder purchase flow: confirming a purchase shows a "no real payment" notice and immediately grants ownership (no money moves). The ownership/access model is built to accept a real payment processor later without rework. Subscriptions are out of scope; one-time per-resource purchases and bundles are in scope.
- Prices and recorded purchase amounts are in euros (EUR); multi-currency is out of scope.
- Resource materials may be downloadable files and/or embedded media; the actual payloads are authored later via the future backoffice.
- Authentication and account management reuse the existing system (`/cuenta`, JWT cookie sessions); no new auth model is introduced.
- Refunds, discounts/coupons, tax/invoicing, and multi-currency are out of scope for this release.
- Booking calls to action link to the existing `/reservas` flow.
