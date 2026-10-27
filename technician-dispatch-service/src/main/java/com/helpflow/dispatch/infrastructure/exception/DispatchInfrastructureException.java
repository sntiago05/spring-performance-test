package com.helpflow.dispatch.infrastructure.exception;

/**
 * Thrown when an infrastructure adapter, configuration, or technical component fails.
 */
public class DispatchInfrastructureException extends RuntimeException {

    public DispatchInfrastructureException(String message) {
        super(message);
    }

    public DispatchInfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
