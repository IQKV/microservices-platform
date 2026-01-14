package com.iqscaffold.leadservice.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for phone number format.
 * Ensures the phone contains only digits, spaces, hyphens, and parentheses.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
@Documented
public @interface ValidPhone {

  String message() default "Phone number must contain only digits, spaces, hyphens, and parentheses";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
