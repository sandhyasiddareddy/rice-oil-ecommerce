# Observability (Phase 1 Baseline)

## Implemented baseline
- Spring Boot Actuator endpoints enabled for health, info, and metrics exposure.
- Correlation ID filter adds X-Correlation-Id for request traceability.
- Centralized API error envelope includes correlation ID.

## Planned in later phases
- OpenTelemetry traces and Application Insights export.
- Payment/shipping/refund/import operational metrics and dashboards.
- Alert rules for stuck payments, refunds, shipment delays, and retry failures.

## Constraints
- Do not log secrets or sensitive payment details.
- Keep customer PII out of routine logs.
