package org.gripday.gatewayservice.config;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gripday.gatewayservice.exception.CircuitBreakerOpenException;
import org.gripday.gatewayservice.exception.InvalidJwtTokenException;
import org.gripday.gatewayservice.exception.MissingTenantContextException;
import org.gripday.gatewayservice.exception.NoHealthyInstancesException;
import org.gripday.gatewayservice.exception.RateLimitExceededException;
import org.gripday.gatewayservice.exception.UnsupportedApiVersionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    exceptionHandler = new GlobalExceptionHandler(objectMapper);
  }

  @Test
  @DisplayName("Should handle RateLimitExceededException")
  void shouldHandleRateLimitExceededException() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new RateLimitExceededException("tenant-123", "/api/v1/users",
        RateLimitExceededException.RateLimitType.TENANT, 60);

    // Act
    var result = exceptionHandler.handle(exchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
    assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
  }

  @Test
  @DisplayName("Should handle NoHealthyInstancesException")
  void shouldHandleNoHealthyInstancesException() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new NoHealthyInstancesException("user-service", 3);

    // Act
    var result = exceptionHandler.handle(exchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  @DisplayName("Should handle UnsupportedApiVersionException")
  void shouldHandleUnsupportedApiVersionException() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v5/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new UnsupportedApiVersionException("v5", List.of("v1", "v2"));

    // Act
    var result = exceptionHandler.handle(exchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("Should handle MissingTenantContextException")
  void shouldHandleMissingTenantContextException() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/tenants/data").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new MissingTenantContextException("/api/v1/tenants/data");

    // Act
    var result = exceptionHandler.handle(exchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("Should handle InvalidJwtTokenException")
  void shouldHandleInvalidJwtTokenException() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new InvalidJwtTokenException("Token expired",
        InvalidJwtTokenException.TokenErrorType.EXPIRED);

    // Act
    var result = exceptionHandler.handle(exchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Should handle CircuitBreakerOpenException")
  void shouldHandleCircuitBreakerOpenException() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/auth/login").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new CircuitBreakerOpenException("user-service-cb", "user-service", 30);

    // Act
    var result = exceptionHandler.handle(exchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("30");
    assertThat(exchange.getResponse().getHeaders().getFirst("X-Circuit-Breaker")).isEqualTo("user-service-cb");
  }

  @Test
  @DisplayName("Should handle generic ResponseStatusException")
  void shouldHandleGenericResponseStatusException() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");

    // Act
    var result = exceptionHandler.handle(exchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("Should handle SecurityException")
  void shouldHandleSecurityException() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/admin/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new SecurityException("Access denied");

    // Act
    var result = exceptionHandler.handle(exchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("Should handle unexpected exceptions")
  void shouldHandleUnexpectedExceptions() {
    // Arrange
    var request = MockServerHttpRequest.get("/api/v1/users").build();
    var exchange = MockServerWebExchange.from(request);
    var exception = new RuntimeException("Unexpected error");

    // Act
    var result = exceptionHandler.handle(exchange, exception);

    // Assert
    StepVerifier.create(result)
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
