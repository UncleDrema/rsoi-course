package ru.uncledrema.flights.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    private final SecurityConfig.RolesClaimConverter converter = new SecurityConfig.RolesClaimConverter();

    @Test
    void mapsRolesClaimToSpringAuthorities() {
        Jwt jwt = new Jwt(
                "token",
                java.time.Instant.now(),
                java.time.Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("roles", List.of("admin", "ROLE_user", "admin"))
        );

        var authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }
}
