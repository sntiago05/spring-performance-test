# HelpFlow Runbook

How to start, test and troubleshoot the whole system. All commands run from the repository root.

## 1. Prerequisites

| Tool | Needed for | Check |
|---|---|---|
| Docker + Compose v2 | running the stack, Testcontainers | `docker compose version` |
| JDK 21+ | building/testing outside Docker | `java -version` |
| `curl`, `jq`, `openssl` | smoke test and scripts | `jq --version` |

Free ports: 4200 (frontend), 8080, 8081, 9090, 3000 and 5433 (change them in `.env`).

## 2. Start everything

```bash
./scripts/setup-env.sh                 # once: creates .env with random secrets
docker compose up -d --build           # builds and starts frontend, backend services, DB and monitoring
docker compose ps                      # wait until db, both services and frontend show (healthy)
```

Startup order is enforced with health checks: `db` -> `technician-dispatch-service` -> `support-ticket-service` -> `frontend` -> `prometheus` -> `grafana`.
`depends_on` only orders start-up; the ticket service additionally tolerates the dispatch service being down.

Credentials (from `.env`): admin `admin@helpflow.example` + `BOOTSTRAP_ADMIN_PASSWORD`; technicians
`tec-001@helpflow.example` ... `tec-015@helpflow.example` + `BOOTSTRAP_TECHNICIAN_PASSWORD`; Grafana `admin` + `GRAFANA_ADMIN_PASSWORD`.

```bash
docker compose logs -f support-ticket-service     # follow logs (JSON)
docker compose stop                                # stop, keep containers
docker compose down                                # remove containers, keep data (volumes)
docker compose down -v                             # remove containers AND data
docker compose up -d --build support-ticket-service   # rebuild one service after code changes
docker compose up -d db                            # start / lift only the PostgreSQL database
```

## 3. Verify it works (in this order)

1. **Health:** `curl -s localhost:8080/actuator/health` and `curl -s localhost:8081/actuator/health` -> `{"status":"UP"}`.
2. **Smoke test (about 30 s):** `./scripts/smoke-test.sh` -> `Result: 31 passed, 0 failed`.
3. **Web Frontend:** open http://localhost:4200 to access the HelpFlow web application.
4. **Swagger:** open http://localhost:8080/swagger-ui.html and follow section 4.
5. **SLA walkthrough:** `./scripts/demo-sla.sh` registers tickets, moves two of them back in time in PostgreSQL, runs the SLA review
   and prints escalated/breached tickets, their history and the metrics.
6. **Prometheus:** http://localhost:9090/targets -> services `UP`. Query `helpflow_tickets_registered_total`.
7. **Grafana:** http://localhost:3000 -> Dashboards -> HelpFlow -> *HelpFlow - Services Overview*.
8. **Automated tests:** `./mvnw clean verify` (see section 6).

## 4. Manual test with Swagger UI

1. `POST /auth/register` (any corporate e-mail such as `ana.perez@helpflow.example`, password >= 8 chars), then `POST /auth/login`.
2. Press **Authorize** and paste the `accessToken`.
3. `POST /tickets` with the example body -> expect `201`, `status: ASIGNADO`, a `technician` and `slaDueAt` 8 h later (priority ALTA).
4. `GET /tickets/{id}/history` -> `CREATED`, `ASSIGNED` (the text says whether dispatch or the rules chose the technician).
5. Log in as the technician shown in the ticket (`tec-0xx@helpflow.example`), authorize with that token and:
   `PATCH /tickets/{id}/status` `{"status":"EN_PROGRESO","observations":"On site"}` then `RESUELTO`.
6. Try the negative cases (each documented in Swagger): skip a state (`409`), read another user's ticket (`403`), no token (`401`), invalid body (`400`).
7. Log in as admin: `GET /tickets?overdue=true`, `PUT /tickets/{id}/assignment`, `PUT /sla-policies/ALTA`, `POST /tickets/escalations`.
8. Add `Accept-Language: es` to any failing request to see Spanish messages.

## 5. Postman

Import `docs/postman/helpflow.postman_collection.json` and `docs/postman/helpflow-local.postman_environment.json`.
Fill the environment values `adminPassword` and `technicianPassword` from `.env`, then run the collection in order with the
Collection Runner. The collection is idempotent: it registers the fixed people (Laura Gomez, Other User, Carlos Ruiz) once and
reuses them on later runs (409 is accepted), so repeated runs do not create duplicate users; tests assert status codes and the main fields.

## 6. Automated tests

```bash
./mvnw clean verify          # everything: unit (surefire) + integration (failsafe, needs Docker)
./mvnw test                  # unit tests only, no Docker needed
./mvnw -pl support-ticket-service verify -Dit.test=TicketAssignmentIT -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false   # one IT class
```

Reports: `*/target/surefire-reports` (unit) and `*/target/failsafe-reports` (integration). The integration tests start their own
PostgreSQL container (`postgres:16-alpine`) and do not touch the Compose database.

## 7. Useful inspection commands

```bash
# Interactive psql session inside the database container
docker compose exec -it db psql -U helpflow -d helpflow

# Quick queries without entering psql
docker compose exec -T db psql -U helpflow -d helpflow -c "select id,status,priority,sla_due_at,escalated,sla_breached from tickets order by id desc limit 10;"
docker compose exec -T db psql -U helpflow -d helpflow -c "select version,description,success from flyway_schema_history;"

# Check for duplicate records
docker compose exec -T db psql -U helpflow -d helpflow -c "select full_name, count(*) from users group by full_name having count(*) > 1;"
docker compose exec -T db psql -U helpflow -d helpflow -c "select title, affected_asset, count(*) from tickets group by title, affected_asset having count(*) > 1;"

# Metrics & Prometheus queries
curl -s localhost:8080/actuator/prometheus | grep ^helpflow_
curl -s 'localhost:9090/api/v1/query?query=helpflow_tickets_overdue' | jq .
```

## 8. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `required variable DB_PASSWORD is missing a value` / `JWT_SECRET is required` | No `.env` | `./scripts/setup-env.sh` |
| Ticket service exits with `Could not resolve placeholder 'JWT_SECRET'` or `DB_PASSWORD` | Env not exported (running with Maven) | `set -a; source .env; set +a` before `./mvnw ... spring-boot:run` |
| `JWT_SECRET must have at least 32 characters` | Short secret | `openssl rand -base64 48` into `.env` |
| `port is already allocated` / `address already in use` | Another process uses 8080/8081/9090/3000/5433 | Change `*_PORT` in `.env`, or stop the other process (`ss -ltnp \| grep :8080`) |
| `password authentication failed for user "helpflow"` | `DB_PASSWORD` in `.env` changed after the volume was created | `docker compose down -v` (wipes data) or restore the old password |
| `Flyway ... Validate failed: checksum mismatch` | An applied migration file was edited | Never edit applied migrations; add `V4__...`. In dev: `docker compose down -v` |
| `Schema-validation: missing column/table` | Entity and migration out of sync | Add a migration that matches the entity |
| Login returns `401` for the admin / technicians | Bootstrap passwords empty when the app first started | Set `BOOTSTRAP_*_PASSWORD` in `.env` and `docker compose up -d` (accounts are created if missing; existing ones keep their password) |
| `401` on a request that worked before | Token expired (1 h) | Log in again |
| `403` on an endpoint | Role or ownership rule (see README matrix) | Use the right role; the `type` field of the problem names the rule |
| Tickets stay `ABIERTO` | No eligible technician (inactive, wrong specialty or at capacity) | `GET /tickets/{id}/history` shows `ASSIGNMENT_PENDING`; admin assigns with `PUT /tickets/{id}/assignment` or raises capacity with `PUT /technicians/{id}` |
| Assignments always show `RULES` | The dispatch service is down or its suggestion is not eligible | `curl localhost:8081/actuator/health`; history text says `Dispatch suggestion ... rejected` when the rules overruled it |
| Prometheus target `DOWN` | Service not healthy yet, or wrong target name | `docker compose ps`; targets use service names (`support-ticket-service:8080`), never `localhost` |
| Grafana panels `No data` | No traffic yet (rates need data in the window) or Prometheus empty | Run `./scripts/smoke-test.sh`, wait 30 s, set the range to *Last 15 minutes* |
| Grafana login fails | Password in `.env` changed after first start (stored in the volume) | `docker compose exec grafana grafana cli admin reset-admin-password <new>` |
| `./mvnw verify` fails with `Could not find a valid Docker environment` | Docker not running / no permission | Start Docker; add your user to the `docker` group |
| Testcontainers pulls hang | No internet or registry limit | `docker pull postgres:16-alpine` beforehand |
| Build fails: `release version 21 not supported` | JDK older than 21 | Use JDK 21+ (`JAVA_HOME`) |
| `docker compose build` fails downloading Maven artifacts | No network inside the builder | Check proxy/DNS, retry; BuildKit caches downloaded artifacts |
| Spanish text shows `?` characters | Terminal not UTF-8 | `export LANG=C.UTF-8` |
| 409 `concurrent-modification` | Two requests changed the same ticket at once | Reload the ticket and retry |

## 9. Database maintenance, cleaning duplicates and reset

### 9.1 Lift and connect to the database

If the stack is stopped or you only need PostgreSQL:
```bash
docker compose up -d db
docker compose exec -it db psql -U helpflow -d helpflow
```

### 9.2 Clean duplicates (automated script)

When repeated tests, scripts or manual registrations generate duplicate records:
```bash
./scripts/clean-duplicates.sh
```
This runs an atomic SQL transaction that:
1. Identifies duplicate tickets with identical title, site, and affected asset, keeps the one with the most advanced state (`CERRADO` > `RESUELTO` > `EN_PROGRESO` > `ASIGNADO` > `ABIERTO`), and deletes duplicates along with their history and assignments (releasing technician capacity).
2. Identifies duplicate user records with the same name that are not referenced by tickets or technician profiles, and deletes them.

### 9.3 Reset to a clean state (wipe everything)

```bash
docker compose down -v && docker compose up -d --build     # empty database, metrics and dashboards re-provisioned
```
