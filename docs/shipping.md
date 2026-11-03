# Shipping (Phase 1 Baseline)

## Finalized planning principle
- Shipping integration is isolated behind the shipping module interface.
- Quote outcomes must explicitly represent AVAILABLE, UNAVAILABLE, EXPIRED, and FAILED.

## Current scope
- Quote and shipment tracking APIs are scaffolded; live provider booking and rate calculation from destination/weight are not implemented yet.
- Quote endpoints verify the authenticated customer or guest cart access token against the cart.

## Planned model
- Quote generation uses weight, destination, and service.
- Shipment booking starts only after order/payment eligibility checks.
- Tracking updates via webhook plus reconciliation fallback.
- Provider-specific statuses mapped to internal shipping states.
