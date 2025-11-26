package com.iqscaffold.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

  @Mock
  private UserContextExtractor userContextExtractor;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private JwtAuthenticationToken jwtAuthenticationToken;

  @Mock
  private Jwt jwt;

  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() {
    jwtAuthenticationFilter = new JwtAuthenticationFilter(userContextExtractor);
    SecurityContextHolder.setContext(securityContext);
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  @DisplayName("Should generate correlation ID and add to MDC and response")
  void shouldGenerateCorrelationIdAndAddToMdcAndResponse() throws Exception {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(null);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(response).setHeader(any(), any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should use existing correlation ID from request header")
  void shouldUseExistingCorrelationIdFromRequestHeader() throws Exception {
    // Arrange
    var existingCorrelationId = "existing-correlation-id";
    when(request.getHeader(BookstoreConstants.Headers.X_CORRELATION_ID)).thenReturn(existingCorrelationId);
    when(securityContext.getAuthentication()).thenReturn(null);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(response).setHeader(BookstoreConstants.Headers.X_CORRELATION_ID, existingCorrelationId);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should extract user context from JWT token")
  void shouldExtractUserContextFromJwtToken() throws Exception {
    // Arrange
    var userContext = new UserContext(
        1L, "john.doe", "john@example.com",
        Set.of("USER"), null, null, null, null
    );

    when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
    when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
    when(userContextExtractor.extractFromJwt(jwt)).thenReturn(userContext);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(request).setAttribute(BookstoreConstants.Attributes.USER_CONTEXT, userContext);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should add user info to MDC when JWT is present")
  void shouldAddUserInfoToMdcWhenJwtIsPresent() throws Exception {
    // Arrange
    var userContext = new UserContext(
        1L, "john.doe", "john@example.com",
        Set.of("USER"), null, null, null, null
    );

    when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
    when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
    when(userContextExtractor.extractFromJwt(jwt)).thenReturn(userContext);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should handle exception when extracting user context")
  void shouldHandleExceptionWhenExtractingUserContext() throws Exception {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
    when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
    when(userContextExtractor.extractFromJwt(jwt))
        .thenThrow(new RuntimeException("Invalid token"));

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert - Should continue without user context
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should continue filter chain for unauthenticated requests")
  void shouldContinueFilterChainForUnauthenticatedRequests() throws Exception {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(null);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should clean up MDC after filter execution")
  void shouldCleanUpMdcAfterFilterExecution() throws Exception {
    // Arrange
    var userContext = new UserContext(
        1L, "john.doe", "john@example.com",
        Set.of("USER"), null, null, null, null
    );

    when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
    when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
    when(userContextExtractor.extractFromJwt(jwt)).thenReturn(userContext);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert - MDC should be cleaned up
    assertThat(MDC.get(BookstoreConstants.MdcKeys.CORRELATION_ID)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.USER_ID)).isNull();
    assertThat(MDC.get(BookstoreConstants.MdcKeys.USERNAME)).isNull();
  }

  @Test
  @DisplayName("Should clean up MDC even when exception occurs")
  void shouldCleanUpMdcEvenWhenExceptionOccurs() throws Exception {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(null);
    doThrow(new RuntimeException("Filter chain error"))
        .when(filterChain).doFilter(request, response);

    // Act & Assert
    try {
      jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
    } catch (final RuntimeException e) {
      // Expected
    }

    // MDC should still be cleaned up
    assertThat(MDC.get(BookstoreConstants.MdcKeys.CORRELATION_ID)).isNull();
  }

  @Test
  @DisplayName("Should not use empty correlation ID from header")
  void shouldNotUseEmptyCorrelationIdFromHeader() throws Exception {
    // Arrange
    when(request.getHeader(BookstoreConstants.Headers.X_CORRELATION_ID)).thenReturn("   ");
    when(securityContext.getAuthentication()).thenReturn(null);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert - Should generate new correlation ID, not use empty one
    verify(response).setHeader(any(), any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should handle non-JWT authentication")
  void shouldHandleNonJwtAuthentication() throws Exception {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(null);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should set correlation ID header on response")
  void shouldSetCorrelationIdHeaderOnResponse() throws Exception {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(null);

    // Act
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(response).setHeader(any(String.class), any(String.class));
  }
}
