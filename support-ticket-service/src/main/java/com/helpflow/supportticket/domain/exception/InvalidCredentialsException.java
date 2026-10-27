package com.helpflow.supportticket.domain.exception;

/** Login failed because of unknown e-mail or wrong password. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException(String code, String message, Object... args) {
        super(code, message, args);
    }
}
