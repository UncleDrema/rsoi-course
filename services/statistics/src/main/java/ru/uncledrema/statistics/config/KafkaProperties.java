package ru.uncledrema.statistics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "statistics.kafka")
public record KafkaProperties(String topic) {
}
