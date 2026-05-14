package ru.uncledrema.statistics.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StatisticsReportDto(
        Instant from,
        Instant to,
        long totalEvents,
        long ticketsPurchased,
        long ticketsCanceled,
        long flightsCreated,
        long airportsCreated,
        long privilegeDeposited,
        long privilegeWithdrawn,
        long privilegeCompensated,
        Map<String, Long> countsByEventType,
        Map<String, Long> countsByService,
        List<EventDto> recentEvents
) {
}
