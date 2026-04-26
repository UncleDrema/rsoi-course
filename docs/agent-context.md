# Agent Context

This file summarizes the current codebase so future Codex work can start from stable context instead of rediscovering the project shape.

## High-Level Architecture

The project is a course microservice system for flight booking. It has four independent Spring Boot services, a shared PostgreSQL instance, and deployment materials for Docker Compose, Helm, and Terraform.

The main public entrypoint is the gateway. Requests are accepted under `/api/v1/**` and forwarded to downstream services by path:

- `/api/v1/flights/**` and `/api/v1/airports/**` -> flights service.
- `/api/v1/tickets/**` -> tickets service.
- `/api/v1/privilege/**` and `/api/v1/privileges/**` -> privileges service.

All services use Spring Security OAuth2 Resource Server JWT validation with the configured issuer URI.

## Services

### Gateway

Path: `services/gateway`

Responsibilities:

- `/api/v1/authorize`: exchanges username/password for Auth0 tokens.
- `/api/v1/me`: aggregates tickets and privilege information for the authenticated user.
- `/api/v1/**`: proxies requests to downstream services.
- Tracks downstream health with circuit-breaker/degradation rules from `application.yml`.

Important packages:

- `web`: proxy and health controllers.
- `services`: downstream clients and circuit breaker.
- `config`: degradation route/service configuration.
- `auth`: Auth0 login/token DTOs and properties.
- `dto`, `types`: external contract objects.

### Flights

Path: `services/flights`

Responsibilities:

- `GET /flights`: paginated flight list.
- `GET /flights/{flightNumber}`: single flight.
- `POST /flights`: create flight.
- `GET /airports`: paginated airport list.
- `POST /airports`: create airport.

Important packages:

- `web`: `FlightsController`, `AirportController`, health endpoint.
- `services`: JPA repositories and domain services.
- `types`: `Flight`, `Airport`.
- `dto`: API contracts.

### Tickets

Path: `services/tickets`

Responsibilities:

- `GET /tickets`: list current user's tickets.
- `POST /tickets`: buy ticket.
- `GET /tickets/{ticketUid}`: get current user's ticket.
- `DELETE /tickets/{ticketUid}`: cancel current user's ticket.
- Calls gateway/downstream APIs for related flight and privilege operations.

Important packages:

- `web`: controller, mapper, exception handler.
- `services`: ticket repository, business service, clients.
- `types`: ticket domain/status.
- `dto`: API contracts.

### Privileges

Path: `services/privileges`

Responsibilities:

- `GET /privilege`: current user's loyalty state.
- `POST /privilege/withdraw`: withdraw loyalty balance.
- `POST /privilege/deposit`: deposit loyalty balance.
- `POST /privilege/cancel/{ticketUid}`: compensate/cancel loyalty operation for a ticket.

Important packages:

- `web`: privilege and health controllers.
- `services`: privilege repository and business service.
- `types`: privilege state/history.
- `dto`: API contracts.

## Build Configuration

Every service has its own:

- `build.gradle.kts`
- `settings.gradle.kts`
- `gradlew.bat`

Common stack:

- Java toolchain `25`
- Spring Boot `4.0.0-SNAPSHOT`
- Spring dependency management `1.1.7`
- Spring MVC, Security, OAuth2 resource server
- Lombok
- Springdoc/OpenAPI
- PostgreSQL driver for stateful services
- JUnit Platform via `spring-boot-starter-test`

There is no root multi-project Gradle build. Run Gradle commands from each service directory.

## Configuration And Ports

Default ports:

- gateway: `8080`
- flights: `8060`
- tickets: `8070`
- privileges: `8050`
- postgres: `5432`

Default database names:

- `flights`
- `tickets`
- `privileges`

`docker-compose.yml` starts PostgreSQL and published images for all four services. For local source-code development, run services directly with Gradle or add local image build steps deliberately.

## Assignment Materials

The directories `v1`, `v2`, `v3`, and `v4` contain original OpenAPI/Postman materials for course variants:

- `v1`: Flight Booking System.
- `v2`: Hotels Booking System.
- `v3`: Car Rental System.
- `v4`: Library System.

The root `README.md` appears to be a lab assignment document with mojibake in the current console encoding; do not assume the visible terminal text is the intended Russian source encoding.

## Known Risks For Future Work

- Several comments in source files display as mojibake in PowerShell output. Avoid changing comment text unless the task requires it.
- `application.yml` contains Auth0 and issuer settings. Do not expose or rotate them casually.
- `docker-compose.yml` uses external images under `uncledrema/*`; local code changes will not affect compose runtime until images are rebuilt and tags updated.
- Generated artifacts are present under `.gradle` and `bin`. Do not treat compiled classes as source of truth.
- There are existing uncommitted changes in `charts/rsoi/values-*.yaml`.

## Recommended Verification Matrix

For code changes in one service:

```powershell
cd services\<service>
.\gradlew.bat compileJava
.\gradlew.bat test
```

For API contract changes:

- Compile and test the owning service.
- Compile gateway if paths/DTOs visible through `/api/v1/**` changed.
- Compile any downstream/upstream service client that consumes the changed contract.

For deployment changes:

- Render or lint Helm charts when Helm is available.
- Check `docker-compose.yml` syntax with Docker Compose when Docker is available.
- Avoid applying Terraform unless explicitly requested.
