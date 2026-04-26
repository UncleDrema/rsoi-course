package ru.uncledrema.tickets.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public class ActionEventSerializer implements Serializer<ActionEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public byte[] serialize(String topic, ActionEvent data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(data);
        }
        catch (JsonProcessingException ex) {
            throw new SerializationException("Failed to serialize ticket action event", ex);
        }
    }
}
