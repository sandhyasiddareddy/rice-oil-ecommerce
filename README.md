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
	- Razorpay provider order creation, Checkout handoff, payment verification, and webhook validation. External sandbox validation is still required.
	- Shipping quote and guest order access scaffolding.
	- Cancellation/refund request scaffolding.
- Storefront UI currently includes:
	- Home catalog, full catalog listing, product detail, cart, orders, login, address book, account recovery, and admin shell routes.
	- Organic-themed styling and customer-focused messaging.
- Customer accounts now include customer-owned carts/orders, an address book, checkout address snapshots, and expiring single-use password reset links.
- Razorpay checkout is wired for server-created provider orders and server-side payment verification. Sandbox checkout still requires account credentials and webhook configuration.
- Guest shipment tracking displays recorded events; those events are currently updated by the admin API until a carrier integration is configured.

## Repository layout
- backend: Spring Boot modular monolith foundation (Java 21, Flyway, Actuator, Security baseline).
- frontend: Next.js TypeScript foundation for storefront/admin UI.
- docs: architecture, decisions, API outline, data model notes, and planning documents.

## Local development prerequisites
- Java 21
- Node.js 22+
- Docker Desktop (optional, for local PostgreSQL)

## Quick start
For Windows PowerShell, Docker/H2 options, environment setup, and copy-paste commands, see [docs/setup.md](docs/setup.md).

In brief, install frontend dependencies once with `cd frontend; npm.cmd install`. Then use separate terminals to run the backend from `backend` and the frontend from `frontend`. If Docker is unavailable, the setup guide includes the H2 startup command and required environment variables.

## Build and test
1. Backend:
	- cd backend
	- ./mvnw verify
2. Frontend:
	- cd frontend
	- npm run lint
	- npm run typecheck
	- npm run build

## External service configuration

Configure these values through environment variables or a managed secret store; do not commit provider credentials:

- `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and `RAZORPAY_WEBHOOK_SECRET` for Razorpay sandbox checkout. `RAZORPAY_BASE_URL` defaults to `https://api.razorpay.com/v1`.
- For local backend development, `backend/.env` is loaded when starting Spring Boot from the `backend` directory. Keep the webhook secret blank until you configure one in Razorpay Test Mode.
- Set `SAMPLE_DATA_ENABLED=true` in the local `backend/.env` to seed sample rice products, pack sizes, stock, and illustrations on backend startup. This is disabled by default and should remain disabled in production.
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM` for password-reset email delivery. `MAIL_SMTP_AUTH` and `MAIL_SMTP_STARTTLS` default to `true`.
- `APP_FRONTEND_BASE_URL` sets the origin used in password-reset links and defaults to `http://localhost:3000`.

Provider accounts and sandbox credentials must be created by the account owner. A successful local test run does not validate connectivity to Razorpay or SMTP.

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
