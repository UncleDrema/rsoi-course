package ru.uncledrema.tickets.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.uncledrema.tickets.dto.BoughtTicketDto;
import ru.uncledrema.tickets.events.ActionEvent;
import ru.uncledrema.tickets.events.CurrentActor;
import ru.uncledrema.tickets.events.CurrentActorResolver;
import ru.uncledrema.tickets.events.TicketEventPublisher;
import ru.uncledrema.tickets.types.Ticket;
import ru.uncledrema.tickets.types.TicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class TicketService {
    private final TicketRepository ticketRepository;
    private final FlightClient flightClient;
    private final PrivilegeClient privilegeClient;
    private final CurrentActorResolver currentActorResolver;
    private final TicketEventPublisher ticketEventPublisher;

    public List<Ticket> findAllByUsername(String username) {
        return ticketRepository.findAllByUsername(username);
    }

    public Ticket findByTicketUid(String username, UUID ticketUid) {
        return ticketRepository.findByUsernameAndTicketUid(username, ticketUid).orElseThrow();
    }

    public void cancelTicket(String username, UUID ticketUid) {
        var ticket = findByTicketUid(username, ticketUid);
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new IllegalStateException("Ticket is not in PAID status");
        }

        var privilegeAfterCancel = privilegeClient.cancel(username, ticketUid).orElseThrow();
        log.info("Canceling ticket {}, after cancel: {}", ticket.getTicketUid(), privilegeAfterCancel);
        ticket.setStatus(TicketStatus.CANCELED);
        ticketRepository.save(ticket);
        publishEvent(
                "TICKET_CANCELED",
                ticket,
                Map.of(
                        "flightNumber", ticket.getFlightNumber(),
                        "price", ticket.getPrice(),
                        "status", ticket.getStatus().name(),
                        "privilegeBalance", privilegeAfterCancel.balance(),
                        "privilegeStatus", privilegeAfterCancel.status().name()
                )
        );
    }

    public BoughtTicketDto buyTicket(String flightNumber, int price, boolean paidFromBalance, String username) {
        var flight = flightClient.getFlight(flightNumber).orElseThrow();
        log.info("Flight {} found: {}", flightNumber, flight);
        var userPrivilege = privilegeClient.getPrivilegeForUser(username).orElseThrow();
        log.info("User {} privilege: {}", username, userPrivilege);
        var ticketUid = UUID.randomUUID();
        int moneyPaid;
        int bonusPaid;
        if (paidFromBalance) {
            bonusPaid = Math.min(userPrivilege.balance(), price);
            moneyPaid = price - bonusPaid;
            privilegeClient.withdrawBonuses(username, ticketUid, bonusPaid);
        }
        else {
            moneyPaid = price;
            bonusPaid = 0;
            var bonusEarned = price * 10 / 100;
            privilegeClient.depositBonuses(username, ticketUid, bonusEarned);
        }

        var finalPrivilege = privilegeClient.getPrivilegeForUser(username).orElseThrow();
        var ticket = new Ticket(ticketUid, username, flightNumber, price, TicketStatus.PAID);
        ticketRepository.save(ticket);
        var result = new BoughtTicketDto(
                ticket.getTicketUid(),
                ticket.getFlightNumber(),
                flight.fromAirport(),
                flight.toAirport(),
                flight.date(),
                ticket.getPrice(),
                moneyPaid,
                bonusPaid,
                ticket.getStatus(),
                finalPrivilege
        );
        publishEvent(
                "TICKET_PURCHASED",
                ticket,
                Map.of(
                        "flightNumber", ticket.getFlightNumber(),
                        "price", ticket.getPrice(),
                        "paidFromBalance", paidFromBalance,
                        "paidByMoney", moneyPaid,
                        "paidByBonuses", bonusPaid,
                        "privilegeBalance", finalPrivilege.balance(),
                        "privilegeStatus", finalPrivilege.status().name()
                )
        );
        return result;
    }

    private void publishEvent(String eventType, Ticket ticket, Map<String, Object> metadata) {
        CurrentActor actor = currentActorResolver.resolveCurrentActor();
        ticketEventPublisher.publish(new ActionEvent(
                UUID.randomUUID(),
                eventType,
                "tickets",
                actor.subject(),
                actor.username(),
                actor.roles(),
                "TICKET",
                ticket.getTicketUid().toString(),
                metadata,
                Instant.now()
        ));
    }
}
