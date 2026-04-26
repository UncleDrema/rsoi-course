package ru.uncledrema.statistics.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import ru.uncledrema.statistics.dto.EventDto;
import ru.uncledrema.statistics.types.StatisticsEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventIngestionService {
    private final StatisticsEventMapper eventMapper;
    private final StatisticsEventRepository eventRepository;

    public void ingest(EventDto eventDto) {
        log.info("Ingesting event: {}", eventDto);
        StatisticsEvent event = eventMapper.toEntity(eventDto);
        eventRepository.save(event);
    }
}
