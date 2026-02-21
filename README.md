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
