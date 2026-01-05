package com.iqscaffold.billingservice.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspect for automatic structured logging of business operations.
 * Provides performance monitoring and operation tracking.
 */
@Aspect
@Component
public class LoggingAspect {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingAspect.class);

  /**
   * Annotation to mark methods for automatic performance logging.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface LogPerformance {
    String operation() default "";
    boolean logArgs() default false;
    boolean logResult() default false;
  }

  /**
   * Annotation to mark methods for automatic business event logging.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface LogBusinessEvent {
    String eventType() default "";
    String description() default "";
  }

  /**
   * Around advice for performance logging.
   */
  @Around("@annotation(logPerformance)")
  public Object logPerformance(ProceedingJoinPoint joinPoint, LogPerformance logPerformance) throws Throwable {
    String operation = logPerformance.operation().isEmpty() 
        ? joinPoint.getSignature().toShortString() 
        : logPerformance.operation();
    
    long startTime = System.currentTimeMillis();
    
    try {
      // Add operation context to MDC
      MDC.put("operation", operation);
      
      if (logPerformance.logArgs()) {
        log.debug("Starting operation: {} with args: {}", operation, joinPoint.getArgs());
      } else {
        log.debug("Starting operation: {}", operation);
      }
      
      Object result = joinPoint.proceed();
      
      long duration = System.currentTimeMillis() - startTime;
      
      // Log performance metric
      LoggingConfiguration.StructuredLogger.logPerformanceMetric(operation, duration, null);
      
      if (logPerformance.logResult()) {
        log.debug("Completed operation: {} in {}ms with result: {}", operation, duration, result);
      } else {
        log.debug("Completed operation: {} in {}ms", operation, duration);
      }
      
      return result;
      
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Failed operation: {} after {}ms - Error: {}", operation, duration, e.getMessage(), e);
      throw e;
    } finally {
      MDC.remove("operation");
    }
  }

  /**
   * Around advice for business event logging.
   */
  @Around("@annotation(logBusinessEvent)")
  public Object logBusinessEvent(ProceedingJoinPoint joinPoint, LogBusinessEvent logBusinessEvent) throws Throwable {
    String eventType = logBusinessEvent.eventType().isEmpty() 
        ? joinPoint.getSignature().getName() 
        : logBusinessEvent.eventType();
    
    String description = logBusinessEvent.description().isEmpty() 
        ? joinPoint.getSignature().toShortString() 
        : logBusinessEvent.description();
    
    try {
      // Add business event context to MDC
      MDC.put("businessEventType", eventType);
      MDC.put("businessEventDescription", description);
      
      log.info("Business event started: {} - {}", eventType, description);
      
      Object result = joinPoint.proceed();
      
      log.info("Business event completed: {} - {}", eventType, description);
      
      return result;
      
    } catch (Exception e) {
      log.error("Business event failed: {} - {} - Error: {}", eventType, description, e.getMessage(), e);
      throw e;
    } finally {
      MDC.remove("businessEventType");
      MDC.remove("businessEventDescription");
    }
  }

  /**
   * Around advice for service layer methods to add automatic logging.
   */
  @Around("execution(* com.iqscaffold.billingservice.*.service.*.*(..))")
  public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    String operation = className + "." + methodName;
    
    long startTime = System.currentTimeMillis();
    
    try {
      MDC.put("serviceClass", className);
      MDC.put("serviceMethod", methodName);
      
      log.debug("Service call: {}", operation);
      
      Object result = joinPoint.proceed();
      
      long duration = System.currentTimeMillis() - startTime;
      
      if (duration > 1000) { // Log slow operations
        log.warn("Slow service operation: {} took {}ms", operation, duration);
      } else {
        log.debug("Service call completed: {} in {}ms", operation, duration);
      }
      
      return result;
      
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Service call failed: {} after {}ms - Error: {}", operation, duration, e.getMessage(), e);
      throw e;
    } finally {
      MDC.remove("serviceClass");
      MDC.remove("serviceMethod");
    }
  }

  /**
   * Around advice for repository layer methods to add automatic logging.
   */
  @Around("execution(* com.iqscaffold.billingservice.*.repository.*.*(..))")
  public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();
    String operation = className + "." + methodName;
    
    long startTime = System.currentTimeMillis();
    
    try {
      MDC.put("repositoryClass", className);
      MDC.put("repositoryMethod", methodName);
      
      Object result = joinPoint.proceed();
      
      long duration = System.currentTimeMillis() - startTime;
      
      if (duration > 500) { // Log slow database operations
        log.warn("Slow database operation: {} took {}ms", operation, duration);
      } else {
        log.trace("Database operation: {} completed in {}ms", operation, duration);
      }
      
      return result;
      
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Database operation failed: {} after {}ms - Error: {}", operation, duration, e.getMessage(), e);
      throw e;
    } finally {
      MDC.remove("repositoryClass");
      MDC.remove("repositoryMethod");
    }
  }
}