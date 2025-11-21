package org.gripday.bookstore.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(BookstoreConstants.FilterOrder.CORRELATION_ID_FILTER)
public class CorrelationIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    var correlationId = extractOrGenerateCorrelationId(request);

    try {
      MDC.put(BookstoreConstants.MdcKeys.CORRELATION_ID, correlationId);
      response.setHeader(BookstoreConstants.Headers.X_CORRELATION_ID, correlationId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(BookstoreConstants.MdcKeys.CORRELATION_ID);
    }
  }

  private String extractOrGenerateCorrelationId(HttpServletRequest request) {
    var correlationId = request.getHeader(BookstoreConstants.Headers.X_CORRELATION_ID);
    if (correlationId == null || correlationId.trim().isEmpty()) {
      correlationId = UUID.randomUUID().toString();
    }
    return correlationId;
  }
}