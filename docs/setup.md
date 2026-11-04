# Farmora Local Setup Guide

This guide covers Windows PowerShell startup with Docker/PostgreSQL or the H2 fallback.

## Prerequisites

- Java 21
- Node.js 22 or newer and npm
- Docker Desktop only for PostgreSQL mode

Check the installed tools from PowerShell:

```powershell
java -version
node --version
npm --version
```

If the repository's `.tools` folder is available, add its Java and Node `bin` folders to `PATH` for the current terminal before continuing.

## One-time frontend install

Run from the repository root:

```powershell
cd frontend
npm.cmd install
```

## Start with PostgreSQL

Use two PowerShell terminals. In terminal 1, start PostgreSQL from the repository root:

```powershell
docker compose up -d
```

Check `backend/.env` before starting the backend. Spring loads that file when its working directory is `backend`; any `DB_URL`, `DB_USERNAME`, or `DB_PASSWORD` values there override the Docker defaults. Then run:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

In terminal 2, start the storefront:

```powershell
cd frontend
npm.cmd run dev
```

Open the frontend URL printed by Next.js (normally `http://localhost:3000`). Backend health is at `http://localhost:8080/actuator/health`.

To seed sample rice products and stock in a local database, set `SAMPLE_DATA_ENABLED=true` in `backend/.env` and restart the backend. The seed is disabled by default; do not enable it in production.

## Start without Docker (H2)

H2 is test-scoped, so use Spring Boot's `test-run` goal. This command skips compiling repository test sources while retaining the H2 test classpath. Run from `backend` in terminal 1:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:mem:platform;MODE=PostgreSQL;DB_CLOSE_DELAY=-1'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'org.h2.Driver'
$env:SPRING_DATASOURCE_USERNAME = 'sa'
$env:SPRING_DATASOURCE_PASSWORD = ''
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'create-drop'
$env:SPRING_FLYWAY_ENABLED = 'false'
.\mvnw.cmd -Dmaven.test.skip=true spring-boot:test-run
```

In terminal 2, start the frontend as above. H2 is in memory; restarting the backend clears accounts, carts, catalog data, and orders.

## Stop the app

Press `Ctrl+C` in both terminals. If using Docker, stop PostgreSQL from the repository root:

```powershell
docker compose down
```

## Troubleshooting

### Port 3000 is already in use

Next.js automatically tries another port, usually 3001. Use the URL printed in the frontend terminal. The default CORS configuration allows both `localhost:3000` and `localhost:3001`.

### Admin portal

Open `/admin` on the active frontend origin. Staff credentials are configured by `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD` in `backend/.env`. The code defaults (`admin` / `change-me-admin-password`) are for local development only; replace them before sharing or deploying. Customer accounts do not grant staff access.

### The UI shows stale pages or missing chunks

Stop the frontend, then remove the Next.js cache and restart it:

```powershell
Remove-Item -Recurse -Force frontend\.next
cd frontend
npm.cmd run dev
```

### Backend tests fail to compile

The H2 startup command above uses `-Dmaven.test.skip=true` to bypass test-source compilation. Run `mvnw.cmd test` separately when working on tests; do not ignore test failures in CI.

## Useful routes

- Home: `http://localhost:3000/`
- Catalog: `http://localhost:3000/catalog`
- Cart: `http://localhost:3000/cart`
- Orders: `http://localhost:3000/orders`
- Account: `http://localhost:3000/login`
- Staff portal: `http://localhost:3000/admin`
- Backend health: `http://localhost:8080/actuator/health`