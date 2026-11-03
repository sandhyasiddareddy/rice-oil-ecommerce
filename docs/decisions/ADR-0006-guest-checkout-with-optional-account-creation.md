# ADR-0006: Guest Checkout with Optional Account Creation

Status: Accepted
Date: 2026-11-02

## Context
Reducing checkout friction is important, while preserving secure order visibility and post-purchase account continuity.

## Decision
- Enable guest checkout.
- Allow optional account creation during or after checkout.
- Implement secure guest-order access controls and auditable access events.

## Consequences
- Improved conversion through reduced account-creation friction.
- Additional security requirements for guest order access token handling.
- Requires clear UX path to link guest orders after account creation.

## Constraints
- Do not expose order details without valid customer ownership or secure guest token.
