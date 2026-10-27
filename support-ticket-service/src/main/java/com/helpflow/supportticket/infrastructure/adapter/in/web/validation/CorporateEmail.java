package com.helpflow.supportticket.infrastructure.adapter.in.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A well-formed e-mail that is not hosted by a public mailbox provider (gmail.com, hotmail.com...).
 */
@Documented
@Constraint(validatedBy = CorporateEmailValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CorporateEmail {

    String message() default "{validation.email.corporate}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
