package ru.uncledrema.statistics.services;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.uncledrema.statistics.types.StatisticsEvent;

import java.util.UUID;

public interface StatisticsEventRepository extends JpaRepository<StatisticsEvent, UUID>, JpaSpecificationExecutor<StatisticsEvent> {
}
