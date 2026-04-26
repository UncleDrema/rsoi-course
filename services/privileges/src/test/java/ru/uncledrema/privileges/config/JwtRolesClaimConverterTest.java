package ru.uncledrema.privileges.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtRolesClaimConverterTest {
    private final JwtRolesClaimConverter converter = new JwtRolesClaimConverter();

    @Test
    void convert_mapsRolesClaimToRoleAuthorities() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                java.util.Map.of("alg", "none"),
                java.util.Map.of("roles", List.of("admin", "ROLE_USER", "admin"))
        );

        var authorities = converter.convert(jwt);

        assertEquals(List.of("ROLE_ADMIN", "ROLE_USER"), authorities.stream().map(Object::toString).toList());
    }
}
