package ru.uncledrema.statistics.types;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "statistics_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsEvent {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private String actorSub;

    @Column(nullable = false)
    private String actorUsername;

    @Column(nullable = false, columnDefinition = "text")
    private String actorRolesJson;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false, columnDefinition = "text")
    private String metadataJson;

    @Column(nullable = false)
    private Instant occurredAt;
}
