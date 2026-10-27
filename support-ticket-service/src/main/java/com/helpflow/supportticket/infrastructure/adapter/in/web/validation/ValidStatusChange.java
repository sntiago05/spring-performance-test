package com.helpflow.supportticket.infrastructure.adapter.in.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cross-field rules of a status change request: ASIGNADO is reached only by assigning a technician,
 * and cancelling a ticket requires a meaningful reason (at least 15 characters).
 */
@Documented
@Constraint(validatedBy = ValidStatusChangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStatusChange {

    String message() default "{validation.status-change.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
