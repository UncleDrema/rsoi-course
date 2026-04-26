# Subagent Playbook

Use this file when splitting future course-work implementation across agents.

## Default Workflow

1. Read the user requirements and map each requirement to a service or deployment layer.
2. Keep one agent responsible for one write scope.
3. Give each agent the relevant service path, expected behavior, and verification command.
4. Ask agents to report changed files, commands run, and any unresolved assumptions.
5. Integrate cross-service API contracts centrally after workers finish.

## Suggested Agent Slices

### Gateway Agent

Write scope:

- `services/gateway/src/main/java`
- `services/gateway/src/main/resources/application.yml`
- `services/gateway/src/test`

Use for:

- Public `/api/v1/**` routes.
- Aggregation endpoints.
- Auth/JWT behavior.
- Fallback and degradation behavior.
- Gateway DTOs and downstream clients.

Verify:

```powershell
cd services\gateway
.\gradlew.bat compileJava
.\gradlew.bat test
```

### Flights Agent

Write scope:

- `services/flights/src/main/java`
- `services/flights/src/main/resources/application.yml`
- `services/flights/src/test`

Use for:

- Flight and airport catalog.
- Flight search/filtering/pagination.
- Flight domain model and persistence.

Verify:

```powershell
cd services\flights
.\gradlew.bat compileJava
.\gradlew.bat test
```

### Tickets Agent

Write scope:

- `services/tickets/src/main/java`
- `services/tickets/src/main/resources/application.yml`
- `services/tickets/src/test`

Use for:

- Ticket purchase, cancellation, status transitions.
- Calls to flights and privileges.
- Ticket DTO and persistence changes.

Verify:

```powershell
cd services\tickets
.\gradlew.bat compileJava
.\gradlew.bat test
```

### Privileges Agent

Write scope:

- `services/privileges/src/main/java`
- `services/privileges/src/main/resources/application.yml`
- `services/privileges/src/test`

Use for:

- Loyalty balance logic.
- Privilege status and history.
- Compensation on ticket cancellation.

Verify:

```powershell
cd services\privileges
.\gradlew.bat compileJava
.\gradlew.bat test
```

### Deployment Agent

Write scope:

- `docker-compose.yml`
- `postgres`
- `charts`
- service Docker/build metadata if added later

Use for:

- Local orchestration.
- Helm values/templates.
- Image names and environment variables.
- Database initialization scripts.

Do not touch Terraform unless the task explicitly involves cloud infrastructure.

## Prompt Template For Worker Agents

```text
You are working in a shared repository. Do not revert unrelated changes.

Task:
<specific requirement>

Ownership:
<paths the worker may edit>

Context:
- Read AGENTS.md and docs/agent-context.md first.
- Follow existing Spring Boot patterns in this service.
- Keep API changes compatible with gateway /api/v1 routing unless the requirement says otherwise.

Verification:
Run:
<commands>

Final response:
- Changed files.
- Verification results.
- Assumptions or blockers.
```

## Prompt Template For Explorer Agents

```text
Read-only task. Do not edit files.

Question:
<specific codebase question>

Focus paths:
<paths>

Return:
- Direct answer.
- Relevant files/classes.
- Risks or follow-up checks.
```

## Cross-Service Contract Checklist

When a requirement changes an endpoint or DTO:

- Update the owning service controller and DTO.
- Update gateway DTO/client/proxy behavior if the endpoint is exposed through gateway aggregation.
- Update callers in other services.
- Update degradation fallback JSON if the shape is used in `services/gateway/src/main/resources/application.yml`.
- Compile all affected services.
