package org.gripday.userservice.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation for password complexity validation. Ensures password contains uppercase, lowercase, number, and special character.
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

  String message() default "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}