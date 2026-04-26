package ru.uncledrema.privileges.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.uncledrema.privileges.events.PrivilegeEventPublisher;
import ru.uncledrema.privileges.types.Privilege;
import ru.uncledrema.privileges.types.PrivilegeHistory;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class PrivilegeService {
    private final PrivilegeRepository privilegeRepository;
    private final PrivilegeEventPublisher privilegeEventPublisher;

    public Privilege getPrivilegeByUsername(String username) {
        return privilegeRepository.findByUsername(username).orElseGet(() -> {
            var privilege = new Privilege(username, 0);
            return privilegeRepository.save(privilege);
        });
    }

    public Privilege withdraw(String username, UUID ticketId, Integer amount) {
        var privilege = getPrivilegeByUsername(username);
        privilege.withdraw(ticketId, amount);
        var savedPrivilege = privilegeRepository.save(privilege);
        privilegeEventPublisher.publish("PRIVILEGE_WITHDRAWN", savedPrivilege, getLastHistoryEntry(savedPrivilege));
        return savedPrivilege;
    }

    public Privilege deposit(String username, UUID ticketId, Integer amount) {
        var privilege = getPrivilegeByUsername(username);
        privilege.deposit(ticketId, amount);
        var savedPrivilege = privilegeRepository.save(privilege);
        privilegeEventPublisher.publish("PRIVILEGE_DEPOSITED", savedPrivilege, getLastHistoryEntry(savedPrivilege));
        return savedPrivilege;
    }

    public Privilege cancel(String username, UUID ticketUid) {
        var privilege = getPrivilegeByUsername(username);
        privilege.cancel(ticketUid);
        var savedPrivilege = privilegeRepository.save(privilege);
        privilegeEventPublisher.publish("PRIVILEGE_COMPENSATED", savedPrivilege, getLastHistoryEntry(savedPrivilege));
        return savedPrivilege;
    }

    private PrivilegeHistory getLastHistoryEntry(Privilege privilege) {
        if (privilege.getHistory().isEmpty()) {
            return null;
        }
        return privilege.getHistory().get(privilege.getHistory().size() - 1);
    }
}
