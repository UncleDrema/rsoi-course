package ru.uncledrema.statistics.services;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import ru.uncledrema.statistics.dto.EventDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsEventMapperTest {
    private final StatisticsEventMapper mapper = new StatisticsEventMapper(JsonMapper.builder().build());

    @Test
    void mapsRoundTrip() {
        EventDto eventDto = new EventDto(
                UUID.randomUUID(),
                "TICKET_CREATED",
                "tickets",
                "auth0|user-1",
                "worker",
                List.of("ROLE_ADMIN", "ROLE_USER"),
                "ticket",
                "abc-123",
                Map.of("price", 1000, "status", "PAID"),
                Instant.parse("2026-04-26T10:15:30Z")
        );

        var entity = mapper.toEntity(eventDto);
        var mappedDto = mapper.toDto(entity);

        assertEquals(eventDto, mappedDto);
    }
}
