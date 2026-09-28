package com.example.plan.exception;

public class DoseAlreadyLoggedException extends RuntimeException {
    public DoseAlreadyLoggedException(String message) {
        super(message);
    }
}
