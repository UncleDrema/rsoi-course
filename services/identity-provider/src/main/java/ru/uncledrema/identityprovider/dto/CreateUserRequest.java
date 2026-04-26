package ru.uncledrema.identityprovider.dto;

public record CreateUserRequest(
        String username,
        String email,
        String name,
        String password
) {
}
