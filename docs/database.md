# Database and Data Model Notes (Phase 0 Updated)

## Core data modeling principles
- Use relational constraints and foreign keys for integrity.
- Use precise decimal types for all INR money values.
- Persist immutable order-item and tax snapshots at order time.
- Keep external provider references and idempotency keys unique.

## Tax and pricing persistence
- Store regular catalog price and calculated cart/order amounts using decimal columns.
- Persist tax-inclusive display values and computed tax breakdown details in order and invoice records.
- Keep tax-rate and classification rules configurable through policy/config tables, not source code constants.

## Shipping quote lifecycle persistence
- Record quote request context (weight, destination, service intent).
- Persist quote state: AVAILABLE, UNAVAILABLE, EXPIRED, FAILED.
- Persist quote expiry timestamp and provider reference when available.
- Prevent checkout from using expired quotes.

## Cancellation and return policy persistence
- Policy inputs are configurable (effective dates, cutoffs, eligibility windows).
- Order/refund workflows read policy from configuration, not hard-coded branching.

## Guest checkout and secure order access
- Support guest order identity tokens or signed access links with expiry.
- Guest and authenticated order access must be auditable.

## Suggested additional entities/tables
- tax_rule (rate, category mapping, effective window, active flag)
- tax_classification (product/category mapping)
- shipping_quote (state, amount, expiry, destination hash, service)
- policy_config (typed key-value with effective windows)
- guest_order_access (token hash, order_id, expiry, attempts)

## Existing required entities retained
- category, product, product_variant, product_image
- inventory_stock, inventory_adjustment, inventory_reservation
- customer, address, cart, cart_item
- order, order_item, order_status_history
- payment_attempt, payment_webhook_event
- refund_request, refund_status_history
- shipment, shipment_tracking_event
- promotion, coupon, coupon_redemption
- import_job, import_row_result, import_audit_log
- notification_attempt
- outbox_event, outbox_delivery_log
