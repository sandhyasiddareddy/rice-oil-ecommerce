# Deployment (Phase 1 Baseline)

## Environments
- Development: local Docker PostgreSQL + local app processes.
- CI: GitHub Actions workflow with backend and frontend jobs.
- Staging/Production: to be defined in later phases with Azure App Service and managed PostgreSQL.

## CI pipeline baseline
- Backend: Java 21 setup, Maven wrapper verify.
- Frontend: Node 22 setup, npm install, lint, typecheck, build.

## Secrets policy
- No production secrets in repository files.
- Use environment variables locally.
- Use secure secret management in hosted environments.

## Rollback and migrations
- Flyway is enabled for schema versioning.
- Backward-compatible migration strategy remains default where practical.
- Rollback approach for destructive changes must use forward-fix migrations when required.
