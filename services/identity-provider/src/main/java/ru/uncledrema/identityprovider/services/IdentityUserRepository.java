package ru.uncledrema.identityprovider.services;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.uncledrema.identityprovider.types.IdentityUser;

import java.util.Optional;
import java.util.UUID;

public interface IdentityUserRepository extends JpaRepository<IdentityUser, UUID> {
    Optional<IdentityUser> findByUsernameIgnoreCase(String username);
}
