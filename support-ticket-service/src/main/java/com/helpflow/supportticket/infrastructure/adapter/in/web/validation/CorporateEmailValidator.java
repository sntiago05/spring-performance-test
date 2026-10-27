package com.helpflow.supportticket.infrastructure.adapter.in.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class CorporateEmailValidator implements ConstraintValidator<CorporateEmail, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null/blank is reported by @NotBlank; this constraint only judges present values.
        return value == null || value.isBlank()
                || com.helpflow.supportticket.domain.model.CorporateEmail.isValid(value.trim());
    }
}
