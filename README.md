# Farmora E-Commerce Platform

This repository contains phased implementation of a modular e-commerce platform with working storefront flows, backend domain APIs, and setup paths for both Docker and no-Docker environments.

## Current status
- Phase 0 documentation finalized with confirmed architecture decisions.
- Phase 1 foundation scaffolded for backend, frontend, CI, and local PostgreSQL.
- Phase 2 backend slice implemented and verified for:
	- Category, product, variant, image, and inventory data model/migrations.
	- Admin catalog creation and inventory adjustment APIs.
	- Excel import workflow with template download, preview validation, confirm apply, and error CSV.
- Phase 3 backend implementation includes:
	- Tax rule and promotion persistence schema.
	- Pricing quote service and coupon validation with redemption constraints.
	- Checkout order placement with idempotency support.
	- Payment create-order, verify, and webhook ingestion scaffolding.
	- Shipping quote and guest order access scaffolding.
	- Cancellation/refund request scaffolding.
- Storefront UI currently includes:
	- Home catalog, full catalog listing, product detail, cart, orders, login, and admin shell routes.
	- Organic-themed styling and customer-focused messaging.

## Repository layout
- backend: Spring Boot modular monolith foundation (Java 21, Flyway, Actuator, Security baseline).
- frontend: Next.js TypeScript foundation for storefront/admin UI.
- docs: architecture, decisions, API outline, data model notes, and planning documents.

## Local development prerequisites
- Java 21
- Node.js 22+
- Docker Desktop (optional, for local PostgreSQL)

## Quick start
Use docs/setup.md for full machine setup, including Docker and H2 fallback modes.

Basic flow:
1. Start database (optional if using Docker mode):
	- docker compose up -d
2. Start backend:
	- cd backend
	- ./mvnw spring-boot:run
3. Start frontend:
	- cd frontend
	- npm install
	- npm run dev

## Build and test
1. Backend:
	- cd backend
	- ./mvnw verify
2. Frontend:
	- cd frontend
	- npm run lint
	- npm run typecheck
	- npm run build

## Notes
- Policy-sensitive behaviors (tax, cancellation, returns, refund timelines, shipping policy) must remain configuration-driven and not hard-coded.
- Backend remains authoritative for pricing and checkout totals.
- Webhook/idempotency, guest order security, and observability constraints are preserved in architecture docs.
- Local verification in this repository was executed using portable runtimes under `.tools` (`jdk-21.0.13+9`, `node-v22.11.0-win-x64`).
- Frontend dependency warnings were observed for deprecated package versions and known vulnerabilities in the baseline stack; these are tracked for dependency-hardening work in later phases.

## Documentation index
- docs/decision-register.md
- docs/architecture.md
- docs/database.md
- docs/api-outline.md
- docs/phase-plan.md
- docs/prelaunch-checklist.md
- docs/setup.md
- docs/decisions/
