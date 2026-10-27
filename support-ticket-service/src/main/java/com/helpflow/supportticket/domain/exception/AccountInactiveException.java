package com.helpflow.supportticket.domain.exception;

/** Login rejected because the account is INACTIVO. */
public class AccountInactiveException extends DomainException {

    public AccountInactiveException(String code, String message, Object... args) {
        super(code, message, args);
    }
}
