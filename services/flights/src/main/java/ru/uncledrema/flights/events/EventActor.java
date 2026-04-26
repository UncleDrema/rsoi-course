package ru.uncledrema.flights.events;

import java.util.List;

record EventActor(
        String sub,
        String username,
        List<String> roles
) {
}
