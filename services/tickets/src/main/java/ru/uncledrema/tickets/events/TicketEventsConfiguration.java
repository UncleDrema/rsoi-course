package ru.uncledrema.tickets.events;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(TicketEventsProperties.class)
public class TicketEventsConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "tickets.events", name = "enabled", havingValue = "true")
    public ProducerFactory<String, ActionEvent> ticketEventProducerFactory(TicketEventsProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ActionEventSerializer.class);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, properties.requestTimeoutMs());
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, properties.deliveryTimeoutMs());
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, properties.maxBlockMs());
        config.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        config.put(ProducerConfig.ACKS_CONFIG, "1");
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "tickets.events", name = "enabled", havingValue = "true")
    public KafkaTemplate<String, ActionEvent> ticketEventKafkaTemplate(
            ProducerFactory<String, ActionEvent> ticketEventProducerFactory
    ) {
        return new KafkaTemplate<>(ticketEventProducerFactory);
    }
}
