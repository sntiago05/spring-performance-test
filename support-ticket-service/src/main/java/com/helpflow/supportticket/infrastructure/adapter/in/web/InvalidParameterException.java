package com.helpflow.supportticket.infrastructure.adapter.in.web;

class InvalidParameterException extends RuntimeException {

    private final String parameter;

    InvalidParameterException(String parameter) {
        super("Invalid value for parameter " + parameter);
        this.parameter = parameter;
    }

    String parameter() {
        return parameter;
    }
}
