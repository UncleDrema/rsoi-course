package ru.uncledrema.privileges.events;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ActionEventDto(
        UUID eventId,
        String eventType,
        String service,
        String actorSub,
        String actorUsername,
        List<String> actorRoles,
        String entityType,
        String entityId,
        Map<String, Object> metadata,
        Instant occurredAt
) {
}
