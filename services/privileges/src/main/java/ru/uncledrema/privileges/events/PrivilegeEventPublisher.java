package ru.uncledrema.privileges.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final KafkaTemplate<String, ActionEventDto> kafkaTemplate;
    private final CurrentActorProvider currentActorProvider;
    private final boolean enabled;
    private final String topic;
    private final String serviceName;

    public PrivilegeEventPublisher(
            KafkaTemplate<String, ActionEventDto> kafkaTemplate,
            CurrentActorProvider currentActorProvider,
            @Value("${privileges.events.enabled:true}") boolean enabled,
            @Value("${privileges.events.topic:rsoi.actions}") String topic,
            @Value("${spring.application.name:privileges}") String serviceName) {
        this.kafkaTemplate = kafkaTemplate;
        this.currentActorProvider = currentActorProvider;
        this.enabled = enabled;
        this.topic = topic;
        this.serviceName = serviceName;
    }

    public void publish(String eventType, Privilege privilege, PrivilegeHistory historyEntry) {
        if (!enabled || historyEntry == null) {
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
                serviceName,
                actor != null ? actor.subject() : null,
                actor != null ? actor.username() : null,
                actor != null ? actor.roles() : java.util.List.of(),
                "PRIVILEGE",
                privilege.getId() != null ? privilege.getId().toString() : privilege.getUsername(),
                metadata,
                Instant.now()
        );

        try {
            kafkaTemplate.send(topic, event.entityId(), event);
        } catch (Exception exception) {
            log.warn("Failed to publish privilege event {}", event.eventType(), exception);
        }
    }
}
