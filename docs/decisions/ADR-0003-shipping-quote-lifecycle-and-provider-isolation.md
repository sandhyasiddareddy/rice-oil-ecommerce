# ADR-0003: Shipping Provider Isolation and Quote Lifecycle

Status: Accepted
Date: 2026-11-02

## Context
Shipping costs depend on destination, weight, and service and provider calls may fail or return stale quotes.

## Decision
- Isolate shipping provider behavior behind a shipping module interface.
- Compute shipping quotes based on weight, destination, and service.
- Model quote states explicitly: AVAILABLE, UNAVAILABLE, EXPIRED, FAILED.

## Consequences
- Reduces coupling to provider-specific APIs.
- Enables clearer customer messaging and safer checkout behavior.
- Supports provider replacement without broad code rewrites.

## Constraints
- No checkout on expired quote without recalculation.
- No provider capability assumptions beyond documented behavior.
