package ru.uncledrema.statistics.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.uncledrema.statistics.dto.EventDto;

@Component
@RequiredArgsConstructor
public class StatisticsConsumer {
    private final ObjectMapper objectMapper;
    private final EventIngestionService eventIngestionService;

    @KafkaListener(
            topics = "#{@kafkaProperties.topic()}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload) throws JsonProcessingException {
        EventDto event = objectMapper.readValue(payload, EventDto.class);
        eventIngestionService.ingest(event);
    }
}
