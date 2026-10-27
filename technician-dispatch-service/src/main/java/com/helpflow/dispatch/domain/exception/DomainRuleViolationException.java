package com.helpflow.dispatch.domain.exception;

/**
 * Thrown when a domain rule or invariant is violated.
 * Framework-free and independent of Spring/HTTP.
 */
public class DomainRuleViolationException extends IllegalArgumentException {

    public DomainRuleViolationException(String message) {
        super(message);
    }

    public DomainRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
