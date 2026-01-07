package com.iqscaffold.userservice.usermanagement;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for supported locales.
 */
@Documented
@Constraint(validatedBy = LocaleValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLocale {
  String message() default "Invalid locale";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
