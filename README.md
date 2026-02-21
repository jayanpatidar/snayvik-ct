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
2. Run app:
```bash
./mvnw spring-boot:run
```
3. Open:
- UI: `http://localhost:8080/`
- API ping: `http://localhost:8080/api/kpi/system/ping`
- Actuator health: `http://localhost:8080/actuator/health`

## Docker compose
```bash
docker compose up --build
```

This boots:
- app on `:8086`
- postgres on `:5432`
- redis on `:6379`
