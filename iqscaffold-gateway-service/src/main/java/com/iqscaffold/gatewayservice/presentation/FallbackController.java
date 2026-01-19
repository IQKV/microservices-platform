package com.iqscaffold.gatewayservice.presentation;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fallback controller for circuit breaker responses.
 *
 * <p>Provides graceful degradation when downstream services are unavailable.
 * Returns appropriate fallback responses based on the endpoint type.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

  /**
   * Fallback for feature-related endpoints.
   * Returns empty feature list to allow frontend to continue functioning.
   */
  @GetMapping("/features")
  public ResponseEntity<Map<String, Object>> featuresFallback() {
    Map<String, Object> fallbackResponse = Map.of(
        "enabledFeatures", Collections.emptyList(),
        "allFeatures", Collections.emptyList(),
        "planName", "Service Unavailable",
        "subscriptionStatus", "UNKNOWN",
        "tenantId", "unknown",
        "error", "Feature service temporarily unavailable",
        "timestamp", Instant.now()
    );

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header("X-Fallback-Response", "true")
        .body(fallbackResponse);
  }

  /**
   * Fallback for billing-related endpoints.
   * Returns service unavailable message.
   */
  @GetMapping("/billing")
  @PostMapping("/billing")
  public ResponseEntity<Map<String, Object>> billingFallback() {
    Map<String, Object> fallbackResponse = Map.of(
        "error", "Billing service temporarily unavailable",
        "message", "Please try again later",
        "timestamp", Instant.now(),
        "status", HttpStatus.SERVICE_UNAVAILABLE.value()
    );

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header("X-Fallback-Response", "true")
        .body(fallbackResponse);
  }

  /**
   * Fallback for webhook endpoints.
   * Returns accepted status to prevent webhook retries.
   */
  @PostMapping("/webhooks")
  public ResponseEntity<Map<String, Object>> webhooksFallback() {
    Map<String, Object> fallbackResponse = Map.of(
        "message", "Webhook received but service temporarily unavailable",
        "status", "queued",
        "timestamp", Instant.now()
    );

    return ResponseEntity.accepted()
        .header("X-Fallback-Response", "true")
        .body(fallbackResponse);
  }

  /**
   * Fallback for internal service-to-service calls.
   * Returns empty response to prevent cascading failures.
   */
  @GetMapping("/internal")
  @PostMapping("/internal")
  public ResponseEntity<Map<String, Object>> internalFallback() {
    Map<String, Object> fallbackResponse = Map.of(
        "enabled", false,
        "features", Collections.emptyList(),
        "error", "Internal service temporarily unavailable",
        "timestamp", Instant.now()
    );

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header("X-Fallback-Response", "true")
        .header("X-Internal-Fallback", "true")
        .body(fallbackResponse);
  }
}
