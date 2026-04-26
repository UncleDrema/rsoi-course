package ru.uncledrema.privileges.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.uncledrema.privileges.types.Privilege;
import ru.uncledrema.privileges.types.PrivilegeHistory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class PrivilegeEventPublisher {
    private final ObjectProvider<KafkaTemplate<String, ActionEventDto>> kafkaTemplateProvider;
    private final CurrentActorProvider currentActorProvider;
    private final PrivilegeEventsProperties properties;

    public PrivilegeEventPublisher(
            ObjectProvider<KafkaTemplate<String, ActionEventDto>> kafkaTemplateProvider,
            CurrentActorProvider currentActorProvider,
            PrivilegeEventsProperties properties) {
        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.currentActorProvider = currentActorProvider;
        this.properties = properties;
    }

    public void publish(String eventType, Privilege privilege, PrivilegeHistory historyEntry) {
        if (!properties.enabled() || historyEntry == null) {
            return;
        }

        KafkaTemplate<String, ActionEventDto> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            log.warn("Skipping privilege event {} because KafkaTemplate is not configured", eventType);
            return;
        }

        CurrentActor actor = currentActorProvider.getCurrentActor();
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (historyEntry.getTicketUid() != null) {
            metadata.put("ticketUid", historyEntry.getTicketUid());
        }
        if (historyEntry.getBalanceDiff() != null) {
            metadata.put("amount", historyEntry.getBalanceDiff());
        }
        if (historyEntry.getOperationType() != null) {
            metadata.put("operationType", historyEntry.getOperationType().name());
        }

        ActionEventDto event = new ActionEventDto(
                UUID.randomUUID(),
                eventType,
                properties.service(),
                actor != null ? actor.subject() : null,
                actor != null ? actor.username() : null,
                actor != null ? actor.roles() : java.util.List.of(),
                "PRIVILEGE",
                privilege.getId() != null ? privilege.getId().toString() : privilege.getUsername(),
                metadata,
                Instant.now()
        );

        try {
            kafkaTemplate.send(properties.topic(), event.entityId(), event);
        } catch (Exception exception) {
            log.warn("Failed to publish privilege event {}", event.eventType(), exception);
        }
    }
}
