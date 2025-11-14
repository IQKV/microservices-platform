package org.gripday.userservice.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for username format validation. Ensures username follows proper format and security guidelines.
 */
@Documented
@Constraint(validatedBy = UsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {

  String message() default "Username must contain only alphanumeric characters, underscores, and hyphens, and cannot start or end with special characters";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}