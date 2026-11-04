# Payment, Refund, and Cancellation Flows

## Current scope
- Razorpay order creation, server-side payment verification, and signed webhook ingestion are implemented.
- `customer_order.payment_reference` stores the Razorpay order ID; `customer_order.payment_id` stores the captured payment ID.
- Customers can request partial or full refunds up to the remaining paid amount. The request is persisted before any provider action.
- Staff approve or reject refund requests from **Admin → Orders & refunds**. Approval calls Razorpay's payment refund endpoint; the Razorpay refund ID and status are stored in `refund_request`.
- Staff approval of a paid-order cancellation initiates a refund of the remaining refundable balance before the order is marked cancelled. An unpaid `CREATED` order can be cancelled without a refund.
- Cancellation requests are hidden for delivered orders and rejected server-side if any shipment is delivered.
- Refund and cancellation requests enter `PROCESSING` before calling Razorpay. If the provider call fails or its result cannot be confirmed, do not approve it again automatically. Check the Razorpay Dashboard first; processing requests are intentionally left visible for reconciliation.
- Razorpay webhook signatures are verified against the raw payload; event IDs are persisted for duplicate detection. Refund events update persisted provider refund state.

## Constraints
- Browser redirect is not proof of payment success.
- A refund request is not a refund until staff approve it and Razorpay returns a refund ID.
- Configure Razorpay webhooks for `payment.captured`, `refund.processed`, and `refund.failed` at `/api/payments/razorpay/webhook`.
- Never store raw card data or sensitive credentials.
