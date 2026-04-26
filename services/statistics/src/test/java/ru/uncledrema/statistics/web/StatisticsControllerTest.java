package ru.uncledrema.statistics.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.uncledrema.statistics.dto.EventDto;
import ru.uncledrema.statistics.dto.PageDto;
import ru.uncledrema.statistics.dto.StatisticsReportDto;
import ru.uncledrema.statistics.services.StatisticsQueryService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsControllerTest {
    private StatisticsQueryService statisticsQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        statisticsQueryService = mock(StatisticsQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new StatisticsController(statisticsQueryService),
                new HealthController()
        ).build();
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/manage/health"))
                .andExpect(status().isOk());
    }

    @Test
    void reportReturnsSummary() throws Exception {
        when(statisticsQueryService.getReport(any(), any())).thenReturn(new StatisticsReportDto(
                Instant.parse("2026-04-26T00:00:00Z"),
                Instant.parse("2026-04-27T00:00:00Z"),
                2,
                Map.of("TICKET_PURCHASED", 2L),
                Map.of("tickets", 2L),
                List.of(sampleEvent())
        ));

        mockMvc.perform(get("/statistics/report")
                        .param("from", "2026-04-26T00:00:00Z")
                        .param("to", "2026-04-27T00:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(2))
                .andExpect(jsonPath("$.countsByEventType.TICKET_PURCHASED").value(2))
                .andExpect(jsonPath("$.recentEvents[0].eventType").value("TICKET_PURCHASED"));
    }

    @Test
    void eventsRejectInvalidPageArguments() throws Exception {
        mockMvc.perform(get("/statistics/events").param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eventsReturnPagedItems() throws Exception {
        when(statisticsQueryService.getEvents(any(), eq(1), eq(10))).thenReturn(new PageDto<>(
                1,
                10,
                1,
                List.of(sampleEvent())
        ));

        mockMvc.perform(get("/statistics/events")
                        .param("page", "1")
                        .param("size", "10")
                        .param("service", "tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].service").value("tickets"))
                .andExpect(jsonPath("$.items[0].metadata.status").value("PAID"));
    }

    private EventDto sampleEvent() {
        return new EventDto(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "TICKET_PURCHASED",
                "tickets",
                "user-1",
                "admin",
                List.of("ROLE_ADMIN"),
                "ticket",
                "abc-123",
                Map.of("status", "PAID"),
                Instant.parse("2026-04-26T10:15:30Z")
        );
    }
}
