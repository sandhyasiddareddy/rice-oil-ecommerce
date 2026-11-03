# ADR-0004: Configurable Cancellation, Return, and Refund Policy

Status: Accepted
Date: 2026-11-02

## Context
Business and legal policy decisions for cancellation cutoffs, return eligibility, and refund timelines are still pending.

## Decision
- Keep eligibility, cutoffs, timelines, and return rules configurable.
- Avoid hard-coded universal policy assumptions.
- Enforce policy through configuration-driven evaluation.

## Consequences
- Phase 1 can proceed without blocking on final legal policy wording.
- Reduces future rework when policies are finalized.
- Requires policy governance and audit-friendly configuration changes.

## Constraints
- Customer-visible behavior must remain explicit and auditable.
- Refund completion must depend on verified provider evidence.
