package ru.uncledrema.statistics.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import ru.uncledrema.statistics.dto.EventDto;
import ru.uncledrema.statistics.types.StatisticsEvent;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StatisticsEventMapper {
    private static final TypeReference<List<String>> ROLES_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public StatisticsEvent toEntity(EventDto eventDto) {
        return StatisticsEvent.builder()
                .eventId(eventDto.eventId())
                .eventType(eventDto.eventType())
                .service(eventDto.service())
                .actorSub(eventDto.actorSub())
                .actorUsername(eventDto.actorUsername())
                .actorRolesJson(writeValue(eventDto.actorRoles() == null ? List.of() : eventDto.actorRoles()))
                .entityType(eventDto.entityType())
                .entityId(eventDto.entityId())
                .metadataJson(writeValue(eventDto.metadata() == null ? Map.of() : eventDto.metadata()))
                .occurredAt(eventDto.occurredAt())
                .build();
    }

    public EventDto toDto(StatisticsEvent event) {
        return new EventDto(
                event.getEventId(),
                event.getEventType(),
                event.getService(),
                event.getActorSub(),
                event.getActorUsername(),
                readValue(event.getActorRolesJson(), ROLES_TYPE),
                event.getEntityType(),
                event.getEntityId(),
                readValue(event.getMetadataJson(), METADATA_TYPE),
                event.getOccurredAt()
        );
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to serialize event payload", exception);
        }
    }

    private <T> T readValue(String value, TypeReference<T> typeReference) {
        if (value == null || value.isBlank()) {
            if (typeReference == ROLES_TYPE) {
                return (T) List.of();
            }
            return (T) Map.of();
        }
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to deserialize event payload", exception);
        }
    }
}
