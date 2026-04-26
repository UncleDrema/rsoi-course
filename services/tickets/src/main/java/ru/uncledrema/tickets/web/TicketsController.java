package ru.uncledrema.tickets.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.uncledrema.tickets.dto.BoughtTicketDto;
import ru.uncledrema.tickets.dto.BuyTicketDto;
import ru.uncledrema.tickets.dto.TicketDto;
import ru.uncledrema.tickets.events.CurrentActorResolver;
import ru.uncledrema.tickets.services.TicketService;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tickets")
public class TicketsController {
    private final TicketService ticketService;
    private final Mapper mapper;
    private final CurrentActorResolver currentActorResolver;

    @GetMapping
    public ResponseEntity<List<TicketDto>> getAllForUser(@AuthenticationPrincipal Jwt jwt) {
        String username = currentActorResolver.resolveUsername(jwt);
        var tickets = ticketService.findAllByUsername(username);
        return ResponseEntity.ok(tickets.stream().map(mapper::toDto).toList());
    }

    @PostMapping
    public ResponseEntity<BoughtTicketDto> buyTicket(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody BuyTicketDto buyTicketDto
    ) {
        String username = currentActorResolver.resolveUsername(jwt);
        var boughtTicket = ticketService.buyTicket(
                buyTicketDto.flightNumber(),
                buyTicketDto.price(),
                buyTicketDto.paidFromBalance(),
                username
        );
        return ResponseEntity.ok(boughtTicket);
    }

    @GetMapping("/{ticketUid}")
    public ResponseEntity<TicketDto> getByUid(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID ticketUid) {
        String username = currentActorResolver.resolveUsername(jwt);
        var ticket = ticketService.findByTicketUid(username, ticketUid);
        return ResponseEntity.ok(mapper.toDto(ticket));
    }

    @DeleteMapping("/{ticketUid}")
    public ResponseEntity<Void> cancelByUid(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID ticketUid) {
        String username = currentActorResolver.resolveUsername(jwt);
        ticketService.cancelTicket(username, ticketUid);
        return ResponseEntity.noContent().build();
    }
}
