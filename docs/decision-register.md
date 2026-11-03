# Decision Register (Phase 0)

## Finalized for architecture planning

1. GST pricing display and persistence (FINAL)
- Display prices inclusive of applicable GST.
- Tax rates, classification, and calculation rules are configurable.
- Applicable tax breakdown is preserved in order and invoice records.
- Note: Subject to professional tax advice before launch.

2. Shipping quote and provider isolation (FINAL)
- Shipping charges are computed using weight, destination, and service.
- Shipping provider integration is isolated behind the shipping module interface.
- Explicit handling is required for unavailable, expired, and failed shipping quotes.

3. Cancellation and return policy configurability (FINAL)
- Eligibility, cancellation cutoffs, refund timelines, and return rules are configurable.
- No universal hard-coded cancellation/return policy is allowed before legal and policy review.

4. Notification channels at launch (FINAL)
- Email is the only launch notification channel.
- SMS and WhatsApp are deferred.

5. Checkout mode (FINAL)
- Guest checkout is enabled.
- Optional account creation is supported.

## Non-negotiable architecture constraints (carried forward)
- Maintain a modular monolith.
- Do not introduce microservices, Kafka, Kubernetes, or Redis without demonstrated need.
- Preserve backend-authoritative pricing.
- Preserve durable and idempotent webhook/event processing.
- Preserve secure guest-order access control and auditability.
- Preserve production observability and operational runbooks.

## Implementation implication for Phase 1
- Phase 1 can proceed before final policy/legal decisions if implementation remains configuration-driven and does not hard-code tax, cancellation, return, or customer-policy assumptions.
