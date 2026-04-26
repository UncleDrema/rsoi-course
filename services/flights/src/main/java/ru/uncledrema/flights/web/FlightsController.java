package ru.uncledrema.flights.web;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.uncledrema.flights.dto.CreateFlightDto;
import ru.uncledrema.flights.dto.FlightDto;
import ru.uncledrema.flights.dto.PageDto;
import ru.uncledrema.flights.events.FlightsEventPublisher;
import ru.uncledrema.flights.services.FlightService;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/flights")
public class FlightsController {
    private final FlightService flightService;
    private final FlightsEventPublisher events;

    @GetMapping
    public ResponseEntity<PageDto<FlightDto>> getAll(
            @RequestParam(name = "page", required = false, defaultValue = "1")
            int page,
            @RequestParam(name = "size", required = false, defaultValue = "20")
            int size
    ) {
        if (page < 0 || size < 1) {
            return ResponseEntity.badRequest().build();
        }
        var flights = flightService.findAll(PageRequest.of(page - 1, size));
        var dtos = flights.stream().map(
                flight -> new FlightDto(flight.getFlightNumber(),
                        flight.getFromAirport().getFullName(),
                        flight.getToAirport().getFullName(),
                        flight.getDatetime().toLocalDateTime(),
                        flight.getPrice())
        ).toList();
        var response = ResponseEntity.ok(new PageDto<>(
                flights.getNumber() + 1,
                flights.getSize(),
                flights.getTotalElements(),
                dtos
        ));
        if (response.getBody() != null) {
            events.publish(
                    "FLIGHTS_VIEWED",
                    "flight-list",
                    "page:" + response.getBody().page(),
                    Map.of(
                            "page", response.getBody().page(),
                            "size", response.getBody().pageSize(),
                            "totalElements", response.getBody().totalElements()
                    )
            );
        }
        return response;
    }

    @GetMapping("/{flightNumber}")
    public ResponseEntity<FlightDto> getByFlightNumber(
            @PathVariable String flightNumber
    ) {
        var flightOpt = flightService.findByFlightNumber(flightNumber);
        if (flightOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var flight = flightOpt.get();
        var dto = new FlightDto(
                flight.getFlightNumber(),
                flight.getFromAirport().getFullName(),
                flight.getToAirport().getFullName(),
                flight.getDatetime().toLocalDateTime(),
                flight.getPrice()
        );
        events.publish(
                "FLIGHT_VIEWED",
                "flight",
                flight.getFlightNumber(),
                Map.of("flightNumber", flight.getFlightNumber())
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightDto> create(
            @RequestBody CreateFlightDto flightCreateDto
    ) {
        if (flightCreateDto.flightNumber() == null ||
                flightCreateDto.datetime() == null) {
            return ResponseEntity.badRequest().build();
        }
        var flight = flightService.create(
                flightCreateDto.flightNumber(),
                flightCreateDto.datetime(),
                flightCreateDto.fromAirportId(),
                flightCreateDto.toAirportId(),
                flightCreateDto.price()
        );
        var dto = new FlightDto(
                flight.getFlightNumber(),
                flight.getFromAirport().getFullName(),
                flight.getToAirport().getFullName(),
                flight.getDatetime().toLocalDateTime(),
                flight.getPrice()
        );
        events.publish(
                "FLIGHT_CREATED",
                "flight",
                flight.getFlightNumber(),
                Map.of(
                        "flightNumber", flight.getFlightNumber(),
                        "fromAirportId", flight.getFromAirport().getId(),
                        "toAirportId", flight.getToAirport().getId(),
                        "price", flight.getPrice()
                )
        );
        return ResponseEntity.ok(dto);
    }
}
