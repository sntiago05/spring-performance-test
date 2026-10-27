package com.helpflow.supportticket.infrastructure.adapter.in.web.validation;

import com.helpflow.supportticket.domain.model.TicketStatus;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.ChangeStatusRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class ValidStatusChangeValidator implements ConstraintValidator<ValidStatusChange, ChangeStatusRequest> {

    static final int MIN_CANCELLATION_REASON = 15;

    @Override
    public boolean isValid(ChangeStatusRequest request, ConstraintValidatorContext context) {
        if (request == null || request.status() == null) {
            return true; // reported by @NotNull on the field
        }
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        if (request.status() == TicketStatus.ASIGNADO) {
            context.buildConstraintViolationWithTemplate("{validation.status-change.use-assignment}")
                    .addPropertyNode("status").addConstraintViolation();
            valid = false;
        }
        if (request.status() == TicketStatus.CANCELADO && (request.observations() == null
                || request.observations().trim().length() < MIN_CANCELLATION_REASON)) {
            context.buildConstraintViolationWithTemplate("{validation.status-change.cancel-reason}")
                    .addPropertyNode("observations").addConstraintViolation();
            valid = false;
        }
        return valid;
    }
}
