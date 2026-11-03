# Refunds and Cancellations (Phase 1 Baseline)

## Finalized planning principle
- Eligibility, cutoffs, timelines, and return rules are configurable and not hard-coded.

## Current scope
- Workflow not implemented yet.

## Planned model
- Explicit cancellation/refund state transitions.
- Idempotent refund request handling.
- Verified provider evidence required before marking refund complete.
- Separate records for customer request, approval, provider submission, and completion.

## Pre-launch dependency
- Policy/legal review is required before production enforcement values are finalized.
