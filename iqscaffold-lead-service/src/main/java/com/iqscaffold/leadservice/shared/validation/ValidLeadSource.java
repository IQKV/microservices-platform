package com.iqscaffold.leadservice.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for lead source values.
 * Ensures the source is one of the predefined valid sources.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LeadSourceValidator.class)
@Documented
public @interface ValidLeadSource {

  String message() default "Invalid lead source. Must be one of: WEBSITE, REFERRAL, COLD_CALL, EMAIL_CAMPAIGN, SOCIAL_MEDIA, TRADE_SHOW, PARTNER, OTHER";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
