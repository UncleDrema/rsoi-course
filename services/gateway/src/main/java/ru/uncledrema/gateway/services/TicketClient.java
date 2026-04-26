package ru.uncledrema.gateway.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.uncledrema.gateway.dto.TicketDto;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketClient {
    private final RestTemplate restTemplate;

    @Value("${downstream.tickets:http://localhost:8070}")
    private String serviceUrl;

    public Optional<List<TicketDto>> getTickets() {
        try {
            ResponseEntity<TicketDto[]> response = restTemplate.exchange(
                    serviceUrl + "/tickets",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    TicketDto[].class
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Optional.of(List.of(response.getBody()));
            }
            return Optional.empty();
        }
        catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw ex;
        }
    }
}
