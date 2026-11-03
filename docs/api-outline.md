# API Outline (Phase 0 Updated)

## Public storefront APIs
- GET /api/catalog/categories
- GET /api/catalog/products
- GET /api/catalog/products/{slugOrSku}
- GET /api/catalog/search

## Cart and checkout APIs
- POST /api/carts
- GET /api/carts/{cartId}
- POST /api/carts/{cartId}/items
- PATCH /api/carts/{cartId}/items/{itemId}
- DELETE /api/carts/{cartId}/items/{itemId}
- POST /api/carts/{cartId}/pricing/refresh
- POST /api/checkout/quote
- POST /api/checkout/orders (idempotency key required)

Current implemented slice:
- POST /api/carts creates a persisted active cart.
- GET /api/carts/{cartId} returns persisted cart and items.
- POST /api/carts/{cartId}/items appends cart items with stored line totals.
- PATCH /api/carts/{cartId}/items/{itemId} updates unit price/quantity and recalculates line total.
- DELETE /api/carts/{cartId}/items/{itemId} removes an item from a cart.
- POST /api/carts/{cartId}/pricing/refresh recomputes totals via pricing and coupon services.
- POST /api/checkout/quote is implemented as a backend quote-composition endpoint using pricing and coupon services.
- POST /api/checkout/coupons/apply is implemented for public coupon validation/apply preview.
- POST /api/checkout/orders is implemented with required `Idempotency-Key` header, creating an order from persisted cart totals.
- Promotion engine supports stackable vs non-stackable behavior, exclusion groups, and usage limits.
- POST /api/payments/razorpay/create-order requires the authenticated order owner and moves status to `PAYMENT_PENDING`.
- Quote response currently includes base amount, tax amount, pre-discount total, applied coupon, discount amount, and final amount.

## Shipping quote and checkout behavior
- POST /api/shipping/quotes
- GET /api/shipping/quotes/{quoteId}
- Quote responses must distinguish AVAILABLE, UNAVAILABLE, EXPIRED, FAILED.
- Checkout requires a valid, non-expired quote or explicit free-shipping policy outcome.

Current implemented slice:
- POST /api/shipping/quotes verifies the authenticated customer or guest cart access token and creates a quote linked to the cart with expiry and provider metadata.
- GET /api/shipping/quotes/{quoteCode} verifies the authenticated customer or guest cart access token and automatically returns EXPIRED state when overdue.

## Authentication and customer APIs
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/guest/upgrade
- GET /api/customers/me
- GET /api/customers/me/orders
- GET /api/customers/me/orders/{orderId}

## Guest order access APIs
- GET /api/guest/orders/{orderId} (requires guest token)
- GET /api/guest/orders/{orderId}/tracking (requires guest token)

Current implemented slice:
- Guest access tokens are returned at guest checkout and validated via hashed token persistence.
- Guest order status and tracking status endpoints validate `X-Guest-Token` against order and expiry.

## Payment APIs
- POST /api/payments/razorpay/create-order
- POST /api/payments/razorpay/verify
- POST /api/payments/razorpay/webhook

Current implemented slice:
- Payment create/verify endpoints require the authenticated order owner or a valid guest-order access token.
- Verify checks the Razorpay order/payment signature using `RAZORPAY_KEY_SECRET` and keeps the gateway order ID separate from the payment ID.
- `POST /api/payments/razorpay/webhook` validates `X-Razorpay-Signature` against the raw body using `RAZORPAY_WEBHOOK_SECRET`, persists event payloads with event-level idempotency, and maps captured payments by gateway order ID.
- Provider order creation remains scaffolded and is not yet connected to Razorpay's API.

## Shipping APIs
- POST /api/shipments
- GET /api/shipments/{shipmentId}
- GET /api/shipments/{shipmentId}/tracking
- POST /api/webhooks/shiprocket

## Refund and cancellation APIs
- POST /api/orders/{orderId}/cancellation-requests
- POST /api/orders/{orderId}/refund-requests
- GET /api/orders/{orderId}/refunds

Current implemented slice:
- Cancellation requests are persisted with REQUESTED status.
- Refund requests are persisted with amount validation and REQUESTED status.
- Refund history endpoint returns order-scoped refund request timeline.

## Admin APIs
- /api/admin/catalog/*
- /api/admin/inventory/*
- /api/admin/promotions/*
- /api/admin/imports/*
- /api/admin/operations/*

## Policy and configuration APIs (internal/admin)
- GET /api/admin/policies
- PUT /api/admin/policies/{policyKey}
- GET /api/admin/tax-rules
- PUT /api/admin/tax-rules/{id}

## Error and traceability requirements
- Consistent error envelope with correlation ID.
- Never expose internal stack traces to customers.
- Log operation IDs and provider references safely.
