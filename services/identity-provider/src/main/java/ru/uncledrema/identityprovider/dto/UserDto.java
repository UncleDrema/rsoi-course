package ru.uncledrema.identityprovider.dto;

import ru.uncledrema.identityprovider.types.IdentityRole;

import java.util.Set;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        String name,
        Set<IdentityRole> roles
) {
}
