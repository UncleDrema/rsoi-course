package ru.uncledrema.tickets.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tickets.events")
public record TicketEventsProperties(
        boolean enabled,
        String topic,
        String bootstrapServers,
        int requestTimeoutMs,
        int deliveryTimeoutMs,
        int maxBlockMs
) {
    public TicketEventsProperties {
        topic = topic == null || topic.isBlank() ? "rsoi.actions" : topic;
        bootstrapServers = bootstrapServers == null || bootstrapServers.isBlank() ? "localhost:9092" : bootstrapServers;
        requestTimeoutMs = requestTimeoutMs <= 0 ? 1500 : requestTimeoutMs;
        deliveryTimeoutMs = deliveryTimeoutMs <= 0 ? 3000 : deliveryTimeoutMs;
        maxBlockMs = maxBlockMs <= 0 ? 1500 : maxBlockMs;
    }
}
