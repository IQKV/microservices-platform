package org.gripday.bookstore.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter Tests")
class CorrelationIdFilterTest {

  private CorrelationIdFilter filter;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter = new CorrelationIdFilter();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  @DisplayName("Should use existing correlation ID from request header")
  void shouldUseExistingCorrelationIdFromRequestHeader() throws Exception {
    // Arrange
    var existingCorrelationId = "existing-correlation-id-123";
    when(request.getHeader(BookstoreConstants.Headers.X_CORRELATION_ID))
        .thenReturn(existingCorrelationId);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(response).setHeader(BookstoreConstants.Headers.X_CORRELATION_ID, existingCorrelationId);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should generate new correlation ID when not present in request")
  void shouldGenerateNewCorrelationIdWhenNotPresentInRequest() throws Exception {
    // Arrange
    when(request.getHeader(BookstoreConstants.Headers.X_CORRELATION_ID)).thenReturn(null);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    var captor = ArgumentCaptor.forClass(String.class);
    verify(response).setHeader(eq(BookstoreConstants.Headers.X_CORRELATION_ID), captor.capture());
    var generatedId = captor.getValue();
    assertThat(generatedId).isNotNull().isNotEmpty();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Should generate new correlation ID when header is empty")
  void shouldGenerateNewCorrelationIdWhenHeaderIsEmpty() throws Exception {
    // Arrange
    when(request.getHeader(BookstoreConstants.Headers.X_CORRELATION_ID)).thenReturn("");

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    var captor = ArgumentCaptor.forClass(String.class);
    verify(response).setHeader(eq(BookstoreConstants.Headers.X_CORRELATION_ID), captor.capture());
    var generatedId = captor.getValue();
    assertThat(generatedId).isNotNull().isNotEmpty();
  }

  @Test
  @DisplayName("Should generate new correlation ID when header is blank")
  void shouldGenerateNewCorrelationIdWhenHeaderIsBlank() throws Exception {
    // Arrange
    when(request.getHeader(BookstoreConstants.Headers.X_CORRELATION_ID)).thenReturn("   ");

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    var captor = ArgumentCaptor.forClass(String.class);
    verify(response).setHeader(eq(BookstoreConstants.Headers.X_CORRELATION_ID), captor.capture());
    var generatedId = captor.getValue();
    assertThat(generatedId).isNotNull().isNotEmpty();
  }

  @Test
  @DisplayName("Should clean up MDC after filter chain execution")
  void shouldCleanUpMdcAfterFilterChainExecution() throws Exception {
    // Arrange
    when(request.getHeader(BookstoreConstants.Headers.X_CORRELATION_ID))
        .thenReturn("test-correlation-id");

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertThat(MDC.get(BookstoreConstants.MdcKeys.CORRELATION_ID)).isNull();
  }

  @Test
  @DisplayName("Should clean up MDC even when exception occurs")
  void shouldCleanUpMdcEvenWhenExceptionOccurs() throws Exception {
    // Arrange
    when(request.getHeader(BookstoreConstants.Headers.X_CORRELATION_ID))
        .thenReturn("test-correlation-id");
    var exception = new RuntimeException("Test exception");
    org.mockito.Mockito.doThrow(exception).when(filterChain).doFilter(any(), any());

    // Act & Assert
    try {
      filter.doFilterInternal(request, response, filterChain);
    } catch (RuntimeException e) {
      // Expected
    }
    assertThat(MDC.get(BookstoreConstants.MdcKeys.CORRELATION_ID)).isNull();
  }
}
