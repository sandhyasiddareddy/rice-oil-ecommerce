# ADR-0002: GST-Inclusive Display with Configurable Tax Rules

Status: Accepted
Date: 2026-11-02

## Context
Prices must be displayed inclusive of GST while tax rates and classifications may evolve and require legal/tax validation.

## Decision
- Display product/customer-facing prices inclusive of applicable GST.
- Keep tax rates, classifications, and calculation rules configurable.
- Persist tax breakdown snapshots in order and invoice records.

## Consequences
- Prevents code changes for routine tax updates.
- Preserves historical correctness for audits and customer support.
- Requires disciplined versioning of tax rules/effective dates.

## Constraints
- Final tax/legal interpretation requires professional advice before production launch.
