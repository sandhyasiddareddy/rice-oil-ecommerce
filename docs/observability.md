# Observability (Phase 1 Baseline)

## Implemented baseline
- Spring Boot Actuator endpoints enabled for health, info, and metrics exposure.
- Correlation ID filter adds X-Correlation-Id for request traceability.
- Centralized API error envelope includes correlation ID.

## Customer journey logging
Every log line carries `[corr:<correlationId>] [cust:<customerId>] [sid:<sessionId>]`. `sessionId` is the JWT id issued at
login/register, so one `grep sid:<id>` returns the whole session from login to logout. Business events use the `business`
logger in `key=value` form (see `BusinessLog`).

| Stage | Events |
|---|---|
| Session | `CUSTOMER_REGISTERED`, `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT` (POST `/api/auth/logout`, called by the storefront Sign out) |
| Cart | `CART_ITEM_ADDED` / `CART_ITEM_UPDATED` / `CART_ITEM_REMOVED` (cartId, sku, quantity, unitPrice, lineTotal) |
| Order | `ORDER_PLACED` (orderNumber, totals, coupons), one `ORDER_ITEM` per line (sku, quantity, unitPrice), `STOCK_RESERVED`, `ORDER_PLACE_REPLAYED`, `ORDER_EXPIRED` |
| Payment | `RAZORPAY_ORDER_CREATED` / `RAZORPAY_ORDER_REUSED` (razorpayOrderId), `PAYMENT_VERIFIED` (razorpayPaymentId, source=checkout_callback or webhook), `PAYMENT_VERIFY_REJECTED`, `ORDER_PAID`, `ORDER_CONFIRMATION_EMAIL_SENT/FAILED` |
| Webhooks | `WEBHOOK_RECEIVED`, `WEBHOOK_DUPLICATE_IGNORED`, `WEBHOOK_REJECTED`, `WEBHOOK_PAYMENT_MISMATCH`, `REFUND_STATUS_UPDATED` |
| Cancel / refund | `CANCELLATION_REQUESTED`, `REFUND_REQUESTED`, `REFUND_APPROVED`, `REFUND_SENT_TO_RAZORPAY` (razorpayRefundId), `REFUND_PROVIDER_CALL_FAILED`, `REFUND_REJECTED`, `CANCELLATION_APPROVED`, `ORDER_CANCELLED`, `LATE_PAYMENT_REFUND_*` |
| HTTP | `HTTP_REQUEST` (method, path, status, durationMs; no query string) and `REQUEST_REJECTED` (status, code, exception, reason for every handled 4xx) |

Trace an order: `grep "ORD-20260101..."` for the order lifecycle, then take its `sid:`/`corr:` values to see the surrounding session.

## Planned in later phases
- OpenTelemetry traces and Application Insights export.
- Payment/shipping/refund/import operational metrics and dashboards.
- Alert rules for stuck payments, refunds, shipment delays, and retry failures.

## Constraints
- Do not log secrets or sensitive payment details (passwords, tokens, Razorpay signatures/secrets).
- Keep customer PII out of routine logs: emails are masked (`j***@example.com`), addresses are never logged.
- Razorpay order/payment/refund ids are references, not secrets, and are logged on purpose.
