# Agent Instructions

This repository is a Java/Spring Boot microservice course project. Use these notes as the first context source before changing code.

## Repository Shape

- `services/gateway`: API gateway and auth entrypoint, port `8080`.
- `services/flights`: flights and airports service, port `8060`, PostgreSQL database `flights`.
- `services/tickets`: tickets service, port `8070`, PostgreSQL database `tickets`.
- `services/privileges`: loyalty/privileges service, port `8050`, PostgreSQL database `privileges`.
- `postgres`: local database initialization scripts.
- `charts`: Helm charts for service deployment.
- `infra`: Terraform/Yandex Cloud infrastructure files.
- `v1`..`v4`: original assignment specs and Postman collections.
- `docs`: agent-facing project notes created for Codex work.

## Build And Test

Each service is an independent Gradle project. Run commands from the service directory.

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat bootRun
```

For multi-service work, verify every touched service. There is no root Gradle build at the moment.

## Local Runtime

`docker-compose.yml` defines PostgreSQL plus four service containers. The compose file currently uses published images, not local builds.

Default local service URLs:

- Gateway: `http://localhost:8080`
- Flights: `http://localhost:8060`
- Tickets: `http://localhost:8070`
- Privileges: `http://localhost:8050`
- PostgreSQL: `localhost:5432`, user `program`, password `test`

## Coding Conventions

- Keep changes inside the owning service unless an API contract requires coordinated edits.
- Prefer existing Spring MVC, RestTemplate, Lombok, DTO record, and JPA repository patterns already present in the service.
- Keep REST contracts consistent with gateway forwarding under `/api/v1/**`.
- Add or update tests in the same service when changing business behavior.
- Do not rewrite generated or compiled outputs under `bin`, `build`, or `.gradle`.
- Avoid broad refactors while implementing course requirements; keep commits and patches reviewable by service boundary.

## Security And Secrets

The repository currently contains local/infrastructure secret-like files and Auth0 values. Do not print, rotate, delete, or commit new secrets unless the user explicitly asks.

Treat these paths as sensitive:

- `infra/key.json`
- `infra/tf/environments/production/terraform.tfstate`
- `infra/tf/environments/production/terraform.tfstate.backup`
- `infra/tf/environments/production/terraform.tfvars`
- `services/*/src/main/resources/application.yml`
- `charts/rsoi/values-*.yaml`

## Existing Local Changes

Before editing, check `git status --short --branch`. At the time this file was created, the following files already had local modifications and should not be reverted without explicit user approval:

- `charts/rsoi/values-flights.yaml`
- `charts/rsoi/values-gateway.yaml`
- `charts/rsoi/values-privileges.yaml`
- `charts/rsoi/values-tickets.yaml`

## Subagent Guidance

Good independent ownership slices:

- Gateway routes, aggregation, auth, degradation: `services/gateway`.
- Flight catalog and airports: `services/flights`.
- Ticket purchase/cancel flows and calls to flights/privileges: `services/tickets`.
- Loyalty balance and history: `services/privileges`.
- Deployment manifests: `charts`, `docker-compose.yml`, and related service configuration.
- Infrastructure: `infra`, only when the task explicitly needs cloud resources.

When using subagents, give each one a disjoint write scope and ask it to list changed files and verification commands. Do not assign two agents to edit the same service unless one is read-only.

## More Context

Read `docs/agent-context.md` for the project map and `docs/subagent-playbook.md` for task-splitting templates.
