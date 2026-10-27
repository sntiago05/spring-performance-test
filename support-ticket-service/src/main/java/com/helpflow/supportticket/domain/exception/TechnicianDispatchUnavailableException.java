package com.helpflow.supportticket.domain.exception;

/** The external technician-dispatch service could not provide a suggestion. */
public class TechnicianDispatchUnavailableException extends DomainException {

    public TechnicianDispatchUnavailableException(String code, String message, Object... args) {
        super(code, message, args);
    }
}
