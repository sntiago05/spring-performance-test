package com.helpflow.supportticket.domain.exception;

/** A request that is well formed but breaks a business rule. */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String code, String message, Object... args) {
        super(code, message, args);
    }
}
