# Farmora Local Setup Guide

This guide helps you run the project on a new machine with either Docker PostgreSQL or an H2 fallback.

## 1. Prerequisites
- Git
- Java 21
- Node.js 22+
- npm
- Docker Desktop (optional, for PostgreSQL mode)

If you do not want global installs, this repository includes portable runtimes in .tools.

## 2. Clone and install dependencies
1. Clone the repository.
2. Open terminal at repository root.
3. Install frontend dependencies:
   - cd frontend
   - npm install

## 3. Run with PostgreSQL (recommended)
1. Start DB from repository root:
   - docker compose up -d
2. Start backend:
   - cd backend
   - ./mvnw spring-boot:run
3. Start frontend:
   - cd frontend
   - npm run dev
4. Open:
   - Frontend: http://localhost:3000
   - Backend health: http://localhost:8080/actuator/health

## 4. Run without Docker (H2 fallback)
Use this if Docker is unavailable.

1. Start backend with H2 on test classpath:
   - cd backend
   - SPRING_DATASOURCE_URL=jdbc:h2:mem:platform;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
   - SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver
   - SPRING_DATASOURCE_USERNAME=sa
   - SPRING_DATASOURCE_PASSWORD=
   - SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop
   - SPRING_FLYWAY_ENABLED=false
   - ./mvnw spring-boot:test-run

On PowerShell (single line):
- $env:SPRING_DATASOURCE_URL='jdbc:h2:mem:platform;MODE=PostgreSQL;DB_CLOSE_DELAY=-1'; $env:SPRING_DATASOURCE_DRIVER_CLASS_NAME='org.h2.Driver'; $env:SPRING_DATASOURCE_USERNAME='sa'; $env:SPRING_DATASOURCE_PASSWORD=''; $env:SPRING_JPA_HIBERNATE_DDL_AUTO='create-drop'; $env:SPRING_FLYWAY_ENABLED='false'; ./mvnw.cmd spring-boot:test-run

2. Start frontend:
   - cd frontend
   - npm run dev

## 5. Portable toolchain mode (if java/npm are not on PATH)
From repository root:

Frontend:
- Windows PowerShell:
  - $env:Path='C:\Users\<you>\rice-oil-ecommerce\.tools\node-v22.13.1-win-x64;' + $env:Path
  - cd frontend
  - npm.cmd run dev

Backend:
- Windows PowerShell:
  - $env:JAVA_HOME='C:\Users\<you>\rice-oil-ecommerce\.tools\jdk-21.0.13+9'
  - $env:Path="$env:JAVA_HOME\bin;$env:Path"
  - cd backend
  - ./mvnw.cmd --batch-mode test

## 6. Verification checks
- Frontend build:
  - cd frontend
  - npm run build
- Backend tests:
  - cd backend
  - ./mvnw --batch-mode test

## 7. Troubleshooting
### UI opens but shows 404/500 or missing style/script chunks
Cause: stale or corrupted Next.js cache.

Fix:
1. Stop dev server.
2. Remove cache files in frontend:
   - .next directory
   - tsconfig.tsbuildinfo
3. Restart dev server:
   - npm run dev

### Port 3000 already in use
- Stop old Node process, then restart frontend dev server.

### npm or java not recognized
- Use portable toolchain commands from section 5.

## 8. Key routes
- Home: http://localhost:3000/
- Catalog: http://localhost:3000/catalog
- Cart: http://localhost:3000/cart
- Orders: http://localhost:3000/orders
- Login: http://localhost:3000/login
- Admin: http://localhost:3000/admin
