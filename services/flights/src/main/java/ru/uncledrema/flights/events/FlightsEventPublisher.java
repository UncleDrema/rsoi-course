package ru.uncledrema.flights.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlightsEventPublisher {
    private final EventsProperties properties;
    private final CurrentActorProvider currentActorProvider;
    private final ObjectProvider<KafkaTemplate<String, EventDto>> kafkaTemplateProvider;

    public void publish(String eventType, String entityType, String entityId, Map<String, Object> metadata) {
        if (!properties.enabled()) {
            return;
        }
        KafkaTemplate<String, EventDto> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            log.debug("Skipping event {} because KafkaTemplate is unavailable", eventType);
            return;
        }

        EventActor actor = currentActorProvider.getCurrentActor();
        EventDto event = new EventDto(
                UUID.randomUUID(),
                eventType,
                properties.service(),
                actor.sub(),
                actor.username(),
                actor.roles() == null ? List.of() : actor.roles(),
                entityType,
                entityId,
                metadata == null ? Map.of() : Map.copyOf(metadata),
                Instant.now()
        );

        kafkaTemplate.send(properties.topic(), entityId, event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("Failed to publish {} event for {} {}", eventType, entityType, entityId, throwable);
                    }
                });
    }
}
