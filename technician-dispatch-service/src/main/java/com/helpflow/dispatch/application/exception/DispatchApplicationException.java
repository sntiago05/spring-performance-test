package com.helpflow.dispatch.application.exception;

/**
 * Thrown when an application use case cannot be completed.
 * Framework-free and independent of Spring/HTTP.
 */
public class DispatchApplicationException extends RuntimeException {

    public DispatchApplicationException(String message) {
        super(message);
    }

    public DispatchApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
