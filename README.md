# Snayvik KPI Governance System

Internal KPI governance and control-plane system.

## Stack
- Backend: Spring Boot 3 (Java 17) - package `com.snayvik.kpi`
- Frontend: React + TypeScript + Vite + Tailwind + Recharts (embedded in Spring Boot)
- Data: PostgreSQL + Redis

## Local run
1. Build and test:
```bash
./mvnw clean test
```
2. Create or update an admin user (required before login):
```bash
./snayvik admin create --username admin --password 'AdminPass@123'
```
You can pass Spring datasource args if needed, for example:
```bash
./snayvik admin create \
  --username admin \
  --password 'AdminPass@123' \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/snayvik_kpi \
  --spring.datasource.username=snayvik \
  --spring.datasource.password=snayvik
```
3. Run app:
```bash
./mvnw spring-boot:run
```
4. Open:
- UI: `http://localhost:8080/`
- API ping: `http://localhost:8080/api/kpi/system/ping`
- Actuator health: `http://localhost:8080/actuator/health`

## Demo Seeder (2 weeks)
- The app includes an idempotent demo seeder that creates:
  - board mappings
  - seed tasks, PRs, commits, task metrics
  - `daily_snapshots` for the last 14 days (2 weeks)
  - seed users, skill groups, and time sessions
- Enable for local run:
```bash
APP_SEED_ENABLED=true ./mvnw spring-boot:run
```
- Docker profile enables seed data by default (override with `APP_SEED_ENABLED=false`).

## Initial Full Sync (Phase 1)
- Full sync and reconciliation scaffolding is available behind `app.sync.enabled`.
- Default clients are no-op placeholders. They are intended to be replaced with real monday/GitHub API adapters.
- Configure:
```bash
APP_SYNC_ENABLED=true
APP_SYNC_GITHUB_REPOSITORIES=snayvik/repo-a,snayvik/repo-b
APP_SYNC_GITHUB_LOOKBACK_DAYS=90
APP_SYNC_INITIAL_RUN_ON_STARTUP=false
```
- Trigger manually (admin auth required):
```bash
curl -X POST -u admin:'AdminPass@123' http://localhost:8080/api/kpi/admin/sync/full
curl -X POST -u admin:'AdminPass@123' http://localhost:8080/api/kpi/admin/sync/reconcile
```
- Nightly reconciliation cron: `app.sync.reconciliation-cron` (default `0 30 2 * * *`).

## Authentication
- UI routes are protected and redirect unauthenticated users to `/login`.
- `/api/**` requires authentication.
- `/api/kpi/admin/**` requires `ADMIN` role.
- `/webhooks/**` remains public for GitHub/monday callbacks.
- API access example (basic auth):
```bash
curl -u admin:'AdminPass@123' http://localhost:8080/api/kpi/system/ping
```

## Docker compose
```bash
docker compose up --build
```

This boots:
- app on `:8086`
- postgres on `:5432`
- redis on `:6379`
