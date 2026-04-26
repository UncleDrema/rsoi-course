package ru.uncledrema.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.uncledrema.statistics.config.KafkaProperties;

@SpringBootApplication
@EnableConfigurationProperties(KafkaProperties.class)
public class StatisticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatisticsApplication.class, args);
    }
}
