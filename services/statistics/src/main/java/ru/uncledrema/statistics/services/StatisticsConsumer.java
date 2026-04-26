package ru.uncledrema.statistics.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ru.uncledrema.statistics.dto.EventDto;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatisticsConsumer {
    private final ObjectMapper objectMapper;
    private final EventIngestionService eventIngestionService;

    @KafkaListener(
            topics = "${statistics.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload) throws JacksonException {
        log.info("Received event");
        EventDto event = objectMapper.readValue(payload, EventDto.class);
        eventIngestionService.ingest(event);
    }
}
