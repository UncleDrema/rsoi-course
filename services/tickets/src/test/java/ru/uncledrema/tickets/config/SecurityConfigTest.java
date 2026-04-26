package ru.uncledrema.tickets.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityConfigTest {
    @Test
    void rolesClaimConverter_normalizesRoles() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("roles", List.of("admin", "ROLE_USER", " "))
        );

        var authorities = new SecurityConfig.RolesClaimConverter().convert(jwt);

        assertEquals(
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")),
                List.copyOf(authorities)
        );
    }
}
