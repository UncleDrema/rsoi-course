package ru.uncledrema.statistics.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.uncledrema.statistics.dto.EventDto;
import ru.uncledrema.statistics.dto.PageDto;
import ru.uncledrema.statistics.dto.StatisticsEventFilters;
import ru.uncledrema.statistics.dto.StatisticsReportDto;
import ru.uncledrema.statistics.types.StatisticsEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsQueryService {
    private static final Sort SORT_BY_OCCURRED_AT_DESC = Sort.by(Sort.Direction.DESC, "occurredAt");
    private static final String EVENT_TICKET_PURCHASED = "TICKET_PURCHASED";
    private static final String EVENT_TICKET_CANCELED = "TICKET_CANCELED";
    private static final String EVENT_FLIGHT_CREATED = "FLIGHT_CREATED";
    private static final String EVENT_AIRPORT_CREATED = "AIRPORT_CREATED";
    private static final String EVENT_PRIVILEGE_DEPOSITED = "PRIVILEGE_DEPOSITED";
    private static final String EVENT_PRIVILEGE_WITHDRAWN = "PRIVILEGE_WITHDRAWN";
    private static final String EVENT_PRIVILEGE_COMPENSATED = "PRIVILEGE_COMPENSATED";

    private final StatisticsEventRepository eventRepository;
    private final StatisticsEventMapper eventMapper;

    public StatisticsReportDto getReport(Instant from, Instant to) {
        List<StatisticsEvent> events = eventRepository.findAll(buildSpecification(
                new StatisticsEventFilters(from, to, null, null, null, null, null, null, null)
        ), SORT_BY_OCCURRED_AT_DESC);

        List<EventDto> recentEvents = events.stream()
                .limit(10)
                .map(eventMapper::toDto)
                .toList();

        return new StatisticsReportDto(
                from,
                to,
                events.size(),
                countByEventType(events, EVENT_TICKET_PURCHASED),
                countByEventType(events, EVENT_TICKET_CANCELED),
                countByEventType(events, EVENT_FLIGHT_CREATED),
                countByEventType(events, EVENT_AIRPORT_CREATED),
                countByEventType(events, EVENT_PRIVILEGE_DEPOSITED),
                countByEventType(events, EVENT_PRIVILEGE_WITHDRAWN),
                countByEventType(events, EVENT_PRIVILEGE_COMPENSATED),
                countBy(events, StatisticsEvent::getEventType),
                countBy(events, StatisticsEvent::getService),
                recentEvents
        );
    }

    public PageDto<EventDto> getEvents(StatisticsEventFilters filters, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, SORT_BY_OCCURRED_AT_DESC);
        var result = eventRepository.findAll(buildSpecification(filters), pageable);
        return new PageDto<>(
                result.getNumber() + 1,
                result.getSize(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious(),
                result.stream().map(eventMapper::toDto).toList()
        );
    }

    static Specification<StatisticsEvent> buildSpecification(StatisticsEventFilters filters) {
        return Specification.<StatisticsEvent>allOf(
                containsIgnoreCaseIfPresent("eventType", filters.eventType()),
                containsIgnoreCaseIfPresent("service", filters.service()),
                equalsIfPresent("actorSub", filters.actorSub()),
                containsIgnoreCaseIfPresent("actorUsername", filters.actorUsername()),
                equalsIfPresent("entityType", filters.entityType()),
                containsIgnoreCaseIfPresent("entityId", filters.entityId()),
                globalQueryIfPresent(filters.query()),
                occurredAtGte(filters.from()),
                occurredAtLte(filters.to())
        );
    }

    private static Specification<StatisticsEvent> equalsIfPresent(String fieldName, String value) {
        return (root, query, builder) -> value == null || value.isBlank() ? null : builder.equal(root.get(fieldName), value);
    }

    private static Specification<StatisticsEvent> containsIgnoreCaseIfPresent(String fieldName, String value) {
        return (root, query, builder) -> {
            if (value == null || value.isBlank()) {
                return null;
            }
            return builder.like(
                    builder.lower(root.get(fieldName)),
                    "%" + value.toLowerCase(Locale.ROOT) + "%"
            );
        };
    }

    private static Specification<StatisticsEvent> globalQueryIfPresent(String value) {
        return (root, query, builder) -> {
            if (value == null || value.isBlank()) {
                return null;
            }
            String pattern = "%" + value.toLowerCase(Locale.ROOT) + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("actorUsername")), pattern),
                    builder.like(builder.lower(root.get("entityId")), pattern),
                    builder.like(builder.lower(root.get("eventType")), pattern),
                    builder.like(builder.lower(root.get("service")), pattern)
            );
        };
    }

    private static Specification<StatisticsEvent> occurredAtGte(Instant from) {
        return (root, query, builder) -> from == null ? null : builder.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    private static Specification<StatisticsEvent> occurredAtLte(Instant to) {
        return (root, query, builder) -> to == null ? null : builder.lessThanOrEqualTo(root.get("occurredAt"), to);
    }

    private static Map<String, Long> countBy(List<StatisticsEvent> events, Function<StatisticsEvent, String> classifier) {
        return events.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()));
    }

    private static long countByEventType(List<StatisticsEvent> events, String eventType) {
        return events.stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .count();
    }
}
