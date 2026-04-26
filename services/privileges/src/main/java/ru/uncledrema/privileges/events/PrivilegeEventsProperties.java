package ru.uncledrema.privileges.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "privileges.events")
public record PrivilegeEventsProperties(
        boolean enabled,
        String service,
        String topic,
        String bootstrapServers,
        int requestTimeoutMs,
        int deliveryTimeoutMs,
        int maxBlockMs
) {
}
