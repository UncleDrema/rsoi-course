package ru.uncledrema.statistics.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EventDto(
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
