package ru.uncledrema.flights.web;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.uncledrema.flights.dto.AirportDto;
import ru.uncledrema.flights.dto.CreateAirportDto;
import ru.uncledrema.flights.dto.PageDto;
import ru.uncledrema.flights.events.FlightsEventPublisher;
import ru.uncledrema.flights.services.AirportService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/airports")
public class AirportController {
    private final AirportService airportService;
    private final FlightsEventPublisher events;

    @GetMapping
    public ResponseEntity<PageDto<AirportDto>> getAll(
            @RequestParam(name = "page", required = false, defaultValue = "1")
            int page,
            @RequestParam(name = "size", required = false, defaultValue = "20")
            int size
    ) {
        if (page < 0 || size < 1) {
            return ResponseEntity.badRequest().build();
        }
        var airports = airportService.findAll(PageRequest.of(page - 1, size));
        var dtos = airports.stream().map(
                flight -> new AirportDto(flight.getId(),
                        flight.getName(),
                        flight.getCity(),
                        flight.getCountry())
        ).toList();
        return ResponseEntity.ok(new PageDto<>(
                airports.getNumber() + 1,
                airports.getSize(),
                airports.getTotalElements(),
                dtos
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AirportDto> create(@RequestBody CreateAirportDto createAirportDto) {
        if (createAirportDto.name() == null || createAirportDto.city() == null || createAirportDto.country() == null) {
            return ResponseEntity.badRequest().build();
        }
        var airport = airportService.create(createAirportDto.name(), createAirportDto.city(), createAirportDto.country());
        var dto = new AirportDto(airport.getId(), airport.getName(), airport.getCity(), airport.getCountry());
        events.publish(
                "AIRPORT_CREATED",
                "airport",
                String.valueOf(airport.getId()),
                java.util.Map.of(
                        "name", airport.getName(),
                        "city", airport.getCity(),
                        "country", airport.getCountry()
                )
        );
        return ResponseEntity.ok(dto);
    }
}
