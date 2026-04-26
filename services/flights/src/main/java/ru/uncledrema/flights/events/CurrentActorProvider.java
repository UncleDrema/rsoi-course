package ru.uncledrema.flights.events;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class CurrentActorProvider {
    public EventActor getCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return new EventActor(null, null, List.of());
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        String subject = jwt.getSubject();
        String username = firstNonBlank(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("username"),
                jwt.getClaimAsString("name"),
                subject
        );
        List<String> roles = jwtAuthenticationToken.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        return new EventActor(subject, username, roles);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
