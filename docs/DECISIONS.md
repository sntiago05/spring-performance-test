# HelpFlow Design Decisions

Short architecture decision records based on the current implementation.

## ADR 1 — Use hexagonal architecture for the ticket service

- **Context:** Ticket rules must remain testable without tying the domain to HTTP or persistence frameworks.
- **Decision:** Keep framework-independent domain models and policies; define application input/output ports; implement REST, scheduler, JPA, dispatch, security and metrics adapters in infrastructure.
- **Alternatives:** Put JPA annotations and business behavior in the same entities.
- **Consequences:** The domain and use cases are independently testable; adapters and mapping code add structure and maintenance overhead.

## ADR 2 — Use two services with a synchronous dispatch boundary

- **Context:** The specification calls for a central ticket service and an external technician availability simulator.
- **Decision:** `support-ticket-service` owns ticket state and PostgreSQL. `technician-dispatch-service` is stateless and provides suggestions over HTTP. The public contract remains `POST /technician-dispatch`.
- **Alternatives:** Merge dispatch into the ticket service or introduce a broker/gateway.
- **Consequences:** The service boundary is explicit and locally deployable; ticket creation depends on a bounded synchronous call, mitigated by timeouts and fallback.

## ADR 3 — Make dispatch deterministic with CRC32

- **Context:** The simulator must produce repeatable suggestions without a database.
- **Decision:** Derive selection and estimated hours from CRC32 of ticket ID, category and priority, choosing among the technicians of a configurable pool that can handle the ticket category (see ADR 11).
- **Alternatives:** Random selection or live availability lookup.
- **Consequences:** Repeated inputs are reproducible and simple to test; results do not reflect operational availability.

## ADR 4 — Validate dispatch suggestions inside the ticket domain

- **Context:** The dispatch simulator keeps only a copy of the technician specialties; it does not own the authoritative status, capacities, sites or SLA policy.
- **Decision:** Treat dispatch as a suggestion. Apply active status, specialty compatibility, capacity, site ranking and SLA-fit rules in the ticket service (the specialty check stays there even though dispatch already filters by it). Continue with internal rules if dispatch fails.
- **Alternatives:** Trust dispatch as the assignment authority or fail ticket creation when it is unavailable.
- **Consequences:** Ticket creation degrades gracefully and remains governed by local rules; the synchronous call still occupies the creation transaction until it completes or times out.

## ADR 5 — Keep JPA schema evolution in Flyway

- **Context:** Persisted relationships and constraints must be reproducible across environments.
- **Decision:** Use Flyway migrations V1–V3 and Hibernate schema validation (`ddl-auto: validate`). Keep domain objects separate from persistence entities and map with MapStruct.
- **Alternatives:** Hibernate schema generation or JPA entities as domain models.
- **Consequences:** Schema changes are explicit and auditable; migrations must be added rather than editing applied files.

## ADR 6 — Use JWT roles plus resource ownership checks

- **Context:** Requesters, technicians and administrators have different access to tickets and configuration.
- **Decision:** Use stateless JWT authentication with `ROLE_USUARIO`, `ROLE_TECNICO` and `ROLE_ADMIN`; BCrypt password hashing; enforce route roles in security configuration and ownership/assignment rules in application services.
- **Alternatives:** Session-based authentication or role checks alone.
- **Consequences:** APIs remain stateless and resource-scoped; tokens remain valid until expiry because there is no revocation list.

## ADR 7 — Return localized ProblemDetail errors

- **Context:** Callers need consistent machine-readable errors and traceable failures.
- **Decision:** Map domain, application, framework and unexpected exceptions to `application/problem+json` with status, type, title, detail, instance, timestamp and trace ID. Resolve messages in English or Spanish from `Accept-Language`.
- **Alternatives:** Ad hoc error DTOs or framework default error pages.
- **Consequences:** Clients can handle a stable shape across services; message catalogs must stay aligned with error codes. The implementation uses Spring `ProblemDetail`, compatible with RFC 9457 (which supersedes RFC 7807).

## ADR 8 — Use transactional assignment and explicit concurrency controls

- **Context:** Ticket state, assignment records and history must remain consistent under concurrent requests.
- **Decision:** Keep create/assign/reassign/status changes transactional; use optimistic ticket versioning and pessimistic locking when checking technician capacity.
- **Alternatives:** Independent writes or optimistic retries for technician capacity.
- **Consequences:** Assignment and audit history commit together and capacity is rechecked under lock; lock contention can limit throughput.

## ADR 9 — Store editable SLA policy and review periodically

- **Context:** Resolution targets differ by priority and tickets nearing deadlines require escalation.
- **Decision:** Persist resolution hours and escalation threshold by priority. Calculate deadlines in calendar hours, run a one-minute scheduler, and expose an administrator-triggered review endpoint. Escalation raises priority once; breach is separately recorded.
- **Alternatives:** Hard-coded policy, business-hour calendars, or external workflow scheduling.
- **Consequences:** Administrators can adjust future tickets; existing deadlines remain unchanged. The scheduler currently assumes a single service instance.

## ADR 10 — Verify persistence with Testcontainers and observe with Prometheus

- **Context:** Integration behavior should be exercised against PostgreSQL, and operational state should be visible.
- **Decision:** Use Testcontainers PostgreSQL for integration tests, Actuator/Micrometer metrics for services, and Prometheus/Grafana in Compose.
- **Alternatives:** H2-only persistence tests or external monitoring infrastructure.
- **Consequences:** Tests exercise the actual database engine and local monitoring is reproducible; integration verification requires Docker and monitoring storage uses Compose volumes.

## ADR 11 — Make dispatch suggestions specialty-aware

- **Context:** The first dispatch selector picked any of the 15 pool codes with no knowledge of specialties, so many suggestions were incompatible and discarded: only 37 % of the automatic assignments used the suggestion, and the history was full of rejections.
- **Decision:** Configure the pool with each technician's code and specialty (`dispatch.technicians`) and let the CRC32 seed choose among the candidates able to handle the category (same rule as the ticket service: own specialty, generalist `OTRO`, or anybody for `OTRO` tickets). The JSON contract is unchanged.
- **Alternatives:** Keep the agnostic selector (simplest, but most suggestions are unusable); read technicians from the ticket service (couples the services and defeats the stateless simulator).
- **Consequences:** Suggestions are compatible by construction. Measured end to end with 60 tickets per priority: 98 % accepted with `MEDIA`, 58 % with `ALTA`, 45 % with `CRITICA`; every remaining discard is either an estimate (0-12 h) that does not fit a short SLA window or a technician at capacity, never a specialty mismatch. The pool is configuration, so technicians created later through the API are unknown to dispatch and are only chosen by the assignment rules. Making estimates depend on the priority would be the next improvement.
