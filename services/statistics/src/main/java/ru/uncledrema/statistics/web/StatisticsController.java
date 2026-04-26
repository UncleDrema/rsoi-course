package ru.uncledrema.statistics.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.uncledrema.statistics.dto.EventDto;
import ru.uncledrema.statistics.dto.PageDto;
import ru.uncledrema.statistics.dto.StatisticsEventFilters;
import ru.uncledrema.statistics.dto.StatisticsReportDto;
import ru.uncledrema.statistics.services.StatisticsQueryService;

import java.time.Instant;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsQueryService statisticsQueryService;

    @GetMapping("/report")
    public ResponseEntity<StatisticsReportDto> getReport(
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to
    ) {
        return ResponseEntity.ok(statisticsQueryService.getReport(from, to));
    }

    @GetMapping("/events")
    public ResponseEntity<PageDto<EventDto>> getEvents(
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "service", required = false) String service,
            @RequestParam(name = "actorSub", required = false) String actorSub,
            @RequestParam(name = "actorUsername", required = false) String actorUsername,
            @RequestParam(name = "entityType", required = false) String entityType,
            @RequestParam(name = "entityId", required = false) String entityId,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        if (page < 1 || size < 1) {
            return ResponseEntity.badRequest().build();
        }
        var filters = new StatisticsEventFilters(from, to, eventType, service, actorSub, actorUsername, entityType, entityId);
        return ResponseEntity.ok(statisticsQueryService.getEvents(filters, page, size));
    }
}
