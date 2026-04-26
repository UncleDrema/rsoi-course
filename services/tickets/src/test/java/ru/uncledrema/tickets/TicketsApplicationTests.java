package ru.uncledrema.tickets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import ru.uncledrema.tickets.dto.BoughtTicketDto;
import ru.uncledrema.tickets.dto.FlightDto;
import ru.uncledrema.tickets.dto.PrivilegeShortInfoDto;
import ru.uncledrema.tickets.events.ActionEvent;
import ru.uncledrema.tickets.events.CurrentActor;
import ru.uncledrema.tickets.events.CurrentActorResolver;
import ru.uncledrema.tickets.events.TicketEventPublisher;
import ru.uncledrema.tickets.services.FlightClient;
import ru.uncledrema.tickets.services.PrivilegeClient;
import ru.uncledrema.tickets.services.TicketRepository;
import ru.uncledrema.tickets.services.TicketService;
import ru.uncledrema.tickets.types.PrivilegeStatus;
import ru.uncledrema.tickets.types.Ticket;
import ru.uncledrema.tickets.types.TicketStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketsApplicationTests {

    private TicketRepository ticketRepository;
    private FlightClient flightClient;
    private PrivilegeClient privilegeClient;
    private CurrentActorResolver currentActorResolver;
    private TicketEventPublisher ticketEventPublisher;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        flightClient = mock(FlightClient.class);
        privilegeClient = mock(PrivilegeClient.class);
        currentActorResolver = mock(CurrentActorResolver.class);
        ticketEventPublisher = mock(TicketEventPublisher.class);
        ticketService = new TicketService(
                ticketRepository,
                flightClient,
                privilegeClient,
                currentActorResolver,
                ticketEventPublisher
        );
    }

    @Test
    void findAllByUsername_returnsTickets() {
        String username = "user";
        List<Ticket> tickets = List.of(new Ticket(UUID.randomUUID(), username, "FL123", 100, TicketStatus.PAID));
        when(ticketRepository.findAllByUsername(username)).thenReturn(tickets);

        List<Ticket> result = ticketService.findAllByUsername(username);

        assertEquals(1, result.size());
        assertEquals(username, result.get(0).getUsername());
    }

    @Test
    void findByTicketUid_returnsTicket() {
        String username = "user";
        UUID ticketUid = UUID.randomUUID();
        Ticket ticket = new Ticket(ticketUid, username, "FL123", 100, TicketStatus.PAID);
        when(ticketRepository.findByUsernameAndTicketUid(username, ticketUid)).thenReturn(Optional.of(ticket));

        Ticket result = ticketService.findByTicketUid(username, ticketUid);

        assertEquals(ticketUid, result.getTicketUid());
    }

    @Test
    void cancelTicket_throwsIfNotPaid() {
        String username = "user";
        UUID ticketUid = UUID.randomUUID();
        Ticket ticket = new Ticket(ticketUid, username, "FL123", 100, TicketStatus.CANCELED);
        when(ticketRepository.findByUsernameAndTicketUid(username, ticketUid)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class, () -> ticketService.cancelTicket(username, ticketUid));
    }

    @Test
    void buyTicket_paidFromBalance_publishesEvent() {
        String username = "user";
        String flightNumber = "FL123";
        int price = 100;
        var flight = new FlightDto(flightNumber, "A", "B", LocalDateTime.now(), price);
        var privilege = new PrivilegeShortInfoDto(50, PrivilegeStatus.BRONZE);

        when(flightClient.getFlight(flightNumber)).thenReturn(Optional.of(flight));
        when(privilegeClient.getPrivilegeForUser(username)).thenReturn(Optional.of(privilege));
        when(currentActorResolver.resolveCurrentActor()).thenReturn(new CurrentActor("sub-1", username, List.of("ROLE_USER")));

        BoughtTicketDto dto = ticketService.buyTicket(flightNumber, price, true, username);

        assertEquals(flightNumber, dto.flightNumber());
        assertEquals(TicketStatus.PAID, dto.status());
        verify(ticketEventPublisher).publish(any(ActionEvent.class));
    }

    @Test
    void buyTicket_paidFromBalanceWithZeroBalance_skipsWithdraw() {
        String username = "user";
        String flightNumber = "FL123";
        int price = 100;
        var flight = new FlightDto(flightNumber, "A", "B", LocalDateTime.now(), price);
        var privilege = new PrivilegeShortInfoDto(0, PrivilegeStatus.BRONZE);

        when(flightClient.getFlight(flightNumber)).thenReturn(Optional.of(flight));
        when(privilegeClient.getPrivilegeForUser(username)).thenReturn(Optional.of(privilege));
        when(currentActorResolver.resolveCurrentActor()).thenReturn(new CurrentActor("sub-1", username, List.of("ROLE_USER")));

        BoughtTicketDto dto = ticketService.buyTicket(flightNumber, price, true, username);

        assertEquals(0, dto.paidByBonuses());
        verify(privilegeClient, never()).withdrawBonuses(any(String.class), any(UUID.class), anyInt());
        verify(ticketEventPublisher).publish(any(ActionEvent.class));
    }

    @Test
    void cancelTicket_publishesCanceledEventAfterSave() {
        String username = "user";
        UUID ticketUid = UUID.randomUUID();
        Ticket ticket = new Ticket(ticketUid, username, "FL123", 100, TicketStatus.PAID);
        PrivilegeShortInfoDto privilege = new PrivilegeShortInfoDto(100, PrivilegeStatus.SILVER);

        when(ticketRepository.findByUsernameAndTicketUid(username, ticketUid)).thenReturn(Optional.of(ticket));
        when(privilegeClient.cancel(username, ticketUid)).thenReturn(Optional.of(privilege));
        when(currentActorResolver.resolveCurrentActor()).thenReturn(new CurrentActor("sub-1", username, List.of("ROLE_USER")));

        ticketService.cancelTicket(username, ticketUid);

        assertEquals(TicketStatus.CANCELED, ticket.getStatus());
        verify(ticketRepository).save(ticket);
        verify(ticketEventPublisher).publish(any(ActionEvent.class));
    }

    @Test
    void buyTicket_doesNotPublishWhenFlightMissing() {
        String username = "user";

        when(flightClient.getFlight("FL123")).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> ticketService.buyTicket("FL123", 100, true, username));
        verify(ticketEventPublisher, never()).publish(any(ActionEvent.class));
    }

    @Test
    void currentActorResolver_usesPreferredUsernameAndAuthorities() {
        CurrentActorResolver resolver = new CurrentActorResolver();
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "sub-1", "preferred_username", "alice")
        );

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            assertEquals("alice", resolver.resolveUsername(jwt));
            CurrentActor actor = resolver.resolveCurrentActor();
            assertEquals("sub-1", actor.subject());
            assertEquals("alice", actor.username());
            assertEquals(List.of("ROLE_ADMIN", "ROLE_USER"), actor.roles());
        }
        finally {
            SecurityContextHolder.clearContext();
        }
    }
}
