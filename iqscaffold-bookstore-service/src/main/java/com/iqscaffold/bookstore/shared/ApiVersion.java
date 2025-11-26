package com.iqscaffold.bookstore.shared.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.web.bind.annotation.RequestMapping;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping
public @interface ApiVersion {

  /**
   * The version number for this API endpoint
   */
  String value() default "1";

  /**
   * Whether this version is deprecated
   */
  boolean deprecated() default false;

  /**
   * Deprecation message if the version is deprecated
   */
  String deprecationMessage() default "";
}