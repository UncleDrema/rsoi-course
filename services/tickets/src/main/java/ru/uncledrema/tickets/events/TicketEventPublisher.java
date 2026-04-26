package ru.uncledrema.tickets.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventPublisher {
    private final TicketEventsProperties properties;
    private final ObjectProvider<KafkaTemplate<String, ActionEvent>> kafkaTemplateProvider;

    public void publish(ActionEvent event) {
        if (!properties.enabled()) {
            return;
        }

        KafkaTemplate<String, ActionEvent> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            log.warn("Skipping ticket event {} because KafkaTemplate is not configured", event.eventId());
            return;
        }

        try {
            kafkaTemplate.send(properties.topic(), event.entityId(), event);
        }
        catch (Exception ex) {
            log.warn("Failed to publish ticket event {}", event.eventId(), ex);
        }
    }
}
