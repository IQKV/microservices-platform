package org.gripday.gatewayservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

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
    @DisplayName("Should handle ResponseStatusException with UNAUTHORIZED status")
    void shouldHandleResponseStatusExceptionWithUnauthorizedStatus() {
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);
        var exception = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token missing");

        exceptionHandler.handle(exchange, exception).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    @DisplayName("Should handle SecurityException with FORBIDDEN status")
    void shouldHandleSecurityExceptionWithForbiddenStatus() {
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);
        var exception = new SecurityException("Access denied");

        exceptionHandler.handle(exchange, exception).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should handle JwtException with UNAUTHORIZED status")
    void shouldHandleJwtExceptionWithUnauthorizedStatus() {
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);
        var jwtException = new JwtException("Invalid token");
        var exception = new RuntimeException("Wrapper", jwtException);

        exceptionHandler.handle(exchange, exception).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should handle generic exception with INTERNAL_SERVER_ERROR status")
    void shouldHandleGenericExceptionWithInternalServerErrorStatus() {
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);
        var exception = new RuntimeException("Unexpected error");

        exceptionHandler.handle(exchange, exception).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Should handle ResponseStatusException with custom reason")
    void shouldHandleResponseStatusExceptionWithCustomReason() {
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);
        var exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input");

        exceptionHandler.handle(exchange, exception).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
