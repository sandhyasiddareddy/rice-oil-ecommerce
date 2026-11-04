# Deployment

## Environments
- Development: local Docker PostgreSQL + local app processes.
- CI: GitHub Actions (`.github/workflows/ci.yml`).
- Staging/Production: not provisioned. No Azure database or storage resources have been created. The `FarmoraDev` resource group remains from failed database provisioning attempts.

## Current Azure status
- The signed-in subscription is active and its portal shows free services through December 4, 2027. The PostgreSQL Flexible Server meters currently show allowances of 750 B1MS compute hours/month, 32 GB storage/month, and 32 GB LRS backup storage/month, all unused.
- The PostgreSQL allowance is limited to eligible meters and is not a general Azure spending cap. Monitor usage and confirm the offer in Cost Management before provisioning. Usage beyond the allowance, other chargeable meters, or use after the free period can incur charges.
- No Azure database or storage resources have been created. The approved preparation scope is app/configuration work only; resource provisioning still requires confirmation.
- App Service F1 is listed as free but is intended for experimentation: 60 shared CPU minutes/day, 1 GB RAM, 1 GB storage, and no SLA. It is not suitable for production.
- Azure Static Web Apps Free is not selected: this app uses Next.js server rendering and runtime API calls, so its deployment compatibility needs a separate validation.
- The free allowance can support a development database, but it does not make the complete application a production-ready or guaranteed zero-cost Azure deployment.

## Azure deployment configuration checklist

Before deploying, determine and record:
1. An active Azure subscription with the required resource-creation permissions.
2. The target region and monthly cost ceiling. South India is preferred; verify the exact SKU and free meters in the subscription before provisioning.
3. Hosting compatibility for the Next.js frontend and Spring Boot backend. `NEXT_PUBLIC_API_BASE_URL` is embedded during the frontend build and must point to the deployed API origin. Set backend `APP_CORS_ALLOWED_ORIGINS` to the exact frontend origin(s), comma-separated.
4. A PostgreSQL connection using `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. For Azure Database for PostgreSQL, use the server-provided hostname/database and TLS (for example, `?sslmode=require`). Never use development credentials in a hosted environment.
5. Hosted settings for `APP_FRONTEND_BASE_URL`, `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`, `APP_DEFAULT_USERNAME`, `APP_DEFAULT_PASSWORD`, and a strong unique `JWT_SECRET`, as well as Razorpay and mail credentials. Keep secrets in hosting settings or a managed secret store, never in source control or deployment artifacts.
6. A verified Razorpay webhook URL configured for `payment.captured`, `refund.processed`, and `refund.failed`; payment, refund, and shipping fee end-to-end checks; database backup/restore; health checks; and a rollback plan before accepting live orders.

### Backend environment settings

The backend reads these settings from environment variables in Azure; the local optional `.env` file is not required there:

| Variable | Azure value |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://<server>.postgres.database.azure.com:5432/<database>?sslmode=require` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials configured for the server |
| `PORT` | Optional; defaults to `8080` |
| `APP_FRONTEND_BASE_URL` | Public frontend origin, used in password-reset links |
| `APP_CORS_ALLOWED_ORIGINS` | Exact public frontend origin(s), comma-separated |
| `JWT_SECRET` | Strong, unique secret; replace the local development default |
| `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` | Unique hosted admin credentials; replace defaults |
| `APP_DEFAULT_USERNAME` / `APP_DEFAULT_PASSWORD` | Unique hosted default-user credentials; replace defaults |
| `SAMPLE_DATA_ENABLED` | `false` |
| `AZURE_STORAGE_BLOB_ENDPOINT` | Optional Blob service URL, for example `https://<account>.blob.core.windows.net`; omit to keep uploads disabled |
| `AZURE_STORAGE_BLOB_CONTAINER` | Existing container for product images; defaults to `product-images` |
| `AZURE_STORAGE_BLOB_PUBLIC_BASE_URL` | Public/CDN URL for that container; must match the frontend setting below |
| `NEXT_PUBLIC_AZURE_BLOB_PUBLIC_BASE_URL` | Frontend build-time public/CDN URL for the container, same value as backend setting |

Product image uploads use the backend's `DefaultAzureCredential`: grant its managed identity the **Storage Blob Data Contributor** role on the storage account (or container). Locally, sign in with Azure CLI. The container must already exist; the app does not create Azure resources or containers. Product images are public storefront content, so the configured public base URL must serve the container's blobs over HTTPS (for example through an appropriately configured CDN). Do not use this storage integration for customer uploads or private files.

An ADMIN can upload a JPEG, PNG, or WebP (up to 5 MB) for an existing product with `POST /api/admin/catalog/products/{productId}/images`, as multipart form data with a `file` part and optional `altText`. The API returns and stores the public image URL in `product_image`; without Blob configuration the endpoint returns HTTP 503. Existing static images and URL metadata remain unaffected.

Never put production values in the repository or deployment artifacts. Configure them as protected application settings after the Azure resources and endpoints are approved.

Do not treat a successful deployment as approval to accept real payments. Complete the production checklist and verify the full checkout total, including shipping, first.

## CI pipeline
Configured in `.github/workflows/ci.yml` (push to `main` and pull requests):
- Backend: Java 21, `./mvnw verify` (ModuleBoundaryTest is excluded until its pre-existing module cycles are fixed).
- Frontend: Node 22, `npm ci`, lint, typecheck, build.
- Docker: builds both images after the above pass.

## Docker
- `backend/Dockerfile` (Java 21 JRE, port 8080) and `frontend/Dockerfile` (Next.js standalone, port 3000).
- `NEXT_PUBLIC_API_BASE_URL` and `NEXT_PUBLIC_AZURE_BLOB_PUBLIC_BASE_URL` are build arguments because Next.js embeds them at build time.
- Local database only: `docker compose up -d`. Full stack: `docker compose --profile app up --build`.

## Runtime business settings
- Shipping fee tiers live in the database (`shipping_fee_tier`, seeded 0 to 80, 499 to 40, 999 to free). ADMIN edits them with `GET`/`PUT /api/admin/shipping/fee-tiers`; no redeploy is needed.
- Tax rules are uploaded as Excel by ADMIN: `GET /api/admin/tax-rules/template`, then `POST /api/admin/tax-rules/import?dryRun=true` to validate and `dryRun=false` to apply. Rows upsert by `code` + `effectiveFrom`; any invalid row blocks the whole upload. With no matching rule, tax is 0.
- Auth rate limit: `AUTH_RATE_LIMIT_MAX_REQUESTS` (default 10) per `AUTH_RATE_LIMIT_WINDOW_SECONDS` (default 60) per client IP and auth path, in memory per instance; `AUTH_RATE_LIMIT_ENABLED=false` disables it.
- Unpaid-order expiry: `ORDER_UNPAID_EXPIRY_MINUTES`, `ORDER_EXPIRY_LOOKBACK_HOURS`, `ORDER_EXPIRY_CHECK_INTERVAL_MS`.

## Secrets policy
- No production secrets in repository files.
- Use environment variables locally.
- Use secure secret management in hosted environments.

## Rollback and migrations
- Flyway is enabled for schema versioning.
- Backward-compatible migration strategy remains default where practical.
- Rollback approach for destructive changes must use forward-fix migrations when required.
