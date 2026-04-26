package ru.uncledrema.statistics.dto;

import java.time.Instant;

public record StatisticsEventFilters(
        Instant from,
        Instant to,
        String eventType,
        String service,
        String actorSub,
        String actorUsername,
        String entityType,
        String entityId
) {
}
