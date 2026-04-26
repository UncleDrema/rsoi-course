package ru.uncledrema.flights.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "events")
public record EventsProperties(
        boolean enabled,
        String service,
        String topic
) {
}
