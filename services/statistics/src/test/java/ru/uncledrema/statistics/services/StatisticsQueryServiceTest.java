package ru.uncledrema.statistics.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import tools.jackson.databind.json.JsonMapper;
import ru.uncledrema.statistics.dto.EventDto;
import ru.uncledrema.statistics.dto.StatisticsEventFilters;
import ru.uncledrema.statistics.types.StatisticsEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        StatisticsEvent first = event("TICKET_PURCHASED", "tickets", "1", "2026-04-26T10:15:30Z");
        StatisticsEvent second = event("PRIVILEGE_WITHDRAWN", "privileges", "2", "2026-04-26T09:15:30Z");
        StatisticsEvent third = event("TICKET_CANCELED", "tickets", "3", "2026-04-26T08:15:30Z");
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(Sort.by(Sort.Direction.DESC, "occurredAt"))))
                .thenReturn(List.of(first, second, third));

        var report = queryService.getReport(
                Instant.parse("2026-04-26T00:00:00Z"),
                Instant.parse("2026-04-27T00:00:00Z")
        );

        assertEquals(3, report.totalEvents());
        assertEquals(1, report.ticketsPurchased());
        assertEquals(1, report.ticketsCanceled());
        assertEquals(0, report.flightsCreated());
        assertEquals(0, report.airportsCreated());
        assertEquals(0, report.privilegeDeposited());
        assertEquals(1, report.privilegeWithdrawn());
        assertEquals(0, report.privilegeCompensated());
        assertEquals(Map.of("TICKET_PURCHASED", 1L, "PRIVILEGE_WITHDRAWN", 1L, "TICKET_CANCELED", 1L), report.countsByEventType());
        assertEquals(Map.of("tickets", 2L, "privileges", 1L), report.countsByService());
        assertEquals(3, report.recentEvents().size());
        assertEquals(first.getEventId(), report.recentEvents().getFirst().eventId());
    }

    @Test
    void getEventsReturnsPagedDtos() {
        StatisticsEvent event = event("TICKET_PURCHASED", "tickets", "1", "2026-04-26T10:15:30Z");
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "occurredAt")))))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 5), 7));

        var result = queryService.getEvents(
                new StatisticsEventFilters(null, null, "TICKET_PURCHASED", "tickets", null, null, null, null, null),
                1,
                5
        );

        assertEquals(1, result.page());
        assertEquals(5, result.size());
        assertEquals(5, result.pageSize());
        assertEquals(7, result.totalElements());
        assertEquals(2, result.totalPages());
        assertEquals(true, result.hasNext());
        assertEquals(false, result.hasPrevious());
        assertEquals(event.getEventId(), result.items().getFirst().eventId());
    }

    @Test
    void specificationUsesCaseInsensitiveContainsForSupportedFields() {
        @SuppressWarnings("unchecked")
        Root<StatisticsEvent> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        CriteriaQuery<Object> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        @SuppressWarnings("unchecked")
        Path<String> eventTypePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Expression<String> loweredEventType = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);

        when(root.<String>get("eventType")).thenReturn(eventTypePath);
        when(builder.lower(eventTypePath)).thenReturn(loweredEventType);
        when(builder.like(loweredEventType, "%ticket%")).thenReturn(predicate);

        Specification<StatisticsEvent> specification = StatisticsQueryService.buildSpecification(
                new StatisticsEventFilters(null, null, "Ticket", null, null, null, null, null, null)
        );

        Predicate actual = specification.toPredicate(root, query, builder);

        assertSame(predicate, actual);
        verify(builder).like(loweredEventType, "%ticket%");
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
