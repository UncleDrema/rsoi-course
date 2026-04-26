package ru.uncledrema.privileges.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.uncledrema.privileges.dto.BalanceOperationDto;
import ru.uncledrema.privileges.dto.PrivilegeHistoryItemDto;
import ru.uncledrema.privileges.dto.PrivilegeInfoDto;
import ru.uncledrema.privileges.events.CurrentActorProvider;
import ru.uncledrema.privileges.services.PrivilegeService;
import ru.uncledrema.privileges.types.Privilege;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/privilege")
public class PrivilegeController {
    private final PrivilegeService privilegeService;
    private final CurrentActorProvider currentActorProvider;

    @GetMapping
    public ResponseEntity<PrivilegeInfoDto> getPrivilege() {
        String username = currentActorProvider.requireCurrentActor().username();
        Privilege privilege = privilegeService.getPrivilegeByUsername(username);

        return ResponseEntity.ok(mapToDto(privilege));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<PrivilegeInfoDto> withdraw(@RequestBody BalanceOperationDto balanceOperation) {
        String username = currentActorProvider.requireCurrentActor().username();
        if (balanceOperation.amount() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        var privilege = privilegeService.withdraw(username, balanceOperation.ticketUid(), balanceOperation.amount());
        return ResponseEntity.ok(mapToDto(privilege));
    }

    @PostMapping("/deposit")
    public ResponseEntity<PrivilegeInfoDto> deposit(@RequestBody BalanceOperationDto balanceOperation) {
        String username = currentActorProvider.requireCurrentActor().username();
        if (balanceOperation.amount() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        var privilege = privilegeService.deposit(username, balanceOperation.ticketUid(), balanceOperation.amount());
        return ResponseEntity.ok(mapToDto(privilege));
    }

    @PostMapping("/cancel/{ticketUid}")
    public ResponseEntity<PrivilegeInfoDto> cancel(@PathVariable UUID ticketUid) {
        String username = currentActorProvider.requireCurrentActor().username();
        var privilege = privilegeService.cancel(username, ticketUid);

        return ResponseEntity.ok(mapToDto(privilege));
    }

    private PrivilegeInfoDto mapToDto(Privilege privilege) {
        return new PrivilegeInfoDto(
                privilege.getBalance(),
                privilege.getStatus(),
                privilege.getHistory().stream().map(
                        entry -> new PrivilegeHistoryItemDto(
                                entry.getDatetime(),
                                entry.getTicketUid(),
                                entry.getBalanceDiff(),
                                entry.getOperationType()
                        )
                ).toList()
        );
    }
}
