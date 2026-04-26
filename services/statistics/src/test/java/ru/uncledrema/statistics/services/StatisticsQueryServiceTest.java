package ru.uncledrema.statistics.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.json.JsonMapper;
import ru.uncledrema.statistics.dto.EventDto;
import ru.uncledrema.statistics.dto.StatisticsEventFilters;
import ru.uncledrema.statistics.types.StatisticsEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatisticsQueryServiceTest {
    private StatisticsEventRepository repository;
    private StatisticsQueryService queryService;

    @BeforeEach
    void setUp() {
        repository = mock(StatisticsEventRepository.class);
        queryService = new StatisticsQueryService(repository, new StatisticsEventMapper(JsonMapper.builder().build()));
    }

    @Test
    void reportAggregatesCountsAndRecentEvents() {
        StatisticsEvent first = event("TICKET_CREATED", "tickets", "1", "2026-04-26T10:15:30Z");
        StatisticsEvent second = event("PRIVILEGE_WITHDRAWN", "privileges", "2", "2026-04-26T09:15:30Z");
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(Sort.by(Sort.Direction.DESC, "occurredAt"))))
                .thenReturn(List.of(first, second));

        var report = queryService.getReport(
                Instant.parse("2026-04-26T00:00:00Z"),
                Instant.parse("2026-04-27T00:00:00Z")
        );

        assertEquals(2, report.totalEvents());
        assertEquals(Map.of("TICKET_CREATED", 1L, "PRIVILEGE_WITHDRAWN", 1L), report.countsByEventType());
        assertEquals(Map.of("tickets", 1L, "privileges", 1L), report.countsByService());
        assertEquals(2, report.recentEvents().size());
        assertEquals(first.getEventId(), report.recentEvents().getFirst().eventId());
    }

    @Test
    void getEventsReturnsPagedDtos() {
        StatisticsEvent event = event("TICKET_CREATED", "tickets", "1", "2026-04-26T10:15:30Z");
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "occurredAt")))))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 5), 1));

        var result = queryService.getEvents(
                new StatisticsEventFilters(null, null, "TICKET_CREATED", "tickets", null, null, null, null),
                1,
                5
        );

        assertEquals(1, result.page());
        assertEquals(5, result.size());
        assertEquals(1, result.totalElements());
        assertEquals(event.getEventId(), result.items().getFirst().eventId());
    }

    private StatisticsEvent event(String eventType, String service, String entityId, String occurredAt) {
        return StatisticsEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .service(service)
                .actorSub("auth0|user")
                .actorUsername("user")
                .actorRolesJson("[\"ROLE_ADMIN\"]")
                .entityType("ticket")
                .entityId(entityId)
                .metadataJson("{\"status\":\"PAID\"}")
                .occurredAt(Instant.parse(occurredAt))
                .build();
    }
}
