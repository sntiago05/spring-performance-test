package com.helpflow.dispatch.domain.exception;

/**
 * Thrown when an operation conflicts with the current domain state.
 * Framework-free and independent of Spring/HTTP.
 */
public class DomainConflictException extends IllegalStateException {

    public DomainConflictException(String message) {
        super(message);
    }

    public DomainConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
