package ru.uncledrema.identityprovider.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.uncledrema.identityprovider.services.IdentityUserService;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {
    @Test
    void jwtConverterMapsRolesAndScopesToAuthorities() {
        SecurityConfig config = new SecurityConfig(mock(IdentityUserService.class), new IdentityProviderProperties());
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                java.util.Map.of("alg", "RS256"),
                java.util.Map.of("scope", "openid profile", "roles", List.of("ADMIN"))
        );

        var authentication = config.jwtAuthenticationConverter().convert(jwt);
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains("SCOPE_openid", "SCOPE_profile", "ROLE_ADMIN");
    }
}
