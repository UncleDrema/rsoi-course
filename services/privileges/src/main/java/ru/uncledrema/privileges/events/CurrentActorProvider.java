package ru.uncledrema.privileges.events;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class CurrentActorProvider {
    public CurrentActor requireCurrentActor() {
        CurrentActor actor = getCurrentActor();
        if (actor == null || actor.username() == null || actor.username().isBlank()) {
            throw new IllegalStateException("Authenticated JWT username claim is missing");
        }
        return actor;
    }

    public CurrentActor getCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return null;
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        String subject = jwt.getSubject();
        String username = firstNonBlank(jwt.getClaimAsString("preferred_username"), subject);
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && !authority.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        return new CurrentActor(subject, username, roles);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }
}
