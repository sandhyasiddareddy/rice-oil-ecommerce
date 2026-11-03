# Payment Flows (Phase 1 Baseline)

## Current scope
- Payment flow remains scaffolded: provider order creation is not yet connected to Razorpay's API.
- Payment creation and verification require an authenticated owner of the order.
- Verification fails closed unless `RAZORPAY_KEY_SECRET` is configured; webhooks require a valid raw-body signature using `RAZORPAY_WEBHOOK_SECRET`.
- Architecture decision preserved: backend-authoritative payment amount and durable, idempotent webhook processing.

## Planned model
- Payment states separate from order and shipment states.
- Razorpay signature verification on server side.
- Durable webhook ingestion with duplicate detection.
- Reconciliation process for delayed or out-of-order events.

## Constraints
- Browser redirect is not proof of payment success.
- Never store raw card data or sensitive credentials.
