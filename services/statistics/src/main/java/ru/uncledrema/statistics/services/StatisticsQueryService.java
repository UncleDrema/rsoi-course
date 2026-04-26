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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsQueryService {
    private static final Sort SORT_BY_OCCURRED_AT_DESC = Sort.by(Sort.Direction.DESC, "occurredAt");

    private final StatisticsEventRepository eventRepository;
    private final StatisticsEventMapper eventMapper;

    public StatisticsReportDto getReport(Instant from, Instant to) {
        List<StatisticsEvent> events = eventRepository.findAll(buildSpecification(
                new StatisticsEventFilters(from, to, null, null, null, null, null, null)
        ), SORT_BY_OCCURRED_AT_DESC);

        List<EventDto> recentEvents = events.stream()
                .limit(10)
                .map(eventMapper::toDto)
                .toList();

        return new StatisticsReportDto(
                from,
                to,
                events.size(),
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
                result.getTotalElements(),
                result.stream().map(eventMapper::toDto).toList()
        );
    }

    static Specification<StatisticsEvent> buildSpecification(StatisticsEventFilters filters) {
        return Specification.<StatisticsEvent>allOf(
                equalsIfPresent("eventType", filters.eventType()),
                equalsIfPresent("service", filters.service()),
                equalsIfPresent("actorSub", filters.actorSub()),
                equalsIfPresent("actorUsername", filters.actorUsername()),
                equalsIfPresent("entityType", filters.entityType()),
                equalsIfPresent("entityId", filters.entityId()),
                occurredAtGte(filters.from()),
                occurredAtLte(filters.to())
        );
    }

    private static Specification<StatisticsEvent> equalsIfPresent(String fieldName, String value) {
        return (root, query, builder) -> value == null || value.isBlank() ? null : builder.equal(root.get(fieldName), value);
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
}
