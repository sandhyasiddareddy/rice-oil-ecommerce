# Architecture (Phase 0 Updated)

## Architecture style
- Modular monolith (single deployable backend) with strict internal module boundaries.
- Next.js frontend for public storefront and admin UI.
- Spring Boot backend as authoritative source for pricing, stock, order, payment, shipping, and refund states.

## Core modules and responsibilities

### catalog
- Categories, products, variants, package sizes, and product images.
- Publication state and search metadata.

### inventory
- Available stock, reservations, and stock adjustments.
- Concurrency-safe reservation and release.

### pricing
- Regular price retrieval and total calculation.
- Tax-inclusive display support with configurable tax rules.
- Produces tax breakdown snapshots for order/invoice persistence.

### promotions
- Automatic discounts and coupon validation.
- Best-eligible-discount-only evaluation (no stacking).

### cart
- Cart lifecycle and line-item composition.
- Server-side reprice trigger and stock checks.

### orders
- Order creation with idempotency key.
- Order lifecycle state machine and order-item snapshots.

### payments
- Razorpay order/payment initiation, verification, and webhooks.
- Durable webhook records and idempotent processing.
- Payment reconciliation and refund initiation tracking.

### shipping
- Provider-agnostic shipping service interface.
- Shiprocket adapter implementation.
- Quote lifecycle: available, unavailable, expired, failed.
- Shipment booking/tracking/cancellation orchestration.

### customers
- Customer profile, addresses, authentication.
- Guest checkout support and optional account creation.
- Secure guest-order access model.

### notifications
- Email notification pipeline for launch.
- Delivery attempts and outcome recording.
- SMS/WhatsApp explicitly deferred.

### admin
- Secured admin APIs for catalog, inventory, promotions, imports, and operations.
- Role-based authorization and audit trails.

### imports
- Excel template, upload validation, preview, confirm, and error reporting.

### operations
- Health/readiness, observability views, reconciliation support, and incident diagnostics.

## Module interaction principles
- Modules expose application services, not repositories.
- Direct synchronous calls are default for simple dependencies.
- Durable events (outbox-style) are used for critical post-commit workflows.

## External integration boundaries
- Razorpay, Shiprocket, and email provider integrations are isolated in module adapters.
- Credentials remain server-side and externally managed (e.g., Key Vault in production).

## Key architecture decisions applied
- Backend authoritative pricing and checkout totals.
- Durable, idempotent webhook processing.
- Guest checkout with secure access to guest orders.
- Configurable policy-driven tax/shipping/cancellation/returns behavior.

## Phase 1 policy constraint
- Phase 1 implementation must not hard-code unresolved legal/business policies.
- Configuration-driven placeholders are required where policy is pending.
