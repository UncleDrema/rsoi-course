package ru.uncledrema.gateway.web;

public class CircuitOpenException extends RuntimeException {
    public CircuitOpenException() {
        super("Circuit is open");
    }
}
