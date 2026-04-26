package ru.uncledrema.statistics.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.uncledrema.statistics.dto.EventDto;
import ru.uncledrema.statistics.types.StatisticsEvent;

@Service
@RequiredArgsConstructor
public class EventIngestionService {
    private final StatisticsEventMapper eventMapper;
    private final StatisticsEventRepository eventRepository;

    public void ingest(EventDto eventDto) {
        StatisticsEvent event = eventMapper.toEntity(eventDto);
        eventRepository.save(event);
    }
}
