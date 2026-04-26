package ru.uncledrema.privileges.events;

import java.util.List;

public record CurrentActor(
        String subject,
        String username,
        List<String> roles
) {
}
