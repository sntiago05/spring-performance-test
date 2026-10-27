package com.helpflow.supportticket.domain.exception;

/** A unique value (document, e-mail) is already in use. */
public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String code, String message, Object... args) {
        super(code, message, args);
    }
}
