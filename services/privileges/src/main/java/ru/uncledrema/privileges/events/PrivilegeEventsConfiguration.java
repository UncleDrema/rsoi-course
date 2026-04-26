package ru.uncledrema.privileges.events;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(PrivilegeEventsProperties.class)
public class PrivilegeEventsConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "privileges.events", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ProducerFactory<String, ActionEventDto> privilegeEventProducerFactory(PrivilegeEventsProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, properties.requestTimeoutMs());
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, properties.deliveryTimeoutMs());
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, properties.maxBlockMs());
        config.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        config.put(ProducerConfig.ACKS_CONFIG, "1");
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "privileges.events", name = "enabled", havingValue = "true", matchIfMissing = true)
    public KafkaTemplate<String, ActionEventDto> privilegeEventKafkaTemplate(
            ProducerFactory<String, ActionEventDto> privilegeEventProducerFactory
    ) {
        return new KafkaTemplate<>(privilegeEventProducerFactory);
    }
}
