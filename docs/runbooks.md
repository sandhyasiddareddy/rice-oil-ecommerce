# Runbooks (Phase 1 Baseline)

## Database unavailable
1. Verify PostgreSQL container/service health.
2. Validate DB connection settings.
3. Check application startup logs for Flyway and datasource errors.
4. Restore service and rerun health checks.

## Failed deployment/build
1. Check CI workflow logs for backend/frontend job failures.
2. Identify whether failure is dependency, test, lint, type, or migration related.
3. Apply fix in branch and rerun CI.

## Incorrect config or policy value
1. Identify affected policy key and effective window.
2. Correct configuration value with audit record.
3. Re-run affected flow in staging before production update.

## Next runbooks to add
- Payment captured but order pending.
- Duplicate webhook handling.
- Refund pending too long.
- Shipment not booked/tracking stale.
- Import failure reconciliation.
