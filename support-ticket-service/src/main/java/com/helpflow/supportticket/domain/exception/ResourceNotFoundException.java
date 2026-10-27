package com.helpflow.supportticket.domain.exception;

/** The requested resource does not exist. */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String code, String message, Object... args) {
        super(code, message, args);
    }
}
