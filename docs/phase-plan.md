# Phased Implementation Plan (Phase 0 Updated)

## Current execution status
- Phase 1 scaffolding has been created in backend/frontend/CI/docs.
- Phase 2 catalog, inventory, and import backend slice has been implemented.
- Phase 3 initial backend scaffold for pricing and promotions has been added.
- Local build and test execution has been run using portable toolchains in `.tools`.
- Backend verification command executed successfully: `backend/mvnw.cmd --batch-mode verify`.
- Frontend verification commands executed successfully: `npm run lint`, `npm run typecheck`, `npm run build`.
- CI workflow remains configured to repeat these checks in GitHub Actions.

## Phase 1 - Foundation
Objective:
- Establish backend/frontend project structure, CI, migrations, health checks, logging, and security baseline.

Policy constraint:
- No hard-coded tax, cancellation, return, or shipping policy assumptions.
- Add configuration mechanism skeletons required by later phases.

Deliverables:
- Build and test pipeline baseline.
- Flyway migration baseline and core schema scaffolding.
- Module package structure and architecture tests for boundary enforcement.
- Structured logging, correlation IDs, actuator readiness/liveness.

## Phase 2 - Catalog, Inventory, Imports
Objective:
- Implement categories/products/variants/images, stock/reservations, and Excel import with preview/confirm/error report.

Policy constraint:
- Product publish/import behavior remains explicit and non-destructive.
- No automatic policy assumptions for tax classes; use configurable mapping.

Current increment status:
- Implemented first Phase 2 slice for catalog and inventory foundations.
- Added schema migrations for category, product, variant, image, stock, adjustments, and reservations.
- Added admin APIs for category/product/variant creation and inventory adjustments.
- Added service-level duplicate and invariant validation with explicit exceptions.
- Implemented Excel import workflow with template endpoint, preview validation persistence, explicit confirm apply, row-level errors, and audit logs.
- Added migration and domain model for import jobs, row results, and audit logs.
- Extended import unit tests for preview idempotency, validation outcomes, confirm safety checks, duplicate prevention, and error CSV output.
- Backend verification completed successfully with `mvn verify` after import implementation.
- Remaining Phase 2 gaps:
	- Image storage integration is still local URL-field based (no object storage pipeline yet).
	- Promotion/coupon import is intentionally excluded and must remain a separate authorized flow.
	- Admin auth hardening beyond baseline security is pending later phases.

## Phase 3 - Pricing and Promotions
Objective:
- Implement regular pricing, tax-inclusive display support, best-eligible-discount-only logic, coupon validation.

Policy constraint:
- Tax engine uses configurable rules and effective dates.
- Do not encode fixed legal/tax assumptions in code.

Current increment status:
- Added Flyway migration for `tax_rule` and `promotion` tables.
- Added pricing quote service scaffold for tax-inclusive/exclusive computations using configurable tax rules.
- Added coupon validation scaffold that selects the best eligible discount from provided codes.
- Added admin promotions create endpoint scaffold under `/api/admin/promotions`.
- Added checkout quote composition endpoint under `/api/checkout/quote` wired to pricing and coupon services.
- Added persisted cart APIs for create/get/add-item and pricing-refresh (`/api/carts/*`).
- Added checkout order placement endpoint (`/api/checkout/orders`) with idempotency-key support and order persistence.
- Added cart item update/delete support (`PATCH/DELETE /api/carts/{cartId}/items/{itemId}`).
- Added public coupon apply endpoint (`POST /api/checkout/coupons/apply`) with coupon outcome payload.
- Added promotion constraints (stackable, exclusion group, total and customer redemption limits) with redemption persistence.
- Added payment-order linkage scaffold (`POST /api/payments/razorpay/create-order`) that links provider order reference and transitions order to `PAYMENT_PENDING`.
- Added shipping quote API scaffold (`POST/GET /api/shipping/quotes`) with expiry and status modeling.
- Added guest order access token flow (`POST /api/guest/orders/access`, `GET /api/guest/orders/{orderId}`, `GET /api/guest/orders/{orderId}/tracking`).
- Added cancellation/refund request APIs (`POST /api/orders/{orderId}/cancellation-requests`, `POST /api/orders/{orderId}/refund-requests`, `GET /api/orders/{orderId}/refunds`).
- Added Razorpay verification and webhook ingestion scaffolding with persisted webhook event idempotency.
- Added unit tests for pricing quote and coupon validation service behavior.
- Added controller and service tests for checkout quote and cart pricing refresh behavior.
- Added service and controller tests for idempotent checkout order creation.
- Remaining Phase 3 gaps:
- Full provider-grade signature validation and asynchronous retry/reconciliation workers remain pending hardening.

## Phase 4 - Shopping Experience
Objective:
- Mobile-first catalog UX, cart, checkout quote lifecycle, guest checkout with optional account creation.

Policy constraint:
- Guest access security and auditability required.

## Phase 5 - Orders and Payments
Objective:
- Order state machine, Razorpay integration, webhook durability, reconciliation and recovery.

Policy constraint:
- Preserve idempotent processing and no trust in browser redirect success.

Current increment status:
- Added initial verification endpoint and webhook event persistence with idempotent event ingestion.
- Added order payment status transition hooks for verified payment capture.
- Remaining: provider-grade signature validation secret management, retry scheduler, and reconciliation jobs.

## Phase 6 - Shipping and After-sales
Objective:
- Shiprocket integration via shipping interface, tracking updates, configurable cancellation/refund/return workflow enforcement.

Policy constraint:
- Use policy configuration for cutoffs/timelines; no universal hard-coded rule.

Current increment status:
- Added shipping quote API and base domain model for shipment readiness.
- Added cancellation and refund request API persistence model with REQUESTED lifecycle entry.
- Remaining: provider integration, async status updates, and policy-driven approval workflows.

## Phase 7 - Production Readiness
Objective:
- Performance, security, accessibility, observability dashboards/alerts, backup-restore verification, runbook validation.

Policy constraint:
- Confirm legal/compliance/business policy sign-off before production launch.
