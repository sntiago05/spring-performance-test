package com.helpflow.supportticket.domain.exception;

/**
 * Base of every business failure. The {@code code} is a stable identifier used by the web adapter
 * to look up localized messages; the {@code args} fill their placeholders. The exception message
 * itself is English and only meant for logs.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;
    private final transient Object[] args;

    protected DomainException(String code, String message, Object... args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args;
    }
}
