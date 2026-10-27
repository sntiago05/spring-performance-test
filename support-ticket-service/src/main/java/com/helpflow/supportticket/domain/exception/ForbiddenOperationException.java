package com.helpflow.supportticket.domain.exception;

/** The authenticated actor is not allowed to perform the operation. */
public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String code, String message, Object... args) {
        super(code, message, args);
    }
}
